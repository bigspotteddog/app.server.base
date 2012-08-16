package com.maintainer.data.provider.datastore;

import java.util.ArrayList;
import java.util.Collection;

import com.google.appengine.api.datastore.Cursor;

@SuppressWarnings("rawtypes")
public class ResultList<T> extends ArrayList<T> {
    private static final ResultList EMPTY = new ResultList();
    private static final long serialVersionUID = -7574470429331801599L;
    private Cursor startCursor;
    private Cursor endCursor;

    public ResultList() {
        super();
    }

    public ResultList(final int initialCapacity) {
        super(initialCapacity);
    }

    public ResultList(final Collection<? extends T> c) {
        super(c);
    }

    public static ResultList emptyList() {
        return EMPTY;
    }

    public void setStartCursor(final Cursor cursor) {
        this.startCursor = cursor;
    }

    public Cursor getStartCursor() {
        return startCursor;
    }

    public void setEndCursor(final Cursor cursor) {
        this.endCursor = cursor;
    }

    public Cursor getEndCursor() {
        return endCursor;
    }
}
