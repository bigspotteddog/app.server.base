package com.maintainer.data.provider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.maintainer.data.model.Autocreate;
import com.maintainer.data.model.EntityBase;
import com.maintainer.util.MyField;
import com.maintainer.util.Utils;

public abstract class AbstractDataProvider<T> implements DataProvider<T>, AutoCreateVisitor {
    private static final Logger log = Logger.getLogger(AbstractDataProvider.class.getName());

    @Override
    public void beginTransaction() {}

    @Override
    public void commitTransaction() {}

    @Override
    public abstract T get(Key key) throws Exception;

    @Override
    public abstract List<T> getAll(Class<?> kind) throws Exception;

    @Override
    public abstract T put(T item) throws Exception;

    @Override
    public abstract Key delete(Key key) throws Exception;

    @Override
    public abstract List<T> find(Query query) throws Exception;

    protected void preMerge(final T incoming, final T existing) {}

    @Override
    public T merge(final T incoming) throws Exception {
        final Key key = getKey(incoming);
        final T existing = get(key);

        if (existing == null) {
            throw new Exception("not found");
        }

        preMerge(incoming, existing);

        merge(incoming, existing);

        put(existing);

        return existing;
    }

    protected Key getKey(final T target) {
        return ((EntityBase) target).getKey();
    }

    protected void merge(final T incoming, final T existing) throws Exception {
        mergeAny(incoming, existing);
    }

    protected boolean isEqual(final Object incoming, final Object existing) throws Exception {
        boolean equals = false;

        if (incoming != null && existing != null) {
            final String incomingJson = Utils.toJson(incoming);
            final String existingJson = Utils.toJson(existing);

            equals = incomingJson.equals(existingJson);
        } else if (incoming == null && existing == null) {
            equals = true;
        }

        return equals;
    }

    protected void mergeAny(final Object incoming, final Object existing) throws Exception {
        log.debug("mergeAny");

        final List<MyField> fields = getFields(incoming);
        for (final MyField f : fields) {
            final Object value = getFieldValue(incoming, f);
            if (value != null) {
                log.debug(f.getName() + " = " + value);
                setFieldValue(existing, f, value);
            } else {
                final Object value2 = getFieldValue(existing, f);
                if (value2 != null) {
                    log.debug("clearing " + f.getName() + " = " + value);
                    setFieldValue(existing, f, null);
                }
            }
        }
    }

    protected Autocreate getAutocreateAnnotation(final MyField f) {
        f.setAccessible(true);
        return f.getAnnotation(Autocreate.class);
    }

    protected Object getFieldValue(final Object obj, final MyField f) throws IllegalAccessException {
        f.setAccessible(true);
        final Object value = f.get(obj);
        return value;
    }

    protected void setFieldValue(final Object obj, final MyField f, final Object value) throws IllegalAccessException {
        f.setAccessible(true);
        f.set(obj, value);
    }

    protected ArrayList<MyField> getFields(final Object target) {
        return getFields(target, true);
    }

    protected ArrayList<MyField> getFields(final Object target, boolean isRecurse) {
        final Map<String, MyField> fieldMap = new LinkedHashMap<String, MyField>();
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            final Field[] fields2 = clazz.getDeclaredFields();
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

            clazz = clazz.getSuperclass();
        }
        return new ArrayList<MyField>(fieldMap.values());
    }

    @SuppressWarnings("unchecked")
    @Override
    public T fromJson(final Class<?> kind, final String json) {
        final T obj = (T) Utils.getGson().fromJson(json, kind);
        return obj;
    }

    @Override
    public EntityBase autocreate(final EntityBase target) throws Exception {

        T existing = null;
        if (!target.isNew()) {
            existing = get(target.getKey());
        }

        final List<MyField> fields = getFields(target, false);
        for (final MyField f : fields) {
            autocreateFromField(target, existing, f);
        }

        return target;
    }

    protected void autocreateFromField(final EntityBase target, final T existing, final MyField f) {
        final Autocreate autocreate = getAutocreateAnnotation(f);
        if (autocreate != null && !autocreate.embedded()) {
            try {
                final Object value = f.get(target);
                if (value != null) {
                    if (EntityBase.class.isAssignableFrom(value.getClass())) {
                        final EntityBase entity = (EntityBase) value;
                        setFieldValue(target, f, createOrUpdate(entity, autocreate));
                    } else if (Collection.class.isAssignableFrom(value.getClass())) {
                        final List<Object> list = new ArrayList<Object>();
                        if (value != null) {
                            list.addAll((Collection<Object>) value);
                        }

                        List<Object> removeThese = null;
                        if (existing != null) {
                            Collection<Object> collection = (Collection<Object>) f.get(existing);
                            if (collection != null) {
                                removeThese = new ArrayList<Object>(collection);
                            }
                        }

                        final ListIterator<Object> iterator = list.listIterator();
                        while(iterator.hasNext()) {
                            final Object o = iterator.next();
                            if (o == null) {
                                continue;
                            }
                            if (EntityBase.class.isAssignableFrom(o.getClass())) {
                                final EntityBase entity = (EntityBase) o;
                                iterator.set(createOrUpdate(entity, autocreate));
                            }
                        }

                        if (removeThese != null && !removeThese.isEmpty()) {
                            removeThese.removeAll(list);
                            for (final Object object : removeThese) {
                                delete(object, autocreate);
                            }
                        }
                    }
                } else {
                    if (existing != null) {
                        final Object object = f.get(existing);
                        delete(object, autocreate);
                    }
                }
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public T autodelete(final Key key) throws Exception {
        final T target = get(key);
        if (target == null) {
            return null;
        }

        final List<MyField> fields = getFields(target, false);
        for (final MyField f : fields) {
            final Autocreate autocreate = getAutocreateAnnotation(f);
            if (autocreate != null) {
                try {
                    final Object object = getFieldValue(target, f);
                    delete(object, autocreate);
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return target;
    }

    @Override
    public Object getId(final Object object) {
        if (object != null && EntityBase.class.isAssignableFrom(object.getClass())) {
            final EntityBase entity = (EntityBase) object;
            return entity.getId();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected EntityBase createOrUpdate(EntityBase target, final Autocreate fieldAutocreate) throws Exception {
        if (target == null) {
            return target;
        }

        final Autocreate classAutocreate = getAutocreate(target);

        boolean readonly = fieldAutocreate.readonly();
        if (readonly) {
            readonly = classAutocreate == null || classAutocreate.readonly();
        }

        if (readonly) {
            return target;
        }

        boolean create = fieldAutocreate.create();
        if (create) {
            create = classAutocreate == null || classAutocreate.create();
        }

        boolean update = fieldAutocreate.update();
        if (update) {
            update = classAutocreate == null || classAutocreate.update();
        }

        if (target.isNew() && create) {
            final DataProvider<EntityBase> dataProvider = (DataProvider<EntityBase>) DataProviderFactory.instance().getDataProvider(target.getClass());
            target = dataProvider.post(target);
        } else if (update) {
            final DataProvider<EntityBase> dataProvider = (DataProvider<EntityBase>) DataProviderFactory.instance().getDataProvider(target.getClass());
            target = dataProvider.put(target);
        }

        return target;
    }

    @SuppressWarnings("unchecked")
    protected void delete(final Object target, final Autocreate fieldAutocreate) throws Exception {
        if (target == null || fieldAutocreate.embedded()) {
            return;
        }

        final Autocreate classAutocreate = getAutocreate(target);

        boolean readonly = fieldAutocreate.readonly();
        if (readonly) {
            readonly = classAutocreate == null || classAutocreate.readonly();
        }

        boolean delete = fieldAutocreate.delete();
        if (delete) {
            delete = classAutocreate == null || classAutocreate.delete();
        }

        if (!readonly && delete) {
            log.debug("deleting: " + target.getClass().getSimpleName());
            if (Collection.class.isAssignableFrom(target.getClass())) {
                final List<Object> list = new ArrayList<Object>((Collection<Object>) target);
                for (final Object o : list) {
                    delete(o, fieldAutocreate);
                }
            } else {
                final Object id = ((EntityBase) target).getId();
                if (id != null) {
                    DataProviderFactory.instance().getDataProvider(target.getClass()).delete(getKey((T) target));
                }
            }
        }
    }

    protected Autocreate getAutocreate(final Object object) {
        final Class<? extends Object> class1 = object.getClass();
        final Autocreate classAutocreate = class1.getAnnotation(Autocreate.class);
        return classAutocreate;
    }
}
