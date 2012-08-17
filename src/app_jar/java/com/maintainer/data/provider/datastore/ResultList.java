package com.maintainer.data.provider.datastore;

import java.util.List;

import com.maintainer.data.model.EntityImpl;


public interface ResultList<T> extends List<T> {
    public String previous();
    public String next();
    public EntityImpl first();
    public EntityImpl last();
}
