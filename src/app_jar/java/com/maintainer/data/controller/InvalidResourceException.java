package com.maintainer.data.controller;

public class InvalidResourceException extends Exception {
    private static final long serialVersionUID = -6020891968427839200L;

    public InvalidResourceException() {
        this("Invalid resource.");
    }

    public InvalidResourceException(String message) {
        super(message);
    }
}
