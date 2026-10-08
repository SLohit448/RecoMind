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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/** Every POST/PUT/DELETE must send the session CSRF token in the X-CSRF-Token header. */
public class CsrfFilter implements Filter {
    public static final String HEADER = "X-CSRF-Token";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        if (SAFE_METHODS.contains(req.getMethod())) {
            chain.doFilter(req, resp);
            return;
        }
        HttpSession session = req.getSession(false);
        Object expected = session == null ? null : session.getAttribute(SessionKeys.CSRF);
        String actual = req.getHeader(HEADER);
        if (expected instanceof String && actual != null
                && MessageDigest.isEqual(((String) expected).getBytes(StandardCharsets.UTF_8),
                        actual.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(req, resp);
        } else {
            ApiResponse.error(resp, HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token");
        }
    }
}