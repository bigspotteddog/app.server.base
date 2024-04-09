package com.github.bigspotteddog.data.security.model;

import com.github.bigspotteddog.data.model.EntityBase;

public interface Function extends EntityBase {
    String getPath();

    String getPermission();
}
