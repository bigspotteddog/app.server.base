package com.github.bigspotteddog.data.model;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Autocreate {
    public static final String EMPTY = "";
    public static final int MAX_DEPTH = 100; // Integer.MAX_VALUE;

    String parent() default EMPTY;

    String id() default EMPTY;

    boolean create() default true;

    boolean update() default true;

    boolean delete() default true;

    boolean readonly() default false;

    boolean remote() default false;

    boolean skip() default false;

    boolean keysOnly() default false;

    boolean embedded() default false;

    int depth() default MAX_DEPTH;
}
