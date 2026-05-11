package com.finex.auth.service.impl;

import com.finex.auth.dto.FinanceCloseLedgerRequestDTO;
import com.finex.auth.dto.FinanceCloseLedgerValidationResultVO;
import com.finex.auth.entity.FaAssetPeriodClose;
import com.finex.auth.entity.FinanceAccountSet;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.FinancePeriodClose;
import com.finex.auth.entity.GlAccass;
import com.finex.auth.entity.GlAccsum;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.mapper.FaAssetPeriodCloseMapper;
import com.finex.auth.mapper.FinanceAccountSetMapper;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceCloseLedgerServiceImplTest {

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
    private FaAssetPeriodCloseMapper faAssetPeriodCloseMapper;
    @Mock
    private UserMapper userMapper;

    private final AtomicReference<FinancePeriodClose> storedClose = new AtomicReference<>();

    private FinanceCloseLedgerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FinanceCloseLedgerServiceImpl(
                systemCompanyMapper,
                financeAccountSetMapper,
                financePostVoucherStateMapper,
                financeAccountSubjectMapper,
                financePeriodCloseMapper,
                financePeriodCloseLogMapper,
                glAccvouchMapper,
                glAccsumMapper,
                glAccassMapper,
                faAssetPeriodCloseMapper,
                userMapper
        );

        storedClose.set(null);

        when(systemCompanyMapper.selectOne(any())).thenReturn(company());
        when(financeAccountSetMapper.selectOne(any())).thenReturn(accountSet());
        when(faAssetPeriodCloseMapper.selectOne(any())).thenReturn(closedFixedAssetPeriod());
        when(financePeriodCloseMapper.selectOne(any())).thenAnswer(invocation -> storedClose.get());
        lenient().when(financePostVoucherStateMapper.selectOne(any())).thenReturn(null);
        lenient().when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of());
        lenient().when(glAccvouchMapper.selectList(any())).thenReturn(List.of());
        lenient().when(glAccsumMapper.selectList(any())).thenReturn(List.of());
        lenient().when(glAccassMapper.selectList(any())).thenReturn(List.of());
        lenient().when(glAccsumMapper.selectCount(any())).thenReturn(0L);
        lenient().when(glAccassMapper.selectCount(any())).thenReturn(0L);
        lenient().when(financePeriodCloseLogMapper.insert(any())).thenReturn(1);
        lenient().when(userMapper.selectById(any())).thenReturn(null);

        lenient().doAnswer(invocation -> {
            FinancePeriodClose entity = invocation.getArgument(0);
            entity.setId(1L);
            storedClose.set(entity);
            return 1;
        }).when(financePeriodCloseMapper).insert(any(FinancePeriodClose.class));
        lenient().doAnswer(invocation -> {
            FinancePeriodClose entity = invocation.getArgument(0);
            storedClose.set(entity);
            return 1;
        }).when(financePeriodCloseMapper).updateById(any(FinancePeriodClose.class));
    }

    @Test
    void validateAllowsEmptyPeriodWithoutRequiringPostedFullyState() {
        FinanceCloseLedgerValidationResultVO result = service.validate(1L, "alice", request(2026, 11));

        assertTrue(result.getPassed());
        assertTrue(result.getGeneralPassed());
        assertTrue(result.getExternalPassed());
        assertEquals("NOT_POSTED", result.getPostStatus());
        assertTrue(result.getBlockingReasons().isEmpty());
        assertTrue(result.getGeneralChecks().stream().noneMatch(item -> "posted_fully".equals(item.getCode())));
        assertTrue(result.getGeneralChecks().stream()
                .anyMatch(item -> item.getMessage() != null && item.getMessage().contains("当前期间无凭证，视为已满足记账前置条件")));
    }

    @Test
    void closeAllowsEmptyPeriodWithoutCreatingFakeCarryForwardRows() {
        var result = service.close(1L, "alice", request(2026, 11));

        assertEquals("CLOSED", result.getStatus());
        assertEquals("已结账", result.getStatusLabel());
        verify(financePeriodCloseMapper).insert(any(FinancePeriodClose.class));
        verify(glAccsumMapper, never()).insert(any(GlAccsum.class));
        verify(glAccassMapper, never()).insert(any(GlAccass.class));
    }

    @Test
    void closeCarriesEndingBalancesIntoNextPeriodWithOppositeDirectionSemantics() {
        FinanceAccountSubject subject = subject("4103", "本年利润", "CREDIT");
        GlAccsum currentSum = currentSumRow("4103", "-88.50");
        GlAccass currentAssist = currentAssistRow("4103", "-56.00", "D-01");

        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(subject));
        when(glAccsumMapper.selectList(any())).thenReturn(List.of(currentSum));
        when(glAccassMapper.selectList(any())).thenReturn(List.of(currentAssist));

        ArgumentCaptor<GlAccsum> sumCaptor = ArgumentCaptor.forClass(GlAccsum.class);
        ArgumentCaptor<GlAccass> assistCaptor = ArgumentCaptor.forClass(GlAccass.class);
        doAnswer(invocation -> {
            GlAccsum entity = invocation.getArgument(0);
            entity.setId(1001);
            return 1;
        }).when(glAccsumMapper).insert(any(GlAccsum.class));
        doAnswer(invocation -> {
            GlAccass entity = invocation.getArgument(0);
            entity.setId(2001);
            return 1;
        }).when(glAccassMapper).insert(any(GlAccass.class));

        var result = service.close(1L, "alice", request(2026, 12));

        verify(glAccsumMapper).insert(sumCaptor.capture());
        verify(glAccassMapper).insert(assistCaptor.capture());
        assertEquals("CLOSED", result.getStatus());

        GlAccsum nextSum = sumCaptor.getValue();
        assertEquals(2027, nextSum.getIyear());
        assertEquals(1, nextSum.getIperiod());
        assertEquals("-88.50", nextSum.getMb().toPlainString());
        assertEquals("-88.50", nextSum.getMe().toPlainString());
        assertEquals("0.00", nextSum.getMd().toPlainString());
        assertEquals("0.00", nextSum.getMc().toPlainString());
        assertEquals("借", nextSum.getCbegindC());
        assertEquals("DEBIT", nextSum.getCbegindCEngl());
        assertEquals("借", nextSum.getCenddC());
        assertEquals("DEBIT", nextSum.getCenddCEngl());

        GlAccass nextAssist = assistCaptor.getValue();
        assertEquals(2027, nextAssist.getIyear());
        assertEquals(1, nextAssist.getIperiod());
        assertEquals("D-01", nextAssist.getCdeptId());
        assertEquals("-56.00", nextAssist.getMb().toPlainString());
        assertEquals("-56.00", nextAssist.getMe().toPlainString());
        assertEquals("借", nextAssist.getCbegindC());
        assertEquals("DEBIT", nextAssist.getCbegindCEngl());
    }

    @Test
    void closeRejectsWhenNextPeriodAlreadyContainsLedgerRows() {
        when(glAccsumMapper.selectCount(any())).thenReturn(1L);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.close(1L, "alice", request(2026, 11))
        );

        assertEquals("下一期间已存在账务数据，不能重复滚转，请先检查期初或期间数据", error.getMessage());
        assertTrue(storedClose.get() == null);
        verify(financePeriodCloseMapper, never()).insert(any(FinancePeriodClose.class));
        verify(glAccsumMapper, never()).insert(any(GlAccsum.class));
        verify(glAccassMapper, never()).insert(any(GlAccass.class));
    }

    private FinanceCloseLedgerRequestDTO request(int iyear, int iperiod) {
        FinanceCloseLedgerRequestDTO dto = new FinanceCloseLedgerRequestDTO();
        dto.setCompanyId("COMP-001");
        dto.setIyear(iyear);
        dto.setIperiod(iperiod);
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
        accountSet.setEnabledYear(2026);
        accountSet.setEnabledPeriod(1);
        return accountSet;
    }

    private FaAssetPeriodClose closedFixedAssetPeriod() {
        FaAssetPeriodClose record = new FaAssetPeriodClose();
        record.setCompanyId("COMP-001");
        record.setBookCode("FINANCE");
        record.setFiscalYear(2026);
        record.setFiscalPeriod(11);
        record.setStatus("CLOSED");
        return record;
    }

    private FinanceAccountSubject subject(String code, String name, String balanceDirection) {
        FinanceAccountSubject subject = new FinanceAccountSubject();
        subject.setCompanyId("COMP-001");
        subject.setSubjectCode(code);
        subject.setSubjectName(name);
        subject.setBalanceDirection(balanceDirection);
        subject.setStatus(1);
        return subject;
    }

    private GlAccsum currentSumRow(String code, String endingBalance) {
        GlAccsum row = new GlAccsum();
        row.setId(1);
        row.setCompanyId("COMP-001");
        row.setIyear(2026);
        row.setIperiod(12);
        row.setIyperiod(202612);
        row.setCcode(code);
        row.setMe(new BigDecimal(endingBalance));
        row.setMeF(BigDecimal.ZERO.setScale(2));
        row.setNeS(BigDecimal.ZERO.setScale(6));
        return row;
    }

    private GlAccass currentAssistRow(String code, String endingBalance, String deptId) {
        GlAccass row = new GlAccass();
        row.setId(1);
        row.setCompanyId("COMP-001");
        row.setIyear(2026);
        row.setIperiod(12);
        row.setIyperiod(202612);
        row.setCcode(code);
        row.setCdeptId(deptId);
        row.setMe(new BigDecimal(endingBalance));
        row.setMeF(BigDecimal.ZERO.setScale(2));
        row.setNeS(BigDecimal.ZERO.setScale(6));
        return row;
    }
}
