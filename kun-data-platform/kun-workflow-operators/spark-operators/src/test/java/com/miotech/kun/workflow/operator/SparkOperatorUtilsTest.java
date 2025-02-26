package com.miotech.kun.workflow.operator;

import com.miotech.kun.commons.testing.MockServerTestBase;
import com.miotech.kun.workflow.testing.executor.MockOperatorContextImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.Assert.assertTrue;

import static com.miotech.kun.workflow.operator.SparkConfiguration.*;
import static com.miotech.kun.workflow.operator.SparkConfiguration.CONF_S3_SECRET_KEY;

public class SparkOperatorUtilsTest {
    private MockOperatorContextImpl context;
    SparkSqlOperatorV2 operator = new SparkSqlOperatorV2();
    SparkOperatorUtils sparkOperatorUtils = new SparkOperatorUtils();
    private static final Logger logger = LoggerFactory.getLogger(SparkOperatorUtilsTest.class);


    @BeforeEach
    public void initSparkOperatorUtils() {
        context = new MockOperatorContextImpl(operator);
        context.setParam(SPARK_SUBMIT_PARMAS, "{\"class\":\"com.miotech.sql.Application\"}");
        context.setParam(SPARK_CONF, "");
        context.setParam(SPARK_PROXY_USER, "hadoop");
        context.setParam(SPARK_APPLICATION, "s3://bucket/sql.jar");
        context.setParam(SPARK_APPLICATION_ARGS, " select 1 ");
        context.setParam(SPARK_YARN_HOST, "localhost:8088");
        context.setParam(CONF_LINEAGE_OUTPUT_PATH, "");
        context.setParam(CONF_LINEAGE_JAR_PATH, "");
        context.setParam(CONF_S3_ACCESS_KEY, "");
        context.setParam(CONF_S3_SECRET_KEY, "");

        sparkOperatorUtils.init(context, logger);
    }

    @Test
    public void testParseSparkSubmitParmas() {
        Map<String, String> params = new HashMap<>();
        params.put(" name ", " test spark app ");
        List<String> paramList = sparkOperatorUtils.parseSparkSubmitParmas(params);
        assertTrue(paramList.get(0).equals("--name"));
        assertTrue(paramList.get(1).equals("test spark app"));
    }

    @Test
    public void testParseSparkConf() {
        Map<String, String> sparkConf = new HashMap<>();
        sparkConf.put(" spark.files ", " test spark file ");
        List<String> sparkConfList = sparkOperatorUtils.parseSparkConf(sparkConf);
        assertTrue(sparkConfList.get(0).equals("--conf"));
        assertTrue(sparkConfList.get(1).equals("spark.files=test spark file"));
    }


    @Test
    public void testExecSparkSubmitCmd() {
//        mockGet("/ws/v1/cluster/apps/application_1628214162676_39692", "{\"app\":{\"id\":\"application_1628214162676_39692\",\"user\":\"hadoop\",\"name\":\"`新闻2.0`: news-operator-seq - 225121685399928832\",\"queue\":\"default\",\"state\":\"FINISHED\",\"finalStatus\":\"SUCCEEDED\",\"progress\":100.0,\"trackingUI\":\"History\",\"trackingUrl\":\"http://ip-10-0-2-14.ap-northeast-1.compute.internal:20888/proxy/application_1628214162676_39692/\",\"diagnostics\":\"\",\"clusterId\":1628214162676,\"applicationType\":\"SPARK\",\"applicationTags\":\"livy-batch-35471-pfypqhdu\",\"priority\":0,\"startedTime\":1630905203970,\"finishedTime\":1630907197965,\"elapsedTime\":1993995,\"amContainerLogs\":\"http://ip-10-0-2-232.ap-northeast-1.compute.internal:8042/node/containerlogs/container_1628214162676_39692_01_000001/hadoop\",\"amHostHttpAddress\":\"ip-10-0-2-232.ap-northeast-1.compute.internal:8042\",\"amRPCAddress\":\"ip-10-0-2-232.ap-northeast-1.compute.internal:32799\",\"allocatedMB\":-1,\"allocatedVCores\":-1,\"runningContainers\":-1,\"memorySeconds\":65920355,\"vcoreSeconds\":21867,\"queueUsagePercentage\":0.0,\"clusterUsagePercentage\":0.0,\"preemptedResourceMB\":0,\"preemptedResourceVCores\":0,\"numNonAMContainerPreempted\":0,\"numAMContainerPreempted\":0,\"preemptedMemorySeconds\":0,\"preemptedVcoreSeconds\":0,\"logAggregationStatus\":\"RUNNING\",\"unmanagedApplication\":false,\"amNodeLabelExpression\":\"CORE\"}}");

        List<String> cmd = new ArrayList<>();
        cmd.add("echo");
        cmd.add("14:30:12.471 INFO  - 21/09/05 14:30:12 INFO Client: Application report for application_1628214162676 (state: ACCEPTED)");
        cmd.add("1>&2");
        boolean status = sparkOperatorUtils.execSparkSubmitCmd(cmd);
        assertTrue(status);
    }

}