package com.github.bigspotteddog.util;

import java.io.Serializable;

@SuppressWarnings("serial")
public class JsonString implements Serializable {
    private final String string;

    public JsonString(final String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }
}
