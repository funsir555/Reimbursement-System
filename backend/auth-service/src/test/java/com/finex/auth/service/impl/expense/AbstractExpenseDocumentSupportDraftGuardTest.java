package com.finex.auth.service.impl.expense;

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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractExpenseDocumentSupportDraftGuardTest {

    @Mock
    private ProcessDocumentActionLogMapper processDocumentActionLogMapper;

    private AbstractExpenseDocumentSupport support;

    @BeforeEach
    void setUp() {
        support = mock(AbstractExpenseDocumentSupport.class, Answers.CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(support, "processDocumentActionLogMapper", processDocumentActionLogMapper);
    }

    @Test
    void assertCanViewDocumentBlocksCrossViewUsersFromDraftDocuments() {
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setStatus("DRAFT");
        instance.setSubmitterUserId(1L);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> support.assertCanViewDocument(instance, 2L, true)
        );

        assertEquals("你无权查看该单据", error.getMessage());
    }

    @Test
    void resolveDisplaySubmittedAtUsesLatestSubmitLogForResubmittedDocuments() {
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-1");
        instance.setStatus("PENDING_APPROVAL");
        instance.setCreatedAt(LocalDateTime.of(2026, 4, 1, 8, 0));
        ProcessDocumentActionLog resubmitLog = new ProcessDocumentActionLog();
        resubmitLog.setCreatedAt(LocalDateTime.of(2026, 4, 20, 9, 30));
        when(processDocumentActionLogMapper.selectOne(any())).thenReturn(resubmitLog);

        LocalDateTime submittedAt = ReflectionTestUtils.invokeMethod(support, "resolveDisplaySubmittedAt", instance);

        assertEquals(LocalDateTime.of(2026, 4, 20, 9, 30), submittedAt);
    }

    @Test
    void resolveDisplaySubmittedAtKeepsDraftOnLatestUpdatedTime() {
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setDocumentCode("DOC-2");
        instance.setStatus("DRAFT");
        instance.setCreatedAt(LocalDateTime.of(2026, 4, 5, 8, 0));
        instance.setUpdatedAt(LocalDateTime.of(2026, 4, 21, 12, 5));

        LocalDateTime submittedAt = ReflectionTestUtils.invokeMethod(support, "resolveDisplaySubmittedAt", instance);

        assertEquals(LocalDateTime.of(2026, 4, 21, 12, 5), submittedAt);
    }
}
