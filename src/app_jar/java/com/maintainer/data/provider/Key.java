package com.maintainer.data.provider;

public class Key {
    private Key parent;
    private Class<?> kind;
    private Long id;
    private String name;

    public Key(Class<?> kind, Long id) {
        this.kind = kind;
        this.id = id;
    }

    public Key(Class<?> kind, String name) {
        this.kind = kind;
        this.name = name;
    }

    public Key(Class<?> kind, Long id, Key parent) {
        this.kind = kind;
        this.id = id;
    }

    public Key(Class<?> kind, String name, Key parent) {
        this.kind = kind;
        this.name = name;
    }

    public Key getParent() {
        return parent;
    }

    public void setParent(Key parent) {
        this.parent = parent;
    }

    public Class<?> getKind() {
        return kind;
    }

    public void setKind(Class<?> kind) {
        this.kind = kind;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKindName() {
        return getKindName(kind);
    }

    public static String getKindName(Class<?> clazz) {
        return clazz.getName();
    }
}
