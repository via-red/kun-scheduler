package com.miotech.kun.workflow.core.model.taskrun;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public enum TaskRunStatus {
    CREATED,
    QUEUED,
    RUNNING,
    SUCCESS,
    FAILED,
    RETRY,
    SKIPPED,
    ABORTING,
    ABORTED,
    ERROR,//系统错误
    UPSTREAM_FAILED,
    BLOCKED,
    CHECK,
    CHECK_FAILED;

    private static final Map<String, TaskRunStatus> mappings = new HashMap<>(16);

    static {
        for (TaskRunStatus status : values()) {
            mappings.put(status.name(), status);
        }
    }

    @Nullable
    public static TaskRunStatus resolve(@Nullable String status) {
        return (status != null ? mappings.get(status) : null);
    }

    public boolean matches(String status) {
        return (this == resolve(status));
    }

    public boolean isSuccess() {
        return this == SUCCESS || this == SKIPPED;
    }

    public boolean isFailure() {
        return this == FAILED || this == ABORTED || this == CHECK_FAILED;
    }

    public boolean isFinished() {
        return isSuccess() || isFailure() || isAborted();
    }

    public boolean isError() {
        return this == ERROR;
    }

    public boolean isAborted() {
        return this == ABORTED;
    }

    public boolean isSkipped() {
        return this == SKIPPED;
    }

    public boolean isTermState() {
        return this == SUCCESS || this == FAILED || this == ABORTED || this == UPSTREAM_FAILED
                || this == CHECK_FAILED;
    }
    public boolean isRunning(){
        return this == RUNNING;
    }

    public boolean isChecking(){
        return this == CHECK;
    }

    public boolean isUpstreamFailed() {
        return this == UPSTREAM_FAILED;
    }

    public boolean isCreated() {
        return this == CREATED;
    }

    public boolean isBlocking(){
        return this == BLOCKED;
    }

    public boolean isReady(){
        return !isCreated() && !isUpstreamFailed() && !isBlocking();
    }
}
