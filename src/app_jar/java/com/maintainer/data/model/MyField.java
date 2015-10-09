package com.maintainer.data.model;

import java.lang.reflect.Field;

public class MyField extends EntityImpl {
    private String name;
    private String typeName;
    private Boolean embedded;
    private Boolean readonly;
    private Boolean create;
    private Boolean update;
    private Boolean delete;
    private Boolean remote;
    private Boolean skip;

    transient private Autocreate autocreate;
    transient private Class<?> type;
    transient private Field field;

    protected MyField() {}

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
        this.typeName = class1.getName();
    }

    public MyField(String name, String typeName) throws ClassNotFoundException {
        this.name = name;
        this.typeName = typeName;
        this.type = Class.forName(typeName);
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        if (type != null) {
            return type;
        }

        if (typeName != null) {
            try {
                Class<?> class1 = Class.forName(typeName);
                type = class1;
                return class1;
            } catch (ClassNotFoundException e) {
                // ignored
            }
        }

        return null;
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
        if (embedded == null) return false;
        return embedded;
    }

    public boolean readonly() {
        if (autocreate != null) {
            return autocreate.readonly();
        }
        if (readonly == null) return false;
        return readonly;
    }

    public boolean create() {
        if (autocreate != null) {
            return autocreate.create();
        }
        if (create == null) return false;
        return create;
    }

    public boolean update() {
        if (autocreate != null) {
            return autocreate.update();
        }
        if (update == null) return false;
        return update;
    }

    public boolean delete() {
        if (autocreate != null) {
            return autocreate.delete();
        }
        if (delete == null) return false;
        return delete;
    }

    public boolean remote() {
        if (autocreate != null) {
            return autocreate.remote();
        }
        if (remote == null) return false;
        return remote;
    }

    public boolean skip() {
        if (autocreate != null) {
            return autocreate.skip();
        }
        if (skip == null) return false;
        return skip;
    }
}
