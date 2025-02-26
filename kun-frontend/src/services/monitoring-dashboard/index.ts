import { get } from '@/utils/requestUtils';
import { PaginationReqBody, PaginationRespBodyBase, SortReqBody } from '@/definitions/common-types';

// Get Metadata Metrics
// GET /kun/api/v1/dashboard/metadata/metrics

export interface MetadataMetrics {
  dataQualityCoveredCount: number;
  dataQualityLongExistingFailedCount: number;
  dataQualityPassCount: number;
  totalCaseCount: number;
  totalDatasetCount: number;
}

export function fetchMetadataMetrics() {
  return get<MetadataMetrics>('/dashboard/metadata/metrics', {
    mockCode: 'monitoring-dashboard.get-metadata-metrics',
  });
}

// Get Max Row Count Change
// GET /kun/api/v1/dashboard/metadata/max-row-count-change

export interface RowCountChange {
  datasetName: string;
  database: string;
  dataSource: string;
  rowChange: number;
  rowCount: number;
  rowChangeRatio: number; // 0 <= rowChangeRatio <= 1.0
}

export interface MaxRowCountChange {
  rowCountChanges: RowCountChange[];
}

export interface FetchMaxRowCountChangeReqParams {
  pageSize?: number;
}

export function fetchMaxRowCountChange(reqParams: FetchMaxRowCountChangeReqParams = {}) {
  return get<MaxRowCountChange>('/dashboard/metadata/max-row-count-change', {
    query: { ...reqParams },
    mockCode: 'monitoring-dashboard.get-max-row-count-change',
  });
}

// Get Failed Test Cases
// GET /kun/api/v1/dashboard/test-cases

export interface FailedTestCaseRule {
  originalValue: string;
  field: string;
  operator: string;
  expectedType: string;
  expectedValue: string;
}

export interface FailedTestCase {
  status: string;
  errorReason: string;
  updateTime: number;
  taskRunId: string;
  // default sort column
  continuousFailingCount: number;
  caseOwner: string;
  datasetGid: string;
  datasetName: string;
  caseName: string;
  caseId: string;
  ruleRecords: FailedTestCaseRule[];
}

export interface FailedTask {
  taskId: string;
  taskName: string;
  taskRunId: string;
  updateTime: string;
}

export interface Glossary {
  id: string;
  name: string;
}
export interface AbnormalDataset {
  datasetGid: string;
  datasetName: string;
  databaseName: string;
  datasourceName: string;
  failedCaseCount: number;
  lastUpdatedTime: string;
  glossaries: Glossary[];
  tasks: FailedTask[];
  cases: FailedTestCase[];
}

export interface AbnormalDatasetDatasetInfo extends PaginationRespBodyBase {
  abnormalDatasets: AbnormalDataset[];
}

export type FetchFailedTestCasesReqParams = Partial<PaginationReqBody> & Partial<SortReqBody<keyof AbnormalDataset>>;

export function fetchFailedTestCases(reqParams: FetchFailedTestCasesReqParams = {}) {
  return get<AbnormalDatasetDatasetInfo>('/dashboard/test-cases', {
    query: {
      ...reqParams,
    },
    mockCode: 'monitoring-dashboard.get-failed-test-cases',
  });
}

// Get Dataset Column Metrics
// GET /kun/api/v1/dashboard/metadata/column/metrics

export interface ColumnMetrics {
  datasetName: string;
  columnName: string;
  columnNullCount: number;
  columnDistinctCount: number;
  totalRowCount: number;
}

export interface DatasetColumnMetricsInfo extends PaginationRespBodyBase {
  columnMetricsList: ColumnMetrics[];
}

export type FetchDatasetColumnMetricsReqParams = Partial<PaginationReqBody> & Partial<SortReqBody<keyof ColumnMetrics>>;

export function fetchDatasetColumnMetrics(reqParams: FetchDatasetColumnMetricsReqParams = {}) {
  return get<DatasetColumnMetricsInfo>('/dashboard/metadata/column/metrics', {
    query: reqParams,
  });
}

// Get Data Development Metrics
// GET /kun/api/v1/dashboard/data-development/metrics

export interface DataDevelopmentMetrics {
  successTaskCount: number;
  failedTaskCount: number;
  runningTaskCount: number;
  pendingTaskCount: number;
  startedTaskCount: number;
  upstreamFailedTaskCount: number;
}

export function fetchDataDevelopmentMetrics() {
  return get<DataDevelopmentMetrics>('/dashboard/data-development/metrics', {
    mockCode: 'monitoring-dashboard.get-data-development-metrics',
  });
}

// Get Data Development Daily Task Count
// GET /kun/api/v1/dashboard/data-development/date-time-metrics

export interface TaskResult {
  status: string;
  finalStatus: string;
  taskCount: number;
}
export interface DailyStatistic {
  time: string;
  totalCount: number;
  taskResultList: TaskResult[];
}

export interface TaskDetailsForWeekParams {
  pageNumber: number;
  pageSize: number;
  targetTime: number;
  status: string;
  finalStatus: string;
  timezoneOffset: number;
}

// Get Data Development Task Details
// GET /kun/api/v1/dashboard/data-development/tasks

export interface DevTaskDetail {
  taskId: string; // workflow task id
  taskRunId: string;
  taskName: string;
  taskStatus: string;
  errorMessage: string;
  startTime: number | null;
  endTime: number | null;
  createTime: number | null;
  updateTime: number | null;
  duration: number | null;
}

export interface DevTaskDetailsInfo extends PaginationRespBodyBase {
  tasks: DevTaskDetail[];
}

export function fetchDataDevelopmentTaskDetails(reqParams: Partial<PaginationReqBody> = {}) {
  return get<DevTaskDetailsInfo>('/dashboard/data-development/tasks', {
    query: reqParams,
  });
}

export function fetchDataDevelopmentChartTaskDetails(reqParams: Partial<PaginationReqBody> = {}) {
  return get<DevTaskDetailsInfo>('/dashboard/data-development/chart-tasks', {
    query: reqParams,
  });
}

export function fetchDataDevelopmentStatisticChart(reqParams: any) {
  return get<any>('/dashboard/data-development/statistic-chart', {
    query: reqParams,
  });
}
