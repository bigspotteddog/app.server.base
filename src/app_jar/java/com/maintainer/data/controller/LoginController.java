package com.maintainer.data.controller;

import org.restlet.data.Form;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.restlet.security.User;

import com.google.gson.Gson;

public class LoginController extends ServerResource {
    @Post(":html")
    public Representation login(Form form) {
        return login();
    }

    @Post("json")
    public Representation login(Representation rep) throws Exception {
        return login();
    }

    public Representation login() {
        String identifier = "unknown";
        User user = getRequest().getClientInfo().getUser();
        if (user != null) {
            identifier = user.getIdentifier();
        }
        Gson gson = new Gson();
        Welcome welcome = new Welcome(identifier);
        String json = gson.toJson(welcome);
        return new JsonRepresentation(json);
    }

    public class Welcome {
        private String welcome = null;

        public Welcome(String identifier) {
            this.welcome = identifier;
        }

        public String getWelcome() {
            return welcome;
        }
    }
}
