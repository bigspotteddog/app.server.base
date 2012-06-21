package com.maintainer.data.provider.datastore;

import com.maintainer.data.model.EntityImpl;

public class VersionedDatastoreDataProvider extends DatastoreDataProvider<EntityImpl> {

    @Override
    protected boolean checkEqual(EntityImpl target, EntityImpl existing) throws Exception {
        boolean equal = super.checkEqual(target, existing);
        if (!equal) {
            target.setId(null);
        }
        return equal;
    }
}
