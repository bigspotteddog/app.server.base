package com.github.bigspotteddog.data.provider;

import java.util.List;

public interface UserDataProviderBase<T> extends DataProvider<T> {
    List<T> findByUsername(String identifier);

    T createAdministrativeUser(String username, String password);
}
