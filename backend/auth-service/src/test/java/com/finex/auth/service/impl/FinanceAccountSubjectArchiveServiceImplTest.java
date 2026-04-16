package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSubjectSaveDTO;
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
class FinanceAccountSubjectArchiveServiceImplTest {

    @Mock
    private FinanceAccountSubjectMapper financeAccountSubjectMapper;

    @Mock
    private SystemCompanyMapper systemCompanyMapper;

    @Mock
    private GlAccvouchMapper glAccvouchMapper;

    private FinanceAccountSubjectArchiveServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FinanceAccountSubjectArchiveServiceImpl(
                financeAccountSubjectMapper,
                systemCompanyMapper,
                glAccvouchMapper,
                new ObjectMapper()
        );
    }

    @Test
    void createChildSubjectDerivesLevelAndParentPrefix() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setStatus(1);

        FinanceAccountSubject parent = new FinanceAccountSubject();
        parent.setCompanyId("COMPANY_A");
        parent.setSubjectCode("1001");
        parent.setSubjectName("????");
        parent.setSubjectLevel(1);
        parent.setStatus(1);
        parent.setBclose(0);
        parent.setLeafFlag(1);
        parent.setBalanceDirection("DEBIT");
        parent.setSubjectCategory("ASSET");

        FinanceAccountSubject inserted = new FinanceAccountSubject();
        FinanceAccountSubject created = new FinanceAccountSubject();
        created.setCompanyId("COMPANY_A");
        created.setSubjectCode("100101");
        created.setSubjectName("???");
        created.setSubjectLevel(2);
        created.setStatus(1);
        created.setBclose(0);
        created.setLeafFlag(1);
        created.setBalanceDirection("DEBIT");
        created.setSubjectCategory("ASSET");
        created.setParentSubjectCode("1001");
        created.setCclassany("ASSET");
        created.setBproperty(1);
        created.setCbookType("CASH");

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(financeAccountSubjectMapper.selectOne(any()))
                .thenReturn(null)
                .thenReturn(created);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(parent)).thenReturn(List.of());
        when(financeAccountSubjectMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            FinanceAccountSubject payload = invocation.getArgument(0);
            inserted.setCompanyId(payload.getCompanyId());
            inserted.setSubjectCode(payload.getSubjectCode());
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
        dto.setSubjectCode("100101");
        dto.setSubjectName("???");
        dto.setParentSubjectCode("1001");
        dto.setSubjectCategory("ASSET");

        assertEquals(2, service.createSubject("COMPANY_A", dto, "tester").getSubjectLevel());
        assertEquals("1001", inserted.getParentSubjectCode());
        assertEquals("ASSET", inserted.getSubjectCategory());
        assertEquals("DEBIT", inserted.getBalanceDirection());
        assertEquals(1, inserted.getLeafFlag());
        assertEquals("ASSET", inserted.getCclassany());
        assertEquals(1, inserted.getBproperty());
        assertEquals("CASH", inserted.getCbookType());
        assertEquals(0, parent.getLeafFlag());
    }

    @Test
    void createSubjectRejectsWhenNoParentPrefixMatched() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setStatus(1);

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(financeAccountSubjectMapper.selectOne(any()))
                .thenReturn(null);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of());

        FinanceAccountSubjectSaveDTO dto = new FinanceAccountSubjectSaveDTO();
        dto.setSubjectCode("990001");
        dto.setSubjectName("未匹配科目");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.createSubject("COMPANY_A", dto, "tester")
        );
        assertEquals("请先创建上级科目，再新增当前科目", exception.getMessage());
    }
}
