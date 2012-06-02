package com.maintainer.data.security;

import java.util.List;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.SecretVerifier;

import com.maintainer.data.model.User;
import com.maintainer.data.router.WebSwitch;
import com.maintainer.util.Utils;

public class MyVerifier extends SecretVerifier {

    private WebSwitch router;

    @SuppressWarnings("unchecked")
    public int verify(Request request, String identifier, char[] secret) {

        try {
            List<User> list = (List<User>) Utils.subrequest(router, "users?username=" + identifier, request);

            if (list == null || list.isEmpty()) {
                return RESULT_INVALID;
            }

            User user = list.get(0);
            if (Utils.validatePassword(new String(secret), user.getPassword())) {
                return RESULT_VALID;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return RESULT_INVALID;
    }

    @Override
    public int verify(Request request, Response response) {
        int result = RESULT_VALID;

        if (request.getChallengeResponse() == null) {
            result = RESULT_MISSING;
        } else {
            if (Utils.isInternalRequest(request)) {
                result = RESULT_VALID;
            } else {
                String identifier = getIdentifier(request, response);
                char[] secret = getSecret(request, response);
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
    public int verify(String identifier, char[] secret) {
        // will not be called
        return 0;
    }

    public void setApplication(WebSwitch application) {
        this.router = application;
    }

    public WebSwitch getApplication() {
        return router;
    }
}
