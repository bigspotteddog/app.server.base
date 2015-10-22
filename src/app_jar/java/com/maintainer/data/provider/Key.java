package com.maintainer.data.provider;

import java.io.CharArrayWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.maintainer.data.controller.GenericController;
import com.maintainer.data.controller.Resource;
import com.maintainer.data.model.MapEntityImpl;
import com.maintainer.data.model.ThreadLocalInfo;
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
                // ignored
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
        String string = asString();
        string = Utils.getEncodedKeyString(string);
        return string;
    }

    public String asString() {
        final StringBuilder buf = new StringBuilder();

        if (parent != null) {
            buf
            .append(parent.asString())
            .append(':');
        }

//        final String[] path = kindName.split("\\.");
//        final String k = path[path.length - 1];

        final boolean isStringId = String.class.isAssignableFrom(id.getClass());

        buf.append(kindName).append('(');

        if (isStringId) {
            buf.append('"');
        }

        buf.append(id);

        if (isStringId) {
            buf.append('"');
        }

        final String string = buf.append(')').toString();

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

    public static Key create(final String kind, final Object id) {
        return create(kind, id, null);
    }

    public static Key create(final String kind, final Object id, final Key parent) {
        return new Key(kind, id, parent);
    }

    public static Key fromString(String string) {
        string = Utils.fromEncodedKeyString(string);
        return fromDecodedString(string);
    }

    public static Key fromDecodedString(final String string) {
        final List<String> keys = new ArrayList<String>();
        final char[] incoming = string.toCharArray();
        int parens = 0;

        final CharArrayWriter chars = new CharArrayWriter();
        for (final char c : incoming) {
            switch (c) {
            case '(':
                chars.append(c);
                parens++;
                break;
            case ')':
                chars.append(c);
                parens--;
                break;
            case ':':
                if (parens == 0) {
                    keys.add(new String(chars.toCharArray()));
                    chars.reset();
                } else {
                    chars.append(c);
                }
                break;
            default:
                chars.append(c);
            }
        }

        if (chars.size() > 0) {
            keys.add(new String(chars.toCharArray()));
        }

        final Key key = getKey(keys);
        return key;
    }

    public static Key getKey(final List<String> strings) {
        final String me = strings.get(strings.size() - 1);

        Key parentKey = null;
        if (strings.size() > 1) {
            strings.remove(strings.size() - 1);
            parentKey = getKey(strings);
        }

        final int firstParen = me.indexOf('(');

        String kind = me.substring(0, firstParen);
        if (kind.indexOf('.') == -1) {
            Class<?> class1 = GenericController.getRegistered(kind);
            if (class1 != null && !MapEntityImpl.class.equals(class1)) {
                kind = Utils.getModelPackageName() + '.' + kind;
            }
        }

        String id = me.substring(firstParen + 1);
        id = id.substring(0, id.length() - 1);

        if (!Utils.isNumeric(id)) {
            if ((id.startsWith("\"") && id.endsWith("\""))) {
                id = id.substring(1, id.length() - 1);
            }
        } else {
            try {
                final Long lid = (Long) Utils.convert(id, Long.class);
                final Key meKey = Key.create(kind, lid, parentKey);
                return meKey;
            } catch (final Exception e) {
                // ignore this and process it as a string
            }
        }

        final Key meKey = Key.create(kind, id, parentKey);
        return meKey;
    }

    public static Key fromString2(String string) {
        string = Utils.fromEncodedKeyString(string);

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
