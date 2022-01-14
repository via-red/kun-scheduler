package com.miotech.kun.metadata.core.model.vo;

import java.util.List;

public class DatasetFieldPageInfo extends PageInfo {

    private List<DatasetFieldInfo> columns;

    public List<DatasetFieldInfo> getColumns() {
        return columns;
    }

    public void setColumns(List<DatasetFieldInfo> columns) {
        this.columns = columns;
    }
}
