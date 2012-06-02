package com.maintainer.data.security;

import java.util.List;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ClientInfo;
import org.restlet.data.CookieSetting;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.crypto.CookieAuthenticator;
import org.restlet.security.User;

import com.maintainer.data.router.WebSwitch;
import com.maintainer.data.security.model.Function;
import com.maintainer.data.security.model.Role;
import com.maintainer.util.Utils;

public class MyCookieAuthenticator extends CookieAuthenticator {
    public MyCookieAuthenticator(Context context, boolean optional, String realm, byte[] encryptSecretKey) {
        super(context, optional, realm, encryptSecretKey);
    }

    @Override
    protected CookieSetting getCredentialsCookie(Request request, Response response) {
        CookieSetting credentialsCookie = response.getCookieSettings()
                .getFirst(getCookieName());

        if (credentialsCookie == null) {
            credentialsCookie = new CookieSetting(getCookieName(), null);
            credentialsCookie.setAccessRestricted(true);
            credentialsCookie.setPath("/");
            response.getCookieSettings().add(credentialsCookie);
        }

        return credentialsCookie;
    }

    @Override
    protected boolean authenticate(Request request, Response response) {
        boolean authenticated = super.authenticate(request, response);
        if (!authenticated) {
            boolean json = false;

            List<Preference<MediaType>> mediaTypes = Request.getCurrent().getClientInfo().getAcceptedMediaTypes();
            for (Preference<MediaType> t : mediaTypes) {
                MediaType type = t.getMetadata();
                if (type.equals(MediaType.APPLICATION_JSON)) {
                    json = true;
                    break;
                }
            }
            if (json) {
                response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED, "Not authorized.");
            } else {
                response.redirectSeeOther(new Reference(request.getRootRef(), "/"));
            }
        }
        return authenticated;
    }

    @SuppressWarnings({ "unused", "unchecked" })
    @Override
    protected int authenticated(Request request, Response response) {
        if (!Utils.isInternalRequest(request)) {
            ClientInfo clientInfo = request.getClientInfo();
            User user = clientInfo.getUser();
            if (user != null) {
                String identifier = user.getIdentifier();

                List<org.restlet.security.Role> roles = clientInfo.getRoles();

                List<Role> groups = (List<Role>) Utils.subrequest((WebSwitch) getApplication(), "roles?users.username=" + user.getIdentifier(), request);
                if (groups != null) {
                    for (Role role : groups) {
                        List<Function> functions = role.getFunctions();
                        for (Function function : functions) {
                            roles.add(new org.restlet.security.Role(function.getPath(), null));
                        }
                    }
                }
            }
        }
        return super.authenticated(request, response);
    }
}
