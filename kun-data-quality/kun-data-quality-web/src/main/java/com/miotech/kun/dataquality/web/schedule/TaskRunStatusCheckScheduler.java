package com.miotech.kun.dataquality.web.schedule;

import com.miotech.kun.commons.utils.DateTimeUtils;
import com.miotech.kun.dataquality.web.model.AbnormalDataset;
import com.miotech.kun.dataquality.web.service.AbnormalDatasetService;
import com.miotech.kun.workflow.client.WorkflowClient;
import com.miotech.kun.workflow.client.model.TaskRunState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class TaskRunStatusCheckScheduler {

    @Autowired
    private AbnormalDatasetService abnormalDatasetService;

    @Autowired
    private WorkflowClient workflowClient;

    @Scheduled(cron = "${data-quality.daily.start:0 0 0 * * ?}")
    public void execute() {
        String now = DateTimeUtils.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        List<AbnormalDataset> abnormalDatasets = abnormalDatasetService.fetchByScheduleAtAndStatusIsNull(now);
        for (AbnormalDataset abnormalDataset : abnormalDatasets) {
            TaskRunState taskRunState = workflowClient.getTaskRunState(abnormalDataset.getTaskRunId());
            abnormalDatasetService.updateStatus(abnormalDataset.getId(), taskRunState.getStatus().isSuccess() ? "SUCCESS" : "FAILED");
        }
    }

}
