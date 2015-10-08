package com.maintainer.util;

import java.lang.reflect.Field;

public class MyField {
    private String name;
    private Class<?> type;
    private Field field;

    public MyField(Field field) {
        this.name = field.getName();
        this.type = field.getType();
        this.field = field;
    }

    public MyField(String name, Class<?> class1) {
        this.name = name;
        this.type = class1;
    }
}
