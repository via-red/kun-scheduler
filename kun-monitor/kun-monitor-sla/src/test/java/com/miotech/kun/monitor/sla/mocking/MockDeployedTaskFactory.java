package com.miotech.kun.monitor.sla.mocking;

import com.miotech.kun.commons.utils.IdGenerator;
import com.miotech.kun.dataplatform.facade.model.commit.TaskCommit;
import com.miotech.kun.dataplatform.facade.model.deploy.DeployedTask;
import com.miotech.kun.dataplatform.web.common.utils.DataPlatformIdGenerator;

import java.util.ArrayList;
import java.util.List;

public class MockDeployedTaskFactory {


    private MockDeployedTaskFactory() {}

    public static DeployedTask createDeployedTask() {
        return createDeployedTask(1).get(0);
    }

    public static List<DeployedTask> createDeployedTask(int num) {
        List<DeployedTask> tasksDefs = new ArrayList<>();

        for (int i = 0; i < num; i++) {
            TaskCommit taskCommit = MockTaskCommitFactory.createTaskCommit();
            long taskId = DataPlatformIdGenerator.nextDeployedTaskId();
            long workflowTaskId = IdGenerator.getInstance().nextId();
            tasksDefs.add(DeployedTask.newBuilder()
                    .withId(taskId)
                    .withName(taskCommit.getSnapshot().getName())
                    .withDefinitionId(taskCommit.getDefinitionId())
                    .withTaskTemplateName("SparkSQL")
                    .withTaskCommit(taskCommit)
                    .withWorkflowTaskId(workflowTaskId)
                    .withOwner(1L)
                    .withArchived(false)
                    .build());
        }
        return tasksDefs;
    }

    public static DeployedTask createDeployedTask(TaskCommit taskCommit) {
        long taskId = DataPlatformIdGenerator.nextDeployedTaskId();
        return DeployedTask.newBuilder()
                .withId(taskId)
                .withName(taskCommit.getSnapshot().getName())
                .withDefinitionId(taskCommit.getDefinitionId())
                .withTaskTemplateName("SparkSQL")
                .withTaskCommit(taskCommit)
                .withWorkflowTaskId(1L)
                .withOwner(1L)
                .withArchived(false)
                .build();
    }
}
