package com.maintainer.data.provider.datastore;

import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.MemcacheService;

public class MyMemcacheServiceFactory {
    private static boolean isActive = true;

    private MyMemcacheServiceFactory() {}

    public static MemcacheService getMemcacheService() {
        return MyMemcacheService.instance();
    }

    public static AsyncMemcacheService getAsyncMemcacheService() {
        return MyAsyncMemcacheService.instance();
    }

    public static boolean isActive() {
        return isActive;
    }

    public static void setActive(final boolean b) {
        isActive = b;
    }
}
