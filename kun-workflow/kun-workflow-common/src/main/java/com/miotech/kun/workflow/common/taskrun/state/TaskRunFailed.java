package com.miotech.kun.workflow.common.taskrun.state;

import com.miotech.kun.workflow.core.model.taskrun.BasicTaskRunState;
import com.miotech.kun.workflow.core.model.taskrun.TaskRunStatus;

public class TaskRunFailed extends BasicTaskRunState {

    public TaskRunFailed() {
        super(TaskRunStatus.FAILED);
    }


    @Override
    protected TaskRunStatus onReschedule(){
        return TaskRunStatus.CREATED;
    }

}
