package com.miotech.kun.metadata.facade;

import com.miotech.kun.metadata.core.model.DSI;
import com.miotech.kun.metadata.core.model.DataStore;
import com.miotech.kun.metadata.core.model.Dataset;

/**
 * Exposed RPC service interface of metadata service module.
 * @author Josh Ouyang
 */
public interface MetadataServiceFacade {
    /**
     * Obtain dataset model object (from remote) by given datastore as search key.
     * @deprecated use {@code MetadataServiceFacade.getDatasetByDSI} instead.
     * @param datastore key datastore object
     * @return Dataset model object. Returns null if not found by datastore key.
     */
    @Deprecated
    Dataset getDatasetByDatastore(DataStore datastore);

    /**
     * Obtain dataset model object (from remote) by given DSI as search key.
     * @param dataStoreIdentifier the data store identifier that represents target dataset
     * @return Dataset model object. Returns null if not found by datastore key.
     */
    Dataset getDatasetByDSI(DSI dataStoreIdentifier);
}
