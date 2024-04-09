package com.github.bigspotteddog.util;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import com.github.bigspotteddog.data.controller.UserResourceController;
import com.github.bigspotteddog.data.model.User;
import com.github.bigspotteddog.data.provider.DataProvider;
import com.github.bigspotteddog.data.provider.DataProviderFactory;
import com.github.bigspotteddog.data.provider.Query;

public class AddAdministrativeUser {
    private static final Logger log = Logger.getLogger(AddAdministrativeUser.class.getName());

    private static final String USAGE = "\nUsage:  java AddAdministrativeUser [-rl] <username> <password>\n\n" +
            "-l\tlist users by [username].\n" +
            "-r\treset an existing <username>'s <password>.\n";

    private boolean reset;
    private boolean list;
    private String username;
    private String password;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            log.debug(USAGE);
            System.exit(0);
        }

        log.debug(args);

        new AddAdministrativeUser(args).process();
    }

    public AddAdministrativeUser(String[] args) throws Exception {
        int arg = 0;

        if (args[arg].equals("-r") || args[arg].equals("-l")) {
            if (args[arg].equals("-r")) {
                reset = true;
                arg++;
            }

            if (args[arg].equals("-l")) {
                list = true;
                arg++;
            }
        }

        if (arg < args.length) {
            username = args[arg++];
        }

        if (arg < args.length) {
            password = args[arg++];
        }

    }

    public AddAdministrativeUser(String username, String password) {
        this(false, false, username, password);
    }

    public AddAdministrativeUser(boolean reset, boolean list, String username, String password) {
        this.reset = reset;
        this.list = list;
        this.username = username;
        this.password = password;
    }

    public void process() throws Exception {
        if (list) {
            Collection<User> users = getUsers();
            log.debug(users.toString());
        } else {
            Collection<User> users = getUsersByUsername(username);

            if (users.size() > 0) {
                if (reset) {
                    User existing = users.iterator().next();
                    existing.setPassword(password);
                    User admin = updateUser(existing);
                    log.debug("An administrative user was updated with username: " + admin.getUsername()
                            + ", password: " + password + ".");
                } else {
                    log.debug("An administrative user with the username '" + username + "' already exists.");
                }
            } else {
                User admin = createUser(username, password);
                log.debug("An administrative user was added with username: " + admin.getUsername() + ", password: "
                        + password + ".");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private User updateUser(User existing) throws Exception {
        DataProvider<User> users = (DataProvider<User>) DataProviderFactory.instance()
                .getDataProvider(UserResourceController.getUserClass());

        users.beginTransaction();
        User admin = users.put(existing);
        users.commitTransaction();

        return admin;
    }

    @SuppressWarnings("unchecked")
    private User createUser(String username, String password) throws Exception {
        User user = UserResourceController.createUser(username, password);
        UserResourceController.encryptPassword(user);

        DataProvider<User> users = (DataProvider<User>) DataProviderFactory.instance().getDataProvider(user.getClass());

        users.beginTransaction();
        User admin = users.post(user);
        users.commitTransaction();

        return admin;
    }

    @SuppressWarnings("unchecked")
    private Collection<User> getUsersByUsername(String username) throws Exception {
        DataProvider<User> users = (DataProvider<User>) DataProviderFactory.instance()
                .getDataProvider(UserResourceController.getUserClass());
        Query q = new Query(User.class);
        q.filter("username", username);
        return users.find(q);
    }

    @SuppressWarnings("unchecked")
    private List<User> getUsers() throws Exception {
        DataProvider<User> users = (DataProvider<User>) DataProviderFactory.instance()
                .getDataProvider(UserResourceController.getUserClass());
        return users.getAll(User.class);
    }
}
