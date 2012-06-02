package com.maintainer.data.provider;


import java.util.Properties;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.maintainer.data.model.EntityBase;
import com.maintainer.data.provider.datastore.DatastoreDataProvider;
import com.maintainer.data.provider.hibernate.HibernateDataProvider;
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

    public void register(Class<?> clazz, DataProvider<?> dataProvider) {
        map.put(clazz, dataProvider);
    }

    public DataProvider<?> getDataProvider(Class<?> clazz) throws DefaultDataProviderInitializationException {
        DataProvider<?> dataProvider = map.get(clazz);

        if (dataProvider == null) {
            dataProvider = getDefaultDataProvider();
        }

        return dataProvider;
    }

    public void setDefaultDataProvider(DataProvider<?> dataProvider) {
        this.defaultDataProvider = dataProvider;
    }

    public DataProvider<?> getDefaultDataProvider() throws DefaultDataProviderInitializationException {
        if (defaultDataProvider == null) {
            Properties properties = Utils.getDatabaseConfigurationProperties();
            String driver = (String) properties.get("hibernate.connection.driver_class");
            if ("com.google.code.morphia.Morhia".equals(driver)) {
                defaultDataProvider = new MongoDataProvider<EntityBase>();
            } else if ("com.maintainer.data.provider.datastore.DatastoreDataProvider".equals(driver)) {
                defaultDataProvider = new DatastoreDataProvider<EntityBase>();
            } else {
                defaultDataProvider = new HibernateDataProvider<EntityBase>();
            }
        }

        return defaultDataProvider;
    }
}
