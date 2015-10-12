package com.maintainer.data.provider;

import java.util.Collection;
import java.util.List;

import com.maintainer.data.model.MyField;

public interface DataProvider<T> {
    public abstract T get(Key key) throws Exception;
    public abstract List<T> getAll(Class<?> kind) throws Exception;
    public abstract List<T> getAll(final Collection<Key> keysNeeded) throws Exception;
    public abstract T post(T item) throws Exception;
    public abstract T put(T item) throws Exception;
    public abstract T merge(T incoming) throws Exception;
    public abstract Key delete(Key key) throws Exception;
    public abstract T fromJson(Class<?> kind, String json) throws Exception;
    public abstract List<T> find(Query query) throws Exception;
    public abstract Object getId(Object object) throws Exception;
    public abstract void beginTransaction() throws Exception;
    public abstract void commitTransaction() throws Exception;

    public abstract List<MyField> getFields(final Object target) throws Exception;
    public abstract List<MyField> getFields(final Object target, boolean isRecurse) throws Exception;
    public abstract void setFieldValue(final Object obj, final MyField f, final Object value) throws IllegalAccessException;
    public abstract Object getFieldValue(final Object obj, final MyField f) throws IllegalAccessException;
}