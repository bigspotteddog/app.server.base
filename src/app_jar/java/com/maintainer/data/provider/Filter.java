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

    public String getField() {
        return condition.split(":")[0];
    }

    public Object getValue() {
        return value;
    }

    public String getOperation() {
        final String[] split = condition.split(":");
        if (split.length == 1) {
            return "eq";
        }
        return split[1];
    }
}
