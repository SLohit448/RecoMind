package com.recomind.servlet;

import com.recomind.exception.AuthenticationException;
import com.recomind.exception.DAOException;
import com.recomind.exception.ValidationException;
import com.recomind.filter.CsrfFilter;
import com.recomind.service.AuthService;
import com.recomind.util.SessionKeys;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Base class of all JSON servlets: one central place that maps exceptions to HTTP status codes. */
public abstract class ApiServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ApiServlet.class);

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            super.service(req, resp);
        } catch (ValidationException e) {
            ApiResponse.error(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (AuthenticationException e) {
            ApiResponse.error(resp, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        } catch (DAOException e) {
            LOG.error("Database error on {}", req.getRequestURI(), e);
            ApiResponse.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        } catch (RuntimeException e) {
            LOG.error("Unexpected error on {}", req.getRequestURI(), e);
            ApiResponse.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unexpected server error");
        }
    }

    /** Id of the logged-in user, or AuthenticationException when nobody is logged in. */
    protected static long userId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        Object id = session == null ? null : session.getAttribute(SessionKeys.USER_ID);
        if (!(id instanceof Long)) {
            throw new AuthenticationException("Not logged in");
        }
        return (Long) id;
    }

    /**
     * Starts a fresh session after login or registration. The old session is invalidated first
     * (session fixation protection) and a new CSRF token is issued. Returns that token.
     */
    protected static String startSession(HttpServletRequest req, AuthService.AuthResult result) {
        HttpSession old = req.getSession(false);
        if (old != null) {
            old.invalidate();
        }
        HttpSession session = req.getSession(true);
        String csrf = CsrfFilter.newToken();
        session.setAttribute(SessionKeys.USER_ID, result.userId());
        session.setAttribute(SessionKeys.EMAIL, result.email());
        session.setAttribute(SessionKeys.ROLE, result.role());
        session.setAttribute(SessionKeys.CSRF, csrf);
        session.setMaxInactiveInterval(SessionKeys.SESSION_TIMEOUT_SECONDS);
        return csrf;
    }
}