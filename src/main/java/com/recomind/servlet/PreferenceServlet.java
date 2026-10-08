package com.recomind.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.recomind.dao.PreferenceDao;
import com.recomind.dao.PreferenceDaoImpl;
import com.recomind.model.UserPreference;
import com.recomind.service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/** GET /api/preferences and PUT /api/preferences */
public class PreferenceServlet extends ApiServlet {
    private final PreferenceDao preferenceDao;
    private final RecommendationService reco;
    private final Gson gson = new Gson();

    public PreferenceServlet(RecommendationService reco) {
        this(reco, new PreferenceDaoImpl());
    }

    public PreferenceServlet(RecommendationService reco, PreferenceDao preferenceDao) {
        this.reco = reco;
        this.preferenceDao = preferenceDao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = userId(req);
        UserPreference pref = preferenceDao.findByUserId(userId);
        Object data;
        if (pref != null && pref.getPreferenceJson() != null && !pref.getPreferenceJson().isBlank()) {
            try {
                data = gson.fromJson(pref.getPreferenceJson(), Object.class);
            } catch (Exception e) {
                data = Map.of("categories", java.util.List.of());
            }
        } else {
            data = Map.of("categories", java.util.List.of());
        }
        ApiResponse.ok(resp, "Preferences retrieved", data);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = userId(req);
        JsonObject body = ApiResponse.readBody(req);
        String jsonStr = gson.toJson(body);

        UserPreference existing = preferenceDao.findByUserId(userId);
        UserPreference pref = new UserPreference(userId, jsonStr);
        if (existing != null) {
            preferenceDao.update(pref);
        } else {
            preferenceDao.create(pref);
        }

        if (reco != null) {
            try {
                reco.refresh();
            } catch (Exception ignored) {
            }
        }

        Object data = gson.fromJson(jsonStr, Object.class);
        ApiResponse.ok(resp, "Preferences updated", data);
    }
}
