package com.miotech.kun.workflow.common.taskrun.vo;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.miotech.kun.metadata.core.model.dataset.DataStore;
import com.miotech.kun.workflow.common.taskrun.bo.TaskAttemptProps;
import com.miotech.kun.workflow.core.execution.Config;
import com.miotech.kun.workflow.core.model.common.Tick;
import com.miotech.kun.workflow.core.model.task.Task;
import com.miotech.kun.workflow.core.model.taskrun.TaskRun;
import com.miotech.kun.workflow.core.model.taskrun.TaskRunStatus;
import com.miotech.kun.commons.utils.JsonLongFieldDeserializer;

import java.time.OffsetDateTime;
import java.util.List;

public class TaskRunVO {
    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = JsonLongFieldDeserializer.class)
    private Long id;

    private Task task;

    private Config config;

    private Tick scheduledTick;

    private TaskRunStatus status;

    private List<DataStore> inlets;

    private List<DataStore> outlets;

    private OffsetDateTime queuedAt;

    private OffsetDateTime startAt;

    private OffsetDateTime endAt;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    private List<TaskAttemptProps> attempts;

    private List<TaskRun> failedUpstreamTaskRuns;

    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> dependentTaskRunIds;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public Tick getScheduledTick() {
        return scheduledTick;
    }

    public void setScheduledTick(Tick scheduledTick) {
        this.scheduledTick = scheduledTick;
    }

    public TaskRunStatus getStatus() {
        return status;
    }

    public void setStatus(TaskRunStatus status) {
        this.status = status;
    }

    public List<DataStore> getInlets() {
        return inlets;
    }

    public void setInlets(List<DataStore> inlets) {
        this.inlets = inlets;
    }

    public List<DataStore> getOutlets() {
        return outlets;
    }

    public void setOutlets(List<DataStore> outlets) {
        this.outlets = outlets;
    }

    public OffsetDateTime getQueuedAt() {
        return queuedAt;
    }

    public void setQueuedAt(OffsetDateTime queuedAt) {
        this.queuedAt = queuedAt;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(OffsetDateTime startAt) {
        this.startAt = startAt;
    }

    public OffsetDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(OffsetDateTime endAt) {
        this.endAt = endAt;
    }

    public List<TaskAttemptProps> getAttempts() {
        return attempts;
    }

    public void setAttempts(List<TaskAttemptProps> attempts) {
        this.attempts = attempts;
    }

    public List<TaskRun> getFailedUpstreamTaskRuns() {
        return failedUpstreamTaskRuns;
    }

    public void setFailedUpstreamTaskRuns(List<TaskRun> failedUpstreamTaskRuns) {
        this.failedUpstreamTaskRuns = failedUpstreamTaskRuns;
    }

    public List<Long> getDependentTaskRunIds() {
        return dependentTaskRunIds;
    }

    public void setDependentTaskRunIds(List<Long> dependencyTaskRunIds) {
        this.dependentTaskRunIds = dependencyTaskRunIds;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
