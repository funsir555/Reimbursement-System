package com.finex.auth.service.impl;

import com.finex.auth.dto.CompanyVO;
import com.finex.auth.dto.DepartmentTreeNodeVO;
import com.finex.auth.dto.FinanceDepartmentArchiveMetaVO;
import com.finex.auth.dto.FinanceDepartmentQueryDTO;
import com.finex.auth.dto.FinanceDepartmentVO;
import com.finex.auth.service.SystemSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceDepartmentArchiveServiceImplTest {

    @Mock
    private SystemSettingsService systemSettingsService;

    private FinanceDepartmentArchiveServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FinanceDepartmentArchiveServiceImpl(systemSettingsService);
    }

    @Test
    void getMetaReturnsSharedDepartmentsAndStatusOptions() {
        when(systemSettingsService.listDepartments()).thenReturn(List.of(buildRootDepartment()));

        FinanceDepartmentArchiveMetaVO meta = service.getMeta();

        assertEquals(1, meta.getDepartments().size());
        assertEquals(2, meta.getStatusOptions().size());
        assertEquals("1", meta.getStatusOptions().get(0).getValue());
        assertEquals("启用", meta.getStatusOptions().get(0).getLabel());
    }

    @Test
    void queryDepartmentsFlattensTreeAndFillsParentAndCompanyNames() {
        when(systemSettingsService.listDepartments()).thenReturn(List.of(buildRootDepartment()));
        when(systemSettingsService.listCompanies()).thenReturn(List.of(buildCompany()));

        List<FinanceDepartmentVO> result = service.queryDepartments(new FinanceDepartmentQueryDTO());

        assertEquals(2, result.size());
        assertEquals("财务中心", result.get(0).getDeptName());
        assertEquals("费用管理部", result.get(1).getDeptName());
        assertEquals("财务中心", result.get(1).getParentDeptName());
        assertEquals("广州测试公司", result.get(1).getCompanyName());
    }

    @Test
    void queryDepartmentsSupportsKeywordParentAndStatusFilters() {
        when(systemSettingsService.listDepartments()).thenReturn(List.of(buildRootDepartment()));
        when(systemSettingsService.listCompanies()).thenReturn(List.of(buildCompany()));

        FinanceDepartmentQueryDTO query = new FinanceDepartmentQueryDTO();
        query.setKeyword("李经理");
        query.setParentId(10L);
        query.setStatus(1);

        List<FinanceDepartmentVO> result = service.queryDepartments(query);

        assertEquals(1, result.size());
        assertEquals("D002", result.get(0).getDeptCode());
    }

    @Test
    void queryDepartmentsDoesNotApplyFinanceCompanyIsolation() {
        DepartmentTreeNodeVO root = buildRootDepartment();
        DepartmentTreeNodeVO shared = new DepartmentTreeNodeVO();
        shared.setId(20L);
        shared.setDeptCode("D003");
        shared.setDeptName("共享部门");
        shared.setCompanyId("COMPANY_B");
        shared.setStatus(1);
        root.getChildren().add(shared);

        CompanyVO companyA = buildCompany();
        CompanyVO companyB = new CompanyVO();
        companyB.setCompanyId("COMPANY_B");
        companyB.setCompanyName("深圳测试公司");

        when(systemSettingsService.listDepartments()).thenReturn(List.of(root));
        when(systemSettingsService.listCompanies()).thenReturn(List.of(companyA, companyB));

        List<FinanceDepartmentVO> result = service.queryDepartments(new FinanceDepartmentQueryDTO());

        assertTrue(result.stream().anyMatch(item -> "COMPANY_A".equals(item.getCompanyId())));
        assertTrue(result.stream().anyMatch(item -> "COMPANY_B".equals(item.getCompanyId())));
        assertFalse(result.isEmpty());
    }

    private CompanyVO buildCompany() {
        CompanyVO company = new CompanyVO();
        company.setCompanyId("COMPANY_A");
        company.setCompanyName("广州测试公司");
        return company;
    }

    private DepartmentTreeNodeVO buildRootDepartment() {
        DepartmentTreeNodeVO root = new DepartmentTreeNodeVO();
        root.setId(10L);
        root.setDeptCode("D001");
        root.setDeptName("财务中心");
        root.setCompanyId("COMPANY_A");
        root.setLeaderUserId(100L);
        root.setLeaderName("张总");
        root.setSyncSource("MANUAL");
        root.setSyncManaged(false);
        root.setSyncEnabled(true);
        root.setStatus(1);
        root.setSortOrder(1);

        DepartmentTreeNodeVO child = new DepartmentTreeNodeVO();
        child.setId(11L);
        child.setDeptCode("D002");
        child.setDeptName("费用管理部");
        child.setParentId(10L);
        child.setCompanyId("COMPANY_A");
        child.setLeaderUserId(101L);
        child.setLeaderName("李经理");
        child.setSyncSource("WECOM");
        child.setSyncManaged(true);
        child.setSyncEnabled(true);
        child.setStatus(1);
        child.setSortOrder(2);
        child.setStatDepartmentBelong("财务");
        child.setStatRegionBelong("华南");
        child.setStatAreaBelong("广州");

        root.setChildren(new ArrayList<>(List.of(child)));
        return root;
    }
}
