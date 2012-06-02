package com.maintainer.data.security;

import java.util.List;

import org.restlet.data.ClientInfo;
import org.restlet.security.Enroler;
import org.restlet.security.Role;

public class MyEnroler implements Enroler {

    private final String applicationName;

    public MyEnroler(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public void enrole(ClientInfo clientInfo) {

        List<Role> roles = clientInfo.getRoles();
        roles.add(new Role("*.*", "Any resource, any method."));
    }

    public String getApplicationName() {
        return applicationName;
    }
}
