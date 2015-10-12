package com.maintainer.data.router;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.StringRepresentation;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.routing.TemplateRoute;

import com.maintainer.data.controller.CheckPermissionController;
import com.maintainer.data.controller.GenericController;
import com.maintainer.data.controller.LoginController;
import com.maintainer.data.controller.LogoutController;
import com.maintainer.data.controller.PingController;
import com.maintainer.data.controller.UserDefinedClassController;
import com.maintainer.data.controller.UserResourceController;
import com.maintainer.data.model.MapEntityImpl;
import com.maintainer.data.model.MyClass;
import com.maintainer.data.model.ThreadLocalInfo;
import com.maintainer.data.provider.DataProvider;
import com.maintainer.data.provider.DataProviderFactory;
import com.maintainer.data.security.MyCookieAuthenticator;
import com.maintainer.data.security.MyEnroler;
import com.maintainer.data.security.MyVerifier;
import com.maintainer.data.security.PermissionsRoleAuthorizer;
import com.maintainer.util.Utils;

public class WebSwitch extends Application {
    private static final Logger log = Logger.getLogger(WebSwitch.class.getName());
    private static final String GENERIC = "/{resource}";
    private static final int FIVE_MINUTES = 300;
    private boolean isSecured = true;
    private Router securedRouter = null;
    private PermissionsRoleAuthorizer roleAuthorizer;
    private boolean isTransactional;
    private MyCookieAuthenticator co;

    public WebSwitch() {
        this(true);
    }

    public WebSwitch(final boolean isSecured) {
        log.fine("Initialize WebSwitch");

        initializeAppServerLogging();

        this.isSecured = isSecured;
        this.isTransactional = true;
        final Properties properties = Utils.getApplicationServerProperties();
        final String secured = (String) properties.get("appserver.application.secured");
        if (secured != null) {
            this.isSecured = Boolean.parseBoolean(secured);
        }

        final String transactional = (String) properties.get("appserver.application.transactional");
        if (transactional != null) {
            this.isTransactional = Boolean.parseBoolean(transactional);
        }

        initializeDefaultDataProvider();
        initializeDataProviders();

        try {
            start();
        } catch (final Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void initializeAppServerLogging() {
    }

    protected String getApplicationName() {
        final Properties properties = Utils.getApplicationServerProperties();
        final String name = (String) properties.get("appserver.application.name");
        if (name != null) {
            return name;
        }
        return "unknown";
    }

    protected int getMaxCookieAge() {
        final Properties properties = Utils.getApplicationServerProperties();
        final String age = (String) properties.get("appserver.application.max.cookie.age");
        if (age == null) {
            return FIVE_MINUTES;
        }
        return Integer.parseInt(age);
    }

    public void attachAdditionalRoutes(final Router router) {}
    protected DataProvider<?> registerDefaultDataProvider() { return null; }
    protected void registerDataProviders(final Map<Class<?>, DataProvider<?>> dataProviders) {}
    protected void registerControllerClasses(final Map<String, Class<?>> controllerClasses) {}

    protected void initializeDefaultDataProvider() {
        final DataProviderFactory factory = DataProviderFactory.instance();

        final DataProvider<?> dataProvider = registerDefaultDataProvider();
        if (dataProvider != null) {
            factory.setDefaultDataProvider(dataProvider);
        }
    }

    @SuppressWarnings("unchecked")
    protected void initializeControllerClasses() {
        final Map<String, Class<?>> map = new LinkedHashMap<String, Class<?>>();
        registerControllerClasses(map);
        for (final Entry<String, Class<?>> e : map.entrySet()) {
            GenericController.register(e.getKey(), e.getValue());
        }
        GenericController.register("classes", MyClass.class);

        DataProvider<MyClass> dataProvider = (DataProvider<MyClass>) DataProviderFactory.instance().getDataProvider(MyClass.class);
        try {
            List<MyClass> list = dataProvider.getAll(MyClass.class);
            for (MyClass c : list) {
                try {
                    Class.forName(c.getName());
                } catch (Exception e) {
                    GenericController.register(c.getName(), MapEntityImpl.class);
                }
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    protected void initializeDataProviders() {
        final Map<Class<?>, DataProvider<?>> map = new LinkedHashMap<Class<?>, DataProvider<?>>();
        registerDataProviders(map);
        final DataProviderFactory factory = DataProviderFactory.instance();
        for (final Entry<Class<?>, DataProvider<?>> e : map.entrySet()) {
            factory.register(e.getKey(), e.getValue());
        }
    }

    @Override
    public Restlet createInboundRoot() {
        log.fine("createInboundRoot");

        initializeControllerClasses();

        final Router router = new Router(getContext());
        securedRouter = router;

        attachRoutes(router);

        if (isSecured) {
            co = getCookieAuthenticator(getContext());
            co.setLoginFormPath("/");
            co.setVerifier(getVerifier());
            co.setEnroler(getEnroler(getApplicationName()));

            final int maxCookieAge = getMaxCookieAge();
            co.setMaxCookieAge(maxCookieAge);
            log.info("Set max cookie age: " + maxCookieAge);

            roleAuthorizer = new PermissionsRoleAuthorizer(getApplicationName());
            roleAuthorizer.setNext(router);

            co.setNext(roleAuthorizer);
            return co;
        }

        return router;
    }

    public String getCrentialsCookieName() {
        return co.getCookieName();
    }

    protected MyCookieAuthenticator getCookieAuthenticator(final Context context) {
        return new MyCookieAuthenticator(context, false, "My cookie realm", "MyExtraSecretKey".getBytes());
    }

    protected MyEnroler getEnroler(final String applicationName) {
        return new MyEnroler(applicationName);
    }

    protected MyVerifier getVerifier() {
        final MyVerifier verifier = new MyVerifier();
        verifier.setApplication(this);
        return verifier;
    }

    protected void attachRoutes(final Router router) {
        final RouteMap routes = new RouteMap(router);
        routes.put("/ping", PingController.class);
        routes.put("/check", CheckPermissionController.class, Template.MODE_STARTS_WITH);
        routes.put("/login", LoginController.class);
        routes.put("/logout", LogoutController.class);
        routes.put("/users", UserResourceController.class, Template.MODE_STARTS_WITH);
        routes.put("/classes", UserDefinedClassController.class, Template.MODE_STARTS_WITH);
        routes.put(GENERIC, getGenericControllerClass(), Template.MODE_STARTS_WITH);

        fillRoutes(routes);

        final TemplateRoute generic = routes.remove(GENERIC);
        routes.put(GENERIC, generic);

        for(final Entry<String, TemplateRoute> e : routes.entrySet()) {
            final TemplateRoute route = e.getValue();
            addRoute(route);
        }
    }

    protected Class<? extends GenericController> getGenericControllerClass() {
        return GenericController.class;
    }

    protected void addRoute(final TemplateRoute route) {
        securedRouter.getRoutes().add(route);
    }

    protected void fillRoutes(final RouteMap routes) {}

    public Router getSecuredRouter() {
        return securedRouter;
    }

    @Override
    public void handle(final Request request, final Response response) {
        try {
            ThreadLocalInfo.getInfo().setPath(request.getOriginalRef().getPath());
            begin(request);
            super.handle(request, response);
            commit(request);
        } catch (final Exception e) {
            response.setStatus(Status.SERVER_ERROR_INTERNAL, e, e.getMessage());
            response.setEntity(new StringRepresentation(e.getMessage(), MediaType.TEXT_PLAIN));
        }
    }

    private void commit(final Request request) {
        if (isTransactional  && !Utils.isInternalRequest(request)) {
            try {
                DataProviderFactory.instance().getDefaultDataProvider().commitTransaction();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void begin(final Request request) {
        if (isTransactional && !Utils.isInternalRequest(request)) {
            try {
                DataProviderFactory.instance().getDefaultDataProvider().beginTransaction();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public PermissionsRoleAuthorizer getRoleAuthorizer() {
        return roleAuthorizer;
    }

    public boolean isSecured() {
        return isSecured;
    }

    public boolean isTransactional() {
        return isTransactional;
    }

    public void setTransactional(final boolean isTransactional) {
        this.isTransactional = isTransactional;
    }

    public void setSecured(final boolean isSecured) {
        this.isSecured = isSecured;
    }
}
