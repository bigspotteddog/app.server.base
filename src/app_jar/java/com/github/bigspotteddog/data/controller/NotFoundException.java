package com.github.bigspotteddog.data.controller;

public class NotFoundException extends Exception {
    private static final long serialVersionUID = -8948567707555858446L;

    public NotFoundException() {
        this("Not found.");
    }

    public NotFoundException(String message) {
        super(message);
    }
}
