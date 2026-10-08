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

/** Admin-only paths (/api/admin/* and the admin dashboard) require the ADMIN role. */
public class RoleAuthorizationFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);
        Object role = session == null ? null : session.getAttribute(SessionKeys.ROLE);
        if (SessionKeys.ROLE_ADMIN.equals(role)) {
            chain.doFilter(req, resp);
        } else if (req.getRequestURI().startsWith("/api/")) {
            ApiResponse.error(resp, HttpServletResponse.SC_FORBIDDEN, "Administrator access required");
        } else {
            resp.sendRedirect("/403.html");
        }
    }
}