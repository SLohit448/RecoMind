package com.recomind.filter;

import com.recomind.servlet.ApiResponse;
import com.recomind.util.SessionKeys;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Set;

/** Blocks anonymous access: API calls get 401 JSON, page requests are redirected to the login page. */
public class AuthenticationFilter implements Filter {
    private static final Set<String> PUBLIC_PATHS =
            Set.of("/api/auth/login", "/api/auth/register", "/api/csrf");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String uri = req.getRequestURI();
        if (PUBLIC_PATHS.contains(uri)) {
            chain.doFilter(req, resp);
            return;
        }
        HttpSession session = req.getSession(false);
        boolean loggedIn = session != null && session.getAttribute(SessionKeys.USER_ID) != null;
        if (loggedIn) {
            if (!uri.startsWith("/api/")) {
                resp.setHeader("Cache-Control", "no-store"); // no cached dashboards after logout
            }
            chain.doFilter(req, resp);
        } else if (uri.startsWith("/api/")) {
            ApiResponse.error(resp, HttpServletResponse.SC_UNAUTHORIZED, "Please log in");
        } else {
            resp.sendRedirect("/login.html");
        }
    }
}