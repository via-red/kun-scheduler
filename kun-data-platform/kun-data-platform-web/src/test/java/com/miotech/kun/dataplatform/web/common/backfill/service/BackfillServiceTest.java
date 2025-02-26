package com.miotech.kun.dataplatform.web.common.backfill.service;

import com.google.common.collect.Maps;
import com.miotech.kun.common.model.PageResult;
import com.miotech.kun.commons.utils.IdGenerator;
import com.miotech.kun.dataplatform.AppTestBase;
import com.miotech.kun.dataplatform.facade.backfill.Backfill;
import com.miotech.kun.dataplatform.mocking.MockBackfillFactory;
import com.miotech.kun.dataplatform.web.common.backfill.dao.BackfillDao;
import com.miotech.kun.dataplatform.web.common.backfill.vo.BackfillCreateInfo;
import com.miotech.kun.dataplatform.web.common.backfill.vo.BackfillSearchParams;
import com.miotech.kun.security.testing.WithMockTestUser;
import com.miotech.kun.workflow.client.WorkflowClient;
import com.miotech.kun.workflow.client.model.RunTaskRequest;
import com.miotech.kun.workflow.client.model.Task;
import com.miotech.kun.workflow.client.model.TaskRun;
import com.miotech.kun.workflow.core.execution.Config;
import lombok.Data;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static com.miotech.kun.dataplatform.web.constant.BackfillConstants.MAX_BACKFILL_TASKS;
import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.*;

@WithMockTestUser
public class BackfillServiceTest extends AppTestBase {
    @Autowired
    private BackfillService backfillService;

    @Autowired
    private BackfillDao backfillDao;

    @Autowired
    private WorkflowClient workflowClient;

    @Test
    public void fetchById_withExistingBackfill_shouldWork() {
        // 1. Prepare
        Backfill backfillCreated = MockBackfillFactory.createBackfill();
        backfillDao.create(backfillCreated);

        // 2. Process
        Optional<Backfill> backfillOptional = backfillService.fetchById(backfillCreated.getId());

        // 3. Validate
        assertTrue(backfillOptional.isPresent());
        assertThat(backfillOptional.get(), sameBeanAs(backfillCreated));
    }

    @Test
    public void fetchById_withNonExistingBackfill_shouldReturnNonPresentingOptional() {
        // 1. Prepare
        // 2. Process
        Optional<Backfill> backfillOptional = backfillService.fetchById(12345L);

        // 3. Validate
        assertFalse(backfillOptional.isPresent());
    }

    @Test
    public void search_withNameAsKeyword_shouldFilterProperly() {
        // 1. Prepare
        List<Backfill> backfills = MockBackfillFactory.createBackfill(100);
        for (Backfill backfill : backfills) {
            backfillDao.create(backfill);
        }
        BackfillSearchParams searchParams = new BackfillSearchParams();
        searchParams.setPageNumber(1);
        searchParams.setPageSize(100);
        searchParams.setName("example-backfill-1");

        // 2. Process
        PageResult<Backfill> resultPage = backfillService.search(searchParams);

        // 3. validate
        // 1, 10, 11, ..., 19, 100
        assertThat(resultPage.getTotalCount(), is(12));
        assertThat(resultPage.getRecords().size(), is(12));
    }

    @Test
    public void createAndRunBackfill_withProperContext_shouldWorkAsExpected() {
        // 1. Prepare mock behaviors
        MockDefinedContext predefinedContext = mockWorkflowClientBehavior();

        // 2. Process
        BackfillCreateInfo createInfo = new BackfillCreateInfo(
                "test-service-backfill",
                Lists.newArrayList(101L),   // workflow task id
                Lists.newArrayList(1L)      // definition id
        );
        Backfill persistedBackfill = backfillService.createAndRun(createInfo);

        // 3. Validate
        assertThat(persistedBackfill.getTaskRunIds(), is(Lists.newArrayList(predefinedContext.getMockTaskRun().getId())));
    }

    @Test
    public void createAndRunBackfill_withTooManyTasks_shouldThrowIllegalArgumentException() {
        // 1. Prepare mock behaviors
        mockWorkflowClientBehavior();
        List<Long> bunchOfWorkflowTaskIds = LongStream.range(101L, 101L + MAX_BACKFILL_TASKS + 1).boxed().collect(Collectors.toList());
        List<Long> bunchOfTaskDefinitionIds = LongStream.range(1L, 1L + MAX_BACKFILL_TASKS + 1).boxed().collect(Collectors.toList());

        // 2. Process
        BackfillCreateInfo createInfo = new BackfillCreateInfo(
                "test-service-backfill",
                Lists.newArrayList(bunchOfWorkflowTaskIds),   // workflow task id
                Lists.newArrayList(bunchOfTaskDefinitionIds)  // definition id
        );
        try {
            backfillService.createAndRun(createInfo);
            fail();
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
        }
    }

    private MockDefinedContext mockWorkflowClientBehavior() {
        RunTaskRequest expectedRunRequest = new RunTaskRequest();
        expectedRunRequest.addTaskConfig(101L, Maps.newHashMap());

        // mock execute tasks
        TaskRun mockTaskRun = TaskRun.newBuilder()
                .withId(1001L)
                .withTask(Task.newBuilder().withId(101L).build())
                .withConfig(Config.EMPTY)
                .build();
        Mockito.doAnswer((invocation) -> {
            RunTaskRequest request = invocation.getArgument(0, RunTaskRequest.class);
            Long taskId = request.getRunTasks().get(0).getTaskId();
            Map<Long, TaskRun> taskRunMap = new HashMap<>();
            if (taskId.equals(101L)) {
                taskRunMap.put(taskId, mockTaskRun);
            }
            return taskRunMap;
        }).when(workflowClient).executeTasks(Mockito.any(RunTaskRequest.class));

        // mock fetch by id
        Mockito.doAnswer(invocation -> mockTaskRun)
                .when(workflowClient)
                .getTaskRun(Mockito.eq(mockTaskRun.getId()));

        return new MockDefinedContext(expectedRunRequest, mockTaskRun);
    }

    @Test
    public void fetchTaskRunsByBackfillId_withExistingBackfill_shouldWork() {
        // 1. Prepare mock behaviors
        MockDefinedContext predefinedContext = mockWorkflowClientBehavior();

        // 2. Process
        BackfillCreateInfo createInfo = new BackfillCreateInfo(
                "test-service-backfill",
                Lists.newArrayList(101L),   // workflow task id
                Lists.newArrayList(1L)      // definition id
        );
        Backfill persistedBackfill = backfillService.createAndRun(createInfo);
        List<TaskRun> taskRunList = backfillService.fetchTaskRunsByBackfillId(persistedBackfill.getId());

        // 3. Validate
        assertThat(taskRunList, sameBeanAs(Lists.newArrayList(predefinedContext.mockTaskRun)));
    }

    @Test
    public void fetchTaskRunsByBackfillId_withNonExistingBackfill_shouldThrowIllegalArgumentException() {
        try {
            backfillService.fetchTaskRunsByBackfillId(12345L);
            fail();
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
        }
    }

    @Test
    public void testCreate() {
        Long taskRunId = IdGenerator.getInstance().nextId();
        Backfill backfill = MockBackfillFactory.createBackfill(taskRunId);
        Optional<Long> backfillIdOpt = backfillService.findDerivedFromBackfill(taskRunId);
        assertFalse(backfillIdOpt.isPresent());

        backfillService.create(backfill);
        backfillIdOpt = backfillService.findDerivedFromBackfill(taskRunId);
        assertTrue(backfillIdOpt.isPresent());
    }

    @Data
    private static class MockDefinedContext {
        private final RunTaskRequest expectedRunRequest;
        private final TaskRun mockTaskRun;
    }
}