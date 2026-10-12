package com.finex.auth.service.impl.postvoucher;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.GlAccass;
import com.finex.auth.entity.GlAccsum;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.mapper.GlAccassMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import java.math.BigDecimal;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostVoucherSignedMovementTest {

    @Test
    void postedRedDebitOffsetsDebitInSubjectAndAuxiliarySnapshots() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), GlAccsum.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), GlAccass.class);
        GlAccsumMapper sumMapper = mock(GlAccsumMapper.class);
        GlAccassMapper assMapper = mock(GlAccassMapper.class);
        SharedPostVoucherSupport support = new SharedPostVoucherSupport(
                null, null, null, null, null, null, null, sumMapper, assMapper, null, null, null);

        FinanceAccountSubject subject = new FinanceAccountSubject();
        subject.setSubjectCode("560101");
        GlAccvouch voucher = new GlAccvouch();
        voucher.setCompanyId("TEST");
        voucher.setIyear(2022);
        voucher.setIperiod(12);
        voucher.setCcode("560101");
        voucher.setCurrencyCode("USD");
        voucher.setCdeptId("D1");
        voucher.setMd(new BigDecimal("-2273513.79"));
        voucher.setMc(BigDecimal.ZERO);
        voucher.setMdF(new BigDecimal("-100.00"));
        voucher.setMcF(BigDecimal.ZERO);
        voucher.setNdS(new BigDecimal("-4.500000"));

        GlAccsum sum = new GlAccsum();
        sum.setMb(BigDecimal.ZERO);
        sum.setMbF(BigDecimal.ZERO);
        sum.setNbS(BigDecimal.ZERO);
        sum.setMd(new BigDecimal("50.00"));
        sum.setMc(BigDecimal.ZERO);
        sum.setMdF(new BigDecimal("5.00"));
        sum.setMcF(BigDecimal.ZERO);
        sum.setNdS(new BigDecimal("5.000000"));
        sum.setNcS(BigDecimal.ZERO);
        GlAccass ass = new GlAccass();
        ass.setMb(BigDecimal.ZERO);
        ass.setMbF(BigDecimal.ZERO);
        ass.setNbS(BigDecimal.ZERO);
        ass.setMd(new BigDecimal("50.00"));
        ass.setMc(BigDecimal.ZERO);
        ass.setMdF(new BigDecimal("5.00"));
        ass.setMcF(BigDecimal.ZERO);
        ass.setNdS(new BigDecimal("5.000000"));
        ass.setNcS(BigDecimal.ZERO);
        when(sumMapper.selectOne(any())).thenReturn(sum);
        when(assMapper.selectOne(any())).thenReturn(ass);

        support.applyToAccsum(voucher, subject);
        support.applyToAccass(voucher, subject);

        assertEquals(new BigDecimal("-2273463.79"), sum.getMd());
        assertEquals(new BigDecimal("0.00"), sum.getMc());
        assertEquals(new BigDecimal("-95.00"), sum.getMdF());
        assertEquals(new BigDecimal("0.00"), sum.getMcF());
        assertEquals(new BigDecimal("0.500000"), sum.getNdS());
        assertEquals(new BigDecimal("-2273463.79"), sum.getMe());
        assertEquals(sum.getMd(), ass.getMd());
        assertEquals(sum.getMc(), ass.getMc());
        assertEquals(sum.getMdF(), ass.getMdF());
        assertEquals(sum.getNdS(), ass.getNdS());
        verify(sumMapper).updateById(sum);
        verify(assMapper).updateById(ass);
    }
}
