package com.maintainer.data.model;

import java.util.ArrayList;
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
        if (fields == null) {
            fields = new ArrayList<MyField>();
        }
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

    public void setName(String name) {
        this.name = name;
    }

    public void addField(MyField field) {
        getFields().add(field);
    }

    public void setRoute(String route) {
        this.route = route;
    }

    @Override
    public String toString() {
        return name + ": " + route;
    }

    public boolean hasFields() {
        return fields != null;
    }

    public void setFields(ArrayList<MyField> fields) {
        this.fields = fields;
    }
}
