package com.miotech.kun.metadata.core.model.vo;

import java.util.List;

public class DatasetBasicSearch extends PageInfo {

    private List<DatasetBasicInfo> datasets;

    public DatasetBasicSearch() {
    }

    public DatasetBasicSearch(List<DatasetBasicInfo> datasets) {
        this.datasets = datasets;
    }

    public List<DatasetBasicInfo> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<DatasetBasicInfo> datasets) {
        this.datasets = datasets;
    }

}
