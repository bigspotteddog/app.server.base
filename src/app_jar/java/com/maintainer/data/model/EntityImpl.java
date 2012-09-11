package com.maintainer.data.model;

import java.util.Date;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.MappedSuperclass;

import com.maintainer.data.provider.AutoCreateVisitor;
import com.maintainer.data.provider.Key;

@MappedSuperclass
public class EntityImpl implements EntityBase {
    @NotIndexed @NotStored
    transient private EntityBase parent;

    @javax.persistence.Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotIndexed @NotStored
    private Object id;

    @NotIndexed
    transient private Date created;

    @NotIndexed
    transient private Date modified;

    @NotIndexed @NotStored
    transient private Key key;

    @NotIndexed @NotStored
    private String cursor;

    @NotIndexed @NotStored
    private String keyString;

    @Override
    public boolean isNew() {
        return id == null;
    }

    @Override
    public void setParent(final EntityBase parent) {
        this.parent = parent;
    }

    @Override
    public EntityBase getParent() {
        return parent;
    }

    @Override
    public void setId(final Object id) {
        this.id = id;
    }

    public String getWebSafeKey() {
        return keyString;
    }

    @Override
    public Object getId() {
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
            key = Key.fromString((String) id);
        }
        return key;
    }

    @Override
    public void setKey(final Key key) {
        this.key = key;
    }

    @Override
   public Date getCreated() {
       return created;
   }

   @Override
   public void setCreated(final Date created) {
       this.created = created;
   }

     @Override
    public Date getModified() {
        return modified;
    }

    @Override
    public void setModified(final Date modified) {
        this.modified = modified;
    }

    @Override
    public void setCursor(final String cursor) {
        this.cursor = cursor;
    }

    @Override
    public String getCursor() {
        return cursor;
    }
}