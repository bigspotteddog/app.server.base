package com.maintainer.data.controller;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.maintainer.data.model.Autocreate;
import com.maintainer.data.model.EntityBase;
import com.maintainer.data.model.EntityImpl;
import com.maintainer.data.model.EntityRemote;
import com.maintainer.data.model.MapEntityImpl;
import com.maintainer.data.model.MyClass;
import com.maintainer.data.model.MyField;
import com.maintainer.data.provider.DataProvider;
import com.maintainer.data.provider.DataProviderFactory;
import com.maintainer.data.provider.DefaultDataProviderInitializationException;
import com.maintainer.data.provider.Key;
import com.maintainer.data.provider.Query;
import com.maintainer.data.provider.ResultList;
import com.maintainer.data.router.WebSwitch;
import com.maintainer.util.Utils;

@SuppressWarnings("unused")
public abstract class ResourcesController<T extends EntityImpl> extends ServerResource {

    private static final String RESOURCE = "resource";

    private static final String RESOURCE_TEMPLATE = "/?([^/\\?]+)";
    private static final String RESOURCE_ID_TEMPLATE = "/?([^/\\?]+)/([^/\\?]+)";
    private static final List<String> RESOURCE_ID = Arrays.asList("resource", "id");

    private static final String UTF_8 = "UTF-8";

    private static final String MINUS = "-";

    private static final Logger log = Logger.getLogger(ResourcesController.class.getName());

    private static final int MAX_ROWS = 1000;
    private static final String _ID = "_id";
    public static final String ID = "id";
    private static final String ID2 = "id2";
    private static final long ID_NOT_PROVIDED = 0;
    public static final String ID_PROVIDED = "Id provided with post.";
    public static final String NO_ID_PROVIDED = "No id provided with put.";

    private int maxRows = MAX_ROWS;
    private boolean checkFields = true;
    private boolean ignoreInvalidFields = false;

    private LinkedHashSet<String> errors;

    protected LinkedHashSet<String> getErrors() {
        if (errors == null) {
            errors = new LinkedHashSet<String>();
        }
        return errors;
    }

    public void addError(final Exception e) {
        e.printStackTrace();
        addError(e.getMessage());
    }

    public void addError(final String error) {
        getErrors().add(error);
    }

    public Representation getHead() throws Exception {
        final String resource = getResource();

        final Map<String, Object> item = new HashMap<String, Object>();
        item.put(RESOURCE, resource);

        final String json = getGson().toJson(item, Utils.getItemType());

        return getJsonResponse(json);
    }

    protected void preGet(final Request request) throws Exception {}

    protected Object postGet(final Request request, final Object result) throws Exception {
        return result;
    }

    @Get("json")
    public Representation getItems() throws Exception {
        final Request request = getRequest();

        final Method method = request.getMethod();
        if (Method.HEAD.equals(method)) {
            return getHead();
        }
        final Representation items = getItems(request);
        return items;
    }

    protected Representation getItems(final Request request) throws Exception {
        Representation response = null;
        String json = null;
        try {
            Status status = Status.SUCCESS_OK;
            preGet(request);
            Object obj = get(request);
            obj = postGet(request, obj);
            if (obj == null) {
                throw new NotFoundException();
            }
            if (List.class.isAssignableFrom(obj.getClass())) {
                json = toJson((List) obj);
            } else {
                json = toJson(obj);
            }

            if (errors != null && !errors.isEmpty()) {
                status = Status.CLIENT_ERROR_PRECONDITION_FAILED;

                Object object = null;
                if (List.class.isAssignableFrom(obj.getClass())) {
                    object = Utils.getGson().fromJson(json, Utils.getItemsType());
                } else {
                    object = Utils.getGson().fromJson(json, Utils.getItemsType());
                }

                final ErrorResponse errorResponse = new ErrorResponse(errors, object);
                json = Utils.getGson().toJson(errorResponse);
            }

            log.info("returning: " + json);
            response = new StringRepresentation(json);
            response.setMediaType(MediaType.APPLICATION_JSON);
            setStatus(status);
        } catch (final NotFoundException nf) {
            setStatus(Status.CLIENT_ERROR_NOT_FOUND);
        } catch (final InvalidResourceException nr) {
            setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
        }

        return response;
    }

    @SuppressWarnings("unchecked")
    protected Object get(final Request request) throws Exception {
        final ArrayList<Resource> resources = getResources(request);

        Object parent = null;
        Object obj = null;

        List<?> list = null;

        for (int i = 0; i < resources.size(); i++) {
            Resource parentResource = null;
            if (i > 0) {
                parentResource = resources.get(i - 1);
            }

            if (obj != null) {
                parent = obj;
                obj = null;
            }

            final Resource resource = resources.get(i);

            if (parent != null) {
                String fieldName = resource.getResource();
                try {
                    obj = getFieldValue(parent, fieldName);
                } catch (final InvalidResourceException ire) {
                    // ignore this one
                }

                if (obj != null && !resource.isProperty()) {
                    if (Collection.class.isAssignableFrom(obj.getClass())) {
                        final boolean isId = resource.getProperty() != null;
                        if (!isId) {
                            throw new InvalidResourceException("Identifier for a collection must be numeric.");
                        }

                        final ArrayList<Object> list2 = new ArrayList<Object>((Collection<?>) obj);
                        for (final Object o : list2) {
                            if (o == null) {
                                continue;
                            }
                            if (EntityBase.class.isAssignableFrom(o.getClass())) {
                                final EntityBase e = (EntityBase) o;
                                final Object id = e.getId();

                                //TODO: This will break with encoded keys on!
                                if (id.equals(resource.getProperty())) {
                                    obj = o;
                                    break;
                                }
                            }
                        }
                    } else {
                        fieldName = resource.getProperty();
                        obj = getFieldValue(obj, fieldName);
                    }
                }
            }

            if (obj == null) {
                final DataProvider<?> dataProvider = getDataProvider(resource);

                final Query query = new Query(getResourceClass(resource));

                if (parentResource != null) {
                    query.filter(parentResource.getResource(), parentResource.getId());
                }

                final boolean isId = resource.getProperty() != null; // && i == resources.size() - 1;
                if (isId) {
                    if (Utils.isNumeric(resource.getProperty())) {
                        query.filter(ID, resource.getProperty());
                    } else {
                        final String string = resource.getProperty();
                        query.setKey(Key.fromString(string));
                    }
                }

                if (isLastResource(resources, i)) {
                    addParametersToQuery(request, resource, query);
                }

                if (!query.isOrdered()) {
                    if (query.isEmpty()) {
                        query.setOrder(ID);
                    } else {
                        String key = query.getFilters().iterator().next().getCondition();
                        final String[] split = key.split(":");
                        key = split[0];
                        query.setOrder(key);
                    }
                }

                if (query.getLimit() == 0 || query.getLimit() > maxRows) {
                    query.setLimit(maxRows);
                }

                list = find(dataProvider, query);

                if (list == null) {
                    throw new NotFoundException();
                }

                final Class<?> type = getType();
                final Autocreate annotation = type.getAnnotation(Autocreate.class);
                final boolean autocreate = annotation == null || (annotation != null && !annotation.skip());
                if (isId && !list.isEmpty()) {
                    obj = list.iterator().next();
                    if (autocreate) {
                        autocreate(obj);
                    }
                } else {
                    obj = list;
                    if (autocreate) {
                        for (final Object o : list) {
                            autocreate(o);
                        }
                    }
                }
            }
        }

        if (obj == null) {
            throw new NotFoundException();
        }

        if (ResultList.class.isAssignableFrom(obj.getClass())) {

            final ResultList<?> list2 = (ResultList<?>) obj;

            final EntityImpl first = list2.first();
            if (first != null) {
                first.setCursor(list2.previous());
            }

            final EntityImpl last = list2.last();
            if (last != null) {
                last.setCursor(list2.next());
            }

            obj = postGet((Collection<T>) obj);
        } else {
            try {
                obj = postGet((T) obj);
            } catch (ClassCastException e) {
                // ignore
            }
        }
        return obj;
    }

    protected List<?> find(final DataProvider<?> dataProvider, final Query query) throws Exception {
        return dataProvider.find(query);
    }

    protected ArrayList<Resource> getResources(final Request request) {
        return Utils.getResources(request);
    }

    @SuppressWarnings("unchecked")
    @Post("json")
    public Representation postItem(final Representation rep) throws Exception {
        final Class<?> kind = getType();
        log.info("kind: " + kind.getName());
        checkReadOnly(kind);

        final String incomingJson = rep.getText();
        log.info("json: " + incomingJson);

        final DataProvider<T> service = getDataProvider();
        log.info("service: " + service.getClass().getName());

        Object obj = null;
        if (incomingJson.charAt(0) == '[') {
            Gson gson = Utils.getGson();

            List<T> list = null;
            if (MapEntityImpl.class.isAssignableFrom(kind)) {
                list = new ArrayList<T>();
                List<?> list2 = gson.fromJson(incomingJson, Utils.getItemsType());
                for (Object o : list2) {
                    String json = gson.toJson(o);
                    T o2 = service.fromJson(kind, json);
                    o2 = postObject(o2);
                    list.add(o2);
                }
            } else {
                Type t = Utils.getParameterizedListType(kind);
                list = gson.fromJson(incomingJson, t);
                for (T o : list) {
                    if (MapEntityImpl.class.isAssignableFrom(kind)) {
                        String json = gson.toJson(o);
                        o = service.fromJson(kind, json);
                    }
                    postObject(o);
                }
            }

            System.out.println(gson.toJson(list));

            obj = list;
        } else {
            T o = service.fromJson(kind, incomingJson);
            postObject(o);
            obj = o;
        }

        if (errors != null && !errors.isEmpty()) {
            final Status status = Status.CLIENT_ERROR_PRECONDITION_FAILED;
            final ErrorResponse errorResponse = new ErrorResponse(errors, obj);
            final String json = Utils.getGson().toJson(errorResponse);

            final JsonRepresentation response = new JsonRepresentation(json);
            response.setMediaType(MediaType.APPLICATION_JSON);
            setStatus(status);

            return response;
        }

        final String json = getGson().toJson(obj);

        return getJsonResponse(json);
    }

    protected T postObject(T obj) throws Exception {
        final EntityImpl obj2 = obj;
        if (obj2.getId() != null) {
            throw new Exception(ID_PROVIDED);
        }

        final DataProvider<T> service = getDataProvider();

        try {
            prePost(obj);
            obj = post(service, obj);
            postPost(obj);
        } catch (final Exception e) {
            addError(e);
        }

        return obj;
    }

    protected T post(final DataProvider<T> service, final T obj) throws Exception {
        return service.post(obj);
    }

    @Put("json")
    public Representation putItem(final Representation rep) throws Exception {
        final Class<?> kind = getType();
        checkReadOnly(kind);

        final String incomingJson = rep.getText();

        final DataProvider<T> service = getDataProvider();
        T obj = service.fromJson(getType(), incomingJson);

        final EntityImpl obj2 = obj;
        if (obj2.getId() == null) {
            throw new Exception(NO_ID_PROVIDED);
        }

        try {
            prePut(obj);
            obj = put(service, obj);
            postPut(obj);
        } catch (final Exception e) {
            addError(e);
        }

        if (errors != null && !errors.isEmpty()) {
            final Status status = Status.CLIENT_ERROR_PRECONDITION_FAILED;
            final ErrorResponse errorResponse = new ErrorResponse(errors, obj);
            final String json = Utils.getGson().toJson(errorResponse);

            final JsonRepresentation response = new JsonRepresentation(json);
            response.setMediaType(MediaType.APPLICATION_JSON);
            setStatus(status);

            return response;
        }

        final String json = getGson().toJson(obj);
        return getJsonResponse(json);
    }

    protected T put(final DataProvider<T> service, final T obj) throws Exception {
        return service.merge(obj);
    }

    @Delete("json")
    public Representation deleteItem() throws Exception {
        final Class<?> kind = getType();
        checkReadOnly(kind);

        final Object id = getId();

        if (id == null) {
            throw new Exception(NO_ID_PROVIDED);
        }

        final DataProvider<T> service = getDataProvider();

        Key key = Key.fromString((String) id);
        final T obj = service.get(key);

        try {
            preDelete(obj);
        } catch (final Exception e) {
            addError(e);
        }

        if (errors != null && !errors.isEmpty()) {
            final Status status = Status.CLIENT_ERROR_PRECONDITION_FAILED;
            final ErrorResponse errorResponse = new ErrorResponse(errors, obj);
            final String json = Utils.getGson().toJson(errorResponse);

            final JsonRepresentation response = new JsonRepresentation(json);
            response.setMediaType(MediaType.APPLICATION_JSON);
            setStatus(status);

            return response;
        }

        key = delete(service, key);

        try {
            postDelete(obj);
        } catch (final Exception e) {
            addError(e);
        }

        final Representation response = new StringRepresentation("{\"" + ID + "\":\"" + key.toString() + "\"}");
        response.setMediaType(MediaType.APPLICATION_JSON);
        setStatus(Status.SUCCESS_OK);
        return response;
    }

    protected Key delete(final DataProvider<T> service, final Key key) throws Exception {
        return service.delete(key);
    }

    protected abstract DataProvider<T> getDataProvider() throws DefaultDataProviderInitializationException;
    protected abstract Class<?> getControllerClass(String resource);

    protected void setMaxRows(final int maxRows) {
        this.maxRows = maxRows;
    }

    protected void setCheckFields(final boolean checkFields) {
        this.checkFields = checkFields;
    }

    protected void setIgnoreInvalidFields(final boolean ignoreInvalidFields) {
        this.ignoreInvalidFields = ignoreInvalidFields;
    }

    protected T postGet(final T entity) throws Exception {
        return entity;
    }

    protected Collection<T> postGet(final Collection<T> collection) throws Exception {
        return collection;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object autocreate(final Object target) throws Exception {
        if (target == null) {
            return null;
        }

        final Field[] fields = target.getClass().getDeclaredFields();
        for (final Field f : fields) {
            f.setAccessible(true);
            final Autocreate autocreate = f.getAnnotation(Autocreate.class);
            if (autocreate != null) {
                if (!autocreate.remote()) {
                    if (EntityBase.class.isAssignableFrom(f.getType())) {
                        final Object value = f.get(target);
                        if (value != null) {
                            autocreate(value);
                        }
                    } else if (Collection.class.isAssignableFrom(f.getType())) {
                        final Collection collection = (Collection) f.get(target);
                        if (collection != null) {
                            for (final Object value : collection) {
                                if (value != null && EntityBase.class.isAssignableFrom(value.getClass())) {
                                    autocreate(value);
                                }
                            }
                        }
                    }
                } else {
                    try {
                        final Object value = f.get(target);
                        if (value != null) {
                            if (EntityRemote.class.isAssignableFrom(value.getClass())) {
                                final EntityRemote entity = (EntityRemote) value;
                                if (entity.getId() != null) {
                                    final com.maintainer.data.model.Resource resourceAnnotation = entity.getClass().getAnnotation(
                                            com.maintainer.data.model.Resource.class);
                                    if (resourceAnnotation != null) {
                                        String url = resourceAnnotation.name();
                                        final Object id = entity.getId();
                                        if (Map.class.isAssignableFrom(id.getClass())) {
                                            final StringBuilder buf = new StringBuilder();
                                            final Map<String, Object> key = (Map<String, Object>) id;

                                            for (final Entry<String, Object> e : key.entrySet()) {
                                                final String key2 = e.getKey();
                                                final Object value2 = e.getValue();
                                                if (buf.length() > 0) {
                                                    buf.append('&');
                                                }
                                                buf.append(key2).append('=').append(value2);
                                            }

                                            if (buf.length() > 0) {
                                                url = buf.insert(0, '?').insert(0, url).toString();
                                            }
                                        } else {
                                            url = new StringBuilder(url).append('/').append(id).toString();
                                        }
                                        log.debug("remote query for: " + url);
                                        Object subrequest = Utils.subrequest((WebSwitch) getApplication(), url, getRequest());
                                        if (subrequest != null && List.class.isAssignableFrom(subrequest.getClass())) {
                                            final List<Object> results = (List<Object>) subrequest;
                                            if (!results.isEmpty()) {
                                                subrequest = results.get(0);
                                            }
                                        }
                                        try {
                                            f.set(target, subrequest);
                                        } catch (final Exception e) {
                                            log.error(e.getMessage());
                                        }
                                    }
                                }
                            } else if (Collection.class.isAssignableFrom(value.getClass())) {
                                final List<Object> list = new ArrayList<Object>((Collection<Object>) value);
                                final ListIterator<Object> iterator = list.listIterator();
                                while (iterator.hasNext()) {
                                    final Object o = iterator.next();
                                    if (EntityRemote.class.isAssignableFrom(o.getClass())) {
                                        final EntityRemote entity = (EntityRemote) o;
                                    }
                                }
                            }
                        }
                    } catch (final Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return target;
    }

    private Object getFieldValue(final Object obj, final String fieldName) throws Exception {
        Object value = null;

        MyField field = null;
        Class<?> clazz = obj.getClass();
        if (MapEntityImpl.class.equals(clazz)) {
            MapEntityImpl mapEntityImpl = (MapEntityImpl) obj;
            Key key = mapEntityImpl.getKey();
            if (key != null) {
                String className = key.getKindName();
                field = Utils.getField(className, fieldName);
            }
        } else {
            field = getField(clazz, fieldName);
        }

        if (field == null) {
            throw new InvalidResourceException("Field " + fieldName + " not found.");
        }

        if (field != null) {
            field.setAccessible(true);
            value = Utils.getFieldValue(obj, field);
        }

        return value;
    }

    protected void addOffset(final Query query, int value) {
        query.setOffset(value);
    }

    protected void addLimit(final Query query, int value) {
        query.setLimit(value);
    }

    protected void addOrder(final Query query, String value) {
        query.setOrder(value);
    }

    protected Query addParametersToQuery(final Request request, final Resource resource, final Query query) throws Exception {
        final Form form = request.getResourceRef().getQueryAsForm(true);
        final Map<String, String> map = form.getValuesMap();
        log.debug("query map: " + map);

        for (final Entry<String, String> e : map.entrySet()) {
            final String key = e.getKey();
            String value = e.getValue();

            log.debug("key = " + key + ", value = " + value);

            if (Query.OFFSET.equals(key)) {
                if (!Strings.isNullOrEmpty(value)) {
                    addOffset(query, Integer.parseInt(value));
                }
            } else if (Query.LIMIT.equals(key)) {
                if (!Strings.isNullOrEmpty(value)) {
                    addLimit(query, Integer.parseInt(value));
                }
            } else if (Query.ORDER.equals(key)) {
                if (!Strings.isNullOrEmpty(value)) {
                    addOrder(query, value);
                }
            } else if (Query.POS.equals(key)) {
                if (!Strings.isNullOrEmpty(value)) {
                    if (value.startsWith(MINUS)) {
                        value = value.substring(1);
                        query.setPreviousCursor(value);
                        query.previous();
                    } else {
                        query.setNextCursor(value);
                        query.next();
                    }
                }
            } else {
                if (key.startsWith(":")) {
                    continue;
                }

                String f = key;
                f = key.split(":")[0];
                f = f.split("\\.")[0];

                addFilterToQuery(resource.getResource(), key, f, value, query);
            }
        }
        return query;
    }

    @SuppressWarnings("rawtypes")
    protected Query addFilterToQuery(final String resource, final String key, final String fieldName, final Object value, final Query query) throws Exception {
        if (checkFields) {
            String route = resource;
            Class<?> class1 = getControllerClass(route);

            MyField field = null;
            if (MapEntityImpl.class.isAssignableFrom(class1)) {
                MyClass myClass = Utils.getMyClassFromRoute(route);
                field = Utils.getField(myClass, fieldName);
            } else {
                field = getField(class1, fieldName);
            }

            if (field != null) {
                if (EntityBase.class.isAssignableFrom(field.getType())) {
                    String[] split = key.split("\\.");
                    if (split.length > 1) {
                        String subField = split[1];
                        MyClass myClass = field.getMyClass();
                        MyField field2 = Utils.getField(myClass, subField);
                        if (field2 != null) {
                            Query q = new Query(myClass);
                            q.setKeysOnly(true);
                            addFilterToQuery(field2.getName(), value, q, field2);
                            DataProvider dataProvider = DataProviderFactory.instance().getDataProvider(field.getType());
                            List<?> list = find(dataProvider, q);
                            List<Key> keys = new ArrayList<Key>();
                            for (Object o : list) {
                                EntityImpl entityImpl = (EntityImpl) o;
                                Key key1 = entityImpl.getKey();
                                keys.add(key1);
                            }
                            addFilterToQuery(fieldName +":in", keys, query, field2);
                        }
                        // TODO: Issue a query to get the values here.
                        // Change the key and value to be a key or
                        // list of entity keys.
                    } else {
                        addFilterToQuery(key, value, query, field);
                    }
                } else {
                    addFilterToQuery(key, value, query, field);
                }
            }
        } else {
            addFilterToQuery(key, value, query);
        }
        return query;
    }

    protected void addFilterToQuery(final String key, final Object value, final Query query) throws Exception {
        query.filter(key, value);
    }

    protected void addFilterToQuery(final String key, final Object value, final Query query, final MyField field) throws Exception {
        Object value2 = value;
        if (EntityBase.class.isAssignableFrom(field.getType())) {
            value2 = Utils.convert(value, Key.class);
        } else {
            value2 = Utils.convert(value, field.getType());
        }

        query.filter(key, value2);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected MyField getField(final Resource resource, final String fieldName) throws Exception {
        final Class<?> clazz = getResourceClass(resource);
        return getField(clazz, fieldName);
    }

    @SuppressWarnings("rawtypes")
    protected MyField getField(final Class<?> clazz, final String fieldName) throws Exception {
        DataProvider dataProvider = DataProviderFactory.instance().getDataProvider(clazz);
        MyField field = Utils.getField(clazz, fieldName);
        return field;
    }

    private boolean isLastResource(final ArrayList<Resource> resources, final int i) {
        return i == resources.size() - 1;
    }

    protected DataProvider<?> getDataProvider(final Resource resource) throws Exception {
        final Class<?> clazz = getResourceClass(resource);

        final DataProvider<?> dataProvider = DataProviderFactory.instance().getDataProvider(clazz);
        if (dataProvider == null) {
            throw new InvalidResourceException();
        }

        return dataProvider;
    }

    protected Class<?> getResourceClass(final Resource resource) throws Exception {
        final Class<?> clazz = getControllerClass(resource.getResource());
        if (clazz == null) {
            return MapEntityImpl.class;
        }
        return clazz;
    }

    @SuppressWarnings({ "rawtypes" })
    protected String toJson(final List list) throws Exception {
        List<Object> entities = new ArrayList<Object>();

        for (Object entity : list) {
            if (MapEntityImpl.class.isAssignableFrom(entity.getClass())) {
                Map<String, Object> map = getMapEntityAsMap(entity);
                entities.add(map);
            } else {
                entities.add(entity);
            }
        }

        return getGson().toJson(entities);
    }

    protected String toJson(final Object entity) throws Exception  {
        Object obj = entity;
        if (MapEntityImpl.class.isAssignableFrom(entity.getClass())) {
            Map<String, Object> map = getMapEntityAsMap(entity);
            obj = map;
        }
        return getGson().toJson(obj);
    }

    protected Map<String, Object> getMapEntityAsMap(final Object entity) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();

        MapEntityImpl mapEntityImpl = (MapEntityImpl) entity;
        for (Entry<String, Object> e : mapEntityImpl.entrySet()) {
            result.put(e.getKey(), e.getValue());
        }

        Map<String, Object> map = Utils.asMap(mapEntityImpl);
        for (Entry<String, Object> e : map.entrySet()) {
            String key = e.getKey();
            if ("properties".equals(key)) continue;
            result.put(e.getKey(), e.getValue());
        }

        return result;
    }

    protected Gson getGson() {
        return Utils.getGson();
    }

    // Return the first value as a string. Later, convert the string
    // value using the reflected type of the java property by name.
    protected Object getFilterValue(final String name, Object value) throws Exception {
        Field field = null;
        final Class<?> clazz = getType();
        try {
            field = clazz.getDeclaredField(name);
        } catch (final NoSuchFieldException e) {
        }

        if (field != null) {
            final Class<?> type = field.getType();
            value = Utils.convert(value, type);
        }

        return value;
    }

    protected void checkReadOnly(final Class<?> kind) {
        if (kind == null) {
            return;
        }

        final Autocreate autocreate = kind.getAnnotation(Autocreate.class);
        if (autocreate != null && autocreate.readonly()) {
            throw new RuntimeException("Cannot create, update, or delete a read-only resource: " + kind.getName());
        }
    }

    protected Class<?> getType() {
        final Class<?> kind = getControllerClass(getResource());
        return kind;
    }

    protected void prePost(final T obj) throws Exception {
        validate(obj);
    }

    private void validate(final T obj) throws Exception {
        final List<String> errors = new ArrayList<String>();
        final boolean valid = obj.validate(errors);

        if (!errors.isEmpty()) {
            getErrors().addAll(errors);
        }

        if (!valid) {
            throw new Exception("Validation errors exist.");
        }
    }

    protected void postPost(final T obj) throws Exception {
    }

    protected Representation getJsonResponse(final String json) {
        final Representation response = new StringRepresentation(json);
        response.setMediaType(MediaType.APPLICATION_JSON);
        setStatus(Status.SUCCESS_OK);
        return response;
    }

    protected void prePut(final T obj) throws Exception {
        validate(obj);
    }

    protected void preDelete(final T obj) throws Exception {
    }

    protected void postPut(final T obj) throws Exception {
    }

    protected void postDelete(final T obj) throws Exception {
    }

    protected Object getId() {
        final Request request = getRequest();
        final ArrayList<Resource> resources = getResources(request);
        if (resources.size() < 1) {
            return ID_NOT_PROVIDED;
        }

        final Resource resource = resources.get(0);
        final Object id = resource.getProperty();
        if (id == null) {
            return ID_NOT_PROVIDED;
        }

        final String string = resource.getProperty();

        return string;
    }

    protected String getResource() {
        final String root = getRoot();

        final Reference resourceRef = getRequest().getResourceRef();
        String ref = resourceRef.toString();
        ref = URI.create(ref).getPath();

        final StringBuilder buf = new StringBuilder();
        if (ref.startsWith(root)) {
            buf.append(root);
        }

        buf.append(RESOURCE_TEMPLATE);
        final String template = buf.toString();

        final Map<String, String> parts = Utils.getParts(ref, template, RESOURCE_ID);
        return parts.get(RESOURCE);
    }

    private String getRoot() {
        String root = getRequest().getRootRef().toString();
        root = URI.create(root).getPath();
        return root;
    }
}
