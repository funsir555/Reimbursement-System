package com.finex.auth.service.impl.openingbalance;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.OpeningBalanceTaskRequestDTO;
import com.finex.auth.entity.AsyncTaskRecord;
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
import com.finex.auth.service.NotificationService;
import com.finex.auth.support.AsyncTaskSupport;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpeningBalanceTaskWorker {

    private final AsyncTaskRecordMapper asyncTaskRecordMapper;
    private final FinanceAccountSubjectMapper financeAccountSubjectMapper;
    private final FinanceCustomerMapper financeCustomerMapper;
    private final FinanceVendorMapper financeVendorMapper;
    private final FinanceProjectClassMapper financeProjectClassMapper;
    private final FinanceProjectArchiveMapper financeProjectArchiveMapper;
    private final SystemCompanyMapper systemCompanyMapper;
    private final SystemDepartmentMapper systemDepartmentMapper;
    private final UserMapper userMapper;
    private final GlAccsumMapper glAccsumMapper;
    private final GlAccassMapper glAccassMapper;
    private final FinanceOpeningBalanceStateMapper financeOpeningBalanceStateMapper;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Async("finexAsyncExecutor")
    public void runOpenBookTask(Long taskId) {
        runTask(taskId, true);
    }

    @Async("finexAsyncExecutor")
    public void runCarryForwardTask(Long taskId) {
        runTask(taskId, false);
    }

    private void runTask(Long taskId, boolean openBook) {
        AsyncTaskRecord task = asyncTaskRecordMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        try {
            markTask(task, AsyncTaskSupport.TASK_STATUS_RUNNING, 15, openBook ? "正在初始化期初账套" : "正在执行年度期初结转", null);
            OpeningBalanceTaskRequestDTO payload = objectMapper.readValue(task.getResultPayload(), OpeningBalanceTaskRequestDTO.class);
            SharedOpeningBalanceSupport support = new SharedOpeningBalanceSupport(
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
                    financeOpeningBalanceStateMapper
            );
            transactionTemplate.executeWithoutResult(status -> {
                if (openBook) {
                    support.performOpenBook(payload.getCompanyId(), payload.getIyear(), payload.getIperiod(), task.getDisplayName());
                } else {
                    support.performCarryForward(payload.getCompanyId(), payload.getIyear(), payload.getIperiod(), task.getDisplayName());
                }
            });
            markTask(task, AsyncTaskSupport.TASK_STATUS_SUCCESS, 100, openBook ? "期初开账完成" : "年度期初结转完成", LocalDateTime.now());
            notificationService.sendAsyncNotification(
                    task.getUserId(),
                    AsyncTaskSupport.NOTIFICATION_TYPE_TASK,
                    openBook ? "期初开账完成" : "年度期初结转完成",
                    openBook ? "期初余额开账任务已完成，请刷新页面查看结果。" : "年度期初结转任务已完成，请刷新页面查看结果。",
                    task.getTaskNo()
            );
        } catch (Exception ex) {
            log.error("opening balance task failed, taskNo={}", task.getTaskNo(), ex);
            markTask(task, AsyncTaskSupport.TASK_STATUS_FAILED, 100, ex.getMessage(), LocalDateTime.now());
            notificationService.sendAsyncNotification(
                    task.getUserId(),
                    AsyncTaskSupport.NOTIFICATION_TYPE_TASK,
                    openBook ? "期初开账失败" : "年度期初结转失败",
                    ex.getMessage() == null ? "任务执行失败，请稍后重试。" : ex.getMessage(),
                    task.getTaskNo()
            );
        }
    }

    private void markTask(AsyncTaskRecord task, String status, int progress, String message, LocalDateTime finishedAt) {
        task.setStatus(status);
        task.setProgress(progress);
        task.setResultMessage(message);
        if (AsyncTaskSupport.TASK_STATUS_RUNNING.equals(status)) {
            task.setStartedAt(task.getStartedAt() == null ? LocalDateTime.now() : task.getStartedAt());
        }
        if (finishedAt != null) {
            task.setFinishedAt(finishedAt);
        }
        asyncTaskRecordMapper.updateById(task);
    }
}
