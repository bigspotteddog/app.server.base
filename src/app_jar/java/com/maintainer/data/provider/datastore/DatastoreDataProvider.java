package com.maintainer.data.provider.datastore;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.appengine.api.datastore.Cursor;
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
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.maintainer.data.model.Autocreate;
import com.maintainer.data.model.EntityBase;
import com.maintainer.data.model.EntityImpl;
import com.maintainer.data.provider.AbstractDataProvider;
import com.maintainer.data.provider.Query;
import com.maintainer.util.Utils;

public class DatastoreDataProvider<T extends EntityBase> extends AbstractDataProvider<T> {
    private static final Logger log = Logger.getLogger(DatastoreDataProvider.class.getName());
    private static final Cache<com.maintainer.data.provider.Key, Object> cache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build();
    private static final AsyncMemcacheService memcache = MemcacheServiceFactory.getAsyncMemcacheService();

    private static final Map<String, FilterOperator> ops = new HashMap<String, FilterOperator>();
    private boolean local;

    public DatastoreDataProvider() {
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
    public Object getId(final Object object) {
        if (object != null && Key.class.isAssignableFrom(object.getClass())) {
            final Key key = (Key) object;
            return key.getId();
        }
        return super.getId(object);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(final com.maintainer.data.provider.Key key) throws Exception {
        final T cached = (T) getCached(key);
        if (cached != null) {
            log.debug(key + " returned from cache.");
            return cached;
        }

        final Key k = createDatastoreKey(key.getKindName(), key.getId());
        try {
            final Entity entity = getEntity(k);
            final T fetched = fromEntity(key.getKind(), entity);
            putCache(key, fetched);
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
    private T fromEntity(final Class<?> kind, final Entity entity) {
        T obj = null;

        try {
            final Constructor<T> c = (Constructor<T>) kind.getDeclaredConstructor((Class[]) null);
            c.setAccessible(true);
            obj = c.newInstance((Object[]) null);

            final Map<String, Object> properties = entity.getProperties();
            final ArrayList<Field> fields = getFields(obj);
            for (final Field f : fields) {
                f.setAccessible(true);

                final String key = f.getName();
                Object value = properties.get(key);

                if (value != null) {
                    if (Key.class.isAssignableFrom(value.getClass())) {
                        final Key k = (Key) value;
                        value = get(createNobodyelsesKey(k));
                    } else if (Text.class.isAssignableFrom(value.getClass())) {
                        value = ((Text) value).getValue();
                    } else if (Double.class.isAssignableFrom(value.getClass()) && BigDecimal.class.isAssignableFrom(f.getType())) {
                        value = new BigDecimal(value.toString());
                    } else if (Collection.class.isAssignableFrom(value.getClass())) {
                        final List<Object> list = new ArrayList<Object>((Collection<? extends Object>) value);

                        final ListIterator<Object> iterator = list.listIterator();
                        while(iterator.hasNext()) {
                            Object o = iterator.next();
                            if (Key.class.isAssignableFrom(o.getClass())) {
                                final Key k = (Key) o;
                                final com.maintainer.data.provider.Key key2 = createNobodyelsesKey(k);
                                o = get(key2);
                                iterator.set(o);
                            }
                        }
                        value = list;
                    }
                    f.set(obj, value);
                }
            }

            final Key key = entity.getKey();
            final com.maintainer.data.provider.Key key2 = createNobodyelsesKey(key);
            obj.setId(key2.getId());

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        return obj;
    }

    private DatastoreService getDatastore() {
        final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        return datastore;
    }

    private String getKindName(final Class<?> clazz) {
        return com.maintainer.data.provider.Key.getKindName(clazz);
    }

    @Override
    public List<T> getAll(final Class<?> kind) throws Exception {
        final String kindName = getKindName(kind);

        final com.google.appengine.api.datastore.Query q = new com.google.appengine.api.datastore.Query(kindName);
        final FetchOptions options = FetchOptions.Builder.withDefaults();

        final List<T> list = getEntities(q, options);
        return list;
    }

    @Override
    public T post(final T target) throws Exception {
        autocreate(target);

        target.setModified(new Date());

        final Entity entity = toEntity(null, target);


        final DatastoreService datastore = getDatastore();
        final Key posted = datastore.put(entity);

        if (posted.getId() == 0) {
            target.setId(posted.getName());
        } else {
            target.setId(posted.getId());
        }

        final com.maintainer.data.provider.Key cacheKey = createNobodyelsesKey(posted);
        putCache(cacheKey, target);

        return target;
    }


    @Override
    public T put(T target) throws Exception {
        final com.maintainer.data.provider.Key key = new com.maintainer.data.provider.Key(target.getClass(), target.getId());
        final T existing = get(key);

        if (checkEqual(target, existing)) {
            return target;
        } else {
            log.debug(key + " changed.");
        }

        autocreate(target);

        if (target.getId() == null) {
            target = post(target);
            return target;
        }

        target.setModified(new Date());

        Entity entity = getEntity(createDatastoreKey(getKindName(target.getClass()), target.getId()));
        entity = toEntity(entity, target);

        final DatastoreService datastore = getDatastore();
        final Key posted = datastore.put(entity);

        if (posted.getId() == 0) {
            target.setId(posted.getName());
        } else {
            target.setId(posted.getId());
        }

        putCache(key, target);
        return target;
    }

    protected boolean checkEqual(final T target, final T existing) throws Exception {
        return isEqual(target, existing);
    }

    @SuppressWarnings("unchecked")
    private Entity toEntity(Entity entity, final T target) throws Exception {

        if (entity == null) {
            final Class<? extends EntityBase> clazz = target.getClass();
            final String kindName = getKindName(clazz);

            final Autocreate annotation = clazz.getAnnotation(Autocreate.class);

            if (annotation != null && annotation.id() != Autocreate.EMPTY) {

                Field field = null;
                try {
                    final String id = annotation.id();
                    field = clazz.getDeclaredField(id);
                    field.setAccessible(true);
                } catch (final NoSuchFieldException e) {}

                if (field != null) {
                    final Object value = field.get(target);
                    Key key = null;
                    if (value != null) {
                        key = createDatastoreKey(kindName, value);
                        target.setId(value);
                    }

                    if (key != null) {
                        entity = new Entity(key);
                    } else {
                        entity = new Entity(kindName);
                    }
                } else {
                    entity = new Entity(kindName);
                }
            } else {
                entity = new Entity(kindName);
            }
        }

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
                            value = createDatastoreKey(getKindName(base.getClass()), base.getId());
                        } else if (Collection.class.isAssignableFrom(value.getClass())) {
                            final List<Object> list = new ArrayList<Object>((Collection<Object>) value);
                            value = list;

                            final ListIterator<Object> iterator = list.listIterator();
                            while(iterator.hasNext()) {
                                final Object o = iterator.next();
                                if (EntityBase.class.isAssignableFrom(o.getClass())) {
                                    final EntityBase base = (EntityBase) o;
                                    final Key key = createDatastoreKey(getKindName(base.getClass()), base.getId());
                                    iterator.set(key);
                                }
                            }
                        }
                    }
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }

            if (value != null) {
                if (String.class.isAssignableFrom(value.getClass())) {
                    final String string = (String) value;
                    if (string.length() > 500) {
                        value = new Text(string);
                    }
                } else if (BigDecimal.class.isAssignableFrom(value.getClass())) {
                    final BigDecimal decimal = (BigDecimal) value;
                    value = decimal.doubleValue();
                }
            }

            entity.setProperty(f.getName(), value);
        }

        return entity;
    }

    private ArrayList<Field> getFields(final T target) {
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
        datastore.delete(createDatastoreKey(key.getKindName(), key.getId()));
        invalidateCached(key);
        return key;
    }

    private void addFilter(final com.google.appengine.api.datastore.Query q, final String propertyName, final FilterOperator operator, Object value) {
        if (EntityImpl.class.isAssignableFrom(value.getClass())) {
            final EntityImpl entity = (EntityImpl) value;
            value = createDatastoreKey(entity.getKey());
        } else if (com.maintainer.data.provider.Key.class.isAssignableFrom(value.getClass())) {
            final com.maintainer.data.provider.Key k = (com.maintainer.data.provider.Key) value;
            value = createDatastoreKey(k);
        }
        q.addFilter(propertyName.trim(), operator, value);
    }

    private com.maintainer.data.provider.Key createNobodyelsesKey(final Key k) throws ClassNotFoundException {
        final Class<?> class1 = getClazz(k);

        Object id = null;
        if (k.getId() == 0) {
            id = k.getName();
        } else {
            id = k.getId();
        }

        final com.maintainer.data.provider.Key key = new com.maintainer.data.provider.Key(class1, id);
        return key;
    }

    private Class<?> getClazz(final Key k) throws ClassNotFoundException {
        final String className = k.getKind();
        final Class<?> class1 = Class.forName(className);
        return class1;
    }

    private Key createDatastoreKey(final com.maintainer.data.provider.Key k) {
        return createDatastoreKey(k.getKindName(), k.getId());
    }

    protected Key createDatastoreKey(final String kind, final Object id) {
        Key key = null;

        if (Utils.isNumeric(id.toString())) {
            key = KeyFactory.createKey(kind, new BigDecimal(id.toString()).longValue());
        } else if (Long.class.isAssignableFrom(id.getClass())) {
            key = KeyFactory.createKey(kind, (Long) id);
        } else if (Double.class.isAssignableFrom(id.getClass())){
            key = KeyFactory.createKey(kind, ((Double) id).longValue());
        } else {
            key = KeyFactory.createKey(kind, (String) id);
        }

        return key;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<T> find(final Query query) throws Exception {
        final DatastoreService datastore = getDatastore();

        if (query.getKey() != null) {
            try {
                final Entity entity = datastore.get(createDatastoreKey(query.getKey()));
                final T fromEntity = fromEntity(query.getKind(), entity);
                final ResultListImpl<T> list = new ResultListImpl<T>();
                list.add(fromEntity);
                putCache(fromEntity.getKey(), fromEntity);
                return list;
            } catch (final EntityNotFoundException e1) {
                e1.printStackTrace();
            }
            return ResultListImpl.emptyList();
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

        final String pageDirection = query.getPageDirection();

        if (query.getOrder() != null) {
            final String[] fields = StringUtils.split(query.getOrder(), ',');
            for (String field : fields) {
                if (field.startsWith("-")) {
                    SortDirection sortDirection = SortDirection.DESCENDING;
                    if (Query.PREVIOUS.equals(pageDirection)) {
                        sortDirection = SortDirection.ASCENDING;
                    }

                    field = field.substring(1);
                    if (!"id".equals(field.toLowerCase())) {
                        q.addSort(field, sortDirection);
                    } else {
                        q.addSort(Entity.KEY_RESERVED_PROPERTY, sortDirection);
                    }
                } else {
                    if (!"id".equals(field.toLowerCase())) {
                        SortDirection sortDirection = SortDirection.ASCENDING;
                        if (Query.PREVIOUS.equals(pageDirection)) {
                            sortDirection = SortDirection.DESCENDING;
                        }

                        if (field.startsWith("+")) {
                            field = field.substring(1);
                        }
                        q.addSort(field, sortDirection);
                    }
                }
            }
        }

        final FetchOptions options = FetchOptions.Builder.withDefaults();

        if (Query.PREVIOUS.equals(pageDirection)) {
            final Cursor fromWebSafeString = Cursor.fromWebSafeString(query.getPreviousCursor());
            options.startCursor(fromWebSafeString);
        } else if (Query.NEXT.equals(pageDirection)) {
            final Cursor fromWebSafeString = Cursor.fromWebSafeString(query.getNextCursor());
            options.startCursor(fromWebSafeString);
        }

        if (query.getOffset() > 0) {
            options.offset(query.getOffset());
        }

        final int limit = query.getLimit();
        if (limit > 0) {
            options.limit(limit + 1);
        }

        final ResultListImpl<T> list = getEntities(q, options);

        if (!list.isEmpty()) {
            final boolean hasMoreRecords = list.size() > limit;

            if (hasMoreRecords) {
                list.remove(list.size() - 1);
            }

            if (Query.PREVIOUS.equals(pageDirection)) {
                Collections.reverse(list);
            }

            final String previous = list.get(0).getCursor();
            final String next = list.get(list.size() - 1).getCursor();

            if (Query.PREVIOUS.equals(pageDirection)) {
                if (hasMoreRecords) {
                    list.setStartCursor(previous == null ? null : Cursor.fromWebSafeString(previous));
                }
                list.setEndCursor(next == null? null : Cursor.fromWebSafeString(next));
            } else if (Query.NEXT.equals(pageDirection)) {
                list.setStartCursor(previous == null ? null : Cursor.fromWebSafeString(previous));
                if (hasMoreRecords) {
                    list.setEndCursor(next == null? null : Cursor.fromWebSafeString(next));
                }
            } else {
                list.setEndCursor(next == null? null : Cursor.fromWebSafeString(next));
            }
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    private ResultListImpl<T> getEntities(final com.google.appengine.api.datastore.Query q, final FetchOptions options) throws Exception {
        q.setKeysOnly();

        final DatastoreService datastore = getDatastore();
        final PreparedQuery p = datastore.prepare(q);

        final List<Entity> entities = new ArrayList<Entity>();
        final List<com.maintainer.data.provider.Key> keysNeeded = new ArrayList<com.maintainer.data.provider.Key>();

        final QueryResultIterator<Entity> iterator = p.asQueryResultIterator(options);

        final Map<Key, String> cursors = new LinkedHashMap<Key, String>();
        while (iterator.hasNext()) {
            final Entity e = iterator.next();

            final Key k = e.getKey();

            final Cursor cursor = iterator.getCursor();
            if (cursor != null) {
                cursors.put(k, cursor.toWebSafeString());
            }

            final com.maintainer.data.provider.Key key = createNobodyelsesKey(k);
            keysNeeded.add(key);
            entities.add(e);
        }

        final Map<com.maintainer.data.provider.Key, Object> map = new LinkedHashMap<com.maintainer.data.provider.Key, Object>();
        final Map<com.maintainer.data.provider.Key, Object> map2 = getAllCache(keysNeeded);

        if (!map2.isEmpty()) {
            map.putAll(map2);
        }

        keysNeeded.removeAll(map2.keySet());

        if (!keysNeeded.isEmpty()) {
            final List<Key> keys = new ArrayList<Key>();
            for (final com.maintainer.data.provider.Key key : keysNeeded) {
                keys.add(createDatastoreKey(key));
            }

            final Map<com.maintainer.data.provider.Key, Object> needsToBeCachedMap = new LinkedHashMap<com.maintainer.data.provider.Key, Object>();
            final Map<Key, Entity> map3 = datastore.get(keys);
            for (final Entry<Key, Entity> e : map3.entrySet()) {
                final Key key = e.getKey();
                final Entity entity = e.getValue();

                final String cursor = cursors.get(key);
                if (cursor != null) {
                    entity.setProperty("cursor", cursor);
                }

                final T target = fromEntity(getClazz(key), entity);
                map.put(target.getKey(), target);
                needsToBeCachedMap.put(target.getKey(), target);
            }
            putAllCache(needsToBeCachedMap);
        }

        final ResultListImpl<T> list = new ResultListImpl<T>(map.size());
        for (final Entity e : entities) {
            final Key k = e.getKey();
            final com.maintainer.data.provider.Key key = createNobodyelsesKey(k);
            final T o = (T) map.get(key);
            list.add(o);
        }

        return list;
    }

    private void putAllCache(final Map<com.maintainer.data.provider.Key, Object> map) {
        putAllLocalCache(map);
        memcache.putAll(map);
    }

    private void putAllLocalCache(final Map<com.maintainer.data.provider.Key, Object> map) {
        for (final Entry<com.maintainer.data.provider.Key, Object> e : map.entrySet()) {
            putLocalCache(e.getKey(), e.getValue());
        }
    }

    private void putCache(final com.maintainer.data.provider.Key key, final Object o) {
        putLocalCache(key, o);
        memcache.put(key, o);
    }

    private Object getCached(final com.maintainer.data.provider.Key key) throws Exception {
        Object o = getLocalCache(key);
        if (o == null) {
            final Future<Object> future = memcache.get(key);
            o = future.get();
            if (o != null) {
                putLocalCache(key, o);
            }
        }
        return o;
    }

    private void invalidateCached(final com.maintainer.data.provider.Key key) {
        invalidateLocalCache(key);
        memcache.delete(key);
    }

    private void putLocalCache(final com.maintainer.data.provider.Key key, final Object o) {
        if (!local) {
            cache.put(key, o);
        }
    }

    private Object getLocalCache(final com.maintainer.data.provider.Key key) {
        if (!local) {
            return null;
        }

        final Object o = cache.getIfPresent(key);
        return o;
    }

    private Map<com.maintainer.data.provider.Key, Object> getAllCache(final List<com.maintainer.data.provider.Key> keys) throws Exception {
        final Map<com.maintainer.data.provider.Key, Object> map = new LinkedHashMap<com.maintainer.data.provider.Key, Object>();
        final Map<com.maintainer.data.provider.Key, Object> map2 = getAllLocalCache(keys);
        if (!map2.isEmpty()) {
            map.putAll(map2);
        }
        final Set<com.maintainer.data.provider.Key> keySet = map2.keySet();
        final boolean isChanged = keySet.isEmpty() || keys.removeAll(keySet);
        if (!keys.isEmpty() && isChanged) {
            final Future<Map<com.maintainer.data.provider.Key, Object>> future = memcache.getAll(keys);
            final Map<com.maintainer.data.provider.Key, Object> map3 = future.get();
            if (!map3.isEmpty()) {
                map.putAll(map3);
            }
        }
        return map;
    }

    private Map<com.maintainer.data.provider.Key, Object> getAllLocalCache(final List<com.maintainer.data.provider.Key> keys) {
        final ImmutableMap<com.maintainer.data.provider.Key, Object> allPresent = cache.getAllPresent(keys);
        return allPresent;
    }

    private void invalidateLocalCache(final com.maintainer.data.provider.Key key) {
        cache.invalidate(key);
    }
}
