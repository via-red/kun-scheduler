package com.miotech.kun.metadata.databuilder.builder;

import com.google.inject.Inject;
import com.miotech.kun.commons.db.DatabaseOperator;
import com.miotech.kun.commons.testing.DatabaseTestBase;
import com.miotech.kun.commons.utils.Props;
import com.miotech.kun.metadata.common.dao.DataSourceDao;
import com.miotech.kun.metadata.common.dao.DatasetSnapshotDao;
import com.miotech.kun.metadata.common.dao.MetadataDatasetDao;
import com.miotech.kun.metadata.core.model.connection.ConnectionConfig;
import com.miotech.kun.metadata.core.model.connection.ConnectionInfo;
import com.miotech.kun.metadata.core.model.connection.ConnectionType;
import com.miotech.kun.metadata.core.model.connection.PostgresConnectionInfo;
import com.miotech.kun.metadata.core.model.constant.StatisticsMode;
import com.miotech.kun.metadata.core.model.dataset.Dataset;
import com.miotech.kun.metadata.core.model.dataset.DatasetSnapshot;
import com.miotech.kun.metadata.core.model.datasource.DataSource;
import com.miotech.kun.metadata.core.model.datasource.DatasourceType;
import com.miotech.kun.metadata.databuilder.container.PostgreSQLTestContainer;
import com.miotech.kun.metadata.databuilder.context.ApplicationContext;
import com.miotech.kun.metadata.databuilder.factory.DataSourceFactory;
import com.miotech.kun.metadata.databuilder.load.Loader;
import com.miotech.kun.metadata.databuilder.load.impl.PostgresLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;

public class MSEBuilderTest extends DatabaseTestBase {

    private static final String TABLE_NAME = "bar";

    @Inject
    private DatabaseOperator databaseOperator;

    @Inject
    public DataSourceDao dataSourceDao;

    @Inject
    public MetadataDatasetDao datasetDao;

    @Inject
    private DatasetSnapshotDao datasetSnapshotDao;

    @Inject
    public MCEBuilder mceBuilder;

    @Inject
    private MSEBuilder mseBuilder;

    @Override
    protected void configuration() {
        super.configuration();
        bind(Loader.class, PostgresLoader.class);
    }

    @BeforeEach
    public void setUp() {
        Props props = createProps();
        ApplicationContext.init(props);
    }

    @Test
    public void testExtractStatistics_success() {
        // Start PostgreSQL Container
        PostgreSQLContainer postgreSQLContainer = PostgreSQLTestContainer.executeInitSQLThenStart("sql/init_postgresql.sql");

        // Mock DataSource
        ConnectionInfo connectionInfo = new PostgresConnectionInfo(ConnectionType.POSTGRESQL,postgreSQLContainer.getHost(),postgreSQLContainer.getFirstMappedPort()
                ,postgreSQLContainer.getUsername(),postgreSQLContainer.getPassword());
        ConnectionConfig connectionConfig = ConnectionConfig.newBuilder().withUserConnection(connectionInfo).build();
        DataSource dataSource = DataSourceFactory.createDataSource(1l,"pg",connectionConfig, DatasourceType.POSTGRESQL);
        dataSourceDao.create(dataSource);

        // Extract schema
        mceBuilder.extractSchemaOfDataSource(dataSource.getId());
        Optional<Dataset> datasetOpt = datasetDao.findByName(TABLE_NAME);
        assertTrue(datasetOpt.isPresent());

        // Extract statistics
        List<DatasetSnapshot> datasetSnapshots = datasetSnapshotDao.findByDatasetGid(datasetOpt.get().getGid());
        assertThat(datasetSnapshots.size(), is(1));
        mseBuilder.extractStatistics(datasetOpt.get().getGid(), datasetSnapshots.get(0).getId(), StatisticsMode.FIELD);

        // Assert
        datasetSnapshots = datasetSnapshotDao.findByDatasetGid(datasetOpt.get().getGid());
        assertThat(datasetSnapshots.size(), is(1));
        DatasetSnapshot datasetSnapshot = datasetSnapshots.get(0);
        assertThat(datasetSnapshot.getStatisticsSnapshot().getTableStatistics().getRowCount(), is(3L));
        assertThat(datasetSnapshot.getStatisticsSnapshot().getTableStatistics().getTotalByteSize(), is(8192L));
        assertThat(datasetSnapshot.getStatisticsSnapshot().getFieldStatistics().size(), is(2));
    }

    @Test
    public void testExtractStatistics_snapshotNotExist() {
        // Start PostgreSQL Container
        PostgreSQLContainer postgreSQLContainer = PostgreSQLTestContainer.executeInitSQLThenStart("sql/init_postgresql.sql");

        // Mock DataSource
        ConnectionInfo connectionInfo = new PostgresConnectionInfo(ConnectionType.POSTGRESQL,postgreSQLContainer.getHost(),postgreSQLContainer.getFirstMappedPort()
                ,postgreSQLContainer.getUsername(),postgreSQLContainer.getPassword());
        ConnectionConfig connectionConfig = ConnectionConfig.newBuilder().withUserConnection(connectionInfo).build();
        DataSource dataSource = DataSourceFactory.createDataSource(1l,"pg",connectionConfig, DatasourceType.POSTGRESQL);
        dataSourceDao.create(dataSource);

        // Extract schema
        mceBuilder.extractSchemaOfDataSource(dataSource.getId());
        Optional<Dataset> datasetOpt = datasetDao.findByName(TABLE_NAME);
        assertTrue(datasetOpt.isPresent());

        // Extract statistics
        List<DatasetSnapshot> datasetSnapshots = datasetSnapshotDao.findByDatasetGid(datasetOpt.get().getGid());
        assertThat(datasetSnapshots.size(), is(1));
        Long notExistSnapshotId = -1L;
        mseBuilder.extractStatistics(datasetOpt.get().getGid(), notExistSnapshotId, StatisticsMode.FIELD);

        // Assert
        datasetSnapshots = datasetSnapshotDao.findByDatasetGid(datasetOpt.get().getGid());
        assertThat(datasetSnapshots.size(), is(1));
        DatasetSnapshot datasetSnapshot = datasetSnapshots.get(0);
        assertThat(datasetSnapshot.getStatisticsSnapshot(), nullValue());
    }

    private Props createProps() {
        Props props = new Props();
        props.put("datasource.jdbcUrl", "jdbc:h2:mem:test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE");
        props.put("datasource.username", "sa");
        props.put("datasource.driverClassName", "org.h2.Driver");
        return props;
    }

}
