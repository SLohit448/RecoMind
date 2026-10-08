package com.recomind.dao;

import com.recomind.model.Item;
import java.util.List;

public interface ItemDao {
    void create(Item item);
    Item findById(long id);
    List<Item> findAll();
    void update(Item item);
    void delete(long id);
}
