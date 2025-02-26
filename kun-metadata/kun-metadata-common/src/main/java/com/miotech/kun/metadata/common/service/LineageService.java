package com.miotech.kun.metadata.common.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.miotech.kun.metadata.common.exception.EntityNotFoundException;
import com.miotech.kun.metadata.common.utils.JSONUtils;
import com.miotech.kun.metadata.core.model.dataset.DataStore;
import com.miotech.kun.metadata.core.model.dataset.Dataset;
import com.miotech.kun.metadata.facade.LineageServiceFacade;
import com.miotech.kun.workflow.core.model.lineage.EdgeInfo;
import com.miotech.kun.workflow.core.model.lineage.EdgeTaskInfo;
import com.miotech.kun.workflow.core.model.lineage.UpstreamTaskBasicInformation;
import com.miotech.kun.workflow.core.model.lineage.node.DatasetNode;
import com.miotech.kun.workflow.core.model.lineage.node.TaskNode;
import com.miotech.kun.workflow.core.model.task.Task;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class LineageService implements LineageServiceFacade {
    private static final Logger logger = LoggerFactory.getLogger(LineageService.class);

    private static final String ERROR_MESSAGE_NULL_ARGUMENT = "Argument cannot be null";

    private final SessionFactory sessionFactory;

    private final MetadataDatasetService metadataDatasetService;

    @Inject
    public LineageService(SessionFactory sessionFactory, MetadataDatasetService metadataDatasetService) {
        this.sessionFactory = sessionFactory;
        this.metadataDatasetService = metadataDatasetService;
    }

    // ---------------- Public methods ----------------

    public Optional<DatasetNode> fetchDatasetNodeById(Long datasetGlobalId) {
        Preconditions.checkNotNull(datasetGlobalId, ERROR_MESSAGE_NULL_ARGUMENT);
        return Optional.ofNullable(getSession().load(DatasetNode.class, datasetGlobalId));
    }

    /**
     * Fetch count of upstream dataset nodes that have direct lineage relation to specific dataset
     *
     * @param datasetGlobalId source dataset node
     * @return count of upstream dataset nodes that have direct lineage relation to the source dataset node
     */
    public Integer fetchDatasetDirectUpstreamCount(Long datasetGlobalId) {
        Preconditions.checkNotNull(datasetGlobalId, ERROR_MESSAGE_NULL_ARGUMENT);
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("datasetGid", datasetGlobalId);
        Iterable<Integer> result = getSession().query(
                Integer.class,
                "MATCH (d:KUN_DATASET)-->(t:KUN_TASK)-->(src:KUN_DATASET) " +
                        "WHERE src.gid = $datasetGid " +
                        "RETURN count(d)",
                paramsMap
        );
        if (result.iterator().hasNext()) {
            return result.iterator().next();
        }
        // else
        return null;
    }

    /**
     * Fetch count of upstream dataset nodes that have direct lineage relation to specific dataset
     *
     * @param datasetGlobalId source dataset node
     * @return count of upstream dataset nodes that have direct lineage relation to the source dataset node
     */
    public Integer fetchDatasetDirectDownstreamCount(Long datasetGlobalId) {
        Preconditions.checkNotNull(datasetGlobalId, ERROR_MESSAGE_NULL_ARGUMENT);
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("datasetGid", datasetGlobalId);
        Iterable<Integer> result = getSession().query(
                Integer.class,
                "MATCH (src:KUN_DATASET)-->(t:KUN_TASK)-->(d:KUN_DATASET) " +
                        "WHERE src.gid = $datasetGid " +
                        "RETURN count(d)",
                paramsMap
        );
        if (result.iterator().hasNext()) {
            return result.iterator().next();
        }
        // else
        return null;
    }

    public Optional<TaskNode> fetchTaskNodeById(Long taskId) {
        Preconditions.checkNotNull(taskId, ERROR_MESSAGE_NULL_ARGUMENT);
        return Optional.ofNullable(getSession().load(TaskNode.class, taskId));
    }

    /**
     * Obtain dataset by datastore object as key
     *
     * @param dataStore datastore object which represents dataset
     * @return Optional dataset object
     */
    public Optional<Dataset> fetchDatasetByDatastore(DataStore dataStore) {
        return Optional.ofNullable(metadataDatasetService.createDataSetIfNotExist(dataStore));
    }


    /**
     * Obtain edge info by upstream and downstream dataset id
     *
     * @param upstreamDatasetGid   global id of upstream dataset
     * @param downstreamDatasetGid global id of downstream dataset
     */
    public EdgeInfo fetchEdgeInfo(Long upstreamDatasetGid, Long downstreamDatasetGid) {
        Preconditions.checkNotNull(upstreamDatasetGid);
        Preconditions.checkNotNull(downstreamDatasetGid);

        logger.debug("Fetching edge info with dataset gid = {} as upstream and dataset gid = {} as downstream", upstreamDatasetGid, downstreamDatasetGid);
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("upstreamDatasetGid", upstreamDatasetGid);
        paramsMap.put("downstreamDatasetGid", downstreamDatasetGid);
        Iterable<TaskNode> taskNodes = getSession()
                .query(TaskNode.class,
                        "MATCH (d1:KUN_DATASET)-->(t:KUN_TASK)-->(d2:KUN_DATASET) " +
                                "WHERE d1.gid = $upstreamDatasetGid AND d2.gid = $downstreamDatasetGid " +
                                "RETURN t",
                        paramsMap
                );
        List<EdgeTaskInfo> edgeTaskInfos = new ArrayList<>();
        for (TaskNode taskNode : taskNodes) {
            edgeTaskInfos.add(
                    EdgeTaskInfo.newBuilder()
                            .withId(taskNode.getTaskId())
                            .withName(taskNode.getTaskName())
                            .withDescription(taskNode.getDescription())
                            .build()
            );
        }
        EdgeInfo edgeInfo = EdgeInfo.newBuilder()
                .withUpstreamDatasetGid(upstreamDatasetGid)
                .withDownstreamDatasetGid(downstreamDatasetGid)
                .withTaskInfos(edgeTaskInfos)
                .build();
        logger.debug("Found {} tasks between upstream dataset gid = {}, downstream dataset gid = {}",
                edgeInfo.getTaskInfos().size(), edgeInfo.getUpstreamDatasetGid(), edgeInfo.getDownstreamDatasetGid());
        return edgeInfo;
    }


    /**
     * Save an dataset entity to graph node and return it
     *
     * @param dataset dataset model entity to save
     * @return saved dataset node
     */
    public DatasetNode saveDataset(Dataset dataset) {
        Optional<DatasetNode> datasetNodeOptional = fetchDatasetNodeById(dataset.getGid());
        DatasetNode datasetNode = datasetNodeOptional.orElseGet(DatasetNode::new);

        // Assign properties
        datasetNode.setGid(dataset.getGid());
        datasetNode.setDatasetName(dataset.getName());

        logger.debug("Saving lineage dataset node, gid = {}, name = {}", dataset.getGid(), dataset.getName());
        saveDatasetNode(datasetNode);
        return datasetNode;
    }

    /**
     * Save an task entity to graph node and return it
     *
     * @param task task model entity to save
     * @return saved task node
     */
    public TaskNode saveTask(Task task) {
        Optional<TaskNode> taskNodeOptional = fetchTaskNodeById(task.getId());
        TaskNode taskNode = taskNodeOptional.orElseGet(() -> TaskNode.from(task));
        logger.debug("Saving lineage task node, id = {}, name = {}", task.getId(), task.getName());
        saveTaskNode(taskNode);
        return taskNode;
    }

    /**
     * Update or insert a dataset node into graph storage
     *
     * @param node dataset node
     * @return <code>true</code> if operation successful
     */
    public boolean saveDatasetNode(DatasetNode node) {
        getSession().save(node);
        logger.debug("Saving lineage dataset node, id = {}, dataset name = {}", node.getGid(), node.getDatasetName());
        return true;
    }

    /**
     * Update or insert a task node into graph storage
     *
     * @param node dataset node
     * @return <code>true</code> if operation successful
     */
    public boolean saveTaskNode(TaskNode node) {
        getSession().save(node);
        logger.debug("Upserting lineage task node, id = {}, name = {}", node.getTaskId(), node.getTaskName());
        return true;
    }

    /**
     * Delete dataset node
     *
     * @param nodeId id of dataset node
     * @return <code>true</code> if operation successful, <code>false</code> if node not found
     */
    public boolean deleteDatasetNode(Long nodeId) {
        Session sess = getSession();
        DatasetNode existingNode = sess.load(DatasetNode.class, nodeId);
        logger.debug("Deleting lineage dataset node, id = {}", nodeId);
        if (Objects.isNull(existingNode)) {
            return false;
        }
        // else
        sess.delete(existingNode);
        return true;
    }

    /**
     * Delete task node
     * @param nodeId id of task node
     * @return <code>true</code> if operation successful, <code>false</code> if node not found
     */
    public boolean deleteTaskNode(Long nodeId) {
        Session sess = getSession();
        TaskNode existingNode = sess.load(TaskNode.class, nodeId);
        logger.debug("Deleting lineage task node, id = {}", nodeId);
        if (Objects.isNull(existingNode)) {
            return false;
        }
        // else
        sess.delete(existingNode);
        return true;
    }

    /**
     * @param datasetGlobalId
     * @return
     */
    public Set<DatasetNode> fetchUpstreamDatasetNodes(Long datasetGlobalId) {
        return fetchUpstreamDatasetNodes(datasetGlobalId, 1);
    }

    /**
     * @param datasetGlobalId
     * @return set of upstream dataset nodes
     * @throws IllegalArgumentException when depth is not positive integer
     */
    public Set<DatasetNode> fetchUpstreamDatasetNodes(Long datasetGlobalId, int depth) {
        Preconditions.checkArgument(depth > 0, "Depth field should be positive but got: %s", depth);
        return searchAllRelatedDatasetNodesWithDepth(datasetGlobalId, DirectionEnum.UPSTREAM, depth);
    }

    /**
     * @param datasetGlobalId
     * @return
     */
    public Set<DatasetNode> fetchDownstreamDatasetNodes(Long datasetGlobalId) {
        return fetchDownstreamDatasetNodes(datasetGlobalId, 1);
    }

    /**
     * @param datasetGlobalId
     * @return set of downstream dataset nodes
     * @throws IllegalArgumentException when depth is not positive integer
     */
    public Set<DatasetNode> fetchDownstreamDatasetNodes(Long datasetGlobalId, int depth) {
        Preconditions.checkArgument(depth > 0, "Depth field should be positive but got: %s", depth);
        return searchAllRelatedDatasetNodesWithDepth(datasetGlobalId, DirectionEnum.DOWNSTREAM, depth);
    }

    /**
     * @param taskNodeId
     * @return
     */
    public Set<DatasetNode> fetchInletNodes(Long taskNodeId) {
        Preconditions.checkNotNull(taskNodeId, ERROR_MESSAGE_NULL_ARGUMENT);

        TaskNode taskNode = getSession().load(TaskNode.class, taskNodeId);
        if (Objects.isNull(taskNode)) {
            return new LinkedHashSet<>();
        }
        // else
        return taskNode.getInlets();
    }

    /**
     * @param taskNodeId
     * @return
     */
    public Set<DatasetNode> fetchOutletNodes(Long taskNodeId) {
        Preconditions.checkNotNull(taskNodeId, ERROR_MESSAGE_NULL_ARGUMENT);

        TaskNode taskNode = getSession().load(TaskNode.class, taskNodeId);
        if (Objects.isNull(taskNode)) {
            return new LinkedHashSet<>();
        }
        // else
        return taskNode.getOutlets();
    }

    /**
     * @param task
     * @param upstreamDatastore
     * @param downstreamDataStore
     * @return
     */
    public void updateTaskLineage(Task task, List<DataStore> upstreamDatastore, List<DataStore> downstreamDataStore) {
        Optional<TaskNode> taskNodeOptional = fetchTaskNodeById(task.getId());
        if(taskNodeOptional.isPresent()){
            deleteTaskNode(taskNodeOptional.get().getTaskId());
        }

        // Create a task node
        TaskNode taskNode = TaskNode.from(task);
        // upsert upstream dataset nodes & relations to task node entity
        for (DataStore store : upstreamDatastore) {
            Optional<Dataset> datasetOptional = fetchDatasetByDatastore(store);
            if (datasetOptional.isPresent()) {
                DatasetNode datasetNode = DatasetNode.from(datasetOptional.get());
                taskNode.addInlet(datasetNode);
                logger.debug("For upstream datastore: {} , found upstream dataset node with gid {} from metadata service",
                        JSONUtils.toJsonString(store), datasetNode.getGid());
            } else {
                logger.debug("For upstream datastore: {} , cannot find corresponding dataset from metadata service",
                        JSONUtils.toJsonString(store));
            }
        }
        // upsert downstream dataset nodes & relations to task node entity
        for (DataStore store : downstreamDataStore) {
            Optional<Dataset> datasetOptional = fetchDatasetByDatastore(store);
            if (datasetOptional.isPresent()) {
                DatasetNode datasetNode = DatasetNode.from(datasetOptional.get());
                taskNode.addOutlet(datasetNode);
                logger.debug("For downstream datastore: {} , found downstream dataset node with gid {} from metadata service",
                        JSONUtils.toJsonString(store), datasetNode.getGid());
            } else {
                logger.debug("For downstream datastore: {} , cannot find corresponding dataset from metadata service",
                        JSONUtils.toJsonString(store));
            }
        }
        // save task node
        saveTaskNode(taskNode);
    }

    @Override
    public List<UpstreamTaskBasicInformation> fetchDirectUpstreamTask(List<Long> datasetGids) {
        if (CollectionUtils.isEmpty(datasetGids)) {
            return Lists.newArrayList();
        }

        String gids = StringUtils.join(datasetGids, ",");
        String cypher = String.format("MATCH (t:KUN_TASK) --> (d:KUN_DATASET) WHERE d.gid IN [%s] return t.taskId as taskId, d.gid as gid", gids);
        Iterator<Map<String, Object>> iterator = getSession().query(cypher, Maps.newHashMap()).iterator();

        Map<Long, List<Long>> datasetTasks = extractFetchedInformation(iterator);
        return datasetTasks.entrySet().stream().map(datasetTask -> {
            Long gid = datasetTask.getKey();
            List<Long> taskIds = datasetTask.getValue();
            return new UpstreamTaskBasicInformation(gid, taskIds);
        }).collect(Collectors.toList());
    }

    // ---------------- Private methods ----------------

    enum DirectionEnum {
        UPSTREAM,
        DOWNSTREAM,
        BOTH,
    }

    private Session getSession() {
        return sessionFactory.openSession();
    }

    private void appendToResultSetConditionally(DatasetNode dsNode, Queue<DatasetNode> searchQueue, Set<DatasetNode> resultSet, boolean shouldAddNextDepthToQueue) {
        if (!resultSet.contains(dsNode)) {
            resultSet.add(dsNode);
            if (shouldAddNextDepthToQueue) {
                searchQueue.add(dsNode);
            }
        }
    }

    private Queue<DatasetNode> searchNeighbors(DatasetNode ds, Set<DatasetNode> resultSet, DirectionEnum direction, boolean shouldAddNextDepthToQueue) {
        Queue<DatasetNode> searchQueue = new LinkedList<>();
        if (Objects.equals(direction, DirectionEnum.DOWNSTREAM)) {
            Set<TaskNode> nextTaskNodeSet = ds.getDownstreamTasks();
            nextTaskNodeSet.forEach(taskNodePartial -> {
                Optional<TaskNode> taskNodeOptional = fetchTaskNodeById(taskNodePartial.getTaskId());
                if (!taskNodeOptional.isPresent()) {
                    throw new EntityNotFoundException(String.format("Cannot find task node with id: %s", taskNodePartial.getTaskId()));
                }
                taskNodeOptional.get()
                        .getOutlets()
                        .forEach(dsNodePartial -> {
                            DatasetNode dsNode = fetchDatasetNodeById(dsNodePartial.getGid()).orElseThrow(
                                    () -> new EntityNotFoundException(String.format("Cannot find dataset node with id: %s", dsNodePartial.getGid()))
                            );
                            appendToResultSetConditionally(dsNode, searchQueue, resultSet, shouldAddNextDepthToQueue);
                        });
            });
        }
        if (Objects.equals(direction, DirectionEnum.UPSTREAM)) {
            Set<TaskNode> nextTaskNodeSet = ds.getUpstreamTasks();
            nextTaskNodeSet.forEach(taskNodePartial -> {
                Optional<TaskNode> taskNodeOptional = fetchTaskNodeById(taskNodePartial.getTaskId());
                if (!taskNodeOptional.isPresent()) {
                    throw new EntityNotFoundException(String.format("Cannot find task node with id: %s", taskNodePartial.getTaskId()));
                }
                taskNodeOptional.get()
                        .getInlets()
                        .forEach(dsNodePartial -> {
                            DatasetNode dsNode = fetchDatasetNodeById(dsNodePartial.getGid()).orElseThrow(
                                    () -> new EntityNotFoundException(String.format("Cannot find dataset node with id: %s", dsNodePartial.getGid()))
                            );
                            appendToResultSetConditionally(dsNode, searchQueue, resultSet, shouldAddNextDepthToQueue);
                        });
            });
        }
        return searchQueue;
    }

    private Set<DatasetNode> searchAllRelatedDatasetNodesWithDepth(Long datasetGlobalId, DirectionEnum direction, int depth) {
        Preconditions.checkNotNull(datasetGlobalId, "Invalid argument `datasetGlobalId`: null");
        Preconditions.checkNotNull(direction, "Invalid argument `direction`: null");
        Preconditions.checkArgument(depth >= 1, "Invalid argument `depth`: %s; should be positive integer.", depth);

        Session sess = getSession();
        DatasetNode datasetNode = sess.load(DatasetNode.class, datasetGlobalId);
        if (Objects.isNull(datasetNode)) {
            throw new EntityNotFoundException(String.format("Cannot find dataset node with id: %s", datasetGlobalId));
        }
        Set<DatasetNode> resultSet = new LinkedHashSet<>();

        Queue<DatasetNode> searchQueue = new LinkedList<>();
        searchQueue.add(datasetNode);

        for (int currentDepth = 1; currentDepth <= depth; currentDepth += 1) {
            int depthElementsSize = searchQueue.size();
            for (int i = 0; i < depthElementsSize; i += 1) {
                DatasetNode ds = searchQueue.poll();
                logger.debug(String.format("Searching dataset node with id = %s, currentDepth = %s, depth = %s", ds.getGid(), currentDepth, depth));
                if (Objects.equals(direction, DirectionEnum.DOWNSTREAM) || Objects.equals(direction, DirectionEnum.BOTH)) {
                    Queue<DatasetNode> nextSearchQueue = searchNeighbors(ds, resultSet, DirectionEnum.DOWNSTREAM, currentDepth < depth);
                    searchQueue.addAll(nextSearchQueue);
                }
                if (Objects.equals(direction, DirectionEnum.UPSTREAM) || Objects.equals(direction, DirectionEnum.BOTH)) {
                    Queue<DatasetNode> nextSearchQueue = searchNeighbors(ds, resultSet, DirectionEnum.UPSTREAM, currentDepth < depth);
                    searchQueue.addAll(nextSearchQueue);
                }
            }
        }

        return resultSet;
    }

    private Map<Long, List<Long>> extractFetchedInformation(Iterator<Map<String, Object>> iterator) {
        Map<Long, List<Long>> datasetTasks = Maps.newHashMap();
        while (iterator.hasNext()) {
            Map<String, Object> information = iterator.next();
            Long gid = (Long) information.get("gid");
            Long taskId = (Long) information.get("taskId");
            if (datasetTasks.containsKey(gid)) {
                datasetTasks.get(gid).add(taskId);
            } else {
                List<Long> taskIds = Lists.newArrayList();
                taskIds.add(taskId);
                datasetTasks.put(gid, taskIds);
            }
        }

        return datasetTasks;
    }
}
