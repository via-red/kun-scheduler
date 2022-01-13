package com.miotech.kun.metadata.common.dao;

import com.google.inject.Inject;
import com.miotech.kun.commons.testing.DatabaseTestBase;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TagDaoTest extends DatabaseTestBase {

    @Inject
    private TagDao tagDao;

    @Test
    public void testSearchTags_empty() {
        List<String> tags =
                tagDao.searchTags(null);
        assertThat(tags.size(), is(0));
    }

}
