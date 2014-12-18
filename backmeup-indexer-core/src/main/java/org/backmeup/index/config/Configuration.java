package org.backmeup.index.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.SystemUtils;

public class Configuration {
    private static final String PROPERTYFILE_WINDOWS = "backmeup-indexer_windows.properties";
    private static final String PROPERTYFILE_LINUX = "backmeup-indexer_linux.properties";

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
        if (SystemUtils.IS_OS_LINUX) {
            try (InputStream stream = loader.getResourceAsStream(PROPERTYFILE_LINUX)) {
                properties.load(stream);
            }
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            try (InputStream stream = loader.getResourceAsStream(PROPERTYFILE_WINDOWS)) {
                properties.load(stream);
            }
        }

        // check if properties were loaded
        if (properties.size() < 1) {
            if (SystemUtils.IS_OS_LINUX) {
                throw new IOException("unable to load properties file: " + PROPERTYFILE_LINUX);
            }
            if (SystemUtils.IS_OS_WINDOWS) {
                throw new IOException("unable to load properties file: " + PROPERTYFILE_WINDOWS);
            }
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
