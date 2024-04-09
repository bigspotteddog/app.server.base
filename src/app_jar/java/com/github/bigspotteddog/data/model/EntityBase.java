package com.github.bigspotteddog.data.model;

import java.io.Serializable;
import java.util.Date;

import com.github.bigspotteddog.data.provider.AutoCreateVisitor;
import com.github.bigspotteddog.data.provider.Key;

public interface EntityBase extends Serializable {
    boolean isNew();

    void setParent(EntityBase parent);

    EntityBase getParent();

    void setId(String id);

    String getId();

    void setIdentity(Object obj);

    Object getIdentity();

    void setCreated(Date date);

    Date getCreated();

    void setModified(Date date);

    Date getModified();

    Key getKey();

    void setKey(Key key);

    EntityBase accept(AutoCreateVisitor visitor) throws Exception;

    void setCursor(String cursor);

    String getCursor();
}
