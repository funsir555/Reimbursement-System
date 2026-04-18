package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSubjectDerivedDefaultsVO;
import com.finex.auth.dto.FinanceAccountSubjectSaveDTO;
import com.finex.auth.entity.FinanceAccountSet;
import com.finex.auth.entity.FinanceAccountSetTemplateSubject;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateSubjectMapper;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Mock
    private FinanceAccountSetMapper financeAccountSetMapper;

    @Mock
    private FinanceAccountSetTemplateSubjectMapper financeAccountSetTemplateSubjectMapper;

    private FinanceAccountSubjectArchiveServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FinanceAccountSubjectArchiveServiceImpl(
                financeAccountSubjectMapper,
                systemCompanyMapper,
                glAccvouchMapper,
                new ObjectMapper(),
                financeAccountSetMapper,
                financeAccountSetTemplateSubjectMapper
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
        parent.setSubjectName("cash");
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
        created.setSubjectName("cash-subject");
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
        dto.setSubjectName("cash-subject");
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
    void createRootSubjectUsesTemplateDerivedDefaults() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setStatus(1);

        FinanceAccountSet accountSet = new FinanceAccountSet();
        accountSet.setCompanyId("COMPANY_A");
        accountSet.setTemplateCode("AS_2007_ENTERPRISE");

        FinanceAccountSetTemplateSubject templateSubject = new FinanceAccountSetTemplateSubject();
        templateSubject.setTemplateCode("AS_2007_ENTERPRISE");
        templateSubject.setSubjectLevel(1);
        templateSubject.setLevelSegment("5401");
        templateSubject.setSubjectCategory("PROFIT");
        templateSubject.setBalanceDirection("DEBIT");

        FinanceAccountSubject inserted = new FinanceAccountSubject();
        FinanceAccountSubject created = new FinanceAccountSubject();
        created.setCompanyId("COMPANY_A");
        created.setSubjectCode("5401");
        created.setSubjectName("business-cost");
        created.setSubjectLevel(1);
        created.setStatus(1);
        created.setBclose(0);
        created.setLeafFlag(1);
        created.setBalanceDirection("DEBIT");
        created.setSubjectCategory("PROFIT");
        created.setCclassany("PROFIT");
        created.setBproperty(1);
        created.setCbookType("GENERAL");

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(financeAccountSetMapper.selectById("COMPANY_A")).thenReturn(accountSet);
        when(financeAccountSetTemplateSubjectMapper.selectList(any())).thenReturn(List.of(templateSubject));
        when(financeAccountSubjectMapper.selectOne(any())).thenReturn(null).thenReturn(created);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of());
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
        dto.setSubjectCode("5401");
        dto.setSubjectName("business-cost");

        assertEquals(1, service.createSubject("COMPANY_A", dto, "tester").getSubjectLevel());
        assertNull(inserted.getParentSubjectCode());
        assertEquals(1, inserted.getSubjectLevel());
        assertEquals("PROFIT", inserted.getSubjectCategory());
        assertEquals("DEBIT", inserted.getBalanceDirection());
        assertEquals(1, inserted.getLeafFlag());
        assertEquals("PROFIT", inserted.getCclassany());
        assertEquals(1, inserted.getBproperty());
        assertEquals("GENERAL", inserted.getCbookType());
    }

    @Test
    void getDerivedDefaultsSupportsTemplateExactRootMatch() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setStatus(1);

        FinanceAccountSet accountSet = new FinanceAccountSet();
        accountSet.setCompanyId("COMPANY_A");
        accountSet.setTemplateCode("AS_2007_ENTERPRISE");

        FinanceAccountSetTemplateSubject templateSubject = new FinanceAccountSetTemplateSubject();
        templateSubject.setTemplateCode("AS_2007_ENTERPRISE");
        templateSubject.setSubjectLevel(1);
        templateSubject.setLevelSegment("6602");
        templateSubject.setSubjectCategory("PROFIT");
        templateSubject.setBalanceDirection("DEBIT");

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(financeAccountSetMapper.selectById("COMPANY_A")).thenReturn(accountSet);
        when(financeAccountSetTemplateSubjectMapper.selectList(any())).thenReturn(List.of(templateSubject));

        FinanceAccountSubjectDerivedDefaultsVO defaults = service.getDerivedDefaults("COMPANY_A", "6602");

        assertNull(defaults.getParentSubjectCode());
        assertEquals(1, defaults.getSubjectLevel());
        assertEquals("PROFIT", defaults.getSubjectCategory());
        assertEquals("DEBIT", defaults.getBalanceDirection());
        assertEquals(1, defaults.getLeafFlag());
        assertEquals("TEMPLATE_EXACT", defaults.getMatchedBy());
    }

    @Test
    void getDerivedDefaultsSupportsTemplatePrefixFallback() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setStatus(1);

        FinanceAccountSet accountSet = new FinanceAccountSet();
        accountSet.setCompanyId("COMPANY_A");
        accountSet.setTemplateCode("AS_2007_ENTERPRISE");

        FinanceAccountSetTemplateSubject templateSubject = new FinanceAccountSetTemplateSubject();
        templateSubject.setTemplateCode("AS_2007_ENTERPRISE");
        templateSubject.setSubjectLevel(1);
        templateSubject.setLevelSegment("6602");
        templateSubject.setSubjectCategory("PROFIT");
        templateSubject.setBalanceDirection("DEBIT");

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(financeAccountSetMapper.selectById("COMPANY_A")).thenReturn(accountSet);
        when(financeAccountSetTemplateSubjectMapper.selectList(any())).thenReturn(List.of(templateSubject));

        FinanceAccountSubjectDerivedDefaultsVO defaults = service.getDerivedDefaults("COMPANY_A", "6608");

        assertNull(defaults.getParentSubjectCode());
        assertEquals(1, defaults.getSubjectLevel());
        assertEquals("PROFIT", defaults.getSubjectCategory());
        assertEquals("DEBIT", defaults.getBalanceDirection());
        assertEquals(1, defaults.getLeafFlag());
        assertEquals("TEMPLATE_PREFIX", defaults.getMatchedBy());
    }

    @Test
    void getDerivedDefaultsReturnsExistingParentForChildSubject() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setStatus(1);

        FinanceAccountSubject parent = new FinanceAccountSubject();
        parent.setCompanyId("COMPANY_A");
        parent.setSubjectCode("1122");
        parent.setSubjectName("accounts-receivable");
        parent.setSubjectLevel(1);
        parent.setStatus(1);
        parent.setBclose(0);
        parent.setLeafFlag(0);
        parent.setBalanceDirection("DEBIT");
        parent.setSubjectCategory("ASSET");

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(parent));

        FinanceAccountSubjectDerivedDefaultsVO defaults = service.getDerivedDefaults("COMPANY_A", "112203");

        assertEquals("1122", defaults.getParentSubjectCode());
        assertEquals(2, defaults.getSubjectLevel());
        assertEquals("ASSET", defaults.getSubjectCategory());
        assertEquals("DEBIT", defaults.getBalanceDirection());
        assertEquals(1, defaults.getLeafFlag());
        assertEquals("EXISTING_PARENT", defaults.getMatchedBy());
    }

    @Test
    void createSubjectRejectsWhenNoParentPrefixMatched() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setStatus(1);

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(financeAccountSubjectMapper.selectOne(any())).thenReturn(null);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of());

        FinanceAccountSubjectSaveDTO dto = new FinanceAccountSubjectSaveDTO();
        dto.setSubjectCode("990001");
        dto.setSubjectName("unmatched-subject");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.createSubject("COMPANY_A", dto, "tester")
        );
        assertEquals("\u8bf7\u5148\u521b\u5efa\u4e0a\u7ea7\u79d1\u76ee\uff0c\u518d\u65b0\u589e\u5f53\u524d\u79d1\u76ee", exception.getMessage());
    }
}
