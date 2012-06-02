package com.maintainer.data.security.model;

import java.util.List;

import com.maintainer.data.model.EntityBase;

public interface Filter extends EntityBase {
    int getRole();
    String getResource();
    String getFilter();
    List<Long> getIds();
}
