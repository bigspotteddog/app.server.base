package com.maintainer.data.controller;


import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.maintainer.data.model.EntityImpl;
import com.maintainer.data.model.MapEntityImpl;
import com.maintainer.data.model.MyClass;
import com.maintainer.data.model.Resource;
import com.maintainer.data.provider.DataProvider;
import com.maintainer.data.provider.DataProviderFactory;
import com.maintainer.data.provider.DefaultDataProviderInitializationException;
import com.maintainer.util.Utils;

public class GenericController<T extends EntityImpl> extends ResourcesController<T> {

    private static final Map<String, Class<?>> map = new LinkedHashMap<String, Class<?>>();

    static {
        final File file = Utils.getDatabaseConfigurationFile();
        final Properties properties = Utils.getProperties(file);
        final String packageName = (String) properties.get("hibernate.model.package");
        final Class<?>[] classes = Utils.getClassesInPackage(packageName, null);

        for (final Class<?> clazz : classes) {
            final Resource resource = clazz.getAnnotation(Resource.class);
            if (resource != null) {
                final String name = resource.name();
                register(name, clazz);
            }
        }

        register("classes", MyClass.class);
    }

    public static void register(final String resource, final Class<?> clazz) {
        map.put(resource, clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected DataProvider<T> getDataProvider() throws DefaultDataProviderInitializationException {
        final Class<?> clazz = getResourceClass();
        return (DataProvider<T>) DataProviderFactory.instance().getDataProvider(clazz);
    }

    private Class<?> getResourceClass() {
        String resource = getResource();
        if (resource == null) {
            // hope the getControllerClass method is overridden
            return getControllerClass(null);
        }

        resource = resource.toLowerCase();

        final Class<?> clazz = getControllerClass(resource);
        if (clazz == null) {
            throw new RuntimeException("Invalid resource: " + resource);
        }

        return clazz;
    }

    public static Map<String, Class<?>>getResourceMap() {
        return map;
    }

    @Override
    public Class<?> getControllerClass(final String resource) {
        Class<?> class1 = map.get(resource);

        if (class1 == null) {
            class1 = MapEntityImpl.class;
//            DataProvider<MyClass> dataProvider = (DataProvider<MyClass>) DataProviderFactory.instance().getDataProvider(MyClass.class);
//            try {
//                Query q = new Query(MyClass.class);
//                q.filter("route", resource);
//                List<MyClass> list = dataProvider.find(q);
//                if (!list.isEmpty()) {
//                    class1 = MapEntityImpl.class;
//                }
//            } catch (Exception e1) {
//                e1.printStackTrace();
//            }
        }

        return class1;
    }

    public static List<String> getResources() {
        final List<String> resources = new ArrayList<String>();
        for (final Entry<String, Class<?>> e : map.entrySet()) {
            resources.add(e.getKey());
        }
        return resources;
    }

    public static Class<?> getRegistered(String kind) {
        return map.get(kind);
    }

    public static void unregister(String key) {
        map.remove(key);
    }
}
