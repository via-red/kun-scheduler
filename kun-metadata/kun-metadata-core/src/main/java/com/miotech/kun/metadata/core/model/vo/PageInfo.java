package com.miotech.kun.metadata.core.model.vo;

public class PageInfo {

    private Integer pageNumber = 1;

    private Integer pageSize = 25;

    private Integer totalCount;

    public PageInfo() {
    }

    public PageInfo(Integer pageNumber, Integer pageSize, Integer totalCount) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }
}
