package com.github.bigspotteddog.data.security;

import java.util.List;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.SecretVerifier;

import com.github.bigspotteddog.util.Utils;
import com.github.bigspotteddog.data.model.User;
import com.github.bigspotteddog.data.router.WebSwitch;

public class MyVerifier extends SecretVerifier {

    private WebSwitch router;

    @SuppressWarnings("unchecked")
    public int verify(final Request request, final String identifier, final char[] secret) {

        try {
            final List<User> list = (List<User>) Utils.subrequest(router, "users?username=" + identifier, request);
            final User user = list.isEmpty() ? null : list.get(0);

            if (user == null) {
                return RESULT_INVALID;
            }

            request.getAttributes().put(Utils._USER_, user);

            if (Utils.validatePassword(new String(secret), user.getPassword())) {
                return RESULT_VALID;
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return RESULT_INVALID;
    }

    @Override
    public int verify(final Request request, final Response response) {
        int result = RESULT_VALID;

        if (request.getChallengeResponse() == null) {
            result = RESULT_MISSING;
        } else {
            if (Utils.isInternalRequest(request)) {
                result = RESULT_VALID;
            } else {
                final String identifier = getIdentifier(request, response);
                final char[] secret = getSecret(request, response);
                result = verify(request, identifier, secret);

                if (result == RESULT_VALID) {
                    request.getClientInfo().setUser(createUser(identifier));
                }
            }
        }

        if (result == RESULT_VALID) {
            if (request.getClientInfo().getUser() != null) {
                request.getClientInfo().getUser().setSecret(getSecret(request, response));
            }
        }

        return result;
    }

    @Override
    public int verify(final String identifier, final char[] secret) {
        // will not be called
        return 0;
    }

    public void setApplication(final WebSwitch application) {
        this.router = application;
    }

    public WebSwitch getApplication() {
        return router;
    }
}
