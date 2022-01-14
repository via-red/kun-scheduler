package com.miotech.kun.metadata.core.model.vo;

public class DatasetDetail extends DatasetBasicInfo {

    private Long rowCount;

    public DatasetDetail() {
    }

    public DatasetDetail(Long rowCount) {
        this.rowCount = rowCount;
    }

    public Long getRowCount() {
        return rowCount;
    }

    public void setRowCount(Long rowCount) {
        this.rowCount = rowCount;
    }
}
