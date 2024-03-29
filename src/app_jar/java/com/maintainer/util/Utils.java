package com.maintainer.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLDecoder;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.maintainer.data.controller.GenericController;
import com.maintainer.data.controller.Resource;
import com.maintainer.data.model.EntityImpl;
import com.maintainer.data.model.MapEntityImpl;
import com.maintainer.data.model.MyClass;
import com.maintainer.data.model.MyField;
import com.maintainer.data.model.ThreadLocalInfo;
import com.maintainer.data.model.User;
import com.maintainer.data.model.UserBase;
import com.maintainer.data.provider.DataProvider;
import com.maintainer.data.provider.DataProviderFactory;
import com.maintainer.data.provider.Key;
import com.maintainer.data.provider.Query;
import com.maintainer.data.router.WebSwitch;

public class Utils {
    public static final String _USER_ = "_user_";
    private static final String APPLICATION_ENCODE_KEYS = "application.encode.keys";
    private static final String UTF_8 = "UTF-8";
    private static final String REMAINING = "remaining";
    private static final String TEMPLATE_PLACEHOLDER = "([^/\\?]+)";
    private static final String TEMPLATE_WORD = "\\{\\{(\\w+)\\}\\}";

    private static final String HIBERNATE_MODEL_PACKAGE = "hibernate.model.package";

    private static final Object[] NO_PARAMS = new Object[0];

    private static final Class<?>[] NO_ARGS = new Class<?>[0];

    private static final String HTTP = "http";

    private static final Logger log = Logger.getLogger(Utils.class.getName());

    public static final String WILDCARD = "*";
    public static final String SYSPROP_CONFIG_PATH = "app.database.configuration";
    public static final String SYSPROP_PATH = "app.configuration";
    public static final String ORG_RESTLET_HTTP_HEADERS = "org.restlet.http.headers";

    private static String packageName;
    private static Boolean isEncodeKeys = null;

    private static SimpleDateFormat sdf = new SimpleDateFormat("MM.dd.yy HH:mm z");
    private static final SimpleDateFormat sdf2 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
    private static final SimpleDateFormat sdf3 = new SimpleDateFormat("MM/dd/yyyy");
    private static final SimpleDateFormat sdf4 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final SimpleDateFormat sdf5 = new SimpleDateFormat("yyyy-MM-dd");

    public static final void setDateSerializationFormat(final SimpleDateFormat incoming) {
        sdf = incoming;
    }

    public static String encrypt(final String s) {
        final StrongPasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();
        final String encryptedPassword = passwordEncryptor.encryptPassword(s);
        return encryptedPassword;
    }

    public static boolean validatePassword(final String incoming, final String encrypted) {
        final StrongPasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();
        final boolean passwordValid = passwordEncryptor.checkPassword(incoming, encrypted);
        return passwordValid;
    }

    public static boolean match(final String path0, final String path1) {
        boolean isMatch = false;

        final String[] split0 = Lists.newArrayList(Splitter.on('.').split(path0)).toArray(new String[0]);
        final String[] split1 = Lists.newArrayList(Splitter.on('.').split(path1)).toArray(new String[0]);

        final String method0 = split0[split0.length - 1];
        final String method1 = split1[split1.length - 1];

        if (WILDCARD.equals(method1) || method0.equals(method1)) {
            int i = 0;
            for (i = 0; i < split1.length - 1; i++) {
                final String segment0 = split0[i];
                final String segment1 = split1[i];

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
            log.fine("Matched: " + path0 + " to " + path1);
        }

        return isMatch;
    }

    public static Type getItemsType() {
        return new TypeToken<List<Map<String, Object>>>() {}.getType();
    }

    public static Type getItemType() {
        return new TypeToken<Map<String, Object>>() {}.getType();
    }

    public static Class<?> getType(final Object obj) {
        final Class<?> clazz = obj.getClass();
        return getGenericClass(clazz);
    }

    private static Class<?> getGenericClass(final Class<?> class1) {
        final Type type = class1.getGenericSuperclass();

        return getGenericClass(type);
    }

    private static Class<?> getGenericClass(final Type type) {
        Class<?> result = null;

        if (type instanceof ParameterizedType) {
            final ParameterizedType pt = (ParameterizedType) type;
            final Type[] fieldArgTypes = pt.getActualTypeArguments();
            result = (Class<?>) fieldArgTypes[0];
        }

        return result;
    }

    /*
     * http://www.xinotes.org/notes/note/1330
     */
    private static Map<Class<?>, Class<?>> primitiveMap = new HashMap<Class<?>, Class<?>>();

    private static Gson gson = null;
    private static Gson gsonPretty = null;

    public static Gson getGson() {
        if (gson == null) {
            gson = getGsonBuilder().create();
        }
        return gson;
    }

    public static Gson getGsonPretty() {
        if (gsonPretty  == null) {
            gsonPretty = getGsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        }
        return gsonPretty;
    }

    public static Date convertToDate(final String asString) {
        if (Utils.isEmpty(asString)) return null;

        Date date = null;

        try {
            date = sdf.parse(asString);
        } catch (final ParseException e) {
            try {
                date = sdf2.parse(asString);
            } catch (final ParseException e1) {
                try {
                    date = sdf3.parse(asString);
                } catch (final ParseException e2) {
                    try {
                        date = sdf4.parse(asString);
                    } catch (final ParseException e3) {
                        try {
                            date = sdf5.parse(asString);
                        } catch (final ParseException e4) {
                            try {
                                final long time = Long.parseLong(asString);
                                date = new Date(time);
                            } catch( final Exception e5) {
                                e5.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        return date;
    }

    public static GsonBuilder getGsonBuilder() {
        // from stackoverflow: http://stackoverflow.com/a/6875295/591203
        final JsonSerializer<Date> dateSerializer = new JsonSerializer<Date>() {
            @Override
            public JsonElement serialize(final Date src, final Type typeOfSrc, final JsonSerializationContext context) {
                // return src == null ? null : new JsonPrimitive(sdf.format(src));
                return src == null ? null : new JsonPrimitive(src.getTime());
            }
        };

        final JsonDeserializer<Date> dateDeserializer = new JsonDeserializer<Date>() {
            @Override
            public Date deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
                if (json == null) return null;

                final String asString = json.getAsString();

                Date date = Utils.convertToDate(asString);
                return date;
            }
        };

        final JsonSerializer<BigDecimal> bigDecimalSerializer = new JsonSerializer<BigDecimal>() {
            @Override
            public JsonElement serialize(final BigDecimal src, final Type typeOfSrc, final JsonSerializationContext context) {
                if (src == null) {
                    return null;
                }

                Number number = null;
                if (src.scale() == 0) {
                    number = src.intValue();
                } else {
                    number = src.doubleValue();
                }

                return new JsonPrimitive(number);
            }

        };

        final JsonDeserializer<BigDecimal> bigDecimalDeserializer = new JsonDeserializer<BigDecimal>() {
            @Override
            public BigDecimal deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
                BigDecimal number = null;
                try {
                    number = new BigDecimal(json.getAsString());
                } catch (final Exception e) {
                    number = BigDecimal.ZERO;
                }
                return number;
            }
        };

        final JsonSerializer<MapEntityImpl> jsonMapEntityImplSerializer = new JsonSerializer<MapEntityImpl>() {
            @Override
            public JsonElement serialize(final MapEntityImpl map, final Type typeOfSrc, final JsonSerializationContext context) {
                JsonObject object = new JsonObject();
                object.add("id", context.serialize(map.getId()));
                object.add("identity", context.serialize(map.getIdentity()));
                for (Entry<String, Object> e : map.entrySet()) {
                    String key = e.getKey();
                    Object value = e.getValue();

                    object.add(key, context.serialize(value));
                }
                return object;
            }
        };

        final JsonSerializer<JsonString> jsonStringSerializer = new JsonSerializer<JsonString>() {
            @Override
            public JsonElement serialize(final JsonString string, final Type typeOfSrc, final JsonSerializationContext context) {
                return new JsonParser().parse(string.getString());
            }
        };

        final JsonDeserializer<JsonString> jsonStringDeserializer = new JsonDeserializer<JsonString>() {
            @Override
            public JsonString deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
                return new JsonString(json.toString());
            }
        };

        final JsonSerializer<Key> keySerializer = new JsonSerializer<Key>() {
            @Override
            public JsonElement serialize(final Key key, final Type typeOfSrc, final JsonSerializationContext context) {
                return null;
            }
        };

        final JsonDeserializer<Key> keyDeserializer = new JsonDeserializer<Key>() {
            @Override
            public Key deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
                return Key.fromString(json.toString());
            }
        };

        final JsonSerializer<UserBase> userSerializer = new JsonSerializer<UserBase>() {
            @Override
            public JsonElement serialize(final UserBase user, final Type typeOfSrc, final JsonSerializationContext context) {
                if (user == null) return null;

                final JsonObject object = new JsonObject();
                if (user.getKey() != null) {
                    object.add("id", new JsonPrimitive(user.getKey().toString()));
                }

                if (user.getUsername() != null) {
                    object.add("username", new JsonPrimitive(user.getUsername()));
                }

                return object;
            }
        };

        final JsonSerializer<Double> doubleSerializer = new JsonSerializer<Double>() {
            @Override
            public JsonElement serialize(final Double src, final Type typeOfSrc, final JsonSerializationContext context) {
                if (src == null) {
                    return null;
                }

                final Number number = src.doubleValue();

                return new JsonPrimitive(number);
            }

        };

        final JsonDeserializer<Double> doubleDeserializer = new JsonDeserializer<Double>() {
            @Override
            public Double deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
                BigDecimal number = null;
                try {
                    number = new BigDecimal(json.getAsString());
                } catch (final Exception e) {
                    return null;
                }
                return number.doubleValue();
            }
        };

        final GsonBuilder builder = new GsonBuilder()
        .registerTypeAdapter(Date.class, dateSerializer)
        .registerTypeAdapter(Date.class, dateDeserializer)
        .registerTypeAdapter(BigDecimal.class, bigDecimalSerializer)
        .registerTypeAdapter(BigDecimal.class, bigDecimalDeserializer)
        .registerTypeAdapter(MapEntityImpl.class, jsonMapEntityImplSerializer)
        .registerTypeAdapter(JsonString.class, jsonStringDeserializer)
        .registerTypeAdapter(JsonString.class, jsonStringSerializer)
        .registerTypeAdapter(Key.class, keyDeserializer)
        .registerTypeAdapter(Key.class, keySerializer)
        .registerTypeAdapter(User.class, userSerializer)
        .registerTypeAdapter(Double.class, doubleSerializer)
        .registerTypeAdapter(Double.class, doubleDeserializer)
        .serializeSpecialFloatingPointValues();

        return builder;
    }

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

    @SuppressWarnings("rawtypes")
    public static Class getType(final Class clazz) {
        final Class<?> class1 = primitiveMap.get(clazz);
        if (class1 == null) {
            return clazz;
        }
        return class1;
    }

    public static Object convert(Object value, Class<?> destClass) throws Exception {
        if ((value == null) || value.toString().trim().length() == 0) {
            return value;
        }

        if (destClass.isPrimitive()) {
            destClass = primitiveMap.get(destClass);
        }

        String value2 = value.toString();

        if (value2.startsWith("[") && value2.endsWith("]")) {
            final ArrayList<Object> list = new ArrayList<Object>();
            value2 = value2.replaceAll("[\\[\\]]", "");
            final String[] split = value2.split(",");
            for (String s : split) {
                s = s.trim();
                final Object convertedValue = getConvertedValue(s, destClass, s);
                log.fine("adding converted value: '" + convertedValue + "'");
                list.add(convertedValue);
            }
            value = list;
        } else {
            if (Date.class.equals(destClass) && Date.class.isAssignableFrom(value.getClass())) {

            } else {
                if (Date.class.equals(destClass)) {
                    value = convertToDate(value2);
                }
                value = getConvertedValue(value, destClass, value2);
            }
        }

        return value;
    }

    /*
     * end
     */

    private static Object getConvertedValue(Object value, final Class<?> destClass, final String incoming) throws Exception {
        if (value.getClass().equals(destClass)) {
            return value;
        }

        try {
            if (Integer.class.isAssignableFrom(destClass)) {
                value = new BigDecimal(incoming).intValue();
            } else if (Double.class.isAssignableFrom(destClass)) {
                value = new BigDecimal(incoming).doubleValue();
            } else if (Float.class.isAssignableFrom(destClass)) {
                value = new BigDecimal(incoming).floatValue();
            } else if (Long.class.isAssignableFrom(destClass)) {
                value = new BigDecimal(incoming).longValue();
            } else {
                final Method m = destClass.getMethod("valueOf", String.class);
                final int mods = m.getModifiers();
                if (Modifier.isStatic(mods) && Modifier.isPublic(mods)) {
                    value = m.invoke(null, incoming);
                }
            }
        } catch (final NoSuchMethodException e) {
            if (destClass == Character.class) {
                value = Character.valueOf(incoming.charAt(0));
            }
        } catch (final IllegalAccessException e) {
            // this won't happen
        } catch (final InvocationTargetException e) {
            // when this happens, the string cannot be converted to the intended type
            // we are not ignoring it here - the original string will be returned.
            // It will be re-thrown as desired!
            throw e;
        }
        return value;
    }

    /*
     * http://stackoverflow.com/questions/1102891/how-to-check-a-string-is-a-numeric
     * -type-in-java
     */
    public static boolean isNumeric(final String str) {
        final NumberFormat formatter = NumberFormat.getInstance();
        final ParsePosition pos = new ParsePosition(0);
        formatter.parse(str, pos);
        return str.length() == pos.getIndex();
    }

    /*
     * end
     */

    public static ArrayList<Resource> getResources(final Request request) {
        final String path = cleansPath(request);
        return getResources(path);
    }

    public static String cleansPath(final Request request) {
        final Reference resourceRef = request.getResourceRef();
        String path = resourceRef.getPath();
        final Reference rootRef = request.getRootRef();
        String root = null;
        if (rootRef != null) {
            try {
                root = rootRef.getPath(true);
            } catch (final Exception e) {
            }
        }

        if (root != null && path.startsWith(root)) {
            path = path.replaceFirst(root, "");
        }

        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    public static ArrayList<Resource> getResources(final String path) {
        final Iterable<String> segments = Splitter.on('/').split(path);

        final ArrayList<Resource> resources = new ArrayList<Resource>();

        Resource resource = null;
        for (String s : segments) {
            try {
                s = URLDecoder.decode(s, UTF_8);
            } catch (final UnsupportedEncodingException e) {
                e.printStackTrace();
            }
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

    public static Class<?> getKeyType(Class<?> clazz, final String key) throws Exception {
        final String[] split = key.split("\\.");
        for (final String fieldName : split) {
            final MyField field = Utils.getField(clazz, fieldName);
            if (field != null) {
                clazz = field.getType();
                if (Collection.class.isAssignableFrom(clazz)) {
                    final Type genericType = field.getGenericType();
                    final Class<?> clazz2 = getGenericClass(genericType);
                    if (clazz2 != null) {
                        clazz = clazz2;
                    }
                }
            }
        }
        return clazz;
    }

    // TODO: Add support for recursion.
    public static Class<?> getKeyType(final String className, final String key) throws Exception {
        Class<?> clazz = null;
        final String[] split = key.split("\\.");
        for (final String fieldName : split) {
            final MyField field = Utils.getField(className, fieldName);
            if (field != null) {
                clazz = field.getType();
            }
            break;
        }
        return clazz;
    }

    public static Class<? extends ServerResource> getTargetServerResource(final WebSwitch router, final Request request) {
        Class<? extends ServerResource> target = null;

        Router securedRouter = router.getSecuredRouter();
        if (securedRouter == null) {
            router.createInboundRoot();
            securedRouter = router.getSecuredRouter();
        }

        final Restlet next = securedRouter.getNext(request, new Response(request));
        if (next instanceof TemplateRoute) {
            final TemplateRoute templateRoute = (TemplateRoute) next;
            final Restlet next2 = templateRoute.getNext();
            if (next2 instanceof Finder) {
                final Finder finder = (Finder) next2;
                target = finder.getTargetClass();
            }
        }

        return target;
    }

    public static Class<? extends ServerResource> getTargetServerResource(final WebSwitch router, final org.restlet.data.Method method, final String resource) {
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
    @SuppressWarnings("rawtypes")
    public static Class<?>[] getClassesInPackage(final String packageName, final String regexFilter) {
        Pattern regex = null;
        if (regexFilter != null) {
            regex = Pattern.compile(regexFilter);
        }

        try {
            final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            assert classLoader != null;
            final String path = packageName.replace('.', '/');
            final Enumeration<URL> resources = classLoader.getResources(path);
            final List<String> dirs = new ArrayList<String>();
            while (resources.hasMoreElements()) {
                final URL resource = resources.nextElement();
                dirs.add(resource.getFile());
            }
            final TreeSet<String> classes = new TreeSet<String>();
            for (final String directory : dirs) {
                classes.addAll(findClasses(directory, packageName, regex));
            }
            final ArrayList<Class<?>> classList = new ArrayList<Class<?>>();
            for (final String clazz : classes) {
                classList.add(Class.forName(clazz));
            }
            return classList.toArray(new Class[classes.size()]);
        } catch (final Exception e) {
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
    private static TreeSet<String> findClasses(final String path, final String packageName, final Pattern regex) throws Exception {
        final TreeSet<String> classes = new TreeSet<String>();
        if (path.startsWith("file:") && path.contains("!")) {
            final String[] split = path.split("!");
            final URL jar = new URL(split[0]);
            final ZipInputStream zip = new ZipInputStream(jar.openStream());
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().endsWith(".class")) {
                    final String className = entry.getName().replaceAll("[$].*", "").replaceAll("[.]class", "").replace('/', '.');
                    if (className.startsWith(packageName) && (regex == null || regex.matcher(className).matches())) {
                        classes.add(className);
                    }
                }
            }
        }
        final File dir = new File(path);
        if (!dir.exists()) {
            return classes;
        }
        final File[] files = dir.listFiles();
        for (final File file : files) {
            if (file.isDirectory()) {
                assert !file.getName().contains(".");
                classes.addAll(findClasses(file.getAbsolutePath(), packageName + "." + file.getName(), regex));
            } else if (file.getName().endsWith(".class")) {
                final String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                if (regex == null || regex.matcher(className).matches()) {
                    log.fine("adding mapped class: " + className);
                    classes.add(className);
                }
            }
        }
        return classes;
    }

    public static Properties getProperties(final File file) {
        final Properties properties = new Properties();
        Reader reader = null;
        try {
            reader = new FileReader(file);
            properties.load(reader);
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
        }
        return properties;
    }

    public static File getLoggingConfigurationFile() {
        final String path = System.getProperty(SYSPROP_CONFIG_PATH, "etc/log4j.properties");
        return new File(path);
    }

    public static Properties getLoggingConfigurationProperties() {
        return getProperties(getLoggingConfigurationFile());
    }

    public static File getDatabaseConfigurationFile() {
        final String path = System.getProperty(SYSPROP_CONFIG_PATH, "etc/database.properties");
        return new File(path);
    }

    public static Properties getDatabaseConfigurationProperties() {
        return getProperties(getDatabaseConfigurationFile());
    }

    public static Properties getApplicationServerProperties() {
        return getProperties(getApplicationConfigurationFile());
    }

    private static File getApplicationConfigurationFile() {
        final String path = System.getProperty(SYSPROP_PATH, "etc/appserver.properties");
        return new File(path);
    }

    public static Class<?> getResourceClass(final String resource) {
        return GenericController.getResourceMap().get(resource);
    }

    public static Object subrequest(final WebSwitch application, final String targetResource, final Request request) {
        final Reference rootRef = request.getRootRef();
        final Object headers = request.getAttributes().get(ORG_RESTLET_HTTP_HEADERS);
        final ClientInfo clientInfo = request.getClientInfo();

        return subrequest(targetResource, application, rootRef, headers, clientInfo, request.getEntity(), request.getChallengeResponse());
    }

    public static Object subrequest(final String targetResource, final WebSwitch application, final Reference rootRef, final Object headers, final ClientInfo clientInfo,
            final Representation entity, final ChallengeResponse challengeResponse) {

        final Request request2 = new Request(org.restlet.data.Method.GET, "/" + targetResource);
        request2.setEntity(entity);
        request2.setRootRef(rootRef);
        if (headers != null) {
            request2.getAttributes().put(ORG_RESTLET_HTTP_HEADERS, headers);
        }
        request2.setClientInfo(clientInfo);
        final Response response2 = new Response(request2);

        final Class<? extends ServerResource> serviceClass = Utils.getTargetServerResource(application, org.restlet.data.Method.GET, "/" + targetResource);
        if (serviceClass != null) {
            try {
                request2.setChallengeResponse(challengeResponse);

                log.fine("Handle: " + request2);
                application.handle(request2, response2);
                log.fine("Response: " + response2.getEntityAsText());

                final Representation representation = response2.getEntity();
                if (representation != null) {
                    final Gson gson = getGson();

                    final ArrayList<Resource> resources = Utils.getResources(request2);
                    if (resources != null) {
                        final Resource res = resources.get(resources.size() - 1);
                        final String resource2 = res.getResource();
                        final Class<?> resourceClass = Utils.getResourceClass(resource2);
                        if (resourceClass != null) {
                            String text = representation.getText();
                            if (text != null) {
                                text = text.trim();
                                if (text.startsWith("[")) {
                                    final ParameterizedTypeImpl type = getParameterizedListType(resourceClass);
                                    return getGson().fromJson(text, type);
                                } else {
                                    if (text.startsWith("{")) {
                                        return getGson().fromJson(text, resourceClass);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static ParameterizedTypeImpl getParameterizedListType(final Class<?> resourceClass) {
        return new ParameterizedTypeImpl(List.class, new Type[] { resourceClass }, null);
    }

    public static boolean isInternalRequest(final Request request) {
        return !request.getResourceRef().toString().startsWith(HTTP);
    }

    public static Date now() {
        return new Date();
    }

    public static String toJson(final Object obj) {
        return getGson().toJson(obj);
    }

    public static Map<String, Object> asMap(final Object object) {
        final String json = getGson().toJson(object);
        final Map<String, Object> map = getGson().fromJson(json, Utils.getItemType());
        return map;
    }

    public static List<Map<String, Object>> asMap(final Collection<?> collection) {
        final String json = getGson().toJson(collection);
        final List<Map<String, Object>> map = getGson().fromJson(json, Utils.getItemsType());
        return map;
    }

    public static Date toDate(final String dateString, final String format) throws ParseException {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }

        final SimpleDateFormat sdf = getSimpleDateFormat(format);

        Date date = null;
        if (dateString != null) {
            date = sdf.parse(dateString);
        }

        return date;
    }

    public static String format(final Date date, final String format) {
        final SimpleDateFormat sdf = getSimpleDateFormat(format);
        return sdf.format(date);
    }

    private static final Map<String, SimpleDateFormat> sdfs = new ConcurrentHashMap<String, SimpleDateFormat>();
    private static SimpleDateFormat getSimpleDateFormat(final String format) {
        SimpleDateFormat sdf = sdfs.get(format);
        if (sdf == null) {
            sdf = new SimpleDateFormat(format);
            sdfs.put(format, sdf);
        }
        return sdf;
    }

    public static BigDecimal toBigDecimal(final String string) {
        if (string == null || string.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(string);
    }

    public static Double toDouble(final String string) {
        if (string == null || string.isEmpty()) {
            return 0d;
        }
        try {
            final Double d = (Double) convert(string, Double.class);
            return d;
        } catch (final Exception e) {
            return 0d;
        }
    }

    public static EntityImpl getKeyedOnly(final Key key) throws Exception {
        Class<?> class1 = key.getKind();
        if (class1 == null) {
            class1 = MapEntityImpl.class;
        }
        return getKeyedOnly(class1, key);
    }

    public static EntityImpl getKeyedOnly(final Class<?> class1, final Key key) throws Exception {
        final Constructor<?> constructor = class1.getDeclaredConstructor(NO_ARGS);
        constructor.setAccessible(true);
        final EntityImpl entity = (EntityImpl) constructor.newInstance(NO_PARAMS);
        entity.setId(key.toString());
        return entity;
    }

    public static String getModelPackageName() {
        if (packageName == null) {
            packageName = (String) Utils.getDatabaseConfigurationProperties().get(HIBERNATE_MODEL_PACKAGE);
        }
        return packageName;
    }

    public static Map<String, String> getParts(final String source, final String template) {
        return getParts(source, template, null);
    }

    public static Map<String, String> getParts(final String source, final String template, List<String> names) {
        final String regex = TEMPLATE_WORD;

        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(template);

        if (names == null) {
            names = new ArrayList<String>();
            while (matcher.find()) {
                names.add(matcher.group(1));
            }
        }

        final String regex2 = matcher.replaceAll(TEMPLATE_PLACEHOLDER);

        final Pattern pattern2 = Pattern.compile(regex2);
        final Matcher matcher2 = pattern2.matcher(source);

        final boolean find = matcher2.find();

        final Map<String, String> map = new LinkedHashMap<String, String>();
        if (find) {
            for(int i = 0; i < matcher2.groupCount(); i++) {
                final String value = matcher2.group(i + 1);

                String key = null;
                if (i < names.size()) {
                    key = names.get(i);
                } else {
                    key = REMAINING;
                }
                map.put(key, value);
            }
        }

        if (find && !map.containsKey(REMAINING)) {
            final int end = matcher2.end();
            final String remaining = source.substring(end);
            if (remaining != null && remaining.length() > 0) {
                map.put(REMAINING, remaining);
            }
        }

        return map;
    }

    public static boolean isEncodeKeys() {
        if (isEncodeKeys == null) {
            try {
                final Properties properties = getApplicationServerProperties();
                final Object b = properties.get(APPLICATION_ENCODE_KEYS);
                isEncodeKeys = b != null && Boolean.valueOf((String) b);
            } catch (final Exception e) {
                e.printStackTrace();
                isEncodeKeys = false;
            }
        }
        return isEncodeKeys;
    }

    public static String getEncodedKeyString(final com.maintainer.data.provider.Key nobodyelsesKey) {
        String string = nobodyelsesKey.toString();
        string = getEncodedKeyString(string);
        return string;
    }

    public static String getEncodedKeyString(String string) {
        if (Utils.isEncodeKeys()) {
            string = Base64.encodeToString(string.getBytes(), false);
        }
        return string;
    }

    public static String fromEncodedKeyString(String string) {
        try {
            string = URLDecoder.decode(string, UTF_8);
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (Utils.isEncodeKeys()) {
            string = new String(Base64.decodeFast(string));
        }
        return string;
    }

    public static User getUser(final Request request) {
        return (User) request.getAttributes().get(_USER_);
    }

    public static boolean isEmpty(final String string) {
        return string == null || string.trim().length() == 0;
    }

    public static void setIsEncodeKeys(final boolean b) {
        isEncodeKeys = b;
    }

    public static void severe(final Logger log, final Exception e) {
        log.severe(e.getMessage());
        log.severe(getStackTrace(e));
    }

    public static void severe(final Logger log, final String message) {
        log.severe(message);
    }

    public static String getStackTrace(final Exception e) {
        final StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        final String stacktrace = sw.toString();
        return stacktrace;
    }

    public static String convertStreamToString(final InputStream is) {
        final java.util.Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static Object getFieldValue(final Object obj, final String fieldName) throws Exception {
        MyField field = getField(obj, fieldName);
        return getFieldValue(obj, field);
    }

    public static Object getFieldValue(final Object obj, final MyField field) throws Exception {
        field.setAccessible(true);

        if (MapEntityImpl.class.isAssignableFrom(obj.getClass())) {
            MapEntityImpl mapEntityImpl = (MapEntityImpl) obj;
            return mapEntityImpl.get(field.getName());
        }

        final Object value = field.get(obj);
        return value;
    }

    public static void setFieldValue(final Object obj, final MyField field, final Object value) throws IllegalAccessException {
        field.setAccessible(true);

        if (MapEntityImpl.class.isAssignableFrom(obj.getClass())) {
            MapEntityImpl mapEntityImpl = (MapEntityImpl) obj;
            mapEntityImpl.set(field.getName(), value);

        } else {
            field.set(obj, value);
        }
    }

    public static MyField getField(final Object obj, final String fieldName) throws Exception {
        Map<String, MyField> fieldsAsMap = getFieldsAsMap(obj, true);
        return fieldsAsMap.get(fieldName);
    }

    public static MyField getField(final Class<?> clazz, final String fieldName) throws Exception {
        Map<String, MyField> fields = getFieldsAsMap(clazz, true);
        return fields.get(fieldName);
    }

    public static MyField getField(final String className, final String fieldName) throws Exception {
        Map<String, MyField> fields = getFieldsAsMap(className);
        return fields.get(fieldName);
    }

    public static MyField getField(final MyClass myClass, final String fieldName) throws Exception {
        Map<String, MyField> fields = getFieldsAsMap(myClass);
        return fields.get(fieldName);
    }

    @SuppressWarnings("unchecked")
    public static List<MyField> getFields(final Object target) throws Exception {
        return getFields(target, true);
    }

    public static List<MyField> getFields(final Object target, final boolean isRecurse) throws Exception {
        Map<String, MyField> fieldMap = getFieldsAsMap(target, isRecurse);
        return new ArrayList<MyField>(fieldMap.values());
    }

    @SuppressWarnings("unchecked")
    public static List<MyField> getFields(final String className) throws Exception {
        com.maintainer.data.provider.Key key = com.maintainer.data.provider.Key.create(MyClass.class, className);
        DataProvider<MyClass> myClassDataProvider = (DataProvider<MyClass>) DataProviderFactory.instance().getDataProvider(MyClass.class);
        MyClass myClass = myClassDataProvider.get(key);
        if (myClass != null) {
            List<MyField> fields = myClass.getFields();
            return fields;
        }
        return Collections.<MyField>emptyList();
    }

    public static Map<String, MyField> getFieldsAsMap(final Object target, final boolean isRecurse) throws Exception {
        Class<?> clazz = target.getClass();
        final Map<String, MyField> fieldMap = new LinkedHashMap<String, MyField>();
        Class<?> class1 = clazz;
        while (class1 != null) {
            final Field[] fields2 = class1.getDeclaredFields();
            for (int i = 0; i < fields2.length; i++) {
                final Field f = fields2[i];
                final String name = f.getName();

                final MyField myField = new MyField(f);
                if (!fieldMap.containsKey(name)) {
                    fieldMap.put(name, myField);
                }
            }

            if (!isRecurse) {
                break;
            }

            class1 = class1.getSuperclass();
        }

        if (MapEntityImpl.class.isAssignableFrom(clazz)) {
            MapEntityImpl mapEntityImpl = (MapEntityImpl) target;
            MyClass myClass = mapEntityImpl.getMyClass();
            if (myClass == null) {
                String path = ThreadLocalInfo.getInfo().getPath();
                myClass = getMyClassFromPath(path);
            }
            if (myClass != null) {
                for (MyField field : myClass.getFields()) {
                    String key = field.getName();
                    fieldMap.remove(key);
                    fieldMap.put(key, field);
                }
            }
        }

        return fieldMap;
    }

    public static Map<String, MyField> getFieldsAsMap(final String className) throws Exception {
        MyClass myClass = getMyClass(className);
        return getFieldsAsMap(myClass);
    }

    public static Map<String, MyField> getFieldsAsMap(final MyClass myClass) throws Exception {
        final Map<String, MyField> fieldMap = new LinkedHashMap<String, MyField>();
        List<MyField> fields = myClass.getFields();
        for (MyField field : fields) {
            String fieldName = field.getName();
            fieldMap.put(fieldName, field);
        }
        return fieldMap;
    }

    public static Map<String, MyField> getFieldsAsMap(final Class<?> clazz, final boolean isRecurse) throws Exception {
        final Map<String, MyField> fieldMap = new LinkedHashMap<String, MyField>();
        Class<?> class1 = clazz;
        while (class1 != null) {
            final Field[] fields2 = class1.getDeclaredFields();
            for (int i = 0; i < fields2.length; i++) {
                final Field f = fields2[i];
                final String name = f.getName();

                final MyField myField = new MyField(f);
                if (!fieldMap.containsKey(name)) {
                    fieldMap.put(name, myField);
                }
            }

            if (!isRecurse) {
                break;
            }

            class1 = class1.getSuperclass();
        }

        if (MapEntityImpl.class.isAssignableFrom(clazz)) {
            String path = ThreadLocalInfo.getInfo().getPath();
            MyClass myClass = getMyClassFromPath(path);
            if (myClass != null) {
                for (MyField field : myClass.getFields()) {
                    String key = field.getName();
                    fieldMap.remove(key);
                    fieldMap.put(key, field);
                }
            }
        }

        return fieldMap;
    }

    public static String getKindName(final Class<?> clazz) throws Exception {
        if (MapEntityImpl.class.isAssignableFrom(clazz)) {
            String path = ThreadLocalInfo.getInfo().getPath();
            MyClass myClass = getMyClassFromPath(path);
            if (myClass == null) {
                return null;
            }
            return myClass.getName();
        }

        return com.maintainer.data.provider.Key.getKindName(clazz);
    }

    @SuppressWarnings("unchecked")
    public static MyClass getMyClass(final String className) throws Exception {
        DataProvider<MyClass> myClassDataProvider = (DataProvider<MyClass>) DataProviderFactory.instance().getDataProvider(MyClass.class);
        Key key = Key.create(MyClass.class, className);
        MyClass myClass = myClassDataProvider.get(key);
        return myClass;
    }

    public static MyClass getMyClassFromPath(final String path) throws Exception {
        String[] split = path.split("/");
        String route = split[2];
        return getMyClassFromRoute(route);
    }

    @SuppressWarnings("unchecked")
    public static MyClass getMyClassFromRoute(final String route) throws Exception {
        DataProvider<MyClass> myClassDataProvider = (DataProvider<MyClass>) DataProviderFactory.instance().getDataProvider(MyClass.class);
        Query q = new Query(MyClass.class);
        q.filter("route", route);
        List<MyClass> list = myClassDataProvider.find(q);
        if (list.size() == 1) {
            return list.get(0);
        }
        return null;
    }
}
