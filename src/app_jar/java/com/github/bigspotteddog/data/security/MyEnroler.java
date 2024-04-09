package com.github.bigspotteddog.data.security;

import java.util.List;

import org.restlet.data.ClientInfo;
import org.restlet.security.Enroler;
import org.restlet.security.Role;

public class MyEnroler implements Enroler {

    private final String applicationName;

    public MyEnroler(final String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public void enrole(final ClientInfo clientInfo) {

        final List<Role> roles = clientInfo.getRoles();
        roles.add(new Role("*.*", "Any resource, any method."));
    }

    public String getApplicationName() {
        return applicationName;
    }
}
