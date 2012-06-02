package com.maintainer.data.provider;

import java.util.LinkedHashMap;

public class Query extends LinkedHashMap<String, Object> {
    private static final long serialVersionUID = -8095776860044348098L;

    public static final String NE = ":ne";
    public static final String GT = ":gt";
    public static final String GE = ":ge";
    public static final String LT = ":lt";
    public static final String LE = ":le";
    public static final String GT_ = ">";
    public static final String GE_ = ">=";
    public static final String LT_ = "<";
    public static final String LE_ = "<=";

    public static final String ORDER = ":order";
    public static final String OFFSET = ":offset";
    public static final String LIMIT = ":limit";

    private Class<?> kind;
    private String order;
    private int offset;
    private int limit;

    private Key key;

    public Query(Class<?> kind) {
        this.kind = kind;
    }

    public Query filter(String condition, Object value) {
        this.put(condition, value);
        return this;
    }

    public Query setOrder(String order) {
        this.order = order;
        return this;
    }

    public Class<?> getKind() {
        return kind;
    }

    public void setKind(Class<?> kind) {
        this.kind = kind;
    }

    public String getOrder() {
        return order;
    }

    public Query setOffset(int offset) {
        this.offset = offset;
        return this;
    }

    public int getOffset() {
        return offset;
    }

    public Query setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public boolean isOrdered() {
        return order != null;
    }

    public String getKindName() {
        return getKind().getSimpleName();
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public Key getKey() {
        return key;
    }

    public Long getId() {
        if (getKey() == null) return null;
        return getKey().getId();
    }
}
