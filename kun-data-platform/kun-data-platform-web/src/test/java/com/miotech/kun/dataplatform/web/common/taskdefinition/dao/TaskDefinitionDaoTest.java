package com.miotech.kun.dataplatform.web.common.taskdefinition.dao;

import com.miotech.kun.dataplatform.AppTestBase;
import com.miotech.kun.dataplatform.facade.model.taskdefinition.TaskDefinition;
import com.miotech.kun.dataplatform.mocking.MockTaskDefinitionFactory;
import com.miotech.kun.dataplatform.web.common.taskdefinition.vo.TaskDefinitionSearchRequest;
import com.miotech.kun.workflow.client.model.PaginationResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static com.shazam.shazamcrest.MatcherAssert.assertThat;
import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaskDefinitionDaoTest extends AppTestBase {

    @Autowired
    private TaskDefinitionDao taskDefinitionDao;

    @Test
    public void testCreate_TaskDefinition_ok() {
        TaskDefinition taskDefinition = MockTaskDefinitionFactory.createTaskDefinition();
        taskDefinitionDao.create(taskDefinition);

        TaskDefinition fetched = taskDefinitionDao.fetchById(taskDefinition.getDefinitionId()).get();
        assertThat(fetched, sameBeanAs(taskDefinition));
    }

    @Test
    public void test_fetchById_notfound() {
        Optional<TaskDefinition> fetched = taskDefinitionDao.fetchById(1L);
        assertFalse(fetched.isPresent());
    }

    @Test
    public void test_fetchByIds_empty() {
        List<TaskDefinition> fetched;
        fetched = taskDefinitionDao.fetchByIds(Collections.emptyList());
        assertTrue(fetched.isEmpty());

        try {
            fetched = taskDefinitionDao.fetchByIds(null);
        } catch (Throwable e) {
            assertThat(e.getClass(), is(NullPointerException.class));
        }
    }

    @Test
    public void test_fetchByIds_ok() {
        TaskDefinition taskDefinition = MockTaskDefinitionFactory.createTaskDefinition();
        taskDefinitionDao.create(taskDefinition);

        List<TaskDefinition> fetched = taskDefinitionDao.fetchByIds(Collections.singletonList(taskDefinition.getDefinitionId()));
        assertThat(fetched.get(0), sameBeanAs(taskDefinition));
    }

    @Test
    public void fetchAll() {
        TaskDefinition taskDefinition = MockTaskDefinitionFactory.createTaskDefinition();
        taskDefinitionDao.create(taskDefinition);

        List<TaskDefinition> fetched = taskDefinitionDao.fetchAll();
        assertThat(fetched.get(0), sameBeanAs(taskDefinition));
    }

    @Test
    public void test_fetchName_notFound() {
        List<TaskDefinition> fetched = taskDefinitionDao.fetchAliveTaskDefinitionByName("test");
        assertTrue(fetched.isEmpty());
    }

    @Test
    public void test_fetchNameWithSingleAlive_ok() {
        TaskDefinition taskDefinition = MockTaskDefinitionFactory.createTaskDefinition();
        taskDefinitionDao.create(taskDefinition);

        Optional<TaskDefinition> fetched = taskDefinitionDao.fetchAliveTaskDefinitionByName(taskDefinition.getName()).stream()
                .findAny();
        assertThat(fetched.get(), sameBeanAs(taskDefinition));
    }

    @Test
    public void test_fetchNameWithSingleArchived_notFound() {
        TaskDefinition taskDefinition = MockTaskDefinitionFactory.createTaskDefinition();
        taskDefinitionDao.create(taskDefinition);
        taskDefinitionDao.archive(taskDefinition.getDefinitionId());
        List<TaskDefinition> fetched = taskDefinitionDao.fetchAliveTaskDefinitionByName(taskDefinition.getName());
        assertTrue(fetched.isEmpty());
    }

    @Test
    public void test_fetchNameWithMultipleArchived_notFound() {
        Random rand = new Random();
        int times = rand.nextInt(5) + 5;
        for (int time = 1; time <= times; time++) {
            TaskDefinition taskDefinitionTemp = MockTaskDefinitionFactory.createTaskDefinition();
            taskDefinitionTemp = taskDefinitionTemp.cloneBuilder()
                    .withName("test")
                    .build();
            taskDefinitionDao.archive(taskDefinitionTemp.getDefinitionId());
        }
        List<TaskDefinition> fetched = taskDefinitionDao.fetchAliveTaskDefinitionByName("test");
        assertTrue(fetched.isEmpty());
    }

    @Test
    public void test_fetchNameWithSingleAliveAndMultipleArchived_ok() {
        Random rand = new Random();
        int times = rand.nextInt(5) + 5;
        for (int time = 1; time <= times; time++) {
            TaskDefinition taskDefinitionTemp = MockTaskDefinitionFactory.createTaskDefinition();
            taskDefinitionTemp = taskDefinitionTemp.cloneBuilder()
                    .withName("test")
                    .build();
            taskDefinitionDao.create(taskDefinitionTemp);
            if (time == times) break;
            taskDefinitionDao.archive(taskDefinitionTemp.getDefinitionId());
        }
        Optional<TaskDefinition> fetched = taskDefinitionDao.fetchAliveTaskDefinitionByName("test").stream()
                .findAny();
        assertTrue(fetched.isPresent());
        assertTrue(fetched.get().getName().equals("test"));
    }



    @Test
    public void search_withArchived() {
        List<TaskDefinition> taskDefinitions = MockTaskDefinitionFactory.createTaskDefinitions(100);
        taskDefinitions.forEach(x -> taskDefinitionDao.create(x));

        TaskDefinitionSearchRequest request;
        PaginationResult<TaskDefinition> taskDefPage;

        // filter with not archived
        request = new TaskDefinitionSearchRequest(10, 1,
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Optional.of(false),
                Collections.emptyList()
        );
        taskDefPage = taskDefinitionDao.search(request);
        assertThat(taskDefPage.getTotalCount(), is(taskDefinitions.size()));
        assertThat(taskDefPage.getPageSize(), is(10));
        assertThat(taskDefPage.getPageNum(), is(1));

        // filter with archived
        TaskDefinition taskDefinition = MockTaskDefinitionFactory.createTaskDefinition()
                .cloneBuilder()
                .withArchived(true)
                .build();
        taskDefinitionDao.create(taskDefinition);
        request = new TaskDefinitionSearchRequest(10, 1,
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Optional.of(true),
                Collections.emptyList()
        );
        taskDefPage = taskDefinitionDao.search(request);
        assertThat(taskDefPage.getTotalCount(), is(1));
        assertThat(taskDefPage.getPageSize(), is(10));
        assertThat(taskDefPage.getPageNum(), is(1));
        assertThat(taskDefPage.getRecords().get(0), sameBeanAs(taskDefinition));
    }

    @Test
    public void search() {
        List<TaskDefinition> taskDefinitions = MockTaskDefinitionFactory.createTaskDefinitions(100);
        taskDefinitions.forEach(x -> taskDefinitionDao.create(x));
        TaskDefinition taskDefinition = taskDefinitions.get(0);
        // filter nothing
        TaskDefinitionSearchRequest request;
        PaginationResult<TaskDefinition> taskDefPage;

        request = new TaskDefinitionSearchRequest(10, 1,
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList(),
                Optional.empty(),
                Collections.emptyList()
        );
        taskDefPage = taskDefinitionDao.search(request);
        assertThat(taskDefPage.getTotalCount(), is(taskDefinitions.size()));
        assertThat(taskDefPage.getPageSize(), is(10));
        assertThat(taskDefPage.getPageNum(), is(1));

        // filter with task template name
        request = new TaskDefinitionSearchRequest(10, 1,
                null,
                taskDefinition.getTaskTemplateName(),
                Collections.emptyList(),
                Collections.emptyList(),
                Optional.empty(),
                Collections.emptyList()
        );
        taskDefPage = taskDefinitionDao.search(request);
        assertThat(taskDefPage.getTotalCount(), is(taskDefinitions.size()));
        assertThat(taskDefPage.getPageSize(), is(10));
        assertThat(taskDefPage.getPageNum(), is(1));
        assertThat(taskDefPage.getRecords().get(0), sameBeanAs(taskDefinition));

        // filter with name and task template name
        request = new TaskDefinitionSearchRequest(10, 1,
                taskDefinition.getName(),
                taskDefinition.getTaskTemplateName(),
                Collections.emptyList(),
                Collections.emptyList(),
                Optional.empty(),
                Collections.emptyList()
        );
        taskDefPage = taskDefinitionDao.search(request);
        assertThat(taskDefPage.getTotalCount(), is(1));
        assertThat(taskDefPage.getPageSize(), is(10));
        assertThat(taskDefPage.getPageNum(), is(1));
        assertThat(taskDefPage.getRecords().get(0), sameBeanAs(taskDefinition));

        // filter with definition id
        request = new TaskDefinitionSearchRequest(10, 1,
                taskDefinition.getName(),
                taskDefinition.getTaskTemplateName(),
                Collections.singletonList(taskDefinition.getDefinitionId()),
                Collections.singletonList(taskDefinition.getCreator()),
                Optional.empty(),
                Collections.emptyList()
        );
        taskDefPage = taskDefinitionDao.search(request);
        assertThat(taskDefPage.getTotalCount(), is(1));
        assertThat(taskDefPage.getRecords().get(0), sameBeanAs(taskDefinition));
    }

    @Test
    public void archive() {
        TaskDefinition taskDefinition = MockTaskDefinitionFactory.createTaskDefinition();
        taskDefinitionDao.create(taskDefinition);

        taskDefinitionDao.archive(taskDefinition.getDefinitionId());
        TaskDefinition fetched = taskDefinitionDao.fetchById(taskDefinition.getDefinitionId()).get();
        assertTrue(fetched.isArchived());
    }
}