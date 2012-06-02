package com.maintainer.data.provider;

public class DefaultDataProviderInitializationException extends RuntimeException {
    private static final long serialVersionUID = 7037263192105749510L;

    public DefaultDataProviderInitializationException() {
        super("The default data provider has not been initalized.");
    }

    public DefaultDataProviderInitializationException(Exception e) {
        super(e);
    }
}
