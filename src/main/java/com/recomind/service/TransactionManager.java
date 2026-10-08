package com.recomind.service;

import com.recomind.exception.DAOException;
import com.recomind.util.DBUtil;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Runs code inside a JDBC transaction (setAutoCommit(false), commit, rollback).
 * DAOs call getCurrentConnection() and join the transaction automatically.
 * The shared connection ignores close(), so DAO try-with-resources blocks are safe.
 */
public class TransactionManager {
    private static final ThreadLocal<Connection> HOLDER = new ThreadLocal<>();

    private TransactionManager() { }

    public static void runInTransaction(Runnable action) {
        callInTransaction(() -> { action.run(); return null; });
    }

    public static <T> T callInTransaction(Supplier<T> action) {
        if (HOLDER.get() != null) {
            return action.get(); // join the transaction that is already open
        }
        try (Connection conn = DBUtil.getDataSource().getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            HOLDER.set(nonClosing(conn));
            try {
                T result = action.get();
                conn.commit();
                return result;
            } catch (RuntimeException | Error e) {
                conn.rollback();
                throw e;
            } finally {
                HOLDER.remove();
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new DAOException("Transaction failed", e);
        }
    }

    /** Connection of the current transaction, or null when none is open. */
    public static Connection getCurrentConnection() {
        return HOLDER.get();
    }

    private static Connection nonClosing(Connection real) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    try {
                        return method.invoke(real, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }
}