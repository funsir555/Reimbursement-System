package com.finex.auth.service.impl;

import com.finex.auth.dto.FinanceProjectClassSaveDTO;
import com.finex.auth.dto.FinanceProjectSaveDTO;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceProjectArchiveServiceImplTest {

    @Mock
    private FinanceProjectClassMapper financeProjectClassMapper;

    @Mock
    private FinanceProjectArchiveMapper financeProjectArchiveMapper;

    @Mock
    private SystemCompanyMapper systemCompanyMapper;

    @Mock
    private GlAccvouchMapper glAccvouchMapper;

    private FinanceProjectArchiveServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FinanceProjectArchiveServiceImpl(
                financeProjectClassMapper,
                financeProjectArchiveMapper,
                systemCompanyMapper,
                glAccvouchMapper
        );
    }

    @Test
    void createProjectClassInitializesEnabledRecord() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setStatus(1);

        FinanceProjectClass created = new FinanceProjectClass();
        created.setCompanyId("COMPANY_A");
        created.setProjectClassCode("CLASS001");
        created.setProjectClassName("研发项目");
        created.setStatus(1);

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(financeProjectClassMapper.selectOne(any())).thenReturn(null, created);
        when(financeProjectClassMapper.selectCount(any())).thenReturn(0L);
        when(financeProjectArchiveMapper.selectCount(any())).thenReturn(0L);

        FinanceProjectClassSaveDTO dto = new FinanceProjectClassSaveDTO();
        dto.setProjectClassCode("CLASS001");
        dto.setProjectClassName("研发项目");

        assertEquals("CLASS001", service.createProjectClass("COMPANY_A", dto, "tester").getProjectClassCode());
    }

    @Test
    void updateProjectRejectsControlledClassChangeWhenReferenced() {
        FinanceProjectArchive existing = new FinanceProjectArchive();
        existing.setCompanyId("COMPANY_A");
        existing.setCitemcode("PROJ001");
        existing.setCitemname("项目一");
        existing.setCitemccode("CLASS001");
        existing.setStatus(1);
        existing.setBclose(0);
        existing.setIotherused(1);

        FinanceProjectClass newClass = new FinanceProjectClass();
        newClass.setCompanyId("COMPANY_A");
        newClass.setProjectClassCode("CLASS002");
        newClass.setProjectClassName("交付项目");
        newClass.setStatus(1);

        when(financeProjectArchiveMapper.selectOne(any())).thenReturn(existing);
        when(financeProjectClassMapper.selectOne(any())).thenReturn(newClass);

        FinanceProjectSaveDTO dto = new FinanceProjectSaveDTO();
        dto.setCitemcode("PROJ001");
        dto.setCitemname("项目一");
        dto.setCitemccode("CLASS002");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                service.updateProject("COMPANY_A", "PROJ001", dto, "tester")
        );
        assertEquals("当前项目已被引用，不能修改受控字段", exception.getMessage());
    }
}
