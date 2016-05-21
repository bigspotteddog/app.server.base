package com.maintainer.data.controller;

import org.restlet.data.Form;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.restlet.security.User;

import com.google.gson.Gson;
import com.maintainer.util.Utils;

public class LoginController extends ServerResource {
    @Post(":html")
    public Representation login(final Form form) {
        return login();
    }

    @Post("json")
    public Representation login(final Representation rep) throws Exception {
        return login();
    }

    public Representation login() {
        String identifier = "unknown";
        final User user = getRequest().getClientInfo().getUser();
        if (user != null) {
            identifier = user.getIdentifier();
        }
        final Gson gson = Utils.getGson();
        final Ping ping = new Ping(user);
        final String json = gson.toJson(ping);
        return new JsonRepresentation(json);
    }

    public class Ping {
        private String username = null;

        public Ping(final User user) {
            this.username = user.getIdentifier();
        }

        public String getWelcome() {
            return username;
        }
    }
}
