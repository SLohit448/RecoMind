package com.recomind.dao;

import com.recomind.exception.DAOException;
import com.recomind.service.TransactionManager;
import com.recomind.util.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** JDBC implementation of SettingsDao. */
public class SettingsDaoImpl implements SettingsDao {

    private Connection getConnection() throws SQLException {
        Connection tx = TransactionManager.getCurrentConnection();
        return tx != null ? tx : DBUtil.getDataSource().getConnection();
    }

    @Override
    public Map<String, String> getAll() {
        Map<String, String> result = new LinkedHashMap<>();
        String sql = "SELECT SETTING_KEY, SETTING_VALUE FROM SYSTEM_SETTINGS ORDER BY SETTING_KEY";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString(1), rs.getString(2));
            }
            return result;
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public String get(String key, String defaultValue) {
        String sql = "SELECT SETTING_VALUE FROM SYSTEM_SETTINGS WHERE SETTING_KEY = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : defaultValue;
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public void save(String key, String value) {
        String sql = "MERGE INTO SYSTEM_SETTINGS (SETTING_KEY, SETTING_VALUE) KEY (SETTING_KEY) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public void saveAll(Map<String, String> values) {
        TransactionManager.runInTransaction(() -> values.forEach(this::save));
    }
}