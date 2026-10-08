package com.recomind.dao;

import com.recomind.model.Category;
import java.util.List;

public interface CategoryDao {
    void create(Category category);
    Category findById(long id);
    List<Category> findAll();
    void update(Category category);
    void delete(long id);
}
