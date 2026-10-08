package com.recomind.dao;

import com.recomind.exception.DAOException;
import com.recomind.model.Interaction;
import com.recomind.service.TransactionManager;
import com.recomind.util.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** JDBC implementation of InteractionDao (PreparedStatement and try-with-resources only). */
public class InteractionDaoImpl implements InteractionDao {
    private static final String SELECT =
            "SELECT ID, USER_ID, ITEM_ID, TYPE, RATING, CREATED_AT FROM INTERACTIONS";

    private Connection getConnection() throws SQLException {
        Connection tx = TransactionManager.getCurrentConnection();
        return tx != null ? tx : DBUtil.getDataSource().getConnection();
    }

    @Override
    public void insert(Interaction i) {
        String sql = "INSERT INTO INTERACTIONS (USER_ID, ITEM_ID, TYPE, RATING, CREATED_AT) VALUES (?,?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, i.getUserId());
            ps.setLong(2, i.getItemId());
            ps.setString(3, i.getType());
            if (i.getRating() == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, i.getRating());
            }
            Timestamp ts = i.getCreatedAt() != null ? i.getCreatedAt() : new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(5, ts);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    i.setId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public List<Interaction> findByUser(long userId) {
        return query(SELECT + " WHERE USER_ID = ? ORDER BY CREATED_AT DESC", userId);
    }

    @Override
    public List<Interaction> findByItem(long itemId) {
        return query(SELECT + " WHERE ITEM_ID = ? ORDER BY CREATED_AT DESC", itemId);
    }

    @Override
    public List<Interaction> findAll() {
        return query(SELECT + " ORDER BY ID", null);
    }

    @Override
    public int countAll() {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM INTERACTIONS");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public Map<String, Integer> countByType() {
        Map<String, Integer> result = new TreeMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT TYPE, COUNT(*) FROM INTERACTIONS GROUP BY TYPE");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString(1), rs.getInt(2));
            }
            return result;
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public Map<Long, Integer> countByItem() {
        Map<Long, Integer> result = new HashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT ITEM_ID, COUNT(*) FROM INTERACTIONS GROUP BY ITEM_ID");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getLong(1), rs.getInt(2));
            }
            return result;
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    @Override
    public Map<String, Integer> dailyCounts(int days) {
        String sql = "SELECT CAST(CREATED_AT AS DATE), COUNT(*) FROM INTERACTIONS WHERE CREATED_AT >= ? "
                + "GROUP BY CAST(CREATED_AT AS DATE) ORDER BY CAST(CREATED_AT AS DATE)";
        Map<String, Integer> result = new TreeMap<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(Instant.now().minus(days, ChronoUnit.DAYS)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString(1), rs.getInt(2));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    private List<Interaction> query(String sql, Long param) {
        List<Interaction> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setLong(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DAOException(e);
        }
    }

    private Interaction map(ResultSet rs) throws SQLException {
        Interaction i = new Interaction();
        i.setId(rs.getLong("ID"));
        i.setUserId(rs.getLong("USER_ID"));
        i.setItemId(rs.getLong("ITEM_ID"));
        i.setType(rs.getString("TYPE"));
        int rating = rs.getInt("RATING");
        i.setRating(rs.wasNull() ? null : rating);
        i.setCreatedAt(rs.getTimestamp("CREATED_AT"));
        return i;
    }
}