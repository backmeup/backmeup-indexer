package org.backmeup.index.config;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public final class Configuration {
	private static final Properties properties = new Properties();

	private static final String PROPERTYFILE = "backmeup-indexer.properties";

	static {

		// check if we're on the Container or within the local Testenvironment
		// on how to load the property file
		try {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			if (loader.getResourceAsStream(PROPERTYFILE) != null) {
				properties.load(loader.getResourceAsStream(PROPERTYFILE));
			} else {
				// seems to be the local JUnit Test Environment
				try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(
						"src/main/resources/" + PROPERTYFILE))) {
				    properties.load(stream);
				}
			}

			// check if finally properties were loaded
			if (properties.size() < 1) {
				throw new IOException("unable to load properties file: "
						+ PROPERTYFILE);
			}
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}

	}

	public static String getProperty(String key) {
		return properties.getProperty(key);
	}

	public static String getProperty(String key, String defaultValue) {
		String value = getProperty(key);
		if (value == null) {
		    return defaultValue;
		} 
	    return value;
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
