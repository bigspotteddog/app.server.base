package com.maintainer.data.security;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.Role;
import org.restlet.security.RoleAuthorizer;

import com.maintainer.util.Utils;

public class PermissionsRoleAuthorizer extends RoleAuthorizer {
    private static final Logger log = Logger.getLogger(PermissionsRoleAuthorizer.class.getName());

    private String applicationName = null;

    public PermissionsRoleAuthorizer(final String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    @Override
    public boolean authorize(final Request request, final Response response) {

        String path = request.getResourceRef().getRemainingPart();
        path = path.split("\\?")[0];
        final String method = request.getMethod().getName();

        final List<Role> roles = request.getClientInfo().getRoles();

        final String pathNeeded = getPermissionNeeded(applicationName, path, method);
        boolean granted = hasRole(pathNeeded, roles);

        log.debug("Has role " + pathNeeded + " = " + granted);

        if (Utils.isInternalRequest(request)) {
            log.debug("Access granted: internal request");
            granted = true;
        }

        return granted;
    }

    public boolean hasRole(final String pathNeeded, final List<Role> roles) {
        final Role role = new Role(pathNeeded, null);

        boolean granted = false;
        for (final Role r : roles) {
            granted = doesRolePathMatchPathNeeded(role, r);
            if (granted) break;
        }
        return granted;
    }

    public boolean doesRolePathMatchPathNeeded(final Role r0, final Role r1) {
        final String path0 = r0.getName();
        final String path1 = r1.getName();

        //return Utils.match(path0, path1);
        return matchAuthorization(path0, path1);
    }

    public String getPermissionNeeded(final String application, final String path, final String method) {
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

    /**
     * Compares a permission requestion to a configured permission
     * @param permission The permission the user is asking for
     * @param authorization The configured authorization for the user
     * @return True if the user has an appropriate authorization, False otherwise
     */
    private boolean matchAuthorization (final String permission, final String authorization) {
        final String[] permSegs = StringUtils.split(permission, ".");
        final String[] authSegs = StringUtils.split(authorization, ".");

        final Queue<String> permQueue = new ArrayBlockingQueue<String>(permSegs.length, true, Arrays.asList(permSegs));
        final Queue<String> authQueue = new ArrayBlockingQueue<String>(authSegs.length, true, Arrays.asList(authSegs));

        for (String permSeg = permQueue.poll(),
                    authSeg = authQueue.poll(),
                    authNext = null;
             permSeg != null;
             permSeg = permQueue.poll()) {

            if (authSeg.equals("**")) {
                authNext = authQueue.peek();

                while (null != permQueue.peek() && !permQueue.peek().equals(authNext)) {
                    // Keep moving until we reach the end or we move past the wild card
                    permQueue.poll();
                }

                authSeg = authQueue.poll();
                continue;
            }

            if (!permSeg.equals(authSeg) && !authSeg.equals("*")) {
                return false;
            }

            authSeg = authQueue.poll();
        }

        return true;
    }
}
