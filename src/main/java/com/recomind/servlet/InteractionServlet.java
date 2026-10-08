package com.recomind.servlet;

import com.google.gson.JsonObject;
import com.recomind.dao.CategoryDao;
import com.recomind.dao.CategoryDaoImpl;
import com.recomind.dao.InteractionDao;
import com.recomind.dao.InteractionDaoImpl;
import com.recomind.dao.ItemDao;
import com.recomind.dao.ItemDaoImpl;
import com.recomind.exception.ValidationException;
import com.recomind.model.Category;
import com.recomind.model.Interaction;
import com.recomind.model.Item;
import com.recomind.service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** GET /api/interactions and POST /api/interactions */
public class InteractionServlet extends ApiServlet {
    private final RecommendationService reco;
    private final InteractionDao interactionDao;
    private final ItemDao itemDao;
    private final CategoryDao categoryDao;

    public InteractionServlet(RecommendationService reco) {
        this(reco, new InteractionDaoImpl(), new ItemDaoImpl(), new CategoryDaoImpl());
    }

    public InteractionServlet(RecommendationService reco, InteractionDao interactionDao, ItemDao itemDao, CategoryDao categoryDao) {
        this.reco = reco;
        this.interactionDao = interactionDao;
        this.itemDao = itemDao;
        this.categoryDao = categoryDao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = userId(req);
        List<Interaction> interactions = interactionDao.findByUser(userId);

        Map<Long, Item> itemMap = new HashMap<>();
        for (Item item : itemDao.findAll()) {
            itemMap.put(item.getId(), item);
        }

        Map<Long, String> categoryNames = new HashMap<>();
        for (Category c : categoryDao.findAll()) {
            categoryNames.put(c.getId(), c.getName());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Interaction in : interactions) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", in.getId());
            map.put("userId", in.getUserId());
            map.put("itemId", in.getItemId());
            map.put("type", in.getType());
            map.put("rating", in.getRating());
            map.put("createdAt", in.getCreatedAt() != null ? in.getCreatedAt().toString() : null);
            Item item = itemMap.get(in.getItemId());
            if (item != null) {
                map.put("itemTitle", item.getTitle());
                map.put("categoryName", categoryNames.getOrDefault(item.getCategoryId(), "Unknown"));
            }
            result.add(map);
        }

        ApiResponse.ok(resp, "Interactions retrieved", result);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = userId(req);
        JsonObject body = ApiResponse.readBody(req);

        if (!body.has("itemId") || body.get("itemId").isJsonNull()) {
            throw new ValidationException("itemId is required");
        }
        long itemId;
        try {
            itemId = body.get("itemId").getAsLong();
        } catch (RuntimeException e) {
            throw new ValidationException("itemId must be a number");
        }
        if (itemId <= 0) {
            throw new ValidationException("itemId must be greater than 0");
        }

        String type = ApiResponse.str(body, "type");
        if (type == null || type.isBlank()) {
            throw new ValidationException("type is required");
        }
        type = type.trim().toUpperCase();

        Integer rating = null;
        if (body.has("rating") && !body.get("rating").isJsonNull()) {
            try {
                rating = body.get("rating").getAsInt();
            } catch (RuntimeException e) {
                throw new ValidationException("rating must be an integer");
            }
        }

        Interaction in = new Interaction();
        in.setUserId(userId);
        in.setItemId(itemId);
        in.setType(type);
        in.setRating(rating);
        in.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        if (reco != null) {
            reco.recordInteraction(in);
        } else {
            interactionDao.insert(in);
        }

        ApiResponse.ok(resp, "Interaction recorded", in);
    }
}
