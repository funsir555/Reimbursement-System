package com.finex.auth.service.impl;

import com.finex.auth.dto.OpeningBalanceReconcileResultVO;
import com.finex.auth.dto.OpeningBalanceTrialResultVO;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.FinanceOpeningBalanceState;
import com.finex.auth.entity.GlAccass;
import com.finex.auth.entity.GlAccsum;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinanceCustomerMapper;
import com.finex.auth.mapper.FinanceOpeningBalanceStateMapper;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.GlAccassMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.impl.openingbalance.OpeningBalanceTaskWorker;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceOpeningBalanceServiceImplTest {

    @Mock
    private FinanceAccountSubjectMapper financeAccountSubjectMapper;
    @Mock
    private FinanceCustomerMapper financeCustomerMapper;
    @Mock
    private FinanceVendorMapper financeVendorMapper;
    @Mock
    private FinanceProjectClassMapper financeProjectClassMapper;
    @Mock
    private FinanceProjectArchiveMapper financeProjectArchiveMapper;
    @Mock
    private SystemCompanyMapper systemCompanyMapper;
    @Mock
    private SystemDepartmentMapper systemDepartmentMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private GlAccsumMapper glAccsumMapper;
    @Mock
    private GlAccassMapper glAccassMapper;
    @Mock
    private FinanceOpeningBalanceStateMapper financeOpeningBalanceStateMapper;
    @Mock
    private AsyncTaskRecordMapper asyncTaskRecordMapper;
    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Mock
    private OpeningBalanceTaskWorker openingBalanceTaskWorker;

    private FinanceOpeningBalanceServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FinanceOpeningBalanceServiceImpl(
                financeAccountSubjectMapper,
                financeCustomerMapper,
                financeVendorMapper,
                financeProjectClassMapper,
                financeProjectArchiveMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                userMapper,
                glAccsumMapper,
                glAccassMapper,
                financeOpeningBalanceStateMapper,
                asyncTaskRecordMapper,
                objectMapper,
                openingBalanceTaskWorker
        );

        when(systemCompanyMapper.selectCount(any())).thenReturn(1L);
        when(financeOpeningBalanceStateMapper.selectOne(any())).thenReturn(openedState());
        lenient().when(financeCustomerMapper.selectList(any())).thenReturn(List.of());
        lenient().when(financeVendorMapper.selectList(any())).thenReturn(List.of());
        lenient().when(financeProjectClassMapper.selectList(any())).thenReturn(List.of());
        lenient().when(financeProjectArchiveMapper.selectList(any())).thenReturn(List.of());
        lenient().when(systemDepartmentMapper.selectList(any())).thenReturn(List.of());
        lenient().when(userMapper.selectList(any())).thenReturn(List.of());
        lenient().when(systemCompanyMapper.selectList(any())).thenReturn(List.of(company()));
    }

    @Test
    void trialBalanceSplitsDebitAndCreditBySubjectDirection() {
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(
                subject("1001", "库存现金", "DEBIT", 1, 0),
                subject("2202", "应付账款", "CREDIT", 1, 0)
        ));
        when(glAccsumMapper.selectList(any())).thenReturn(List.of(
                sumRow("1001", "100.00"),
                sumRow("2202", "100.00")
        ));

        OpeningBalanceTrialResultVO result = service.trialBalance("COMP-001", 2026, 4, "alice");

        assertTrue(result.getBalanced());
        assertEquals("100.00", result.getTotalDebit().toPlainString());
        assertEquals("100.00", result.getTotalCredit().toPlainString());
        assertEquals("0.00", result.getDifference().toPlainString());
    }

    @Test
    void reconcileDetectsAuxiliaryDifferenceAgainstSubjectTotal() {
        FinanceAccountSubject assistSubject = subject("560101", "广告宣传费", "DEBIT", 1, 1);
        assistSubject.setBdept(1);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(assistSubject));
        when(glAccsumMapper.selectList(any())).thenReturn(List.of(sumRow("560101", "120.00")));
        when(glAccassMapper.selectList(any())).thenReturn(List.of(assistRow("560101", "100.00", "10")));

        OpeningBalanceReconcileResultVO result = service.reconcile("COMP-001", 2026, 4, "alice");

        assertFalse(result.getMatched());
        assertEquals(1, result.getDifferenceSubjects().size());
        assertEquals("560101", result.getDifferenceSubjects().get(0).getSubjectCode());
    }

    private FinanceOpeningBalanceState openedState() {
        FinanceOpeningBalanceState state = new FinanceOpeningBalanceState();
        state.setCompanyId("COMP-001");
        state.setIyear(2026);
        state.setIperiod(4);
        state.setStatus("OPENED");
        return state;
    }

    private SystemCompany company() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMP-001");
        company.setCompanyCode("001");
        company.setCompanyName("测试公司");
        company.setStatus(1);
        return company;
    }

    private FinanceAccountSubject subject(String code, String name, String direction, int leafFlag, int bitem) {
        FinanceAccountSubject subject = new FinanceAccountSubject();
        subject.setCompanyId("COMP-001");
        subject.setSubjectCode(code);
        subject.setSubjectName(name);
        subject.setBalanceDirection(direction);
        subject.setLeafFlag(leafFlag);
        subject.setBitem(bitem);
        subject.setStatus(1);
        subject.setBclose(0);
        return subject;
    }

    private GlAccsum sumRow(String code, String mb) {
        GlAccsum row = new GlAccsum();
        row.setCompanyId("COMP-001");
        row.setIyear(2026);
        row.setIperiod(4);
        row.setCcode(code);
        row.setMb(new BigDecimal(mb));
        return row;
    }

    private GlAccass assistRow(String code, String mb, String deptId) {
        GlAccass row = new GlAccass();
        row.setCompanyId("COMP-001");
        row.setIyear(2026);
        row.setIperiod(4);
        row.setCcode(code);
        row.setMb(new BigDecimal(mb));
        row.setCdeptId(deptId);
        return row;
    }
}
