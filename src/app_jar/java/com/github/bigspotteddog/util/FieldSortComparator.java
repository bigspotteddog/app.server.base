package com.github.bigspotteddog.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class FieldSortComparator implements Comparator<Object> {

    private final Class<?> clazz;
    private final String[] sorts;

    public FieldSortComparator(Class<?> clazz, String order) {
        this.clazz = clazz;
        this.sorts = order.replaceAll("^[,\\s]+", "").split("[,\\s]+");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public int compare(Object o1, Object o2) {
        int compareTo = 0;
        try {
            for (String s : sorts) {
                int direction = 1;
                if (s.startsWith("-")) {
                    direction = -1;
                    s = s.substring(1);
                }

                List<Field> fields = getFields(clazz, s);
                Object v1 = getValue(o1, fields);
                Object v2 = getValue(o2, fields);

                v1 = Utils.convert(v1, clazz);
                v2 = Utils.convert(v2, clazz);

                if (!Comparable.class.isAssignableFrom(v1.getClass())
                        || !Comparable.class.isAssignableFrom(v2.getClass())) {
                    throw new Exception("Not comparable.");
                }

                if (!v1.getClass().isAssignableFrom(v2.getClass())) {
                    throw new Exception("Not compatible");
                }

                Comparable c1 = (Comparable) v1;
                Comparable c2 = (Comparable) v2;

                compareTo = c1.compareTo(c2) * direction;
                if (compareTo != 0) {
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return compareTo;
    }

    private List<Field> getFields(Class<?> clazz, String order) throws Exception {
        String[] split = order.split("\\.");

        List<Field> fields = new ArrayList<Field>();
        for (String s : split) {
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField(s);
                    fields.add(field);
                    clazz = field.getType();
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
        }

        return fields;
    }

    private Object getValue(Object obj, List<Field> fields) throws Exception {
        if (fields.isEmpty()) {
            return "";
        }

        Field typeField = fields.get(fields.size() - 1);
        Class type = typeField.getType();

        for (Field f : fields) {
            if (obj == null) {
                return "";
            }

            Class<?> fieldType = f.getType();
            if (Collection.class.isAssignableFrom(fieldType)) {
                return "";
            }

            f.setAccessible(true);
            obj = f.get(obj);
        }

        if (obj == null) {
            return "";
        }

        return obj;
    }
}
