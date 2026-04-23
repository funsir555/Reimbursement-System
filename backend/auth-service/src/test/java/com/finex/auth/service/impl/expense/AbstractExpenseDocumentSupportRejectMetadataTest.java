package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFlowNodeDTO;
import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.mapper.ProcessDocumentActionLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractExpenseDocumentSupportRejectMetadataTest {

    @Mock
    private ProcessDocumentActionLogMapper processDocumentActionLogMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AbstractExpenseDocumentSupport support;

    @BeforeEach
    void setUp() {
        support = mock(AbstractExpenseDocumentSupport.class, Answers.CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(support, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(support, "processDocumentActionLogMapper", processDocumentActionLogMapper);
    }

    @Test
    void rejectToAnyNodeWithoutDirectReachDoesNotCreateResumeNode() throws Exception {
        ProcessDocumentInstance instance = createInstance(List.of("REJECT_TO_ANY_NODE"));
        mockRejectLog("approval-current", Map.of("targetNodeKey", "approval-upstream"));

        Map<String, Object> metadata = invokeResolveRejectRuntimeMetadata(instance);

        assertEquals("approval-current", metadata.get("latestRejectNodeKey"));
        assertEquals("approval-upstream", metadata.get("latestRejectTargetNodeKey"));
        assertFalse(metadata.containsKey("resumeNodeKey"));
        assertNull(metadata.get("resumeNodeKey"));
    }

    @Test
    void rejectToAnyNodeWithDirectReachAfterAnyRejectResumesAtRejectedNode() throws Exception {
        ProcessDocumentInstance instance = createInstance(List.of("REJECT_TO_ANY_NODE", "DIRECT_REACH_AFTER_ANY_REJECT"));
        mockRejectLog("approval-current", Map.of("targetNodeKey", "approval-upstream"));

        Map<String, Object> metadata = invokeResolveRejectRuntimeMetadata(instance);

        assertEquals("approval-current", metadata.get("latestRejectNodeKey"));
        assertEquals("approval-upstream", metadata.get("latestRejectTargetNodeKey"));
        assertEquals("approval-current", metadata.get("resumeNodeKey"));
    }

    @Test
    void directReachAfterResubmitWithoutTargetStillResumesAtRejectedNode() throws Exception {
        ProcessDocumentInstance instance = createInstance(List.of("DIRECT_REACH_AFTER_RESUBMIT"));
        mockRejectLog("approval-current", Map.of());

        Map<String, Object> metadata = invokeResolveRejectRuntimeMetadata(instance);

        assertEquals("approval-current", metadata.get("latestRejectNodeKey"));
        assertNull(metadata.get("latestRejectTargetNodeKey"));
        assertEquals("approval-current", metadata.get("resumeNodeKey"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeResolveRejectRuntimeMetadata(ProcessDocumentInstance instance) {
        return (Map<String, Object>) ReflectionTestUtils.invokeMethod(support, "resolveRejectRuntimeMetadata", instance);
    }

    private ProcessDocumentInstance createInstance(List<String> specialSettings) throws Exception {
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-REJECT-001");
        instance.setFlowSnapshotJson(objectMapper.writeValueAsString(Map.of(
                "nodes", List.of(
                        createApprovalNode("approval-current", specialSettings),
                        createApprovalNode("approval-upstream", List.of())
                ),
                "routes", List.of()
        )));
        return instance;
    }

    private ProcessFlowNodeDTO createApprovalNode(String nodeKey, List<String> specialSettings) {
        ProcessFlowNodeDTO node = new ProcessFlowNodeDTO();
        node.setNodeKey(nodeKey);
        node.setNodeType("APPROVAL");
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("specialSettings", specialSettings);
        node.setConfig(config);
        return node;
    }

    private void mockRejectLog(String nodeKey, Map<String, Object> payload) throws Exception {
        ProcessDocumentActionLog rejectLog = new ProcessDocumentActionLog();
        rejectLog.setActionType("REJECT");
        rejectLog.setNodeKey(nodeKey);
        rejectLog.setPayloadJson(objectMapper.writeValueAsString(payload));
        when(processDocumentActionLogMapper.selectList(any())).thenReturn(List.of(rejectLog));
    }
}
