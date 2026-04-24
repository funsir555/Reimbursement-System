package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.ProcessDocumentActionLogMapper;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.UserMapper;
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

    @Mock
    private ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;

    @Mock
    private UserMapper userMapper;

    private AbstractExpenseDocumentSupport support;

    @BeforeEach
    void setUp() {
        support = mock(AbstractExpenseDocumentSupport.class, Answers.CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(support, "processDocumentActionLogMapper", processDocumentActionLogMapper);
        ReflectionTestUtils.setField(support, "processDocumentExpenseDetailMapper", processDocumentExpenseDetailMapper);
        ReflectionTestUtils.setField(support, "userMapper", userMapper);
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

        assertEquals("\u4f60\u65e0\u6743\u67e5\u770b\u8be5\u5355\u636e", error.getMessage());
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

    @Test
    void requireExpenseDetailUsesReadableChineseMessage() {
        when(processDocumentExpenseDetailMapper.selectOne(any())).thenReturn(null);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> support.requireExpenseDetail("DOC-1", "D001")
        );

        assertEquals("\u5f53\u524d\u8d39\u7528\u660e\u7ec6\u4e0d\u5b58\u5728", error.getMessage());
    }

    @Test
    void requireSubmitterUsesReadableChineseMessage() {
        ProcessDocumentInstance instance = new ProcessDocumentInstance();
        instance.setSubmitterUserId(1L);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> support.requireSubmitter(instance, 2L)
        );

        assertEquals("\u53ea\u6709\u63d0\u5355\u4eba\u672c\u4eba\u53ef\u4ee5\u64cd\u4f5c\u5f53\u524d\u5355\u636e", error.getMessage());
    }

    @Test
    void requireCurrentUserCompanyIdUsesReadableChineseMessage() {
        User user = new User();
        user.setId(1L);
        user.setStatus(1);
        user.setCompanyId(" ");
        when(userMapper.selectById(1L)).thenReturn(user);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(support, "requireCurrentUserCompanyId", 1L)
        );

        assertEquals("\u5f53\u524d\u7528\u6237\u672a\u914d\u7f6e\u6240\u5c5e\u516c\u53f8\uff0c\u65e0\u6cd5\u7ee7\u7eed\u5904\u7406", error.getMessage());
    }
}
