package com.finex.auth.service.impl.financearchive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSubjectCloseDTO;
import com.finex.auth.dto.FinanceAccountSubjectSaveDTO;
import com.finex.auth.dto.FinanceAccountSubjectStatusDTO;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceAccountSubjectMutationDomainSupportTest {

    @Mock
    private FinanceAccountSubjectMapper financeAccountSubjectMapper;
    @Mock
    private SystemCompanyMapper systemCompanyMapper;
    @Mock
    private GlAccvouchMapper glAccvouchMapper;

    private FinanceAccountSubjectMutationDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new FinanceAccountSubjectMutationDomainSupport(
                financeAccountSubjectMapper,
                systemCompanyMapper,
                glAccvouchMapper,
                new ObjectMapper()
        );
    }

    @Test
    void createSubjectRejectsDisabledParent() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setStatus(1);

        FinanceAccountSubject parent = subject("COMPANY_A", "1001", null, 1);
        parent.setStatus(0);

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(financeAccountSubjectMapper.selectOne(any())).thenReturn(null).thenReturn(parent);

        FinanceAccountSubjectSaveDTO dto = new FinanceAccountSubjectSaveDTO();
        dto.setSubjectCode("100101");
        dto.setSubjectName("库存现金-子科目");
        dto.setParentSubjectCode("1001");
        dto.setSubjectCategory("ASSET");

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                support.createSubject("COMPANY_A", dto, "tester")
        );
        assertEquals("上级科目未启用或已封存，不能新增子科目", error.getMessage());
    }

    @Test
    void updateSubjectRejectsControlledFieldChangeWhenVoucherReferenced() {
        FinanceAccountSubject existing = subject("COMPANY_A", "1001", null, 1);
        existing.setSubjectName("库存现金");
        existing.setBperson(0);

        when(financeAccountSubjectMapper.selectOne(any())).thenReturn(existing).thenReturn(existing);
        when(glAccvouchMapper.selectCount(any())).thenReturn(1L);
        when(financeAccountSubjectMapper.selectCount(any())).thenReturn(0L);

        FinanceAccountSubjectSaveDTO dto = new FinanceAccountSubjectSaveDTO();
        dto.setSubjectCode("1001");
        dto.setSubjectName("库存现金");
        dto.setSubjectCategory("ASSET");
        dto.setBperson(1);

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                support.updateSubject("COMPANY_A", "1001", dto, "tester")
        );
        assertEquals("当前科目已被总账引用，不能修改受控属性", error.getMessage());
    }

    @Test
    void updateStatusRejectsDisableWhenEnabledChildrenRemain() {
        FinanceAccountSubject subject = subject("COMPANY_A", "1001", null, 1);

        when(financeAccountSubjectMapper.selectOne(any())).thenReturn(subject);
        when(financeAccountSubjectMapper.selectCount(any())).thenReturn(1L);

        FinanceAccountSubjectStatusDTO dto = new FinanceAccountSubjectStatusDTO();
        dto.setStatus(0);

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                support.updateStatus("COMPANY_A", "1001", dto, "tester")
        );
        assertEquals("请先停用下级科目，再停用当前科目", error.getMessage());
    }

    @Test
    void updateCloseStatusRejectsCloseWhenOpenChildrenRemain() {
        FinanceAccountSubject subject = subject("COMPANY_A", "1001", null, 1);

        when(financeAccountSubjectMapper.selectOne(any())).thenReturn(subject);
        when(financeAccountSubjectMapper.selectCount(any())).thenReturn(1L);

        FinanceAccountSubjectCloseDTO dto = new FinanceAccountSubjectCloseDTO();
        dto.setBclose(1);

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                support.updateCloseStatus("COMPANY_A", "1001", dto, "tester")
        );
        assertEquals("请先封存下级科目，再封存当前科目", error.getMessage());
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
