package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finex.auth.dto.FinanceGeneralLedgerRollbackPeriodDTO;
import com.finex.auth.dto.FinanceGeneralLedgerRollbackPeriodResultVO;
import com.finex.auth.entity.FinanceAccountSet;
import com.finex.auth.entity.FinanceAccountSetModuleEnable;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.FinancePostVoucherState;
import com.finex.auth.entity.GlAccass;
import com.finex.auth.entity.GlAccsum;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSetModuleEnableMapper;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinancePeriodCloseLogMapper;
import com.finex.auth.mapper.FinancePeriodCloseMapper;
import com.finex.auth.mapper.FinancePostVoucherStateMapper;
import com.finex.auth.mapper.GlAccassMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceGeneralLedgerMaintenanceServiceImplTest {

    @Mock
    private SystemCompanyMapper systemCompanyMapper;
    @Mock
    private FinanceAccountSetMapper financeAccountSetMapper;
    @Mock
    private FinancePostVoucherStateMapper financePostVoucherStateMapper;
    @Mock
    private FinanceAccountSubjectMapper financeAccountSubjectMapper;
    @Mock
    private FinancePeriodCloseMapper financePeriodCloseMapper;
    @Mock
    private FinancePeriodCloseLogMapper financePeriodCloseLogMapper;
    @Mock
    private GlAccvouchMapper glAccvouchMapper;
    @Mock
    private GlAccsumMapper glAccsumMapper;
    @Mock
    private GlAccassMapper glAccassMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private FinanceAccountSetModuleEnableMapper financeAccountSetModuleEnableMapper;

    private final AtomicReference<FinancePostVoucherState> storedPostState = new AtomicReference<>();

    private FinanceGeneralLedgerMaintenanceServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), GlAccvouch.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), GlAccsum.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), GlAccass.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), FinancePostVoucherState.class);

        service = new FinanceGeneralLedgerMaintenanceServiceImpl(
                systemCompanyMapper,
                financeAccountSetMapper,
                financePostVoucherStateMapper,
                financeAccountSubjectMapper,
                financePeriodCloseMapper,
                financePeriodCloseLogMapper,
                glAccvouchMapper,
                glAccsumMapper,
                glAccassMapper,
                userMapper,
                financeAccountSetModuleEnableMapper
        );

        storedPostState.set(postState());

        when(systemCompanyMapper.selectOne(any())).thenReturn(company());
        when(financeAccountSetMapper.selectOne(any())).thenReturn(accountSet());
        when(financeAccountSetModuleEnableMapper.selectOne(any())).thenReturn(generalLedgerModule());
        when(financePeriodCloseMapper.selectOne(any())).thenReturn(null);
        when(financePostVoucherStateMapper.selectOne(any())).thenAnswer(invocation -> storedPostState.get());
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(subject("1001", null), subject("5601", null)));
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(
                voucherRow(1, 1, "5601", "88.00", "0.00"),
                voucherRow(1, 2, "1001", "0.00", "88.00")
        ));
        when(glAccsumMapper.selectList(any())).thenReturn(List.of(currentSum()), List.of(previousSum()));
        when(glAccassMapper.selectList(any())).thenReturn(List.of(currentAssist()), List.of(previousAssist()));
        lenient().when(glAccvouchMapper.update(eq(null), any())).thenReturn(2);
        lenient().when(glAccsumMapper.delete(any())).thenReturn(1);
        lenient().when(glAccassMapper.delete(any())).thenReturn(1);
        lenient().when(glAccsumMapper.insert(any())).thenReturn(1);
        lenient().when(glAccassMapper.insert(any())).thenReturn(1);
        lenient().when(financePeriodCloseLogMapper.insert(any())).thenReturn(1);
        doAnswer(invocation -> {
            FinancePostVoucherState entity = invocation.getArgument(0);
            storedPostState.set(entity);
            return 1;
        }).when(financePostVoucherStateMapper).updateById(any(FinancePostVoucherState.class));
    }

    @Test
    void rollbackPeriodReturnsReviewedToNotPostedBaseState() {
        FinanceGeneralLedgerRollbackPeriodResultVO result = service.rollbackPeriod(1L, "财务张三", request());

        assertEquals("COMP-001", result.getCompanyId());
        assertEquals(2022, result.getIyear());
        assertEquals(12, result.getIperiod());
        assertEquals(202212, result.getIyperiod());
        assertFalse(Boolean.TRUE.equals(result.getClosedPeriodDetected()));
        assertEquals(1, result.getRolledBackVoucherCount());
        assertEquals(1, result.getRebuildAccsumCount());
        assertEquals(1, result.getRebuildAccassCount());
        assertEquals("OPEN", result.getPeriodStatus());
        assertEquals("NOT_POSTED", result.getPostStatus());

        FinancePostVoucherState state = storedPostState.get();
        assertNotNull(state);
        assertEquals("NOT_POSTED", state.getStatus());
        assertEquals(0, state.getPostedVoucherCount());
        assertEquals("财务张三", state.getLastPostedBy());
        assertEquals("ROLLBACK_SUCCESS", state.getLastTaskStatus());

        ArgumentCaptor<GlAccsum> sumCaptor = ArgumentCaptor.forClass(GlAccsum.class);
        ArgumentCaptor<GlAccass> assistCaptor = ArgumentCaptor.forClass(GlAccass.class);
        verify(glAccsumMapper).insert(sumCaptor.capture());
        verify(glAccassMapper).insert(assistCaptor.capture());

        GlAccsum rebuiltSum = sumCaptor.getValue();
        assertEquals("88.00", rebuiltSum.getMb().toPlainString());
        assertEquals("0.00", rebuiltSum.getMd().toPlainString());
        assertEquals("0.00", rebuiltSum.getMc().toPlainString());
        assertEquals("88.00", rebuiltSum.getMe().toPlainString());

        GlAccass rebuiltAssist = assistCaptor.getValue();
        assertEquals("D-01", rebuiltAssist.getCdeptId());
        assertEquals("66.00", rebuiltAssist.getMb().toPlainString());
        assertEquals("0.00", rebuiltAssist.getMd().toPlainString());
        assertEquals("0.00", rebuiltAssist.getMc().toPlainString());
        assertEquals("66.00", rebuiltAssist.getMe().toPlainString());
    }

    private FinanceGeneralLedgerRollbackPeriodDTO request() {
        FinanceGeneralLedgerRollbackPeriodDTO dto = new FinanceGeneralLedgerRollbackPeriodDTO();
        dto.setCompanyId("COMP-001");
        dto.setIyear(2022);
        dto.setIperiod(12);
        return dto;
    }

    private SystemCompany company() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMP-001");
        company.setCompanyCode("001");
        company.setCompanyName("测试公司");
        company.setStatus(1);
        return company;
    }

    private FinanceAccountSet accountSet() {
        FinanceAccountSet accountSet = new FinanceAccountSet();
        accountSet.setCompanyId("COMP-001");
        accountSet.setStatus("ACTIVE");
        accountSet.setEnabledYear(2022);
        accountSet.setEnabledPeriod(1);
        return accountSet;
    }

    private FinanceAccountSetModuleEnable generalLedgerModule() {
        FinanceAccountSetModuleEnable module = new FinanceAccountSetModuleEnable();
        module.setCompanyId("COMP-001");
        module.setModuleCode("GENERAL_LEDGER");
        module.setEnabled(1);
        return module;
    }

    private FinancePostVoucherState postState() {
        FinancePostVoucherState state = new FinancePostVoucherState();
        state.setId(99L);
        state.setCompanyId("COMP-001");
        state.setIyear(2022);
        state.setIperiod(12);
        state.setIyperiod(202212);
        state.setStatus("FULLY_POSTED");
        state.setPostedVoucherCount(1);
        return state;
    }

    private FinanceAccountSubject subject(String code, String parentCode) {
        FinanceAccountSubject subject = new FinanceAccountSubject();
        subject.setCompanyId("COMP-001");
        subject.setSubjectCode(code);
        subject.setParentSubjectCode(parentCode);
        subject.setStatus(1);
        subject.setBalanceDirection("DEBIT");
        return subject;
    }

    private GlAccvouch voucherRow(int inoId, int inid, String subjectCode, String md, String mc) {
        GlAccvouch row = new GlAccvouch();
        row.setId(inoId * 10 + inid);
        row.setCompanyId("COMP-001");
        row.setIyear(2022);
        row.setIyperiod(202212);
        row.setIperiod(12);
        row.setCsign("记");
        row.setInoId(inoId);
        row.setInid(inid);
        row.setCcheck("审核员");
        row.setCheckedAt(LocalDateTime.now());
        row.setIbook(1);
        row.setPostedAt(LocalDateTime.now());
        row.setCcode(subjectCode);
        row.setCurrencyCode("CNY");
        row.setCexchName("人民币");
        row.setMd(new BigDecimal(md));
        row.setMc(new BigDecimal(mc));
        return row;
    }

    private GlAccsum currentSum() {
        GlAccsum row = new GlAccsum();
        row.setId(1);
        row.setCompanyId("COMP-001");
        row.setIyear(2022);
        row.setIperiod(12);
        row.setIyperiod(202212);
        row.setCcode("5601");
        row.setCurrencyCode("CNY");
        row.setMb(new BigDecimal("88.00"));
        row.setMd(new BigDecimal("88.00"));
        row.setMc(BigDecimal.ZERO.setScale(2));
        row.setMe(new BigDecimal("176.00"));
        row.setMbF(BigDecimal.ZERO.setScale(2));
        row.setMdF(BigDecimal.ZERO.setScale(2));
        row.setMcF(BigDecimal.ZERO.setScale(2));
        row.setMeF(BigDecimal.ZERO.setScale(2));
        row.setNbS(BigDecimal.ZERO.setScale(6));
        row.setNdS(BigDecimal.ZERO.setScale(6));
        row.setNcS(BigDecimal.ZERO.setScale(6));
        row.setNeS(BigDecimal.ZERO.setScale(6));
        return row;
    }

    private GlAccsum previousSum() {
        GlAccsum row = new GlAccsum();
        row.setId(2);
        row.setCompanyId("COMP-001");
        row.setIyear(2022);
        row.setIperiod(11);
        row.setIyperiod(202211);
        row.setCcode("5601");
        row.setCurrencyCode("CNY");
        row.setMe(new BigDecimal("88.00"));
        row.setMeF(BigDecimal.ZERO.setScale(2));
        row.setNeS(BigDecimal.ZERO.setScale(6));
        return row;
    }

    private GlAccass currentAssist() {
        GlAccass row = new GlAccass();
        row.setId(1);
        row.setCompanyId("COMP-001");
        row.setIyear(2022);
        row.setIperiod(12);
        row.setIyperiod(202212);
        row.setCcode("5601");
        row.setCurrencyCode("CNY");
        row.setCdeptId("D-01");
        row.setMb(new BigDecimal("66.00"));
        row.setMd(new BigDecimal("66.00"));
        row.setMc(BigDecimal.ZERO.setScale(2));
        row.setMe(new BigDecimal("132.00"));
        row.setMbF(BigDecimal.ZERO.setScale(2));
        row.setMdF(BigDecimal.ZERO.setScale(2));
        row.setMcF(BigDecimal.ZERO.setScale(2));
        row.setMeF(BigDecimal.ZERO.setScale(2));
        row.setNbS(BigDecimal.ZERO.setScale(6));
        row.setNdS(BigDecimal.ZERO.setScale(6));
        row.setNcS(BigDecimal.ZERO.setScale(6));
        row.setNeS(BigDecimal.ZERO.setScale(6));
        return row;
    }

    private GlAccass previousAssist() {
        GlAccass row = new GlAccass();
        row.setId(2);
        row.setCompanyId("COMP-001");
        row.setIyear(2022);
        row.setIperiod(11);
        row.setIyperiod(202211);
        row.setCcode("5601");
        row.setCurrencyCode("CNY");
        row.setCdeptId("D-01");
        row.setMe(new BigDecimal("66.00"));
        row.setMeF(BigDecimal.ZERO.setScale(2));
        row.setNeS(BigDecimal.ZERO.setScale(6));
        return row;
    }
}
