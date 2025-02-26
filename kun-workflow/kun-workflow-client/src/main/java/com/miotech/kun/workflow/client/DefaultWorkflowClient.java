package com.miotech.kun.workflow.client;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.miotech.kun.workflow.client.model.*;
import com.miotech.kun.workflow.core.model.common.Tag;
import com.miotech.kun.workflow.core.model.executetarget.ExecuteTarget;
import com.miotech.kun.workflow.core.model.lineage.DatasetLineageInfo;
import com.miotech.kun.workflow.core.model.lineage.EdgeInfo;
import com.miotech.kun.workflow.core.model.lineage.node.DatasetInfo;
import com.miotech.kun.workflow.core.model.taskrun.TaskRunStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultWorkflowClient implements WorkflowClient {

    private static final Logger logger = LoggerFactory.getLogger(DefaultWorkflowClient.class);
    private final WorkflowApi wfApi;

    public DefaultWorkflowClient(WorkflowApi wfApi) {
        this.wfApi = wfApi;
    }

    public DefaultWorkflowClient(String baseUrl) {
        wfApi = new WorkflowApi(baseUrl);
    }

    @Override
    public Operator saveOperator(String name, Operator operator) {
        Optional<Operator> optionalOperator = getOperator(name);
        Operator newOperator;
        if (optionalOperator.isPresent()) {
            long opId = optionalOperator.get().getId();
            newOperator = wfApi.updateOperator(opId, operator);
        } else {
            newOperator = wfApi.createOperator(operator);
        }
        return newOperator;
    }

    @Override
    public Operator updateOperator(Long operatorId, Operator operator) {
        return wfApi.updateOperator(operatorId, operator);
    }

    @Override
    public void deleteOperator(Long operatorId) {
        wfApi.deleteOperator(operatorId);
    }

    @Override
    public void uploadOperatorJar(Long id, File jarFile) {
        wfApi.uploadJar(id, jarFile);
    }

    @Override
    public void updateOperatorJar(String name, File jarFile) {
        Optional<Operator> optionalOperator = getOperator(name);
        if (optionalOperator.isPresent()) {
            wfApi.uploadJar(optionalOperator.get().getId(), jarFile);
        } else {
            throw new IllegalArgumentException("Operator not found with name " + name);
        }
    }

    @Override
    public Optional<Operator> getOperator(String name) {
        List<Operator> operators = wfApi.getOperators(
                OperatorSearchRequest.newBuilder()
                        .withName(name)
                        .build())
                .getRecords();
        return operators.stream()
                .filter(x -> x.getName().equals(name))
                .findAny();
    }

    @Override
    public List<Operator> getExistOperators() {
        List<Operator> operators = wfApi.getOperators(
                OperatorSearchRequest.newBuilder()
                        .build())
                .getRecords();
        return operators;
    }

    @Override
    public Operator getOperator(Long id) {
        return wfApi.getOperator(id);
    }

    @Override
    public Task createTask(Task task) {
        return wfApi.createTask(task);
    }

    @Override
    public Task getTask(Long taskId) {
        return wfApi.getTask(taskId);
    }

    @Override
    public Optional<Task> getTask(String name) {
        List<Task> tasks = wfApi.getTasks(TaskSearchRequest.newBuilder().withName(name).build());
        return tasks.stream().filter(x -> x.getName().equals(name)).findAny();
    }

    @Override
    public Task saveTask(Task task, List<Tag> filterTags) {
        if (filterTags != null && !filterTags.isEmpty()) {
            String tags = filterTags.stream()
                    .map(Tag::toString)
                    .collect(Collectors.joining(","));
            TaskSearchRequest request = TaskSearchRequest.newBuilder()
                    .withTags(filterTags)
                    .build();
            PaginationResult<Task> taskPage = wfApi.searchTasks(request);
            if (taskPage.getTotalCount() == 0) {
                logger.debug("do not found task \"{}\" with tag \"{}\", create new one", task.getName(), tags);
                return createTask(task);
            }
            List<Task> tasks = taskPage.getRecords();
            Preconditions.checkArgument(tasks.size() == 1, "Multiple tasks found for tags: " + filterTags);
            logger.debug("Update task \"{}\" with tag \"{}\"", task.getName(), tags);
            return wfApi.updateTask(tasks.get(0).getId(), task);
        } else if (task.getId() > 0) {
            logger.debug("Update task \"{}\" with id \"{}\"", task.getName(), task.getId());
            return wfApi.updateTask(task.getId(), task);
        } else {
            logger.debug("Create new task \"{}\"", task.getName());
            return createTask(task);
        }
    }

    @Override
    public void deleteTask(Long taskId) {
        wfApi.deleteTask(taskId);
    }

    @Override
    public TaskRun executeTask(Task task, Map<String, Object> taskConfig) {
        return executeTask(task, taskConfig, null);
    }

    @Override
    public TaskRun executeTask(Task task, Map<String, Object> taskConfig, Long targetId) {
        Task saved;
        Optional<Task> taskOptional = getTask(task.getName());
        if (taskOptional.isPresent()) {
            Task existTask = taskOptional.get();
            saved = wfApi.updateTask(existTask.getId(), task);
        } else {
            saved = wfApi.createTask(task);
        }
        return executeTask(saved.getId(), taskConfig, targetId);
    }

    @Override
    public TaskRun executeTask(Long taskId, Map<String, Object> taskConfig) {
        return executeTask(taskId, taskConfig, null);
    }

    @Override
    public TaskRun executeTask(Long taskId, Map<String, Object> taskConfig, Long targetId) {
        RunTaskRequest request = new RunTaskRequest();
        request.addTaskConfig(taskId, taskConfig != null ? taskConfig : Maps.newHashMap());
        request.setTargetId(targetId);
        List<Long> taskRunIds = wfApi.runTasks(request);
        if (taskRunIds.isEmpty()) {
            throw new WorkflowClientException("No task run found after execution for task: " + taskId);
        }

        return wfApi.getTaskRun(taskRunIds.get(0));
    }

    @Override
    public Map<Long, TaskRun> executeTasks(RunTaskRequest request) {
        List<Long> taskRunIds = wfApi.runTasks(request);
        TaskRunSearchRequest taskRunSearchRequest = TaskRunSearchRequest
                .newBuilder()
                .withTaskRunIds(taskRunIds)
                .build();
        List<TaskRun> taskRunList = wfApi.searchTaskRuns(taskRunSearchRequest)
                .getRecords();
        return taskRunList.stream().collect(Collectors.toMap(x -> x.getTask().getId(), x -> x));
    }

    @Override
    public TaskDAG getTaskDAG(Long taskId, int upstreamLevel, int downstreamLevel) {
        return wfApi.getTaskDAG(taskId, upstreamLevel, downstreamLevel);
    }

    @Override
    public TaskRun getTaskRun(Long taskRunId) {
        return wfApi.getTaskRun(taskRunId);
    }

    @Override
    public PaginationResult<TaskRun> searchTaskRun(TaskRunSearchRequest request) {
        return wfApi.searchTaskRuns(request);
    }

    @Override
    public Integer countTaskRun(TaskRunSearchRequest request) {
        return wfApi.countTaskRuns(request);
    }

    @Override
    public TaskRunState getTaskRunState(Long taskRunId) {
        return wfApi.getTaskRunStatus(taskRunId);
    }

    @Override
    public TaskRunLog getLatestRunLog(Long taskRunId) {
        return getLatestRunLog(taskRunId, -1);
    }

    @Override
    public TaskRunLog getLatestRunLog(Long taskRunId, Integer attempt) {
        TaskRunLogRequest request = TaskRunLogRequest.newBuilder()
                .withTaskRunId(taskRunId)
                .withAttempt(attempt)
                .build();
        return wfApi.getTaskRunLog(request);
    }

    @Override
    public TaskRunLog getLatestRunLog(Long taskRunId, Integer start, Integer end) {
        return getLatestRunLog(taskRunId, start, end, -1);
    }

    @Override
    public TaskRunLog getLatestRunLog(Long taskRunId, Integer start, Integer end, Integer attempt) {
        TaskRunLogRequest request = TaskRunLogRequest.newBuilder()
                .withTaskRunId(taskRunId)
                .withStartLine(start)
                .withEndLine(end)
                .withAttempt(attempt)
                .build();
        return wfApi.getTaskRunLog(request);
    }

    @Override
    public TaskRunDAG getTaskRunDAG(Long taskRunId, int upstreamLevel, int downstreamLevel) {
        return wfApi.getTaskRunDAG(taskRunId, upstreamLevel, downstreamLevel);
    }

    @Override
    public TaskRunLog getTaskRunLog(TaskRunLogRequest logRequest) {
        return wfApi.getTaskRunLog(logRequest);
    }

    @Override
    public TaskRun stopTaskRun(Long taskRunId) {
        wfApi.stopTaskRun(taskRunId);
        return wfApi.getTaskRun(taskRunId);
    }

    @Override
    public TaskRun restartTaskRun(Long taskRunId) {
        wfApi.restartTaskRun(taskRunId);
        return wfApi.getTaskRun(taskRunId);
    }

    @Override
    public void stopTaskRuns(List<Long> taskRunIds) {
        for (Long taskRunId : taskRunIds) {
            wfApi.stopTaskRun(taskRunId);
        }
    }

    @Override
    public List<TaskRun> getLatestTaskRuns(Long taskId, List<TaskRunStatus> filterStatus, int limit) {
        return wfApi.getLatestTaskRuns(taskId, filterStatus, limit);
    }

    @Override
    public Map<Long, List<TaskRun>> getLatestTaskRuns(List<Long> taskIds, int limit) {
        return wfApi.getLatestTaskRuns(taskIds, limit , true);
    }

    /**
     * get latest TaskRun
     * @param taskIds
     * @param limit
     * @param containsAttempt true:taskrun contains all taskAttempt info
     * @return
     */
    public Map<Long, List<TaskRun>> getLatestTaskRuns(List<Long> taskIds, int limit , boolean containsAttempt) {
        return wfApi.getLatestTaskRuns(taskIds, limit , containsAttempt);
    }

    @Override
    public DatasetLineageInfo getLineageNeighbors(Long datasetGid, LineageQueryDirection direction, int depth) {
        return wfApi.getLineageNeighbors(datasetGid, direction, depth);
    }

    @Override
    public EdgeInfo getLineageEdgeInfo(Long upstreamDatasetGid, Long downstreamDatasetGid) {
        return wfApi.getLineageEdgeInfo(upstreamDatasetGid, downstreamDatasetGid);
    }

    @Override
    public Set<DatasetInfo> fetchOutletNodes(Long taskId) {
        return wfApi.fetchOutletNodes(taskId);
    }

    @Override
    public List<VariableVO> getAllVariables() {
        return wfApi.getAllVariables();
    }

    @Override
    public VariableVO createVariable(VariableVO variableVO) {
        return wfApi.createVariable(variableVO);
    }

    @Override
    public VariableVO updateVariable(VariableVO variableVO) {
        return wfApi.updateVariable(variableVO);
    }

    @Override
    public Boolean deleteVariable(String namespace, String key) {
        return wfApi.deleteVariable(namespace + "." + key);
    }

    @Override
    public Boolean changeTaskRunPriority(Long taskRunId, Integer priority) {
        return wfApi.changePriority(taskRunId, priority);
    }

    @Override
    public List<ExecuteTarget> getTargetList() {
        return wfApi.getTargetList();
    }

    @Override
    public void removeTaskRunDependency(Long taskRunId, List<Long> upstreamTaskRunIds) {
        wfApi.removeTaskRunDependency(taskRunId,upstreamTaskRunIds);
    }
}
