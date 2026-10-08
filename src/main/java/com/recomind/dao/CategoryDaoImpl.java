package com.recomind.dao;

import com.recomind.exception.DAOException;
import com.recomind.model.Category;
import com.recomind.service.TransactionManager;
import com.recomind.util.DBUtil;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CategoryDaoImpl implements CategoryDao {
    private final HikariDataSource dataSource = DBUtil.getDataSource();

    private Connection getConnection() throws SQLException {
        Connection txConn = TransactionManager.getCurrentConnection();
        return (txConn != null) ? txConn : dataSource.getConnection();
    }

    @Override
    public void create(Category category) {
        String sql = "INSERT INTO CATEGORIES (NAME) VALUES (?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, category.getName());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    category.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public Category findById(long id) {
        String sql = "SELECT ID, NAME FROM CATEGORIES WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Category c = new Category();
                    c.setId(rs.getLong("ID"));
                    c.setName(rs.getString("NAME"));
                    return c;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public List<Category> findAll() {
        String sql = "SELECT ID, NAME FROM CATEGORIES";
        List<Category> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Category c = new Category();
                c.setId(rs.getLong("ID"));
                c.setName(rs.getString("NAME"));
                list.add(c);
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public void update(Category category) {
        String sql = "UPDATE CATEGORIES SET NAME = ? WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setLong(2, category.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM CATEGORIES WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }
}
