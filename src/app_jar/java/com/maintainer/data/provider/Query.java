package com.maintainer.data.provider;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Strings;
import com.maintainer.data.model.Autocreate;
import com.maintainer.data.model.MyClass;
import com.maintainer.util.Utils;


public class Query {
    private static final String ID = "id";
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


    private MyClass myClass;
    private Class<?> kind;
    private final List<Filter> filters = new ArrayList<Filter>();
    private String order;
    private int offset;
    private int limit;
    private Key parent;
    private Key key;
    private boolean isKeysOnly;

    private String previousCursor;
    private String nextCursor;
    private String pageDirection;

    public Query(final Class<?> kind) {
        this.kind = kind;
    }

    public Query(final MyClass myClass) {
        this.myClass = myClass;
    }

    public Class<?> getType() {
        Class<?> kind = getKind();
        if (kind == null) {
            kind = myClass.getType();
        }
        return kind;
    }

    public Query filter(final String condition, Object value) throws Exception {
        final Class<?> kind = getKind();

        if (kind != null) {
            final Autocreate annotation = kind.getAnnotation(Autocreate.class);
            if (ID.equals(condition) && (annotation == null || Autocreate.EMPTY.equals(annotation.parent()))) {
                final Key key = Key.create(kind, value);
                setKey(key);
                return this;
            }
        }

        if (ID.equals(condition)) {
            value = Utils.convert(value, Long.class);
        }

        final Filter filter = new Filter(condition, value);
        filters.add(filter);

        return this;
    }

    public List<Filter> getFilters() {
        return filters;
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
        Class<?> kind = getKind();
        if (kind == null) {
            return myClass.getName();
        }
        return kind.getSimpleName();
    }

    public void setParent(final Key parent) {
        this.parent = parent;
    }

    public Key getParent() {
        return parent;
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

    public boolean isEmpty() {
        return filters.isEmpty() && Strings.isNullOrEmpty(order);
    }

    public boolean isKeysOnly() {
        return isKeysOnly;
    }

    public void setKeysOnly(final boolean isKeysOnly) {
        this.isKeysOnly = isKeysOnly;
    }
}
