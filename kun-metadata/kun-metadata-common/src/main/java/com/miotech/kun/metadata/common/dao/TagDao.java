package com.miotech.kun.metadata.common.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.miotech.kun.commons.db.DatabaseOperator;
import org.apache.commons.lang.StringUtils;

import java.util.List;

@Singleton
public class TagDao {

    @Inject
    private DatabaseOperator dbOperator;

    public List<String> searchTags(String keyword) {
        String sql = "select tag from kun_mt_tag ";
        if (StringUtils.isBlank(keyword)) {
            return dbOperator.fetchAll(sql, rs -> rs.getString("tag"));
        }

        String whereClause = "where tag like '%?%'";
        sql = sql + whereClause;
        return dbOperator.fetchAll(sql, rs -> rs.getString("tag"), keyword);
    }

}
