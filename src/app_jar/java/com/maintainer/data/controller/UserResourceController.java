package com.maintainer.data.controller;

import java.lang.reflect.Constructor;
import java.util.Properties;

import com.maintainer.data.model.User;
import com.maintainer.data.provider.DataProvider;
import com.maintainer.data.provider.Key;
import com.maintainer.util.Utils;

public class UserResourceController extends GenericController {
    @Override
    protected void prePost(Object obj) {
        User user = (User) obj;
        encryptPassword(user);
    }

    public static void encryptPassword(User user) {
        user.setPassword(Utils.encrypt(user.getPassword()));
    }

    @Override
    protected void prePut(Object obj) {
        DataProvider<?> provider = getDataProvider();
        User user = (User) obj;

        User existing = (User) provider.get(new Key(user.getClass(), user.getId()));
        if (existing.getPassword() == null || (user.getPassword() != null && !existing.getPassword().equals(user.getPassword()))) {
            encryptPassword(user);
        }
    }

    public static User createUser(String username, String password) {
        User user = null;

        Class<?> userClass = getUserClass();
        if (userClass != null) {
            try {
                Constructor<?> c = userClass.getDeclaredConstructor();
                c.setAccessible(true);
                user = (User) c.newInstance();
                user.setUsername(username);
                user.setPassword(password);
                return user;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new User(username, password);
    }

    public static Class<?> getUserClass() {
        Properties properties = Utils.getApplicationServerProperties();
        String className = (String) properties.get("appserver.user.class");
        if (className != null) {
            Class<?> userClass;
            try {
                userClass = Class.forName(className);
                return userClass;
            } catch (Exception e) {
                // let it be null
            }
        }
        return null;
    }
}
