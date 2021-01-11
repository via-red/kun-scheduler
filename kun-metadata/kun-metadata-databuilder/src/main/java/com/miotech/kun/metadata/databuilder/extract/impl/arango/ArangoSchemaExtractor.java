package com.miotech.kun.metadata.databuilder.extract.impl.arango;

import com.google.common.collect.Iterators;
import com.google.inject.Singleton;
import com.miotech.kun.metadata.core.model.Dataset;
import com.miotech.kun.metadata.core.model.DatasetField;
import com.miotech.kun.metadata.databuilder.client.ArangoClient;
import com.miotech.kun.metadata.databuilder.extract.schema.DatasetSchemaExtractor;
import com.miotech.kun.metadata.databuilder.model.ArangoDataSource;
import com.miotech.kun.metadata.databuilder.model.DataSource;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Singleton
public class ArangoSchemaExtractor extends ArangoExistenceExtractor implements DatasetSchemaExtractor {

    @Override
    public List<DatasetField> extract(Dataset dataset, DataSource dataSource) {
        ArangoCollectionSchemaExtractor schemaExtractor = null;
        try {
            schemaExtractor = new ArangoCollectionSchemaExtractor((ArangoDataSource) dataSource,
                    dataset.getDatabaseName(), dataset.getName());
            return schemaExtractor.getSchema();
        } finally {
            if (schemaExtractor != null) {
                schemaExtractor.close();
            }
        }
    }

    @Override
    public Iterator<Dataset> extract(DataSource dataSource) {
        ArangoClient arangoClient = null;
        try {
            ArangoDataSource arangoDataSource = (ArangoDataSource) dataSource;
            arangoClient = new ArangoClient(arangoDataSource);
            Collection<String> databases = arangoClient.getDatabases();
            return Iterators.concat(databases.stream().filter(db -> !db.startsWith("_")).map(databasesName ->
                    new ArangoDatabaseSchemaExtractor(arangoDataSource, databasesName).extract()).iterator());
        } finally {
            if (arangoClient != null) {
                arangoClient.close();
            }
        }
    }

}
