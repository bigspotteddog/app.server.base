package com.maintainer.data.provider.datastore;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

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
import com.google.common.collect.Lists;
import com.maintainer.data.model.Autocreate;
import com.maintainer.data.model.EntityBase;
import com.maintainer.data.model.EntityImpl;
import com.maintainer.data.provider.AbstractDataProvider;
import com.maintainer.data.provider.Query;

public class DatastoreDataProvider<T extends EntityBase> extends AbstractDataProvider<T> {
    private static final Logger log = Logger.getLogger(DatastoreDataProvider.class.getName());
    @Override
    public Long getId(Object object) {
        if (object != null && Key.class.isAssignableFrom(object.getClass())) {
            Key key = (Key) object;
            return key.getId();
        }
        return super.getId(object);
    }

    @Override
    public T get(com.maintainer.data.provider.Key key) {
        Key k = createKey(key.getKindName(), key.getId());
        try {
            Entity entity = getEntity(k);
            return fromEntity(key.getKind(), entity);
        } catch (EntityNotFoundException e) {
            //ignore, it will just be null
        }
        return null;
    }

    private Entity getEntity(Key key) throws EntityNotFoundException {
        DatastoreService datastore = getDatastore();
        Entity entity = datastore.get(key);
        return entity;
    }

    @SuppressWarnings("unchecked")
    private T fromEntity(Class<?> kind, Entity entity) {
        T obj = null;

        try {
            Constructor<T> c = (Constructor<T>) kind.getDeclaredConstructor((Class[]) null);
            c.setAccessible(true);
            obj = c.newInstance((Object[]) null);

            Map<String, Object> properties = entity.getProperties();
            ArrayList<Field> fields = getFields(obj);
            for (Field f : fields) {
                f.setAccessible(true);

                String key = f.getName();
                Object value = properties.get(key);

                if (value != null) {
                    if (Key.class.isAssignableFrom(value.getClass())) {
                        Key k = (Key) value;
                        String className = k.getKind();
                        Class<?> class1 = Class.forName(className);
                        value = get(new com.maintainer.data.provider.Key(class1, k.getId()));
                    } else if (Collection.class.isAssignableFrom(value.getClass())) {
                        List<Object> list = new ArrayList<Object>((Collection<? extends Object>) value);

                        ListIterator<Object> iterator = list.listIterator();
                        while(iterator.hasNext()) {
                            Object o = iterator.next();
                            if (Key.class.isAssignableFrom(o.getClass())) {
                                Key k = (Key) o;
                                String className = k.getKind();
                                Class<?> class1 = Class.forName(className);
                                o = get(new com.maintainer.data.provider.Key(class1, k.getId()));
                                iterator.set(o);
                            }
                        }
                        value = list;
                    }
                    f.set(obj, value);
                }
            }

            obj.setId(entity.getKey().getId());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return obj;
    }

    private Key createKey(String kind, long id) {
        Key key = KeyFactory.createKey(kind, id);
        return key;
    }

    private DatastoreService getDatastore() {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        return datastore;
    }

    private String getKindName(Class<?> clazz) {
        return com.maintainer.data.provider.Key.getKindName(clazz);
    }

    @Override
    public List<T> getAll(Class<?> kind) throws Exception {
        com.google.appengine.api.datastore.Query q = new com.google.appengine.api.datastore.Query(getKindName(kind));
        DatastoreService datastore = getDatastore();
        PreparedQuery p = datastore.prepare(q);
        List<T> list = new ArrayList<T>();
        List<Entity> entities = p.asList(FetchOptions.Builder.withDefaults());
        for (Entity e : entities) {
            T obj = fromEntity(kind, e);
            list.add(obj);
        }
        return list;
    }

    @Override
    public T post(T target) throws Exception {
        autocreate(target);

        Entity entity = new Entity(getKindName(target.getClass()));
        entity = toEntity(entity, target);

        DatastoreService datastore = getDatastore();
        Key posted = datastore.put(entity);
        target.setId(posted.getId());
        return target;
    }

    @Override
    public T put(T target) throws Exception {
        autocreate(target);

        Entity entity = getEntity(KeyFactory.createKey(getKindName(target.getClass()), target.getId()));
        entity = toEntity(entity, target);

        DatastoreService datastore = getDatastore();
        Key posted = datastore.put(entity);
        target.setId(posted.getId());
        return target;
    }

    @SuppressWarnings("unchecked")
    private Entity toEntity(Entity entity, T target) throws Exception {

        ArrayList<Field> fields = getFields(target);

        for (Field f : fields) {
            f.setAccessible(true);
            Object value = f.get(target);

            Autocreate annotation = f.getAnnotation(Autocreate.class);
            if (annotation != null) {
                try {
                    if (value != null) {
                        if (EntityBase.class.isAssignableFrom(value.getClass())) {
                            EntityBase base = (EntityBase) value;
                            value = KeyFactory.createKey(getKindName(base.getClass()), base.getId());
                        } else if (Collection.class.isAssignableFrom(value.getClass())) {
                            List<Object> list = new ArrayList<Object>((Collection<Object>) value);
                            value = list;

                            ListIterator<Object> iterator = list.listIterator();
                            while(iterator.hasNext()) {
                                Object o = iterator.next();
                                if (EntityBase.class.isAssignableFrom(o.getClass())) {
                                    EntityBase base = (EntityBase) o;
                                    Key key = KeyFactory.createKey(getKindName(base.getClass()), base.getId());
                                    iterator.set(key);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            entity.setProperty(f.getName(), value);
        }
        return entity;
    }

    private ArrayList<Field> getFields(T target) {
        ArrayList<Field> fields = new ArrayList<Field>();
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            log.debug("clazz = " + clazz.getName());
            Field[] fields2 = clazz.getDeclaredFields();
            fields.addAll(Lists.newArrayList(fields2));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    @Override
    public com.maintainer.data.provider.Key delete(com.maintainer.data.provider.Key key) throws Exception {
        autodelete(key);

        DatastoreService datastore = getDatastore();
        datastore.delete(createKey(key.getKindName(), key.getId()));
        return key;
    }

    private void addFilter(com.google.appengine.api.datastore.Query q, String propertyName, FilterOperator operator, Object value) {
        if (EntityImpl.class.isAssignableFrom(value.getClass())) {
            EntityImpl entity = (EntityImpl) value;
            value = createKey(entity.getKey());
        } else if (com.maintainer.data.provider.Key.class.isAssignableFrom(value.getClass())) {
            com.maintainer.data.provider.Key k = (com.maintainer.data.provider.Key) value;
            value = createKey(k);
        }
        q.addFilter(propertyName.trim(), operator, value);
    }

    private Key createKey(com.maintainer.data.provider.Key k) {
        return createKey(k.getKindName(), k.getId());
    }

    @Override
    public List<T> find(Query query) {
        DatastoreService datastore = getDatastore();

        if (query.getKey() != null) {
            try {
                Entity entity = datastore.get(createKey(query.getKey()));
                T fromEntity = fromEntity(query.getKind(), entity);
                ArrayList<T> list = new ArrayList<T>();
                list.add(fromEntity);
                return list;
            } catch (EntityNotFoundException e1) {
                e1.printStackTrace();
            }
            return Collections.emptyList();
        }

        com.google.appengine.api.datastore.Query q = new com.google.appengine.api.datastore.Query(getKindName(query.getKind()));

        for (Entry<String, Object> e : query.entrySet()) {
            String key = e.getKey();
            Object value = e.getValue();
            if (key.endsWith(Query.GE)) {
                key = key.substring(0, key.length() - 3);
                addFilter(q, key, FilterOperator.GREATER_THAN_OR_EQUAL, value);
            } else if (key.endsWith(Query.GE_)) {
                key = key.substring(0, key.length() - 2);
                addFilter(q, key, FilterOperator.GREATER_THAN_OR_EQUAL, value);
            } else if (key.endsWith(Query.GT)) {
                key = key.substring(0, key.length() - 3);
                addFilter(q, key, FilterOperator.GREATER_THAN, value);
            } else if (key.endsWith(Query.GE_)) {
                key = key.substring(0, key.length() - 1);
                addFilter(q, key, FilterOperator.GREATER_THAN, value);
            } else if (key.endsWith(Query.LE)) {
                key = key.substring(0, key.length() - 3);
                addFilter(q, key, FilterOperator.LESS_THAN_OR_EQUAL, value);
            } else if (key.endsWith(Query.LE_)) {
                key = key.substring(0, key.length() - 2);
                addFilter(q, key, FilterOperator.LESS_THAN_OR_EQUAL, value);
            } else if (key.endsWith(Query.LT)) {
                key = key.substring(0, key.length() - 3);
                addFilter(q, key, FilterOperator.LESS_THAN, value);
            } else if (key.endsWith(Query.LT_)) {
                key = key.substring(0, key.length() - 1);
                addFilter(q, key, FilterOperator.LESS_THAN, value);
            } else {
                addFilter(q, key, FilterOperator.EQUAL, value);
            }
        }

        if (query.getOrder() != null) {
            String[] fields = StringUtils.split(query.getOrder(), ',');
            for (String field : fields) {


                if (field.startsWith("-")) {
                    field = field.substring(1);
                    if (!"id".equals(field.toLowerCase())) {
                        q.addSort(field, SortDirection.DESCENDING);
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

        FetchOptions options = FetchOptions.Builder.withDefaults();
        if (query.getOffset() > 0) {
            options.offset(query.getOffset());
        }

        if (query.getLimit() > 0) {
            options.limit(query.getLimit());
        }

        PreparedQuery p = datastore.prepare(q);

        List<T> list = new ArrayList<T>();
        List<Entity> entities = p.asList(options);
        for (Entity e : entities) {
            T target = fromEntity(query.getKind(), e);
            list.add(target);
        }

        return list;
    }
}
