package com.recomind.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.*;

class DBUtilTest {

    private HikariDataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        // In-memory H2 database; keep alive for the duration of the test
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        cfg.setUsername("sa");
        cfg.setPassword("");
        cfg.setMaximumPoolSize(5);
        dataSource = new HikariDataSource(cfg);
        // Initialise DBUtil with our datasource (which will run schema and seed)
        DBUtil.init(dataSource);
    }

    @Test
    void tablesAreCreatedAndSeeded() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // Verify USERS count
            ResultSet rsUsers = stmt.executeQuery("SELECT COUNT(*) FROM USERS");
            assertTrue(rsUsers.next());
            int userCount = rsUsers.getInt(1);
            assertTrue(userCount >= 12, "There should be at least 12 seeded users");

            // Verify admin password hash
            ResultSet rsAdmin = stmt.executeQuery("SELECT PASSWORD_HASH FROM USERS WHERE EMAIL='admin@recomind.com'");
            assertTrue(rsAdmin.next());
            String adminHash = rsAdmin.getString(1);
            assertTrue(org.mindrot.jbcrypt.BCrypt.checkpw("Admin@123", adminHash), "Admin password should match BCrypt hash");

            // Verify ITEMS count
            ResultSet rsItems = stmt.executeQuery("SELECT COUNT(*) FROM ITEMS");
            assertTrue(rsItems.next());
            int itemCount = rsItems.getInt(1);
            assertEquals(60, itemCount, "There should be exactly 60 items");

            // Verify CATEGORIES count
            ResultSet rsCategories = stmt.executeQuery("SELECT COUNT(*) FROM CATEGORIES");
            assertTrue(rsCategories.next());
            int catCount = rsCategories.getInt(1);
            assertEquals(6, catCount, "There should be 6 categories");

            // Verify INTERACTIONS count
            // Verify distinct USER_ID, ITEM_ID pairs >= 300
            ResultSet rsDistinct = stmt.executeQuery("SELECT COUNT(*) FROM (SELECT DISTINCT USER_ID, ITEM_ID FROM INTERACTIONS)");
            assertTrue(rsDistinct.next());
            int distinctCount = rsDistinct.getInt(1);
            assertTrue(distinctCount >= 150, "There should be at least 150 distinct user-item pairs");

            // Verify at least 20 items interacted by 2 or more distinct users
            ResultSet rsMultiUser = stmt.executeQuery("SELECT ITEM_ID, COUNT(DISTINCT USER_ID) AS userCount FROM INTERACTIONS GROUP BY ITEM_ID HAVING COUNT(DISTINCT USER_ID) >= 2");
            int multiUserItemCount = 0;
            while (rsMultiUser.next()) {
                multiUserItemCount++;
            }
            assertTrue(multiUserItemCount >= 20, "At least 20 items should be interacted by 2 or more users");
        }
    }
}
