package com.maintainer.data.controller;

import java.io.Serializable;
import java.util.Collection;

@SuppressWarnings("serial")
public class ErrorResponse implements Serializable {

    private final Collection<String> errors;
    private final Object response;

    public ErrorResponse(final Collection<String> errors, final Object response) {
        this.errors = errors;
        this.response = response;
    }
}
