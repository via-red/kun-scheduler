package com.miotech.kun.workflow.core.model.taskrun;

import java.util.Comparator;

public class TaskPriorityComparator implements Comparator<TaskAttempt> {
    @Override
    public int compare(TaskAttempt o1, TaskAttempt o2) {
        Integer priority1 = o1.getPriority() == null ? 16 : o1.getPriority();
        Integer priority2 = o2.getPriority() == null ? 16 : o2.getPriority();
        if (priority1 == priority2) {
            return o1.getId().compareTo(o2.getId());
        }
        return priority2 - priority1;
    }
}
