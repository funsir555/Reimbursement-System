package com.finex.auth.service.impl.postvoucher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinancePostVoucherTaskRequestDTO;
import com.finex.auth.entity.AsyncTaskRecord;
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
import com.finex.auth.service.NotificationService;
import com.finex.auth.support.AsyncTaskSupport;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostVoucherTaskWorker {

    private final AsyncTaskRecordMapper asyncTaskRecordMapper;
    private final FinanceAccountSetMapper financeAccountSetMapper;
    private final FinanceAccountSubjectMapper financeAccountSubjectMapper;
    private final FinanceOpeningBalanceStateMapper financeOpeningBalanceStateMapper;
    private final FinancePeriodCloseMapper financePeriodCloseMapper;
    private final FinancePostVoucherStateMapper financePostVoucherStateMapper;
    private final GlAccvouchMapper glAccvouchMapper;
    private final GlAccsumMapper glAccsumMapper;
    private final GlAccassMapper glAccassMapper;
    private final SystemCompanyMapper systemCompanyMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Async("finexAsyncExecutor")
    public void runPostingTask(Long taskId) {
        AsyncTaskRecord task = asyncTaskRecordMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        SharedPostVoucherSupport support = new SharedPostVoucherSupport(
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
                userMapper
        );
        PostVoucherValidationSupport validationSupport = new PostVoucherValidationSupport(support);
        PostVoucherMutationSupport mutationSupport = new PostVoucherMutationSupport(support);
        PostVoucherRunContext context = null;
        try {
            markTask(task, AsyncTaskSupport.TASK_STATUS_RUNNING, 5, "正在校验记账条件", null);
            FinancePostVoucherTaskRequestDTO payload = objectMapper.readValue(task.getResultPayload(), FinancePostVoucherTaskRequestDTO.class);
            context = validationSupport.prepareRun(payload.getCompanyId(), payload.getIyear(), payload.getIperiod());
            mutationSupport.markPostingStarted(context, task);
            String operatorName = validationSupport.resolveOperatorName(task.getUserId(), task.getDisplayName());
            List<Map.Entry<AbstractFinancePostVoucherSupport.VoucherKey, List<com.finex.auth.entity.GlAccvouch>>> entries = context.getReviewableVoucherEntries();
            int total = entries.size();
            for (int index = 0; index < total; index += 1) {
                Map.Entry<AbstractFinancePostVoucherSupport.VoucherKey, List<com.finex.auth.entity.GlAccvouch>> entry = entries.get(index);
                String voucherNo = support.buildVoucherNo(entry.getKey());
                int progress = Math.min(95, 10 + (int) Math.floor(((index) * 85.0d) / Math.max(total, 1)));
                markTask(task, AsyncTaskSupport.TASK_STATUS_RUNNING, progress, "正在记账 " + voucherNo, null);
                transactionTemplate.executeWithoutResult(status -> mutationSupport.postVoucherGroup(entry.getKey(), entry.getValue(), operatorName));
            }
            mutationSupport.markPostingSuccess(context, validationSupport.resolveOperatorName(task.getUserId(), task.getDisplayName()), task.getTaskNo());
            markTask(task, AsyncTaskSupport.TASK_STATUS_SUCCESS, 100, "记账完成", LocalDateTime.now());
            notificationService.sendAsyncNotification(
                    task.getUserId(),
                    AsyncTaskSupport.NOTIFICATION_TYPE_TASK,
                    "总账记账完成",
                    "当前期间已完成本次记账，请刷新页面查看最新结果。",
                    task.getTaskNo()
            );
        } catch (Exception ex) {
            log.error("post voucher task failed, taskNo={}", task.getTaskNo(), ex);
            if (context != null) {
                mutationSupport.markPostingFailed(context, task.getTaskNo(), ex.getMessage());
            }
            markTask(task, AsyncTaskSupport.TASK_STATUS_FAILED, 100, ex.getMessage(), LocalDateTime.now());
            notificationService.sendAsyncNotification(
                    task.getUserId(),
                    AsyncTaskSupport.NOTIFICATION_TYPE_TASK,
                    "总账记账失败",
                    ex.getMessage() == null ? "记账任务执行失败，请稍后重试。" : ex.getMessage(),
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
