package com.maintainer.data.controller;

import org.restlet.data.Form;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.restlet.security.User;

import com.google.gson.Gson;

public class PingController extends ServerResource {
    @Post(":html")
    @Get(":html")
    public Representation ping(Form form) {
        return ping();
    }

    @Post("json")
    @Get("json")
    public Representation ping(Representation rep) throws Exception {
        return ping();
    }

    public Representation ping() {
        String identifier = "unknown";
        User user = getRequest().getClientInfo().getUser();
        if (user != null) {
            identifier = user.getIdentifier();
        }
        Gson gson = new Gson();
        Ping welcome = new Ping(identifier);
        String json = gson.toJson(welcome);
        return new JsonRepresentation(json);
    }

    public class Ping {
        private String ping = null;

        public Ping(String identifier) {
            this.ping = identifier;
        }

        public String getWelcome() {
            return ping;
        }
    }
}
