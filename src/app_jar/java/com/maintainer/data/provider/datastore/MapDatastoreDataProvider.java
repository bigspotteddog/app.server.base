package com.maintainer.data.provider.datastore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Text;
import com.maintainer.data.model.EntityBase;
import com.maintainer.data.model.MapEntityImpl;

public class MapDatastoreDataProvider<T extends MapEntityImpl> extends DatastoreDataProvider<T> {
    private static final Logger log = Logger.getLogger(MapDatastoreDataProvider.class.getName());

    @Override
    @SuppressWarnings("unchecked")
    public T fromEntity(final Class<?> kind, final Entity entity) {
        final T obj = super.fromEntity(kind, entity);

        try {
            final Map<String, Object> properties = entity.getProperties();
            for (final Entry<String, Object> e : properties.entrySet()) {
                final String field = e.getKey();
                if ("id".equals(field) || "created".equals(field) || "modified".equals(field)) {
                    continue;
                }

                Object value = e.getValue();

                if (value != null) {
                    if (Key.class.isAssignableFrom(value.getClass())) {
                        final Key k = (Key) value;
                        final String className = k.getKind();
                        final Class<?> class1 = Class.forName(className);
                        value = get(com.maintainer.data.provider.Key.create(class1, k.getId()));
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
                                o = get(com.maintainer.data.provider.Key.create(class1, k.getId()));
                                iterator.set(o);
                            }
                        }
                        value = list;
                    }
                    obj.put(field, value);
                }
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        return obj;
    }

    @Override
    protected Entity toEntity(Entity entity, final T target) throws Exception {

        entity = super.toEntity(entity, target);

        for (final Entry<String, Object> entry : target.entrySet()) {
            final String field = entry.getKey();
            if ("id".equals(field) || "created".equals(field) || "modified".equals(field)) {
                continue;
            }

            Object value = entry.getValue();

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

            if (value != null && String.class.isAssignableFrom(value.getClass())) {
                final String string = (String) value;
                if (string.length() > 500) {
                    value = new Text(string);
                }
            }
            entity.setProperty(field, value);
        }

        return entity;
    }
}
