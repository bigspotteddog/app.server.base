package com.maintainer.data.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.maintainer.data.provider.Key;

@SuppressWarnings("serial")
public class MapEntityImpl extends EntityImpl { //implements Map<String, Object> {

    @NotStored @NotIndexed
    private transient MyClass myClass;

    @NotStored @NotIndexed
    private final LinkedHashMap<String, Object> properties = new LinkedHashMap<String, Object>();

    public boolean isIndexed(final Object key) {
        return true;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void clear() {
        properties.clear();
    }


    public boolean containsKey(final Object key) {
        return properties.containsKey(key);
    }


    public boolean containsValue(final Object value) {
        return properties.containsValue(value);
    }


    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return properties.entrySet();
    }


    public Object get(final Object key) {
        return properties.get(key);
    }

    public void set(final String key, final Object value) {
        properties.put(key, value);
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }


    public Set<String> keySet() {
        return properties.keySet();
    }


    public Object put(final String key, final Object value) {
        return properties.put(key, value);
    }

    public void putAll(final Map<? extends String, ? extends Object> m) {
        properties.putAll(m);
    }

    public Object remove(final Object key) {
        return properties.remove(key);
    }

    public int size() {
        return properties.size();
    }

    public Collection<Object> values() {
        return properties.values();
    }

    public static Key valueOf(final String string) {
        return Key.fromString(string);
    }

    public MyClass getMyClass() {
        return myClass;
    }

    public void setMyClass(MyClass myClass) {
        this.myClass = myClass;
    }
}
