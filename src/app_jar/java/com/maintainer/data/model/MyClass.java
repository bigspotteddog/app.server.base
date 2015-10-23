package com.maintainer.data.model;

import java.util.List;

@SuppressWarnings("serial")
@Resource(name="classes")
@Autocreate(id="name")
public class MyClass extends EntityImpl {
    private String name;
    private String route;

    @NotIndexed
    private String description;

    @NotIndexed
    @Autocreate(embedded=true)
    private List<MyField> fields;

    @NotIndexed
    private String baseClassName;

    public String getName() {
        return name;
    }

    public String getRoute() {
        return route;
    }

    public String getDescription() {
        return description;
    }

    public List<MyField> getFields() {
        return fields;
    }

    public String getBaseClassName() {
        return baseClassName;
    }

    public Class<?> getType() {
        if (baseClassName != null) {
            try {
                Class<?> clazz = Class.forName(baseClassName);
                return clazz;
            } catch (ClassNotFoundException e) {
                // ignored
            }
        } else {
            return MapEntityImpl.class;
        }
        return null;
    }
}
