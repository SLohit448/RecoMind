package com.recomind.servlet;

import com.recomind.dao.CategoryDao;
import com.recomind.dao.CategoryDaoImpl;
import com.recomind.dao.ItemDao;
import com.recomind.dao.ItemDaoImpl;
import com.recomind.model.Category;
import com.recomind.model.Item;
import com.recomind.recommender.Recommendation;
import com.recomind.service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** GET /api/recommendations */
public class RecommendationServlet extends ApiServlet {
    private final RecommendationService reco;
    private final ItemDao itemDao;
    private final CategoryDao categoryDao;

    public RecommendationServlet(RecommendationService reco) {
        this(reco, new ItemDaoImpl(), new CategoryDaoImpl());
    }

    public RecommendationServlet(RecommendationService reco, ItemDao itemDao, CategoryDao categoryDao) {
        this.reco = reco;
        this.itemDao = itemDao;
        this.categoryDao = categoryDao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = userId(req);
        List<Recommendation> recommendations = reco.recommend(userId);

        Map<Long, String> categoryNames = new HashMap<>();
        for (Category c : categoryDao.findAll()) {
            categoryNames.put(c.getId(), c.getName());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Recommendation r : recommendations) {
            Item item = itemDao.findById(r.getItemId());
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("itemId", r.getItemId());
            map.put("score", r.getScore());
            map.put("algorithm", r.getAlgorithm());
            map.put("explanation", r.getExplanation());
            if (item != null) {
                map.put("title", item.getTitle());
                map.put("categoryId", item.getCategoryId());
                map.put("categoryName", categoryNames.getOrDefault(item.getCategoryId(), "Unknown"));
                map.put("tags", item.getTags());
                map.put("description", item.getDescription());
                map.put("imageUrl", item.getImageUrl());
                map.put("price", item.getPrice());
            }
            result.add(map);
        }

        ApiResponse.ok(resp, "Recommendations retrieved", result);
    }
}
