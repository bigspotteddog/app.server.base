package com.maintainer.data.controller;

import java.util.List;

import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.restlet.security.Role;

import com.maintainer.data.router.WebSwitch;
import com.maintainer.data.security.PermissionsRoleAuthorizer;

public class CheckPermissionController extends ServerResource {
    @Get("json")
    public Representation check(Representation rep) throws Exception {
        return check();
    }

    public Representation check() {
        Status status = Status.SUCCESS_OK;

        WebSwitch router = (WebSwitch) this.getApplication();
        if (router.isSecured()) {
            Request request = getRequest();

            Form form = getReference().getQueryAsForm();
            String path = form.getFirstValue("url", true);
            String method = form.getFirstValue("method", true);

            PermissionsRoleAuthorizer roleAuthorizer = router.getRoleAuthorizer();

            String permissionNeeded = roleAuthorizer.getPermissionNeeded(roleAuthorizer.getApplicationName(), path, method);

            List<Role> roles = request.getClientInfo().getRoles();
            boolean granted = roleAuthorizer.hasRole(permissionNeeded, roles);

            if (!granted) {
                status = Status.CLIENT_ERROR_FORBIDDEN;
            }
        }

        getResponse().setStatus(status);
        return new JsonRepresentation("{}");
    }
}
