package com.github.bigspotteddog.data.security.model;

import java.util.List;

import com.github.bigspotteddog.data.model.EntityBase;

public interface Filter extends EntityBase {
    int getRole();

    String getResource();

    String getFilter();

    List<Long> getIds();
}
