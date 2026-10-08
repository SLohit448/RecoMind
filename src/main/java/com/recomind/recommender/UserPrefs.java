package com.recomind.recommender;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

/** Stated user preferences, stored as JSON in USER_PREFERENCES.PREFERENCE_JSON. */
public class UserPrefs {
    private static final Gson GSON = new Gson();

    private List<Long> categories = new ArrayList<>();
    private List<String> tags = new ArrayList<>();
    private Double minPrice;
    private Double maxPrice;
    private String contentType;

    public static UserPrefs fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new UserPrefs();
        }
        try {
            UserPrefs prefs = GSON.fromJson(json, UserPrefs.class);
            return prefs == null ? new UserPrefs() : prefs.normalized();
        } catch (RuntimeException e) {
            return new UserPrefs();
        }
    }

    private UserPrefs normalized() {
        if (categories == null) { categories = new ArrayList<>(); }
        if (tags == null) { tags = new ArrayList<>(); }
        return this;
    }

    public String toJson() { return GSON.toJson(this); }

    public boolean isEmpty() {
        return categories.isEmpty() && tags.isEmpty() && minPrice == null && maxPrice == null;
    }

    public List<Long> getCategories() { return categories; }
    public List<String> getTags() { return tags; }
    public Double getMinPrice() { return minPrice; }
    public Double getMaxPrice() { return maxPrice; }
    public String getContentType() { return contentType; }
}