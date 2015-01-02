package com.maintainer.data.provider;


import java.util.Properties;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.maintainer.util.Utils;


public class DataProviderFactory {
    private final BiMap<Class<?>, DataProvider<?>> map = HashBiMap.create();
    private DataProvider<?> defaultDataProvider;

    private static final class DataProviderFactoryHolder {
        private static final DataProviderFactory INSTANCE = new DataProviderFactory();
    }

    private DataProviderFactory() {}

    public static DataProviderFactory instance() {
        return DataProviderFactoryHolder.INSTANCE;
    }

    public void register(final Class<?> clazz, final DataProvider<?> dataProvider) {
        map.put(clazz, dataProvider);
    }

    public DataProvider<?> getDataProvider(final Class<?> clazz) throws DefaultDataProviderInitializationException {
        DataProvider<?> dataProvider = map.get(clazz);

        if (dataProvider == null) {
            dataProvider = getDefaultDataProvider();
        }

        return dataProvider;
    }

    public void setDefaultDataProvider(final DataProvider<?> dataProvider) {
        this.defaultDataProvider = dataProvider;
    }

    public DataProvider<?> getDefaultDataProvider() throws DefaultDataProviderInitializationException {
        if (defaultDataProvider == null) {
            final Properties properties = Utils.getDatabaseConfigurationProperties();
            final String driver = (String) properties.get("hibernate.connection.driver_class");
            try {
                Class<?> driverClass = Class.forName(driver);
                try {
                        defaultDataProvider = (DataProvider<?>) driverClass.newInstance();
                    } catch (InstantiationException e) {
                        throw new DefaultDataProviderInitializationException(e);
                    } catch (IllegalAccessException e) {
                        throw new DefaultDataProviderInitializationException(e);
                    }
            } catch (ClassNotFoundException e) {
                throw new DefaultDataProviderInitializationException(e);
            }
        }
        return defaultDataProvider;
    }
}
