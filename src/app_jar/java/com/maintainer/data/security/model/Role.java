package com.maintainer.data.security.model;

import java.util.List;

import com.maintainer.data.model.EntityBase;
import com.maintainer.data.model.User;

public interface Role extends EntityBase {
    String getName();
    List<Function> getFunctions();
    List<User> getUsers();
}
