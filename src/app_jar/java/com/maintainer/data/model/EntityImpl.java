package com.maintainer.data.model;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.MappedSuperclass;

import com.maintainer.data.provider.AutoCreateVisitor;
import com.maintainer.data.provider.Key;

@MappedSuperclass
public class EntityImpl implements EntityBase {

    @javax.persistence.Id
    @com.google.code.morphia.annotations.Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public EntityBase accept(AutoCreateVisitor visitor) throws Exception {
        return visitor.autocreate(this);
    }

    @Override
    public boolean equals(Object obj) {
        EntityImpl other = (EntityImpl) obj;
        return this.getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        if (getId() == null) return 0;
        return getId().hashCode();
    }

    @Override
    public Key getKey() {
        return new Key(getClass(), getId());
    }
}