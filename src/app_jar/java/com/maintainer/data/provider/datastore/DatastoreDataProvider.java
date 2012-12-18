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

import com.google.appengine.api.datastore.Blob;
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
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Query.SortPredicate;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.maintainer.data.model.Autocreate;
import com.maintainer.data.model.EntityBase;
import com.maintainer.data.model.EntityImpl;
import com.maintainer.data.model.NotIndexed;
import com.maintainer.data.model.NotStored;
import com.maintainer.data.provider.Filter;
import com.maintainer.data.provider.Query;
import com.maintainer.util.JsonString;
import com.maintainer.util.Utils;

public class DatastoreDataProvider<T extends EntityBase> extends AbstractDatastoreDataProvider<T> {
    private static final Logger log = Logger.getLogger(DatastoreDataProvider.class.getName());
    private static final Cache<String, Object> cache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build();
    private static final AsyncMemcacheService memcache = MemcacheServiceFactory.getAsyncMemcacheService();

    private boolean local;
    private DatastoreService datastore;

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

        final Key k = createDatastoreKey(key);
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

    @Override
    public List<T> getAll(final Class<?> kind) throws Exception {
        final String kindName = getKindName(kind);

        final com.google.appengine.api.datastore.Query q = new com.google.appengine.api.datastore.Query(kindName);
        final FetchOptions options = FetchOptions.Builder.withDefaults();

        final List<T> list = getEntities(q, options, 0);
        return list;
    }

    @Override
    public T post(final T target) throws Exception {
        autocreate(target);

        target.setCreated(new Date());

        final Entity entity = toEntity(null, target);

        final DatastoreService datastore = getDatastore();
        final Key posted = datastore.put(entity);

        final com.maintainer.data.provider.Key nobodyelsesKey = createNobodyelsesKey(posted);
        target.setKey(nobodyelsesKey);
        target.setId(getEncodedKeyString(nobodyelsesKey));

        invalidateCached(nobodyelsesKey);

        return target;
    }

    @Override
    public T put(T target) throws Exception {
        final com.maintainer.data.provider.Key nobodyelsesKey = target.getKey();
        final T existing = get(nobodyelsesKey);

        if (checkEqual(target, existing)) {
            return target;
        } else {
            log.debug(nobodyelsesKey + " changed.");
        }

        autocreate(target);

        if (target.getId() == null) {
            target = post(target);
            return target;
        }

        target.setModified(new Date());

        Entity entity = getEntity(createDatastoreKey(nobodyelsesKey));
        entity = toEntity(entity, target);

        final DatastoreService datastore = getDatastore();
        datastore.put(entity);

        invalidateCached(nobodyelsesKey);

        return target;
    }

    @Override
    public com.maintainer.data.provider.Key delete(final com.maintainer.data.provider.Key key) throws Exception {
        autodelete(key);

        final DatastoreService datastore = getDatastore();
        datastore.delete(createDatastoreKey(key));
        invalidateCached(key);
        return key;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<T> find(final Query query) throws Exception {
        final DatastoreService datastore = getDatastore();

        if (query.getKey() != null) {
            try {
                final Key key = createDatastoreKey(query.getKey());
                final Entity entity = datastore.get(key);
                final T fromEntity = fromEntity(query.getKind(), entity);
                final ResultListImpl<T> list = new ResultListImpl<T>();
                list.add(fromEntity);
                putCache(fromEntity.getKey(), fromEntity);
                return list;
            } catch (final EntityNotFoundException e) {
                e.printStackTrace();
            }
            return ResultListImpl.emptyList();
        }

        final String pageDirection = query.getPageDirection();

        final com.google.appengine.api.datastore.Query q = getQuery(query);

        if (query.getParent() != null) {
            q.setAncestor(createDatastoreKey(query.getParent()));
        }

        FetchOptions options = FetchOptions.Builder.withDefaults();

        try {
            if (Query.PREVIOUS.equals(pageDirection)) {
                final Cursor fromWebSafeString = Cursor.fromWebSafeString(query.getPreviousCursor());
                options.startCursor(fromWebSafeString);
            } else if (Query.NEXT.equals(pageDirection)) {
                final Cursor fromWebSafeString = Cursor.fromWebSafeString(query.getNextCursor());
                options.startCursor(fromWebSafeString);
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        if (query.getOffset() > 0) {
            options.offset(query.getOffset());
        }

        final int limit = query.getLimit();
        if (limit > 0) {
            options.limit(limit + 1);
        }

        if (query.isKeysOnly()) {
            q.setKeysOnly();
        }

        final ResultListImpl<T> list = getEntities(q, options, limit);

        if (!list.isEmpty()) {
            boolean hasMoreRecords = false;
            if (limit > 0) {
                hasMoreRecords = list.size() > limit;
            }

            if (hasMoreRecords) {
                list.remove(list.size() - 1);
            }

            if (Query.PREVIOUS.equals(pageDirection)) {
                Collections.reverse(list);
                final Cursor end = list.getStartCursor();
                final Cursor start = list.getEndCursor();
                list.setStartCursor(start);
                list.setEndCursor(end);

                if (!hasMoreRecords) {
                    list.setStartCursor(null);
                }
            } else if (Query.NEXT.equals(pageDirection)) {
                if (!hasMoreRecords) {
                    list.setEndCursor(null);
                }
            } else {
                if (containsEqualOrIn(q)) {
                    list.setStartCursor(null);
                    list.setEndCursor(null);
                } else {
                    if (!hasMoreRecords) {
                        list.setEndCursor(null);
                    }

                    options = cloneOptionsWithoutCursors(options);

                    final boolean empty = testBoundary(q, options);
                    if (empty) {
                        list.setStartCursor(null);
                    }
                }
            }

            if (options.getStartCursor() == null) {
                list.setStartCursor(null);
            }
        }

        return list;
    }

    protected boolean checkEqual(final T target, final T existing) throws Exception {
        return isEqual(target, existing);
    }

    private Entity getEntity(final Key key) throws EntityNotFoundException {
        final DatastoreService datastore = getDatastore();
        final Entity entity = datastore.get(key);
        return entity;
    }

    @SuppressWarnings("unchecked")
    public T fromEntity(final Class<?> kind, final Entity entity) {
        T obj = null;

        try {
            final Constructor<T> c = (Constructor<T>) kind.getDeclaredConstructor((Class[]) null);
            c.setAccessible(true);
            obj = c.newInstance((Object[]) null);

            final Map<String, Object> properties = entity.getProperties();
            final ArrayList<Field> fields = getFields(obj);
            for (final Field f : fields) {
                final NotStored notStored = f.getAnnotation(NotStored.class);
                if (notStored != null) {
                    continue;
                }

                f.setAccessible(true);

                final Autocreate autocreate = f.getAnnotation(Autocreate.class);
                final String key = f.getName();
                Object value = properties.get(key);

                if (value != null) {
                    if (autocreate != null && autocreate.embedded()) {
                        value = getEmbedded(f, value);
                    } else if (Key.class.isAssignableFrom(value.getClass())) {
                        final Key k = (Key) value;
                        final com.maintainer.data.provider.Key key2 = createNobodyelsesKey(k);
                        if (autocreate != null && autocreate.keysOnly()) {
                            value = Utils.getKeyedOnly(key2);
                        } else {
                            value = get(key2);
                        }
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
                                if (autocreate != null && autocreate.keysOnly()) {
                                    o = Utils.getKeyedOnly(key2);
                                } else {
                                    o = get(key2);
                                }
                                iterator.set(o);
                            }
                        }
                        value = list;
                    }

                    if (JsonString.class.isAssignableFrom(f.getType())) {
                        value = new JsonString(value.toString());
                    }

                    f.set(obj, value);
                }
            }

            final Key key = entity.getKey();
            final com.maintainer.data.provider.Key nobodyelsesKey = createNobodyelsesKey(key);
            nobodyelsesKey.setKind(kind);

            obj.setKey(nobodyelsesKey);
            obj.setId(getEncodedKeyString(nobodyelsesKey));

            if (key.getParent() != null) {
                final Autocreate autocreate = obj.getClass().getAnnotation(Autocreate.class);
                if (autocreate != null && !Autocreate.EMPTY.equals(autocreate.parent())) {
                    final Field field = Utils.getField(obj, autocreate.parent());
                    field.setAccessible(true);

                    EntityImpl parent = null;
                    final Autocreate fieldAutocreate = field.getAnnotation(Autocreate.class);
                    if (fieldAutocreate != null && fieldAutocreate.keysOnly()) {
                        parent = Utils.getKeyedOnly(nobodyelsesKey.getParent());
                    } else {
                        parent = (EntityImpl) get(nobodyelsesKey.getParent());
                    }
                    field.set(obj, parent);
                    obj.setParent(parent);
                }
            }

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        return obj;
    }

    protected Object getEmbedded(final Field f, Object value) {
        if (Text.class.isAssignableFrom(value.getClass())) {
            value = ((Text) value).getValue();
        }
        final String json = (String) value;
        value = Utils.getGson().fromJson(json, f.getType());
        return value;
    }

    protected DatastoreService getDatastore() {
        if (datastore == null) {
            datastore = DatastoreServiceFactory.getDatastoreService();
        }
        return datastore;
    }

    protected String getKindName(final Class<?> clazz) {
        return com.maintainer.data.provider.Key.getKindName(clazz);
    }

    private Entity newEntity(final EntityBase parent, final String kind) {
        if (parent != null) {
            return new Entity(kind, createDatastoreKey(parent.getKey()));
        }
        return new Entity(kind);
    }

    @SuppressWarnings("unchecked")
    protected Entity toEntity(Entity entity, final T target) throws Exception {

        if (entity == null) {
            final Class<? extends EntityBase> clazz = target.getClass();
            final Autocreate annotation = clazz.getAnnotation(Autocreate.class);

            EntityBase parent = target.getParent();
            if (annotation != null && !Autocreate.EMPTY.equals(annotation.parent())) {
                parent = (EntityBase) Utils.getFieldValue(target, annotation.parent());
            }

            final String kindName = getKindName(clazz);

            if (annotation != null && !Autocreate.EMPTY.equals(annotation.id())) {

                Object id = Utils.getFieldValue(target, annotation.id());

                if (id != null) {
                    if (EntityBase.class.isAssignableFrom(id.getClass())) {
                        id = ((EntityBase) id).getKey().toString();
                    }

                    Key key = null;
                    key = createDatastoreKey(parent, kindName, id);
                    final com.maintainer.data.provider.Key nobodyelsesKey = new com.maintainer.data.provider.Key(clazz.getName(), id);
                    target.setId(getEncodedKeyString(nobodyelsesKey));
                    entity = newEntity(key);
                } else {
                    entity = newEntity(parent, kindName);
                }
            } else {
                entity = newEntity(parent, kindName);
            }
        }

        final ArrayList<Field> fields = getFields(target);

        for (final Field f : fields) {
            final NotStored notStored = f.getAnnotation(NotStored.class);
            if (notStored != null) {
                continue;
            }

            f.setAccessible(true);
            Object value = f.get(target);

            final Autocreate autocreate = f.getAnnotation(Autocreate.class);

            if (autocreate != null) {
                try {
                    if (value != null) {
                        if (autocreate.embedded()) {
                            value = Utils.getGson().toJson(value);
                        } else if (EntityBase.class.isAssignableFrom(value.getClass())) {
                            final EntityBase base = (EntityBase) value;
                            value = createDatastoreKey(base.getKey());
                        } else if (Collection.class.isAssignableFrom(value.getClass())) {
                            final List<Object> list = new ArrayList<Object>((Collection<Object>) value);
                            value = list;

                            final ListIterator<Object> iterator = list.listIterator();
                            while(iterator.hasNext()) {
                                final Object o = iterator.next();
                                if (EntityBase.class.isAssignableFrom(o.getClass())) {
                                    final EntityBase base = (EntityBase) o;
                                    final Key key = createDatastoreKey(base.getKey());
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
                if (JsonString.class.isAssignableFrom(value.getClass())) {
                    value = ((JsonString) value).getString();
                }

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

            final NotIndexed notIndexed = f.getAnnotation(NotIndexed.class);
            if (notIndexed == null) {
                entity.setProperty(f.getName(), value);
            } else {
                entity.setUnindexedProperty(f.getName(), value);
            }
        }

        return entity;
    }

    private Entity newEntity(final Key key) {
        return new Entity(key);
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

    private boolean containsEqualOrIn(final com.google.appengine.api.datastore.Query q) {
        for (final FilterPredicate f : q.getFilterPredicates()) {
            final FilterOperator operator = f.getOperator();
            if (FilterOperator.IN == operator || FilterOperator.EQUAL == operator) {
                return true;
            }
        }
        return false;
    }

    private com.google.appengine.api.datastore.Query getQuery(final Query query) throws Exception {
        final com.google.appengine.api.datastore.Query q = new com.google.appengine.api.datastore.Query(getKindName(query.getKind()));

        for (final Filter e : query.getFilters()) {
            final String condition = e.getCondition();

            final String key = getFieldFromCondition(condition);
            final String op = getOperatorFromCondition(condition);

            final FilterOperator operator = getOperator(op);

            final Class<?> keyType = Utils.getKeyType(query.getKind(), key);
            Object value = e.getValue();
            value = Utils.convert(value, keyType);

            addFilter(q, key, operator, value);
        }

        final String pageDirection = query.getPageDirection();

        if (!Strings.isNullOrEmpty(query.getOrder())) {
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
        } else if (hasInequalityFilter(query)) {
            final String field = getInequalityField(query);
            q.addSort(field);
        }

        if (Query.PREVIOUS.equals(pageDirection)) {
            q.addSort(Entity.KEY_RESERVED_PROPERTY, SortDirection.DESCENDING);
        } else {
            q.addSort(Entity.KEY_RESERVED_PROPERTY, SortDirection.ASCENDING);
        }
        return q;
    }

    private String getInequalityField(final Query query) {
        for (final Filter f : query.getFilters()) {
            final String condition = f.getCondition();

            final String op = this.getOperatorFromCondition(condition);
            final boolean b = this.isInequalityOperator(op);
            if (b) {
                return this.getFieldFromCondition(condition);
            }
        }
        return null;
    }

    private boolean hasInequalityFilter(final Query query) {
        for (final Filter f : query.getFilters()) {
            final String condition = f.getCondition();

            final String op = this.getOperatorFromCondition(condition);
            final boolean b = this.isInequalityOperator(op);
            if (b) {
                return true;
            }
        }

        return false;
    }

    private boolean testBoundary(com.google.appengine.api.datastore.Query q, final FetchOptions options) throws Exception {
        q = reverse(q);
        q.setKeysOnly();
        options.limit(1);

        final DatastoreService datastore = getDatastore();

        try {
            final PreparedQuery p = datastore.prepare(q);
            final List<Entity> list = p.asList(options);
            if (!list.isEmpty()) {
                return false;
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private com.google.appengine.api.datastore.Query reverse(final com.google.appengine.api.datastore.Query q) {
        final com.google.appengine.api.datastore.Query q2 = new com.google.appengine.api.datastore.Query(q.getKind());

        for (final FilterPredicate f : q.getFilterPredicates()) {
            final FilterOperator operator = f.getOperator();
            if (FilterOperator.GREATER_THAN == operator) {
                q2.addFilter(f.getPropertyName(), FilterOperator.LESS_THAN_OR_EQUAL, f.getValue());
            } else if (FilterOperator.GREATER_THAN_OR_EQUAL == operator) {
                q2.addFilter(f.getPropertyName(), FilterOperator.LESS_THAN, f.getValue());
            } else if (FilterOperator.LESS_THAN == operator) {
                q2.addFilter(f.getPropertyName(), FilterOperator.GREATER_THAN_OR_EQUAL, f.getValue());
            } else if (FilterOperator.LESS_THAN_OR_EQUAL == operator) {
                q2.addFilter(f.getPropertyName(), FilterOperator.GREATER_THAN, f.getValue());
            } else {
                q2.addFilter(f.getPropertyName(), operator, f.getValue());
            }
        }

        for (final SortPredicate s : q.getSortPredicates()) {
            final SortPredicate reverse = s.reverse();
            q2.addSort(reverse.getPropertyName(), reverse.getDirection());
        }

        if (q.getAncestor() != null) {
            q2.setAncestor(q.getAncestor());
        }

        if (q.isKeysOnly()) {
            q2.setKeysOnly();
        }

        return q2;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<T> getAll(final Collection<com.maintainer.data.provider.Key> keysNeeded) throws Exception {
        final List<T> list = new ArrayList<T>(keysNeeded.size());

        final Map<com.maintainer.data.provider.Key, Object> map = getCachedAndTrimKeysNeeded(keysNeeded);
        for (final Entry<com.maintainer.data.provider.Key, Object> e : map.entrySet()) {
            final com.maintainer.data.provider.Key key = e.getKey();
            final T t = (T) e.getValue();

            // because the keys are not in the cache
            if (t.getKey() ==  null) {
                t.setKey(key);
                t.setId(getEncodedKeyString(key));
            }

            list.add(t);
        }

        final List<Key> keys = new ArrayList<Key>();
        for (final com.maintainer.data.provider.Key key : keysNeeded) {
            keys.add(createDatastoreKey(key));
        }

        final Map<com.maintainer.data.provider.Key, Object> needsToBeCachedMap = new LinkedHashMap<com.maintainer.data.provider.Key, Object>();
        final Map<Key, Entity> map3 = getDatastore().get(keys);
        for (final Entry<Key, Entity> e : map3.entrySet()) {
            final Key key = e.getKey();
            final Entity entity = e.getValue();

            final T target = fromEntity(getClazz(key), entity);
            list.add(target);
            needsToBeCachedMap.put(target.getKey(), target);
        }
        putAllCache(needsToBeCachedMap);

        return list;
    }

    @SuppressWarnings("unchecked")
    private ResultListImpl<T> getEntities(final com.google.appengine.api.datastore.Query q, FetchOptions options, final int limit) throws Exception {
        final boolean isKeysOnly = q.isKeysOnly();

        if (!isKeysOnly) {
            q.setKeysOnly();
        }

        final DatastoreService datastore = getDatastore();
        final PreparedQuery p = datastore.prepare(q);

        final List<Entity> entities = new ArrayList<Entity>();
        final List<com.maintainer.data.provider.Key> keysNeeded = new ArrayList<com.maintainer.data.provider.Key>();

        final Map<Key, String> cursors = new LinkedHashMap<Key, String>();
        Cursor start = null;
        Cursor end = null;
        boolean removedCursors = false;

        for (int i = 0; i < 2; i++) {
            final QueryResultIterator<Entity> iterator = p.asQueryResultIterator(options);

            try {
                while (iterator.hasNext()) {
                    final Entity e = iterator.next();
                    if (start == null) {
                        start = iterator.getCursor();
                    }
                    final Key k = e.getKey();
                    final com.maintainer.data.provider.Key key = createNobodyelsesKey(k);
                    keysNeeded.add(key);
                    entities.add(e);

                    if (limit > 0 && entities.size() >= limit && end == null) {
                        end = iterator.getCursor();
                    }
                }
                break;
            } catch (final IllegalArgumentException e) {
                if (options != null && (options.getStartCursor() != null || options.getEndCursor() != null)) {
                    System.out.println("Cursor may not be relevant for query. Trying again without cursors.");
                    removedCursors = true;
                    options = cloneOptionsWithoutCursors(options);
                } else {
                    e.printStackTrace();
                    break;
                }
            } catch (final Exception e) {
                e.printStackTrace();
                break;
            }
        }

        if (isKeysOnly) {
            final ResultListImpl<T> list = new ResultListImpl<T>(keysNeeded.size());
            for (final com.maintainer.data.provider.Key key : keysNeeded) {
                final EntityImpl keyedOnly = Utils.getKeyedOnly(key);
                list.add((T) keyedOnly);
            }
            list.setStartCursor(start);
            list.setEndCursor(end);
            list.setRemovedCursors(removedCursors);

            return list;
        }

        final Map<com.maintainer.data.provider.Key, Object> map = new LinkedHashMap<com.maintainer.data.provider.Key, Object>();
        final Map<com.maintainer.data.provider.Key, Object> map2 = getCachedAndTrimKeysNeeded(keysNeeded);

        if (!map2.isEmpty()) {
            map.putAll(map2);
        }

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
            if (o != null) {
                o.setKey(key);
                list.add(o);
            }
        }

        list.setStartCursor(start);
        list.setEndCursor(end);
        list.setRemovedCursors(removedCursors);

        return list;
    }

    public Map<com.maintainer.data.provider.Key, Object> getCachedAndTrimKeysNeeded(final Collection<com.maintainer.data.provider.Key> keysNeeded) throws Exception {
        final Map<com.maintainer.data.provider.Key, Object> map2 = getAllCache(keysNeeded);
        keysNeeded.removeAll(map2.keySet());
        return map2;
    }

    private FetchOptions cloneOptionsWithoutCursors(final FetchOptions options) {
        final FetchOptions options2 = FetchOptions.Builder.withDefaults();
        if (options.getLimit() != null) {
            options2.limit(options.getLimit());
        }
        if (options.getOffset() != null) {
            options2.offset(options.getOffset());
        }
        if (options.getChunkSize() != null) {
            options2.chunkSize(options.getChunkSize());
        }
        if (options.getPrefetchSize() != null) {
            options2.prefetchSize(options.getPrefetchSize());
        }
        return options2;
    }

    private void putAllCache(final Map<com.maintainer.data.provider.Key, Object> map) {
        putAllLocalCache(map);
        putAllMemcache(map);
    }

    private void putAllMemcache(final Map<com.maintainer.data.provider.Key, Object> map) {
        final Map<String, Object> cacheable = new HashMap<String, Object>();
        for (final Entry<com.maintainer.data.provider.Key, Object> e : map.entrySet()) {
            cacheable.put(e.getKey().toString(), e.getValue());
        }

        memcache.putAll(cacheable);
    }

    private void putAllLocalCache(final Map<com.maintainer.data.provider.Key, Object> map) {
        if (!local) {
            return;
        }

        for (final Entry<com.maintainer.data.provider.Key, Object> e : map.entrySet()) {
            putLocalCache(e.getKey(), e.getValue());
        }
    }

    private void putCache(final com.maintainer.data.provider.Key key, final Object o) {
        putLocalCache(key, o);
        putMemcache(key, o);
    }

    private void putMemcache(final com.maintainer.data.provider.Key key, final Object o) {
        memcache.put(key.toString(), o);
    }

    private Object getCached(final com.maintainer.data.provider.Key key) throws Exception {
        Object o = getLocalCache(key);
        if (o == null) {
            final Future<Object> future = memcache.get(key.toString());
            o = future.get();
            if (o != null) {
                putLocalCache(key, o);
            }
        }
        return o;
    }

    private void invalidateCached(final com.maintainer.data.provider.Key key) {
        invalidateLocalCache(key);
        memcache.delete(key.toString());
    }

    private void putLocalCache(final com.maintainer.data.provider.Key key, final Object o) {
        if (local) {
            cache.put(key.toString(), o);
        }
    }

    private Object getLocalCache(final com.maintainer.data.provider.Key key) {
        if (!local) {
            return null;
        }

        final Object o = cache.getIfPresent(key.toString());
        return o;
    }

    private Map<com.maintainer.data.provider.Key, Object> getAllCache(final Collection<com.maintainer.data.provider.Key> keys) throws Exception {
        final Map<com.maintainer.data.provider.Key, Object> map = new LinkedHashMap<com.maintainer.data.provider.Key, Object>();
        final Map<com.maintainer.data.provider.Key, Object> map2 = getAllLocalCache(keys);
        if (!map2.isEmpty()) {
            map.putAll(map2);
        }
        final Set<com.maintainer.data.provider.Key> keySet = map2.keySet();
        final boolean isChanged = keySet.isEmpty() || keys.removeAll(keySet);
        if (!keys.isEmpty() && isChanged) {

            final List<String> stringKeys = getStringKeys(keys);

            final Future<Map<String, Object>> future = memcache.getAll(stringKeys);
            final Map<String, Object> map3 = future.get();
            if (!map3.isEmpty()) {
                for (final Entry<String, Object> e : map3.entrySet()) {
                    map.put(com.maintainer.data.provider.Key.fromString(e.getKey()), e.getValue());
                }
            }
        }
        return map;
    }

    private Map<com.maintainer.data.provider.Key, Object> getAllLocalCache(final Collection<com.maintainer.data.provider.Key> keys) {
        if (!local) {
            return Collections.emptyMap();
        }

        final List<String> stringKeys = getStringKeys(keys);

        final ImmutableMap<String, Object> allPresent = cache.getAllPresent(stringKeys);

        final Map<com.maintainer.data.provider.Key, Object> keysPresent = new LinkedHashMap<com.maintainer.data.provider.Key, Object>();
        for (final Entry<String, Object> e : allPresent.entrySet()) {
            keysPresent.put(com.maintainer.data.provider.Key.fromString(e.getKey()), e.getValue());
        }

        return keysPresent;
    }

    private List<String> getStringKeys(final Collection<com.maintainer.data.provider.Key> keys) {
        final List<String> stringKeys = new ArrayList<String>();
        for (final com.maintainer.data.provider.Key k : keys) {
            stringKeys.add(k.toString());
        }
        return stringKeys;
    }

    private void invalidateLocalCache(final com.maintainer.data.provider.Key key) {
        if (local) {
            cache.invalidate(key.toString());
        }
    }

    public static void writeBlob(final String folder, final String name, final byte[] bytes) {
        writeBlob(folder, name, bytes, true);
    }

    public static void writeBlob(final String folder, final String name, final byte[] bytes, final boolean cache) {
        if (Utils.isEmpty(folder) || Utils.isEmpty(name) || bytes == null) return;

        final Blob blob = new Blob(bytes);
        final Entity entity = new Entity(folder, name);
        entity.setUnindexedProperty("content", blob);

        DatastoreServiceFactory.getDatastoreService().put(entity);

        if (cache) {
            final Key key = entity.getKey();
            final String keyToString = KeyFactory.keyToString(key);
            MemcacheServiceFactory.getMemcacheService().put(keyToString, bytes);
        }
    }

    public static byte[] readBlob(final String folder, final String name) {
        final Key key = KeyFactory.createKey(folder,  name);

        final String keyToString = KeyFactory.keyToString(key);
        byte[] bytes = (byte[]) MemcacheServiceFactory.getMemcacheService().get(keyToString);
        if (bytes != null) {
            return bytes;
        }

        try {
            final Entity entity = DatastoreServiceFactory.getDatastoreService().get(key);
            final Blob blob = (Blob) entity.getProperty("content");
            if (blob != null) {
                bytes = blob.getBytes();
                MemcacheServiceFactory.getMemcacheService().put(keyToString, bytes);
                return bytes;
            }
        } catch (final EntityNotFoundException e) {}
        return null;
    }

    public static void deleteBlob(final String folder, final String name) {
        final Key key = KeyFactory.createKey(folder, name);
        DatastoreServiceFactory.getAsyncDatastoreService().delete(key);
        final String keyToString = KeyFactory.keyToString(key);
        MemcacheServiceFactory.getAsyncMemcacheService().delete(keyToString);
    }
}
