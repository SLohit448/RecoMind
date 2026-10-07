package com.recomind.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import com.recomind.config.Config;

public class DBUtil {
    private static final Logger logger = LoggerFactory.getLogger(DBUtil.class);
    private static HikariDataSource dataSource;

    /**
     * Initialise the datasource using the application.properties configuration.
     * If the database is empty (no tables), run schema.sql and seed.sql from the classpath.
     */
    public static void init() {
        Properties props = Config.getInstance().getProperties();
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(props.getProperty("db.url"));
        cfg.setUsername(props.getProperty("db.username", ""));
        cfg.setPassword(props.getProperty("db.password", ""));
        cfg.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.poolSize", "10")));
        dataSource = new HikariDataSource(cfg);
        bootstrapIfEmpty();
    }

    /** For tests – initialise with a supplied DataSource */
    public static void init(HikariDataSource ds) {
        dataSource = ds;
        bootstrapIfEmpty();
    }

    private static void bootstrapIfEmpty() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // Simple check: look for a known table 'USERS'
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME='USERS'");
            boolean empty = true;
            if (rs.next()) {
                empty = rs.getInt(1) == 0;
            }
            if (empty) {
                logger.info("Database appears empty – running schema and seed scripts");
                runScript(conn, "/schema.sql");
                runScript(conn, "/seed.sql");
            } else {
                logger.info("Database already initialized");
            }
        } catch (Exception e) {
            logger.error("Error during DB bootstrap", e);
            throw new RuntimeException(e);
        }
    }

    private static void runScript(Connection conn, String resourcePath) throws IOException, java.sql.SQLException {
        try (InputStream is = DBUtil.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                logger.warn("SQL script {} not found on classpath", resourcePath);
                return;
            }
            String sql = new String(is.readAllBytes());
            try (Statement st = conn.createStatement()) {
                for (String part : sql.split(";")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        st.execute(trimmed);
                    }
                }
            }
        }
    }

    public static HikariDataSource getDataSource() {
        return dataSource;
    }
}
