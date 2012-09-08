package com.maintainer.data.provider;

import java.io.Serializable;

public class Key implements Comparable<Key>, Serializable {
    private static final long serialVersionUID = -6778571338817109631L;
    transient private Class<?> kind;
    private Key parent;
    private String kindName;
    private Object id;

    protected Key() {}

    public Key(final Class<?> kind, final Long id) {
        setKind(kind);
        this.id = id;
    }

    public Key(final Class<?> kind, final String name) {
        setKind(kind);
        this.id = name;
    }

    public Key(final Class<?> kind, final Object id) {
        setKind(kind);
        this.id = id;
    }

    public Key(final Class<?> kind, final Object id, final Key parent) {
        setKind(kind);
        this.parent = parent;
        this.id = id;
    }

    public Key(final Class<?> kind, final Long id, final Key parent) {
        setKind(kind);
        this.parent = parent;
        this.id = id;
    }

    public Key(final Class<?> kind, final String name, final Key parent) {
        setKind(kind);
        this.parent = parent;
        this.id = name;
    }

    public Key getParent() {
        return parent;
    }

    public void setParent(final Key parent) {
        this.parent = parent;
    }

    public Class<?> getKind() {
        return kind;
    }

    public void setKind(final Class<?> kind) {
        this.kind = kind;
        if (kind == null) {
            this.setKindName(null);
        }
        this.setKindName(kind.getName());
    }

    public Object getId() {
        return id;
    }

    public void setId(final Object id) {
        this.id = id;
    }

    public String getKindName() {
        return kindName;
    }

    public void setKindName(final String kindName) {
        this.kindName = kindName;
    }

    public static String getKindName(final Class<?> kind) {
        return kind.getName();
    }

    @Override
    public boolean equals(final Object other) {
        if (other == null) {
            return false;
        }
        return this.toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        if (parent != null) {
            buf
            .append(parent.toString())
            .append('/');
        }

        return buf
            .append(kindName)
            .append('(')
            .append(id)
            .append(')')
            .toString();

    }

    @Override
    public int compareTo(final Key other) {
        return toString().compareTo(other.toString());
    }

    public static Key fromString(final String string) {
        return null;
    }
}
