package com.maintainer.data.provider;

public class Key {
    transient private Class<?> kind;
    private Key parent;
    private String kindName;
    private Long id;
    private String name;

    protected Key() {}

    public Key(final Class<?> kind, final Long id) {
        setKind(kind);
        this.id = id;
    }

    public Key(final Class<?> kind, final String name) {
        setKind(kind);
        this.name = name;
    }

    public Key(final Class<?> kind, final Long id, final Key parent) {
        setKind(kind);
        this.id = id;
    }

    public Key(final Class<?> kind, final String name, final Key parent) {
        setKind(kind);
        this.name = name;
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

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
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
}
