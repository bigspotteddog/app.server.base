package com.maintainer.data.provider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.maintainer.data.model.Autocreate;
import com.maintainer.data.model.EntityBase;
import com.maintainer.data.model.EntityImpl;

public abstract class AbstractDataProvider<T> implements DataProvider<T>, AutoCreateVisitor {
    private static final Logger log = Logger.getLogger(AbstractDataProvider.class.getName());
    private static final Gson gson = new Gson();

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
        final Object id = ((EntityImpl) incoming).getId();

        if (id == null) {
            throw new Exception("no id specified");
        }

        final Key key = new Key(incoming.getClass(), id);
        final T existing = get(key);

        if (existing == null) {
            throw new Exception("not found");
        }

        preMerge(incoming, existing);

        merge(incoming, existing);

        put(existing);

        return existing;
    }

    protected void merge(final T incoming, final T existing) throws Exception {
        mergeAny(incoming, existing);
    }

    protected boolean isEqual(final Object incoming, final Object existing) throws Exception {
        boolean equals = false;

        if (incoming != null && existing != null) {
            final String incomingJson = gson.toJson(incoming);
            final String existingJson = gson.toJson(existing);

            equals = incomingJson.equals(existingJson);
        } else if (incoming == null && existing == null) {
            equals = true;
        }

        return equals;
    }

    protected void mergeAny(final Object incoming, final Object existing) throws Exception {
        log.debug("mergeAny");

        final Field[] fields = getFields(incoming);
        for (final Field f : fields) {
            f.setAccessible(true);

            final Object value = f.get(incoming);
            if (value != null) {
                log.debug(f.getName() + " = " + value);
                f.set(existing, value);
            } else {
                final Object value2 = f.get(existing);
                if (value2 != null) {
                    log.debug("clearing " + f.getName() + " = " + value);
                    f.set(existing, null);
                }
            }
        }
    }

    private Field[] getFields(final Object incoming) {
        final ArrayList<Field> fields = new ArrayList<Field>();
        Class<?> clazz = incoming.getClass();

        do {
            final Field[] f = clazz.getDeclaredFields();
            fields.addAll(Arrays.asList(f));
            clazz = clazz.getSuperclass();
        } while (clazz != null);

        return fields.toArray(new Field[0]);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T fromJson(final Class<?> kind, final String json) {
        final T obj = (T) new Gson().fromJson(json, kind);
        return obj;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EntityBase autocreate(final EntityBase target) throws Exception {

        T existing = null;
        if (target.getId() != null) {
            existing = get(new Key(target.getClass(), target.getId()));
        }

        final Field[] fields = target.getClass().getDeclaredFields();
        for (final Field f : fields) {
            f.setAccessible(true);
            final Autocreate autocreate = f.getAnnotation(Autocreate.class);
            if (autocreate != null) {
                try {
                    final Object value = f.get(target);
                    if (value != null) {
                        if (EntityBase.class.isAssignableFrom(value.getClass())) {
                            final EntityBase entity = (EntityBase) value;
                            f.set(target, createOrUpdate(entity, autocreate));
                        } else if (Collection.class.isAssignableFrom(value.getClass())) {
                            final List<Object> list = new ArrayList<Object>((Collection<Object>) value);

                            List<Object> removeThese = null;
                            if (existing != null) {
                                removeThese = (List<Object>) f.get(existing);
                                if (removeThese != null) {
                                    removeThese = new ArrayList<Object>(removeThese);
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

        return target;
    }

    public T autodelete(final Key key) throws Exception {

        final T target = get(key);
        if (target == null) {
            return null;
        }

        final Field[] fields = target.getClass().getDeclaredFields();
        for (final Field f : fields) {
            f.setAccessible(true);
            final Autocreate autocreate = f.getAnnotation(Autocreate.class);
            if (autocreate != null) {
                try {
                    final Object object = f.get(target);
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
    private EntityBase createOrUpdate(EntityBase target, final Autocreate fieldAutocreate) throws Exception {
        if (target == null) {
            return target;
        }

        final Autocreate classAutocreate = getAutocreate(target);

        if (target.getId() == null) {
            if ((classAutocreate == null || (classAutocreate.create()  && !classAutocreate.readonly())) && fieldAutocreate.create()) {
                final DataProvider<EntityBase> dataProvider = (DataProvider<EntityBase>) DataProviderFactory.instance().getDataProvider(target.getClass());
                target = dataProvider.post(target);
            }
        } else {
            if ((classAutocreate == null || (classAutocreate.update()  && !classAutocreate.readonly())) && fieldAutocreate.update()) {
                final DataProvider<EntityBase> dataProvider = (DataProvider<EntityBase>) DataProviderFactory.instance().getDataProvider(target.getClass());
                target = dataProvider.put(target);
            }
        }

        return target;
    }

    @SuppressWarnings("unchecked")
    private void delete(final Object target, final Autocreate fieldAutocreate) throws Exception {
        if (target == null) {
            return;
        }

        final Autocreate classAutocreate = getAutocreate(target);

        if ((classAutocreate == null || (classAutocreate.delete() && !classAutocreate.readonly())) && fieldAutocreate.delete()) {
            log.debug("deleting: " + target.getClass().getSimpleName());
            if (Collection.class.isAssignableFrom(target.getClass())) {
                final List<Object> list = new ArrayList<Object>((Collection<Object>) target);
                for (final Object o : list) {
                    delete(o, fieldAutocreate);
                }
            } else {
                final Object id = getId(target);
                if (id != null) {
                    DataProviderFactory.instance().getDataProvider(target.getClass()).delete(new Key(target.getClass(), id));
                }
            }
        }
    }

    private Autocreate getAutocreate(final Object object) {
        final Class<? extends Object> class1 = object.getClass();
        final Autocreate classAutocreate = class1.getAnnotation(Autocreate.class);
        return classAutocreate;
    }
}
