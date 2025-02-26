package com.miotech.kun.workflow.common.task.service;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.miotech.kun.metadata.common.service.MetadataDatasetService;
import com.miotech.kun.metadata.facade.LineageServiceFacade;
import com.miotech.kun.metadata.facade.MetadataServiceFacade;
import com.miotech.kun.workflow.common.CommonTestBase;
import com.miotech.kun.workflow.common.exception.EntityNotFoundException;
import com.miotech.kun.workflow.common.graph.DirectTaskGraph;
import com.miotech.kun.workflow.common.lineage.service.LineageService;
import com.miotech.kun.workflow.common.operator.dao.OperatorDao;
import com.miotech.kun.workflow.common.operator.service.LineageMockOperator;
import com.miotech.kun.workflow.common.operator.service.OperatorService;
import com.miotech.kun.workflow.common.task.dao.TaskDao;
import com.miotech.kun.workflow.common.task.dependency.TaskDependencyFunctionProvider;
import com.miotech.kun.workflow.common.task.filter.TaskSearchFilter;
import com.miotech.kun.workflow.common.task.vo.PaginationVO;
import com.miotech.kun.workflow.common.task.vo.RunTaskVO;
import com.miotech.kun.workflow.common.task.vo.TaskPropsVO;
import com.miotech.kun.workflow.core.Scheduler;
import com.miotech.kun.workflow.core.model.common.Tag;
import com.miotech.kun.workflow.core.model.common.Tick;
import com.miotech.kun.workflow.core.model.lineage.node.TaskNode;
import com.miotech.kun.workflow.core.model.operator.Operator;
import com.miotech.kun.workflow.core.model.task.*;
import com.miotech.kun.workflow.testing.factory.MockOperatorFactory;
import com.miotech.kun.workflow.testing.factory.MockTaskFactory;
import com.miotech.kun.workflow.testing.operator.NopOperator;
import com.miotech.kun.workflow.testing.operator.OperatorCompiler;
import com.miotech.kun.workflow.utils.WorkflowIdGenerator;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TaskServiceTest extends CommonTestBase {
    private static final String PACKAGE_PATH_LINEAGE_OPERATOR = OperatorCompiler.compileJar(LineageMockOperator.class, "LineageMockOperator");

    @Inject
    private MetadataServiceFacade metadataFacade;

    @Inject
    private LineageService lineageService;

    @Inject
    private TaskDao taskDao;

    @Inject
    private OperatorDao operatorDao;

    @Inject
    private TaskDependencyFunctionProvider taskDependencyFunctionProvider;

    @Inject
    private OperatorService operatorService;

    private final Scheduler scheduler = mock(Scheduler.class);

    @Inject
    private TaskService taskService;

    private MetadataDatasetService metadataDatasetService;

    @BeforeEach
    public void beforeEach() {
        operatorService = spy(operatorService);
    }

    @Override
    protected void configuration() {
        super.configuration();
        metadataDatasetService = mock(MetadataDatasetService.class);
        bind(MetadataDatasetService.class, metadataDatasetService);
        bind(MetadataServiceFacade.class, MetadataDatasetService.class);
        bind(LineageServiceFacade.class, com.miotech.kun.metadata.common.service.LineageService.class);

    }

    private List<Operator> insertSampleOperators() {
        List<Operator> operators = MockOperatorFactory.createOperators(10);
        operators.forEach(operator -> {
            operatorDao.create(operator);
        });
        return operators;
    }

    private Pair<Task, List<Operator>> mockOperatorsAndCreateSingleTask() {
        List<Operator> availableOperators = insertSampleOperators();
        TaskPropsVO vo = MockTaskFactory.createTaskPropsVO().cloneBuilder()
                .withOperatorId(availableOperators.get(0).getId())
                .build();
        Task createdTask = taskService.createTask(vo);
        return Pair.of(createdTask, availableOperators);
    }

    private Pair<List<Task>, List<Operator>> mockOperatorsAndCreateMultipleTasks(int taskNum) {
        List<Operator> availableOperators = insertSampleOperators();
        List<Task> createdTasks = new ArrayList<>();
        for (int i = 0; i < taskNum; i += 1) {
            TaskPropsVO vo = MockTaskFactory.createTaskPropsVO().cloneBuilder()
                    .withOperatorId(availableOperators.get(i % availableOperators.size()).getId())
                    .build();
            Task createdTask = taskService.createTask(vo);
            createdTasks.add(createdTask);
        }
        return Pair.of(createdTasks, availableOperators);
    }

    @Test
    public void createTask_withValidVOAndOperators_shouldSuccess() {
        // Prepare
        List<Operator> preparedOperators = insertSampleOperators();

        // 1. create a valid task value object
        TaskPropsVO vo = MockTaskFactory.createTaskPropsVO().cloneBuilder()
                .withOperatorId(preparedOperators.get(0).getId())
                .build();

        // Process
        // 2. create through service
        Task createdTask = taskService.createTask(vo);

        // Validate
        // 3. task should persist
        Task persistedTask = taskService.fetchById(createdTask.getId());
        assertTrue(persistedTask.getId() > 0);
    }

    @Test
    public void createTask_withNonExistOperatorId_shouldThrowException() {
        // Prepare
        // 1. generate a non-exist operator id
        Long nonExistOperatorId = WorkflowIdGenerator.nextOperatorId();

        // 2. create a valid task value object
        TaskPropsVO vo = MockTaskFactory.createTaskPropsVO().cloneBuilder()
                .withOperatorId(nonExistOperatorId)
                .build();

        // Process & Validate
        try {
            Task createdTask = taskService.createTask(vo);
            fail();
        } catch (Exception e) {
            assertThat(e, instanceOf(EntityNotFoundException.class));
        }
    }

    @Test
    public void updateTask_withValidProps_shouldSuccess() {
        // Prepare
        // 1. create a valid task value object
        Pair<Task, List<Operator>> preparedEntities = mockOperatorsAndCreateSingleTask();
        Task createdTask = preparedEntities.getLeft();
        List<Operator> availableOperators = preparedEntities.getRight();

        // 2. produce task vo with overwritten properties
        Task taskToUpdate = createdTask.cloneBuilder()
                .withName("Updated Task Name")
                .withDescription("Lorem ipsum dolor sit amet")
                .withOperatorId(availableOperators.get(1).getId())
                .build();

        // Process
        // 3. perform full update
        taskService.fullUpdateTask(taskToUpdate);

        // Validate
        // 4. fetch updated entity and check
        Task updatedTask = taskService.fetchById(createdTask.getId());
        assertThat(updatedTask, sameBeanAs(taskToUpdate));
    }

    @Test
    public void updateTask_withQueueName_shouldSuccess() {
        // Prepare
        // 1. create a valid task value object
        Pair<Task, List<Operator>> preparedEntities = mockOperatorsAndCreateSingleTask();
        Task createdTask = preparedEntities.getLeft();
        // 2. produce task vo with overwritten properties
        Task taskToUpdate = createdTask.cloneBuilder()
                .withQueueName("user")
                .build();

        // Process
        // 3. perform full update
        taskService.fullUpdateTask(taskToUpdate);

        // Validate
        // 4. fetch updated entity and check
        Task updatedTask = taskService.fetchById(createdTask.getId());
        assertThat(updatedTask, sameBeanAs(taskToUpdate));
    }

    @Test
    public void updateTask_withInvalidProps_shouldThrowException() {
        // Prepare
        Pair<Task, List<Operator>> preparedEntities = mockOperatorsAndCreateSingleTask();
        Task createdTask = preparedEntities.getLeft();

        // Case 1: should throw exception when update with empty object
        try {
            taskService.fullUpdateTask(null);
            fail();
        } catch (Exception e) {
            assertThat(e, instanceOf(NullPointerException.class));
        }

        // Case 2: should throw exception when update with non-exist task id
        try {
            taskService.fullUpdateTask(createdTask
                    .cloneBuilder()
                    // Generate a non-exist task id
                    .withId(WorkflowIdGenerator.nextTaskId())
                    .build()
            );
            fail();
        } catch (Exception e) {
            assertThat(e, instanceOf(EntityNotFoundException.class));
        }

        // Case 3: should throw exception when update with non-exist operator id
        try {
            taskService.fullUpdateTask(createdTask
                    .cloneBuilder()
                    // Generate a non-exist operator id
                    .withOperatorId(WorkflowIdGenerator.nextOperatorId())
                    .build()
            );
            fail();
        } catch (Exception e) {
            assertThat(e, instanceOf(EntityNotFoundException.class));
        }

        // After all invalid operations, persisted task entity should remain unchanged
        Task persistedTask = taskService.fetchById(createdTask.getId());
        assertThat(persistedTask, sameBeanAs(createdTask));
    }

    @Test
    public void partialUpdateTask_withValidProps_shouldSuccess() {
        // Prepare
        // 1. create a valid task value object
        List<Operator> existOperators = insertSampleOperators();
        TaskPropsVO vo = MockTaskFactory.createTaskPropsVO().cloneBuilder()
                .withOperatorId(existOperators.get(0).getId())
                .build();
        Task createdTask = taskService.createTask(vo);

        // 2. produce a task value object with properties initialized partially
        TaskPropsVO updateVo = TaskPropsVO.newBuilder()
                .withName("Updated Task Name")
                .build();

        // Process
        // 3. update task partially
        taskService.partialUpdateTask(createdTask.getId(), updateVo);

        // Validate
        // 4. Fetch updated task
        Task updatedTask = taskService.fetchById(createdTask.getId());
        // 5. and `name` property should be updated
        assertThat(updatedTask, samePropertyValuesAs(createdTask, "config", "name"));
        assertThat(updatedTask.getConfig().size(), is(createdTask.getConfig().size()));
        // 6. and `name` property should be updated
        assertThat(updatedTask.getName(), is("Updated Task Name"));
        // 7. all properties except `name` should remain unchanged
        // TODO: improve `sameBeanAs()` to accept ignored fields
        createdTask = createdTask.cloneBuilder().withName(updatedTask.getName()).build();
        assertThat(updatedTask, sameBeanAs(createdTask));
    }

    @Test
    public void partialIUpdateTask_withInvalidProps_shouldThrowException() {
        // Prepare
        Pair<Task, List<Operator>> preparedEntities = mockOperatorsAndCreateSingleTask();
        Task createdTask = preparedEntities.getLeft();

        // Case 1: should throw exception when update with empty object
        try {
            taskService.partialUpdateTask(createdTask.getId(), null);
            fail();
        } catch (Exception e) {
            assertThat(e, instanceOf(NullPointerException.class));
        }

        // Case 2: should throw exception when update with non-exist operator id
        TaskPropsVO voWithNonExistOperatorId = TaskPropsVO.newBuilder()
                .withOperatorId(1234L)
                .build();
        try {
            taskService.partialUpdateTask(createdTask.getId(), voWithNonExistOperatorId);
            fail();
        } catch (Exception e) {
            assertThat(e, instanceOf(EntityNotFoundException.class));
        }

        // After all invalid operations, persisted task entity should remain unchanged
        Task persistedTask = taskService.fetchById(createdTask.getId());
        assertThat(persistedTask, sameBeanAs(createdTask));
    }

    @Test
    public void fetchTasksByFilters_withNameFilter_shouldReturnFilteredTasks() {
        // Prepare
        // 1. create a list of 100 tasks and persist
        Pair<List<Task>, List<Operator>> preparedEntities = mockOperatorsAndCreateMultipleTasks(100);
        List<Task> preparedTasks = preparedEntities.getLeft();
        // 2. Update 10 task entities with specific name prefix
        for (int i = 5; i < 15; i += 1) {
            Task taskToBeUpdate = preparedTasks.get(i);
            taskService.fullUpdateTask(taskToBeUpdate.cloneBuilder()
                    .withName("name_prefix_" + i)
                    .build()
            );
        }
        // 3. create a name filter
        TaskSearchFilter nameFilter = TaskSearchFilter.newBuilder()
                .withName("name_prefix")
                .withPageNum(1)
                .withPageSize(100)
                .build();

        // Process
        // 4. fetch all tasks by name filter
        PaginationVO<Task> filteredTasks = taskService.fetchTasksByFilters(nameFilter);

        // Validate
        assertEquals(10, filteredTasks.getRecords().size());
    }

    @Test
    public void fetchTasksByFilters_withTagsFilter_shouldReturnFilteredTasks() {
        // Prepare
        // 1. create a list of 100 tasks and persist
        Pair<List<Task>, List<Operator>> preparedEntities = mockOperatorsAndCreateMultipleTasks(100);
        List<Task> preparedTasks = preparedEntities.getLeft();
        // 2. Update 10 task entities with specific name prefix
        for (int i = 5; i < 15; i += 1) {
            Task taskToBeUpdate = preparedTasks.get(i);
            taskService.fullUpdateTask(taskToBeUpdate.cloneBuilder()
                    .withName("name_prefix_" + i)
                    .withTags(Lists.newArrayList(
                            new Tag("version", String.valueOf(i % 2 + 1)),
                            new Tag("priority", String.valueOf(i % 3 + 1)),
                            new Tag("owner", "foo")
                    ))
                    .build()
            );
        }
        // 3. create a tag filter
        TaskSearchFilter versionTagFilter = TaskSearchFilter.newBuilder()
                .withTags(Lists.newArrayList(
                        new Tag("version", "1")
                ))
                .withPageNum(1).withPageSize(100).build();

        TaskSearchFilter ownerTagFilter = TaskSearchFilter.newBuilder()
                .withTags(Lists.newArrayList(
                        new Tag("owner", "foo")
                ))
                .withPageNum(1).withPageSize(100).build();

        TaskSearchFilter multipleTagsFilter = TaskSearchFilter.newBuilder()
                .withTags(Lists.newArrayList(
                        new Tag("version", "2"),
                        new Tag("priority", "3"),
                        new Tag("owner", "foo")
                ))
                .withPageNum(1).withPageSize(100).build();

        TaskSearchFilter TagAndNameFilter = TaskSearchFilter.newBuilder()
                .withName("name_prefix_10")
                .withTags(Lists.newArrayList(
                        new Tag("owner", "foo")
                ))
                .withPageNum(1).withPageSize(100).build();


        // Process
        // 4. fetch all tasks by tag filter
        PaginationVO<Task> filteredTasksWithVersionTag = taskService.fetchTasksByFilters(versionTagFilter);
        PaginationVO<Task> filteredTasksWithOwnerTag = taskService.fetchTasksByFilters(ownerTagFilter);
        PaginationVO<Task> filteredTasksWithMultipleTags = taskService.fetchTasksByFilters(multipleTagsFilter);
        PaginationVO<Task> filteredTasksWithJointConditions = taskService.fetchTasksByFilters(TagAndNameFilter);

        // Validate
        assertEquals(5, filteredTasksWithVersionTag.getRecords().size());
        assertEquals(10, filteredTasksWithOwnerTag.getRecords().size());
        assertEquals(2, filteredTasksWithMultipleTags.getRecords().size());
        assertEquals(1, filteredTasksWithJointConditions.getRecords().size());
    }

    @Test
    public void fetchTasksByFilters_withInvalidPageNumOrPageSize_shouldThrowInvalidArgumentException() {
        // Prepare
        // 1. create a list of 100 tasks and persist
        mockOperatorsAndCreateMultipleTasks(100);

        // 2. create a filter with pageNum and pageSize not initialized
        TaskSearchFilter invalidFilter = TaskSearchFilter.newBuilder()
                .build();

        try {
            taskService.fetchTasksByFilters(invalidFilter);
            fail();
        } catch (Exception e) {
            // 3. should throw IllegalArgumentException
            assertThat(e, instanceOf(IllegalArgumentException.class));
        }
    }

    @Test
    public void deleteTask_withExistTask_shouldSuccess() {
        // Prepare
        Pair<Task, List<Operator>> preparedEntities = mockOperatorsAndCreateSingleTask();
        Task createdTask = preparedEntities.getLeft();

        // Process
        taskService.deleteTask(createdTask);

        // Validate
        Optional<Task> removedTask = taskDao.fetchById(createdTask.getId());
        assertFalse(removedTask.isPresent());
    }

    @Test
    public void deleteTask_withNonExistTask_shouldThrowEntityNotFoundException() {
        // Prepare
        Pair<Task, List<Operator>> preparedEntities = mockOperatorsAndCreateSingleTask();
        Task createdTask = preparedEntities.getLeft();

        // Process
        taskService.deleteTask(createdTask);

        try {
            taskService.deleteTask(createdTask);
            fail();
        } catch (Exception e) {
            // Validate
            assertThat(e, instanceOf(EntityNotFoundException.class));
        }
    }

    @Test
    public void runTask_task_does_not_exist() {
        RunTaskVO vo = new RunTaskVO();
        vo.setTaskId(1L);

        // process
        try {
            taskService.runTasks(Lists.newArrayList(vo));
            fail();
        } catch (Exception e) {
            assertThat(e, instanceOf(IllegalArgumentException.class));
        }
    }

    @Test
    public void runTask_single_task_no_variables() {
        // prepare
        Operator operator = MockOperatorFactory.createOperator();
        operatorDao.create(operator);
        Task task = MockTaskFactory.createTask(operator.getId());
        taskDao.create(task);

        RunTaskVO vo = new RunTaskVO();
        vo.setTaskId(task.getId());

        ArgumentCaptor<TaskGraph> captor1 = ArgumentCaptor.forClass(TaskGraph.class);
        ArgumentCaptor<TaskRunEnv> captor2 = ArgumentCaptor.forClass(TaskRunEnv.class);

        // process
        taskService.runTasks(Lists.newArrayList(vo));
        verify(scheduler, times(1))
                .run(captor1.capture(), captor2.capture());
        DirectTaskGraph graph = (DirectTaskGraph) captor1.getValue();
        TaskRunEnv context = captor2.getValue();

        // verify
        assertThat(graph.getTasks().get(0), sameBeanAs(task));
        assertThat(context.getConfig(task.getId()).size(), is(0));
    }

    @Test
    public void deleteTask_withOperatorResolver_shouldRemoveTaskNode() {
        // Prepare
        Operator operator = MockOperatorFactory.createOperator();
        TaskPropsVO taskVO = MockTaskFactory.createTaskPropsVO().cloneBuilder()
                .withOperatorId(operator.getId())
                .build();
        operatorDao.create(operator);

        // Process
        Task createdTask = taskService.createTask(taskVO);

        taskService.deleteTask(createdTask);
        Optional<TaskNode> taskNodeOptionalAfterDelete = lineageService.fetchTaskNodeById(createdTask.getId());

        // Validate
        assertFalse(taskNodeOptionalAfterDelete.isPresent());
    }


    @Test
    public void testCreateTaskWithSecondsConfigInCron_shouldThrowException() {
        ScheduleConf conf = ScheduleConf.newBuilder()
                .withCronExpr("*/2 0 0 * * ?")
                .withType(ScheduleType.SCHEDULED)
                .build();
        TaskPropsVO taskPropsVO = MockTaskFactory
                .createTaskPropsVO()
                .cloneBuilder()
                .withScheduleConf(conf)
                .build();
        long operatorId = taskPropsVO.getOperatorId();
        Operator op = MockOperatorFactory.createOperator()
                .cloneBuilder()
                .withId(operatorId)
                .withName("Operator_" + operatorId)
                .withClassName("NopOperator")
                .withPackagePath(OperatorCompiler.compileJar(NopOperator.class, "NopOperator"))
                .build();
        operatorDao.createWithId(op, operatorId);
        //verify
        Exception ex = assertThrows(IllegalArgumentException.class, () -> taskService.createTask(taskPropsVO));
        assertEquals("config seconds in cron is not supported yet", ex.getMessage());
    }

    @Test
    public void testFullUpdateTaskWithSecondsConfigInCron_shouldThrowException() {
        ScheduleConf conf = ScheduleConf.newBuilder()
                .withCronExpr("*/2 0 0 * * ?")
                .withType(ScheduleType.SCHEDULED)
                .build();
        TaskPropsVO taskPropsVO = MockTaskFactory.createTaskPropsVO();
        long operatorId = taskPropsVO.getOperatorId();
        Operator op = MockOperatorFactory.createOperator()
                .cloneBuilder()
                .withId(operatorId)
                .withName("Operator_" + operatorId)
                .withClassName("NopOperator")
                .withPackagePath(OperatorCompiler.compileJar(NopOperator.class, "NopOperator"))
                .build();
        operatorDao.createWithId(op, operatorId);
        Task saved = taskService.createTask(taskPropsVO);
        TaskPropsVO newVo = taskPropsVO.cloneBuilder()
                .withScheduleConf(conf)
                .build();
        //verify
        Exception ex = assertThrows(IllegalArgumentException.class, () -> taskService.fullUpdateTaskById(saved.getId(), newVo));
        assertEquals("config seconds in cron is not supported yet", ex.getMessage());
    }

    @Test
    public void testPartialUpdateTaskWithSecondsConfigInCron_shouldThrowException() {
        ScheduleConf conf = ScheduleConf.newBuilder()
                .withCronExpr("*/2 0 0 * * ?")
                .withType(ScheduleType.SCHEDULED)
                .build();
        TaskPropsVO taskPropsVO = MockTaskFactory.createTaskPropsVO();
        long operatorId = taskPropsVO.getOperatorId();
        Operator op = MockOperatorFactory.createOperator()
                .cloneBuilder()
                .withId(operatorId)
                .withName("Operator_" + operatorId)
                .withClassName("NopOperator")
                .withPackagePath(OperatorCompiler.compileJar(NopOperator.class, "NopOperator"))
                .build();
        operatorDao.createWithId(op, operatorId);
        Task saved = taskService.createTask(taskPropsVO);
        TaskPropsVO newVo = taskPropsVO.cloneBuilder()
                .withScheduleConf(conf)
                .build();
        //verify
        Exception ex = assertThrows(IllegalArgumentException.class, () -> taskService.partialUpdateTask(saved.getId(), newVo));
        assertEquals("config seconds in cron is not supported yet", ex.getMessage());
    }

    @Test
    public void testCreateTaskWithTimeZone_nextTickShouldBeUTC() {
        ScheduleConf conf = ScheduleConf.newBuilder()
                .withType(ScheduleType.SCHEDULED)
                .withCronExpr("0 0 8 * * ?")
                .withTimeZone("Asia/Shanghai")
                .build();
        TaskPropsVO taskPropsVO = MockTaskFactory.createTaskPropsVO().cloneBuilder()
                .withScheduleConf(conf)
                .build();
        long operatorId = taskPropsVO.getOperatorId();
        Operator op = MockOperatorFactory.createOperator()
                .cloneBuilder()
                .withId(operatorId)
                .withName("Operator_" + operatorId)
                .withClassName("NopOperator")
                .withPackagePath(OperatorCompiler.compileJar(NopOperator.class, "NopOperator"))
                .build();
        operatorDao.createWithId(op, operatorId);
        Task saved = taskService.createTask(taskPropsVO);
        Optional<Tick> tick = taskDao.fetchNextExecutionTickByTaskId(saved.getId());
        assertThat(tick.get().getTime().substring(8), is("0000"));
    }

    @Test
    public void testUpdateTaskWithTimeZone_nextTickShouldBeUTC() {
        ScheduleConf conf = ScheduleConf.newBuilder()
                .withType(ScheduleType.SCHEDULED)
                .withCronExpr("0 0 8 * * ?")
                .withTimeZone("Asia/Shanghai")
                .build();
        Task task = MockTaskFactory.createTask().cloneBuilder()
                .withScheduleConf(conf)
                .build();
        taskDao.create(task);
        long operatorId = task.getOperatorId();
        Operator op = MockOperatorFactory.createOperator()
                .cloneBuilder()
                .withId(operatorId)
                .withName("Operator_" + operatorId)
                .withClassName("NopOperator")
                .withPackagePath(OperatorCompiler.compileJar(NopOperator.class, "NopOperator"))
                .build();
        operatorDao.createWithId(op, operatorId);
        Task updated = taskService.fullUpdateTask(task);
        Optional<Tick> tick = taskDao.fetchNextExecutionTickByTaskId(updated.getId());
        assertThat(tick.get().getTime().substring(8), is("0000"));
    }

    @Test
    public void testCreateScheduleTaskWithoutTimeZone_nextTickShouldBeUTC() {
        ScheduleConf conf = ScheduleConf.newBuilder()
                .withType(ScheduleType.SCHEDULED)
                .withCronExpr("0 0 8 * * ?")
                .build();
        TaskPropsVO taskPropsVO = MockTaskFactory.createTaskPropsVO().cloneBuilder()
                .withScheduleConf(conf)
                .build();
        long operatorId = taskPropsVO.getOperatorId();
        Operator op = MockOperatorFactory.createOperator()
                .cloneBuilder()
                .withId(operatorId)
                .withName("Operator_" + operatorId)
                .withClassName("NopOperator")
                .withPackagePath(OperatorCompiler.compileJar(NopOperator.class, "NopOperator"))
                .build();
        operatorDao.createWithId(op, operatorId);
        Task created = taskService.createTask(taskPropsVO);
        Optional<Tick> tick = taskDao.fetchNextExecutionTickByTaskId(created.getId());
        assertThat(tick.get().getTime().substring(8), is("0800"));

    }


    @Test
    public void testUpdateScheduleTaskWithoutTimeZone_nextTickShouldBeUTC() {
        ScheduleConf conf = ScheduleConf.newBuilder()
                .withType(ScheduleType.SCHEDULED)
                .withCronExpr("0 0 8 * * ?")
                .build();
        Task task = MockTaskFactory.createTask();
        taskDao.create(task);
        task = task.cloneBuilder()
                .withScheduleConf(conf)
                .build();
        long operatorId = task.getOperatorId();
        Operator op = MockOperatorFactory.createOperator()
                .cloneBuilder()
                .withId(operatorId)
                .withName("Operator_" + operatorId)
                .withClassName("NopOperator")
                .withPackagePath(OperatorCompiler.compileJar(NopOperator.class, "NopOperator"))
                .build();
        operatorDao.createWithId(op, operatorId);
        Task updated = taskService.fullUpdateTask(task);
        Optional<Tick> tick = taskDao.fetchNextExecutionTickByTaskId(updated.getId());
        assertThat(tick.get().getTime().substring(8), is("0800"));

    }


    @Test
    public void testCreateTaskWithoutSettingValues_should_be_default() {
        //prepare
        TaskPropsVO taskPropsVO = MockTaskFactory.createTaskPropsVO();
        long operatorId = taskPropsVO.getOperatorId();
        Operator op = MockOperatorFactory.createOperator()
                .cloneBuilder()
                .withId(operatorId)
                .withName("Operator_" + operatorId)
                .withClassName("NopOperator")
                .withPackagePath(OperatorCompiler.compileJar(NopOperator.class, "NopOperator"))
                .build();
        operatorDao.createWithId(op, operatorId);

        Task task = taskService.createTask(taskPropsVO);
        Task saved = taskDao.fetchById(task.getId()).get();

        //verify
        assertThat(saved.getRetries(), is(0));
        assertThat(saved.getRetryDelay(), is(30));
        assertThat(saved.getCheckType(), is(CheckType.SKIP));
        assertThat(saved.getScheduleConf().getTimeZone(), is("Z"));
        assertThat(saved.getScheduleConf().getBlockType(), is(BlockType.NONE));
    }

    @Test
    public void testFullUpdateTaskWithoutChanges_should_be_saved() {
        //prepare
        TaskPropsVO createdTask = MockTaskFactory.createTaskPropsVO();
        long operatorId = createdTask.getOperatorId();
        Operator op = MockOperatorFactory.createOperator()
                .cloneBuilder()
                .withId(operatorId)
                .withName("Operator_" + operatorId)
                .withClassName("NopOperator")
                .withPackagePath(OperatorCompiler.compileJar(NopOperator.class, "NopOperator"))
                .build();
        operatorDao.createWithId(op, operatorId);
        Task task = taskService.createTask(createdTask);
        Task saved = taskDao.fetchById(task.getId()).get();

        TaskPropsVO taskPropsVO = TaskPropsVO.from(task);
        taskService.fullUpdateTaskById(task.getId(), taskPropsVO);
        Task updated = taskDao.fetchById(task.getId()).get();

        //verify
        assertThat(updated.getRetries(), is(saved.getRetries()));
        assertThat(updated.getRetryDelay(), is(saved.getRetryDelay()));
        assertThat(updated.getCheckType(), is(saved.getCheckType()));
        assertThat(updated.getScheduleConf().getTimeZone(), is(saved.getScheduleConf().getTimeZone()));
        assertThat(updated.getScheduleConf().getBlockType(), is(saved.getScheduleConf().getBlockType()));
    }

    @Test
    public void testPartialUpdateTaskWithoutSettingValues_should_be_saved_value() {
        //prepare
        TaskPropsVO createdTask = MockTaskFactory.createTaskPropsVO();
        long operatorId = createdTask.getOperatorId();
        Operator op = MockOperatorFactory.createOperator()
                .cloneBuilder()
                .withId(operatorId)
                .withName("Operator_" + operatorId)
                .withClassName("NopOperator")
                .withPackagePath(OperatorCompiler.compileJar(NopOperator.class, "NopOperator"))
                .build();
        operatorDao.createWithId(op, operatorId);
        Task task = taskService.createTask(createdTask);

        TaskPropsVO taskPropsVO = TaskPropsVO.from(task);
        Task saved = taskDao.fetchById(task.getId()).get();
        taskService.partialUpdateTask(task.getId(), taskPropsVO);
        Task updated = taskDao.fetchById(task.getId()).get();

        //verify
        assertThat(updated.getRetries(), is(saved.getRetries()));
        assertThat(updated.getRetryDelay(), is(saved.getRetryDelay()));
        assertThat(updated.getCheckType(), is(saved.getCheckType()));
        assertThat(updated.getScheduleConf().getTimeZone(), is(saved.getScheduleConf().getTimeZone()));
        assertThat(updated.getScheduleConf().getBlockType(), is(saved.getScheduleConf().getBlockType()));
    }

    @Test
    public void testCreateTaskWithSettingValues_should_be_given_value() {
        //prepare
        ScheduleConf scheduleConf = ScheduleConf
                .newBuilder()
                .withType(ScheduleType.NONE)
                .withTimeZone("Asia/Shanghai")
                .withBlockType(BlockType.WAIT_PREDECESSOR)
                .build();
        TaskPropsVO taskPropsVO = MockTaskFactory.createTaskPropsVO()
                .cloneBuilder()
                .withRetries(1)
                .withRetryDelay(10)
                .withCheckType(CheckType.WAIT_EVENT.name())
                .withScheduleConf(scheduleConf)
                .build();
        long operatorId = taskPropsVO.getOperatorId();
        Operator op = MockOperatorFactory.createOperator()
                .cloneBuilder()
                .withId(operatorId)
                .withName("Operator_" + operatorId)
                .withClassName("NopOperator")
                .withPackagePath(OperatorCompiler.compileJar(NopOperator.class, "NopOperator"))
                .build();
        operatorDao.createWithId(op, operatorId);

        Task task = taskService.createTask(taskPropsVO);
        Task saved = taskDao.fetchById(task.getId()).get();

        //verify
        assertThat(saved.getRetries(), is(1));
        assertThat(saved.getRetryDelay(), is(10));
        assertThat(saved.getCheckType(), is(CheckType.WAIT_EVENT));
        assertThat(saved.getScheduleConf().getType(), is(ScheduleType.NONE));
        assertThat(saved.getScheduleConf().getTimeZone(), is("Asia/Shanghai"));
        assertThat(saved.getScheduleConf().getBlockType(), is(BlockType.WAIT_PREDECESSOR));
    }

    @Test
    public void testFullUpdateTaskWithSettingValues_should_be_given_value() {
        //prepare
        TaskPropsVO createdTask = MockTaskFactory.createTaskPropsVO();
        long operatorId = createdTask.getOperatorId();
        Operator op = MockOperatorFactory.createOperator()
                .cloneBuilder()
                .withId(operatorId)
                .withName("Operator_" + operatorId)
                .withClassName("NopOperator")
                .withPackagePath(OperatorCompiler.compileJar(NopOperator.class, "NopOperator"))
                .build();
        operatorDao.createWithId(op, operatorId);
        Task task = taskService.createTask(createdTask);

        ScheduleConf scheduleConf = ScheduleConf
                .newBuilder()
                .withType(ScheduleType.NONE)
                .withTimeZone("Asia/Shanghai")
                .withBlockType(BlockType.WAIT_PREDECESSOR)
                .build();
        TaskPropsVO taskPropsVO = TaskPropsVO.from(task)
                .cloneBuilder()
                .withRetries(1)
                .withRetryDelay(10)
                .withCheckType(CheckType.WAIT_EVENT.name())
                .withScheduleConf(scheduleConf)
                .build();
        taskService.fullUpdateTaskById(task.getId(), taskPropsVO);
        Task saved = taskDao.fetchById(task.getId()).get();

        //verify
        assertThat(saved.getRetries(), is(1));
        assertThat(saved.getRetryDelay(), is(10));
        assertThat(saved.getCheckType(), is(CheckType.WAIT_EVENT));
        assertThat(saved.getScheduleConf().getType(), is(ScheduleType.NONE));
        assertThat(saved.getScheduleConf().getTimeZone(), is("Asia/Shanghai"));
        assertThat(saved.getScheduleConf().getBlockType(), is(BlockType.WAIT_PREDECESSOR));
    }

    @Test
    public void testPartialUpdateTaskWithSettingValues_should_be_given_value() {
        //prepare
        TaskPropsVO createdTask = MockTaskFactory.createTaskPropsVO();
        long operatorId = createdTask.getOperatorId();
        Operator op = MockOperatorFactory.createOperator()
                .cloneBuilder()
                .withId(operatorId)
                .withName("Operator_" + operatorId)
                .withClassName("NopOperator")
                .withPackagePath(OperatorCompiler.compileJar(NopOperator.class, "NopOperator"))
                .build();
        operatorDao.createWithId(op, operatorId);
        Task task = taskService.createTask(createdTask);

        ScheduleConf scheduleConf = ScheduleConf
                .newBuilder()
                .withType(ScheduleType.NONE)
                .withTimeZone("Asia/Shanghai")
                .withBlockType(BlockType.WAIT_PREDECESSOR)
                .build();
        TaskPropsVO taskPropsVO = TaskPropsVO.from(task)
                .cloneBuilder()
                .withRetries(1)
                .withRetryDelay(10)
                .withCheckType(CheckType.WAIT_EVENT.name())
                .withScheduleConf(scheduleConf)
                .build();
        taskService.partialUpdateTask(task.getId(), taskPropsVO);
        Task updated = taskDao.fetchById(task.getId()).get();

        //verify
        assertThat(updated.getRetries(), is(1));
        assertThat(updated.getRetryDelay(), is(10));
        assertThat(updated.getCheckType(), is(CheckType.WAIT_EVENT));
        assertThat(updated.getScheduleConf().getType(), is(ScheduleType.NONE));
        assertThat(updated.getScheduleConf().getTimeZone(), is("Asia/Shanghai"));
        assertThat(updated.getScheduleConf().getBlockType(), is(BlockType.WAIT_PREDECESSOR));
    }
}
