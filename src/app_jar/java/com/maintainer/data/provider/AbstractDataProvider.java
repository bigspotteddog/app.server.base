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
    public abstract T get(Key key);

    @Override
    public abstract List<T> getAll(Class<?> kind) throws Exception;

    @Override
    public abstract T put(T item) throws Exception;

    @Override
    public abstract Key delete(Key key) throws Exception;

    @Override
    public abstract Collection<T> find(Query query);

    protected void preMerge(T incoming, T existing) {}

    @Override
    public T merge(T incoming) throws Exception {
        long id = ((EntityImpl) incoming).getId();

        if (id == 0) {
            throw new Exception("no id specified");
        }

        Key key = new Key(incoming.getClass(), id);
        T existing = get(key);

        if (existing == null) {
            throw new Exception("not found");
        }

        preMerge(incoming, existing);

        merge(incoming, existing);

        put(existing);

        return existing;
    }

    protected void merge(T incoming, T existing) throws Exception {
        mergeAny(incoming, existing);
    }

    protected boolean isEqual(Object incoming, Object existing) throws Exception {
        boolean equals = false;

        if (incoming != null && existing != null) {
            String incomingJson = gson.toJson(incoming);
            String existingJson = gson.toJson(existing);

            equals = incomingJson.equals(existingJson);
        } else if (incoming == null && existing == null) {
            equals = true;
        }

        return equals;
    }

    protected void mergeAny(Object incoming, Object existing) throws Exception {
        log.debug("mergeAny");

        Field[] fields = getFields(incoming);
        for (Field f : fields) {
            f.setAccessible(true);

            Object value = f.get(incoming);
            if (value != null) {
                log.debug(f.getName() + " = " + value);
                f.set(existing, value);
            } else {
                Object value2 = f.get(existing);
                if (value2 != null) {
                    log.debug("clearing " + f.getName() + " = " + value);
                    f.set(existing, null);
                }
            }
        }
    }

    private Field[] getFields(Object incoming) {
        ArrayList<Field> fields = new ArrayList<Field>();
        Class<?> clazz = incoming.getClass();

        do {
            Field[] f = clazz.getDeclaredFields();
            fields.addAll(Arrays.asList(f));
            clazz = clazz.getSuperclass();
        } while (clazz != null);

        return fields.toArray(new Field[0]);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T fromJson(Class<?> kind, String json) {
        T obj = (T) new Gson().fromJson(json, kind);
        return obj;
    }

    @Override
    @SuppressWarnings("unchecked")
    public EntityBase autocreate(EntityBase target) throws Exception {

        T existing = null;
        if (target.getId() != null) {
            existing = get(new Key(target.getClass(), target.getId()));
        }

        Field[] fields = target.getClass().getDeclaredFields();
        for (Field f : fields) {
            f.setAccessible(true);
            Autocreate autocreate = f.getAnnotation(Autocreate.class);
            if (autocreate != null) {
                try {
                    Object value = f.get(target);
                    if (value != null) {
                        if (EntityBase.class.isAssignableFrom(value.getClass())) {
                            EntityBase entity = (EntityBase) value;
                            f.set(target, createOrUpdate(entity, autocreate));
                        } else if (Collection.class.isAssignableFrom(value.getClass())) {
                            List<Object> list = new ArrayList<Object>((Collection<Object>) value);

                            List<Object> removeThese = null;
                            if (existing != null) {
                                removeThese = (List<Object>) f.get(existing);
                                if (removeThese != null) {
                                    removeThese = new ArrayList<Object>(removeThese);
                                }
                            }

                            ListIterator<Object> iterator = list.listIterator();
                            while(iterator.hasNext()) {
                                Object o = iterator.next();
                                if (o == null) continue;
                                if (EntityBase.class.isAssignableFrom(o.getClass())) {
                                    EntityBase entity = (EntityBase) o;
                                    iterator.set(createOrUpdate(entity, autocreate));
                                }
                            }

                            if (removeThese != null && !removeThese.isEmpty()) {
                                removeThese.removeAll(list);
                                for (Object object : removeThese) {
                                    delete(object, autocreate);
                                }
                            }
                        }
                    } else {
                        if (existing != null) {
                            Object object = f.get(existing);
                            delete(object, autocreate);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return target;
    }

    public T autodelete(Key key) {

        T target = get(key);
        if (target == null) return null;

        Field[] fields = target.getClass().getDeclaredFields();
        for (Field f : fields) {
            f.setAccessible(true);
            Autocreate autocreate = f.getAnnotation(Autocreate.class);
            if (autocreate != null) {
                try {
                    Object object = f.get(target);
                    delete(object, autocreate);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return target;
    }

    @Override
    public Long getId(Object object) {
        if (object != null && EntityBase.class.isAssignableFrom(object.getClass())) {
            EntityBase entity = (EntityBase) object;
            return entity.getId();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private EntityBase createOrUpdate(EntityBase target, Autocreate fieldAutocreate) throws Exception {
        if (target == null) return target;

        Autocreate classAutocreate = getAutocreate(target);

        if (target.getId() == null) {
            if ((classAutocreate == null || (classAutocreate.create()  && !classAutocreate.readonly())) && fieldAutocreate.create()) {
                DataProvider<EntityBase> dataProvider = (DataProvider<EntityBase>) DataProviderFactory.instance().getDataProvider(target.getClass());
                target = dataProvider.post(target);
            }
        } else {
            if ((classAutocreate == null || (classAutocreate.update()  && !classAutocreate.readonly())) && fieldAutocreate.update()) {
                DataProvider<EntityBase> dataProvider = (DataProvider<EntityBase>) DataProviderFactory.instance().getDataProvider(target.getClass());
                target = dataProvider.put(target);
            }
        }

        return target;
    }

    @SuppressWarnings("unchecked")
    private void delete(Object target, Autocreate fieldAutocreate) throws Exception {
        if (target == null) return;

        Autocreate classAutocreate = getAutocreate(target);

        if ((classAutocreate == null || (classAutocreate.delete() && !classAutocreate.readonly())) && fieldAutocreate.delete()) {
            log.debug("deleting: " + target.getClass().getSimpleName());
            if (Collection.class.isAssignableFrom(target.getClass())) {
                List<Object> list = new ArrayList<Object>((Collection<Object>) target);
                for (Object o : list) {
                    delete(o, fieldAutocreate);
                }
            } else {
                Long id = getId(target);
                if (id != null) {
                    DataProviderFactory.instance().getDataProvider(target.getClass()).delete(new Key(target.getClass(), id));
                }
            }
        }
    }

    private Autocreate getAutocreate(Object object) {
        Class<? extends Object> class1 = object.getClass();
        Autocreate classAutocreate = class1.getAnnotation(Autocreate.class);
        return classAutocreate;
    }
}
