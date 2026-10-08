package com.recomind.dao;

import com.recomind.model.Interaction;
import java.util.List;
import java.util.Map;

/** Data access for user-item interactions (views, clicks, likes, ratings, purchases). */
public interface InteractionDao {
    void insert(Interaction interaction);
    List<Interaction> findByUser(long userId);
    List<Interaction> findByItem(long itemId);
    List<Interaction> findAll();
    int countAll();
    /** Interaction count per type, sorted by type name. */
    Map<String, Integer> countByType();
    /** Interaction count per item id. */
    Map<Long, Integer> countByItem();
    /** Interaction count per day (yyyy-MM-dd) for the last N days, sorted by date. */
    Map<String, Integer> dailyCounts(int days);
}