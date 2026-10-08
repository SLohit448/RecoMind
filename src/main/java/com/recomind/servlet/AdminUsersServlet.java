package com.recomind.servlet;

import com.recomind.dao.UserDao;
import com.recomind.dao.UserDaoImpl;
import com.recomind.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** GET /api/admin/users */
public class AdminUsersServlet extends ApiServlet {
    private final UserDao userDao;

    public AdminUsersServlet() {
        this(new UserDaoImpl());
    }

    public AdminUsersServlet(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<User> users = userDao.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("email", u.getEmail());
            map.put("active", u.isActive());
            map.put("roleId", u.getRoleId());
            map.put("role", u.getRoleId() == 1 ? "ADMIN" : "USER");
            result.add(map);
        }
        ApiResponse.ok(resp, "Users retrieved", result);
    }
}
