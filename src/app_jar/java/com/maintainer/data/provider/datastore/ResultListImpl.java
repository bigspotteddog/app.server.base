package com.maintainer.data.provider.datastore;

import java.util.ArrayList;
import java.util.Collection;

import com.google.appengine.api.datastore.Cursor;
import com.maintainer.data.model.EntityImpl;

@SuppressWarnings("rawtypes")
public class ResultListImpl<T> extends ArrayList<T> implements ResultList<T> {
    private static final ResultListImpl EMPTY = new ResultListImpl();
    private static final long serialVersionUID = -7574470429331801599L;
    private Cursor startCursor;
    private Cursor endCursor;
    private boolean removedCursors;

    public ResultListImpl() {
        super();
    }

    public ResultListImpl(final int initialCapacity) {
        super(initialCapacity);
    }

    public ResultListImpl(final Collection<? extends T> c) {
        super(c);
    }

    public static ResultListImpl emptyList() {
        return EMPTY;
    }

    public void setStartCursor(final Cursor cursor) {
        this.startCursor = cursor;
    }

    public Cursor getStartCursor() {
        return startCursor;
    }

    @Override
    public String previous() {
        if (startCursor == null) {
            return null;
        }
        return startCursor.toWebSafeString();
    }

    public void setEndCursor(final Cursor cursor) {
        this.endCursor = cursor;
    }

    public Cursor getEndCursor() {
        return endCursor;
    }

    @Override
    public String next() {
        if (endCursor == null) {
            return null;
        }
        return endCursor.toWebSafeString();
    }

    @Override
    public EntityImpl first() {
        if (isEmpty()) {
            return null;
        }
        return (EntityImpl) get(0);
    }

    @Override
    public EntityImpl last() {
        if (isEmpty()) {
            return null;
        }
        return (EntityImpl) get(size() - 1);
    }

    public void setRemovedCursors(final boolean removedCursors) {
        this.removedCursors = removedCursors;
    }

    public boolean isRemovedCursors() {
        return removedCursors;
    }
}
