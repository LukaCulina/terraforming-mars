package hr.terraforming.mars.terraformingmars.jndi;

import hr.terraforming.mars.terraformingmars.exception.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigurationReader {
    private static final Properties props = new Properties();
    private static final String CONFIG_FILE = "/application.properties";

    static {
        try (InputStream input = ConfigurationReader.class.getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new ConfigurationException("Configuration file not found in resources: " + CONFIG_FILE);
            }
            props.load(input);
        } catch (IOException e) {
            throw new ConfigurationException("Error loading application configuration!", e);
        }
    }

    private ConfigurationReader() {
    }

    public static String getStringValue(ConfigurationKey key) {
        String value = props.getProperty(key.getKey());
        if (value == null) {
            throw new ConfigurationException("Configuration key '" + key.getKey() + "' does not exist.");
        }
        return value;
    }

    public static Integer getIntegerValue(ConfigurationKey key) {

        try {
            return Integer.valueOf(getStringValue(key));
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Invalid number format for configuration key: '" + key.getKey() + "'", e);
        }
    }
}
