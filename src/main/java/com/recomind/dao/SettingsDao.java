package com.recomind.dao;

import java.util.Map;

/** Key/value access to the SYSTEM_SETTINGS table (algorithm weights, N, thresholds...). */
public interface SettingsDao {
    Map<String, String> getAll();
    String get(String key, String defaultValue);
    void save(String key, String value);
    /** Saves all values; joins an open transaction or runs in its own. */
    void saveAll(Map<String, String> values);
}