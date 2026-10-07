package com.recomind.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts an embedded Jetty server on port 8080 serving static files from classpath:/static.
 */
public class WebServer {
    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);
    private static final int PORT = 8080;
    private static Server server;

    public static void start() {
        try {
            ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
            handler.setContextPath("/");
            // Locate the static resources on the classpath
            var staticUrl = WebServer.class.getClassLoader().getResource("static");
            if (staticUrl == null) {
                throw new IllegalStateException("static resources not found on classpath");
            }
            handler.setBaseResource(Resource.newResource(staticUrl));
            // DefaultServlet will serve files from the base resource
            handler.addServlet(DefaultServlet.class, "/");

            server = new Server(PORT);
            server.setHandler(handler);
            server.start();
            logger.info("Jetty started on http://localhost:{}", PORT);
        } catch (Exception e) {
            logger.error("Failed to start Jetty", e);
            throw new RuntimeException(e);
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
