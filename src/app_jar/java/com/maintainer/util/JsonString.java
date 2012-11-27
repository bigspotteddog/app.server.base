package com.maintainer.util;

import java.io.Serializable;

public class JsonString implements Serializable {
    private final String string;

    public JsonString(final String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }
}
