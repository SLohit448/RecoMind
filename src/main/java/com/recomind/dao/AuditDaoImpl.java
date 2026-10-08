package com.recomind.dao;

import com.recomind.exception.DAOException;
import com.recomind.model.AuditLog;
import com.recomind.service.TransactionManager;
import com.recomind.util.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** JDBC implementation of AuditDao. */
public class AuditDaoImpl implements AuditDao {

    private Connection getConnection() throws SQLException {
        Connection tx = TransactionManager.getCurrentConnection();
        return tx != null ? tx : DBUtil.getDataSource().getConnection();
    }

    @Override
    public void insert(AuditLog entry) {
        String sql = "INSERT INTO AUDIT_LOG (USER_ID, ACTION, DETAILS) VALUES (?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (entry.getUserId() <= 0) {
                ps.setNull(1, Types.BIGINT);
            } else {
                ps.setLong(1, entry.getUserId());
            }
            ps.setString(2, entry.getAction());
            ps.setString(3, entry.getDetails());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public List<AuditLog> findRecent(int limit) {
        String sql = "SELECT ID, USER_ID, ACTION, \"TIMESTAMP\", DETAILS FROM AUDIT_LOG ORDER BY ID DESC LIMIT ?";
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditLog a = new AuditLog();
                    a.setId(rs.getLong(1));
                    a.setUserId(rs.getLong(2));
                    a.setAction(rs.getString(3));
                    a.setTimestamp(rs.getTimestamp(4));
                    a.setDetails(rs.getString(5));
                    list.add(a);
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }
}