package com.finex.auth.service.impl.financearchive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSubjectMetaVO;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class FinanceAccountSubjectMetaSupportTest {

    @Mock
    private FinanceAccountSubjectMapper financeAccountSubjectMapper;
    @Mock
    private SystemCompanyMapper systemCompanyMapper;
    @Mock
    private GlAccvouchMapper glAccvouchMapper;

    private FinanceAccountSubjectMetaSupport support;

    @BeforeEach
    void setUp() {
        support = new FinanceAccountSubjectMetaSupport(
                financeAccountSubjectMapper,
                systemCompanyMapper,
                glAccvouchMapper,
                new ObjectMapper()
        );
    }

    @Test
    void getMetaReturnsStableOptionGroups() {
        FinanceAccountSubjectMetaVO meta = support.getMeta();

        assertEquals(5, meta.getSubjectCategoryOptions().size());
        assertEquals("ASSET", meta.getSubjectCategoryOptions().get(0).getValue());
        assertEquals(2, meta.getStatusOptions().size());
        assertEquals("1", meta.getStatusOptions().get(0).getValue());
        assertEquals(2, meta.getCloseStatusOptions().size());
        assertEquals(2, meta.getYesNoOptions().size());
    }
}
