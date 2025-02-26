package com.miotech.kun.dataquality.web.model.entity;

import com.miotech.kun.common.model.PageInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Jie Chen
 * @created: 2020/7/16
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class DataQualityCaseBasics extends PageInfo {

    List<DataQualityCaseBasic> dqCases = new ArrayList<>();

    public void add(DataQualityCaseBasic caseBasic) {
        dqCases.add(caseBasic);
    }
}
