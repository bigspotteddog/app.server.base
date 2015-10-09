package com.maintainer.data.model;

import java.util.List;

@Resource(name="classes")
@Autocreate(id="name")
public class MyClass extends EntityImpl {
    private String name;
    @Autocreate(embedded=true)
    private List<MyField> fields;

    public String getName() {
        return name;
    }

    public List<MyField> getFields() {
        return fields;
    }
}
