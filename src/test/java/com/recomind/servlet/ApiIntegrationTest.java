package com.recomind.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.recomind.dao.CategoryDaoImpl;
import com.recomind.dao.InteractionDaoImpl;
import com.recomind.dao.ItemDaoImpl;
import com.recomind.dao.PreferenceDaoImpl;
import com.recomind.dao.SettingsDaoImpl;
import com.recomind.dao.UserDaoImpl;
import com.recomind.service.RecommendationService;
import com.recomind.util.DBUtil;
import com.recomind.util.SessionKeys;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiIntegrationTest {
    private HikariDataSource ds;
    private RecommendationService reco;

    @BeforeEach
    void setUp() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:h2:mem:api_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        cfg.setUsername("sa");
        cfg.setPassword("");
        cfg.setMaximumPoolSize(4);
        ds = new HikariDataSource(cfg);
        DBUtil.init(ds);
        reco = new RecommendationService();
    }

    @AfterEach
    void tearDown() {
        reco.shutdown();
        ds.close();
    }

    private static StringWriter capture(HttpServletResponse resp) throws IOException {
        StringWriter sw = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(sw));
        return sw;
    }

    private static HttpServletRequest request(String method, String uri, long userId, String role) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn(method);
        when(req.getRequestURI()).thenReturn(uri);
        when(req.getServletPath()).thenReturn(uri);
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(SessionKeys.USER_ID)).thenReturn(userId);
        when(session.getAttribute(SessionKeys.ROLE)).thenReturn(role);
        when(req.getSession(false)).thenReturn(session);
        return req;
    }

    @Test
    void testRecommendationServlet() throws Exception {
        RecommendationServlet servlet = new RecommendationServlet(reco, new ItemDaoImpl(), new CategoryDaoImpl());
        HttpServletRequest req = request("GET", "/api/recommendations", 2L, "USER");
        HttpServletResponse resp = mock(HttpServletResponse.class);
        StringWriter out = capture(resp);

        servlet.service((ServletRequest) req, (ServletResponse) resp);

        verifyStatus(resp, 200);
        assertTrue(out.toString().contains("Recommendations retrieved"));
        assertTrue(out.toString().contains("\"success\":true"));
    }

    @Test
    void testInteractionServletGetAndPost() throws Exception {
        InteractionServlet servlet = new InteractionServlet(reco, new InteractionDaoImpl(), new ItemDaoImpl(), new CategoryDaoImpl());

        // POST interaction
        HttpServletRequest postReq = request("POST", "/api/interactions", 2L, "USER");
        when(postReq.getReader()).thenReturn(new BufferedReader(new StringReader("{\"itemId\":1,\"type\":\"LIKE\"}")));
        HttpServletResponse postResp = mock(HttpServletResponse.class);
        StringWriter postOut = capture(postResp);

        servlet.service((ServletRequest) postReq, (ServletResponse) postResp);
        verifyStatus(postResp, 200);
        assertTrue(postOut.toString().contains("Interaction recorded"));

        // GET interactions
        HttpServletRequest getReq = request("GET", "/api/interactions", 2L, "USER");
        HttpServletResponse getResp = mock(HttpServletResponse.class);
        StringWriter getOut = capture(getResp);

        servlet.service((ServletRequest) getReq, (ServletResponse) getResp);
        verifyStatus(getResp, 200);
        assertTrue(getOut.toString().contains("Interactions retrieved"));
    }

    @Test
    void testPreferenceServletGetAndPut() throws Exception {
        PreferenceServlet servlet = new PreferenceServlet(reco, new PreferenceDaoImpl());

        // PUT preferences
        HttpServletRequest putReq = request("PUT", "/api/preferences", 2L, "USER");
        when(putReq.getReader()).thenReturn(new BufferedReader(new StringReader("{\"categories\":[1,2,3]}")));
        HttpServletResponse putResp = mock(HttpServletResponse.class);
        StringWriter putOut = capture(putResp);

        servlet.service((ServletRequest) putReq, (ServletResponse) putResp);
        verifyStatus(putResp, 200);
        assertTrue(putOut.toString().contains("Preferences updated"));

        // GET preferences
        HttpServletRequest getReq = request("GET", "/api/preferences", 2L, "USER");
        HttpServletResponse getResp = mock(HttpServletResponse.class);
        StringWriter getOut = capture(getResp);

        servlet.service((ServletRequest) getReq, (ServletResponse) getResp);
        verifyStatus(getResp, 200);
        assertTrue(getOut.toString().contains("Preferences retrieved"));
        assertTrue(getOut.toString().contains("1"));
    }

    @Test
    void testItemAndCategoryServlets() throws Exception {
        ItemServlet itemServlet = new ItemServlet(new ItemDaoImpl(), new CategoryDaoImpl());
        HttpServletRequest itemReq = request("GET", "/api/items", 2L, "USER");
        HttpServletResponse itemResp = mock(HttpServletResponse.class);
        StringWriter itemOut = capture(itemResp);

        itemServlet.service((ServletRequest) itemReq, (ServletResponse) itemResp);
        verifyStatus(itemResp, 200);
        assertTrue(itemOut.toString().contains("Items retrieved"));

        CategoryServlet catServlet = new CategoryServlet(new CategoryDaoImpl());
        HttpServletRequest catReq = request("GET", "/api/categories", 2L, "USER");
        HttpServletResponse catResp = mock(HttpServletResponse.class);
        StringWriter catOut = capture(catResp);

        catServlet.service((ServletRequest) catReq, (ServletResponse) catResp);
        verifyStatus(catResp, 200);
        assertTrue(catOut.toString().contains("Categories retrieved"));
    }

    @Test
    void testAdminAnalyticsReturnsRealValues() throws Exception {
        AdminAnalyticsServlet analyticsServlet = new AdminAnalyticsServlet(reco, new UserDaoImpl(),
                new ItemDaoImpl(), new InteractionDaoImpl(), new CategoryDaoImpl(), new SettingsDaoImpl());
        HttpServletRequest analyticsReq = request("GET", "/api/admin/analytics", 1L, "ADMIN");
        HttpServletResponse analyticsResp = mock(HttpServletResponse.class);
        StringWriter analyticsOut = capture(analyticsResp);

        analyticsServlet.service((ServletRequest) analyticsReq, (ServletResponse) analyticsResp);
        verifyStatus(analyticsResp, 200);
        String json = analyticsOut.toString();
        assertTrue(json.contains("\"totalUsers\":12"));
        assertTrue(json.contains("\"totalItems\":60"));
        assertTrue(json.contains("\"totalInteractions\":600"));
        assertTrue(json.contains("dailyCounts"));
        assertTrue(json.contains("countByType"));
        assertTrue(json.contains("topCategories"));
    }

    @Test
    void testAdminUsersAndItemsServlets() throws Exception {
        AdminUsersServlet usersServlet = new AdminUsersServlet(new UserDaoImpl());
        HttpServletRequest usersReq = request("GET", "/api/admin/users", 1L, "ADMIN");
        HttpServletResponse usersResp = mock(HttpServletResponse.class);
        StringWriter usersOut = capture(usersResp);

        usersServlet.service((ServletRequest) usersReq, (ServletResponse) usersResp);
        verifyStatus(usersResp, 200);
        assertTrue(usersOut.toString().contains("admin@recomind.com"));

        AdminItemsServlet itemsServlet = new AdminItemsServlet(new ItemDaoImpl(), new CategoryDaoImpl());
        HttpServletRequest itemsReq = request("GET", "/api/admin/items", 1L, "ADMIN");
        HttpServletResponse itemsResp = mock(HttpServletResponse.class);
        StringWriter itemsOut = capture(itemsResp);

        itemsServlet.service((ServletRequest) itemsReq, (ServletResponse) itemsResp);
        verifyStatus(itemsResp, 200);
        assertTrue(itemsOut.toString().contains("Items retrieved"));
    }

    @Test
    void testAdminSettingsServletGetAndPost() throws Exception {
        AdminSettingsServlet settingsServlet = new AdminSettingsServlet(reco, new SettingsDaoImpl());

        // GET settings
        HttpServletRequest getReq = request("GET", "/api/admin/settings", 1L, "ADMIN");
        HttpServletResponse getResp = mock(HttpServletResponse.class);
        StringWriter getOut = capture(getResp);

        settingsServlet.service((ServletRequest) getReq, (ServletResponse) getResp);
        verifyStatus(getResp, 200);
        assertTrue(getOut.toString().contains("weight.content"));

        // POST settings
        HttpServletRequest postReq = request("POST", "/api/admin/settings", 1L, "ADMIN");
        when(postReq.getReader()).thenReturn(new BufferedReader(new StringReader("{\"weight.content\":\"0.5\",\"weight.collaborative\":\"0.3\",\"weight.popularity\":\"0.2\"}")));
        HttpServletResponse postResp = mock(HttpServletResponse.class);
        StringWriter postOut = capture(postResp);

        settingsServlet.service((ServletRequest) postReq, (ServletResponse) postResp);
        verifyStatus(postResp, 200);
        assertTrue(postOut.toString().contains("Settings saved"));
    }

    private static void verifyStatus(HttpServletResponse resp, int status) {
        org.mockito.Mockito.verify(resp).setStatus(status);
    }
}
