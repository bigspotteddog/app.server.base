package com.maintainer.data.provider;

public class Filter {
    private final String condition;
    private final Object value;

    public Filter(final String condition, final Object value) {
        this.condition = condition;
        this.value = value;
    }

    public String getCondition() {
        return condition;
    }

    public Object getValue() {
        return value;
    }
}
