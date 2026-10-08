package com.recomind.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.recomind.exception.DAOException;
import com.recomind.model.AuditLog;
import com.recomind.model.Interaction;
import com.recomind.model.User;
import com.recomind.service.TransactionManager;
import com.recomind.util.DBUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** DAO and transaction tests against a fresh in-memory H2 database per test. */
class DaoIntegrationTest {
    private HikariDataSource ds;
    private UserDaoImpl userDao;
    private InteractionDaoImpl interactionDao;
    private SettingsDaoImpl settingsDao;
    private AuditDaoImpl auditDao;

    @BeforeEach
    void setUp() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:h2:mem:dao_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        cfg.setUsername("sa");
        cfg.setPassword("");
        cfg.setMaximumPoolSize(4);
        ds = new HikariDataSource(cfg);
        DBUtil.init(ds); // creates schema and seed data
        userDao = new UserDaoImpl();
        interactionDao = new InteractionDaoImpl();
        settingsDao = new SettingsDaoImpl();
        auditDao = new AuditDaoImpl();
    }

    @AfterEach
    void tearDown() {
        ds.close();
    }

    private static Interaction interaction(long userId, long itemId, String type) {
        Interaction i = new Interaction();
        i.setUserId(userId);
        i.setItemId(itemId);
        i.setType(type);
        return i;
    }

    @Test
    void userCrud() {
        User admin = userDao.findByEmail("admin@recomind.com");
        assertNotNull(admin);
        assertTrue(admin.getPasswordHash().startsWith("$2a$"));
        assertTrue(userDao.findAll().size() >= 12);

        User u = new User();
        u.setEmail("new.user@test.com");
        u.setPasswordHash("hash");
        u.setActive(true);
        u.setRoleId(userDao.findByEmail("user@recomind.com").getRoleId());
        userDao.create(u);

        User saved = userDao.findByEmail("new.user@test.com");
        assertNotNull(saved);
        saved.setActive(false);
        userDao.update(saved);
        assertFalse(userDao.findById(saved.getId()).isActive());

        userDao.delete(saved.getId());
        assertNull(userDao.findByEmail("new.user@test.com"));
    }

    @Test
    void interactionInsertAndQueries() {
        int before = interactionDao.countAll();
        assertTrue(before >= 500);

        Interaction i = interaction(1, 1, "LIKE");
        interactionDao.insert(i);
        assertTrue(i.getId() > 0);
        assertEquals(before + 1, interactionDao.countAll());

        assertTrue(interactionDao.findByUser(1).stream().anyMatch(x -> x.getId() == i.getId()));
        assertFalse(interactionDao.findByItem(1).isEmpty());
        assertTrue(interactionDao.countByType().containsKey("LIKE"));
        assertEquals(60, interactionDao.countByItem().size());
        assertFalse(interactionDao.dailyCounts(30).isEmpty());
    }

    @Test
    void settingsSaveAndLoad() {
        assertEquals("0.4", settingsDao.getAll().get("weight.content"));
        settingsDao.save("weight.content", "0.5");
        assertEquals("0.5", settingsDao.get("weight.content", "x"));

        Map<String, String> more = new HashMap<>();
        more.put("recommendation.count", "7");
        more.put("brand.new.key", "abc");
        settingsDao.saveAll(more);
        assertEquals("7", settingsDao.get("recommendation.count", "10"));
        assertEquals("abc", settingsDao.get("brand.new.key", ""));
        assertEquals("fallback", settingsDao.get("missing.key", "fallback"));
    }

    @Test
    void auditInsertAndRecent() {
        auditDao.insert(new AuditLog(1L, "TEST_ACTION", null, "details"));
        List<AuditLog> recent = auditDao.findRecent(5);
        assertFalse(recent.isEmpty());
        assertEquals("TEST_ACTION", recent.get(0).getAction());
    }

    @Test
    void transactionCommitsAllSteps() {
        int before = interactionDao.countAll();
        TransactionManager.runInTransaction(() -> {
            interactionDao.insert(interaction(2, 3, "VIEW"));
            auditDao.insert(new AuditLog(2L, "COMMIT_TEST", null, "ok"));
        });
        assertEquals(before + 1, interactionDao.countAll());
        assertEquals("COMMIT_TEST", auditDao.findRecent(1).get(0).getAction());
    }

    @Test
    void transactionRollsBackEverythingOnFailure() {
        int before = interactionDao.countAll();
        // third step violates the foreign key (user 99999 does not exist) -> whole transaction must roll back
        assertThrows(DAOException.class, () -> TransactionManager.runInTransaction(() -> {
            interactionDao.insert(interaction(1, 1, "LIKE"));
            auditDao.insert(new AuditLog(1L, "SHOULD_ROLL_BACK", null, "x"));
            interactionDao.insert(interaction(99999, 1, "LIKE"));
        }));
        assertEquals(before, interactionDao.countAll());
        assertTrue(auditDao.findRecent(10).stream().noneMatch(a -> "SHOULD_ROLL_BACK".equals(a.getAction())));
    }
}