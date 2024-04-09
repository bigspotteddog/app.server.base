package com.github.bigspotteddog.data.router;

import java.util.LinkedHashMap;

import org.restlet.resource.Finder;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.routing.TemplateRoute;

public class RouteMap extends LinkedHashMap<String, TemplateRoute> {
    private static final long serialVersionUID = 2743010519393872424L;
    private final Router router;

    public RouteMap(Router router) {
        this.router = router;
    }

    public void put(String pattern, Class<? extends ServerResource> resourceClass) {
        put(pattern, resourceClass, Template.MODE_EQUALS);
    }

    public void put(String pattern, Class<? extends ServerResource> resourceClass, int templateMode) {
        put(pattern, createRoute(pattern, resourceClass, templateMode));
    }

    public TemplateRoute createRoute(String pathTemplate, Class<? extends ServerResource> resourceClass) {
        return createRoute(pathTemplate, resourceClass, Template.MODE_EQUALS);
    }

    public TemplateRoute createRoute(String pathTemplate, Class<? extends ServerResource> resourceClass,
            int matchingMode) {
        Finder finder = router.createFinder(resourceClass);

        TemplateRoute result = new TemplateRoute(router, pathTemplate, finder);
        result.getTemplate().setMatchingMode(matchingMode);
        result.setMatchingQuery(router.getDefaultMatchingQuery());

        return result;
    }

}
