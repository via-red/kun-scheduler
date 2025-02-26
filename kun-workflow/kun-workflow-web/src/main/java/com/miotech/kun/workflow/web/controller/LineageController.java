package com.miotech.kun.workflow.web.controller;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.miotech.kun.commons.web.annotation.QueryParameter;
import com.miotech.kun.commons.web.annotation.RequestBody;
import com.miotech.kun.commons.web.annotation.RouteMapping;
import com.miotech.kun.workflow.common.lineage.service.LineageService;
import com.miotech.kun.workflow.common.task.service.TaskService;
import com.miotech.kun.workflow.core.model.lineage.*;
import com.miotech.kun.workflow.core.model.lineage.node.DatasetInfo;
import com.miotech.kun.workflow.core.model.lineage.node.DatasetNode;
import com.miotech.kun.workflow.core.model.lineage.node.TaskNode;
import com.miotech.kun.workflow.core.model.task.Task;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class LineageController {
    @Inject
    private LineageService lineageService;

    @Inject
    private TaskService taskService;


    @RouteMapping(url = "/lineages", method = "GET")
    public Object getLineageNeighbors(
            @QueryParameter Long datasetGid,
            @QueryParameter(defaultValue = "BOTH") String direction,
            @QueryParameter(defaultValue = "1") Integer depth
    ) {
        // Pre-check if parameters are valid
        Preconditions.checkArgument(Objects.nonNull(datasetGid), "dataset global id cannot be null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(direction), "Illegal query parameter `direction`: {}", direction);
        Preconditions.checkArgument(Objects.nonNull(depth) && (depth > 0), "Illegal query parameter `depth`: {}", depth);

        return searchLineageInfo(datasetGid, depth, direction);
    }

    @RouteMapping(url = "/lineage/datasets/upstream-task", method = "POST")
    public List<UpstreamTaskInformation> getLineageTask(@RequestBody UpstreamTaskRequest upstreamTaskRequest) {
        return lineageService.fetchDirectUpstreamTask(upstreamTaskRequest.getDatasetGids());
    }

    private DatasetLineageInfo searchLineageInfo(Long datasetGid, int depth, String direction) {
        DatasetNodeSearchResult searchResult = this.doLineageSearch(datasetGid, direction, depth);
        Set<DatasetNode> upstreamNodes = searchResult.getUpstreamNodes();
        Set<DatasetNode> downstreamNodes = searchResult.getDownstreamNodes();
        Optional<DatasetNode> sourceNode = searchResult.getSourceNode();

        Set<Long> relatedTaskIds = collectAllRelatedTaskIdsFromSearchResult(searchResult);
        Map<Long, Optional<Task>> idToTaskMap = taskService.fetchByIds(relatedTaskIds);

        return DatasetLineageInfo.newBuilder()
                .withSourceNode(sourceNode.map(node -> this.datasetNodeToInfo(node, idToTaskMap, direction)).orElse(null))
                .withUpstreamNodes(datasetNodesToInfoList(upstreamNodes, idToTaskMap, direction))
                .withDownstreamNodes(datasetNodesToInfoList(downstreamNodes, idToTaskMap, direction))
                .withQueryDepth(depth)
                .build();
    }

    private Set<Long> collectAllRelatedTaskIdsFromSearchResult(DatasetNodeSearchResult searchResult) {
        Set<Long> relatedTaskIds = new HashSet<>();
        Set<DatasetNode> upstreamNodes = searchResult.getUpstreamNodes();
        Set<DatasetNode> downstreamNodes = searchResult.getDownstreamNodes();
        Optional<DatasetNode> sourceNode = searchResult.getSourceNode();
        if (sourceNode.isPresent()) {
            relatedTaskIds.addAll(sourceNode.get().getUpstreamTasks().stream().map(TaskNode::getTaskId).collect(Collectors.toSet()));
            relatedTaskIds.addAll(sourceNode.get().getDownstreamTasks().stream().map(TaskNode::getTaskId).collect(Collectors.toSet()));

        }
        upstreamNodes.forEach(node -> {
            relatedTaskIds.addAll(node.getUpstreamTasks().stream().map(TaskNode::getTaskId).collect(Collectors.toSet()));
            relatedTaskIds.addAll(node.getDownstreamTasks().stream().map(TaskNode::getTaskId).collect(Collectors.toSet()));
        });
        downstreamNodes.forEach(node -> {
            relatedTaskIds.addAll(node.getUpstreamTasks().stream().map(TaskNode::getTaskId).collect(Collectors.toSet()));
            relatedTaskIds.addAll(node.getDownstreamTasks().stream().map(TaskNode::getTaskId).collect(Collectors.toSet()));
        });
        return relatedTaskIds;
    }

    private DatasetNodeInfo datasetNodeToInfo(DatasetNode datasetNode, Map<Long, Optional<Task>> idToTaskMap, String direction) {
        // Set upstream/downstream tasks to empty if direction is not matching
        Set<TaskNode> upstreamTaskNodes = !Objects.equals(direction, "DOWNSTREAM") ?
                datasetNode.getUpstreamTasks() : new HashSet<>();
        Set<TaskNode> downstreamTaskNodes = !Objects.equals(direction, "UPSTREAM") ?
                datasetNode.getDownstreamTasks() : new HashSet<>();
        return DatasetNodeInfo.newBuilder()
                .withGid(datasetNode.getGid())
                .withDatasetName(datasetNode.getDatasetName())
                .withUpstreamTasks(upstreamTaskNodes.stream()
                        .map(TaskNode::getTaskId)
                        .map(taskId -> idToTaskMap.getOrDefault(taskId, Optional.empty()).orElse(null))
                        .collect(Collectors.toList()))
                .withDownstreamTasks(downstreamTaskNodes.stream()
                        .map(TaskNode::getTaskId)
                        .map(taskId -> idToTaskMap.getOrDefault(taskId, Optional.empty()).orElse(null))
                        .collect(Collectors.toList()))
                .withUpstreamDatasetCount(lineageService.fetchDatasetDirectUpstreamCount(datasetNode.getGid()))
                .withDownstreamDatasetCount(lineageService.fetchDatasetDirectDownstreamCount(datasetNode.getGid()))
                .build();
    }

    private List<DatasetNodeInfo> datasetNodesToInfoList(Set<DatasetNode> datasetNodes, Map<Long, Optional<Task>> idToTaskMap, String direction) {
        return datasetNodes.stream()
                .map(node -> this.datasetNodeToInfo(node, idToTaskMap, direction))
                .collect(Collectors.toList());
    }

    private DatasetNodeSearchResult doLineageSearch(Long datasetGid, String direction, int depth) {
        Set<DatasetNode> upstreamNodes = new LinkedHashSet<>();
        Set<DatasetNode> downstreamNodes = new LinkedHashSet<>();
        Optional<DatasetNode> sourceNode = lineageService.fetchDatasetNodeById(datasetGid);

        switch (direction) {
            case "UPSTREAM":
                upstreamNodes.addAll(lineageService.fetchUpstreamDatasetNodes(datasetGid, depth));
                break;
            case "DOWNSTREAM":
                downstreamNodes.addAll(lineageService.fetchDownstreamDatasetNodes(datasetGid, depth));
                break;
            case "BOTH":
                downstreamNodes.addAll(lineageService.fetchDownstreamDatasetNodes(datasetGid, depth));
                upstreamNodes.addAll(lineageService.fetchUpstreamDatasetNodes(datasetGid, depth));
                break;
            default:
                throw new IllegalArgumentException(String.format("Illegal query parameter `direction`: %s", direction));
        }
        return new DatasetNodeSearchResult(upstreamNodes, downstreamNodes, sourceNode.orElse(null));
    }

    static final class DatasetNodeSearchResult {
        private final Set<DatasetNode> upstreamNodes;
        private final Set<DatasetNode> downstreamNodes;
        private final DatasetNode sourceNode;

        public DatasetNodeSearchResult(Set<DatasetNode> upstreamNodes, Set<DatasetNode> downstreamNodes, DatasetNode sourceNode) {
            this.upstreamNodes = upstreamNodes;
            this.downstreamNodes = downstreamNodes;
            this.sourceNode = sourceNode;
        }

        public Set<DatasetNode> getUpstreamNodes() {
            return upstreamNodes;
        }

        public Set<DatasetNode> getDownstreamNodes() {
            return downstreamNodes;
        }

        public Optional<DatasetNode> getSourceNode() {
            return Optional.ofNullable(sourceNode);
        }
    }

    @RouteMapping(url = "/lineages/edges", method = "GET")
    public EdgeInfo getEdgeInfoBetweenNodes(
            @QueryParameter Long upstreamDatasetGid,
            @QueryParameter Long downstreamDatasetGid
    ) {
        Preconditions.checkArgument(Objects.nonNull(upstreamDatasetGid), "upstream dataset gid cannot be null.");
        Preconditions.checkArgument(Objects.nonNull(downstreamDatasetGid), "downstream dataset gid cannot be null.");
        return lineageService.fetchEdgeInfo(upstreamDatasetGid, downstreamDatasetGid);
    }

    @RouteMapping(url = "/lineages/outlets", method = "GET")
    public TaskOutlets fetchOutletNodes(@QueryParameter Long taskId) {
        Set<DatasetNode> datasetNodes = lineageService.fetchOutletNodes(taskId);
        return new TaskOutlets(datasetNodes.stream().map(node ->
                new DatasetInfo(node.getGid(), node.getDatasetName())).collect(Collectors.toSet()));
    }

}
