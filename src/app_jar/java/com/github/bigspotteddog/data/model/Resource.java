package com.github.bigspotteddog.data.model;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Resource {
    String name() default "";

    boolean secured() default true;

    boolean useKey() default false;
}
