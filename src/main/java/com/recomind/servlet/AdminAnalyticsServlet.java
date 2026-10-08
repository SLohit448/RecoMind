package com.recomind.servlet;

import com.recomind.dao.CategoryDao;
import com.recomind.dao.CategoryDaoImpl;
import com.recomind.dao.InteractionDao;
import com.recomind.dao.InteractionDaoImpl;
import com.recomind.dao.ItemDao;
import com.recomind.dao.ItemDaoImpl;
import com.recomind.dao.SettingsDao;
import com.recomind.dao.SettingsDaoImpl;
import com.recomind.dao.UserDao;
import com.recomind.dao.UserDaoImpl;
import com.recomind.model.Category;
import com.recomind.model.Interaction;
import com.recomind.model.Item;
import com.recomind.service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** GET /api/admin/analytics */
public class AdminAnalyticsServlet extends ApiServlet {
    private final UserDao userDao;
    private final ItemDao itemDao;
    private final InteractionDao interactionDao;
    private final CategoryDao categoryDao;
    private final SettingsDao settingsDao;
    private final RecommendationService reco;

    public AdminAnalyticsServlet(RecommendationService reco) {
        this(reco, new UserDaoImpl(), new ItemDaoImpl(), new InteractionDaoImpl(),
                new CategoryDaoImpl(), new SettingsDaoImpl());
    }

    public AdminAnalyticsServlet(RecommendationService reco, UserDao userDao, ItemDao itemDao,
                                 InteractionDao interactionDao, CategoryDao categoryDao, SettingsDao settingsDao) {
        this.reco = reco;
        this.userDao = userDao;
        this.itemDao = itemDao;
        this.interactionDao = interactionDao;
        this.categoryDao = categoryDao;
        this.settingsDao = settingsDao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int totalUsers = userDao.findAll().size();
        List<Item> items = itemDao.findAll();
        int totalItems = items.size();
        int totalInteractions = interactionDao.countAll();
        Map<String, Integer> countByType = interactionDao.countByType();
        Map<String, Integer> dailyCounts = interactionDao.dailyCounts(30);
        Map<String, String> settings = settingsDao.getAll();

        Map<Long, Long> itemToCategory = new HashMap<>();
        for (Item item : items) {
            itemToCategory.put(item.getId(), item.getCategoryId());
        }

        Map<Long, String> categoryNames = new HashMap<>();
        for (Category c : categoryDao.findAll()) {
            categoryNames.put(c.getId(), c.getName());
        }

        Map<String, Integer> categoryInteractions = new HashMap<>();
        for (Interaction in : interactionDao.findAll()) {
            Long catId = itemToCategory.get(in.getItemId());
            if (catId != null) {
                String catName = categoryNames.getOrDefault(catId, "Other");
                categoryInteractions.put(catName, categoryInteractions.getOrDefault(catName, 0) + 1);
            }
        }

        List<Map<String, Object>> topCategories = new ArrayList<>();
        categoryInteractions.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(6)
                .forEach(e -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("category", e.getKey());
                    entry.put("count", e.getValue());
                    topCategories.add(entry);
                });

        Map<String, Object> algorithmUsage = new LinkedHashMap<>();
        algorithmUsage.put("activeAlgorithm", settings.getOrDefault("algorithm.active", "HYBRID"));
        algorithmUsage.put("weightContent", settings.getOrDefault("weight.content", "0.4"));
        algorithmUsage.put("weightCollaborative", settings.getOrDefault("weight.collaborative", "0.4"));
        algorithmUsage.put("weightPopularity", settings.getOrDefault("weight.popularity", "0.2"));

        int totalRecommendations = totalUsers * Integer.parseInt(settings.getOrDefault("recommendation.count", "10"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalUsers", totalUsers);
        data.put("totalItems", totalItems);
        data.put("totalInteractions", totalInteractions);
        data.put("totalRecommendations", totalRecommendations);
        data.put("dailyCounts", dailyCounts);
        data.put("countByType", countByType);
        data.put("topCategories", topCategories);
        data.put("algorithmUsage", algorithmUsage);
        data.put("settings", settings);

        ApiResponse.ok(resp, "Analytics retrieved", data);
    }
}
