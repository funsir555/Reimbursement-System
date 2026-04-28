package com.finex.auth.service.impl.postvoucher;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.FinancePostVoucherTaskRequestDTO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.support.AsyncTaskSupport;
import java.time.LocalDateTime;

public class PostVoucherTaskSupport {

    private static final String TASK_TYPE = AsyncTaskSupport.TASK_TYPE_FINANCE_POST_VOUCHER_RUN;
    private static final String BUSINESS_TYPE = "finance_post_voucher";

    private final AsyncTaskRecordMapper asyncTaskRecordMapper;
    private final ObjectMapper objectMapper;
    private final PostVoucherTaskWorker postVoucherTaskWorker;

    public PostVoucherTaskSupport(
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            ObjectMapper objectMapper,
            PostVoucherTaskWorker postVoucherTaskWorker
    ) {
        this.asyncTaskRecordMapper = asyncTaskRecordMapper;
        this.objectMapper = objectMapper;
        this.postVoucherTaskWorker = postVoucherTaskWorker;
    }

    public AsyncTaskSubmitResultVO runPosting(Long currentUserId, String operatorName, FinancePostVoucherTaskRequestDTO dto) {
        String businessKey = dto.getCompanyId() + "#" + dto.getIyear() + "#" + dto.getIperiod() + "#POST";
        AsyncTaskRecord active = asyncTaskRecordMapper.selectOne(Wrappers.<AsyncTaskRecord>lambdaQuery()
                .eq(AsyncTaskRecord::getUserId, currentUserId)
                .eq(AsyncTaskRecord::getTaskType, TASK_TYPE)
                .eq(AsyncTaskRecord::getBusinessKey, businessKey)
                .in(AsyncTaskRecord::getStatus, AsyncTaskSupport.TASK_STATUS_PENDING, AsyncTaskSupport.TASK_STATUS_RUNNING)
                .last("limit 1"));
        if (active != null) {
            return toSubmitResult(active, "当前已有相同期间的记账任务在执行，请稍后查看结果");
        }
        AsyncTaskRecord task = new AsyncTaskRecord();
        task.setTaskNo(AsyncTaskSupport.buildTaskNo(TASK_TYPE));
        task.setUserId(currentUserId);
        task.setCompanyId(dto.getCompanyId());
        task.setTaskType(TASK_TYPE);
        task.setBusinessType(BUSINESS_TYPE);
        task.setBusinessKey(businessKey);
        task.setDisplayName(operatorName == null || operatorName.isBlank() ? "system" : operatorName);
        task.setStatus(AsyncTaskSupport.TASK_STATUS_PENDING);
        task.setProgress(0);
        task.setResultMessage("记账任务已提交");
        task.setResultPayload(writePayload(dto));
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        asyncTaskRecordMapper.insert(task);
        postVoucherTaskWorker.runPostingTask(task.getId());
        return toSubmitResult(task, "记账任务已提交");
    }

    private String writePayload(FinancePostVoucherTaskRequestDTO dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception ex) {
            throw new IllegalStateException("记账任务参数序列化失败", ex);
        }
    }

    private AsyncTaskSubmitResultVO toSubmitResult(AsyncTaskRecord task, String message) {
        AsyncTaskSubmitResultVO vo = new AsyncTaskSubmitResultVO();
        vo.setTaskNo(task.getTaskNo());
        vo.setTaskType(task.getTaskType());
        vo.setBusinessType(task.getBusinessType());
        vo.setStatus(task.getStatus());
        vo.setMessage(message);
        return vo;
    }
}
