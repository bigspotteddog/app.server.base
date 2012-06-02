package com.maintainer.data.controller;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.maintainer.data.model.Resource;
import com.maintainer.data.provider.DataProvider;
import com.maintainer.data.provider.DataProviderFactory;
import com.maintainer.data.provider.DefaultDataProviderInitializationException;
import com.maintainer.util.Utils;

public class GenericController extends ResourcesController<Object> {

    private static final BiMap<String, Class<?>> map = HashBiMap.create();

    static {
        File file = Utils.getDatabaseConfigurationFile();
        Properties properties = Utils.getProperties(file);
        String packageName = (String) properties.get("hibernate.model.package");
        Class<?>[] classes = Utils.getClassesInPackage(packageName, null);

        for (Class<?> clazz : classes) {
            Resource resource = clazz.getAnnotation(Resource.class);
            if (resource != null) {
                String name = resource.name();
                register(name, clazz);
            }
        }
    }

    public static void register(String resource, Class<?> clazz) {
        map.put(resource, clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected DataProvider<Object> getDataProvider() throws DefaultDataProviderInitializationException {
        Class<?> clazz = getResourceClass();
        return (DataProvider<Object>) DataProviderFactory.instance().getDataProvider(clazz);
    }

    private Class<?> getResourceClass() {
        String resource = getResource();
        if (resource == null) {
            // hope the getControllerClass method is overridden
            return getControllerClass(null);
        }

        resource = resource.toLowerCase();

        Class<?> clazz = getControllerClass(resource);
        if (clazz == null) {
            throw new RuntimeException("Invalid resource: " + resource);
        }

        return clazz;
    }

    public static Map<String, Class<?>>getResourceMap() {
        return map;
    }

    @Override
    public Class<?> getControllerClass(String resource) {
        return map.get(resource);
    }

    @Override
    public String getResourceMapping(Class<?> clazz) {
        return map.inverse().get(clazz);
    }

    public static List<String> getResources() {
        List<String> resources = new ArrayList<String>();
        for (Entry<String, Class<?>> e : map.entrySet()) {
            resources.add(e.getKey());
        }
        return resources;
    }
}
