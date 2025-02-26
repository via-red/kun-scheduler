package com.miotech.kun.dataquality.web;

import com.miotech.kun.commons.pubsub.publish.EventPublisher;
import com.miotech.kun.commons.pubsub.subscribe.EventSubscriber;
import com.miotech.kun.dataquality.web.utils.WorkflowUtils;
import com.miotech.kun.workflow.client.model.Operator;
import com.miotech.kun.workflow.core.pubsub.RedisEventPublisher;
import com.miotech.kun.workflow.core.pubsub.RedisEventSubscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author: Jie Chen
 * @created: 2020/7/17
 */
@Configuration
@ConditionalOnProperty(value = "testenv", havingValue = "false", matchIfMissing = true)
public class DataQualityBeanConfig {

    @Value("${workflow.base-url:http://kun-workflow:8088}")
    String workflowUrl;

    @Value("${redis.host}")
    private String redisHost = null;

    @Value("${redis.notify-channel:kun-notify}")
    private String channel;

    @Autowired
    WorkflowUtils workflowUtils;

    @Bean
    Operator getOperator() {
        return Operator.newBuilder()
                .withName(DataQualityConfiguration.WORKFLOW_OPERATOR_NAME)
                .withDescription("Data Quality Operator")
                .withClassName("com.miotech.kun.workflow.operator.DataQualityOperator")
                .build();
    }

    @Bean("dataQuality-subscriber")
    public EventSubscriber getRedisSubscriber() {
        JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), redisHost);
        return new RedisEventSubscriber(channel, jedisPool);
    }

    @Bean("dataQuality-publisher")
    public EventPublisher getRedisPublisher() {
        JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), redisHost);
        return new RedisEventPublisher(channel, jedisPool);
    }
}
