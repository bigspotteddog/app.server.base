package com.github.bigspotteddog.data.model;

public interface UserBase extends EntityBase {
    String getUsername();

    void setUsername(String username);

    String getPassword();

    void setPassword(String password);
}
