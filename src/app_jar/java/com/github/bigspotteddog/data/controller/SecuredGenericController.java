package com.github.bigspotteddog.data.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.restlet.Request;
import org.restlet.data.ClientInfo;
import org.restlet.security.User;

import com.github.bigspotteddog.util.Utils;
import com.github.bigspotteddog.data.provider.Query;
import com.github.bigspotteddog.data.router.WebSwitch;
import com.github.bigspotteddog.data.security.model.Filter;
import com.github.bigspotteddog.data.security.model.Role;

public class SecuredGenericController extends GenericController {
    private static final Logger log = Logger.getLogger(SecuredGenericController.class.getName());
    private static final Map<String, String> securityResources = new HashMap<String, String>();

    static {
        securityResources.put("users", "users");
        securityResources.put("applications", "applications");
        securityResources.put("roles", "roles");
        securityResources.put("functions", "functions");
        securityResources.put("filters", "filters");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Query addParametersToQuery(final Request request, final Resource resource, final Query query)
            throws Exception {
        final String security = Utils.getApplicationServerProperties().getProperty("appserver.application.secured");
        if (Boolean.parseBoolean(security)) {
            final ClientInfo clientInfo = request.getClientInfo();
            final User user = clientInfo.getUser();
            final String identifier = user.getIdentifier();

            if (notSecurityResource(resource)) {
                final WebSwitch application = (WebSwitch) getApplication();

                final List<Role> roles = (List<Role>) Utils.subrequest(
                        application,
                        "roles?users.username=" + identifier,
                        request);

                if (roles != null && !roles.isEmpty()) {
                    final StringBuilder buf = new StringBuilder().append('[');
                    for (final Role role : roles) {
                        if (buf.length() > 1) {
                            buf.append(',');
                        }
                        buf.append(role.getId());
                    }
                    buf.append(']');

                    final String idsString = buf.toString();
                    log.fine("Looking for resource: " + "filters?role:in=" + idsString + "&resource="
                            + resource.getResource());

                    final List<Filter> filters = (List<Filter>) Utils.subrequest(
                            application,
                            "filters?role:in=" + idsString + "&resource=" + resource.getResource(),
                            request);

                    for (final Filter filter : filters) {
                        log.fine("Adding filter: " + filter.getFilter() + ", " + filter.getIds().toString());
                        query.filter(filter.getFilter(), filter.getIds());
                    }
                }
            }
        }

        return super.addParametersToQuery(request, resource, query);
    }

    private boolean notSecurityResource(final Resource resource) {
        return securityResources.get(resource.getResource()) == null;
    }
}
