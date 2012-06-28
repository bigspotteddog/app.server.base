package com.maintainer.data.controller;

import java.lang.reflect.Constructor;
import java.util.Properties;

import com.maintainer.data.model.User;
import com.maintainer.data.provider.DataProvider;
import com.maintainer.data.provider.Key;
import com.maintainer.util.Utils;

public class UserResourceController extends GenericController<User> {
    @Override
    protected void prePost(final User obj) {
        final User user = obj;
        encryptPassword(user);
    }

    public static void encryptPassword(final User user) {
        user.setPassword(Utils.encrypt(user.getPassword()));
    }

    @Override
    protected void prePut(final User obj) {
        final DataProvider<?> provider = getDataProvider();
        final User user = obj;

        final User existing = (User) provider.get(new Key(user.getClass(), user.getId()));
        if (existing.getPassword() == null || (user.getPassword() != null && !existing.getPassword().equals(user.getPassword()))) {
            encryptPassword(user);
        }
    }

    public static User createUser(final String username, final String password) {
        User user = null;

        final Class<?> userClass = getUserClass();
        if (userClass != null) {
            try {
                final Constructor<?> c = userClass.getDeclaredConstructor();
                c.setAccessible(true);
                user = (User) c.newInstance();
                user.setUsername(username);
                user.setPassword(password);
                return user;
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        return new User(username, password);
    }

    public static Class<?> getUserClass() {
        final Properties properties = Utils.getApplicationServerProperties();
        final String className = (String) properties.get("appserver.user.class");
        if (className != null) {
            Class<?> userClass;
            try {
                userClass = Class.forName(className);
                return userClass;
            } catch (final Exception e) {
                // let it be null
            }
        }
        return null;
    }
}
