package com.recomind.servlet;

import com.recomind.dao.CategoryDao;
import com.recomind.dao.CategoryDaoImpl;
import com.recomind.model.Category;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/** GET /api/categories */
public class CategoryServlet extends ApiServlet {
    private final CategoryDao categoryDao;

    public CategoryServlet() {
        this(new CategoryDaoImpl());
    }

    public CategoryServlet(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Category> categories = categoryDao.findAll();
        ApiResponse.ok(resp, "Categories retrieved", categories);
    }
}
