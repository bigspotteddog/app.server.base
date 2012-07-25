package com.maintainer.data.model;

import java.io.Serializable;
import java.util.Date;

import com.maintainer.data.provider.AutoCreateVisitor;
import com.maintainer.data.provider.Key;

public interface EntityBase extends Serializable {
    void setId(Object id);
    Object getId();
    void setModified(Date date);
    Date getModified();
    Key getKey();
    void setKey(Key key);
    EntityBase accept(AutoCreateVisitor visitor) throws Exception;
}
