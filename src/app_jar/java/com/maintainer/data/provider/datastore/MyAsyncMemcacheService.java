package com.maintainer.data.provider.datastore;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.ErrorHandler;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService.CasValues;
import com.google.appengine.api.memcache.MemcacheService.IdentifiableValue;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.Stats;

@SuppressWarnings("deprecation")
public class MyAsyncMemcacheService implements AsyncMemcacheService {

    private static final class MyAsyncMemcacheServiceHolder {
        private static final MyAsyncMemcacheService INSTANCE = new MyAsyncMemcacheService();
    }

    private MyAsyncMemcacheService() {}

    public static MyAsyncMemcacheService instance() {
        return MyAsyncMemcacheServiceHolder.INSTANCE;
    }

    private final boolean isActive() {
        return MyMemcacheServiceFactory.isActive();
    }

    @Override
    public ErrorHandler getErrorHandler() {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().getErrorHandler();
    }

    @Override
    public String getNamespace() {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().getNamespace();
    }

    @Override
    public void setErrorHandler(final ErrorHandler arg0) {
        if (!isActive()) return;
        MemcacheServiceFactory.getAsyncMemcacheService().setErrorHandler(arg0);
    }

    @Override
    public Future<Void> clearAll() {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().clearAll();
    }

    @Override
    public Future<Boolean> contains(final Object arg0) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().contains(arg0);
    }

    @Override
    public Future<Boolean> delete(final Object arg0) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().delete(arg0);
    }

    @Override
    public Future<Boolean> delete(final Object arg0, final long arg1) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().delete(arg0, arg1);
    }

    @Override
    public <T> Future<Set<T>> deleteAll(final Collection<T> arg0) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().deleteAll(arg0);
    }

    @Override
    public <T> Future<Set<T>> deleteAll(final Collection<T> arg0, final long arg1) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().deleteAll(arg0, arg1);
    }

    @Override
    public Future<Object> get(final Object arg0) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().get(arg0);
    }

    @Override
    public <T> Future<Map<T, Object>> getAll(final Collection<T> arg0) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().getAll(arg0);
    }

    @Override
    public Future<IdentifiableValue> getIdentifiable(final Object arg0) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().getIdentifiable(arg0);
    }

    @Override
    public <T> Future<Map<T, IdentifiableValue>> getIdentifiables(final Collection<T> arg0) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().getIdentifiables(arg0);
    }

    @Override
    public Future<Stats> getStatistics() {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().getStatistics();
    }

    @Override
    public Future<Long> increment(final Object arg0, final long arg1) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().increment(arg0, arg1);
    }

    @Override
    public Future<Long> increment(final Object arg0, final long arg1, final Long arg2) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().increment(arg0, arg1, arg2);
    }

    @Override
    public <T> Future<Map<T, Long>> incrementAll(final Map<T, Long> arg0) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().incrementAll(arg0);
    }

    @Override
    public <T> Future<Map<T, Long>> incrementAll(final Collection<T> arg0, final long arg1) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().incrementAll(arg0, arg1);
    }

    @Override
    public <T> Future<Map<T, Long>> incrementAll(final Map<T, Long> arg0, final Long arg1) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().incrementAll(arg0, arg1);
    }

    @Override
    public <T> Future<Map<T, Long>> incrementAll(final Collection<T> arg0, final long arg1, final Long arg2) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().incrementAll(arg0, arg1, arg2);
    }

    @Override
    public Future<Void> put(final Object arg0, final Object arg1) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().put(arg0, arg1);
    }

    @Override
    public Future<Void> put(final Object arg0, final Object arg1, final Expiration arg2) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().put(arg0, arg1, arg2);
    }

    @Override
    public Future<Boolean> put(final Object arg0, final Object arg1, final Expiration arg2, final SetPolicy arg3) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().put(arg0, arg1, arg2, arg3);
    }

    @Override
    public Future<Void> putAll(final Map<?, ?> arg0) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().putAll(arg0);
    }

    @Override
    public Future<Void> putAll(final Map<?, ?> arg0, final Expiration arg1) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().putAll(arg0, arg1);
    }

    @Override
    public <T> Future<Set<T>> putAll(final Map<T, ?> arg0, final Expiration arg1, final SetPolicy arg2) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().putAll(arg0, arg1, arg2);
    }

    @Override
    public <T> Future<Set<T>> putIfUntouched(final Map<T, CasValues> arg0) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().putIfUntouched(arg0);
    }

    @Override
    public <T> Future<Set<T>> putIfUntouched(final Map<T, CasValues> arg0, final Expiration arg1) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().putIfUntouched(arg0, arg1);
    }

    @Override
    public Future<Boolean> putIfUntouched(final Object arg0, final IdentifiableValue arg1, final Object arg2) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().putIfUntouched(arg0, arg1, arg2);
    }

    @Override
    public Future<Boolean> putIfUntouched(final Object arg0, final IdentifiableValue arg1, final Object arg2, final Expiration arg3) {
        if (!isActive()) return null;
        return MemcacheServiceFactory.getAsyncMemcacheService().putIfUntouched(arg0, arg1, arg2, arg3);
    }
}
