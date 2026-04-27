package com.finex.auth.service.impl.openingbalance;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.OpeningBalanceTaskRequestDTO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.support.AsyncTaskSupport;
import java.time.LocalDateTime;

public class OpeningBalanceTaskSupport {

    private static final String TASK_TYPE_OPEN_BOOK = AsyncTaskSupport.TASK_TYPE_FINANCE_OPENING_BALANCE_OPEN_BOOK;
    private static final String TASK_TYPE_CARRY_FORWARD = AsyncTaskSupport.TASK_TYPE_FINANCE_OPENING_BALANCE_CARRY_FORWARD;
    private static final String BUSINESS_TYPE = "finance_opening_balance";

    private final AsyncTaskRecordMapper asyncTaskRecordMapper;
    private final ObjectMapper objectMapper;
    private final OpeningBalanceTaskWorker openingBalanceTaskWorker;

    public OpeningBalanceTaskSupport(
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            ObjectMapper objectMapper,
            OpeningBalanceTaskWorker openingBalanceTaskWorker
    ) {
        this.asyncTaskRecordMapper = asyncTaskRecordMapper;
        this.objectMapper = objectMapper;
        this.openingBalanceTaskWorker = openingBalanceTaskWorker;
    }

    public AsyncTaskSubmitResultVO openBook(Long currentUserId, String operatorName, OpeningBalanceTaskRequestDTO dto) {
        String businessKey = dto.getCompanyId() + "#" + dto.getIyear() + "#" + dto.getIperiod() + "#OPEN_BOOK";
        AsyncTaskRecord active = findActiveTask(currentUserId, TASK_TYPE_OPEN_BOOK, businessKey);
        if (active != null) {
            return toSubmitResult(active, "当前已有相同开账任务在执行，请稍后查看结果");
        }
        AsyncTaskRecord task = createTask(currentUserId, operatorName, TASK_TYPE_OPEN_BOOK, businessKey, dto);
        openingBalanceTaskWorker.runOpenBookTask(task.getId());
        return toSubmitResult(task, "开账任务已提交");
    }

    public AsyncTaskSubmitResultVO carryForward(Long currentUserId, String operatorName, OpeningBalanceTaskRequestDTO dto) {
        String businessKey = dto.getCompanyId() + "#" + dto.getIyear() + "#" + dto.getIperiod() + "#CARRY_FORWARD";
        AsyncTaskRecord active = findActiveTask(currentUserId, TASK_TYPE_CARRY_FORWARD, businessKey);
        if (active != null) {
            return toSubmitResult(active, "当前已有相同结转任务在执行，请稍后查看结果");
        }
        AsyncTaskRecord task = createTask(currentUserId, operatorName, TASK_TYPE_CARRY_FORWARD, businessKey, dto);
        openingBalanceTaskWorker.runCarryForwardTask(task.getId());
        return toSubmitResult(task, "结转任务已提交");
    }

    private AsyncTaskRecord createTask(Long currentUserId, String operatorName, String taskType, String businessKey, OpeningBalanceTaskRequestDTO dto) {
        AsyncTaskRecord task = new AsyncTaskRecord();
        task.setTaskNo(AsyncTaskSupport.buildTaskNo(taskType));
        task.setUserId(currentUserId);
        task.setCompanyId(dto.getCompanyId());
        task.setTaskType(taskType);
        task.setBusinessType(BUSINESS_TYPE);
        task.setBusinessKey(businessKey);
        task.setDisplayName(operatorName == null || operatorName.isBlank() ? "system" : operatorName);
        task.setStatus(AsyncTaskSupport.TASK_STATUS_PENDING);
        task.setProgress(0);
        task.setResultMessage("任务已提交");
        task.setResultPayload(writePayload(dto));
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        asyncTaskRecordMapper.insert(task);
        return task;
    }

    private AsyncTaskRecord findActiveTask(Long currentUserId, String taskType, String businessKey) {
        return asyncTaskRecordMapper.selectOne(Wrappers.<AsyncTaskRecord>lambdaQuery()
                .eq(AsyncTaskRecord::getUserId, currentUserId)
                .eq(AsyncTaskRecord::getTaskType, taskType)
                .eq(AsyncTaskRecord::getBusinessKey, businessKey)
                .in(AsyncTaskRecord::getStatus, AsyncTaskSupport.TASK_STATUS_PENDING, AsyncTaskSupport.TASK_STATUS_RUNNING)
                .last("limit 1"));
    }

    private String writePayload(OpeningBalanceTaskRequestDTO dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception ex) {
            throw new IllegalStateException("任务参数序列化失败", ex);
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
