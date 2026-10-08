package com.recomind.servlet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.recomind.dao.SettingsDao;
import com.recomind.dao.SettingsDaoImpl;
import com.recomind.service.RecommendationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** GET and POST /api/admin/settings */
public class AdminSettingsServlet extends ApiServlet {
    private final SettingsDao settingsDao;
    private final RecommendationService reco;

    public AdminSettingsServlet(RecommendationService reco) {
        this(reco, new SettingsDaoImpl());
    }

    public AdminSettingsServlet(RecommendationService reco, SettingsDao settingsDao) {
        this.reco = reco;
        this.settingsDao = settingsDao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> settings = settingsDao.getAll();
        ApiResponse.ok(resp, "Settings retrieved", settings);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = ApiResponse.readBody(req);
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : body.entrySet()) {
            if (!entry.getValue().isJsonNull()) {
                map.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        settingsDao.saveAll(map);
        if (reco != null) {
            try {
                reco.refresh();
            } catch (Exception ignored) {
            }
        }
        ApiResponse.ok(resp, "Settings saved", settingsDao.getAll());
    }
}
