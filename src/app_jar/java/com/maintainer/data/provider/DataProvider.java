package com.maintainer.data.provider;

import java.util.Collection;
import java.util.List;

public interface DataProvider<T> {
    public abstract T get(Key key) throws Exception;
    public abstract List<T> getAll(Class<?> kind) throws Exception;
    public abstract List<T> getAll(final Collection<Key> keysNeeded) throws Exception;
    public abstract T post(T item) throws Exception;
    public abstract T put(T item) throws Exception;
    public abstract T merge(T incoming) throws Exception;
    public abstract Key delete(Key key) throws Exception;
    public abstract T fromJson(Class<?> kind, String json);
    public abstract List<T> find(Query query) throws Exception;
    public abstract Object getId(Object object);
    public abstract void beginTransaction();
    public abstract void commitTransaction();
}