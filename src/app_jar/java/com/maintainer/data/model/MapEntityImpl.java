package com.maintainer.data.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class MapEntityImpl extends EntityImpl implements Map<String, Object> {

    @NotStored
    private final LinkedHashMap<String, Object> properties = new LinkedHashMap<String, Object>();

    public boolean isIndexed(final Object key) {
        return true;
    }

    @Override
    public void setId(final Object id) {
        put("id", id);
    }

    @Override
    public void setCursor(final String cursor) {
        put("cursor", cursor);
    }

    @Override
    public void clear() {
        properties.clear();
    }


    @Override
    public boolean containsKey(final Object key) {
        return properties.containsKey(key);
    }


    @Override
    public boolean containsValue(final Object value) {
        return properties.containsValue(value);
    }


    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return properties.entrySet();
    }


    @Override
    public Object get(final Object key) {
        return properties.get(key);
    }


    @Override
    public boolean isEmpty() {
        return properties.isEmpty();
    }


    @Override
    public Set<String> keySet() {
        return properties.keySet();
    }


    @Override
    public Object put(final String key, final Object value) {
        return properties.put(key, value);
    }


    @Override
    public void putAll(final Map<? extends String, ? extends Object> m) {
        properties.putAll(m);
    }


    @Override
    public Object remove(final Object key) {
        return properties.remove(key);
    }


    @Override
    public int size() {
        return properties.size();
    }


    @Override
    public Collection<Object> values() {
        return properties.values();
    }
}
