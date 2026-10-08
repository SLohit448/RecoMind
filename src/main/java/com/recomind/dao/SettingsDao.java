package com.recomind.dao;

import com.recomind.model.SystemSettings;
import java.util.List;

public interface SettingsDao {
    void create(SystemSettings settings);
    SystemSettings findByKey(String key);
    List<SystemSettings> findAll();
    void update(SystemSettings settings);
    void delete(String key);
}
