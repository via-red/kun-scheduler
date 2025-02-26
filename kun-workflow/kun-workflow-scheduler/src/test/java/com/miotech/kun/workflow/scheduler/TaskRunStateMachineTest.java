package com.miotech.kun.workflow.scheduler;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.miotech.kun.commons.testing.DatabaseTestBase;
import com.miotech.kun.workflow.TaskRunStateMachine;
import com.miotech.kun.workflow.common.lineage.service.LineageService;
import com.miotech.kun.workflow.common.task.dao.TaskDao;
import com.miotech.kun.workflow.common.taskrun.dao.TaskRunDao;
import com.miotech.kun.workflow.core.event.TaskRunTransitionEvent;
import com.miotech.kun.workflow.core.event.TaskRunTransitionEventType;
import com.miotech.kun.workflow.core.model.task.Task;
import com.miotech.kun.workflow.core.model.taskrun.TaskAttempt;
import com.miotech.kun.workflow.core.model.taskrun.TaskRun;
import com.miotech.kun.workflow.core.model.taskrun.TaskRunStatus;
import com.miotech.kun.workflow.testing.factory.MockTaskAttemptFactory;
import com.miotech.kun.workflow.testing.factory.MockTaskFactory;
import com.miotech.kun.workflow.testing.factory.MockTaskRunFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TaskRunStateMachineTest extends DatabaseTestBase {

    private TaskRunStateMachine taskRunStateMachine;

    @Inject
    private TaskDao taskDao;

    @Inject
    private TaskRunDao taskRunDao;

    @Inject
    private EventBus eventBus;

    public static Stream<Arguments> prepareData() {
        Task task = MockTaskFactory.createTask();
        //prepare attempt
        TaskAttempt createdAttempt = prepareAttempt(task, TaskRunStatus.CREATED);
        TaskAttempt blockedAttempt = prepareAttempt(task, TaskRunStatus.BLOCKED);
        TaskAttempt upstreamFailedAttempt = prepareAttempt(task, TaskRunStatus.UPSTREAM_FAILED);
        TaskAttempt queuedAttempt = prepareAttempt(task, TaskRunStatus.QUEUED);
        TaskAttempt runningAttempt = prepareAttempt(task, TaskRunStatus.RUNNING);
        TaskAttempt failedAttempt = prepareAttempt(task, TaskRunStatus.FAILED);
        TaskAttempt checkAttempt = prepareAttempt(task, TaskRunStatus.CHECK);

        //prepare event
        TaskRunTransitionEvent createToQueuedEvent = createEvent(createdAttempt, TaskRunTransitionEventType.SUBMIT);
        TaskRunTransitionEvent createToBlockedEvent = createEvent(createdAttempt, TaskRunTransitionEventType.HANGUP);
        TaskRunTransitionEvent createToUpstreamFailedEvent = createEvent(createdAttempt, TaskRunTransitionEventType.UPSTREAM_FAILED);
        TaskRunTransitionEvent blockToCreateEvent = createEvent(blockedAttempt, TaskRunTransitionEventType.AWAKE);
        TaskRunTransitionEvent upstreamFailedToCreate = createEvent(upstreamFailedAttempt, TaskRunTransitionEventType.RESCHEDULE);
        TaskRunTransitionEvent queueToRunning = createEvent(queuedAttempt, TaskRunTransitionEventType.RUNNING);
        TaskRunTransitionEvent runningToFailed = createEvent(runningAttempt, TaskRunTransitionEventType.FAILED);
        TaskRunTransitionEvent runningToCheck = createEvent(runningAttempt, TaskRunTransitionEventType.CHECK);
        TaskRunTransitionEvent checkToSuccess = createEvent(checkAttempt, TaskRunTransitionEventType.CHECK_SUCCESS);
        TaskRunTransitionEvent checkToCheckFailed = createEvent(checkAttempt, TaskRunTransitionEventType.CHECK_FAILED);
        TaskRunTransitionEvent failedToCreated = createEvent(failedAttempt, TaskRunTransitionEventType.RESCHEDULE);
        TaskRunTransitionEvent createToAbort = createEvent(createdAttempt, TaskRunTransitionEventType.ABORT);
        TaskRunTransitionEvent queueToAbort = createEvent(queuedAttempt, TaskRunTransitionEventType.ABORT);
        TaskRunTransitionEvent runningToAbort = createEvent(runningAttempt, TaskRunTransitionEventType.ABORT);
        TaskRunTransitionEvent blockToAbort = createEvent(blockedAttempt, TaskRunTransitionEventType.ABORT);
        TaskRunTransitionEvent checkToAbort = createEvent(checkAttempt, TaskRunTransitionEventType.ABORT);


        return Stream.of(
                Arguments.of(createdAttempt, createToQueuedEvent, TaskRunStatus.QUEUED),
                Arguments.of(createdAttempt, createToBlockedEvent, TaskRunStatus.BLOCKED),
                Arguments.of(createdAttempt, createToUpstreamFailedEvent, TaskRunStatus.UPSTREAM_FAILED),
                Arguments.of(blockedAttempt, blockToCreateEvent, TaskRunStatus.CREATED),
                Arguments.of(upstreamFailedAttempt, upstreamFailedToCreate, TaskRunStatus.CREATED),
                Arguments.of(queuedAttempt, queueToRunning, TaskRunStatus.RUNNING),
                Arguments.of(runningAttempt, runningToCheck, TaskRunStatus.CHECK),
                Arguments.of(runningAttempt, runningToFailed, TaskRunStatus.FAILED),
                Arguments.of(checkAttempt, checkToCheckFailed, TaskRunStatus.CHECK_FAILED),
                Arguments.of(checkAttempt, checkToSuccess, TaskRunStatus.SUCCESS),
                Arguments.of(failedAttempt, failedToCreated, TaskRunStatus.CREATED),
                Arguments.of(createdAttempt, createToAbort, TaskRunStatus.ABORTED),
                Arguments.of(queuedAttempt, queueToAbort, TaskRunStatus.ABORTED),
                Arguments.of(runningAttempt, runningToAbort, TaskRunStatus.ABORTED),
                Arguments.of(blockedAttempt, blockToAbort, TaskRunStatus.ABORTED),
                Arguments.of(checkAttempt, checkToAbort, TaskRunStatus.ABORTED)
        );
    }

    @BeforeEach
    public void init() {
        taskRunStateMachine = new TaskRunStateMachine(taskRunDao, eventBus, mock(LineageService.class));
        taskRunStateMachine.start();
    }

    @ParameterizedTest
    @MethodSource("prepareData")
    public void testStateTransition(TaskAttempt taskAttempt, TaskRunTransitionEvent event, TaskRunStatus nextStatus) {
        TaskRun taskRun = taskAttempt.getTaskRun();
        Task task = taskRun.getTask();
        taskDao.create(task);
        taskRunDao.createTaskRun(taskRun);
        taskRunDao.createAttempt(taskAttempt);

        eventBus.post(event);

        TaskAttempt nextAttempt = taskRunDao.fetchAttemptById(taskAttempt.getId()).get();

        assertThat(nextAttempt.getStatus(), is(nextStatus));


    }

    private static TaskAttempt prepareAttempt(Task task, TaskRunStatus taskRunStatus) {
        TaskRun taskRun = MockTaskRunFactory.createTaskRunWithStatus(task, taskRunStatus);
        TaskAttempt taskAttempt = MockTaskAttemptFactory.createTaskAttemptWithStatus(taskRun, taskRunStatus);
        return taskAttempt;
    }

    private static TaskRunTransitionEvent createEvent(TaskAttempt taskAttempt, TaskRunTransitionEventType eventType) {
        TaskRunTransitionEvent taskRunTransitionEvent = new TaskRunTransitionEvent(eventType, taskAttempt.getId());
        return taskRunTransitionEvent;
    }
}
