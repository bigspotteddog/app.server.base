package com.maintainer.data.provider.datastore;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Text;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.maintainer.data.model.Autocreate;
import com.maintainer.data.model.EntityBase;
import com.maintainer.data.model.EntityImpl;
import com.maintainer.data.provider.AbstractDataProvider;
import com.maintainer.data.provider.Query;
import com.maintainer.util.Utils;

public class MapDatastoreDataProvider extends AbstractDataProvider<Map<String, Object>> {
    private static final Logger log = Logger.getLogger(DatastoreDataProvider.class.getName());
    private static final Cache<com.maintainer.data.provider.Key, Object> cache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

    private static final Map<String, FilterOperator> ops = new HashMap<String, FilterOperator>();

    public MapDatastoreDataProvider() {
        ops.put("ge", FilterOperator.GREATER_THAN_OR_EQUAL);
        ops.put("gt", FilterOperator.GREATER_THAN);
        ops.put("le", FilterOperator.LESS_THAN_OR_EQUAL);
        ops.put("lt", FilterOperator.LESS_THAN);
        ops.put("eq", FilterOperator.EQUAL);
        ops.put(">=", FilterOperator.GREATER_THAN_OR_EQUAL);
        ops.put(">", FilterOperator.GREATER_THAN);
        ops.put("<=", FilterOperator.LESS_THAN_OR_EQUAL);
        ops.put("<", FilterOperator.LESS_THAN);
        ops.put("=", FilterOperator.EQUAL);
        ops.put("in", FilterOperator.IN);
    }

    @Override
    public Long getId(final Object object) {
        if (object != null && Key.class.isAssignableFrom(object.getClass())) {
            final Key key = (Key) object;
            return key.getId();
        }
        return super.getId(object);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> get(final com.maintainer.data.provider.Key key) {
        final Map<String, Object> cached = (Map<String, Object>) cache.getIfPresent(key);
        if (cached != null) {
            log.debug(key + " returned from cache.");
            return cached;
        }

        final Key k = createKey(key.getKindName(), key.getId());
        try {
            final Entity entity = getEntity(k);
            final Map<String, Object> fetched = fromEntity(key.getKind(), entity);
            cache.put(key, fetched);
            return fetched;
        } catch (final EntityNotFoundException e) {
            //ignore, it will just be null
        }
        return null;
    }

    private Entity getEntity(final Key key) throws EntityNotFoundException {
        final DatastoreService datastore = getDatastore();
        final Entity entity = datastore.get(key);
        return entity;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fromEntity(final Class<?> kind, final Entity entity) {
        Map<String, Object> obj = null;

        try {
            obj = new LinkedHashMap<String, Object>();

            final Map<String, Object> properties = entity.getProperties();
            for (final Entry<String, Object> e : properties.entrySet()) {
                Object value = e.getValue();

                if (value != null) {
                    if (Key.class.isAssignableFrom(value.getClass())) {
                        final Key k = (Key) value;
                        final String className = k.getKind();
                        final Class<?> class1 = Class.forName(className);
                        value = get(new com.maintainer.data.provider.Key(class1, k.getId()));
                    } else if (Text.class.isAssignableFrom(value.getClass())) {
                        value = ((Text) value).getValue();
                    } else if (Collection.class.isAssignableFrom(value.getClass())) {
                        final List<Object> list = new ArrayList<Object>((Collection<? extends Object>) value);

                        final ListIterator<Object> iterator = list.listIterator();
                        while(iterator.hasNext()) {
                            Object o = iterator.next();
                            if (Key.class.isAssignableFrom(o.getClass())) {
                                final Key k = (Key) o;
                                final String className = k.getKind();
                                final Class<?> class1 = Class.forName(className);
                                o = get(new com.maintainer.data.provider.Key(class1, k.getId()));
                                iterator.set(o);
                            }
                        }
                        value = list;
                    }
                    e.setValue(value);
                }
            }
            obj.put("id", entity.getKey().getId());

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        return obj;
    }

    private Key createKey(final String kind, final long id) {
        final Key key = KeyFactory.createKey(kind, id);
        return key;
    }

    private DatastoreService getDatastore() {
        final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        return datastore;
    }

    private String getKindName(final Class<?> clazz) {
        return com.maintainer.data.provider.Key.getKindName(clazz);
    }

    @Override
    public List<Map<String, Object>> getAll(final Class<?> kind) throws Exception {
        final com.google.appengine.api.datastore.Query q = new com.google.appengine.api.datastore.Query(getKindName(kind));
        final DatastoreService datastore = getDatastore();
        final PreparedQuery p = datastore.prepare(q);
        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final List<Entity> entities = p.asList(FetchOptions.Builder.withDefaults());
        for (final Entity e : entities) {
            final Map<String, Object> obj = fromEntity(kind, e);
            list.add(obj);
        }
        return list;
    }

    @Override
    public Map<String, Object> post(final Map<String, Object> target) throws Exception {
        autocreate2(target);

        Entity entity = new Entity(getKindName(target.getClass()));
        entity = toEntity(entity, target);

        final DatastoreService datastore = getDatastore();
        final Key posted = datastore.put(entity);
        target.put("id", posted.getId());
        return target;
    }


    @Override
    public Map<String, Object> put(Map<String, Object> target) throws Exception {
        final com.maintainer.data.provider.Key key = new com.maintainer.data.provider.Key(target.getClass(), (Long) target.get("id"));
        final Map<String, Object> existing = get(key);

        if (checkEqual(target, existing)) {
            return target;
        } else {
            log.debug(key + " changed.");
        }

        autocreate2(target);

        if (target.get("id") == null) {
            target = post(target);
            return target;
        }

        Entity entity = getEntity(KeyFactory.createKey(getKindName(target.getClass()), (Long) target.get("id")));
        entity = toEntity(entity, target);

        final DatastoreService datastore = getDatastore();
        final Key posted = datastore.put(entity);
        target.put("id", posted.getId());
        return target;
    }

    protected boolean checkEqual(final Map<String, Object> target, final Map<String, Object> existing) throws Exception {
        return isEqual(target, existing);
    }

    @SuppressWarnings("unchecked")
    private Entity toEntity(final Entity entity, final Map<String, Object> target) throws Exception {

        final ArrayList<Field> fields = getFields(target);

        for (final Field f : fields) {
            f.setAccessible(true);
            Object value = f.get(target);

            final Autocreate annotation = f.getAnnotation(Autocreate.class);
            if (annotation != null) {
                try {
                    if (value != null) {
                        if (EntityBase.class.isAssignableFrom(value.getClass())) {
                            final EntityBase base = (EntityBase) value;
                            value = KeyFactory.createKey(getKindName(base.getClass()), base.getId());
                        } else if (Collection.class.isAssignableFrom(value.getClass())) {
                            final List<Object> list = new ArrayList<Object>((Collection<Object>) value);
                            value = list;

                            final ListIterator<Object> iterator = list.listIterator();
                            while(iterator.hasNext()) {
                                final Object o = iterator.next();
                                if (EntityBase.class.isAssignableFrom(o.getClass())) {
                                    final EntityBase base = (EntityBase) o;
                                    final Key key = KeyFactory.createKey(getKindName(base.getClass()), base.getId());
                                    iterator.set(key);
                                }
                            }
                        }
                    }
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }

            if (value != null && String.class.isAssignableFrom(value.getClass())) {
                final String string = (String) value;
                if (string.length() > 500) {
                    value = new Text(string);
                }
            }
            entity.setProperty(f.getName(), value);
        }
        return entity;
    }

    private ArrayList<Field> getFields(final Map<String, Object> target) {
        final ArrayList<Field> fields = new ArrayList<Field>();
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            final Field[] fields2 = clazz.getDeclaredFields();
            fields.addAll(Lists.newArrayList(fields2));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    @Override
    public com.maintainer.data.provider.Key delete(final com.maintainer.data.provider.Key key) throws Exception {
        autodelete(key);

        final DatastoreService datastore = getDatastore();
        datastore.delete(createKey(key.getKindName(), key.getId()));
        return key;
    }

    private void addFilter(final com.google.appengine.api.datastore.Query q, final String propertyName, final FilterOperator operator, Object value) {
        if (EntityImpl.class.isAssignableFrom(value.getClass())) {
            final EntityImpl entity = (EntityImpl) value;
            value = createKey(entity.getKey());
        } else if (com.maintainer.data.provider.Key.class.isAssignableFrom(value.getClass())) {
            final com.maintainer.data.provider.Key k = (com.maintainer.data.provider.Key) value;
            value = createKey(k);
        }
        q.addFilter(propertyName.trim(), operator, value);
    }

    private Key createKey(final com.maintainer.data.provider.Key k) {
        return createKey(k.getKindName(), k.getId());
    }

    @Override
    public List<Map<String, Object>> find(final Query query) {
        final DatastoreService datastore = getDatastore();

        if (query.getKey() != null) {
            try {
                final Entity entity = datastore.get(createKey(query.getKey()));
                final Map<String, Object> fromEntity = fromEntity(query.getKind(), entity);
                final ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
                list.add(fromEntity);
                return list;
            } catch (final EntityNotFoundException e1) {
                e1.printStackTrace();
            }
            return Collections.emptyList();
        }

        final com.google.appengine.api.datastore.Query q = new com.google.appengine.api.datastore.Query(getKindName(query.getKind()));

        for (final Entry<String, Object> e : query.entrySet()) {
            String key = e.getKey();

            final String[] split = key.split("(\\s|:)");
            key = split[0];

            FilterOperator op = null;
            if (split.length > 1) {
                final String o = split[1];
                op = ops.get(o);
            }

            if (op == null) {
                op = FilterOperator.EQUAL;
            }

            Object value = e.getValue();

            final Class<?> keyType = Utils.getKeyType(query.getKind(), key);
            value = Utils.convert(value, keyType);

            addFilter(q, key, op, value);
        }

        if (query.getOrder() != null) {
            final String[] fields = StringUtils.split(query.getOrder(), ',');
            for (String field : fields) {


                if (field.startsWith("-")) {
                    field = field.substring(1);
                    if (!"id".equals(field.toLowerCase())) {
                        q.addSort(field, SortDirection.DESCENDING);
                    } else {
                        q.addSort(Entity.KEY_RESERVED_PROPERTY, SortDirection.DESCENDING);
                    }
                } else {
                    if (!"id".equals(field.toLowerCase())) {
                        if (field.startsWith("+")) {
                            field = field.substring(1);
                        }
                        q.addSort(field);
                    }
                }
            }
        }

        final FetchOptions options = FetchOptions.Builder.withDefaults();
        if (query.getOffset() > 0) {
            options.offset(query.getOffset());
        }

        if (query.getLimit() > 0) {
            options.limit(query.getLimit());
        }

        final PreparedQuery p = datastore.prepare(q);

        final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        final List<Entity> entities = p.asList(options);
        for (final Entity e : entities) {
            final Map<String, Object> target = fromEntity(query.getKind(), e);
            list.add(target);
        }

        return list;
    }

    private void autocreate2(final Map<String, Object> target) {
        // TODO Auto-generated method stub
    }
}
