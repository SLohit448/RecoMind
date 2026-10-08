package com.recomind.servlet;

import com.recomind.filter.CsrfFilter;
import com.recomind.util.SessionKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

/** GET /api/csrf returns the token for the current session (the session is created if needed). */
public class CsrfServlet extends ApiServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(true);
        Object existing = session.getAttribute(SessionKeys.CSRF);
        String token = existing instanceof String ? (String) existing : null;
        if (token == null) {
            token = CsrfFilter.newToken();
            session.setAttribute(SessionKeys.CSRF, token);
        }
        ApiResponse.ok(resp, "OK", Map.of("token", token));
    }
}