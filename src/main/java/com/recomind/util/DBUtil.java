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
import org.h2.tools.RunScript;
import java.io.InputStreamReader;

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
            boolean needBootstrap = false;
            try {
                // Try a simple query on a known table; if it fails, assume DB is empty
                stmt.executeQuery("SELECT 1 FROM USERS LIMIT 1");
            } catch (Exception e) {
                needBootstrap = true;
            }
            if (needBootstrap) {
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
            // Use H2 RunScript to execute the entire script without manual splitting
            RunScript.execute(conn, new InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    public static HikariDataSource getDataSource() {
        return dataSource;
    }
}
