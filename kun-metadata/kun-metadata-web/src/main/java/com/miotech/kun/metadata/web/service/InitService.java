package com.miotech.kun.metadata.web.service;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.miotech.kun.commons.rpc.RpcPublisher;
import com.miotech.kun.commons.utils.Props;
import com.miotech.kun.metadata.facade.MetadataServiceFacade;
import com.miotech.kun.metadata.web.constant.PropKey;
import com.miotech.kun.metadata.web.constant.TaskParam;
import com.miotech.kun.metadata.web.constant.WorkflowApiParam;
import com.miotech.kun.metadata.web.util.RequestParameterBuilder;
import com.miotech.kun.workflow.client.WorkflowClient;
import com.miotech.kun.workflow.client.model.Operator;
import com.miotech.kun.workflow.client.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Singleton
public class InitService {
    private static final Logger logger = LoggerFactory.getLogger(InitService.class);

    @Inject
    private WorkflowClient workflowClient;

    @Inject
    private Props props;

    @Inject
    private RpcPublisher rpcPublisher;

    @Inject
    private Injector injector;

    @Inject
    private MetadataServiceFacade metadataServiceFacade;

    public void publishRpcServices() {
        rpcPublisher.exportService(MetadataServiceFacade.class, "1.0", metadataServiceFacade);
    }

    public void initDataBuilder() {
        checkOperator(WorkflowApiParam.DATA_BUILDER_OPERATOR);
        checkTask(WorkflowApiParam.DATA_BUILDER_TASK_MANUAL, WorkflowApiParam.DATA_BUILDER_TASK_AUTO);
        uploadJar();
    }

    private void uploadJar() {
        // Upload jar
    }

    private Optional<Operator> findOperatorByName(String operatorName) {
        return workflowClient.getOperator(operatorName);
    }

    private Optional<Task> findTaskByName(String taskName) {
        return workflowClient.getTask(taskName);
    }

    private void createOperator(String operatorName) {
        Operator operatorOfCreated = workflowClient.saveOperator(operatorName, RequestParameterBuilder.buildOperatorForCreate(operatorName));
        setProp(PropKey.OPERATOR_ID, operatorOfCreated.getId().toString());
    }

    private void createTask(String taskName) {
        Task taskOfCreated = workflowClient.createTask(RequestParameterBuilder.buildTaskForCreate(taskName,
                props.getLong(PropKey.OPERATOR_ID), props));
        setProp(TaskParam.get(taskName).getTaskKey(), taskOfCreated.getId().toString());
    }

    private void checkOperator(String... operatorNames) {
        for (String operatorName : operatorNames) {
            Optional<Operator> operatorOpt = findOperatorByName(operatorName);
            if (operatorOpt.isPresent()) {
                props.put(PropKey.OPERATOR_ID, operatorOpt.get().getId().toString());
            } else {
                createOperator(operatorName);
                logger.info("Create Operator: {} Success", operatorName);
            }
        }
    }

    private void checkTask(String... taskNames) {
        for (String taskName : taskNames) {
            Optional<Task> taskOpt = findTaskByName(taskName);
            if (taskOpt.isPresent()) {
                props.put(TaskParam.get(taskName).getTaskKey(), taskOpt.get().getId().toString());
            } else {
                createTask(taskName);
                logger.info("Create Task: {} Success", taskName);
            }
        }
    }

    private void setProp(String key, String value) {
        props.put(key, value);
    }

}
