// UserDao interface
package com.recomind.dao;

import com.recomind.model.User;
import java.util.List;

public interface UserDao {
    void create(User user);
    User findById(long id);
    User findByEmail(String email);
    List<User> findAll();
    void update(User user);
    void delete(long id);
}
