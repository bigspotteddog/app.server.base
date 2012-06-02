package com.maintainer.data.provider;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.maintainer.util.Utils;
import com.mongodb.Mongo;

public class MongoConnection {

    private static final String DB_NAME = "security";
    private Datastore datastore;
    private Morphia morphia;

    private void mapClasses(Morphia morphia) {
        // Register classes here, like so:
        //morphia.map(<Class>.class);
    }

    public static MongoConnection getInstance() {
        return MongoConnectionHolder.INSTANCE;
    }

    public String getDatabaseName() {
        return DB_NAME;
    }

    public Datastore getDatastore() {
        return datastore;
    }

    public Morphia getMorphia() {
        return morphia;
    }

    private static final class MongoConnectionHolder {
        private static final MongoConnection INSTANCE = new MongoConnection();
    }

    private MongoConnection() {
        try {
            Properties properties = Utils.getDatabaseConfigurationProperties();

            //ex: morphia://localhost:27017/security
            String connection = (String) properties.get("hibernate.connection.url");
            String[] split = StringUtils.splitByWholeSeparator(connection, "://");
            if (!"morphia".equals(split[0])) {
                throw new InvalidDatabaseConfiguration("The database connection string was not correct.  Unknown: " + split[0]);
            }

            split = StringUtils.split(split[1], "/");
            String server = split[0];
            int port = 27017;
            String db = split[1];
            if (server.contains(":")) {
                split = StringUtils.split(server, ":");
                server = split[0];
                port = Integer.parseInt(split[1]);
            }

            Mongo m = new Mongo(server, port);

            morphia = new Morphia();
            mapClasses(morphia);
            datastore = morphia.createDatastore(m, db);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
