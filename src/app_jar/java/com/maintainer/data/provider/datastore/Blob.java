package com.maintainer.data.provider.datastore;

import com.maintainer.data.model.EntityImpl;

public class Blob extends EntityImpl {

    private final byte[] bytes;
    private int version;

    public Blob(final byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }
}
