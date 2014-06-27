package com.maintainer.data.controller;

import org.restlet.data.CookieSetting;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.restlet.security.User;
import org.restlet.util.Series;

import com.google.gson.Gson;
import com.maintainer.util.Utils;

public class LogoutController extends ServerResource {
    @SuppressWarnings("unused")
    @Get
    @Post
    public Representation process() {
        final Series<CookieSetting> cookieSettings = getResponse().getCookieSettings();
        return logout();
    }

    public Representation logout() {
        String identifier = "unknown";
        final User user = getRequest().getClientInfo().getUser();
        if (user != null) {
            identifier = user.getIdentifier();
        }

        final Gson gson = Utils.getGson();
        final Goodbye welcome = new Goodbye(identifier);
        final String json = gson.toJson(welcome);
        return new JsonRepresentation(json);
    }

    public class Goodbye {
        private String goodbye = null;

        public Goodbye(final String identifier) {
            this.goodbye = identifier;
        }

        public String getGoodbye() {
            return goodbye;
        }
    }

}
