package com.miotech.kun.dataquality.web.model.bo;

import lombok.Data;

import java.util.List;

/**
 * @author: Jie Chen
 * @created: 2020/11/5
 */
@Data
public class DataQualityHistoryRequest {

    List<Long> caseIds;

    int limit = 7;
}
