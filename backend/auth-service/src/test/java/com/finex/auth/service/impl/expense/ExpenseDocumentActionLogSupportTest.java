package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.mapper.ProcessDocumentActionLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseDocumentActionLogSupportTest {

    @Mock
    private ProcessDocumentActionLogMapper processDocumentActionLogMapper;
    @Mock
    private ObjectMapper objectMapper;

    @Test
    void appendLogPersistsNormalizedPayload() throws Exception {
        ExpenseDocumentActionLogSupport actionLogSupport = new ExpenseDocumentActionLogSupport(processDocumentActionLogMapper, objectMapper);
        Map<String, Object> payload = Map.of("taskId", 1L);
        when(objectMapper.writeValueAsString(payload)).thenReturn("{\"taskId\":1}");

        actionLogSupport.appendLog("DOC-1", "NODE-1", "Approve", "COMMENT", 1L, "tester", "ok", payload);

        verify(processDocumentActionLogMapper).insert(org.mockito.ArgumentMatchers.argThat(log ->
                "DOC-1".equals(log.getDocumentCode())
                        && "NODE-1".equals(log.getNodeKey())
                        && "Approve".equals(log.getNodeName())
                        && "COMMENT".equals(log.getActionType())
                        && Long.valueOf(1L).equals(log.getActorUserId())
                        && "tester".equals(log.getActorName())
                        && "ok".equals(log.getActionComment())
                        && "{\"taskId\":1}".equals(log.getPayloadJson())
                        && log.getCreatedAt() != null
        ));
    }

    @Test
    void loadActionLogsReturnsOrderedLogsFromMapper() {
        ExpenseDocumentActionLogSupport actionLogSupport = new ExpenseDocumentActionLogSupport(processDocumentActionLogMapper, objectMapper);
        ProcessDocumentActionLog log = new ProcessDocumentActionLog();
        log.setDocumentCode("DOC-1");
        when(processDocumentActionLogMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(log));

        List<ProcessDocumentActionLog> actual = actionLogSupport.loadActionLogs("DOC-1");

        org.junit.jupiter.api.Assertions.assertEquals(1, actual.size());
        org.junit.jupiter.api.Assertions.assertSame(log, actual.get(0));
        verify(processDocumentActionLogMapper).selectList(org.mockito.ArgumentMatchers.any());
    }
}
