package com.github.bigspotteddog.data.security.model;

import java.util.List;

import com.github.bigspotteddog.data.model.EntityBase;

public interface Application extends EntityBase {
    String getName();

    List<Function> getFunctions();
}
