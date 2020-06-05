import React, { useCallback, useMemo, useEffect, useState } from 'react';
import { Link } from 'umi';
import { RematchDispatch } from '@rematch/core';
import _ from 'lodash';
import { Input, Select, Table, Tag, Spin } from 'antd';
import { ColumnProps } from 'antd/es/table';
import { PaginationProps } from 'antd/es/pagination';
import { useDispatch, useSelector, shallowEqual } from 'react-redux';
import { RootDispatch, RootState } from '@/rematch/store';
import { Mode, Dataset, Watermark } from '@/rematch/models/dataDiscovery';

import color from '@/styles/color';

import useI18n from '@/hooks/useI18n';

import Card from '@/components/Card/Card';
import { RootModel, DbType } from '@/rematch/models';
import { watermarkFormatter } from '@/utils';
import TimeSelect from './components/TimeSelect/TimeSelect';

import styles from './index.less';

const { Search } = Input;
const { Option } = Select;

export default function DataDisvocery() {
  const t = useI18n();

  const dispatch = useDispatch<RootDispatch>();
  const {
    searchContent,
    wartermarkMode,
    wartermarkAbsoluteValue,
    wartermarkQuickeValue,

    dbTypeList,
    ownerList,
    tagList,
    allOwnerList,
    allTagList,

    datasetList,

    pagination,
  } = useSelector(
    (state: RootState) => ({
      searchContent: state.dataDiscovery.searchContent,
      wartermarkMode: state.dataDiscovery.wartermarkMode,
      wartermarkAbsoluteValue: state.dataDiscovery.wartermarkAbsoluteValue,
      wartermarkQuickeValue: state.dataDiscovery.wartermarkQuickeValue,

      dbTypeList: state.dataDiscovery.dbTypeList,
      ownerList: state.dataDiscovery.ownerList,
      tagList: state.dataDiscovery.tagList,
      allOwnerList: state.dataDiscovery.allOwnerList,
      allTagList: state.dataDiscovery.allTagList,

      datasetList: state.dataDiscovery.datasetList,
      pagination: state.dataDiscovery.pagination,
    }),
    shallowEqual,
  );

  useEffect(() => {
    dispatch.dataDiscovery.fetchAllOwnerList();
    dispatch.dataDiscovery.fetchAllTagList();
  }, [dispatch.dataDiscovery]);

  const [dataListFetchLoading, setDataListFetchLoading] = useState(false);

  const searchFunc = useMemo(
    () =>
      _.debounce(async (theDispatch: RematchDispatch<RootModel>) => {
        setDataListFetchLoading(true);
        theDispatch.dataDiscovery.searchDatasets().then(() => {
          setDataListFetchLoading(false);
        });
      }, 500),
    [],
  );

  const handleSearch = useCallback(() => {
    searchFunc(dispatch);
  }, [dispatch, searchFunc]);

  useEffect(() => {
    handleSearch();
  }, [
    handleSearch,
    wartermarkMode,
    wartermarkAbsoluteValue,
    wartermarkQuickeValue,
    dbTypeList,
    ownerList,
    tagList,
    pagination.pageSize,
    pagination.pageNumber,
  ]);

  const handleChangeSearch = useCallback(
    e => {
      dispatch.dataDiscovery.updateState({
        key: 'searchContent',
        value: e.target.value,
      });
    },
    [dispatch],
  );

  const handleChangeWatermarkMode = useCallback(
    mode => {
      dispatch.dataDiscovery.updateState({
        key: 'wartermarkMode',
        value: mode,
      });
    },
    [dispatch],
  );

  const timeSelectValue = useMemo(
    () =>
      wartermarkMode === Mode.ABSOLUTE
        ? wartermarkAbsoluteValue
        : wartermarkQuickeValue,
    [wartermarkMode, wartermarkAbsoluteValue, wartermarkQuickeValue],
  );

  const handleChangeWatermarkValue = useCallback(
    (v, mode) => {
      if (mode === Mode.ABSOLUTE) {
        dispatch.dataDiscovery.updateState({
          key: 'wartermarkAbsoluteValue',
          value: v,
        });
      }
      if (mode === Mode.QUICK) {
        dispatch.dataDiscovery.updateState({
          key: 'wartermarkQuickeValue',
          value: v,
        });
      }
    },
    [dispatch],
  );

  const columns: ColumnProps<Dataset>[] = useMemo(
    () =>
      [
        {
          title: t('dataDiscovery.datasetsTable.header.name'),
          dataIndex: 'name',
          key: 'name',
          width: 170,
          render: (name: string, record: Dataset) => (
            <Link
              className={styles.nameLink}
              to={`/data-discovery/${record.id}`}
            >
              {name}
            </Link>
          ),
        },
        {
          title: t('dataDiscovery.datasetsTable.header.schema'),
          dataIndex: 'schema',
          key: 'schema',
          width: 80,
        },
        {
          title: t('dataDiscovery.datasetsTable.header.dbtype'),
          dataIndex: 'type',
          key: 'type',
          width: 80,
        },
        {
          title: t('dataDiscovery.datasetsTable.header.description'),
          dataIndex: 'description',
          key: 'description',
          width: 200,
        },
        {
          title: t('dataDiscovery.datasetsTable.header.owners'),
          dataIndex: 'owners',
          key: 'owners',
          width: 300,
          render: (owners: string[]) => (
            <>
              {(owners || []).map(owner => (
                <Tag
                  key={owner}
                  className="light-blue-tag"
                  color={color.lightBlue}
                >
                  {owner}
                </Tag>
              ))}
            </>
          ),
        },
        {
          title: t('dataDiscovery.datasetsTable.header.watermark'),
          dataIndex: 'high_watermark',
          key: 'high_watermark',
          width: 150,
          render: (watermark: Watermark) => watermarkFormatter(watermark.time),
        },

        {
          title: t('dataDiscovery.datasetsTable.header.tags'),
          dataIndex: 'tags',
          key: 'tags',
          width: 300,
          render: (tags: string[]) => (
            <>
              {(tags || []).map(tag => (
                <Tag
                  key={tag}
                  className="light-blue-tag"
                  color={color.lightBlue}
                >
                  {tag}
                </Tag>
              ))}
            </>
          ),
        },
      ] as ColumnProps<Dataset>[],
    [t],
  );

  const scroll = useMemo(
    () => ({
      x: 2000,
    }),
    [],
  );

  const handleChangePage = useCallback(
    (pageNumber, pageSize) => {
      dispatch.dataDiscovery.updateState({
        key: 'pagination',
        value: {
          ...pagination,
          pageNumber,
          pageSize,
        },
      });
    },
    [dispatch.dataDiscovery, pagination],
  );

  const handleChangePageSize = useCallback(
    (_pageNumber, pageSize) => {
      dispatch.dataDiscovery.updateState({
        key: 'pagination',
        value: {
          ...pagination,
          pageNumber: 1,
          pageSize,
        },
      });
    },
    [dispatch.dataDiscovery, pagination],
  );

  const tablePagination: PaginationProps = useMemo(
    () => ({
      size: 'small',
      total: pagination.totalCount,
      current: pagination.pageNumber,
      pageSize: pagination.pageSize,
      pageSizeOptions: ['15', '25', '50', '100'],
      onChange: handleChangePage,
      onShowSizeChange: handleChangePageSize,
    }),
    [
      handleChangePage,
      handleChangePageSize,
      pagination.pageNumber,
      pagination.pageSize,
      pagination.totalCount,
    ],
  );

  return (
    <div className={styles.page}>
      <Card className={styles.content}>
        <div className={styles.searchArea}>
          <Search
            size="large"
            placeholder={t('dataDiscovery.searchContent')}
            onSearch={handleSearch}
            onChange={handleChangeSearch}
            value={searchContent}
            style={{ width: '70%' }}
          />
        </div>
        <div className={styles.tableArea}>
          <div className={styles.filterRow}>
            <div className={styles.filterItem}>
              <div className={styles.filterItemTitle}>
                {t('dataDiscovery.waterMark')}
              </div>
              <div>
                <TimeSelect
                  mode={wartermarkMode}
                  onModeChange={handleChangeWatermarkMode}
                  value={timeSelectValue}
                  onChange={handleChangeWatermarkValue}
                />
              </div>
            </div>

            <div className={styles.filterItem}>
              <div className={styles.filterItemTitle}>
                {t('dataDiscovery.dbtype')}
              </div>
              <div className={styles.filterItemSelect}>
                <Select
                  value={dbTypeList}
                  mode="multiple"
                  size="large"
                  onChange={v => {
                    dispatch.dataDiscovery.updateState({
                      key: 'dbTypeList',
                      value: v,
                    });
                  }}
                  placeholder={t('dataDiscovery.pleaseSelect')}
                  allowClear
                >
                  {Object.values(DbType).map(option => (
                    <Option key={option} value={option}>
                      {option}
                    </Option>
                  ))}
                </Select>
              </div>
            </div>

            <div className={styles.filterItem}>
              <div className={styles.filterItemTitle}>
                {t('dataDiscovery.owners')}
              </div>
              <div className={styles.filterItemSelect}>
                <Select
                  value={ownerList}
                  mode="multiple"
                  size="large"
                  onChange={v => {
                    dispatch.dataDiscovery.updateState({
                      key: 'ownerList',
                      value: v,
                    });
                  }}
                  placeholder={t('dataDiscovery.pleaseSelect')}
                  allowClear
                >
                  {allOwnerList.map(option => (
                    <Option key={option} value={option}>
                      {option}
                    </Option>
                  ))}
                </Select>
              </div>
            </div>

            <div className={styles.filterItem}>
              <div className={styles.filterItemTitle}>
                {t('dataDiscovery.tags')}
              </div>
              <div className={styles.filterItemSelect}>
                <Select
                  value={tagList}
                  mode="multiple"
                  size="large"
                  onChange={v => {
                    dispatch.dataDiscovery.updateState({
                      key: 'tagList',
                      value: v,
                    });
                  }}
                  placeholder={t('dataDiscovery.pleaseSelect')}
                  allowClear
                >
                  {allTagList.map(option => (
                    <Option key={option} value={option}>
                      {option}
                    </Option>
                  ))}
                </Select>
              </div>
            </div>
          </div>

          <div className={styles.resultRow}>
            {t('dataDiscovery.datasetsTable.resultCount', {
              count: pagination.totalCount ?? 0,
            })}
          </div>

          <div className={styles.table}>
            <Spin spinning={dataListFetchLoading}>
              <Table
                rowKey="id"
                columns={columns}
                dataSource={datasetList}
                size="small"
                scroll={scroll}
                pagination={tablePagination}
              />
            </Spin>
          </div>
        </div>
      </Card>
    </div>
  );
}
