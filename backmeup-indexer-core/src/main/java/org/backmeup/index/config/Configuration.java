package org.backmeup.index.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Configuration {
    private static final String PROPERTYFILE = "backmeup-indexer.properties";

    private static final Properties properties = new Properties();

    static {
        try {
            loadPropertiesFromClasspath();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }

    }

    private static void loadPropertiesFromClasspath() throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = loader.getResourceAsStream(PROPERTYFILE)) {
            properties.load(stream);
        }

        // check if properties were loaded
        if (properties.size() < 1) {
            throw new IOException("unable to load properties file: " + PROPERTYFILE);
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static List<String> getPropertyList(String key) {
        List<String> result = new ArrayList<>();
        if (key != null) {
            String str = getProperty(key);
            if (str != null) {
                result = Arrays.asList(str.split(","));
            }
        }
        return result;
    }
}
