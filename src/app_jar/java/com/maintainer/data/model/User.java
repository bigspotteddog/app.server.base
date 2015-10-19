package com.maintainer.data.model;

import javax.persistence.MappedSuperclass;

@SuppressWarnings("serial")
@MappedSuperclass
public class User extends EntityImpl implements UserBase {

    private String username;
    @NotIndexed
    private String password;

    protected User() {}

    public User(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void setUsername(final String username) {
        this.username = username;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setPassword(final String password) {
        this.password = password;
    }

    @Override
    public String getPassword() {
        return password;
    }
}
