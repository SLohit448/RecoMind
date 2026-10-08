package com.recomind.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recomind.dao.PreferenceDaoImpl;
import com.recomind.exception.AuthenticationException;
import com.recomind.exception.ValidationException;
import com.recomind.filter.AuthenticationFilter;
import com.recomind.filter.CsrfFilter;
import com.recomind.filter.RoleAuthorizationFilter;
import com.recomind.model.UserPreference;
import com.recomind.service.AuthService;
import com.recomind.util.DBUtil;
import com.recomind.util.SessionKeys;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Auth service tests on in-memory H2, and servlet/filter tests with Mockito. */
class SecurityAndAuthTest {
    private HikariDataSource ds;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:h2:mem:auth_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        cfg.setUsername("sa");
        cfg.setPassword("");
        cfg.setMaximumPoolSize(4);
        ds = new HikariDataSource(cfg);
        DBUtil.init(ds);
        auth = new AuthService();
    }

    @AfterEach
    void tearDown() {
        ds.close();
    }

    private static StringWriter capture(HttpServletResponse resp) throws IOException {
        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));
        return sw;
    }

    private static HttpServletRequest request(String method, String uri, HttpSession session) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn(method);
        when(req.getRequestURI()).thenReturn(uri);
        when(req.getSession(false)).thenReturn(session);
        return req;
    }

    private static HttpSession sessionWith(String role, String csrf) {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(2L);
        when(session.getAttribute(SessionKeys.ROLE)).thenReturn(role);
        when(session.getAttribute(SessionKeys.CSRF)).thenReturn(csrf);
        return session;
    }

    // ---------- AuthService ----------

    @Test
    void loginSucceedsForSeededAdmin() {
        AuthService.AuthResult r = auth.login("admin@recomind.com", "Admin@123");
        assertEquals("ADMIN", r.role());
        assertEquals("USER", auth.login("USER@recomind.com", "User@123").role());
    }

    @Test
    void loginFailsWithWrongPasswordOrUnknownEmail() {
        assertThrows(AuthenticationException.class, () -> auth.login("admin@recomind.com", "wrong"));
        assertThrows(AuthenticationException.class, () -> auth.login("nobody@recomind.com", "Admin@123"));
        assertThrows(AuthenticationException.class, () -> auth.login(null, null));
    }

    @Test
    void registerCreatesUserWithPreferencesAndAllowsLogin() {
        AuthService.AuthResult r = auth.register("new.person@test.com", "Passw0rd1", List.of(1L, 2L));
        assertEquals("USER", r.role());
        assertEquals("USER", auth.login("new.person@test.com", "Passw0rd1").role());
        UserPreference p = new PreferenceDaoImpl().findByUserId(r.userId());
        assertNotNull(p);
        assertTrue(p.getPreferenceJson().contains("1"));
    }

    @Test
    void registerRejectsDuplicateEmailAndWeakPasswords() {
        assertThrows(ValidationException.class, () -> auth.register("admin@recomind.com", "Passw0rd1", List.of()));
        assertThrows(ValidationException.class, () -> auth.register("a@test.com", "short1", List.of()));
        assertThrows(ValidationException.class, () -> auth.register("b@test.com", "onlyletters", List.of()));
        assertThrows(ValidationException.class, () -> auth.register("not-an-email", "Passw0rd1", List.of()));
    }

    // ---------- AuthServlet (Mockito) ----------

    @Test
    void loginServletRotatesSessionAndReturnsCsrfToken() throws Exception {
        HttpSession oldSession = mock(HttpSession.class);
        HttpSession newSession = mock(HttpSession.class);
        HttpServletRequest req = request("POST", "/api/auth/login", oldSession);
        when(req.getServletPath()).thenReturn("/api/auth/login");
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(
                "{\"email\":\"admin@recomind.com\",\"password\":\"Admin@123\"}")));
        when(req.getSession(true)).thenReturn(newSession);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        StringWriter out = capture(resp);

        new AuthServlet(auth).service((ServletRequest) req, (ServletResponse) resp);

        verify(oldSession).invalidate(); // session fixation protection
        verify(newSession).setAttribute(SessionKeys.ROLE, "ADMIN");
        verify(resp).setStatus(200);
        assertTrue(out.toString().contains("csrfToken"));
        assertTrue(out.toString().contains("Login successful"));
    }

    @Test
    void loginServletReturns401ForBadCredentials() throws Exception {
        HttpServletRequest req = request("POST", "/api/auth/login", null);
        when(req.getServletPath()).thenReturn("/api/auth/login");
        when(req.getReader()).thenReturn(new BufferedReader(new StringReader(
                "{\"email\":\"admin@recomind.com\",\"password\":\"nope\"}")));
        HttpServletResponse resp = mock(HttpServletResponse.class);
        StringWriter out = capture(resp);

        new AuthServlet(auth).service((ServletRequest) req, (ServletResponse) resp);

        verify(resp).setStatus(401);
        assertTrue(out.toString().contains("Invalid email or password"));
    }

    // ---------- Filters (Mockito) ----------

    @Test
    void authenticationFilterBlocksAnonymousApiCalls() throws Exception {
        HttpServletRequest req = request("GET", "/api/recommendations", null);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        capture(resp);
        FilterChain chain = mock(FilterChain.class);

        new AuthenticationFilter().doFilter(req, resp, chain);

        verify(resp).setStatus(401);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void authenticationFilterRedirectsAnonymousPageRequests() throws Exception {
        HttpServletRequest req = request("GET", "/admin-dashboard.html", null);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        new AuthenticationFilter().doFilter(req, resp, chain);

        verify(resp).sendRedirect("/login.html");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void authenticationFilterLetsLoggedInUsersAndPublicPathsThrough() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        HttpServletRequest loggedIn = request("GET", "/api/recommendations", sessionWith("USER", "t"));
        HttpServletResponse resp = mock(HttpServletResponse.class);
        new AuthenticationFilter().doFilter(loggedIn, resp, chain);
        verify(chain).doFilter(loggedIn, resp);

        FilterChain chain2 = mock(FilterChain.class);
        HttpServletRequest publicPath = request("POST", "/api/auth/login", null);
        new AuthenticationFilter().doFilter(publicPath, resp, chain2);
        verify(chain2).doFilter(publicPath, resp);
    }

    @Test
    void roleFilterBlocksNormalUsersFromAdminApi() throws Exception {
        HttpServletRequest req = request("GET", "/api/admin/users", sessionWith("USER", "t"));
        HttpServletResponse resp = mock(HttpServletResponse.class);
        capture(resp);
        FilterChain chain = mock(FilterChain.class);

        new RoleAuthorizationFilter().doFilter(req, resp, chain);

        verify(resp).setStatus(403);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void roleFilterAllowsAdmins() throws Exception {
        HttpServletRequest req = request("GET", "/api/admin/users", sessionWith("ADMIN", "t"));
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        new RoleAuthorizationFilter().doFilter(req, resp, chain);

        verify(chain).doFilter(req, resp);
    }

    @Test
    void csrfFilterRejectsPostWithoutToken() throws Exception {
        HttpServletRequest req = request("POST", "/api/interactions", sessionWith("USER", "secret"));
        HttpServletResponse resp = mock(HttpServletResponse.class);
        capture(resp);
        FilterChain chain = mock(FilterChain.class);

        new CsrfFilter().doFilter(req, resp, chain);

        verify(resp).setStatus(403);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void csrfFilterAcceptsMatchingToken() throws Exception {
        HttpServletRequest req = request("POST", "/api/interactions", sessionWith("USER", "secret"));
        when(req.getHeader("X-CSRF-Token")).thenReturn("secret");
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        new CsrfFilter().doFilter(req, resp, chain);

        verify(chain).doFilter(req, resp);
    }

    @Test
    void csrfFilterAllowsGetRequests() throws Exception {
        HttpServletRequest req = request("GET", "/api/recommendations", null);
        HttpServletResponse resp = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        new CsrfFilter().doFilter(req, resp, chain);

        verify(chain).doFilter(req, resp);
    }
}