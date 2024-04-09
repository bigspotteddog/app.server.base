package com.github.bigspotteddog.data.provider;

import com.github.bigspotteddog.data.model.EntityBase;

public interface AutoCreateVisitor {
    EntityBase autocreate(EntityBase entity) throws Exception;
}
