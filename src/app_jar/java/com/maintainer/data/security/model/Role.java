package com.maintainer.data.security.model;

import java.util.List;

import com.maintainer.data.model.EntityBase;

public interface Role extends EntityBase {
    String getName();
    List getFunctions();
    List getUsers();
}
