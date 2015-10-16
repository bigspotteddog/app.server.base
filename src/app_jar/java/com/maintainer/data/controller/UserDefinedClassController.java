package com.maintainer.data.controller;

import java.util.logging.Logger;

import com.maintainer.data.model.MapEntityImpl;
import com.maintainer.data.model.MyClass;

public class UserDefinedClassController extends GenericController<MyClass> {
    private static final Logger log = Logger.getLogger(UserDefinedClassController.class.getName());

    @Override
    protected void postPost(MyClass obj) throws Exception {
        super.postPost(obj);
        registerPath(obj);
    }

    @Override
    protected void postPut(MyClass obj) throws Exception {
        super.postPut(obj);
        registerPath(obj);
    }

    @Override
    protected void postDelete(MyClass obj) throws Exception {
        super.postDelete(obj);
        unregisterPath(obj);
    }

    private void unregisterPath(MyClass obj) {
        try {
            Class.forName(obj.getName());
        } catch (Exception e) {
            GenericController.unregister(obj.getName());
        }
    }

    private void registerPath(MyClass obj) {
        try {
            Class.forName(obj.getName());
        } catch (Exception e) {
            GenericController.register(obj.getName(), MapEntityImpl.class);
        }
    }
}
