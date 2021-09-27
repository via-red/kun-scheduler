package com.miotech.kun.dataquality;

import com.miotech.kun.dataquality.mock.MockDataQualityFactory;
import com.miotech.kun.dataquality.model.bo.DataQualityRequest;
import com.miotech.kun.dataquality.persistence.DataQualityRepository;
import com.miotech.kun.dataquality.service.WorkflowService;
import com.miotech.kun.workflow.core.model.task.CheckType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;


public class DataQualityControllerTest extends DataQualityTestBase {


    @Autowired
    private MockMvc mvc;

    @Autowired
    private DataQualityRepository dataQualityRepository;

    @SpyBean
    private WorkflowService workflowService;

    @Test
    public void updateIsBlocking_shouldCallWorkflowApi() throws Exception {
        //prepare
        DataQualityRequest dataQualityRequest = MockDataQualityFactory.createRequest();
        Long caseId = dataQualityRepository.addCase(dataQualityRequest);
        String url = "/kun/api/v1/data-quality/" + caseId + "/edit";

        doNothing().when(workflowService).updateUpstreamTaskCheckType(anyLong(), any(CheckType.class));

        String content = "{\"name\":\"total_count_test\",\"types\":null,\"description\":\"\",\"dimension\":\"CUSTOMIZE\",\"isBlocking\":true,\"dimensionConfig\":{\"sql\":\"select count(*) as total_count from dev.demo_sales;\"},\"validateRules\":[{\"field\":\"total_count\",\"operator\":\"=\",\"expectedType\":\"NUMBER\",\"expectedValue\":\"3\"}],\"relatedTableIds\":[\"231692912319004672\"],\"primaryDatasetGid\":\"231692912319004672\"}";
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(url)
                .content(content)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        MockHttpServletResponse response = mvcResult.getResponse();

        //verify
        int status = response.getStatus();
        assertThat(status,is(200));
        verify(workflowService).updateUpstreamTaskCheckType(anyLong(),any(CheckType.class));
    }

    @Test
    public void addCaseWithIsBlocking_shouldCallWorkflowApi() throws Exception {
        //prepare
        String url = "/kun/api/v1/data-quality/add";
        doNothing().when(workflowService).updateUpstreamTaskCheckType(anyLong(), any(CheckType.class));

        String content = "{\"name\":\"total_count_test\",\"types\":null,\"description\":\"\",\"dimension\":\"CUSTOMIZE\",\"isBlocking\":true,\"dimensionConfig\":{\"sql\":\"select count(*) as total_count from dev.demo_sales;\"},\"validateRules\":[{\"field\":\"total_count\",\"operator\":\"=\",\"expectedType\":\"NUMBER\",\"expectedValue\":\"3\"}],\"relatedTableIds\":[\"231692912319004672\"],\"primaryDatasetGid\":\"231692912319004672\"}";
        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
                .andReturn();
        MockHttpServletResponse response = mvcResult.getResponse();

        //verify
        int status = response.getStatus();
        assertThat(status,is(200));
        verify(workflowService).updateUpstreamTaskCheckType(anyLong(),any(CheckType.class));
    }

}
