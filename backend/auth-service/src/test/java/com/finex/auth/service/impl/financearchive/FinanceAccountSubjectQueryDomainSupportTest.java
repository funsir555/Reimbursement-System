package com.finex.auth.service.impl.financearchive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSubjectDetailVO;
import com.finex.auth.dto.FinanceAccountSubjectSummaryVO;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceAccountSubjectQueryDomainSupportTest {

    @Mock
    private FinanceAccountSubjectMapper financeAccountSubjectMapper;
    @Mock
    private SystemCompanyMapper systemCompanyMapper;
    @Mock
    private GlAccvouchMapper glAccvouchMapper;

    private FinanceAccountSubjectQueryDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new FinanceAccountSubjectQueryDomainSupport(
                financeAccountSubjectMapper,
                systemCompanyMapper,
                glAccvouchMapper,
                new ObjectMapper()
        );
    }

    @Test
    void listSubjectsBuildsHierarchyAndChildrenFlags() {
        FinanceAccountSubject root = subject("COMPANY_A", "1001", null, 1);
        FinanceAccountSubject child = subject("COMPANY_A", "100101", "1001", 2);

        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(root, child));
        when(financeAccountSubjectMapper.selectCount(any())).thenReturn(1L, 0L);

        List<FinanceAccountSubjectSummaryVO> result = support.listSubjects("COMPANY_A", null, null, null, null);

        assertEquals(1, result.size());
        assertEquals("1001", result.get(0).getSubjectCode());
        assertTrue(result.get(0).getHasChildren());
        assertEquals(1, result.get(0).getChildren().size());
        assertEquals("100101", result.get(0).getChildren().get(0).getSubjectCode());
        assertFalse(result.get(0).getChildren().get(0).getHasChildren());
    }

    @Test
    void getSubjectDetailPreservesDetailReadModel() {
        FinanceAccountSubject subject = subject("COMPANY_A", "1001", null, 1);
        subject.setId(9L);
        subject.setSubjectName("库存现金");
        subject.setChelp("1001");

        when(financeAccountSubjectMapper.selectOne(any())).thenReturn(subject);
        when(financeAccountSubjectMapper.selectCount(any())).thenReturn(0L);

        FinanceAccountSubjectDetailVO detail = support.getSubjectDetail("COMPANY_A", "1001");

        assertEquals(9L, detail.getId());
        assertEquals("COMPANY_A", detail.getCompanyId());
        assertEquals("1001", detail.getSubjectCode());
        assertEquals("库存现金", detail.getSubjectName());
        assertFalse(detail.getHasChildren());
    }

    private FinanceAccountSubject subject(String companyId, String subjectCode, String parentCode, int level) {
        FinanceAccountSubject subject = new FinanceAccountSubject();
        subject.setCompanyId(companyId);
        subject.setSubjectCode(subjectCode);
        subject.setParentSubjectCode(parentCode);
        subject.setSubjectLevel(level);
        subject.setSubjectName(subjectCode);
        subject.setBalanceDirection("DEBIT");
        subject.setSubjectCategory("ASSET");
        subject.setStatus(1);
        subject.setBclose(0);
        subject.setLeafFlag(parentCode == null ? 0 : 1);
        return subject;
    }
}
