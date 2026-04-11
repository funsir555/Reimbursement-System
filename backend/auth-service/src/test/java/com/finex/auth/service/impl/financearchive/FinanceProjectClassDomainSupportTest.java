package com.finex.auth.service.impl.financearchive;

import com.finex.auth.dto.FinanceProjectClassSaveDTO;
import com.finex.auth.dto.FinanceProjectStatusDTO;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceProjectClassDomainSupportTest {

    @Mock
    private FinanceProjectClassMapper financeProjectClassMapper;

    @Mock
    private FinanceProjectArchiveMapper financeProjectArchiveMapper;

    @Mock
    private SystemCompanyMapper systemCompanyMapper;

    @Mock
    private GlAccvouchMapper glAccvouchMapper;

    private FinanceProjectClassDomainSupport support;

    @BeforeEach
    void setUp() {
        support = new FinanceProjectClassDomainSupport(
                financeProjectClassMapper,
                financeProjectArchiveMapper,
                systemCompanyMapper,
                glAccvouchMapper
        );
    }

    @Test
    void updateProjectClassRejectsCodeChangeWhenProjectsExist() {
        FinanceProjectClass existing = new FinanceProjectClass();
        existing.setCompanyId("COMPANY_A");
        existing.setProjectClassCode("01");
        existing.setProjectClassName("研发项目");

        FinanceProjectClassSaveDTO dto = new FinanceProjectClassSaveDTO();
        dto.setProjectClassCode("02");
        dto.setProjectClassName("交付项目");

        when(financeProjectClassMapper.selectOne(any())).thenReturn(existing);
        when(financeProjectArchiveMapper.selectCount(any())).thenReturn(1L);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                support.updateProjectClass("COMPANY_A", "01", dto, "tester")
        );

        assertEquals("当前项目分类已被项目档案引用，不能修改分类编码", exception.getMessage());
    }

    @Test
    void updateProjectClassStatusRejectsDisableWhenActiveProjectsExist() {
        FinanceProjectClass existing = new FinanceProjectClass();
        existing.setCompanyId("COMPANY_A");
        existing.setProjectClassCode("01");
        existing.setStatus(1);

        FinanceProjectStatusDTO dto = new FinanceProjectStatusDTO();
        dto.setStatus(0);

        when(financeProjectClassMapper.selectOne(any())).thenReturn(existing);
        when(financeProjectArchiveMapper.selectCount(any())).thenReturn(1L);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                support.updateProjectClassStatus("COMPANY_A", "01", dto, "tester")
        );

        assertEquals("当前项目分类下仍存在启用中的项目档案，请先停用项目后再停用分类", exception.getMessage());
    }
}