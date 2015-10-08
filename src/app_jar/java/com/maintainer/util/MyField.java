package com.maintainer.util;

import java.lang.reflect.Field;

import com.maintainer.data.model.Autocreate;

public class MyField {
    private String name;
    private Class<?> type;
    private Field field;
    private Autocreate autocreate;

    public MyField(Field field) {
        this.name = field.getName();
        this.type = field.getType();
        this.field = field;

        field.setAccessible(true);
        this.autocreate = field.getAnnotation(Autocreate.class);
    }

    public MyField(String name, Class<?> class1) {
        this.name = name;
        this.type = class1;
    }

    public String getName() {
        return name;
    }

    public void setAccessible(boolean b) {
        if (field != null) {
            field.setAccessible(b);
        }
    }

    public Object get(Object obj) throws IllegalArgumentException, IllegalAccessException {
        if (field != null) {
            return field.get(obj);
        }
        return null;
    }

    public void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException {
        if (field != null) {
            field.set(obj, value);
        }
    }

    public Autocreate getAnnotation(Class<Autocreate> class1) {
        if (field != null) {
            return field.getAnnotation(class1);
        }
        return null;
    }

    public boolean isAutocreate() {
        return autocreate != null;
    }

    public boolean embedded() {
        if (autocreate != null) {
            return autocreate.embedded();
        }
        return false;
    }

    public boolean readonly() {
        if (autocreate != null) {
            return autocreate.readonly();
        }
        return false;
    }

    public boolean create() {
        if (autocreate != null) {
            return autocreate.create();
        }
        return false;
    }

    public boolean update() {
        if (autocreate != null) {
            return autocreate.update();
        }
        return false;
    }

    public boolean delete() {
        if (autocreate != null) {
            return autocreate.delete();
        }
        return false;
    }
}
