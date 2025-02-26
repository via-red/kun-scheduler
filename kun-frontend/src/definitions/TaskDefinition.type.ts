import * as T from 'runtypes';

import { TaskDatasetProperty } from '@/definitions/DatasetTaskDefSummary.type';
import { DeployStatusEnum, RunStatusEnum } from '@/definitions/StatEnums.type';
import { NotifyWhen, UserNotifyConfigItem } from '@/definitions/NotifyConfig.type';

export interface TaskDefinitionModel {
  id: number | string; // do not use this as identifier, use definitionId instead
  name: string;
  definitionId: number | string;
  taskTemplateName: string;
  taskPayload: TaskPayload;
  creator: number | string;
  owner: number | string;
  archived: boolean;
  lastModifier: string;
  createTime: string;
  updateTime: string;
}

export interface TaskDefinition {
  creator: number | string;
  id: number | string;
  isArchived: boolean;
  isDeployed: boolean;
  isUpdated: boolean;
  name: string;
  taskTemplateName: string;
  taskPayload: TaskPayload | null;
  taskCommits: TaskCommit[];
  upstreamTaskDefinitions: { id: string; name: string }[];
  lastUpdateTime: string;
  lastModifier: string;
  createTime: string;
  owner: string | number;
}

export const TaskDefinitionType = T.Record({
  creator: T.String.Or(T.Number),
  id: T.String.Or(T.Number),
  isArchived: T.Boolean,
  isDeployed: T.Boolean,
  name: T.String,
  taskTemplateName: T.String,
  taskPayload: T.Unknown.Or(T.Null),
  upstreamTaskDefinitions: T.Array(T.Record({ id: T.String, name: T.String })),
  lastUpdateTime: T.String.Or(T.Null),
  lastModifier: T.String.Or(T.Null),
  createTime: T.String.Or(T.Null),
  owner: T.String.Or(T.Number),
});
export interface TaskCommit {
  id: string;
  committedAt: string;
  committer: string;
  version: string;
  message: string;
  snapshot: { taskPayload: TaskPayload; taskTemplateName: string; owner: string };
  latestCommit: boolean;
}
export interface TaskPayload {
  scheduleConfig: ScheduleConfig;
  taskConfig: Record<string, any>;
  notifyConfig: NotifyConfig;
}

export interface SlaConfig {
  level: number;
  hours: number;
  minutes: number;
}

export enum BlockType {
  NONE = 'NONE',
  WAIT_PREDECESSOR = 'WAIT_PREDECESSOR',
  WAIT_PREDECESSOR_DOWNSTREAM = 'WAIT_PREDECESSOR_DOWNSTREAM',
}

export interface ScheduleConfig {
  cronExpr: string;
  timeZone: String;
  inputNodes: number[];
  type: string;
  inputDatasets: TaskDatasetProperty[];
  outputDatasets: TaskDatasetProperty[];
  retries: number;
  retryDelay: number;
  slaConfig: SlaConfig;
  blockType: BlockType;
}

export interface TaskTryVO {
  creator: number;
  definitionId: string | number;
  id: string;
  status: RunStatusEnum;
  taskConfig: Record<string, any>;
  workflowTaskId: string | number;
  workflowTaskRunId: string | number;
}

export interface DeployVO {
  creator: number;
  deployedAt: string; // Datetime
  deployer: number;
  id: string | number;
  name: string;
  status: DeployStatusEnum;
  submittedAt: string; // Datetime
}

export interface NotifyConfig {
  notifyWhen: NotifyWhen;
  notifierConfig: UserNotifyConfigItem[];
}
