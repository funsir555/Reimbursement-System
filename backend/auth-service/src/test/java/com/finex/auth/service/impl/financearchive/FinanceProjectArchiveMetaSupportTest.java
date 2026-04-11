package com.finex.auth.service.impl.financearchive;

import com.finex.auth.dto.FinanceProjectArchiveMetaVO;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceProjectArchiveMetaSupportTest {

    @Mock
    private FinanceProjectClassMapper financeProjectClassMapper;

    @Mock
    private FinanceProjectArchiveMapper financeProjectArchiveMapper;

    @Mock
    private SystemCompanyMapper systemCompanyMapper;

    @Mock
    private GlAccvouchMapper glAccvouchMapper;

    private FinanceProjectArchiveMetaSupport support;

    @BeforeEach
    void setUp() {
        support = new FinanceProjectArchiveMetaSupport(
                financeProjectClassMapper,
                financeProjectArchiveMapper,
                systemCompanyMapper,
                glAccvouchMapper
        );
    }

    @Test
    void getMetaReturnsEnabledProjectClassOptions() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setStatus(1);

        FinanceProjectClass projectClass = new FinanceProjectClass();
        projectClass.setCompanyId("COMPANY_A");
        projectClass.setProjectClassCode("01");
        projectClass.setProjectClassName("研发项目");
        projectClass.setStatus(1);

        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(financeProjectClassMapper.selectList(any())).thenReturn(List.of(projectClass));

        FinanceProjectArchiveMetaVO result = support.getMeta("COMPANY_A");

        assertEquals(2, result.getStatusOptions().size());
        assertEquals(2, result.getCloseStatusOptions().size());
        assertEquals(1, result.getProjectClassOptions().size());
        assertEquals("01 / 研发项目", result.getProjectClassOptions().get(0).getLabel());
    }
}