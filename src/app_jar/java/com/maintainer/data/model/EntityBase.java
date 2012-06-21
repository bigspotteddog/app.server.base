package com.maintainer.data.model;

import java.util.Date;

import com.maintainer.data.provider.AutoCreateVisitor;
import com.maintainer.data.provider.Key;

public interface EntityBase {
    void setId(Long id);
    Long getId();
    void setModified(Date date);
    Date getModified();
    Key getKey();
    EntityBase accept(AutoCreateVisitor visitor) throws Exception;
}
