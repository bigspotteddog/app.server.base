package com.github.bigspotteddog.data.provider;

public class InvalidDatabaseConfiguration extends RuntimeException {
    private static final long serialVersionUID = -6932097831408193100L;

    public InvalidDatabaseConfiguration(String message) {
        super(message);
    }
}
