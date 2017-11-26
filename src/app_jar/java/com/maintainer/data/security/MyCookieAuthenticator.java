package com.maintainer.data.security;

import java.util.List;

import org.apache.log4j.Logger;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ClientInfo;
import org.restlet.data.CookieSetting;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Status;
import org.restlet.ext.crypto.CookieAuthenticator;
import org.restlet.resource.ServerResource;
import org.restlet.security.User;

import com.maintainer.data.model.Resource;
import com.maintainer.data.router.WebSwitch;
import com.maintainer.data.security.model.Function;
import com.maintainer.data.security.model.Role;
import com.maintainer.util.Utils;

public class MyCookieAuthenticator extends CookieAuthenticator {
    private static final Logger log = Logger.getLogger(MyCookieAuthenticator.class.getName());

    public MyCookieAuthenticator(final Context context, final boolean optional, final String realm, final byte[] encryptSecretKey) {
        super(context, optional, realm, encryptSecretKey);
    }

    @Override
    protected CookieSetting getCredentialsCookie(final Request request, final Response response) {
        CookieSetting credentialsCookie = response.getCookieSettings()
                .getFirst(getCookieName());

        if (credentialsCookie == null) {
            credentialsCookie = new CookieSetting(getCookieName(), null);
            credentialsCookie.setAccessRestricted(true);
            credentialsCookie.setPath("/");
            response.getCookieSettings().add(credentialsCookie);
        }

        log.debug("===== Credentials =====");
        log.debug(credentialsCookie);

        return credentialsCookie;
    }

    @Override
    protected ChallengeResponse parseCredentials(String cookieValue) {
        if (cookieValue != null) {
            cookieValue = cookieValue.split(",")[0];
        }
        return super.parseCredentials(cookieValue);
    }


    @Override
    protected boolean authenticate(final Request request, final Response response) {
        boolean authenticated = false;

        final Class<? extends ServerResource> resourceClass = Utils.getTargetServerResource((WebSwitch) getApplication(), request);
        final Resource annotation = resourceClass.getAnnotation(Resource.class);

        if (annotation != null) {
            if (annotation.useKey()) {
                final CookieSetting credentialsCookie = new CookieSetting(getCookieName(), null);
                final String credentials = request.getResourceRef().getQueryAsForm().getFirstValue("key");
                if (credentials != null) {
                    credentialsCookie.setValue(credentials);
                    request.getCookies().add(credentialsCookie);
                }
            } else if (!annotation.secured()) {
                final ChallengeResponse cr = new ChallengeResponse(
                        getScheme(),
                        "guest",
                        "guest"
                );
                request.setChallengeResponse(cr);
                authenticated = true;
            }
        }

        if (!authenticated) {
            authenticated = super.authenticate(request, response);
        }

        if (!authenticated) {
            boolean html = false;

            final List<Preference<MediaType>> mediaTypes = Request.getCurrent().getClientInfo().getAcceptedMediaTypes();
            if (mediaTypes.isEmpty()) {
                html = true;
            } else {
                for (final Preference<MediaType> t : mediaTypes) {
                    final MediaType type = t.getMetadata();
                    if (type.equals(MediaType.TEXT_HTML)) {
                        html = true;
                        break;
                    }
                }
            }

            if (html) {
                //response.redirectSeeOther(new Reference(request.getRootRef(), "/"));
                response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED, "Not authorized.");
            } else {
                response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED, "Not authorized.");
            }
        }
        return authenticated;
    }

    @Override
    protected int authenticated(final Request request, final Response response) {
        if (!Utils.isInternalRequest(request)) {
            final ClientInfo clientInfo = request.getClientInfo();
            final User user = clientInfo.getUser();
            if (user != null) {
                final List<org.restlet.security.Role> roles = clientInfo.getRoles();

                final List<Role> groups = getUserRoles(request, user);
                if (groups != null) {
                    for (final Role role : groups) {
                        final List<Function> functions = role.getFunctions();
                        if (functions != null) {
                            for (final Function function : functions) {
                                roles.add(new org.restlet.security.Role(function.getPath(), null));
                            }
                        }
                    }
                }
            }
        }
        return super.authenticated(request, response);
    }

    @SuppressWarnings("unchecked")
    protected List<Role> getUserRoles(final Request request, final User user) {
        final List<Role> groups = (List<Role>) Utils.subrequest((WebSwitch) getApplication(), "roles?users.username=" + user.getIdentifier(), request);
        return groups;
    }
}
