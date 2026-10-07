package com.recomind.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @Test
    void loadsProperties() {
        Config config = Config.getInstance();
        assertNotNull(config);
        String url = config.get("db.url");
        assertNotNull(url, "db.url should be defined");
        assertFalse(url.contains("AUTO_SERVER"), "db.url should not contain AUTO_SERVER");
    }
}
