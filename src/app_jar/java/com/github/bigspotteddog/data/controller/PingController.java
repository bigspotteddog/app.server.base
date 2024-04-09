package com.github.bigspotteddog.data.controller;

import org.restlet.data.Form;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.restlet.security.User;

import com.github.bigspotteddog.util.Utils;
import com.google.gson.Gson;

public class PingController extends ServerResource {
    private static final String LOGGED_IN_AS_GUEST = "Logged in as guest";

    @Post(":html")
    @Get(":html")
    public Representation ping(final Form form) {
        return ping();
    }

    @Post("json")
    @Get("json")
    public Representation ping(final Representation rep) throws Exception {
        return ping();
    }

    public Representation ping() {
        String identifier = LOGGED_IN_AS_GUEST;
        final User user = getRequest().getClientInfo().getUser();
        if (user != null) {
            identifier = user.getIdentifier();
        }
        final Gson gson = Utils.getGson();
        final Ping welcome = new Ping(identifier);
        final String json = gson.toJson(welcome);
        return new JsonRepresentation(json);
    }

    public class Ping {
        private String ping = null;

        public Ping(final String identifier) {
            this.ping = identifier;
        }

        public String getWelcome() {
            return ping;
        }
    }
}
