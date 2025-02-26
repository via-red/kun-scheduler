import React, { useCallback, useState } from 'react';
import moment from 'moment';
import { Button, Descriptions, message, Modal, Space, Tag, Tooltip } from 'antd';
import { FormInstance } from 'antd/es/form';
import { history, Link, useRouteMatch } from 'umi';
import isArray from 'lodash/isArray';
import { EditText } from '@/components/EditText';
import useRedux from '@/hooks/useRedux';
import useI18n from '@/hooks/useI18n';
import LogUtils from '@/utils/logUtils';
import {
  commitAndDeployTaskDefinition,
  deleteTaskDefinition,
  updateTaskDefinition,
  fetchTaskDefinitionDetail,
} from '@/services/data-development/task-definitions';
import { getFlattenedTaskDefinition } from '@/utils/transformDataset';
import { TaskCommitModal } from '@/pages/data-development/task-definition-config/components/TaskCommitModal';
import { UserSelect } from '@/components/UserSelect';
import { UsernameText } from '@/components/UsernameText';

import { TaskDefinition } from '@/definitions/TaskDefinition.type';
import { TaskTemplate } from '@/definitions/TaskTemplate.type';

import { transformFormTaskConfig } from '@/pages/data-development/task-definition-config/helpers';
import { ConfirmBackfillCreateModal } from '@/pages/data-development/components/ConfirmBackfillCreateModal/ConfirmBackfillCreateModal';
import SafeUrlAssembler from 'safe-url-assembler';
import { ArrowRightOutlined } from '@ant-design/icons';
import { NotifyWhen } from '@/definitions/NotifyConfig.type';
import styles from './Header.less';

export interface Props {
  draftTaskDef: TaskDefinition | null;
  setDraftTaskDef: any;
  form: FormInstance;
  taskDefId: string | number;
  handleCommitDryRun: () => any;
  taskTemplate: TaskTemplate | null;
}

const logger = LogUtils.getLoggers('Header');

export const Header: React.FC<Props> = props => {
  const match = useRouteMatch<{ taskDefId: string }>();
  const { draftTaskDef, setDraftTaskDef, form, taskDefId, handleCommitDryRun } = props;

  const [commitModalVisible, setCommitModalVisible] = useState<boolean>(false);
  const [confirmBackfillCreateModalVisible, setConfirmBackfillCreateModalVisible] = useState<boolean>(false);

  const t = useI18n();

  const {
    selector: { definitionFormDirty, backUrl },
    dispatch,
  } = useRedux(s => ({
    definitionFormDirty: s.dataDevelopment.definitionFormDirty,
    backUrl: s.dataDevelopment.backUrl,
  }));

  const handleDeleteBtnClick = useCallback(() => {
    if (draftTaskDef && draftTaskDef.id) {
      Modal.confirm({
        title: t('dataDevelopement.definition.deleteAlert.title'),
        content: t('dataDevelopement.definition.deleteAlert.content'),
        okText: t('common.button.delete'),
        okType: 'danger',
        onOk: () => {
          deleteTaskDefinition(draftTaskDef.id)
            .then(() => {
              if (backUrl) {
                logger.debug('Getting back to url: %o', backUrl);
                history.push(backUrl);
              } else {
                history.push('/data-development');
              }
            })
            .catch(() => {
              message.error('Failed to delete task');
            });
        },
      });
    }
  }, [t, draftTaskDef, backUrl]);

  const handleSaveBtnClick = useCallback(async () => {
    try {
      logger.debug('Form.values =', form.getFieldsValue());
      await form.validateFields();
      // if all fields are valid
      const newTaskDefinition = await updateTaskDefinition({
        id: taskDefId,
        name: draftTaskDef?.name || '',
        owner: draftTaskDef?.owner || '',
        taskPayload: {
          scheduleConfig: {
            ...form.getFieldValue(['taskPayload', 'scheduleConfig']),
            // convert dataset related fields to conform API required shape
            inputDatasets: getFlattenedTaskDefinition(
              form.getFieldValue(['taskPayload', 'scheduleConfig', 'inputDatasets']) || [],
            ),
            inputNodes: (form.getFieldValue(['taskPayload', 'scheduleConfig', 'inputNodes']) || []).map(
              (valueObj: { value: string | number }) => valueObj.value,
            ),
          },
          taskConfig: transformFormTaskConfig(form.getFieldValue(['taskPayload', 'taskConfig']), props.taskTemplate),
          notifyConfig: {
            notifyWhen: form.getFieldValue(['taskPayload', 'notifyConfig', 'notifyWhen']) || NotifyWhen.SYSTEM_DEFAULT,
            notifierConfig: form.getFieldValue(['taskPayload', 'notifyConfig', 'notifierConfig']) || [],
          },
        },
      });
      message.success(t('common.operateSuccess'));
      dispatch.dataDevelopment.setDefinitionFormDirty(false);
      setDraftTaskDef(newTaskDefinition);
    } catch (e) {
      logger.warn(e);
      // hint each form error
      if (e && e.errorFields && isArray(e.errorFields)) {
        e.errorFields.forEach((fieldErr: { errors: string[] }) => message.error(fieldErr.errors[0]));
      }
    }
  }, [
    form,
    taskDefId,
    draftTaskDef?.name,
    draftTaskDef?.owner,
    props.taskTemplate,
    t,
    dispatch.dataDevelopment,
    setDraftTaskDef,
  ]);

  const renderCommitBtn = () => {
    if (definitionFormDirty) {
      return (
        <Tooltip title={t('dataDevelopement.definition.commitBtnDisabledTooltip')}>
          <Button type="primary" disabled>
            {t('common.button.commit')}
          </Button>
        </Tooltip>
      );
    }
    // else
    return (
      <Button
        type="primary"
        onClick={() => {
          setCommitModalVisible(true);
        }}
      >
        {t('common.button.commit')}
      </Button>
    );
  };

  return (
    <header className={styles.EditHeader}>
      <div className={styles.TitleAndToolBtnGroup}>
        <h2 className={styles.DefinitionTitle}>
          {/* Definition title and edit input */}
          <EditText
            viewContainerClassName={styles.EditText}
            value={draftTaskDef?.name || ''}
            type="text"
            validation={value => {
              return `${value}`.trim().length !== 0;
            }}
            onSave={(value: string) => {
              dispatch.dataDevelopment.setDefinitionFormDirty(true);
              setDraftTaskDef({
                ...draftTaskDef,
                name: value,
              });
            }}
          />
        </h2>
        {/* Tool buttons */}
        <div className={styles.HeadingButtons}>
          <Space>
            {/* Go to scheduled tasks */}
            {draftTaskDef?.isDeployed ? (
              <Link
                to={SafeUrlAssembler()
                  .template('/operation-center/scheduled-tasks/:taskDefId')
                  .param({
                    taskDefId: draftTaskDef.id,
                  })
                  .toString()}
              >
                <Button icon={<ArrowRightOutlined />}>{t('dataDevelopment.goToScheduledTasks')}</Button>
              </Link>
            ) : (
              <Button icon={<ArrowRightOutlined />} disabled>
                {t('dataDevelopment.goToScheduledTasks')}
              </Button>
            )}
            {/* Delete */}
            <Button onClick={handleDeleteBtnClick}>{t('common.button.delete')}</Button>
            {/* Dry run */}
            <Button onClick={handleCommitDryRun}>{t('common.button.dryrun')}</Button>
            {/* Backfill */}
            <Button onClick={() => setConfirmBackfillCreateModalVisible(true)}>{t('dataDevelopment.backfill')}</Button>
            {/* Save */}
            <Button onClick={handleSaveBtnClick}>{t('common.button.save')}</Button>
            {/* Commit */}
            {renderCommitBtn()}
          </Space>
        </div>
      </div>
      <div className={styles.TaskDefMetas}>
        <Descriptions column={2}>
          {/* Task template type */}
          <Descriptions.Item label={t('dataDevelopment.definition.property.taskTemplateName')}>
            {props.taskTemplate?.name || '-'}
          </Descriptions.Item>
          {/* Status */}
          <Descriptions.Item label={t('dataDevelopment.definition.property.currentState')}>
            {!draftTaskDef?.isUpdated && draftTaskDef?.isDeployed && (
              <Tag color="processing">{t('dataDevelopment.definition.property.isDeployed')}</Tag>
            )}
            {draftTaskDef?.isArchived && <Tag color="error">{t('dataDevelopment.definition.property.isArchived')}</Tag>}
            {!draftTaskDef?.isUpdated && !draftTaskDef?.isDeployed && !draftTaskDef?.isArchived && (
              <Tag color="default">{t('dataDevelopment.definition.property.draft')}</Tag>
            )}
            {draftTaskDef?.isUpdated && (
              <Tag className={styles.updatedTag}>{t('dataDevelopment.definition.property.isUpdated')}</Tag>
            )}
          </Descriptions.Item>
          {/* Owner */}
          <Descriptions.Item label={t('dataDevelopment.definition.property.owner')}>
            {draftTaskDef?.owner ? (
              <UserSelect
                style={{ width: '200px' }}
                size="small"
                value={`${draftTaskDef.owner}`}
                onChange={(nextUserValue: string | string[]) => {
                  if (typeof nextUserValue !== 'string') {
                    return;
                  }
                  setDraftTaskDef({
                    ...draftTaskDef,
                    owner: nextUserValue,
                  });
                }}
              />
            ) : (
              '...'
            )}
          </Descriptions.Item>
          {/* Last modifier */}
          <Descriptions.Item label={t('dataDevelopment.definition.property.updater')}>
            {draftTaskDef?.lastModifier ? <UsernameText userId={draftTaskDef?.lastModifier} /> : '...'}
          </Descriptions.Item>
          {/* Create time */}
          <Descriptions.Item label={t('dataDevelopment.definition.property.createTime')}>
            {draftTaskDef?.createTime ? moment(draftTaskDef.createTime).format('YYYY-MM-DD HH:mm:ss') : '...'}
          </Descriptions.Item>
          {/* Last update time */}
          <Descriptions.Item label={t('dataDevelopment.definition.property.lastUpdateTime')}>
            {draftTaskDef?.lastUpdateTime ? moment(draftTaskDef.lastUpdateTime).format('YYYY-MM-DD HH:mm:ss') : '...'}
          </Descriptions.Item>
        </Descriptions>
      </div>
      {/* Commit confirm modal */}
      <TaskCommitModal
        visible={commitModalVisible}
        onCancel={() => {
          setCommitModalVisible(false);
        }}
        onConfirm={async (commitMsg: string) => {
          const respData = await commitAndDeployTaskDefinition(taskDefId, commitMsg);
          if (respData) {
            const newTaskDefinition = await await fetchTaskDefinitionDetail(taskDefId);
            message.success('Commit success.');
            setDraftTaskDef(newTaskDefinition);
            setCommitModalVisible(false);
          }
          return respData;
        }}
      />
      <ConfirmBackfillCreateModal
        visible={confirmBackfillCreateModalVisible}
        selectedTaskDefIds={[match.params.taskDefId]}
        initValue={draftTaskDef?.name}
        onCancel={() => {
          setConfirmBackfillCreateModalVisible(false);
        }}
      />
    </header>
  );
};
