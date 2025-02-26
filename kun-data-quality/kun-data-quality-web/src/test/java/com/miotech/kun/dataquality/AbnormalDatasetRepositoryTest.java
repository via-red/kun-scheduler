package com.miotech.kun.dataquality;

import com.miotech.kun.dataquality.mock.MockAbnormalDatasetFactory;
import com.miotech.kun.dataquality.web.model.AbnormalDataset;
import com.miotech.kun.dataquality.web.persistence.AbnormalDatasetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AbnormalDatasetRepositoryTest extends DataQualityTestBase {

    @Autowired
    private AbnormalDatasetRepository abnormalDatasetRepository;

    @Test
    public void testCreate() {
        AbnormalDataset abnormalDataset = MockAbnormalDatasetFactory.create();
        abnormalDatasetRepository.create(abnormalDataset);

        List<AbnormalDataset> abnormalDatasets = abnormalDatasetRepository.fetchAll();
        assertThat(abnormalDatasets.size(), is(1));
        assertThat(abnormalDatasets.get(0), sameBeanAs(abnormalDataset).ignoring("id"));
    }

    @Test
    public void testUpdateStatus_notExist() {
        boolean result = abnormalDatasetRepository.updateStatus(1L, "SUCCESS");
        assertFalse(result);
    }

    @Test
    public void testUpdateStatus_createThenUpdate() {
        // create abnormal dataset
        AbnormalDataset abnormalDataset = MockAbnormalDatasetFactory.create();
        abnormalDatasetRepository.create(abnormalDataset);

        List<AbnormalDataset> abnormalDatasets = abnormalDatasetRepository.fetchAll();
        assertThat(abnormalDatasets.size(), is(1));
        boolean result = abnormalDatasetRepository.updateStatus(abnormalDatasets.get(0).getId(), "SUCCESS");
        assertTrue(result);
    }

    @Test
    public void testFetchByScheduleAtAndStatusIsNull() {
        // create abnormal dataset
        AbnormalDataset abnormalDataset = MockAbnormalDatasetFactory.create();
        abnormalDatasetRepository.create(abnormalDataset);

        List<AbnormalDataset> abnormalDatasets = abnormalDatasetRepository.fetchByScheduleAtAndStatusIsNull(abnormalDataset.getScheduleAt());
        assertThat(abnormalDatasets.size(), is(1));
        assertThat(abnormalDatasets.get(0), sameBeanAs(abnormalDataset).ignoring("id"));
    }

    @Test
    public void testUpdateStatusByTaskRunId() {
        // create abnormal dataset
        AbnormalDataset abnormalDataset = MockAbnormalDatasetFactory.create();
        abnormalDatasetRepository.create(abnormalDataset);

        List<AbnormalDataset> abnormalDatasets = abnormalDatasetRepository.fetchAll();
        assertThat(abnormalDatasets.size(), is(1));
        assertThat(abnormalDatasets.get(0).getStatus(), nullValue());

        // execute
        abnormalDatasetRepository.updateStatusByTaskRunId(abnormalDataset.getTaskRunId(), "SUCCESS");

        // validate
        abnormalDatasets = abnormalDatasetRepository.fetchAll();
        assertThat(abnormalDatasets.size(), is(1));
        assertThat(abnormalDatasets.get(0).getStatus(), is("SUCCESS"));
    }

}
