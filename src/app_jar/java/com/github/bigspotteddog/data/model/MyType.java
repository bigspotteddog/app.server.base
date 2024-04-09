package com.github.bigspotteddog.data.model;

@SuppressWarnings("serial")
@Resource(name = "types")
@Autocreate(id = "name")
public class MyType extends EntityImpl {
    private String name;
    private String className;

    public MyType() {
    }

    public MyType(final String name, final String className) {
        this.name = name;
        this.className = className;
    }

    public MyType(Class<?> class1) {
        name = class1.getSimpleName();
        className = class1.getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
