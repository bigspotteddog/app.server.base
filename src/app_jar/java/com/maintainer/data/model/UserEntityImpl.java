package com.maintainer.data.model;

import com.maintainer.data.model.Autocreate;
import com.maintainer.data.model.EntityImpl;

@SuppressWarnings("serial")
public class UserEntityImpl extends EntityImpl {
    @Autocreate(readonly=true)
    private User user;

    public void setUser(final User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
