package com.maintainer.data.provider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.maintainer.util.Utils;

public class Key implements Comparable<Key>, Serializable {
    private static final String KEY_PATTERN = "([\\w\\.]+\\([^\\)]+\\))";
    private static final long serialVersionUID = -6778571338817109631L;
    transient private Class<?> kind;
    private Key parent;
    private String kindName;
    private Object id;

    protected Key() {}

    public Key(final String kind, final Object id) {
        this(kind, id, null);
    }

    public Key(final String kind, final Object id, final Key parent) {
        this.kindName = kind;
        this.id = id;
        this.parent = parent;
    }

    public Key getParent() {
        return parent;
    }

    public void setParent(final Key parent) {
        this.parent = parent;
    }

    public Class<?> getKind() {
        if (kind == null) {
            try {
                kind = Class.forName(kindName);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        return kind;
    }

    public void setKind(final Class<?> kind) {
        this.kind = kind;
        if (kind == null) {
            this.setKindName(null);
        }
        this.setKindName(kind.getName());
    }

    public Object getId() {
        return id;
    }

    public void setId(final Object id) {
        this.id = id;
    }

    public String getKindName() {
        return kindName;
    }

    public void setKindName(final String kindName) {
        this.kindName = kindName;
    }

    public static String getKindName(final Class<?> kind) {
        return kind.getName();
    }

    @Override
    public boolean equals(final Object other) {
        if (other == null) {
            return false;
        }
        return this.toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        if (parent != null) {
            buf
            .append(parent.toString())
            .append(':');
        }

        final String[] path = kindName.split("\\.");
        final String k = path[path.length - 1];

        final String string = buf
            .append(k)
            .append('(')
            .append(id)
            .append(')')
            .toString();

        return string;
    }

    @Override
    public int compareTo(final Key other) {
        return toString().compareTo(other.toString());
    }

    public static Key create(final Class<?> kind, final Object id) {
        return create(kind, id, null);
    }

    public static Key create(final Class<?> kind, final Object id, final Key parent) {
        return create(kind.getName(), id, parent);
    }

    public static Key create(final String kind, final Object id, final Key parent) {
        return new Key(kind, id, parent);
    }

    public static Key fromString(final String string) {
        final Pattern p = Pattern.compile(KEY_PATTERN);
        final Matcher m = p.matcher(string);

        final List<String> keys = new ArrayList<String>();
        while(m.find()) {
            final String key = m.group();
            keys.add(key);
        }

        Key parent = null;
        Key key = null;
        for (final String k : keys) {
            final String[] split = k.split("[\\(\\)]");

            String kindName = split[0];
            String className = kindName;
            if (className.indexOf('.') == -1) {
                className = Utils.getModelPackageName() + '.' + className;
            }

            try {
                final Class<?> kind = Class.forName(className);
                kindName = kind.getName();
            } catch(final Exception e) {
                e.printStackTrace();
            }

            final String id = split[1];
            key = Key.create(kindName, id, null);
            if (parent != null) {
                key.setParent(parent);
            }
            parent = key;
        }

        return key;
    }
}
