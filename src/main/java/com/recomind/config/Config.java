// src/main/java/com/recomind/config/Config.java
package com.recomind.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton configuration loader. Reads {@code application.properties} from the classpath.
 */
public class Config {
    private static final Config INSTANCE = new Config();
    private final Properties properties = new Properties();

    private Config() {
        try (InputStream is = Config.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is == null) {
                throw new IllegalStateException("application.properties not found on classpath");
            }
            properties.load(is);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load configuration", e);
        }
    }

    public static Config getInstance() {
        return INSTANCE;
    }

    public Properties getProperties() {
        return properties;
    }

    public String get(String key) {
        return properties.getProperty(key);
    }
}
