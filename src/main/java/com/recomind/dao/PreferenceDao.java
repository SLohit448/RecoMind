package com.recomind.dao;

import com.recomind.model.UserPreference;
import java.util.List;

public interface PreferenceDao {
    void create(UserPreference pref);
    UserPreference findByUserId(long userId);
    List<UserPreference> findAll();
    void update(UserPreference pref);
    void delete(long userId);
}
