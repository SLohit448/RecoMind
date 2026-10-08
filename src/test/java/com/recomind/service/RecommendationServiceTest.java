package com.recomind.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.recomind.dao.InteractionDaoImpl;
import com.recomind.model.Interaction;
import com.recomind.recommender.Recommendation;
import com.recomind.util.DBUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** End-to-end test of the service on the seeded in-memory database. */
class RecommendationServiceTest {
    private HikariDataSource ds;
    private RecommendationService service;

    @BeforeEach
    void setUp() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:h2:mem:reco_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        cfg.setUsername("sa");
        cfg.setPassword("");
        cfg.setMaximumPoolSize(4);
        ds = new HikariDataSource(cfg);
        DBUtil.init(ds);
        service = new RecommendationService();
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
        ds.close();
    }

    @Test
    void recommendsUnseenItemsWithExplanations() {
        service.refresh();
        List<Recommendation> recs = service.recommend(2);
        assertFalse(recs.isEmpty());
        assertTrue(recs.size() <= service.getSettings().getRecommendationCount());
        Set<Long> seen = new InteractionDaoImpl().findByUser(2).stream()
                .map(Interaction::getItemId).collect(Collectors.toSet());
        for (Recommendation r : recs) {
            assertFalse(seen.contains(r.getItemId()));
            assertFalse(r.getExplanation().isBlank());
        }
    }

    @Test
    void recordInteractionRefreshesRecommendations() throws Exception {
        long target = service.recommend(2).get(0).getItemId();
        Interaction like = new Interaction();
        like.setUserId(2);
        like.setItemId(target);
        like.setType("LIKE");
        service.recordInteraction(like).get(5, TimeUnit.SECONDS);
        assertTrue(service.recommend(2).stream().noneMatch(r -> r.getItemId() == target));
    }

    @Test
    void schedulerStartsAndStopsCleanly() {
        service.start();
        assertFalse(service.recommend(2).isEmpty());
        service.shutdown();
    }
}