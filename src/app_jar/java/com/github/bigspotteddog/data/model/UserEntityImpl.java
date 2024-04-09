package com.github.bigspotteddog.data.model;

import com.github.bigspotteddog.data.model.Autocreate;
import com.github.bigspotteddog.data.model.EntityImpl;

@SuppressWarnings("serial")
public class UserEntityImpl extends EntityImpl {
    @Autocreate(readonly = true, keysOnly = true)
    private User user;

    public void setUser(final User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
