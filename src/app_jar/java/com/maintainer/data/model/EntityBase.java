package com.maintainer.data.model;

import com.maintainer.data.provider.AutoCreateVisitor;
import com.maintainer.data.provider.Key;

public interface EntityBase {
    void setId(Long id);
    Long getId();
    Key getKey();
    EntityBase accept(AutoCreateVisitor visitor) throws Exception;
}
