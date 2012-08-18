package com.maintainer.data.provider;

import java.util.LinkedHashMap;

public class Query extends LinkedHashMap<String, Object> {
    public static final String NEXT = "NEXT";
    public static final String PREVIOUS = "PREVIOUS";

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
	public static final String IN = ":in";

    public static final String ORDER = ":order";
    public static final String OFFSET = ":offset";
    public static final String LIMIT = ":limit";
    public static final String POS = ":pos";


    private Class<?> kind;
    private String order;
    private int offset;
    private int limit;
    private Key key;

    private String previousCursor;
    private String nextCursor;
    private String pageDirection;

    public Query(final Class<?> kind) {
        this.kind = kind;
    }

    public Query filter(final String condition, final Object value) {
        this.put(condition, value);
        return this;
    }

    public Query setOrder(final String order) {
        this.order = order;
        return this;
    }

    public Class<?> getKind() {
        return kind;
    }

    public void setKind(final Class<?> kind) {
        this.kind = kind;
    }

    public String getOrder() {
        return order;
    }

    public Query setOffset(final int offset) {
        this.offset = offset;
        return this;
    }

    public int getOffset() {
        return offset;
    }

    public Query setLimit(final int limit) {
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

    public void setKey(final Key key) {
        this.key = key;
    }

    public Key getKey() {
        return key;
    }

    public Object getId() {
        if (getKey() == null) {
            return null;
        }
        return getKey().getId();
    }

    public void setPreviousCursor(final String cursor) {
        this.previousCursor = cursor;
    }

    public String getPreviousCursor() {
        return previousCursor;
    }

    public void setNextCursor(final String cursor) {
        this.nextCursor = cursor;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void previous() {
        pageDirection = PREVIOUS;
    }

    public void next() {
        pageDirection = NEXT;
    }

    public String getPageDirection() {
        return pageDirection;
    }

    public void resetPagination() {
        previousCursor = null;
        nextCursor = null;
        pageDirection = null;
    }

    public Query reverse() {
        return null;
    }
}
