package com.maintainer.data.provider.datastore;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.maintainer.data.model.EntityBase;
import com.maintainer.data.provider.AbstractDataProvider;
import com.maintainer.util.Utils;

public abstract class AbstractDatastoreDataProvider<T> extends AbstractDataProvider<T> {
    private static final Map<String, FilterOperator> ops = new HashMap<String, FilterOperator>();
    private static final Map<String, FilterOperator> ineq = new HashMap<String, FilterOperator>();

    public AbstractDatastoreDataProvider() {
        ops.put("ge", FilterOperator.GREATER_THAN_OR_EQUAL);
        ops.put("gt", FilterOperator.GREATER_THAN);
        ops.put("le", FilterOperator.LESS_THAN_OR_EQUAL);
        ops.put("lt", FilterOperator.LESS_THAN);
        ops.put("eq", FilterOperator.EQUAL);
        ops.put(">=", FilterOperator.GREATER_THAN_OR_EQUAL);
        ops.put(">", FilterOperator.GREATER_THAN);
        ops.put("<=", FilterOperator.LESS_THAN_OR_EQUAL);
        ops.put("<", FilterOperator.LESS_THAN);
        ops.put("=", FilterOperator.EQUAL);
        ops.put("in", FilterOperator.IN);

        ineq.put("ge", FilterOperator.GREATER_THAN_OR_EQUAL);
        ineq.put("gt", FilterOperator.GREATER_THAN);
        ineq.put("le", FilterOperator.LESS_THAN_OR_EQUAL);
        ineq.put("lt", FilterOperator.LESS_THAN);
        ineq.put(">=", FilterOperator.GREATER_THAN_OR_EQUAL);
        ineq.put(">", FilterOperator.GREATER_THAN);
        ineq.put("<=", FilterOperator.LESS_THAN_OR_EQUAL);
        ineq.put("<", FilterOperator.LESS_THAN);
        ineq.put("in", FilterOperator.IN);
    }

    protected FilterOperator getOperator(final String op) {
        if (op == null) {
            return FilterOperator.EQUAL;
        }

        return ops.get(op);
    }

    protected boolean isInequalityOperator(final String op) {
        return ineq.containsKey(op);
    }

    protected String getOperatorFromCondition(final String condition) {
        final String[] split = condition.split("(\\s|:)");

        if (split.length > 1) {
            return split[1];
        }

        return null;
    }

    protected String getFieldFromCondition(String condition) {
        final String[] split = condition.split("(\\s|:)");
        condition = split[0];
        return condition;
    }

    protected com.maintainer.data.provider.Key createNobodyelsesKey(final Key k) throws ClassNotFoundException {
        final Class<?> class1 = getClazz(k);

        Object id = null;
        if (k.getId() == 0) {
            id = k.getName();
        } else {
            id = k.getId();
        }

        final com.maintainer.data.provider.Key key = com.maintainer.data.provider.Key.create(class1, id);

        if (k.getParent() != null) {
            key.setParent(createNobodyelsesKey(k.getParent()));
        }

        return key;
    }

    protected Class<?> getClazz(final Key k) throws ClassNotFoundException {
        final String className = k.getKind();
        final Class<?> class1 = Class.forName(className);
        return class1;
    }

    public static Key createDatastoreKey(final com.maintainer.data.provider.Key k) {
        if (k.getParent() == null) {
            return createDatastoreKey(k.getKindName(), k.getId());
        } else {
            return createDatastoreKey(k.getParent(), k.getKindName(), k.getId());
        }
    }

    public static Key createDatastoreKey(final String kind, final Object id) {
        Key key = null;

        if (Utils.isNumeric(id.toString())) {
            key = KeyFactory.createKey(kind, new BigDecimal(id.toString()).longValue());
        } else if (Long.class.isAssignableFrom(id.getClass())) {
            key = KeyFactory.createKey(kind, (Long) id);
        } else if (Double.class.isAssignableFrom(id.getClass())){
            key = KeyFactory.createKey(kind, ((Double) id).longValue());
        } else {
            key = KeyFactory.createKey(kind, (String) id);
        }

        return key;
    }

    public static Key createDatastoreKey(final EntityBase parent, final String kind, final Object id) {
        if (parent == null) {
            return createDatastoreKey(kind, id);
        }
        return createDatastoreKey(parent.getKey(), kind, id);
    }

    public static Key createDatastoreKey(final com.maintainer.data.provider.Key parent, final String kind, final Object id) {
        Key key = null;

        final Key parentKey = createDatastoreKey(parent);

        if (Utils.isNumeric(id.toString())) {
            key = KeyFactory.createKey(parentKey, kind, new BigDecimal(id.toString()).longValue());
        } else if (Long.class.isAssignableFrom(id.getClass())) {
            key = KeyFactory.createKey(parentKey, kind, (Long) id);
        } else if (Double.class.isAssignableFrom(id.getClass())){
            key = KeyFactory.createKey(parentKey, kind, ((Double) id).longValue());
        } else {
            key = KeyFactory.createKey(parentKey, kind, (String) id);
        }

        return key;
    }

    protected String getEncodedKeyString(final com.maintainer.data.provider.Key nobodyelsesKey) {
        return nobodyelsesKey.toString();
    }
}
