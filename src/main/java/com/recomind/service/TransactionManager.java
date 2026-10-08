package com.recomind.service;

import com.recomind.util.DBUtil;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Helper for executing code within a transaction.
 * Usage: TransactionManager.runInTransaction(() -> { // transactional code });
 */
public class TransactionManager {
    private static final HikariDataSource dataSource = DBUtil.getDataSource();
    private static final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    public static void runInTransaction(Runnable action) {
        try (Connection conn = dataSource.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                connectionHolder.set(conn);
                action.run();
                conn.commit();
            } catch (RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                connectionHolder.remove();
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /** Retrieve the connection associated with the current transaction, or null if none */
    public static Connection getCurrentConnection() {
        return connectionHolder.get();
    }
}
