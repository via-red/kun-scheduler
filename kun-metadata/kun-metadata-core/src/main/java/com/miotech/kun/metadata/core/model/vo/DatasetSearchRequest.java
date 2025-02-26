package com.miotech.kun.metadata.core.model.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DatasetSearchRequest extends PageInfo {

    private String searchContent;

    private List<String> ownerList;

    private List<String> tagList;

    private List<Long> dsTypeList;

    private List<Long> dsIdList;

    private List<String> dbList;

    private Long watermarkStart;

    private Long watermarkEnd;

    private String sortKey;

    private String sortOrder;

    private Boolean displayDeleted;

    public DatasetSearchRequest() {
    }

    @JsonCreator
    public DatasetSearchRequest(@JsonProperty("searchContent") String searchContent,
                                @JsonProperty("ownerList") List<String> ownerList,
                                @JsonProperty("tagList") List<String> tagList,
                                @JsonProperty("dsTypeList") List<Long> dsTypeList,
                                @JsonProperty("dsIdList") List<Long> dsIdList,
                                @JsonProperty("dbList") List<String> dbList,
                                @JsonProperty("watermarkStart") Long watermarkStart,
                                @JsonProperty("watermarkEnd") Long watermarkEnd,
                                @JsonProperty("sortKey") String sortKey,
                                @JsonProperty("sortOrder") String sortOrder,
                                @JsonProperty("displayDeleted") Boolean displayDeleted) {
        this.searchContent = searchContent;
        this.ownerList = ownerList;
        this.tagList = tagList;
        this.dsTypeList = dsTypeList;
        this.dsIdList = dsIdList;
        this.dbList = dbList;
        this.watermarkStart = watermarkStart;
        this.watermarkEnd = watermarkEnd;
        this.sortKey = sortKey;
        this.sortOrder = sortOrder;
        this.displayDeleted = displayDeleted;
    }

    public String getSearchContent() {
        return searchContent;
    }

    public void setSearchContent(String searchContent) {
        this.searchContent = searchContent;
    }

    public List<String> getOwnerList() {
        return ownerList;
    }

    public void setOwnerList(List<String> ownerList) {
        this.ownerList = ownerList;
    }

    public List<String> getTagList() {
        return tagList;
    }

    public void setTagList(List<String> tagList) {
        this.tagList = tagList;
    }

    public List<Long> getDsTypeList() {
        return dsTypeList;
    }

    public void setDsTypeList(List<Long> dsTypeList) {
        this.dsTypeList = dsTypeList;
    }

    public List<Long> getDsIdList() {
        return dsIdList;
    }

    public void setDsIdList(List<Long> dsIdList) {
        this.dsIdList = dsIdList;
    }

    public List<String> getDbList() {
        return dbList;
    }

    public void setDbList(List<String> dbList) {
        this.dbList = dbList;
    }

    public Long getWatermarkStart() {
        return watermarkStart;
    }

    public void setWatermarkStart(Long watermarkStart) {
        this.watermarkStart = watermarkStart;
    }

    public Long getWatermarkEnd() {
        return watermarkEnd;
    }

    public void setWatermarkEnd(Long watermarkEnd) {
        this.watermarkEnd = watermarkEnd;
    }

    public String getSortKey() {
        return sortKey;
    }

    public void setSortKey(String sortKey) {
        this.sortKey = sortKey;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getDisplayDeleted() {
        return displayDeleted;
    }

    public void setDisplayDeleted(Boolean displayDeleted) {
        this.displayDeleted = displayDeleted;
    }
}
