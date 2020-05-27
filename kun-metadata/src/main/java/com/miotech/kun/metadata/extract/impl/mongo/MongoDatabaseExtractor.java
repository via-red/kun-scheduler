package com.miotech.kun.metadata.extract.impl.mongo;

import com.google.common.collect.Iterators;
import com.miotech.kun.metadata.extract.Extractor;
import com.miotech.kun.metadata.model.Dataset;
import com.miotech.kun.metadata.model.MongoCluster;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class MongoDatabaseExtractor implements Extractor {
    private static Logger logger = LoggerFactory.getLogger(MongoDatabaseExtractor.class);

    private final MongoCluster cluster;

    private final String database;

    public MongoDatabaseExtractor(MongoCluster cluster, String database) {
        this.cluster = cluster;
        this.database = database;
    }

    @Override
    public Iterator<Dataset> extract() {
        MongoClient client = null;
        try {
            client = new MongoClient(new MongoClientURI(cluster.getUrl()));
            MongoDatabase useDatabase = client.getDatabase(this.database);
            MongoIterable<String> collections = useDatabase.listCollectionNames();
            return Iterators.concat(collections.map((collection) -> new MongoCollectionExtractor(cluster, database, collection).extract()).iterator());
        } catch (Exception e) {
            logger.error("mongo operate error: ", e);
            throw new RuntimeException(e);
        }
    }
}
