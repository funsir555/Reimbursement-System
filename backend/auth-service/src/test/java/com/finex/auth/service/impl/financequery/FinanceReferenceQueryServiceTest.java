package com.finex.auth.service.impl.financequery;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.mapper.GlAccvouchMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinanceReferenceQueryServiceTest {

    private final GlAccvouchMapper mapper = mock(GlAccvouchMapper.class);
    private final FinanceReferenceQueryService service = new FinanceReferenceQueryService(mapper);

    @Test
    void preservesExistingAllCompanyOptionSetAndBoundsResult() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), GlAccvouch.class);
        when(mapper.selectObjs(any())).thenReturn(List.of(" 6601 ", "6601", "2202"));

        assertThat(service.listDistinctAccountCodes(999)).containsExactly("6601", "2202");

        ArgumentCaptor<LambdaQueryWrapper<GlAccvouch>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectObjs(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("limit 200").doesNotContain("company_id");
    }

    @Test
    void clampsLowerBoundWithoutChangingSelection() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), GlAccvouch.class);
        when(mapper.selectObjs(any())).thenReturn(List.of());

        assertThat(service.listDistinctAccountCodes(0)).isEmpty();

        ArgumentCaptor<LambdaQueryWrapper<GlAccvouch>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(mapper).selectObjs(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("limit 1");
    }
}
