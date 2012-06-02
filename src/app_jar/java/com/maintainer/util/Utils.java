package com.maintainer.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.jasypt.util.password.StrongPasswordEncryptor;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ClientInfo;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.resource.Finder;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Router;
import org.restlet.routing.TemplateRoute;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.maintainer.data.controller.GenericController;
import com.maintainer.data.controller.Resource;
import com.maintainer.data.router.WebSwitch;

public class Utils {
    private static final String HTTP = "http";

    private static final Logger log = Logger.getLogger(Utils.class.getName());

    public static final String WILDCARD = "*";
    public static final String SYSPROP_CONFIG_PATH = "app.database.configuration";
    public static final String SYSPROP_PATH = "app.configuration";
    public static final String ORG_RESTLET_HTTP_HEADERS = "org.restlet.http.headers";

    public static String encrypt(String s) {
        StrongPasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();
        String encryptedPassword = passwordEncryptor.encryptPassword(s);
        return encryptedPassword;
    }

    public static boolean validatePassword(String incoming, String encrypted) {
        StrongPasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();
        boolean passwordValid = passwordEncryptor.checkPassword(incoming, encrypted);
        return passwordValid;
    }

    public static boolean match(String path0, String path1) {
        boolean isMatch = false;

        String[] split0 = Lists.newArrayList(Splitter.on('.').split(path0)).toArray(new String[0]);
        String[] split1 = Lists.newArrayList(Splitter.on('.').split(path1)).toArray(new String[0]);

        String method0 = split0[split0.length - 1];
        String method1 = split1[split1.length - 1];

        if (WILDCARD.equals(method1) || method0.equals(method1)) {
            int i = 0;
            for (i = 0; i < split1.length - 1; i++) {
                String segment0 = split0[i];
                String segment1 = split1[i];

                if (WILDCARD.equals(segment1)) {
                    isMatch = true;
                    break;
                }
                if (!segment1.equals(segment0)) {
                    isMatch = false;
                    break;
                }
            }

            if (split1[i].equals(split0[i])) {
                isMatch = true;
            }
        } else {
            isMatch = false;
        }

        if (isMatch) {
            log.debug("Matched: " + path0 + " to " + path1);
        }

        return isMatch;
    }

    public static Type getItemsType() {
        return new TypeToken<Collection<Map<String, Object>>>() {
        }.getType();
    }

    public static Type getItemType() {
        return new TypeToken<Map<String, Object>>() {
        }.getType();
    }

    public static Class<?> getType(Object obj) {
        Class<?> clazz = obj.getClass();
        return getGenericClass(clazz);
    }

    private static Class<?> getGenericClass(Class<?> class1) {
        Type type = class1.getGenericSuperclass();

        return getGenericClass(type);
    }

    private static Class<?> getGenericClass(Type type) {
        Class<?> result = null;

        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Type[] fieldArgTypes = pt.getActualTypeArguments();
            result = (Class<?>) fieldArgTypes[0];
        }

        return result;
    }

    /*
     * http://www.xinotes.org/notes/note/1330
     */
    private static Map<Class<?>, Class<?>> primitiveMap = new HashMap<Class<?>, Class<?>>();

    static {
        primitiveMap.put(boolean.class, Boolean.class);
        primitiveMap.put(byte.class, Byte.class);
        primitiveMap.put(char.class, Character.class);
        primitiveMap.put(short.class, Short.class);
        primitiveMap.put(int.class, Integer.class);
        primitiveMap.put(long.class, Long.class);
        primitiveMap.put(float.class, Float.class);
        primitiveMap.put(double.class, Double.class);
    }

    public static Object convert(Object value, Class<?> destClass) {
        if ((value == null) || "".equals(value)) {
            return value;
        }

        if (destClass.isPrimitive()) {
            destClass = primitiveMap.get(destClass);
        }

        String value2 = value.toString();

        if (value2.startsWith("[") && value2.endsWith("]")) {
            ArrayList<Object> list = new ArrayList<Object>();
            value2 = value2.replaceAll("[\\[\\]]", "");
            String[] split = value2.split(",");
            for (String s : split) {
                s = s.trim();
                Object convertedValue = getConvertedValue(s, destClass, s);
                log.debug("adding converted value: '" + convertedValue + "'");
                list.add(convertedValue);
            }
            value = list;
        } else {
            value = getConvertedValue(value, destClass, value2);
        }

        return value;
    }

    /*
     * end
     */

    private static Object getConvertedValue(Object value, Class<?> destClass, String incoming) {
        if (value.getClass().equals(destClass)) {
            return value;
        }

        try {
            Method m = destClass.getMethod("valueOf", String.class);
            int mods = m.getModifiers();
            if (Modifier.isStatic(mods) && Modifier.isPublic(mods)) {
                value = m.invoke(null, incoming);
            }
        } catch (NoSuchMethodException e) {
            if (destClass == Character.class) {
                value = Character.valueOf(incoming.charAt(0));
            }
        } catch (IllegalAccessException e) {
            // this won't happen
        } catch (InvocationTargetException e) {
            // when this happens, the string cannot be converted to the intended
            // type
            // we are ignoring it here - the original string will be returned.
            // But it can be re-thrown if desired!
        }
        return value;
    }

    /*
     * http://stackoverflow.com/questions/1102891/how-to-check-a-string-is-a-numeric
     * -type-in-java
     */
    public static boolean isNumeric(String str) {
        NumberFormat formatter = NumberFormat.getInstance();
        ParsePosition pos = new ParsePosition(0);
        formatter.parse(str, pos);
        return str.length() == pos.getIndex();
    }

    /*
     * end
     */

    public static ArrayList<Resource> getResources(Request request) {
        Reference resourceRef = request.getResourceRef();
        String path = resourceRef.getPath(true);
        Reference rootRef = request.getRootRef();
        String root = null;
        if (rootRef != null) {
            try {
                root = rootRef.getPath(true);
            } catch (Exception e) {
            }
        }

        if (root != null && path.startsWith(root)) {
            path = path.replaceFirst(root, "");
        }

        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        Iterable<String> segments = Splitter.on('/').split(path);

        ArrayList<Resource> resources = new ArrayList<Resource>();

        Resource resource = null;
        for (String s : segments) {
            if (resource == null) {
                resource = new Resource(s);
                resources.add(resource);
                continue;
            }
            resource.setProperty(s);
            resource = null;
        }
        return resources;
    }

    public static Class<?> getKeyType(Class<?> clazz, String key) {
        String[] split = key.split("\\.");
        for (String fieldName : split) {
            Field field = getField(clazz, fieldName);
            if (field != null) {
                clazz = field.getType();
                if (Collection.class.isAssignableFrom(clazz)) {
                    Type genericType = field.getGenericType();
                    Class<?> clazz2 = getGenericClass(genericType);
                    if (clazz2 != null) {
                        clazz = clazz2;
                    }
                }
            }
        }
        return clazz;
    }

    public static Field getField(Object obj, String fieldName) {
        Class<?> clazz = obj.getClass();
        Field field = getField(clazz, fieldName);
        return field;
    }

    public static Field getField(Class<?> clazz, String name) {
        Field field = null;
        try {
            field = clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            if (field == null) {
                Class<?> superclass = clazz.getSuperclass();
                if (superclass != null) {
                    field = getField(superclass, name);
                }
            }
        }
        return field;
    }

    public static Class<? extends ServerResource> getTargetServerResource(WebSwitch router, Request request) {
        Class<? extends ServerResource> target = null;

        Router securedRouter = router.getSecuredRouter();
        if (securedRouter == null) {
            router.createInboundRoot();
            securedRouter = router.getSecuredRouter();
        }

        Restlet next = securedRouter.getNext(request, new Response(request));
        if (next instanceof TemplateRoute) {
            TemplateRoute templateRoute = (TemplateRoute) next;
            Restlet next2 = templateRoute.getNext();
            if (next2 instanceof Finder) {
                Finder finder = (Finder) next2;
                target = finder.getTargetClass();
            }
        }

        return target;
    }

    public static Class<? extends ServerResource> getTargetServerResource(WebSwitch router, org.restlet.data.Method method, String resource) {
        return getTargetServerResource(router, new Request(method, resource));
    }

    /**
     * Scans all classes accessible from the context class loader which belong
     * to the given package and subpackages. Adapted from
     * http://snippets.dzone.com/posts/show/4831 and extended to support use of
     * JAR files
     *
     * @param packageName
     *            The base package
     * @param regexFilter
     *            an optional class name pattern.
     * @return The classes
     */
    public static Class[] getClassesInPackage(String packageName, String regexFilter) {
        Pattern regex = null;
        if (regexFilter != null)
            regex = Pattern.compile(regexFilter);

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            assert classLoader != null;
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);
            List<String> dirs = new ArrayList<String>();
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                dirs.add(resource.getFile());
            }
            TreeSet<String> classes = new TreeSet<String>();
            for (String directory : dirs) {
                classes.addAll(findClasses(directory, packageName, regex));
            }
            ArrayList<Class> classList = new ArrayList<Class>();
            for (String clazz : classes) {
                classList.add(Class.forName(clazz));
            }
            return classList.toArray(new Class[classes.size()]);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Recursive method used to find all classes in a given path (directory or
     * zip file url). Directories are searched recursively. (zip files are
     * Adapted from http://snippets.dzone.com/posts/show/4831 and extended to
     * support use of JAR files
     *
     * @param path
     *            The base directory or url from which to search.
     * @param packageName
     *            The package name for classes found inside the base directory
     * @param regex
     *            an optional class name pattern. e.g. .*Test
     * @return The classes
     */
    private static TreeSet<String> findClasses(String path, String packageName, Pattern regex) throws Exception {
        TreeSet<String> classes = new TreeSet<String>();
        if (path.startsWith("file:") && path.contains("!")) {
            String[] split = path.split("!");
            URL jar = new URL(split[0]);
            ZipInputStream zip = new ZipInputStream(jar.openStream());
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().endsWith(".class")) {
                    String className = entry.getName().replaceAll("[$].*", "").replaceAll("[.]class", "").replace('/', '.');
                    if (className.startsWith(packageName) && (regex == null || regex.matcher(className).matches()))
                        classes.add(className);
                }
            }
        }
        File dir = new File(path);
        if (!dir.exists()) {
            return classes;
        }
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file.getAbsolutePath(), packageName + "." + file.getName(), regex));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                if (regex == null || regex.matcher(className).matches()) {
                    log.debug("adding mapped class: " + className);
                    classes.add(className);
                }
            }
        }
        return classes;
    }

    public static Properties getProperties(File file) throws ExceptionInInitializerError {
        Properties properties = new Properties();
        Reader reader = null;
        try {
            reader = new FileReader(file);
            properties.load(reader);
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Failed to load " + file.getAbsolutePath() + ": " + e);
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Failed to decrypt " + file.getAbsolutePath() + ": " + e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return properties;
    }

    public static File getDatabaseConfigurationFile() {
        String path = System.getProperty(SYSPROP_CONFIG_PATH, "etc/database.properties");
        return new File(path);
    }

    public static Properties getDatabaseConfigurationProperties() {
        return getProperties(getDatabaseConfigurationFile());
    }

    public static Properties getApplicationServerProperties() {
        return getProperties(getApplicationConfigurationFile());
    }

    private static File getApplicationConfigurationFile() {
        String path = System.getProperty(SYSPROP_PATH, "etc/appserver.properties");
        return new File(path);
    }

    public static Class<?> getResourceClass(String resource) {
        return GenericController.getResourceMap().get(resource);
    }

    public static Object subrequest(WebSwitch application, String targetResource, Request request) {
        Reference rootRef = request.getRootRef();
        Object headers = request.getAttributes().get(ORG_RESTLET_HTTP_HEADERS);
        ClientInfo clientInfo = request.getClientInfo();

        return subrequest(targetResource, application, rootRef, headers, clientInfo, request.getEntity(), request.getChallengeResponse());
    }

    public static Object subrequest(String targetResource, WebSwitch application, Reference rootRef, Object headers, ClientInfo clientInfo,
            Representation entity, ChallengeResponse challengeResponse) {

        Request request2 = new Request(org.restlet.data.Method.GET, "/" + targetResource);
        request2.setEntity(entity);
        request2.setRootRef(rootRef);
        if (headers != null) {
            request2.getAttributes().put(ORG_RESTLET_HTTP_HEADERS, headers);
        }
        request2.setClientInfo(clientInfo);
        Response response2 = new Response(request2);

        Class<? extends ServerResource> serviceClass = Utils.getTargetServerResource(application, org.restlet.data.Method.GET, "/" + targetResource);
        if (serviceClass != null) {
            try {
                request2.setChallengeResponse(challengeResponse);

                log.debug("Handle: " + request2);
                application.handle(request2, response2);
                log.debug("Response: " + response2.getEntityAsText());

                Representation representation = response2.getEntity();
                if (representation != null) {
                    Gson gson = new Gson();

                    ArrayList<Resource> resources = Utils.getResources(request2);
                    if (resources != null) {
                        Resource res = resources.get(resources.size() - 1);
                        String resource2 = res.getResource();
                        Class<?> resourceClass = Utils.getResourceClass(resource2);
                        if (resourceClass != null) {
                            String text = representation.getText();
                            if (text != null) {
                                text = text.trim();
                                if (text.startsWith("[")) {
                                    ParameterizedTypeImpl type = getParameterizedListType(resourceClass);
                                    return gson.fromJson(text, type);
                                } else {
                                    if (text.startsWith("{")) {
                                        return gson.fromJson(text, resourceClass);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static ParameterizedTypeImpl getParameterizedListType(Class<?> resourceClass) {
        return new ParameterizedTypeImpl(List.class, new Type[] { resourceClass }, null);
    }

    public static boolean isInternalRequest(Request request) {
        return !request.getResourceRef().toString().startsWith(HTTP);
    }

    public static Date now() {
        return new Date();
    }
}
