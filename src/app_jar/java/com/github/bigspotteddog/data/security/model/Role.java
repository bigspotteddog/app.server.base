package com.github.bigspotteddog.data.security.model;

import java.util.List;

import com.github.bigspotteddog.data.model.EntityBase;
import com.github.bigspotteddog.data.model.User;

public interface Role extends EntityBase {
    String getName();

    List<Function> getFunctions();

    List<User> getUsers();
}
