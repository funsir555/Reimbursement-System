package com.finex.auth.service.impl.financearchive;

import com.finex.auth.dto.FinanceProjectCloseDTO;
import com.finex.auth.dto.FinanceProjectDetailVO;
import com.finex.auth.dto.FinanceProjectSaveDTO;
import com.finex.auth.dto.FinanceProjectStatusDTO;
import com.finex.auth.entity.FinanceProjectArchive;
import com.finex.auth.entity.FinanceProjectClass;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceProjectMutationDomainSupportTest {

    @Mock
    private FinanceProjectClassMapper financeProjectClassMapper;

    @Mock
    private FinanceProjectArchiveMapper financeProjectArchiveMapper;

    @Mock
    private SystemCompanyMapper systemCompanyMapper;

    @Mock
    private GlAccvouchMapper glAccvouchMapper;

    private FinanceProjectMutationDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new FinanceProjectMutationDomainSupport(
                financeProjectClassMapper,
                financeProjectArchiveMapper,
                systemCompanyMapper,
                glAccvouchMapper
        );
    }

    @Test
    void createProjectInitializesEnabledUnclosedRecord() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setStatus(1);

        FinanceProjectClass projectClass = new FinanceProjectClass();
        projectClass.setCompanyId("COMPANY_A");
        projectClass.setProjectClassCode("01");
        projectClass.setProjectClassName("研发项目");
        projectClass.setStatus(1);

        FinanceProjectArchive persisted = new FinanceProjectArchive();
        persisted.setCompanyId("COMPANY_A");
        persisted.setCitemcode("000001");
        persisted.setCitemname("项目一");
        persisted.setCitemccode("01");
        persisted.setStatus(1);
        persisted.setBclose(0);

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(financeProjectArchiveMapper.selectOne(any())).thenReturn(null, persisted);
        when(financeProjectClassMapper.selectOne(any())).thenReturn(projectClass);
        when(financeProjectArchiveMapper.selectCount(any())).thenReturn(0L);
        when(financeProjectArchiveMapper.insert(any(FinanceProjectArchive.class))).thenReturn(1);
        when(glAccvouchMapper.selectCount(any())).thenReturn(0L);

        FinanceProjectSaveDTO dto = new FinanceProjectSaveDTO();
        dto.setCitemcode("000001");
        dto.setCitemname("项目一");
        dto.setCitemccode("01");

        FinanceProjectDetailVO result = support.createProject("COMPANY_A", dto, "tester");

        ArgumentCaptor<FinanceProjectArchive> captor = ArgumentCaptor.forClass(FinanceProjectArchive.class);
        verify(financeProjectArchiveMapper).insert(captor.capture());
        FinanceProjectArchive inserted = captor.getValue();
        assertEquals("COMPANY_A", inserted.getCompanyId());
        assertEquals(1, inserted.getStatus());
        assertEquals(0, inserted.getBclose());
        assertNotNull(inserted.getCreatedAt());
        assertNotNull(inserted.getUpdatedAt());
        assertEquals("研发项目", result.getProjectClassName());
    }

    @Test
    void updateProjectStatusRequiresEnabledClassWhenEnabling() {
        FinanceProjectArchive existing = new FinanceProjectArchive();
        existing.setCompanyId("COMPANY_A");
        existing.setCitemcode("000001");
        existing.setCitemccode("01");
        existing.setStatus(0);

        FinanceProjectClass disabledClass = new FinanceProjectClass();
        disabledClass.setCompanyId("COMPANY_A");
        disabledClass.setProjectClassCode("01");
        disabledClass.setStatus(0);

        FinanceProjectStatusDTO dto = new FinanceProjectStatusDTO();
        dto.setStatus(1);

        when(financeProjectArchiveMapper.selectOne(any())).thenReturn(existing);
        when(financeProjectClassMapper.selectOne(any())).thenReturn(disabledClass);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                support.updateProjectStatus("COMPANY_A", "000001", dto, "tester")
        );

        assertEquals("项目分类未启用，不能作为正式项目来源", exception.getMessage());
    }

    @Test
    void updateProjectCloseStatusRejectsDirectUncloseForDisabledProject() {
        FinanceProjectArchive existing = new FinanceProjectArchive();
        existing.setCompanyId("COMPANY_A");
        existing.setCitemcode("000001");
        existing.setCitemccode("01");
        existing.setStatus(0);
        existing.setBclose(1);

        FinanceProjectClass projectClass = new FinanceProjectClass();
        projectClass.setCompanyId("COMPANY_A");
        projectClass.setProjectClassCode("01");
        projectClass.setStatus(1);

        FinanceProjectCloseDTO dto = new FinanceProjectCloseDTO();
        dto.setBclose(0);

        when(financeProjectArchiveMapper.selectOne(any())).thenReturn(existing);
        when(financeProjectClassMapper.selectOne(any())).thenReturn(projectClass);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                support.updateProjectCloseStatus("COMPANY_A", "000001", dto, "tester")
        );

        assertEquals("停用项目不能直接解封，请先启用后再解封", exception.getMessage());
    }
}