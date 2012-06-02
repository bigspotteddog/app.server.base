package com.maintainer.data.model;

public interface UserBase extends EntityBase {
    String getUsername();
    void setUsername(String username);

    String getPassword();
    void setPassword(String password);
}
