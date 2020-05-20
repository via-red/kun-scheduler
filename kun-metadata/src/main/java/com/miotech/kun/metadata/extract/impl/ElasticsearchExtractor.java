package com.miotech.kun.metadata.extract.impl;

import com.miotech.kun.metadata.client.ElasticsearchClient;
import com.miotech.kun.metadata.extract.Extractor;
import com.miotech.kun.metadata.model.Dataset;
import com.miotech.kun.metadata.models.DBType;
import com.miotech.kun.metadata.models.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Elastic Search Extractor
 */
public class ElasticsearchExtractor implements Extractor {
    private static Logger logger = LoggerFactory.getLogger(ElasticsearchExtractor.class);

    @Override
    public Iterator<Dataset> extract() {
        try {
            // get Tables info
            List<String> tableStrs = ElasticsearchClient.getIndices("");
            for (String tableStr: tableStrs) {
                String[] tokens = tableStr.split(" +");
                // es metadata example
                // health status index uuid pri rep docs.count docs.deleted store.size pri.store.size
                Table.Builder tableBuilder = Table.newBuilder();
                tableBuilder.setDBType(DBType.ELASTICSEARCH);
                tableBuilder.setName(tokens[2]);
            }
        } catch (IOException e) {
            logger.error("Failed to get es indices", e);
        }
        return null;
    }

}
