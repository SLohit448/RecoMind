package com.recomind.dao;

import com.recomind.exception.DAOException;
import com.recomind.model.User;
import com.recomind.util.DBUtil;
import com.recomind.service.TransactionManager;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDaoImpl implements UserDao {
    private final HikariDataSource dataSource = DBUtil.getDataSource();

    private Connection getConnection() throws SQLException {
        Connection txConn = TransactionManager.getCurrentConnection();
        return (txConn != null) ? txConn : dataSource.getConnection();
    }

    @Override
    public void create(User user) {
        String sql = "INSERT INTO USERS (EMAIL, PASSWORD_HASH, ACTIVE, ROLE_ID) VALUES (?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getPasswordHash());
            ps.setBoolean(3, user.isActive());
            ps.setLong(4, user.getRoleId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public User findById(long id) {
        String sql = "SELECT ID, EMAIL, PASSWORD_HASH, ACTIVE, ROLE_ID FROM USERS WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setId(rs.getLong("ID"));
                    u.setEmail(rs.getString("EMAIL"));
                    u.setPasswordHash(rs.getString("PASSWORD_HASH"));
                    u.setActive(rs.getBoolean("ACTIVE"));
                    u.setRoleId(rs.getLong("ROLE_ID"));
                    return u;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public User findByEmail(String email) {
        String sql = "SELECT ID, EMAIL, PASSWORD_HASH, ACTIVE, ROLE_ID FROM USERS WHERE EMAIL = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setId(rs.getLong("ID"));
                    u.setEmail(rs.getString("EMAIL"));
                    u.setPasswordHash(rs.getString("PASSWORD_HASH"));
                    u.setActive(rs.getBoolean("ACTIVE"));
                    u.setRoleId(rs.getLong("ROLE_ID"));
                    return u;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT ID, EMAIL, PASSWORD_HASH, ACTIVE, ROLE_ID FROM USERS";
        List<User> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getLong("ID"));
                u.setEmail(rs.getString("EMAIL"));
                u.setPasswordHash(rs.getString("PASSWORD_HASH"));
                u.setActive(rs.getBoolean("ACTIVE"));
                u.setRoleId(rs.getLong("ROLE_ID"));
                list.add(u);
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public void update(User user) {
        String sql = "UPDATE USERS SET EMAIL = ?, PASSWORD_HASH = ?, ACTIVE = ?, ROLE_ID = ? WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getPasswordHash());
            ps.setBoolean(3, user.isActive());
            ps.setLong(4, user.getRoleId());
            ps.setLong(5, user.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM USERS WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }
}
