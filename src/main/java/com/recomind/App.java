package com.recomind;

import com.recomind.server.WebServer;
import com.recomind.util.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        logger.info("Starting RecoMind application...");
        // Initialise DB (creates schema if needed)
        DBUtil.init();
        // Start embedded Jetty server
        WebServer.start();
    }
}
