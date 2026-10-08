package com.recomind.dao;

import com.recomind.exception.DAOException;
import com.recomind.model.UserPreference;
import com.recomind.service.TransactionManager;
import com.recomind.util.DBUtil;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PreferenceDaoImpl implements PreferenceDao {
    private final HikariDataSource dataSource = DBUtil.getDataSource();

    private Connection getConnection() throws SQLException {
        Connection txConn = TransactionManager.getCurrentConnection();
        return (txConn != null) ? txConn : dataSource.getConnection();
    }

    @Override
    public void create(UserPreference pref) {
        String sql = "INSERT INTO USER_PREFERENCES (USER_ID, PREFERENCE_JSON) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, pref.getUserId());
            ps.setString(2, pref.getPreferenceJson());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public UserPreference findByUserId(long userId) {
        String sql = "SELECT USER_ID, PREFERENCE_JSON FROM USER_PREFERENCES WHERE USER_ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UserPreference pref = new UserPreference();
                    pref.setUserId(rs.getLong("USER_ID"));
                    pref.setPreferenceJson(rs.getString("PREFERENCE_JSON"));
                    return pref;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public List<UserPreference> findAll() {
        String sql = "SELECT USER_ID, PREFERENCE_JSON FROM USER_PREFERENCES";
        List<UserPreference> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UserPreference pref = new UserPreference();
                pref.setUserId(rs.getLong("USER_ID"));
                pref.setPreferenceJson(rs.getString("PREFERENCE_JSON"));
                list.add(pref);
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public void update(UserPreference pref) {
        String sql = "UPDATE USER_PREFERENCES SET PREFERENCE_JSON = ? WHERE USER_ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pref.getPreferenceJson());
            ps.setLong(2, pref.getUserId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public void delete(long userId) {
        String sql = "DELETE FROM USER_PREFERENCES WHERE USER_ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }
}
