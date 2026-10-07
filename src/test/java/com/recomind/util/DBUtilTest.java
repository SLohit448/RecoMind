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
            // Verify USERS table has rows from seed.sql
            ResultSet rsUsers = stmt.executeQuery("SELECT COUNT(*) FROM USERS");
            assertTrue(rsUsers.next());
            int userCount = rsUsers.getInt(1);
            assertTrue(userCount >= 2, "There should be at least two seeded users");

            // Verify ITEMS table has rows from seed.sql
            ResultSet rsItems = stmt.executeQuery("SELECT COUNT(*) FROM ITEMS");
            assertTrue(rsItems.next());
            int itemCount = rsItems.getInt(1);
            assertTrue(itemCount >= 2, "There should be at least two seeded items");
        }
    }
}
