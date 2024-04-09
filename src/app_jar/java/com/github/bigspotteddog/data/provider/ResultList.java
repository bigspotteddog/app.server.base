package com.github.bigspotteddog.data.provider;

import java.util.List;

import com.github.bigspotteddog.data.model.EntityImpl;

public interface ResultList<T> extends List<T> {
    public String previous();

    public String next();

    public EntityImpl first();

    public EntityImpl last();
}
