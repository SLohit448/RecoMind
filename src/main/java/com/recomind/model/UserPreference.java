package com.recomind.model;

public class UserPreference {
    private long userId;
    private String preferenceJson;

    public UserPreference() {}

    public UserPreference(long userId, String preferenceJson) {
        this.userId = userId;
        this.preferenceJson = preferenceJson;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getPreferenceJson() {
        return preferenceJson;
    }

    public void setPreferenceJson(String preferenceJson) {
        this.preferenceJson = preferenceJson;
    }
}
