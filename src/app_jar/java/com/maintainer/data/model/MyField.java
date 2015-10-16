package com.maintainer.data.model;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

@SuppressWarnings("serial")
public class MyField extends EntityImpl {
    private String name;
    private String description;
    private Boolean embedded;
    private Boolean readonly;
    private Boolean create;
    private Boolean update;
    private Boolean delete;
    private Boolean remote;
    private Boolean skip;

    @Autocreate(create=false, update=false, delete=false, embedded=true)
    private MyType myType;

    transient private Autocreate autocreate;
    transient private Class<?> type;
    transient private Field field;
    transient private NotStored notStored;
    transient private NotIndexed notIndexed;

    protected MyField() {}

    public MyField(Field field) {
        this.name = field.getName();
        this.type = field.getType();
        this.field = field;

        field.setAccessible(true);
        this.autocreate = field.getAnnotation(Autocreate.class);
        this.notStored = field.getAnnotation(NotStored.class);
        this.notIndexed = field.getAnnotation(NotIndexed.class);
    }

    public MyField(String name, Class<?> class1) {
        this.name = name;
        this.type = class1;
        this.myType = new MyType(class1);
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        if (type != null) {
            return type;
        }

        if (myType != null) {
            try {
                String className = myType.getClassName();
                Class<?> class1 = Class.forName(className);
                type = class1;
                return class1;
            } catch (ClassNotFoundException e) {
                // ignored
            }
        }

        return MapEntityImpl.class;
    }

    public MyType getMyType() {
        return myType;
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

    public boolean isAutocreate() {
        if (MapEntityImpl.class == type || type == null) {
            // Assume autocreate for now.
            return true;
        }
        return autocreate != null;
    }

    public Autocreate getAutocreate() {
        return autocreate;
    }

    public boolean isNotStored() {
        return notStored != null;
    }

    public NotStored getNotStored() {
        return notStored;
    }

    public boolean isNotIndexed() {
        return notIndexed != null;
    }

    public NotIndexed getNotIndexed() {
        return notIndexed;
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

    public Type getGenericType() {
        if (field != null) {
            return field.getGenericType();
        }
        return null;
    }

    @Override
    public int hashCode() {
        if (name != null) {
            return name.hashCode();
        }
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        MyField other = (MyField) obj;
        if (obj == null) return false;

        if (name != null) {
            return name.equals(other.name);
        }
        return false;
    }
}
