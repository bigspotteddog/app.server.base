package com.maintainer.data.controller;

import org.restlet.data.CookieSetting;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.restlet.security.User;
import org.restlet.util.Series;

import com.google.gson.Gson;

public class LogoutController extends ServerResource {
    @Post
    public Representation process() {
        Series<CookieSetting> cookieSettings = getResponse().getCookieSettings();
        return logout();
    }

    public Representation logout() {
        String identifier = "unknown";
        User user = getRequest().getClientInfo().getUser();
        if (user != null) {
            identifier = user.getIdentifier();
        }
        Gson gson = new Gson();
        Goodbye welcome = new Goodbye(identifier);
        String json = gson.toJson(welcome);
        return new JsonRepresentation(json);
    }

    public class Goodbye {
        private String goodbye = null;

        public Goodbye(String identifier) {
            this.goodbye = identifier;
        }

        public String getGoodbye() {
            return goodbye;
        }
    }

}
