package com.maintainer.data.model;

import java.util.Date;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.MappedSuperclass;

import com.maintainer.data.provider.AutoCreateVisitor;
import com.maintainer.data.provider.Key;

@MappedSuperclass
public class EntityImpl implements EntityBase {

    @javax.persistence.Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Object id;

    private Date modified;

    private Key key;

    @Override
    public boolean isNew() {
        return id == null;
    }

    @Override
    public void setId(final Object id) {
        this.id = id;
    }

    @Override
    public Object getId() {
        if (key != null) {
            return key.getId();
        }
        return id;
    }

    @Override
    public EntityBase accept(final AutoCreateVisitor visitor) throws Exception {
        return visitor.autocreate(this);
    }

    @Override
    public boolean equals(final Object obj) {
        final EntityImpl other = (EntityImpl) obj;
        return this.getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        if (getId() == null) {
            return 0;
        }
        return getId().hashCode();
    }

    @Override
    public Key getKey() {
        if (key == null) {
            return new Key(getClass(), getId());
        }
        return key;
    }

    @Override
    public void setKey(final Key key) {
        this.key = key;
        this.id = key.getId();
    }

     @Override
    public Date getModified() {
        return modified;
    }

    @Override
    public void setModified(final Date modified) {
        this.modified = modified;
    }
}