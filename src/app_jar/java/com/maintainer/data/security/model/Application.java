package com.maintainer.data.security.model;

import java.util.List;

import com.maintainer.data.model.EntityBase;

public interface Application extends EntityBase {
    String getName();
    List getFunctions();
}
