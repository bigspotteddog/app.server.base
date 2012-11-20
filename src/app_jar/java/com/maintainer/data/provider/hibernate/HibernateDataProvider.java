package com.maintainer.data.provider.hibernate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;

import com.maintainer.data.model.EntityBase;
import com.maintainer.data.provider.AbstractDataProvider;
import com.maintainer.data.provider.Filter;
import com.maintainer.data.provider.Key;
import com.maintainer.data.provider.Query;
import com.maintainer.util.Utils;

public class HibernateDataProvider<T extends EntityBase> extends AbstractDataProvider<T> {
    private static final Logger log = Logger
            .getLogger(HibernateDataProvider.class.getName());
    private static final Map<String, String> ops = new HashMap<String, String>();

    public HibernateDataProvider() {
        ops.put("ge", ">=");
        ops.put("gt", ">");
        ops.put("le", "<=");
        ops.put("lt", "<");
        ops.put("eq", "=");
        ops.put(">=", ">=");
        ops.put(">", ">");
        ops.put("<=", "<=");
        ops.put("<", "<");
        ops.put("=", "=");
        ops.put("like", "like");
        ops.put("in", "in");
    }

    @Override
    protected void preMerge(final T incoming, final T existing) {
        getDatastore().evict(existing);
    }

    @Override
    public void beginTransaction() {
        HibernateUtil.beginTransaction();
    }

    @Override
    public void commitTransaction() {
        try {
            HibernateUtil.commitTransaction(true);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T post(final T item) throws Exception {
        autocreate(item);
        final Session datastore = getDatastore();
        datastore.saveOrUpdate(item);
        return item;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(final Key key) {
        final T obj = (T) getDatastore().get(key.getKind(), (Long) key.getId());
        return obj;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<T> getAll(final Class<?> kind) throws Exception {
        final Session datastore = getDatastore();
        final Criteria crit = datastore.createCriteria(kind);
        final List<T> list = crit.list();
        return list;
    }

    @Override
    public T put(final T item) throws Exception {
        autocreate(item);
        final Session datastore = getDatastore();
        datastore.merge(item);
        return item;
    }

    @Override
    public Key delete(final Key key) throws Exception {
        final Session datastore = getDatastore();

        final T cleared = autodelete(key);
        datastore.evict(cleared);

        final T item = get(key);
        datastore.merge(cleared);
        datastore.delete(item);
        return key;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<T> find(final Query query) throws Exception {
        final Map<String, String> alias = new HashMap<String, String>();

        final Session datastore = getDatastore();
        final Criteria q = datastore.createCriteria(query.getKind(),
                query.getKindName());
        for (final Filter e : query.getFilters()) {
            String key = e.getCondition();

            final String[] split = key.split("(\\s|:)");
            key = split[0];

            String op = null;
            if (split.length > 1) {
                op = split[1];
                op = ops.get(op);
            }

            if (op == null) {
                op = "=";
            }

            Object value = e.getValue();

            final Class<?> keyType = Utils.getKeyType(query.getKind(), key);
            value = Utils.convert(value, keyType);

            boolean ignoreCase = false;
            if (value != null && String.class.isAssignableFrom(value.getClass())) {
                ignoreCase = true;
                if ("like".equals(op)) {
                    value = ((String) value) + "%";
                }
            }

            String shortKey = key;
            if (key.contains(".")) {
                shortKey = createAlias(q, query.getKindName(), key, alias);
            }

            if ("in".equals(op)) {
                log.debug("in clause value: " + value);
                q.add(Restrictions.in(shortKey, (List<String>) value));
            } else if ("like".equals(op)) {
                q.add(Restrictions.ilike(shortKey, value.toString(), MatchMode.ANYWHERE));
            } else if ("ne".equals(op)) {
                q.add(Restrictions.ne(shortKey, value));
            } else {
                q.add(new MySimpleExpression(shortKey, value, op, ignoreCase));
            }
        }

        if (query.getOrder() != null) {
            final String[] fields = StringUtils.split(query.getOrder(), ',');
            for (String field : fields) {
                if (field.startsWith("-")) {
                    field = field.substring(1);
                    field = createAlias(q, query.getKindName(), field, alias);
                    q.addOrder(Order.desc(field));
                } else {
                    if (field.startsWith("+")) {
                        field = field.substring(1);
                    }
                    field = createAlias(q, query.getKindName(), field, alias);
                    q.addOrder(Order.asc(field));
                }
            }
        }

        if (query.getOffset() > 0) {
            q.setFirstResult(query.getOffset());
        }

        if (query.getLimit() > 0) {
            q.setMaxResults(query.getLimit());
        }

        final List<T> list = q.list();
        return new ArrayList<T>(new LinkedHashSet<T>(list));
    }

    protected String createAlias(final Criteria q, final String kind, final String field,
            final Map<String, String> aliasMap) {
        final String[] split = (kind + '.' + field).split("\\.");
        if (split.length < 2) {
			return field;
		}

        int i = 0;
        for (i = 0; i < split.length - 2; i++) {
            final String a = split[i + 1];
            final String b = split[i] + '.' + a;
            if (aliasMap.get(a) == null) {
                q.createAlias(b, a);
                aliasMap.put(a, b);
            }
        }

        return split[i] + '.' + split[i + 1];
    }

    protected Session getDatastore() {
        return HibernateUtil.getCurrentSession();
    }

    protected Session getDatastore(final boolean forceNewConnection) {
        return HibernateUtil.getCurrentSession(forceNewConnection);
    }

    @SuppressWarnings("unused")
    private void evict(final T item) {
        getDatastore().evict(item);
    }

    private class MySimpleExpression extends SimpleExpression {
        private static final long serialVersionUID = -6702408555310953731L;

        protected MySimpleExpression(final String propertyName, final Object value,
                final String op, final boolean ignoreCase) {

            super(propertyName, value, op, ignoreCase);
        }
    }

    @Override
    public List<T> getAll(final Collection<Key> keysNeeded) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }
}
