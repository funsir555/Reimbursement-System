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

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(financeAccountSubjectMapper.selectOne(any()))
                .thenReturn(null)
                .thenReturn(parent)
                .thenReturn(created);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of());
        when(financeAccountSubjectMapper.selectCount(any())).thenReturn(0L);

        FinanceAccountSubjectSaveDTO dto = new FinanceAccountSubjectSaveDTO();
        dto.setSubjectCode("100101");
        dto.setSubjectName("???");
        dto.setParentSubjectCode("1001");
        dto.setSubjectCategory("ASSET");

        assertEquals(2, service.createSubject("COMPANY_A", dto, "tester").getSubjectLevel());
    }

    @Test
    void createChildSubjectRejectsCodeOutsideParentPrefix() {
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

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(financeAccountSubjectMapper.selectOne(any()))
                .thenReturn(null)
                .thenReturn(parent);

        FinanceAccountSubjectSaveDTO dto = new FinanceAccountSubjectSaveDTO();
        dto.setSubjectCode("220201");
        dto.setSubjectName("?????");
        dto.setParentSubjectCode("1001");
        dto.setSubjectCategory("ASSET");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.createSubject("COMPANY_A", dto, "tester")
        );
        assertEquals("子科目编码必须以前级科目编码为前缀", exception.getMessage());
    }
}
