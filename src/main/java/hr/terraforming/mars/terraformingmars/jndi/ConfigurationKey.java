package hr.terraforming.mars.terraformingmars.jndi;

import lombok.Getter;

@Getter
public enum ConfigurationKey {

    RMI_PORT("rmi.port"), SERVER_PORT("server.port"), HOSTNAME("hostname"), GEMINI_MODEL("gemini.model");

    private final String key;

    ConfigurationKey(String key) {
        this.key = key;
    }
}