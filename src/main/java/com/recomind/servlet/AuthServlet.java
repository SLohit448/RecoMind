package com.recomind.servlet;

import com.google.gson.JsonObject;
import com.recomind.service.AuthService;
import com.recomind.util.SessionKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** POST /api/auth/login, /logout, /password and GET /api/auth/me. */
public class AuthServlet extends ApiServlet {
    private final AuthService auth;

    public AuthServlet(AuthService auth) {
        this.auth = auth;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        switch (req.getServletPath()) {
            case "/api/auth/login":
                login(req, resp);
                break;
            case "/api/auth/logout":
                logout(req, resp);
                break;
            case "/api/auth/password":
                changePassword(req, resp);
                break;
            default:
                ApiResponse.error(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown endpoint");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if ("/api/auth/me".equals(req.getServletPath())) {
            long id = userId(req);
            HttpSession session = req.getSession(false);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("userId", id);
            data.put("email", session.getAttribute(SessionKeys.EMAIL));
            data.put("role", session.getAttribute(SessionKeys.ROLE));
            data.put("csrfToken", session.getAttribute(SessionKeys.CSRF));
            ApiResponse.ok(resp, "OK", data);
        } else {
            ApiResponse.error(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown endpoint");
        }
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = ApiResponse.readBody(req);
        AuthService.AuthResult result = auth.login(ApiResponse.str(body, "email"), ApiResponse.str(body, "password"));
        String csrf = startSession(req, result);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("email", result.email());
        data.put("role", result.role());
        data.put("csrfToken", csrf);
        ApiResponse.ok(resp, "Login successful", data);
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        ApiResponse.ok(resp, "Logged out", null);
    }

    private void changePassword(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = ApiResponse.readBody(req);
        auth.changePassword(userId(req), ApiResponse.str(body, "oldPassword"), ApiResponse.str(body, "newPassword"));
        ApiResponse.ok(resp, "Password changed", null);
    }
}