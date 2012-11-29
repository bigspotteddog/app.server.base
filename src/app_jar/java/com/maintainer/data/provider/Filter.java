package com.maintainer.data.provider;

public class Filter {
    private final String condition;
    private final Object value;
    private final int weight;
    private final String field;
    private final String op;
    private final String name;
    private final String label;

    public Filter(final String condition, final Object value) {
        this(condition, value, 0, null, null, null, null);
    }

    public Filter(final String condition, final Object value, final int weight, final String field, final String op) {
        this(condition, value, 0, field, op, null, null);
    }

    public Filter(final String condition, final Object value, final int weight, final String field, final String op, final String name, final String label) {
        this.condition = condition;
        this.value = value;
        this.weight = weight;
        this.field = field;
        this.op = op;
        this.name = name;
        this.label = label;
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

    public int getWeight() {
        return weight;
    }

    public String getOperation() {
        final String[] split = condition.split(":");
        if (split.length == 1) {
            return "eq";
        }
        return split[1];
    }

    public String getField2() {
        return field;
    }

    public String getOperator2() {
        return op;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }
}
