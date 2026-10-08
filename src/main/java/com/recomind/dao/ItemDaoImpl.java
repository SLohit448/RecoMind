package com.recomind.dao;

import com.recomind.exception.DAOException;
import com.recomind.model.Item;
import com.recomind.service.TransactionManager;
import com.recomind.util.DBUtil;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ItemDaoImpl implements ItemDao {
    private final HikariDataSource dataSource = DBUtil.getDataSource();

    private Connection getConnection() throws SQLException {
        Connection txConn = TransactionManager.getCurrentConnection();
        return (txConn != null) ? txConn : dataSource.getConnection();
    }

    @Override
    public void create(Item item) {
        String sql = "INSERT INTO ITEMS (TITLE, CATEGORY_ID, TAGS, DESCRIPTION, IMAGE_URL, PRICE) VALUES (?,?,?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getTitle());
            ps.setLong(2, item.getCategoryId());
            ps.setString(3, item.getTags());
            ps.setString(4, item.getDescription());
            ps.setString(5, item.getImageUrl());
            ps.setDouble(6, item.getPrice());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    item.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public Item findById(long id) {
        String sql = "SELECT ID, TITLE, CATEGORY_ID, TAGS, DESCRIPTION, IMAGE_URL, PRICE FROM ITEMS WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Item i = new Item();
                    i.setId(rs.getLong("ID"));
                    i.setTitle(rs.getString("TITLE"));
                    i.setCategoryId(rs.getLong("CATEGORY_ID"));
                    i.setTags(rs.getString("TAGS"));
                    i.setDescription(rs.getString("DESCRIPTION"));
                    i.setImageUrl(rs.getString("IMAGE_URL"));
                    i.setPrice(rs.getDouble("PRICE"));
                    return i;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public List<Item> findAll() {
        String sql = "SELECT ID, TITLE, CATEGORY_ID, TAGS, DESCRIPTION, IMAGE_URL, PRICE FROM ITEMS";
        List<Item> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Item i = new Item();
                i.setId(rs.getLong("ID"));
                i.setTitle(rs.getString("TITLE"));
                i.setCategoryId(rs.getLong("CATEGORY_ID"));
                i.setTags(rs.getString("TAGS"));
                i.setDescription(rs.getString("DESCRIPTION"));
                i.setImageUrl(rs.getString("IMAGE_URL"));
                i.setPrice(rs.getDouble("PRICE"));
                list.add(i);
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public void update(Item item) {
        String sql = "UPDATE ITEMS SET TITLE = ?, CATEGORY_ID = ?, TAGS = ?, DESCRIPTION = ?, IMAGE_URL = ?, PRICE = ? WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getTitle());
            ps.setLong(2, item.getCategoryId());
            ps.setString(3, item.getTags());
            ps.setString(4, item.getDescription());
            ps.setString(5, item.getImageUrl());
            ps.setDouble(6, item.getPrice());
            ps.setLong(7, item.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "DELETE FROM ITEMS WHERE ID = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }
}
