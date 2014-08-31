package com.maintainer.data.commands;

public abstract class AbstractCommand<T> {
    public abstract T execute() throws Exception;
}
