package com.maintainer.data.controller;

import com.maintainer.util.Utils;

public class Resource {
    private static final Long ID_NOT_PROVIDED = Long.MIN_VALUE;

    private final String resource;
    private String property;

    public Resource(String resource) {
        this.resource = resource;
    }

    public Resource(String resource, String property) {
        this.resource = resource;
        this.property = property;
    }

    public String getResource() {
        return resource;
    }

    public boolean isProperty() {
        return property == null;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getProperty() {
        return property;
    }

    public boolean isId() {
        return property != null && Utils.isNumeric(property);
    }

    public Long getId() {
        if (!isId()) return ID_NOT_PROVIDED;
        return Long.parseLong(property);
    }

    public String getPath() {
        return resource + (property != null?"/" + property:"");
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        return getPath().equals(other.toString());
    }

    @Override
    public String toString() {
        return getPath();
    }
}
