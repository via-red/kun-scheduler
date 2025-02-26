package com.miotech.kun.metadata.common.cataloger;

import com.miotech.kun.metadata.common.client.*;
import com.miotech.kun.metadata.common.service.FieldMappingService;
import com.miotech.kun.metadata.core.model.connection.*;
import com.miotech.kun.metadata.core.model.datasource.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CatalogerFactory {

    private Logger logger = LoggerFactory.getLogger(CatalogerFactory.class);

    private final FieldMappingService fieldMappingService;

    private final ClientFactory clientFactory;

    @Inject
    public CatalogerFactory(FieldMappingService fieldMappingService, ClientFactory clientFactory) {
        this.fieldMappingService = fieldMappingService;
        this.clientFactory = clientFactory;
    }

    public Cataloger generateCataloger(DataSource dataSource, CatalogerConfig config) {
        ConnectionInfo metaConnectionInfo = dataSource.getConnectionConfig().getMetadataConnection();
        ConnectionInfo storageConnectionInfo = dataSource.getConnectionConfig().getStorageConnection();
        MetadataBackend metadataBackend = createMetaBackend(metaConnectionInfo, config);
        StorageBackend storageBackend = createStorage(storageConnectionInfo, config);
        logger.debug("metaConnectionInfo is {}\n storageConnectionInfo is {}", metaConnectionInfo, storageConnectionInfo);
        return new Cataloger(metadataBackend, storageBackend);
    }

    private MetadataBackend createMetaBackend(ConnectionInfo metaConnection, CatalogerConfig config) {
        ConnectionType metaType = metaConnection.getConnectionType();
        switch (metaType) {
            case GLUE:
                return new GlueBackend((GlueConnectionInfo) metaConnection, fieldMappingService, clientFactory, config);
            case HIVE_THRIFT:
                return new HiveThriftBackend((HiveMetaStoreConnectionInfo) metaConnection, fieldMappingService, clientFactory, config);
            case POSTGRESQL:
                return new PostgresBackend((PostgresConnectionInfo) metaConnection, fieldMappingService, config);
            default:
                throw new IllegalStateException("metadata type : " + metaType + " not support yet");
        }

    }

    private StorageBackend createStorage(ConnectionInfo storageConnection, CatalogerConfig config) {
        ConnectionType storageType = storageConnection.getConnectionType();
        switch (storageType) {
            case S3:
                return new S3Backend((S3ConnectionInfo) storageConnection, clientFactory);
            case HIVE_THRIFT:
                return new HiveThriftBackend((HiveMetaStoreConnectionInfo) storageConnection, fieldMappingService, clientFactory, config);
            case POSTGRESQL:
                return new PostgresBackend((PostgresConnectionInfo) storageConnection, fieldMappingService, config);
            default:
                throw new IllegalStateException("storage type : " + storageType + " not support yet");
        }

    }
}
