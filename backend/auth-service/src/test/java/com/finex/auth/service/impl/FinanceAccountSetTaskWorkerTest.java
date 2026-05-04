package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.entity.FinanceAccountSetTemplateSubject;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.FinanceAccountSetCodeRuleMapper;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateSubjectMapper;
import com.finex.auth.mapper.FinanceCashFlowItemMapper;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceAccountSetTaskWorkerTest {

    @Mock
    private AsyncTaskRecordMapper asyncTaskRecordMapper;
    @Mock
    private FinanceAccountSetMapper financeAccountSetMapper;
    @Mock
    private FinanceAccountSetCodeRuleMapper financeAccountSetCodeRuleMapper;
    @Mock
    private FinanceAccountSetTemplateMapper financeAccountSetTemplateMapper;
    @Mock
    private FinanceAccountSetTemplateSubjectMapper financeAccountSetTemplateSubjectMapper;
    @Mock
    private FinanceCashFlowItemMapper financeCashFlowItemMapper;
    @Mock
    private FinanceAccountSubjectMapper financeAccountSubjectMapper;
    @Mock
    private SystemCompanyMapper systemCompanyMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private TransactionTemplate transactionTemplate;

    private FinanceAccountSetTaskWorker worker;

    @BeforeEach
    void setUp() {
        worker = new FinanceAccountSetTaskWorker(
                asyncTaskRecordMapper,
                financeAccountSetMapper,
                financeAccountSetCodeRuleMapper,
                financeAccountSetTemplateMapper,
                financeAccountSetTemplateSubjectMapper,
                financeCashFlowItemMapper,
                financeAccountSubjectMapper,
                systemCompanyMapper,
                userMapper,
                notificationService,
                new ObjectMapper(),
                transactionTemplate
        );
    }

    @Test
    void buildBlankSubjectsRecalculatesLeafFlagsFromActualHierarchy() {
        FinanceAccountSetTemplateSubject parent = templateSubject("ROOT", null, 1, "1001", 1, 1);
        FinanceAccountSetTemplateSubject child = templateSubject("CHILD", "ROOT", 2, "01", 0, 0);

        when(financeAccountSetTemplateSubjectMapper.selectList(any())).thenReturn(List.of(parent, child));

        @SuppressWarnings("unchecked")
        List<FinanceAccountSubject> subjects = (List<FinanceAccountSubject>) ReflectionTestUtils.invokeMethod(
                worker,
                "buildBlankSubjects",
                "TPL-001",
                "COMP-001",
                "4-2-2-2"
        );

        assertEquals(2, subjects.size());
        assertEquals(0, findSubject(subjects, "1001").getLeafFlag());
        assertEquals(1, findSubject(subjects, "100101").getLeafFlag());
    }

    @Test
    void copyReferenceSubjectsRecalculatesLeafFlagsFromActualHierarchy() {
        FinanceAccountSubject parent = subject("SRC-001", "1002", null, 1, 1);
        FinanceAccountSubject child = subject("SRC-001", "100201", "1002", 2, 0);

        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(parent, child));

        @SuppressWarnings("unchecked")
        List<FinanceAccountSubject> subjects = (List<FinanceAccountSubject>) ReflectionTestUtils.invokeMethod(
                worker,
                "copyReferenceSubjects",
                "SRC-001",
                "TGT-001",
                "TPL-001"
        );

        assertEquals(2, subjects.size());
        assertEquals(0, findSubject(subjects, "1002").getLeafFlag());
        assertEquals(1, findSubject(subjects, "100201").getLeafFlag());
    }

    private FinanceAccountSetTemplateSubject templateSubject(
            String subjectKey,
            String parentKey,
            int subjectLevel,
            String segment,
            int leafFlag,
            int sortOrder
    ) {
        FinanceAccountSetTemplateSubject subject = new FinanceAccountSetTemplateSubject();
        subject.setTemplateCode("TPL-001");
        subject.setSubjectKey(subjectKey);
        subject.setParentSubjectKey(parentKey);
        subject.setSubjectLevel(subjectLevel);
        subject.setLevelSegment(segment);
        subject.setSubjectName(subjectKey);
        subject.setBalanceDirection("DEBIT");
        subject.setSubjectCategory("ASSET");
        subject.setLeafFlag(leafFlag);
        subject.setStatus(1);
        subject.setSortOrder(sortOrder);
        return subject;
    }

    private FinanceAccountSubject subject(
            String companyId,
            String subjectCode,
            String parentSubjectCode,
            int subjectLevel,
            int leafFlag
    ) {
        FinanceAccountSubject subject = new FinanceAccountSubject();
        subject.setCompanyId(companyId);
        subject.setSubjectCode(subjectCode);
        subject.setParentSubjectCode(parentSubjectCode);
        subject.setSubjectLevel(subjectLevel);
        subject.setSubjectName(subjectCode);
        subject.setBalanceDirection("DEBIT");
        subject.setSubjectCategory("ASSET");
        subject.setLeafFlag(leafFlag);
        subject.setStatus(1);
        subject.setSortOrder(Integer.parseInt(subjectCode));
        return subject;
    }

    private FinanceAccountSubject findSubject(List<FinanceAccountSubject> subjects, String subjectCode) {
        return subjects.stream()
                .filter(subject -> subjectCode.equals(subject.getSubjectCode()))
                .findFirst()
                .orElseThrow();
    }
}
