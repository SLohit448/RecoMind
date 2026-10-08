package com.recomind;

import com.recomind.server.WebServer;
import com.recomind.service.RecommendationService;
import com.recomind.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Application entry point: database, recommendation service (background threads), web server. */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("Starting RecoMind application...");
        DBUtil.init();
        RecommendationService reco = new RecommendationService();
        reco.start();
        WebServer.start(reco);
        // Clean shutdown of Jetty and the background executors when the JVM stops
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            WebServer.stop();
            reco.shutdown();
        }, "shutdown-hook"));
    }
}