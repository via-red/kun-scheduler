package com.miotech.kun.metadata.databuilder.operator;

import com.google.inject.Injector;
import com.miotech.kun.commons.utils.ExceptionUtils;
import com.miotech.kun.commons.utils.Props;
import com.miotech.kun.metadata.databuilder.builder.MCEBuilder;
import com.miotech.kun.metadata.core.model.constant.DataBuilderDeployMode;
import com.miotech.kun.metadata.databuilder.context.ApplicationContext;
import com.miotech.kun.workflow.core.execution.*;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.miotech.kun.metadata.core.model.constant.OperatorKey.*;

public class MCEOperator extends KunOperator {
    private static final Logger logger = LoggerFactory.getLogger(MCEOperator.class);
    private Props props;

    @Override
    public void init() {
        props = buildPropsFromVariable(getContext());
        ApplicationContext.init(props);
    }

    @Override
    public boolean run() {
        DataSource dataSource = null;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("MCEOperator use props: {}", ApplicationContext.getContext().getProps());
            }

            Injector injector = ApplicationContext.getContext().getInjector();
            dataSource = injector.getInstance(DataSource.class);
            MCEBuilder dataBuilder = injector.getInstance(MCEBuilder.class);

            String deployModeStr = props.getString(DEPLOY_MODE);
            DataBuilderDeployMode deployMode = DataBuilderDeployMode.resolve(deployModeStr);
            switch (deployMode) {
                case DATASOURCE:
                    Long datasourceId = Long.parseLong(props.getString(DATASOURCE_ID));
                    dataBuilder.extractSchemaOfDataSource(datasourceId);
                    break;
                case DATASET:
                    Long gid = Long.parseLong(props.getString(GID));
                    dataBuilder.extractSchemaOfDataset(gid);
                    break;
                case PUSH:
                    dataBuilder.extractSchemaOfPush(props.getString(MCE));
                    break;
                default:
                    throw new IllegalArgumentException("Invalid deployMode: " + deployModeStr);
            }

            return true;
        } catch (Exception e) {
            logger.error("MCEOperator run error:", e);
            throw ExceptionUtils.wrapIfChecked(e);
        } finally {
            if (dataSource instanceof HikariDataSource) {
                ((HikariDataSource) dataSource).close();
            }
        }
    }

    @Override
    public void abort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConfigDef config() {
        ConfigDef configDef = new ConfigDef();
        configDef.define(DATASOURCE_JDBC_URL, ConfigDef.Type.STRING, "<should-configured-at-runtime>", true, "jdbcUrl", "jdbcUrl")
                .define(DATASOURCE_USERNAME, ConfigDef.Type.STRING, "<should-configured-at-runtime>", true, "username", "username")
                .define(DATASOURCE_PASSWORD, ConfigDef.Type.STRING, "<should-configured-at-runtime>", true, "password", "password")
                .define(DATASOURCE_DRIVER_CLASS_NAME, ConfigDef.Type.STRING, "<should-configured-at-runtime>", true, "driverClassName", "driverClassName")
                .define(DEPLOY_MODE, ConfigDef.Type.STRING, "<should-configured-at-runtime>", true, DEPLOY_MODE, DEPLOY_MODE)
                .define(DATASOURCE_ID, ConfigDef.Type.STRING, "<should-configured-at-runtime>", true, DATASOURCE_ID, DATASOURCE_ID)
                .define(GID, ConfigDef.Type.STRING, "<should-configured-at-runtime>", true, GID, GID)
                .define(MCE, ConfigDef.Type.STRING, "<should-configured-at-runtime>", true, MCE, MCE)
                .define(MSE_URL, ConfigDef.Type.STRING, "<should-configured-at-runtime>", true, MSE_URL, MSE_URL)
                .define(CATALOGER_WHITE_LIST, ConfigDef.Type.LIST, new ArrayList<>(), true, CATALOGER_WHITE_LIST, CATALOGER_WHITE_LIST)
                .define(CATALOGER_BLACK_LIST, ConfigDef.Type.LIST, new ArrayList<>(), true, CATALOGER_BLACK_LIST, CATALOGER_BLACK_LIST);

        return configDef;
    }

    @Override
    public Resolver getResolver() {
        return new NopResolver();
    }

    private Props buildPropsFromVariable(OperatorContext operatorContext) {
        Props props = PropsBuilder.putConn(operatorContext);

        props.put(DEPLOY_MODE, operatorContext.getConfig().getString(DEPLOY_MODE));
        props.put(DATASOURCE_ID, operatorContext.getConfig().getString(DATASOURCE_ID));
        props.put(GID, operatorContext.getConfig().getString(GID));
        props.put(MCE, operatorContext.getConfig().getString(MCE));
        props.put(MSE_URL, operatorContext.getConfig().getString(MSE_URL));
        List<String> whiteList = operatorContext.getConfig().getList(CATALOGER_WHITE_LIST);
        List<String> blackList = operatorContext.getConfig().getList(CATALOGER_BLACK_LIST);
        props.put(CATALOGER_WHITE_LIST, whiteList.stream().collect(Collectors.joining(",")));
        props.put(CATALOGER_BLACK_LIST, blackList.stream().collect(Collectors.joining(",")));
        return props;
    }

}
