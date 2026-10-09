package com.recomind.server;

import com.recomind.filter.AuthenticationFilter;
import com.recomind.filter.CharacterEncodingFilter;
import com.recomind.filter.CsrfFilter;
import com.recomind.filter.RoleAuthorizationFilter;
import com.recomind.service.AuthService;
import com.recomind.service.RecommendationService;
import com.recomind.servlet.AdminAnalyticsServlet;
import com.recomind.servlet.AdminItemsServlet;
import com.recomind.servlet.AdminSettingsServlet;
import com.recomind.servlet.AdminUsersServlet;
import com.recomind.servlet.AuthServlet;
import com.recomind.servlet.CategoryServlet;
import com.recomind.servlet.CsrfServlet;
import com.recomind.servlet.InteractionServlet;
import com.recomind.servlet.ItemServlet;
import com.recomind.servlet.PreferenceServlet;
import com.recomind.servlet.RecommendationServlet;
import com.recomind.servlet.RegisterServlet;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServlet;
import java.util.EnumSet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Embedded Jetty: static pages from classpath:/static plus the JSON API servlets and security filters. */
public class WebServer {
    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);
    private static final int PORT =
        Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    private static Server server;

    public static void start(RecommendationService reco) {
        try {
            ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
            handler.setContextPath("/");
            handler.getSessionHandler().setMaxInactiveInterval(com.recomind.util.SessionKeys.SESSION_TIMEOUT_SECONDS);
            handler.getSessionHandler().getSessionCookieConfig().setHttpOnly(true);

            // Filters run in the order they are registered: encoding, authentication, role, CSRF
            EnumSet<DispatcherType> requests = EnumSet.of(DispatcherType.REQUEST);
            FilterHolder auth = new FilterHolder(new AuthenticationFilter());
            FilterHolder role = new FilterHolder(new RoleAuthorizationFilter());
            handler.addFilter(new FilterHolder(new CharacterEncodingFilter()), "/*", requests);
            handler.addFilter(auth, "/api/*", requests);
            handler.addFilter(role, "/api/admin/*", requests);
            handler.addFilter(new FilterHolder(new CsrfFilter()), "/api/*", requests);
            handler.addFilter(auth, "/user-dashboard.html", requests);
            handler.addFilter(auth, "/admin-dashboard.html", requests);
            handler.addFilter(role, "/admin-dashboard.html", requests);
            handler.addFilter(auth, "/dashboard.html", requests);
            handler.addFilter(auth, "/discover.html", requests);
            handler.addFilter(auth, "/interactions.html", requests);
            handler.addFilter(auth, "/preferences.html", requests);
            handler.addFilter(auth, "/profile.html", requests);
            handler.addFilter(auth, "/admin.html", requests);
            handler.addFilter(role, "/admin.html", requests);

            AuthService authService = new AuthService();
            add(handler, new CsrfServlet(), "/api/csrf");
            add(handler, new AuthServlet(authService),
                    "/api/auth/login", "/api/auth/logout", "/api/auth/me", "/api/auth/password");
            add(handler, new RegisterServlet(authService), "/api/auth/register");

            // User API servlets
            add(handler, new RecommendationServlet(reco), "/api/recommendations");
            add(handler, new InteractionServlet(reco), "/api/interactions");
            add(handler, new PreferenceServlet(reco), "/api/preferences");
            add(handler, new ItemServlet(), "/api/items");
            add(handler, new CategoryServlet(), "/api/categories");

            // Admin API servlets
            add(handler, new AdminAnalyticsServlet(reco), "/api/admin/analytics");
            add(handler, new AdminUsersServlet(), "/api/admin/users");
            add(handler, new AdminItemsServlet(), "/api/admin/items");
            add(handler, new AdminSettingsServlet(reco), "/api/admin/settings");

            var staticUrl = WebServer.class.getClassLoader().getResource("static");
            if (staticUrl == null) {
                throw new IllegalStateException("static resources not found on classpath");
            }
            handler.setBaseResource(Resource.newResource(staticUrl));
            handler.setWelcomeFiles(new String[] {"login.html"});
            ServletHolder files = new ServletHolder(DefaultServlet.class);
            files.setInitParameter("dirAllowed", "false");
            handler.addServlet(files, "/");

            server = new Server(PORT);
            server.setHandler(handler);
            server.start();
            logger.info("Jetty started on http://localhost:{}", PORT);
        } catch (Exception e) {
            logger.error("Failed to start Jetty", e);
            throw new RuntimeException(e);
        }
    }

    private static void add(ServletContextHandler handler, HttpServlet servlet, String... paths) {
        ServletHolder holder = new ServletHolder(servlet);
        for (String path : paths) {
            handler.addServlet(holder, path);
        }
    }

    public static void stop() {
        if (server != null) {
            try {
                server.stop();
                logger.info("Jetty stopped");
            } catch (Exception e) {
                logger.warn("Error stopping Jetty", e);
            }
        }
    }
}