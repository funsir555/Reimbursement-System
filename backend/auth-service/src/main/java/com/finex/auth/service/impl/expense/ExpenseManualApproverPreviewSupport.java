package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseApprovalTimelineItemVO;
import com.finex.auth.dto.ExpenseDetailInstanceDTO;
import com.finex.auth.dto.ExpenseDocumentSubmitDTO;
import com.finex.auth.dto.ExpenseDocumentUpdateDTO;
import com.finex.auth.dto.ExpenseManualApproverPreviewNodeVO;
import com.finex.auth.dto.ExpenseManualApproverPreviewVO;
import com.finex.auth.dto.ProcessFlowNodeDTO;
import com.finex.auth.dto.ProcessFlowRouteDTO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseDetailDesign;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
class ExpenseManualApproverPreviewSupport {

    private static final String NODE_TYPE_APPROVAL = "APPROVAL";
    private static final String NODE_TYPE_CC = "CC";
    private static final String NODE_TYPE_PAYMENT = "PAYMENT";
    private static final String NODE_TYPE_BRANCH = "BRANCH";

    private static final String APPROVER_TYPE_MANAGER = "MANAGER";
    private static final String APPROVER_TYPE_MANUAL_SELECT = "MANUAL_SELECT";
    private static final String CANDIDATE_SCOPE_ALL_ACTIVE_USERS = "ALL_ACTIVE_USERS";

    private final AbstractExpenseDocumentSupport support;
    private final ExpenseWorkflowRuntimeSupport expenseWorkflowRuntimeSupport;
    private final ExpenseDocumentMutationApplySupport mutationApplySupport;

    ExpenseManualApproverPreviewVO previewForCreate(Long userId, ExpenseDocumentSubmitDTO dto) {
        ProcessDocumentTemplate template = support.requireTemplate(dto == null ? null : dto.getTemplateCode());
        ProcessFormDesign formDesign = support.loadFormDesign(template.getFormDesignCode());
        ProcessExpenseDetailDesign expenseDetailDesign = support.loadExpenseDetailDesign(template.getExpenseDetailDesignCode());
        Map<String, Object> formData = dto == null || dto.getFormData() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(dto.getFormData());
        List<ExpenseDetailInstanceDTO> expenseDetails = support.normalizeExpenseDetails(
                dto == null ? Collections.emptyList() : dto.getExpenseDetails()
        );
        String flowSnapshotJson = support.validateSubmitContext(template, formDesign, expenseDetailDesign, formData, expenseDetails);
        User submitter = support.loadActiveUser(userId);
        Map<String, Object> runtimeContext = expenseWorkflowRuntimeSupport.buildRuntimeFlowContext(
                submitter,
                template,
                formDesign,
                formData,
                expenseDetailDesign,
                expenseDetails
        );
        runtimeContext.put("manualApproverSelections", support.normalizeManualApproverSelections(
                dto == null ? null : dto.getManualApproverSelections()
        ));
        return buildPreview(flowSnapshotJson, runtimeContext);
    }

    ExpenseManualApproverPreviewVO previewForResubmit(Long userId, String documentCode, ExpenseDocumentUpdateDTO dto) {
        ProcessDocumentInstance instance = support.requireDocument(documentCode);
        support.requireSubmitter(instance, userId);
        String status = support.trimToNull(instance.getStatus());
        if (!Objects.equals(status, "DRAFT") && !Objects.equals(status, "REJECTED")) {
            throw new IllegalStateException("当前单据不是可重提状态");
        }
        AbstractExpenseDocumentSupport.DocumentMutationContext mutation =
                mutationApplySupport.buildMutationContext(instance, dto, true);
        return buildPreview(mutation.flowSnapshotJson(), mutation.runtimeContext());
    }

    void validateBeforeSubmit(String flowSnapshotJson, Map<String, Object> runtimeContext) {
        ExpenseManualApproverPreviewVO preview = buildPreview(flowSnapshotJson, runtimeContext);
        Map<String, List<Long>> normalizedSelections = support.normalizeManualApproverSelections(
                toSelectionMap(runtimeContext == null ? null : runtimeContext.get("manualApproverSelections"))
        );
        for (ExpenseManualApproverPreviewNodeVO node : preview.getManualNodes()) {
            List<Long> selectedUserIds = normalizedSelections.getOrDefault(node.getNodeKey(), Collections.emptyList());
            if (selectedUserIds.isEmpty()) {
                throw new IllegalStateException("请先为节点【" + defaultText(node.getNodeName(), node.getNodeKey()) + "】选择审批人");
            }
            Set<Long> candidateUserIds = node.getCandidateOptions().stream()
                    .map(ProcessFormOptionVO::getValue)
                    .map(this::asLong)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (!candidateUserIds.containsAll(selectedUserIds)) {
                throw new IllegalStateException("节点【" + defaultText(node.getNodeName(), node.getNodeKey()) + "】包含超出候选范围的审批人");
            }
        }
    }

    private ExpenseManualApproverPreviewVO buildPreview(String flowSnapshotJson, Map<String, Object> runtimeContext) {
        FlowRuntimeSnapshot snapshot = support.readFlowRuntimeSnapshot(flowSnapshotJson);
        ExpenseManualApproverPreviewVO result = new ExpenseManualApproverPreviewVO();
        Map<String, Object> flowSnapshotMap = support.readMap(flowSnapshotJson);
        Map<String, List<Long>> normalizedSelections = support.normalizeManualApproverSelections(
                toSelectionMap(runtimeContext == null ? null : runtimeContext.get("manualApproverSelections"))
        );
        ProcessFlowNodeDTO resumeNode = resolveResumeNode(snapshot, runtimeContext);
        if (resumeNode != null) {
            collectMatchedNodes(
                    snapshot,
                    runtimeContext,
                    flowSnapshotMap,
                    normalizedSelections,
                    resumeNode.getParentNodeKey(),
                    snapshot.indexInContainer(resumeNode.getParentNodeKey(), resumeNode.getNodeKey()),
                    result
            );
        } else {
            collectMatchedNodes(snapshot, runtimeContext, flowSnapshotMap, normalizedSelections, null, 0, result);
        }
        return result;
    }

    private void collectMatchedNodes(
            FlowRuntimeSnapshot snapshot,
            Map<String, Object> runtimeContext,
            Map<String, Object> flowSnapshotMap,
            Map<String, List<Long>> normalizedSelections,
            String containerKey,
            int startIndex,
            ExpenseManualApproverPreviewVO result
    ) {
        collectContainer(snapshot, runtimeContext, flowSnapshotMap, normalizedSelections, containerKey, startIndex, result);
        ProcessFlowRouteDTO route = snapshot.routeByKey(containerKey);
        if (route == null) {
            return;
        }
        ProcessFlowNodeDTO branchNode = snapshot.node(route.getSourceNodeKey());
        if (branchNode == null) {
            return;
        }
        boolean branchHasAttachedTail = snapshot.routes(route.getSourceNodeKey()).stream()
                .anyMatch(item -> Boolean.TRUE.equals(item.getAttachBelowNodes()));
        if (!branchHasAttachedTail || Boolean.TRUE.equals(route.getAttachBelowNodes())) {
            collectMatchedNodes(
                    snapshot,
                    runtimeContext,
                    flowSnapshotMap,
                    normalizedSelections,
                    branchNode.getParentNodeKey(),
                    nextIndex(snapshot, branchNode),
                    result
            );
            return;
        }
        String parentContainerKey = branchNode.getParentNodeKey();
        collectMatchedNodes(
                snapshot,
                runtimeContext,
                flowSnapshotMap,
                normalizedSelections,
                parentContainerKey,
                snapshot.children(parentContainerKey).size(),
                result
        );
    }

    private void collectContainer(
            FlowRuntimeSnapshot snapshot,
            Map<String, Object> runtimeContext,
            Map<String, Object> flowSnapshotMap,
            Map<String, List<Long>> normalizedSelections,
            String containerKey,
            int startIndex,
            ExpenseManualApproverPreviewVO result
    ) {
        List<ProcessFlowNodeDTO> nodes = snapshot.children(containerKey);
        for (int index = startIndex; index < nodes.size(); index++) {
            ProcessFlowNodeDTO node = nodes.get(index);
            String nodeType = defaultText(asText(node.getNodeType()), "");
            switch (nodeType) {
                case NODE_TYPE_BRANCH -> {
                    ProcessFlowRouteDTO matchedRoute = expenseWorkflowRuntimeSupport.previewMatchedRoute(
                            snapshot.routes(node.getNodeKey()),
                            runtimeContext
                    );
                    if (matchedRoute == null) {
                        throw new IllegalStateException("节点【" + defaultText(node.getNodeName(), node.getNodeKey()) + "】未命中任何分支条件");
                    }
                    result.getApprovalTimeline().add(buildTimelineItem(
                            "route-" + matchedRoute.getRouteKey(),
                            node,
                            NODE_TYPE_BRANCH,
                            "命中分支",
                            "命中分支：" + defaultText(matchedRoute.getRouteName(), matchedRoute.getRouteKey())
                    ));
                    collectMatchedNodes(
                            snapshot,
                            runtimeContext,
                            flowSnapshotMap,
                            normalizedSelections,
                            matchedRoute.getRouteKey(),
                            0,
                            result
                    );
                    return;
                }
                case NODE_TYPE_APPROVAL -> {
                    if (isManualSelectApprovalNode(node)) {
                        ExpenseManualApproverPreviewNodeVO manualNode = buildManualNode(
                                node,
                                flowSnapshotMap,
                                normalizedSelections.getOrDefault(node.getNodeKey(), Collections.emptyList())
                        );
                        result.getManualNodes().add(manualNode);
                        result.getApprovalTimeline().add(buildTimelineItem(
                                "manual-" + node.getNodeKey(),
                                node,
                                NODE_TYPE_APPROVAL,
                                manualNode.getSelectedUserIds().isEmpty() ? "待选择审批人" : "已完成手动选人",
                                manualNode.getSelectedUserIds().isEmpty()
                                        ? "提交前需要为该节点指定审批人"
                                        : "已选审批人：" + resolveSelectedLabels(manualNode)
                        ));
                        continue;
                    }
                    List<String> approverNames = expenseWorkflowRuntimeSupport.previewResolvedApprovers(node, runtimeContext).stream()
                            .map(user -> defaultText(support.resolveUserDisplayName(user, null), String.valueOf(user.getId())))
                            .distinct()
                            .toList();
                    result.getApprovalTimeline().add(buildTimelineItem(
                            "approval-" + node.getNodeKey(),
                            node,
                            NODE_TYPE_APPROVAL,
                            "审批节点",
                            approverNames.isEmpty() ? "提交后按节点规则解析审批人" : "审批人：" + String.join("、", approverNames)
                    ));
                    continue;
                }
                case NODE_TYPE_CC -> {
                    result.getApprovalTimeline().add(buildTimelineItem(
                            "cc-" + node.getNodeKey(),
                            node,
                            NODE_TYPE_CC,
                            "抄送节点",
                            "当前轨迹会经过该抄送节点"
                    ));
                    continue;
                }
                case NODE_TYPE_PAYMENT -> {
                    result.getApprovalTimeline().add(buildTimelineItem(
                            "payment-" + node.getNodeKey(),
                            node,
                            NODE_TYPE_PAYMENT,
                            "支付节点",
                            "当前轨迹会进入支付处理"
                    ));
                    continue;
                }
                default -> {
                }
            }
        }
    }

    private ExpenseManualApproverPreviewNodeVO buildManualNode(
            ProcessFlowNodeDTO node,
            Map<String, Object> flowSnapshotMap,
            List<Long> selectedUserIds
    ) {
        ExpenseManualApproverPreviewNodeVO item = new ExpenseManualApproverPreviewNodeVO();
        item.setNodeKey(node.getNodeKey());
        item.setNodeName(node.getNodeName());
        item.setNodeType(node.getNodeType());
        item.setCandidateOptions(resolveManualCandidateOptions(node, flowSnapshotMap));
        item.setSelectedUserIds(new ArrayList<>(selectedUserIds));
        return item;
    }

    private List<ProcessFormOptionVO> resolveManualCandidateOptions(ProcessFlowNodeDTO node, Map<String, Object> flowSnapshotMap) {
        Map<String, Object> config = node == null || node.getConfig() == null ? Collections.emptyMap() : node.getConfig();
        Map<String, Object> manualSelectConfig = toObjectMap(config.get("manualSelectConfig"));
        String candidateScope = defaultText(asText(manualSelectConfig.get("candidateScope")), CANDIDATE_SCOPE_ALL_ACTIVE_USERS);
        if (!Objects.equals(candidateScope, CANDIDATE_SCOPE_ALL_ACTIVE_USERS)) {
            return support.loadUserOptions(flowSnapshotMap);
        }
        return support.loadUserOptions(flowSnapshotMap);
    }

    private ExpenseApprovalTimelineItemVO buildTimelineItem(
            String key,
            ProcessFlowNodeDTO node,
            String nodeType,
            String statusLabel,
            String description
    ) {
        ExpenseApprovalTimelineItemVO item = new ExpenseApprovalTimelineItemVO();
        item.setKey(key);
        item.setNodeKey(node == null ? null : node.getNodeKey());
        item.setNodeName(node == null ? null : node.getNodeName());
        item.setNodeType(nodeType);
        item.setTitle(defaultText(node == null ? null : node.getNodeName(), "未命名节点"));
        item.setStatusLabel(statusLabel);
        item.setDescription(description);
        item.setPending(true);
        return item;
    }

    private boolean isManualSelectApprovalNode(ProcessFlowNodeDTO node) {
        if (node == null || !Objects.equals(node.getNodeType(), NODE_TYPE_APPROVAL)) {
            return false;
        }
        Map<String, Object> config = node.getConfig() == null ? Collections.emptyMap() : node.getConfig();
        return Objects.equals(
                defaultText(asText(config.get("approverType")), APPROVER_TYPE_MANAGER),
                APPROVER_TYPE_MANUAL_SELECT
        );
    }

    private ProcessFlowNodeDTO resolveResumeNode(FlowRuntimeSnapshot snapshot, Map<String, Object> context) {
        String resumeNodeKey = asText(context == null ? null : context.get("resumeNodeKey"));
        return resumeNodeKey == null ? null : snapshot.node(resumeNodeKey);
    }

    private String resolveSelectedLabels(ExpenseManualApproverPreviewNodeVO node) {
        if (node.getSelectedUserIds().isEmpty() || node.getCandidateOptions().isEmpty()) {
            return "";
        }
        Map<Long, String> labelMap = node.getCandidateOptions().stream()
                .map(item -> Map.entry(asLong(item.getValue()), item.getLabel()))
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        return node.getSelectedUserIds().stream()
                .map(labelMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("、"));
    }

    private Map<String, List<Long>> toSelectionMap(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Collections.emptyMap();
        }
        Map<String, List<Long>> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String nodeKey = asText(entry.getKey());
            if (nodeKey == null) {
                continue;
            }
            List<Long> userIds = toLongList(entry.getValue());
            if (!userIds.isEmpty()) {
                result.put(nodeKey, userIds);
            }
        }
        return result;
    }

    private Map<String, Object> toObjectMap(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private List<Long> toLongList(Object value) {
        if (value instanceof List<?> items) {
            return items.stream()
                    .map(this::asLong)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }
        Long parsed = asLong(value);
        return parsed == null ? Collections.emptyList() : List.of(parsed);
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private int nextIndex(FlowRuntimeSnapshot snapshot, ProcessFlowNodeDTO node) {
        return snapshot.indexInContainer(node.getParentNodeKey(), node.getNodeKey()) + 1;
    }
}
