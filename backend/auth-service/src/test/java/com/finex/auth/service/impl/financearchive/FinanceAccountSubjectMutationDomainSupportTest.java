package com.finex.auth.service.impl.financearchive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSubjectCloseDTO;
import com.finex.auth.dto.FinanceAccountSubjectDetailVO;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
        when(financeAccountSubjectMapper.selectOne(any())).thenReturn(null);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(parent));

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
    void createSubjectAutoDerivesControlledFields() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setStatus(1);

        FinanceAccountSubject parent = subject("COMPANY_A", "2202", null, 1);
        parent.setSubjectName("应付账款");
        parent.setBalanceDirection("CREDIT");
        parent.setSubjectCategory("LIABILITY");
        parent.setLeafFlag(1);

        FinanceAccountSubject created = subject("COMPANY_A", "220201", "2202", 2);
        created.setSubjectName("应付账款-子科目");
        created.setBalanceDirection("CREDIT");
        created.setSubjectCategory("LIABILITY");
        created.setLeafFlag(1);
        created.setCclassany("LIABILITY");
        created.setBproperty(0);
        created.setCbookType("GENERAL");

        FinanceAccountSubject inserted = new FinanceAccountSubject();

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(financeAccountSubjectMapper.selectOne(any())).thenReturn(null).thenReturn(created);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(parent)).thenReturn(List.of());
        when(financeAccountSubjectMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            FinanceAccountSubject payload = invocation.getArgument(0);
            inserted.setParentSubjectCode(payload.getParentSubjectCode());
            inserted.setSubjectLevel(payload.getSubjectLevel());
            inserted.setSubjectCategory(payload.getSubjectCategory());
            inserted.setBalanceDirection(payload.getBalanceDirection());
            inserted.setLeafFlag(payload.getLeafFlag());
            inserted.setCclassany(payload.getCclassany());
            inserted.setBproperty(payload.getBproperty());
            inserted.setCbookType(payload.getCbookType());
            return 1;
        }).when(financeAccountSubjectMapper).insert(any(FinanceAccountSubject.class));

        FinanceAccountSubjectSaveDTO dto = new FinanceAccountSubjectSaveDTO();
        dto.setSubjectCode("220201");
        dto.setSubjectName("应付账款-子科目");
        dto.setParentSubjectCode("1001");
        dto.setSubjectCategory("ASSET");
        dto.setLeafFlag(0);
        dto.setBproperty(1);
        dto.setCclassany("ASSET");
        dto.setCbookType("CASH");

        FinanceAccountSubjectDetailVO detail = support.createSubject("COMPANY_A", dto, "tester");

        assertEquals("2202", inserted.getParentSubjectCode());
        assertEquals(2, inserted.getSubjectLevel());
        assertEquals("LIABILITY", inserted.getSubjectCategory());
        assertEquals("CREDIT", inserted.getBalanceDirection());
        assertEquals(1, inserted.getLeafFlag());
        assertEquals("LIABILITY", inserted.getCclassany());
        assertEquals(0, inserted.getBproperty());
        assertEquals("GENERAL", inserted.getCbookType());
        assertEquals("LIABILITY", detail.getSubjectCategory());
        assertEquals("CREDIT", detail.getBalanceDirection());
        assertEquals(0, parent.getLeafFlag());
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

    @Test
    void updateSubjectIgnoresClientControlledFieldsAndKeepsAuthorityValues() {
        FinanceAccountSubject parent = subject("COMPANY_A", "6602", null, 1);
        parent.setSubjectName("管理费用");
        parent.setSubjectCategory("PROFIT");
        parent.setBalanceDirection("DEBIT");

        FinanceAccountSubject existing = subject("COMPANY_A", "660201", "6602", 2);
        existing.setSubjectName("办公费");
        existing.setSubjectCategory("PROFIT");
        existing.setBalanceDirection("DEBIT");
        existing.setCclassany("PROFIT");
        existing.setBproperty(1);
        existing.setCbookType("GENERAL");
        existing.setLeafFlag(1);

        when(financeAccountSubjectMapper.selectOne(any())).thenReturn(existing).thenReturn(parent).thenReturn(existing);
        when(financeAccountSubjectMapper.selectCount(any())).thenReturn(0L);

        FinanceAccountSubjectSaveDTO dto = new FinanceAccountSubjectSaveDTO();
        dto.setSubjectCode("660201");
        dto.setSubjectName("办公费-更新");
        dto.setParentSubjectCode("1001");
        dto.setSubjectLevel(99);
        dto.setSubjectCategory("ASSET");
        dto.setLeafFlag(0);
        dto.setBproperty(0);
        dto.setCclassany("ASSET");
        dto.setCbookType("CASH");

        FinanceAccountSubjectDetailVO detail = support.updateSubject("COMPANY_A", "660201", dto, "tester");

        assertEquals("6602", existing.getParentSubjectCode());
        assertEquals(2, existing.getSubjectLevel());
        assertEquals("PROFIT", existing.getSubjectCategory());
        assertEquals("DEBIT", existing.getBalanceDirection());
        assertEquals(1, existing.getLeafFlag());
        assertEquals("PROFIT", existing.getCclassany());
        assertEquals(1, existing.getBproperty());
        assertEquals("GENERAL", existing.getCbookType());
        assertEquals("PROFIT", detail.getSubjectCategory());
        assertEquals("DEBIT", detail.getBalanceDirection());
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
