package com.recomind.servlet;

import com.recomind.dao.CategoryDao;
import com.recomind.dao.CategoryDaoImpl;
import com.recomind.dao.ItemDao;
import com.recomind.dao.ItemDaoImpl;
import com.recomind.model.Category;
import com.recomind.model.Item;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** GET /api/admin/items */
public class AdminItemsServlet extends ApiServlet {
    private final ItemDao itemDao;
    private final CategoryDao categoryDao;

    public AdminItemsServlet() {
        this(new ItemDaoImpl(), new CategoryDaoImpl());
    }

    public AdminItemsServlet(ItemDao itemDao, CategoryDao categoryDao) {
        this.itemDao = itemDao;
        this.categoryDao = categoryDao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Item> items = itemDao.findAll();
        Map<Long, String> categoryNames = new HashMap<>();
        for (Category c : categoryDao.findAll()) {
            categoryNames.put(c.getId(), c.getName());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Item item : items) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", item.getId());
            map.put("title", item.getTitle());
            map.put("categoryId", item.getCategoryId());
            map.put("categoryName", categoryNames.getOrDefault(item.getCategoryId(), "Unknown"));
            map.put("tags", item.getTags());
            map.put("description", item.getDescription());
            map.put("imageUrl", item.getImageUrl());
            map.put("price", item.getPrice());
            result.add(map);
        }

        ApiResponse.ok(resp, "Items retrieved", result);
    }
}
