package com.maintainer.data.provider.datastore;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.appengine.api.memcache.ErrorHandler;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.Stats;

@SuppressWarnings("deprecation")
public class MyMemcacheService implements com.google.appengine.api.memcache.MemcacheService {

    private static final class MyMemcacheServiceHolder {
        private static final MyMemcacheService INSTANCE = new MyMemcacheService();
    }

    private MyMemcacheService() {}

    public static MyMemcacheService instance() {
        return MyMemcacheServiceHolder.INSTANCE;
    }

    private final boolean isActive() {
        return MyMemcacheServiceFactory.isActive();
    }

    @Override
    public ErrorHandler getErrorHandler() {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getMemcacheService().getErrorHandler();
    }

    @Override
    public String getNamespace() {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getMemcacheService().getNamespace();
    }

    @Override
    public void setErrorHandler(final ErrorHandler arg0) {
        if (!isActive()) return;
        MemcacheServiceFactory.getMemcacheService().setErrorHandler(arg0);
    }

    @Override
    public void clearAll() {
        if (!isActive()) return;
        MemcacheServiceFactory.getMemcacheService().clearAll();
    }

    @Override
    public boolean contains(final Object arg0) {
        if (!isActive()) return false;
        return MemcacheServiceFactory.getMemcacheService().contains(arg0);
    }

    @Override
    public boolean delete(final Object arg0) {
        if (!isActive()) return false;
        return MemcacheServiceFactory.getMemcacheService().delete(arg0);
    }

    @Override
    public boolean delete(final Object arg0, final long arg1) {
        if (!isActive()) return false;
        return MemcacheServiceFactory.getMemcacheService().delete(arg0, arg1);
    }

    @Override
    public <T> Set<T> deleteAll(final Collection<T> arg0) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getMemcacheService().deleteAll(arg0);
    }

    @Override
    public <T> Set<T> deleteAll(final Collection<T> arg0, final long arg1) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getMemcacheService().deleteAll(arg0, arg1);
    }

    @Override
    public Object get(final Object arg0) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getMemcacheService().get(arg0);
    }

    @Override
    public <T> Map<T, Object> getAll(final Collection<T> arg0) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getMemcacheService().getAll(arg0);
    }

    @Override
    public IdentifiableValue getIdentifiable(final Object arg0) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getMemcacheService().getIdentifiable(arg0);
    }

    @Override
    public <T> Map<T, IdentifiableValue> getIdentifiables(final Collection<T> arg0) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getMemcacheService().getIdentifiables(arg0);
    }

    @Override
    public Stats getStatistics() {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getMemcacheService().getStatistics();
    }

    @Override
    public Long increment(final Object arg0, final long arg1) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getMemcacheService().increment(arg0, arg1);
    }

    @Override
    public Long increment(final Object arg0, final long arg1, final Long arg2) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getMemcacheService().increment(arg0, arg1, arg2);
    }

    @Override
    public <T> Map<T, Long> incrementAll(final Map<T, Long> arg0) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getMemcacheService().incrementAll(arg0);
    }

    @Override
    public <T> Map<T, Long> incrementAll(final Collection<T> arg0, final long arg1) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getMemcacheService().incrementAll(arg0, arg1);
    }

    @Override
    public <T> Map<T, Long> incrementAll(final Map<T, Long> arg0, final Long arg1) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getMemcacheService().incrementAll(arg0, arg1);
    }

    @Override
    public <T> Map<T, Long> incrementAll(final Collection<T> arg0, final long arg1, final Long arg2) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getMemcacheService().incrementAll(arg0, arg1, arg2);
    }

    @Override
    public void put(final Object arg0, final Object arg1) {
        if (!isActive()) return;
        MemcacheServiceFactory.getMemcacheService().put(arg0, arg1);
    }

    @Override
    public void put(final Object arg0, final Object arg1, final Expiration arg2) {
        if (!isActive()) return;
        MemcacheServiceFactory.getMemcacheService().put(arg0, arg1, arg2);

    }

    @Override
    public boolean put(final Object arg0, final Object arg1, final Expiration arg2, final SetPolicy arg3) {
        if (!isActive()) return false;
        return MemcacheServiceFactory.getMemcacheService().put(arg0, arg1, arg2, arg3);
    }

    @Override
    public void putAll(final Map<?, ?> arg0) {
        if (!isActive()) return;
        MemcacheServiceFactory.getMemcacheService().putAll(arg0);
    }

    @Override
    public void putAll(final Map<?, ?> arg0, final Expiration arg1) {
        if (!isActive()) return;
        MemcacheServiceFactory.getMemcacheService().putAll(arg0, arg1);
    }

    @Override
    public <T> Set<T> putAll(final Map<T, ?> arg0, final Expiration arg1, final SetPolicy arg2) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getMemcacheService().putAll(arg0, arg1, arg2);
    }

    @Override
    public <T> Set<T> putIfUntouched(final Map<T, CasValues> arg0) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getMemcacheService().putIfUntouched(arg0);
    }

    @Override
    public <T> Set<T> putIfUntouched(final Map<T, CasValues> arg0, final Expiration arg1) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getMemcacheService().putIfUntouched(arg0, arg1);
    }

    @Override
    public boolean putIfUntouched(final Object arg0, final IdentifiableValue arg1, final Object arg2) {
        if (!isActive()) return false;
        return MemcacheServiceFactory.getMemcacheService().putIfUntouched(arg0, arg1, arg2);
    }

    @Override
    public boolean putIfUntouched(final Object arg0, final IdentifiableValue arg1, final Object arg2, final Expiration arg3) {
        if (!isActive()) return false;
        return MemcacheServiceFactory.getMemcacheService().putIfUntouched(arg0, arg1, arg2, arg3);
    }

    @Override
    @Deprecated
    public void setNamespace(final String arg0) {
        if (!isActive()) return;
        MemcacheServiceFactory.getMemcacheService().setNamespace(arg0);
    }
}
