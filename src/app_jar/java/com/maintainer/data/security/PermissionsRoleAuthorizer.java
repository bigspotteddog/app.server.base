package com.maintainer.data.security;

import java.util.List;

import org.apache.log4j.Logger;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.Role;
import org.restlet.security.RoleAuthorizer;

import com.maintainer.util.Utils;

public class PermissionsRoleAuthorizer extends RoleAuthorizer {
    private static final Logger log = Logger.getLogger(PermissionsRoleAuthorizer.class.getName());

    private String applicationName = null;

    public PermissionsRoleAuthorizer(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    @Override
    public boolean authorize(Request request, Response response) {

        String path = request.getResourceRef().getRemainingPart();
        path = path.split("\\?")[0];
        String method = request.getMethod().getName();

        List<Role> roles = request.getClientInfo().getRoles();

        String pathNeeded = getPermissionNeeded(applicationName, path, method);
        boolean granted = hasRole(pathNeeded, roles);

        log.debug("Has role " + pathNeeded + " = " + granted);

        if (Utils.isInternalRequest(request)) {
            log.debug("Access granted: internal request");
            granted = true;
        }

        return granted;
    }

    public boolean hasRole(String pathNeeded, List<Role> roles) {
        Role role = new Role(pathNeeded, null);

        boolean granted = false;
        for (Role r : roles) {
            granted = doesRolePathMatchPathNeeded(role, r);
            if (granted) break;
        }
        return granted;
    }

    public boolean doesRolePathMatchPathNeeded(Role r0, Role r1) {
        String path0 = r0.getName();
        String path1 = r1.getName();

        return Utils.match(path0, path1);
    }

    public String getPermissionNeeded(String application, String path, String method) {
        String needsPermission = null;

        log.debug(path);

        String resource = path;
        if (resource.startsWith("/")) {
            resource = resource.substring(1);
        }

        needsPermission = resource;
        needsPermission = needsPermission.replace('/', '.');
        needsPermission += "." + method;

        return needsPermission;
    }
}