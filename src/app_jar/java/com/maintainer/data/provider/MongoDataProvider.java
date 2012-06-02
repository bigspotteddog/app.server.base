package com.maintainer.data.provider;

import java.util.List;
import java.util.Map.Entry;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.query.UpdateOperations;
import com.maintainer.data.model.EntityBase;

public class MongoDataProvider<T extends EntityBase> extends AbstractDataProvider<T> {

    @Override
    protected void merge(T incoming, T existing) throws Exception {
        autocreate(incoming);
        super.merge(incoming, existing);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(Key key) {
        Datastore datastore = getDatastore();
        T obj = (T) datastore.get(key.getKind(), key.getId());
        return obj;
    }

    @Override
    public List<T> find(com.maintainer.data.provider.Query query) {
        com.google.code.morphia.query.Query<T> q = createQueryInternal(query.getKind());

        for (Entry<String, Object> e : query.entrySet()) {
            String key = e.getKey();
            if (key.endsWith(Query.GE)) {
                key = key.substring(0, key.length() - 3);
                q.filter(key + " >=", e.getValue());
            } else if (key.endsWith(Query.GE_)) {
                key = key.substring(0, key.length() - 2);
                q.filter(key + " >=", e.getValue());
            } else if (key.endsWith(Query.GT)) {
                key = key.substring(0, key.length() - 3);
                q.filter(key + " >", e.getValue());
            } else if (key.endsWith(Query.GE_)) {
                key = key.substring(0, key.length() - 1);
                q.filter(key + " >", e.getValue());
            } else if (key.endsWith(Query.LE)) {
                key = key.substring(0, key.length() - 3);
                q.filter(key + " <=", e.getValue());
            } else if (key.endsWith(Query.LE_)) {
                key = key.substring(0, key.length() - 2);
                q.filter(key + " <=", e.getValue());
            } else if (key.endsWith(Query.LT)) {
                key = key.substring(0, key.length() - 3);
                q.filter(key + " <", e.getValue());
            } else if (key.endsWith(Query.LT_)) {
                key = key.substring(0, key.length() - 1);
                q.filter(key + " <", e.getValue());
            } else {
                q.filter(key, e.getValue());
            }
        }

        if (query.getOrder() != null) {
            String order = query.getOrder();
            order = order.replaceFirst("\\bid", "_id");
            q.order(order);
        }

        if (query.getOffset() > 0) {
            q.offset(query.getOffset());
        }

        if (query.getLimit() > 0) {
            q.limit(query.getLimit());
        }

        List<T> list = q.asList();
        return list;
    }

    @Override
    public List<T> getAll(Class<?> kind) throws Exception {
        com.google.code.morphia.query.Query<T> find = createQueryInternal(kind);
        List<T> list = find.asList();
        return list;
    }

    @SuppressWarnings("unchecked")
    private com.google.code.morphia.query.Query<T> createQueryInternal(Class<?> kind) {
        Datastore datastore = getDatastore();
        com.google.code.morphia.query.Query<T> find = (com.google.code.morphia.query.Query<T>) datastore.find(kind);
        return find;
    }

    @Override
    public T post(T obj) throws Exception {
        autocreate(obj);

        Datastore datastore = getDatastore();

        Long id = obj.getId();
        if (id == null) {
            String collName = datastore.getCollection(obj.getClass()).getName();
            com.google.code.morphia.query.Query<StoredId> q = datastore.find(StoredId.class, "_id", collName);
            UpdateOperations<StoredId> uOps = datastore.createUpdateOperations(StoredId.class).inc("value");
            StoredId newId = datastore.findAndModify(q, uOps);
            if (newId == null) {
               newId = new StoredId(collName);
               datastore.save(newId);
            }

            id = newId.getValue();
            obj.setId(id);
        }

        datastore.save(obj);

        return obj;
    }

    @Entity(value="ids", noClassnameStored=true)
    public static class StoredId {
        final @Id String className;
        protected Long value = 1L;

        public StoredId(String name) {
            className = name;
        }

        protected StoredId(){
            className = "";
        }

        public Long getValue() {
            return value;
        }
    }

    @Override
    public T put(T obj) throws Exception {
        autocreate(obj);

        Datastore datastore = getDatastore();
        datastore.save(obj);
        return obj;
    }

    @Override
    public Key delete(Key key) {
        autodelete(key);
        Datastore datastore = getDatastore();
        datastore.delete(key.getKind(), key.getId());
        return key;
    }


    private Datastore getDatastore() {
        Datastore datastore = MongoConnection.getInstance().getDatastore();
        return datastore;
    }
}
