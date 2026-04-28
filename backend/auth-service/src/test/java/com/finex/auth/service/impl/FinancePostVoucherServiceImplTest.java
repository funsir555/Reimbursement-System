package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.FinancePostVoucherMetaVO;
import com.finex.auth.dto.FinancePostVoucherTaskRequestDTO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.entity.FinanceAccountSet;
import com.finex.auth.entity.FinanceOpeningBalanceState;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinanceOpeningBalanceStateMapper;
import com.finex.auth.mapper.FinancePeriodCloseMapper;
import com.finex.auth.mapper.FinancePostVoucherStateMapper;
import com.finex.auth.mapper.GlAccassMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.impl.postvoucher.PostVoucherTaskWorker;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancePostVoucherServiceImplTest {

    @Mock
    private FinanceAccountSetMapper financeAccountSetMapper;
    @Mock
    private FinanceAccountSubjectMapper financeAccountSubjectMapper;
    @Mock
    private FinanceOpeningBalanceStateMapper financeOpeningBalanceStateMapper;
    @Mock
    private FinancePostVoucherStateMapper financePostVoucherStateMapper;
    @Mock
    private FinancePeriodCloseMapper financePeriodCloseMapper;
    @Mock
    private AsyncTaskRecordMapper asyncTaskRecordMapper;
    @Mock
    private GlAccvouchMapper glAccvouchMapper;
    @Mock
    private GlAccsumMapper glAccsumMapper;
    @Mock
    private GlAccassMapper glAccassMapper;
    @Mock
    private SystemCompanyMapper systemCompanyMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PostVoucherTaskWorker postVoucherTaskWorker;

    private FinancePostVoucherServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), AsyncTaskRecord.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), GlAccvouch.class);

        service = new FinancePostVoucherServiceImpl(
                financeAccountSetMapper,
                financeAccountSubjectMapper,
                financeOpeningBalanceStateMapper,
                financePostVoucherStateMapper,
                financePeriodCloseMapper,
                asyncTaskRecordMapper,
                glAccvouchMapper,
                glAccsumMapper,
                glAccassMapper,
                systemCompanyMapper,
                userMapper,
                new ObjectMapper(),
                postVoucherTaskWorker
        );

        lenient().when(systemCompanyMapper.selectOne(any())).thenReturn(company());
        lenient().when(financeAccountSetMapper.selectOne(any())).thenReturn(activeAccountSet());
        lenient().when(userMapper.selectById(1L)).thenReturn(currentUser());
        lenient().when(financeOpeningBalanceStateMapper.selectOne(any())).thenReturn(openedState());
        lenient().when(financePostVoucherStateMapper.selectOne(any())).thenReturn(null);
        lenient().when(financePeriodCloseMapper.selectOne(any())).thenReturn(null);
        lenient().when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void getMetaBlocksWhenCurrentPeriodHasUnpostedVoucher() {
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(
                voucherRow(8, 1, null, null, 0, "560101", "100.00", "0.00"),
                voucherRow(8, 2, null, null, 0, "100201", "0.00", "100.00")
        ));

        FinancePostVoucherMetaVO meta = service.getMeta(1L, "COMP-001", 2026, 4);

        assertFalse(Boolean.TRUE.equals(meta.getCanPost()));
        assertEquals(1, meta.getUnpostedVoucherCount());
        assertTrue(meta.getBlockedReason() != null && !meta.getBlockedReason().isBlank());
    }

    @Test
    void getMetaBlocksWhenCurrentPeriodHasErrorVoucher() {
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(
                voucherRow(9, 1, "\u674e\u56db", LocalDateTime.now(), 1, "560101", "88.00", "0.00"),
                voucherRow(9, 2, "\u674e\u56db", LocalDateTime.now(), 1, "100201", "0.00", "88.00")
        ));

        FinancePostVoucherMetaVO meta = service.getMeta(1L, "COMP-001", 2026, 4);

        assertFalse(Boolean.TRUE.equals(meta.getCanPost()));
        assertEquals(1, meta.getErrorVoucherCount());
        assertTrue(meta.getBlockedReason() != null && !meta.getBlockedReason().isBlank());
    }

    @Test
    void getMetaMarksPeriodPostableWhenReviewedVoucherExists() {
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(
                voucherRow(10, 1, "\u8d22\u52a1\u738b\u4e94", LocalDateTime.now(), 0, "560101", "128.00", "0.00"),
                voucherRow(10, 2, "\u8d22\u52a1\u738b\u4e94", LocalDateTime.now(), 0, "100201", "0.00", "128.00")
        ));

        FinancePostVoucherMetaVO meta = service.getMeta(1L, "COMP-001", 2026, 4);

        assertTrue(Boolean.TRUE.equals(meta.getCanPost()));
        assertEquals(1, meta.getReviewableVoucherCount());
        assertEquals(0, meta.getPostedVoucherCount());
    }

    @Test
    void getMetaReturnsBlockedReasonWhenOpeningBalanceNotOpened() {
        when(financeOpeningBalanceStateMapper.selectOne(any())).thenReturn(null);
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(
                voucherRow(13, 1, "\u8d22\u52a1\u738b\u4e94", LocalDateTime.now(), 0, "560101", "64.00", "0.00"),
                voucherRow(13, 2, "\u8d22\u52a1\u738b\u4e94", LocalDateTime.now(), 0, "100201", "0.00", "64.00")
        ));

        FinancePostVoucherMetaVO meta = service.getMeta(1L, "COMP-001", 2026, 4);

        assertFalse(Boolean.TRUE.equals(meta.getCanPost()));
        assertEquals("\u9996\u671f\u8bb0\u8d26\u524d\u5fc5\u987b\u5148\u5b8c\u6210\u671f\u521d\u5f00\u8d26", meta.getBlockedReason());
        assertEquals(1, meta.getReviewableVoucherCount());
    }

    @Test
    void runPostingCreatesAsyncTaskAndTriggersWorker() {
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(
                voucherRow(11, 1, "\u8d22\u52a1\u738b\u4e94", LocalDateTime.now(), 0, "560101", "210.00", "0.00"),
                voucherRow(11, 2, "\u8d22\u52a1\u738b\u4e94", LocalDateTime.now(), 0, "100201", "0.00", "210.00")
        ));
        when(asyncTaskRecordMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            AsyncTaskRecord task = invocation.getArgument(0);
            task.setId(99L);
            return 1;
        }).when(asyncTaskRecordMapper).insert(any(AsyncTaskRecord.class));

        FinancePostVoucherTaskRequestDTO dto = new FinancePostVoucherTaskRequestDTO();
        dto.setCompanyId("COMP-001");
        dto.setIyear(2026);
        dto.setIperiod(4);

        AsyncTaskSubmitResultVO result = service.runPosting(1L, "\u8d22\u52a1\u738b\u4e94", dto);

        assertEquals("PENDING", result.getStatus());
        assertTrue(result.getTaskNo().startsWith("FPV"));
        verify(postVoucherTaskWorker).runPostingTask(99L);

        ArgumentCaptor<AsyncTaskRecord> captor = ArgumentCaptor.forClass(AsyncTaskRecord.class);
        verify(asyncTaskRecordMapper).insert(captor.capture());
        assertEquals("COMP-001#2026#4#POST", captor.getValue().getBusinessKey());
        assertEquals("\u8d22\u52a1\u738b\u4e94", captor.getValue().getDisplayName());
    }

    @Test
    void runPostingReusesExistingActiveTaskWithoutTriggeringWorkerAgain() {
        AsyncTaskRecord activeTask = new AsyncTaskRecord();
        activeTask.setId(77L);
        activeTask.setTaskNo("FPV202604270001");
        activeTask.setTaskType("finance_post_voucher_run");
        activeTask.setBusinessType("finance_post_voucher");
        activeTask.setStatus("RUNNING");
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(
                voucherRow(12, 1, "\u8d22\u52a1\u8d75\u516d", LocalDateTime.now(), 0, "560101", "320.00", "0.00"),
                voucherRow(12, 2, "\u8d22\u52a1\u8d75\u516d", LocalDateTime.now(), 0, "100201", "0.00", "320.00")
        ));
        when(asyncTaskRecordMapper.selectOne(any())).thenReturn(activeTask);

        FinancePostVoucherTaskRequestDTO dto = new FinancePostVoucherTaskRequestDTO();
        dto.setCompanyId("COMP-001");
        dto.setIyear(2026);
        dto.setIperiod(4);

        AsyncTaskSubmitResultVO result = service.runPosting(1L, "\u8d22\u52a1\u8d75\u516d", dto);

        assertEquals("FPV202604270001", result.getTaskNo());
        assertEquals("RUNNING", result.getStatus());
        verify(asyncTaskRecordMapper, never()).insert(any(AsyncTaskRecord.class));
        verify(postVoucherTaskWorker, never()).runPostingTask(any());
    }

    private SystemCompany company() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMP-001");
        company.setCompanyCode("001");
        company.setCompanyName("\u5e7f\u5dde\u5206\u516c\u53f8");
        company.setStatus(1);
        return company;
    }

    private User currentUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setName("\u8d22\u52a1\u738b\u4e94");
        user.setCompanyId("COMP-001");
        return user;
    }

    private FinanceAccountSet activeAccountSet() {
        FinanceAccountSet set = new FinanceAccountSet();
        set.setCompanyId("COMP-001");
        set.setStatus("ACTIVE");
        set.setEnabledYear(2026);
        set.setEnabledPeriod(4);
        return set;
    }

    private FinanceOpeningBalanceState openedState() {
        FinanceOpeningBalanceState state = new FinanceOpeningBalanceState();
        state.setCompanyId("COMP-001");
        state.setIyear(2026);
        state.setIperiod(4);
        state.setStatus("OPENED");
        return state;
    }

    private GlAccvouch voucherRow(
            int inoId,
            int inid,
            String checkerName,
            LocalDateTime checkedAt,
            int iflag,
            String subjectCode,
            String md,
            String mc
    ) {
        GlAccvouch row = new GlAccvouch();
        row.setId(inoId * 10 + inid);
        row.setCompanyId("COMP-001");
        row.setIyear(2026);
        row.setIyperiod(202604);
        row.setIperiod(4);
        row.setCsign("\u8bb0");
        row.setInoId(inoId);
        row.setInid(inid);
        row.setCcheck(checkerName);
        row.setCheckedAt(checkedAt);
        row.setIflag(iflag);
        row.setCcode(subjectCode);
        row.setMd(new BigDecimal(md));
        row.setMc(new BigDecimal(mc));
        row.setCurrencyCode("CNY");
        row.setCexchName("\u4eba\u6c11\u5e01");
        return row;
    }
}
