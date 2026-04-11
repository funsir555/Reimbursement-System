package com.finex.auth.service.impl.financearchive;

import com.finex.auth.dto.FinanceProjectDetailVO;
import com.finex.auth.dto.FinanceProjectSummaryVO;
import com.finex.auth.entity.FinanceProjectArchive;
import com.finex.auth.entity.FinanceProjectClass;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
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
class FinanceProjectQueryDomainSupportTest {

    @Mock
    private FinanceProjectClassMapper financeProjectClassMapper;

    @Mock
    private FinanceProjectArchiveMapper financeProjectArchiveMapper;

    @Mock
    private SystemCompanyMapper systemCompanyMapper;

    @Mock
    private GlAccvouchMapper glAccvouchMapper;

    private FinanceProjectQueryDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new FinanceProjectQueryDomainSupport(
                financeProjectClassMapper,
                financeProjectArchiveMapper,
                systemCompanyMapper,
                glAccvouchMapper
        );
    }

    @Test
    void listProjectsMapsClassNameAndVoucherReference() {
        FinanceProjectClass projectClass = new FinanceProjectClass();
        projectClass.setCompanyId("COMPANY_A");
        projectClass.setProjectClassCode("01");
        projectClass.setProjectClassName("研发项目");

        FinanceProjectArchive project = new FinanceProjectArchive();
        project.setCompanyId("COMPANY_A");
        project.setCitemcode("000001");
        project.setCitemname("项目一");
        project.setCitemccode("01");
        project.setStatus(1);
        project.setBclose(0);

        when(financeProjectClassMapper.selectList(any())).thenReturn(List.of(projectClass));
        when(financeProjectArchiveMapper.selectList(any())).thenReturn(List.of(project));
        when(glAccvouchMapper.selectCount(any())).thenReturn(1L);

        List<FinanceProjectSummaryVO> result = support.listProjects("COMPANY_A", "项目", "01", 1, 0);

        assertEquals(1, result.size());
        assertEquals("研发项目", result.get(0).getProjectClassName());
        assertTrue(result.get(0).getReferencedByVoucher());
    }

    @Test
    void getProjectDetailResolvesClassName() {
        FinanceProjectArchive project = new FinanceProjectArchive();
        project.setCompanyId("COMPANY_A");
        project.setCitemcode("000001");
        project.setCitemname("项目一");
        project.setCitemccode("01");
        project.setStatus(1);
        project.setBclose(0);

        FinanceProjectClass projectClass = new FinanceProjectClass();
        projectClass.setCompanyId("COMPANY_A");
        projectClass.setProjectClassCode("01");
        projectClass.setProjectClassName("研发项目");
        projectClass.setStatus(1);

        when(financeProjectArchiveMapper.selectOne(any())).thenReturn(project);
        when(financeProjectClassMapper.selectOne(any())).thenReturn(projectClass);
        when(glAccvouchMapper.selectCount(any())).thenReturn(0L);

        FinanceProjectDetailVO result = support.getProjectDetail("COMPANY_A", "000001");

        assertEquals("研发项目", result.getProjectClassName());
        assertFalse(result.getReferencedByVoucher());
    }
}