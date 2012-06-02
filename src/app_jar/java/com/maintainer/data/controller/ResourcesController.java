package com.maintainer.data.controller;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.maintainer.data.model.Autocreate;
import com.maintainer.data.model.EntityBase;
import com.maintainer.data.model.EntityImpl;
import com.maintainer.data.model.EntityRemote;
import com.maintainer.data.provider.DataProvider;
import com.maintainer.data.provider.DataProviderFactory;
import com.maintainer.data.provider.DefaultDataProviderInitializationException;
import com.maintainer.data.provider.Key;
import com.maintainer.data.provider.Query;
import com.maintainer.data.router.WebSwitch;
import com.maintainer.util.Utils;

public abstract class ResourcesController<T> extends ServerResource {
    private static final Logger log = Logger.getLogger(ResourcesController.class.getName());

    private static final int MAX_ROWS = 1000;
    private static final String _ID = "_id";
    private static final String ID = "id";
    private static final String ID2 = "id2";
    private static final long ID_NOT_PROVIDED = 0;
    private static final String NO_ID_PROVIDED = "no id provided";

    protected abstract DataProvider<T> getDataProvider() throws DefaultDataProviderInitializationException;

    protected abstract Class<?> getControllerClass(String resource);

    protected abstract String getResourceMapping(Class<?> clazz);

    public Representation getHead() throws Exception {
        String resource = getResource();

        Map<String, Object> item = new HashMap<String, Object>();
        item.put("resource", resource);

        String json = getGson().toJson(item, Utils.getItemType());

        Representation response = new StringRepresentation(json);
        response.setMediaType(MediaType.APPLICATION_JSON);
        setStatus(Status.SUCCESS_OK);
        return response;
    }

    private Object get(Request request) throws Exception {
        ArrayList<Resource> resources = Utils.getResources(request);

        Object parent = null;
        Object obj = null;

        for (int i = 0; i < resources.size(); i++) {
            Resource parentResource = null;
            if (i > 0) {
                parentResource = resources.get(i - 1);
            }

            if (obj != null) {
                parent = obj;
                obj = null;
            }

            Resource resource = resources.get(i);

            if (parent != null) {
                String fieldName = resource.getResource();
                try {
                    obj = getFieldValue(parent, fieldName);
                } catch (InvalidResourceException ire) {
                    // ignore this one
                }

                if (obj != null && !resource.isProperty()) {
                    if (Collection.class.isAssignableFrom(obj.getClass())) {
                        if (!resource.isId())
                            throw new InvalidResourceException("Identifier for a collection must be numeric.");

                        ArrayList<Object> list = new ArrayList<Object>((Collection<?>) obj);
                        for (Object o : list) {
                            if (o == null)
                                continue;
                            if (EntityBase.class.isAssignableFrom(o.getClass())) {
                                EntityBase e = (EntityBase) o;
                                Long id = e.getId();
                                if (id.longValue() == resource.getId().longValue()) {
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
                DataProvider<?> dataProvider = getDataProvider(resource);

                Query query = new Query(getResourceClass(resource));

                if (parentResource != null) {
                    query.filter(parentResource.getResource(), parentResource.getId());
                }

                if (resource.isId()) {
                    query.filter(ID, resource.getId());
                    query.setKey(new Key(query.getKind(), resource.getId()));
                }

                if (isLastResource(resources, i)) {
                    addParametersToQuery(request, resource, query);
                }

                if (!query.isOrdered()) {
                    if (query.isEmpty()) {
                        query.setOrder(ID);
                    } else {
                        String key = query.entrySet().iterator().next().getKey();
                        String[] split = key.split(":");
                        key = split[0];
                        query.setOrder(key);
                    }
                }

                if (query.getLimit() == 0 || query.getLimit() > MAX_ROWS) {
                    query.setLimit(MAX_ROWS);
                }

                Collection<?> list = dataProvider.find(query);

                if (list == null) {
                    throw new NotFoundException();
                }

                if (resource.isId() && !list.isEmpty()) {
                    obj = list.iterator().next();
                    autocreate(obj);
                } else {
                    obj = list;
                    for (Object o : list) {
                        autocreate(o);
                    }
                }
            }
        }

        if (obj == null) {
            throw new NotFoundException();
        }

        return obj;
    }

    @SuppressWarnings({ "unused", "unchecked", "rawtypes" })
    public Object autocreate(Object target) throws Exception {

        Field[] fields = target.getClass().getDeclaredFields();
        for (Field f : fields) {
            f.setAccessible(true);
            Autocreate autocreate = f.getAnnotation(Autocreate.class);
            if (autocreate != null) {
                if (!autocreate.remote()) {
                    if (EntityBase.class.isAssignableFrom(f.getType())) {
                        Object value = f.get(target);
                        if (value != null) {
                            autocreate(value);
                        }
                    } else if (Collection.class.isAssignableFrom(f.getType())) {
                        Collection collection = (Collection) f.get(target);
                        for (Object value : collection) {
                            if (EntityBase.class.isAssignableFrom(value.getClass())) {
                                autocreate(value);
                            }
                        }
                    }
                } else {
                    try {
                        Object value = f.get(target);
                        if (value != null) {
                            if (EntityRemote.class.isAssignableFrom(value.getClass())) {
                                EntityRemote entity = (EntityRemote) value;
                                if (entity.getId() != null) {
                                    com.maintainer.data.model.Resource resourceAnnotation = entity.getClass().getAnnotation(
                                            com.maintainer.data.model.Resource.class);
                                    if (resourceAnnotation != null) {
                                        String url = resourceAnnotation.name();
                                        Object id = entity.getId();
                                        if (Map.class.isAssignableFrom(id.getClass())) {
                                            StringBuilder buf = new StringBuilder();
                                            Map<String, Object> key = (Map<String, Object>) id;

                                            for (Entry<String, Object> e : key.entrySet()) {
                                                String key2 = e.getKey();
                                                Object value2 = e.getValue();
                                                if (buf.length() > 0)
                                                    buf.append('&');
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
                                            List<Object> results = (List<Object>) subrequest;
                                            if (!results.isEmpty()) {
                                                subrequest = results.get(0);
                                            }
                                        }
                                        try {
                                            f.set(target, subrequest);
                                        } catch (Exception e) {
                                            log.error(e.getMessage());
                                        }
                                    }
                                }
                            } else if (Collection.class.isAssignableFrom(value.getClass())) {
                                List<Object> list = new ArrayList<Object>((Collection<Object>) value);
                                ListIterator<Object> iterator = list.listIterator();
                                while (iterator.hasNext()) {
                                    Object o = iterator.next();
                                    if (EntityRemote.class.isAssignableFrom(o.getClass())) {
                                        EntityRemote entity = (EntityRemote) o;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        return target;
    }

    private Object getFieldValue(Object obj, String fieldName) throws IllegalAccessException, InvalidResourceException {
        Object value = null;

        Field field = Utils.getField(obj, fieldName);

        if (field == null)
            throw new InvalidResourceException("Field " + fieldName + " not found.");

        if (field != null) {
            field.setAccessible(true);
            value = field.get(obj);
        }

        return value;
    }

    protected Query addParametersToQuery(Request request, Resource resource, Query query) throws Exception {
        Form form = request.getResourceRef().getQueryAsForm(true);
        Map<String, String> map = form.getValuesMap();
        log.debug("query map: " + map);

        for (Entry<String, String> e : map.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();

            log.debug("key = " + key + ", value = " + value);

            if (Query.OFFSET.equals(key)) {
                query.setOffset(Integer.parseInt(value));
            } else if (Query.LIMIT.equals(key)) {
                query.setLimit(Integer.parseInt(value));
            } else if (Query.ORDER.equals(key)) {
                query.setOrder(value);
            } else {
                if (key.startsWith(":")) {
                    continue;
                }

                String f = key;
                f = key.split(":")[0];
                f = f.split("\\.")[0];

                Class<?> clazz = getResourceClass(resource);
                Field field = Utils.getField(clazz, f);
                if (field == null) {
                    throw new InvalidResourceException();
                }
                query.filter(key, Utils.convert(value, field.getType()));
            }
        }
        return query;
    }

    private boolean isLastResource(ArrayList<Resource> resources, int i) {
        return i == resources.size() - 1;
    }

    private DataProvider<?> getDataProvider(Resource resource) throws Exception {
        Class<?> clazz = getResourceClass(resource);

        DataProvider<?> dataProvider = DataProviderFactory.instance().getDataProvider(clazz);
        if (dataProvider == null) {
            throw new InvalidResourceException();
        }

        return dataProvider;
    }

    private Class<?> getResourceClass(Resource resource) throws Exception {
        Class<?> clazz = getControllerClass(resource.getResource());
        if (clazz == null) {
            throw new InvalidResourceException();
        }
        return clazz;
    }

    @Get("json")
    public Representation getItems() throws Exception {
        Request request = getRequest();
        Method method = request.getMethod();
        if (Method.HEAD.equals(method))
            return getHead();

        Representation response = null;
        String json = null;
        try {
            Object obj = get(request);
            json = toJson(obj);
            response = new StringRepresentation(json);
            response.setMediaType(MediaType.APPLICATION_JSON);
            setStatus(Status.SUCCESS_OK);
        } catch (NotFoundException nf) {
            setStatus(Status.CLIENT_ERROR_NOT_FOUND);
        } catch (InvalidResourceException nr) {
            setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
        }

        return response;
    }

    protected String toJson(Object obj) {
        return getGson().toJson(obj);
    }

    protected Gson getGson() {
        return new GsonBuilder().setPrettyPrinting().create();
    }

    // Return the first value as a string. Later, convert the string
    // value using the reflected type of the java property by name.
    protected Object getFilterValue(String name, Object value) {
        Field field = null;
        Class<?> clazz = getType();
        try {
            field = clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
        }

        if (field != null) {
            Class<?> type = field.getType();
            value = Utils.convert(value, type);
        }

        return value;
    }

    @Post("json")
    public Representation postItem(Representation rep) throws Exception {
        Class<?> kind = getType();
        checkReadOnly(kind);

        String incomingJson = rep.getText();

        DataProvider<T> service = getDataProvider();

        T obj = service.fromJson(kind, incomingJson);
        prePost(obj);
        obj = service.post(obj);

        String json = getGson().toJson(obj);

        Representation response = new StringRepresentation(json);
        response.setMediaType(MediaType.APPLICATION_JSON);
        setStatus(Status.SUCCESS_OK);
        return response;
    }

    private void checkReadOnly(Class<?> kind) {
        if (kind == null)
            return;

        Autocreate autocreate = kind.getAnnotation(Autocreate.class);
        if (autocreate != null && autocreate.readonly()) {
            throw new RuntimeException("Cannot create, update, or delete a read-only resource: " + kind.getName());
        }
    }

    private Class<?> getType() {
        Class<?> kind = getControllerClass(getResource());
        return kind;
    }

    protected void prePost(T obj) {
    }

    @Put("json")
    public Representation putItem(Representation rep) throws Exception {
        Class<?> kind = getType();
        checkReadOnly(kind);

        String incomingJson = rep.getText();

        DataProvider<T> service = getDataProvider();
        T obj = service.fromJson(getType(), incomingJson);

        if (((EntityImpl) obj).getId() == -1) {
            throw new Exception(NO_ID_PROVIDED);
        }

        prePut(obj);
        T merged = service.merge(obj);

        String json = getGson().toJson(merged);

        Representation response = new StringRepresentation(json);
        response.setMediaType(MediaType.APPLICATION_JSON);
        setStatus(Status.SUCCESS_OK);
        return response;
    }

    protected void prePut(T obj) {
    }

    @Delete("json")
    public Representation deleteItem() throws Exception {
        Class<?> kind = getType();
        checkReadOnly(kind);

        long id = getId();

        if (id == ID_NOT_PROVIDED) {
            throw new Exception(NO_ID_PROVIDED);
        }

        DataProvider<T> service = getDataProvider();

        Key key = new Key(kind, id);
        T obj = service.get(key);
        preDelete(obj);

        key = service.delete(key);

        Representation response = new StringRepresentation("{\"" + ID + "\":" + key.getId() + "}");
        response.setMediaType(MediaType.APPLICATION_JSON);
        setStatus(Status.SUCCESS_OK);
        return response;
    }

    protected void preDelete(T obj) {
    }

    protected String getResource() {
        ArrayList<Resource> resources = Utils.getResources(getRequest());
        if (resources.size() > 0)
            return resources.get(0).getResource();
        return null;
    }

    protected long getId() {
        ArrayList<Resource> resources = Utils.getResources(getRequest());
        if (resources.size() < 1)
            return ID_NOT_PROVIDED;

        Resource resource = resources.get(0);
        boolean isId = resource.isId();
        if (!isId)
            return ID_NOT_PROVIDED;

        return resource.getId();
    }

    protected String getResource2() {
        ArrayList<Resource> resources = Utils.getResources(getRequest());
        if (resources.size() > 1)
            return resources.get(1).getResource();
        return null;
    }

    protected long getId2() {
        ArrayList<Resource> resources = Utils.getResources(getRequest());
        if (resources.size() < 2)
            return ID_NOT_PROVIDED;

        Resource resource = resources.get(1);
        boolean isId = resource.isId();
        if (!isId)
            return ID_NOT_PROVIDED;

        return resource.getId();
    }
}
