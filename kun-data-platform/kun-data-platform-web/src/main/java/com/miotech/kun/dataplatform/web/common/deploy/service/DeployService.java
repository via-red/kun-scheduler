package com.miotech.kun.dataplatform.web.common.deploy.service;

import com.google.common.base.Preconditions;
import com.miotech.kun.dataplatform.facade.model.commit.TaskCommit;
import com.miotech.kun.dataplatform.facade.model.deploy.Deploy;
import com.miotech.kun.dataplatform.facade.model.deploy.DeployCommit;
import com.miotech.kun.dataplatform.facade.model.deploy.DeployStatus;
import com.miotech.kun.dataplatform.facade.model.deploy.DeployedTask;
import com.miotech.kun.dataplatform.facade.model.taskdefinition.TaskDefinition;
import com.miotech.kun.dataplatform.web.common.commit.service.TaskCommitService;
import com.miotech.kun.dataplatform.web.common.commit.vo.CommitRequest;
import com.miotech.kun.dataplatform.web.common.deploy.dao.DeployDao;
import com.miotech.kun.dataplatform.web.common.deploy.vo.DeployRequest;
import com.miotech.kun.dataplatform.web.common.deploy.vo.DeploySearchRequest;
import com.miotech.kun.dataplatform.web.common.deploy.vo.DeployVO;
import com.miotech.kun.dataplatform.web.common.taskdefinition.service.TaskDefinitionService;
import com.miotech.kun.dataplatform.web.common.utils.DataPlatformIdGenerator;
import com.miotech.kun.dataplatform.web.exception.UpstreamTaskNotPublishedException;
import com.miotech.kun.monitor.facade.alert.TaskNotifyConfigFacade;
import com.miotech.kun.security.service.BaseSecurityService;
import com.miotech.kun.workflow.client.model.PaginationResult;
import com.miotech.kun.workflow.utils.DateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DeployService extends BaseSecurityService {

    @Autowired
    private DeployDao deployDao;

    @Autowired
    private DeployedTaskService deployedTaskService;

    @Autowired
    private TaskCommitService commitService;

    @Autowired
    private TaskDefinitionService taskDefinitionService;

    @Autowired
    private TaskNotifyConfigFacade taskNotifyConfigFacade;

    public Deploy find(Long deployId) {
        return deployDao.fetchById(deployId)
                .<IllegalArgumentException>orElseThrow(() -> {
                    throw new IllegalArgumentException(String.format("Deploy not found: \"%s\"", deployId));
                });
    }

    public Deploy create(DeployRequest request) {
        List<TaskCommit> commits = commitService.find(request.getCommitIds());
        Preconditions.checkArgument(commits.size() > 0, "Deploy commits should not be empty");
        Optional<TaskCommit> oldCommit = commits.stream().filter(x -> !x.isLatestCommit()).findAny();
        oldCommit.ifPresent(c -> {
                    throw new IllegalArgumentException(
                            String.format("Task commit \"%s\" is not latest commit", c.getId())
                    );
                });

        Long deployId = DataPlatformIdGenerator.nextDeployId();
        String name = commits.stream()
                .map(x -> x.getSnapshot().getName())
                .collect(Collectors.joining("|"));
        log.debug("Create deploy \"{}\"-\"{}\" with commits \"{}\"",
                name,
                deployId,
                commits.stream()
                        .map(x -> x.getId().toString())
                        .collect(Collectors.joining(",")));
        List<DeployCommit> deployCommits = commits
                .stream()
                .map(x -> DeployCommit.newBuilder()
                        .withDeployId(deployId)
                        .withCommit(x.getId())
                        .withDeployStatus(DeployStatus.CREATED)
                        .build())
                .collect(Collectors.toList());
        Deploy deploy = Deploy.newBuilder()
                .withId(deployId)
                .withCreator(getCurrentUser().getId())
                .withName(name)
                .withCommits(deployCommits)
                .withSubmittedAt(DateTimeUtils.now())
                .withStatus(DeployStatus.CREATED)
                .build();
        deployDao.create(deploy);
        return deploy;
    }

    /**
     * publish a deploy to remote workflow server
     * @param deployId
     * @return
     */
    @Transactional
    public Deploy publish(Long deployId) {
        Deploy deploy = find(deployId);
        List<Long> commitIds = deploy.getCommits()
                .stream().map(DeployCommit::getCommit)
                .collect(Collectors.toList());
        log.debug("Publish deploy \"{}\" with commits \"{}\"",
                deployId,
                commitIds.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(",")));
        List<TaskCommit> commits = commitService.find(commitIds);
        Preconditions.checkArgument(!commits.isEmpty(), "Deploy commits should not be empty list");
        // do deploy
        reorderAndDeploy(commits);
        // update notification config
        for (TaskCommit taskCommit : commits) {
            updateNotifyConfig(taskCommit.getDefinitionId());
        }
        // update status
        DeployStatus success = DeployStatus.SUCCESS;
        List<DeployCommit> updatedCommits = deploy.getCommits()
                .stream().map(x -> x.cloneBuilder()
                .withDeployStatus(success).build())
                .collect(Collectors.toList());
        Deploy updatedDeploy = deploy.cloneBuilder()
                .withStatus(success)
                .withCommits(updatedCommits)
                .withDeployer(getCurrentUser().getId())
                .withDeployedAt(DateTimeUtils.now())
                .build();
        deployDao.updateDeploy(updatedDeploy);
        return updatedDeploy;
    }

    /**
     * if commits contains dependencies, should follow the dependency order
     * @param commits
     */
    private void reorderAndDeploy(List<TaskCommit> commits) {
        Map<Long, List<Long>> deployPackage = new HashMap<>();
        Map<Long, TaskCommit> deployCommitsMap = new HashMap<>();
        List<Long> allUpstreamDefIds = new ArrayList<>();
        for (TaskCommit taskCommit : commits) {
            List<Long> dependencyDefinitionIds = taskDefinitionService.resolveUpstreamTaskDefIds(
                    taskCommit.getSnapshot().getTaskPayload());
            allUpstreamDefIds.addAll(dependencyDefinitionIds);
            Long definitionId = taskCommit.getDefinitionId();
            deployPackage.put(definitionId, dependencyDefinitionIds);
            deployCommitsMap.put(definitionId, taskCommit);
        }

        // if current deploy package contains non-deployed task as dependency, should fail to deploy
        if(! allUpstreamDefIds.isEmpty()){
            Set<Long> dependentDefIds = new HashSet<>(allUpstreamDefIds);
            List<DeployedTask> deployedTasks = deployedTaskService.findByDefIds(allUpstreamDefIds);
            Set<Long> deployedDefIds = deployedTasks.stream().filter(x -> !x.isArchived()).map(x -> x.getDefinitionId()).collect(Collectors.toSet());
            Set<Long> currentDeployDefIds = deployPackage.keySet();
            dependentDefIds.removeAll(deployedDefIds);
            dependentDefIds.removeAll(currentDeployDefIds);
            if (!dependentDefIds.isEmpty()) {
                throw new UpstreamTaskNotPublishedException("At least one of upstream tasks hasn't been deployed yet. Please deploy upstream tasks first.");
            }
        }

        Collection<Long> pendingDefinitionIds = deployPackage.keySet();
        Queue<Long> workingQueue = new ArrayBlockingQueue(commits.size());

        do {
            if (!workingQueue.isEmpty()) {
                TaskCommit taskCommit = deployCommitsMap.get(workingQueue.poll());
                deployedTaskService.deployTask(taskCommit);
                pendingDefinitionIds.remove(taskCommit.getDefinitionId());
            }

            // for a sub DAG, must have a no dependency node
            // working queue must not empty after initial loop
            for (Long definitionId: pendingDefinitionIds) {
                List<Long> dependencies = deployPackage.get(definitionId)
                        .stream()
                        .filter(pendingDefinitionIds::contains)
                        .collect(Collectors.toList());
                deployPackage.put(definitionId, dependencies);
                if (dependencies.isEmpty()) {
                    workingQueue.offer(definitionId);
                }
            }
        } while(!workingQueue.isEmpty());
    }

    @Transactional
    public Deploy deployFast(Long definitionId, CommitRequest request) {
        // commit first
        TaskCommit commit = commitService.commit(definitionId, request.getMessage());
        List<Long> commitIds = Collections.singletonList(commit.getId());
        DeployRequest deployRequest = new DeployRequest();
        deployRequest.setCommitIds(commitIds);
        // create a deploy and publish
        Deploy deploy = create(deployRequest);
        return publish(deploy.getId());
    }

    /**
     * Update notify configurations by binding deployed workflow task ids with notification config in task definition
     * @param definitionId id of task definition
     */
    private void updateNotifyConfig(Long definitionId) {
        // Update deployed task notification configuration if presented
        TaskDefinition taskDefinition = taskDefinitionService.find(definitionId);
        Optional<DeployedTask> deployedTaskOptional = deployedTaskService.findOptional(definitionId);
        deployedTaskOptional.ifPresent(deployedTask -> taskNotifyConfigFacade.updateRelatedTaskNotificationConfig(
                deployedTask.getWorkflowTaskId(),
                taskDefinition.getTaskPayload().getNotifyConfig()
        ));
    }

    public PaginationResult<Deploy> search(DeploySearchRequest request) {
        Preconditions.checkArgument(request.getPageNum() > 0, "page number should be a positive number");
        Preconditions.checkArgument(request.getPageSize() > 0, "page size should be a positive number");

        return deployDao.search(request);
    }

    public DeployVO convertVO(Deploy deploy) {
        return new DeployVO(
                deploy.getId(),
                deploy.getName(),
                deploy.getCommits(),
                deploy.getCreator(),
                deploy.getSubmittedAt(),
                deploy.getDeployer(),
                deploy.getDeployedAt(),
                deploy.getStatus()
        );
    }
}
