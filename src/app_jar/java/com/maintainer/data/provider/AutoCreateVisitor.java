package com.maintainer.data.provider;

import com.maintainer.data.model.EntityBase;

public interface AutoCreateVisitor {
    EntityBase autocreate(EntityBase entity) throws Exception;
}
