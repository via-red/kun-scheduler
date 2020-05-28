package com.miotech.kun.metadata.extract.impl.hive;

import com.beust.jcommander.internal.Lists;
import com.miotech.kun.metadata.client.JDBCClient;
import com.miotech.kun.metadata.constant.DatabaseType;
import com.miotech.kun.metadata.extract.template.ExtractorTemplate;
import com.miotech.kun.metadata.extract.tool.DatasetNameGenerator;
import com.miotech.kun.metadata.model.DatasetField;
import com.miotech.kun.metadata.model.DatasetFieldStat;
import com.miotech.kun.metadata.model.DatasetStat;
import com.miotech.kun.metadata.model.HiveCluster;
import com.miotech.kun.workflow.core.model.lineage.DataStore;
import com.miotech.kun.workflow.core.model.lineage.HiveTableStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HiveTableExtractor extends ExtractorTemplate {
    private static Logger logger = LoggerFactory.getLogger(HiveTableExtractor.class);

    private final String database;
    private final String table;
    private final HiveCluster hiveCluster;

    public HiveTableExtractor(HiveCluster cluster, String database, String table) {
        super(cluster);
        this.database = database;
        this.table = table;
        this.hiveCluster = cluster;
    }

    @Override
    public List<DatasetField> getSchema() {
        // Get schema information of table
        List<DatasetField> fields = Lists.newArrayList();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = JDBCClient.getConnection(DatabaseType.MYSQL, hiveCluster.getMetaStoreUrl(),
                    hiveCluster.getMetaStoreUsername(), hiveCluster.getMetaStorePassword());
            String sql = "SELECT source.* FROM  " +
                    "    (SELECT t.TBL_ID, d.NAME as `schema`, t.TBL_NAME name, t.TBL_TYPE, tp.PARAM_VALUE as description,  " +
                    "           p.PKEY_NAME as col_name, p.INTEGER_IDX as col_sort_order,  " +
                    "           p.PKEY_TYPE as col_type, p.PKEY_COMMENT as col_description, 1 as is_partition_col,  " +
                    "           IF(t.TBL_TYPE = 'VIRTUAL_VIEW', 1, 0) is_view " +
                    "    FROM TBLS t" +
                    "    JOIN DBS d ON t.DB_ID = d.DB_ID" +
                    "    JOIN PARTITION_KEYS p ON t.TBL_ID = p.TBL_ID " +
                    "    LEFT JOIN TABLE_PARAMS tp ON (t.TBL_ID = tp.TBL_ID AND tp.PARAM_KEY='comment') " +
                    "    WHERE t.TBL_NAME = ? " +
                    "    UNION " +
                    "    SELECT t.TBL_ID, d.NAME as `schema`, t.TBL_NAME name, t.TBL_TYPE, tp.PARAM_VALUE as description, " +
                    "           c.COLUMN_NAME as col_name, c.INTEGER_IDX as col_sort_order, " +
                    "           c.TYPE_NAME as col_type, c.COMMENT as col_description, 0 as is_partition_col, " +
                    "           IF(t.TBL_TYPE = 'VIRTUAL_VIEW', 1, 0) is_view " +
                    "    FROM TBLS t " +
                    "    JOIN DBS d ON t.DB_ID = d.DB_ID " +
                    "    JOIN SDS s ON t.SD_ID = s.SD_ID " +
                    "    JOIN COLUMNS_V2 c ON s.CD_ID = c.CD_ID " +
                    "    LEFT JOIN TABLE_PARAMS tp ON (t.TBL_ID = tp.TBL_ID AND tp.PARAM_KEY='comment') " +
                    "    WHERE t.TBL_NAME = ? " +
                    "    ) source " +
                    "    ORDER by tbl_id, is_partition_col desc;";
            statement = connection.prepareStatement(sql);

            statement.setString(1, table);
            statement.setString(2, table);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String name = resultSet.getString(6);
                String type = resultSet.getString(8);
                String description = resultSet.getString(9);

                DatasetField field = new DatasetField(name, type, description);
                fields.add(field);
            }
        } catch (ClassNotFoundException classNotFoundException) {
            logger.error("driver class not found, DatabaseType: {}", DatabaseType.MYSQL.getName(), classNotFoundException);
            throw new RuntimeException(classNotFoundException);
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        } finally {
            JDBCClient.close(connection, statement, resultSet);
        }

        return fields;
    }

    @Override
    public DatasetFieldStat getFieldStats(DatasetField datasetField) {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            long distinctCount = 0;
            long nonnullCount = 0;
            connection = JDBCClient.getConnection(DatabaseType.HIVE, hiveCluster.getDataStoreUrl() + "/" + database, hiveCluster.getDataStoreUsername(), hiveCluster.getDataStorePassword());
            String sql = "SELECT COUNT(*) FROM (SELECT " + datasetField.getName() + " FROM " + table + " GROUP BY " + datasetField.getName() + ") t1";
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);

            while (resultSet.next()) {
                distinctCount = resultSet.getLong(1);
            }

            sql = "SELECT COUNT(*) FROM " + table + " WHERE " + datasetField.getName() + " IS NOT NULL";
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);

            while (resultSet.next()) {
                nonnullCount = resultSet.getLong(1);
            }

            DatasetFieldStat result = new DatasetFieldStat(datasetField.getName(), distinctCount, nonnullCount, null, new Date());
            return result;
        } catch (ClassNotFoundException classNotFoundException) {
            logger.error("driver class not found, DatabaseType: {}", DatabaseType.MYSQL.getName(), classNotFoundException);
            throw new RuntimeException(classNotFoundException);
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        } finally {
            JDBCClient.close(connection, statement, resultSet);
        }

    }

    @Override
    public DatasetStat getTableStats() {
        DatasetStat.Builder datasetStatBuilder = DatasetStat.newBuilder();

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = JDBCClient.getConnection(DatabaseType.HIVE, hiveCluster.getDataStoreUrl() + "/" + database, hiveCluster.getDataStoreUsername(), hiveCluster.getDataStorePassword());
            String sql = "SELECT COUNT(*) FROM " + table;
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);

            while (resultSet.next()) {
                Long rowCount = resultSet.getLong(1);
                datasetStatBuilder.withRowCount(rowCount);
                datasetStatBuilder.withStatDate(new Date());
            }
        } catch (ClassNotFoundException classNotFoundException) {
            logger.error("driver class not found, DatabaseType: {}", DatabaseType.HIVE.getName(), classNotFoundException);
            throw new RuntimeException(classNotFoundException);
        } catch (SQLException sqlException) {
            throw new RuntimeException(sqlException);
        } finally {
            JDBCClient.close(connection, statement, resultSet);
        }

        return datasetStatBuilder.build();
    }

    @Override
    protected DataStore getDataStore() {
        return new HiveTableStore(hiveCluster.getDataStoreUrl(), database, table);
    }

    @Override
    protected String getName() {
        return DatasetNameGenerator.generateDatasetName(DatabaseType.HIVE, table);
    }

    @Override
    protected long getClusterId() {
        return cluster.getClusterId();
    }

}
