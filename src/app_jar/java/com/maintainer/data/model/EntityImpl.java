package com.maintainer.data.model;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.MappedSuperclass;

import com.maintainer.data.provider.AutoCreateVisitor;
import com.maintainer.data.provider.Key;

@MappedSuperclass
public class EntityImpl implements EntityBase {
    @NotIndexed @NotStored
    private EntityBase parent;

    @javax.persistence.Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotIndexed @NotStored
    private Object id;

    @NotIndexed
    private Date created;

    @NotIndexed
    private Date modified;

    @NotIndexed @NotStored
    private Key key;

    @NotIndexed @NotStored
    private String cursor;

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

            if (getId() != null && !String.class.isAssignableFrom(getId().getClass())) {
                setId(new BigDecimal(getId().toString()).longValue());
            }

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