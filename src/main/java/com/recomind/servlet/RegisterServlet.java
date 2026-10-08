package com.recomind.servlet;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.recomind.exception.ValidationException;
import com.recomind.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** POST /api/auth/register: creates the account (with optional onboarding categories) and logs the user in. */
public class RegisterServlet extends ApiServlet {
    private final AuthService auth;

    public RegisterServlet(AuthService auth) {
        this.auth = auth;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JsonObject body = ApiResponse.readBody(req);
        List<Long> categories = new ArrayList<>();
        if (body.has("categories") && body.get("categories").isJsonArray()) {
            JsonArray array = body.getAsJsonArray("categories");
            for (JsonElement e : array) {
                try {
                    categories.add(e.getAsLong());
                } catch (RuntimeException ex) {
                    throw new ValidationException("categories must be numbers");
                }
            }
        }
        AuthService.AuthResult result = auth.register(
                ApiResponse.str(body, "email"), ApiResponse.str(body, "password"), categories);
        String csrf = startSession(req, result);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("email", result.email());
        data.put("role", result.role());
        data.put("csrfToken", csrf);
        ApiResponse.send(resp, HttpServletResponse.SC_CREATED, true, "Account created", data);
    }
}