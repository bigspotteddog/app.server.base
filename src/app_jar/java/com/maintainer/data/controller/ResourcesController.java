package com.maintainer.data.controller;

import java.lang.reflect.Field;
import java.math.BigDecimal;
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
import com.maintainer.data.model.Autocreate;
import com.maintainer.data.model.EntityBase;
import com.maintainer.data.model.EntityImpl;
import com.maintainer.data.model.EntityRemote;
import com.maintainer.data.provider.DataProvider;
import com.maintainer.data.provider.DataProviderFactory;
import com.maintainer.data.provider.DefaultDataProviderInitializationException;
import com.maintainer.data.provider.Key;
import com.maintainer.data.provider.Query;
import com.maintainer.data.provider.datastore.ResultList;
import com.maintainer.data.router.WebSwitch;
import com.maintainer.util.Utils;

@SuppressWarnings("unused")
public abstract class ResourcesController<T> extends ServerResource {
    private static final String MINUS = "-";

    private static final Logger log = Logger.getLogger(ResourcesController.class.getName());

    private static final int MAX_ROWS = 1000;
    private static final String _ID = "_id";
    private static final String ID = "id";
    private static final String ID2 = "id2";
    private static final long ID_NOT_PROVIDED = 0;
    private static final String ID_PROVIDED = "id provided with post";
    private static final String NO_ID_PROVIDED = "no id provided with put";

    private int maxRows = MAX_ROWS;
    private boolean checkFields = true;
    private boolean ignoreInvalidFields = false;

    protected abstract DataProvider<T> getDataProvider() throws DefaultDataProviderInitializationException;
    protected abstract Class<?> getControllerClass(String resource);
    protected abstract String getResourceMapping(Class<?> clazz);

    public Representation getHead() throws Exception {
        final String resource = getResource();

        final Map<String, Object> item = new HashMap<String, Object>();
        item.put("resource", resource);

        final String json = getGson().toJson(item, Utils.getItemType());

        return getJsonResponse(json);
    }

    protected void setMaxRows(final int maxRows) {
        this.maxRows = maxRows;
    }

    protected void setCheckFields(final boolean checkFields) {
        this.checkFields = checkFields;
    }

    protected void setIgnoreInvalidFields(final boolean ignoreInvalidFields) {
        this.ignoreInvalidFields = ignoreInvalidFields;
    }

    @SuppressWarnings("unchecked")
    protected Object get(final Request request) throws Exception {
        final ArrayList<Resource> resources = Utils.getResources(request);

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
                        if (!resource.isId()) {
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
                                if (id.equals(resource.getId())) {
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

                final boolean isId = resource.getProperty() != null && i == resources.size() - 1;
                if (isId) {
                    query.filter(ID, resource.getProperty());
                    query.setKey(new Key(query.getKind(), resource.getProperty()));
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

                list = dataProvider.find(query);

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

            postGet((Collection<T>) obj);
        } else {
            postGet((T) obj);
        }
        return obj;
    }

    protected void postGet(final T entity) {}

    protected void postGet(final Collection<T> collection) {}

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object autocreate(final Object target) throws Exception {
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

    private Object getFieldValue(final Object obj, final String fieldName) throws IllegalAccessException, InvalidResourceException {
        Object value = null;

        final Field field = Utils.getField(obj, fieldName);

        if (field == null) {
            throw new InvalidResourceException("Field " + fieldName + " not found.");
        }

        if (field != null) {
            field.setAccessible(true);
            value = field.get(obj);
        }

        return value;
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
                query.setOffset(Integer.parseInt(value));
            } else if (Query.LIMIT.equals(key)) {
                query.setLimit(Integer.parseInt(value));
            } else if (Query.ORDER.equals(key)) {
                query.setOrder(value);
            } else if (Query.POS.equals(key)) {
                if (value.startsWith(MINUS)) {
                    value = value.substring(1);
                    query.setPreviousCursor(value);
                    query.previous();
                } else {
                    query.setNextCursor(value);
                    query.next();
                }
            } else {
                if (key.startsWith(":")) {
                    continue;
                }

                String f = key;
                f = key.split(":")[0];
                f = f.split("\\.")[0];

                addFilterToQuery(resource, key, f, value, query);
            }
        }
        return query;
    }

    protected Query addFilterToQuery(final Resource resource, final String key, final String fieldName, final Object value, final Query query) throws Exception {
        if (checkFields) {
            final Field field = getField(resource, fieldName);
            if (field != null) {
                query.filter(key, Utils.convert(value, field.getType()));
            }
        } else {
            query.filter(key, value);
        }
        return query;
    }

    protected Field getField(final Resource resource, final String fieldName) throws Exception, InvalidResourceException {
        final Class<?> clazz = getResourceClass(resource);
        final Field field = Utils.getField(clazz, fieldName);
        if (!ignoreInvalidFields && field == null) {
            throw new InvalidResourceException();
        }
        return field;
    }

    private boolean isLastResource(final ArrayList<Resource> resources, final int i) {
        return i == resources.size() - 1;
    }

    private DataProvider<?> getDataProvider(final Resource resource) throws Exception {
        final Class<?> clazz = getResourceClass(resource);

        final DataProvider<?> dataProvider = DataProviderFactory.instance().getDataProvider(clazz);
        if (dataProvider == null) {
            throw new InvalidResourceException();
        }

        return dataProvider;
    }

    private Class<?> getResourceClass(final Resource resource) throws Exception {
        final Class<?> clazz = getControllerClass(resource.getResource());
        if (clazz == null) {
            throw new InvalidResourceException();
        }
        return clazz;
    }

    @Get("json")
    public Representation getItems() throws Exception {
        final Request request = getRequest();
        final Method method = request.getMethod();
        if (Method.HEAD.equals(method)) {
            return getHead();
        }

        return getItems(request);
    }

    @SuppressWarnings("unchecked")
    protected Representation getItems(final Request request) throws Exception {
        Representation response = null;
        String json = null;
        try {
            final Object obj = get(request);
            if (List.class.isAssignableFrom(obj.getClass())) {
                json = toJson((List<T>) obj);
            } else {
                json = toJson((T) obj);
            }
            response = new StringRepresentation(json);
            response.setMediaType(MediaType.APPLICATION_JSON);
            setStatus(Status.SUCCESS_OK);
        } catch (final NotFoundException nf) {
            setStatus(Status.CLIENT_ERROR_NOT_FOUND);
        } catch (final InvalidResourceException nr) {
            setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
        }

        return response;
    }

    protected String toJson(final List<T> list) {
        return getGson().toJson(list);
    }

    protected String toJson(final T entity) {
        return getGson().toJson(entity);
    }

    protected Gson getGson() {
        return Utils.getGson();
    }

    // Return the first value as a string. Later, convert the string
    // value using the reflected type of the java property by name.
    protected Object getFilterValue(final String name, Object value) {
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

    @Post("json")
    public Representation postItem(final Representation rep) throws Exception {
        final Class<?> kind = getType();
        checkReadOnly(kind);

        final String incomingJson = rep.getText();

        final DataProvider<T> service = getDataProvider();

        T obj = service.fromJson(kind, incomingJson);

        final EntityImpl obj2 = (EntityImpl) obj;
        if (obj2.getId() != null) {
            throw new Exception(ID_PROVIDED);
        } else {
            convertIdToLong(obj2);
        }

        prePost(obj);
        obj = service.post(obj);

        final String json = getGson().toJson(obj);

        return getJsonResponse(json);
    }

    private void convertIdToLong(final EntityImpl obj) {
        if (obj.getId() != null && !String.class.isAssignableFrom(obj.getId().getClass())) {
            obj.setId(new BigDecimal(obj.getId().toString()).longValue());
        }
    }

    private void checkReadOnly(final Class<?> kind) {
        if (kind == null) {
            return;
        }

        final Autocreate autocreate = kind.getAnnotation(Autocreate.class);
        if (autocreate != null && autocreate.readonly()) {
            throw new RuntimeException("Cannot create, update, or delete a read-only resource: " + kind.getName());
        }
    }

    private Class<?> getType() {
        final Class<?> kind = getControllerClass(getResource());
        return kind;
    }

    protected void prePost(final T obj) throws Exception {
    }

    @Put("json")
    public Representation putItem(final Representation rep) throws Exception {
        final Class<?> kind = getType();
        checkReadOnly(kind);

        final String incomingJson = rep.getText();

        final DataProvider<T> service = getDataProvider();
        final T obj = service.fromJson(getType(), incomingJson);

        final EntityImpl obj2 = (EntityImpl) obj;
        if (obj2.getId() == null) {
            throw new Exception(NO_ID_PROVIDED);
        } else {
            convertIdToLong(obj2);
        }

        prePut(obj);
        final T merged = service.merge(obj);

        final String json = getGson().toJson(merged);

        return getJsonResponse(json);
    }

    protected Representation getJsonResponse(final String json) {
        final Representation response = new StringRepresentation(json);
        response.setMediaType(MediaType.APPLICATION_JSON);
        setStatus(Status.SUCCESS_OK);
        return response;
    }

    protected void prePut(final T obj) throws Exception {
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

        Key key = new Key(kind, id);
        final T obj = service.get(key);
        preDelete(obj);

        key = service.delete(key);

        final Representation response = new StringRepresentation("{\"" + ID + "\":\"" + key.getId() + "\"}");
        response.setMediaType(MediaType.APPLICATION_JSON);
        setStatus(Status.SUCCESS_OK);
        return response;
    }

    protected void preDelete(final T obj) throws Exception {
    }

    protected String getResource() {
        final ArrayList<Resource> resources = Utils.getResources(getRequest());
        if (resources.size() > 0) {
            return resources.get(0).getResource();
        }
        return null;
    }

    protected Object getId() {
        final ArrayList<Resource> resources = Utils.getResources(getRequest());
        if (resources.size() < 1) {
            return ID_NOT_PROVIDED;
        }

        final Resource resource = resources.get(0);
        final Object id = resource.getProperty();
        if (id == null) {
            return ID_NOT_PROVIDED;
        }

        return resource.getProperty();
    }

    protected String getResource2() {
        final ArrayList<Resource> resources = Utils.getResources(getRequest());
        if (resources.size() > 1) {
            return resources.get(1).getResource();
        }
        return null;
    }

    protected long getId2() {
        final ArrayList<Resource> resources = Utils.getResources(getRequest());
        if (resources.size() < 2) {
            return ID_NOT_PROVIDED;
        }

        final Resource resource = resources.get(1);
        final boolean isId = resource.isId();
        if (!isId) {
            return ID_NOT_PROVIDED;
        }

        return resource.getId();
    }
}
