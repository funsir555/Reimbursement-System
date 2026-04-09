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
    void createProjectClassRejectsNonTwoDigitCode() {
        FinanceProjectClassSaveDTO dto = new FinanceProjectClassSaveDTO();
        dto.setProjectClassCode("A1");
        dto.setProjectClassName("市场项目");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.createProjectClass("COMPANY_A", dto, "tester")
        );
        assertEquals("项目分类编码必须为2位数字文本", exception.getMessage());
    }

    @Test
    void createProjectClassInitializesEnabledRecord() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setStatus(1);

        FinanceProjectClass created = new FinanceProjectClass();
        created.setCompanyId("COMPANY_A");
        created.setProjectClassCode("01");
        created.setProjectClassName("研发项目");
        created.setStatus(1);

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(financeProjectClassMapper.selectOne(any())).thenReturn(null, created);
        when(financeProjectClassMapper.selectCount(any())).thenReturn(0L);
        when(financeProjectArchiveMapper.selectCount(any())).thenReturn(0L);

        FinanceProjectClassSaveDTO dto = new FinanceProjectClassSaveDTO();
        dto.setProjectClassCode("01");
        dto.setProjectClassName("研发项目");

        assertEquals("01", service.createProjectClass("COMPANY_A", dto, "tester").getProjectClassCode());
    }

    @Test
    void createProjectRejectsNonSixDigitCode() {
        FinanceProjectSaveDTO dto = new FinanceProjectSaveDTO();
        dto.setCitemcode("P001");
        dto.setCitemname("项目一");
        dto.setCitemccode("01");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                service.createProject("COMPANY_A", dto, "tester")
        );
        assertEquals("项目编码必须为6位数字文本", exception.getMessage());
    }

    @Test
    void updateProjectRejectsControlledClassChangeWhenReferenced() {
        FinanceProjectArchive existing = new FinanceProjectArchive();
        existing.setCompanyId("COMPANY_A");
        existing.setCitemcode("000001");
        existing.setCitemname("项目一");
        existing.setCitemccode("01");
        existing.setStatus(1);
        existing.setBclose(0);
        existing.setIotherused(1);

        FinanceProjectClass newClass = new FinanceProjectClass();
        newClass.setCompanyId("COMPANY_A");
        newClass.setProjectClassCode("02");
        newClass.setProjectClassName("交付项目");
        newClass.setStatus(1);

        when(financeProjectArchiveMapper.selectOne(any())).thenReturn(existing);
        when(financeProjectClassMapper.selectOne(any())).thenReturn(newClass);

        FinanceProjectSaveDTO dto = new FinanceProjectSaveDTO();
        dto.setCitemcode("000001");
        dto.setCitemname("项目一");
        dto.setCitemccode("02");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                service.updateProject("COMPANY_A", "000001", dto, "tester")
        );
        assertEquals("当前项目已被引用，不能修改受控字段", exception.getMessage());
    }
}
