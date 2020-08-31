import { DatasetDetail, Column } from '@/rematch/models/datasetDetail';
import { Pagination } from '@/definitions/common-types';
import { get, post } from '@/utils/requestUtils';
import { DEFAULT_API_PREFIX } from '@/constants/api-prefixes';

export interface FetchDatasetDetailRespBody extends DatasetDetail {}

export async function fetchDatasetDetailService(detailId: string) {
  return get<FetchDatasetDetailRespBody>('/metadata/dataset/:detailId', {
    pathParams: { detailId },
    prefix: DEFAULT_API_PREFIX,
  });
}

export interface FetchDatasetColumnsRespBody extends Pagination {
  columns: Column[];
}

export async function fetchDatasetColumnsService(
  datasetId: string,
  keyword: string,
  pagination: Pagination,
) {
  const { pageSize, pageNumber } = pagination;
  const params = {
    keyword,
    pageSize,
    pageNumber,
  };
  return get<FetchDatasetColumnsRespBody>(
    '/metadata/dataset/:datasetId/columns',
    {
      pathParams: { datasetId },
      prefix: DEFAULT_API_PREFIX,
      query: params,
    },
  );
}

export interface PullDatasetRespBody {
  duration: number;
}

export async function pullDatasetService(datasetId: string) {
  return post<PullDatasetRespBody>('/metadata/dataset/:datasetId/pull', {
    pathParams: { datasetId },
    prefix: DEFAULT_API_PREFIX,
  });
}

export interface UpdateDatasetReqBody {
  description: string | null;
  owners: string[] | null;
  tags: string[] | null;
}

export interface UpdateDatasetRespBody extends FetchDatasetDetailRespBody {}

export async function updateDatasetService(
  datasetId: string,
  reqBody: UpdateDatasetReqBody,
) {
  return post<UpdateDatasetRespBody>('/metadata/dataset/:datasetId/update', {
    pathParams: { datasetId },
    data: reqBody,
    prefix: DEFAULT_API_PREFIX,
  });
}

export interface UpdateColumnRespBody {
  id: string;
  name: string;
  description: string;
}

export async function updateColumnService(
  id: string,
  params: { description: string },
) {
  return post<UpdateColumnRespBody>('/metadata/column/:id/update', {
    pathParams: {
      id,
    },
    data: params,
    prefix: DEFAULT_API_PREFIX,
  });
}
