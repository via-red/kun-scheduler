package com.miotech.kun.workflow.common.taskrun.service;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.miotech.kun.workflow.LocalScheduler;
import com.miotech.kun.workflow.common.CommonTestBase;
import com.miotech.kun.workflow.common.task.dao.TaskDao;
import com.miotech.kun.workflow.common.taskrun.dao.TaskRunDao;
import com.miotech.kun.workflow.common.taskrun.vo.TaskRunVO;
import com.miotech.kun.workflow.core.Executor;
import com.miotech.kun.workflow.core.Scheduler;
import com.miotech.kun.workflow.core.model.task.Task;
import com.miotech.kun.workflow.core.model.taskrun.TaskRun;
import com.miotech.kun.workflow.core.model.taskrun.TaskRunStatus;
import com.miotech.kun.workflow.testing.factory.MockTaskFactory;
import com.miotech.kun.workflow.testing.factory.MockTaskRunFactory;
import com.miotech.kun.workflow.utils.DateTimeUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

public class LatestTaskRunTest extends CommonTestBase {

    @Inject
    private TaskRunService taskRunService;

    @Inject
    private TaskDao taskDao;

    @Inject
    private TaskRunDao taskRunDao;

    @Override
    protected void configuration() {
        bind(Scheduler.class, LocalScheduler.class);
        mock(Executor.class);
        super.configuration();
    }


    @ParameterizedTest
    @MethodSource("latestTaskRunsProvider")
    public void fetchLatestTaskRunsWithFilterStatus(List<TaskRunStatus> filter,Integer limit,Integer expectSize) {
        // Prepare
        Task task = MockTaskFactory.createTask();
        taskDao.create(task);

        DateTimeUtils.setClock(Clock.fixed(Instant.parse("2020-01-01T00:00:01.00Z"), ZoneId.of("UTC")));
        TaskRun taskrun1 = MockTaskRunFactory.createTaskRunWithStatus(task, TaskRunStatus.CREATED);

        DateTimeUtils.setClock(Clock.fixed(Instant.parse("2020-01-01T00:00:02.00Z"), ZoneId.of("UTC")));
        TaskRun taskrun2 = MockTaskRunFactory.createTaskRunWithStatus(task, TaskRunStatus.QUEUED);

        DateTimeUtils.setClock(Clock.fixed(Instant.parse("2020-01-01T00:00:03.00Z"), ZoneId.of("UTC")));
        TaskRun taskrun3 = MockTaskRunFactory.createTaskRunWithStatus(task, TaskRunStatus.RUNNING);

        DateTimeUtils.setClock(Clock.fixed(Instant.parse("2020-01-01T00:00:03.00Z"), ZoneId.of("UTC")));
        TaskRun taskrun4 = MockTaskRunFactory.createTaskRunWithStatus(task, TaskRunStatus.FAILED);

        DateTimeUtils.setClock(Clock.fixed(Instant.parse("2020-01-01T00:00:03.00Z"), ZoneId.of("UTC")));
        TaskRun taskrun5 = MockTaskRunFactory.createTaskRunWithStatus(task, TaskRunStatus.SUCCESS);

        DateTimeUtils.setClock(Clock.fixed(Instant.parse("2020-01-01T00:00:03.00Z"), ZoneId.of("UTC")));
        TaskRun taskrun6 = MockTaskRunFactory.createTaskRunWithStatus(task, TaskRunStatus.CHECK);

        List<TaskRun> sampleTaskRuns = Lists.newArrayList(taskrun1, taskrun2, taskrun3, taskrun4, taskrun5, taskrun6);

        taskRunDao.createTaskRuns(sampleTaskRuns);

        // Process
        List<TaskRunVO> latestTaskRuns = taskRunService.fetchLatestTaskRuns(task.getId(), filter, limit);

        assertThat(latestTaskRuns, hasSize(expectSize));

    }


    public static Stream<Arguments> latestTaskRunsProvider() {
        return Stream.of(
                Arguments.of(Arrays.asList(TaskRunStatus.FAILED, TaskRunStatus.SUCCESS, TaskRunStatus.ABORTED), 3, 2),
                Arguments.of(Arrays.asList(TaskRunStatus.FAILED, TaskRunStatus.SUCCESS, TaskRunStatus.ABORTED), 1, 1),
                Arguments.of(Arrays.asList(TaskRunStatus.FAILED, TaskRunStatus.SUCCESS, TaskRunStatus.ABORTED), 1, 1),
                Arguments.of(Arrays.asList(TaskRunStatus.ABORTED), 1, 0),
                Arguments.of(Arrays.asList(TaskRunStatus.QUEUED, TaskRunStatus.CREATED), 2, 2)
        );
    }
}
