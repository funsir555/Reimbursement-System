package com.finex.auth.service.impl.financesystem;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSetCreateDTO;
import com.finex.auth.dto.FinanceAccountSetTaskPayload;
import com.finex.auth.dto.FinanceAccountSetTaskStatusVO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.entity.FinanceAccountSet;
import com.finex.auth.entity.FinanceAccountSetTemplate;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.FinanceAccountSetCodeRuleMapper;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateSubjectMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.impl.FinanceAccountSetTaskWorker;
import com.finex.auth.support.AsyncTaskSupport;

public class FinanceAccountSetTaskDomainSupport extends AbstractFinanceSystemManagementSupport {

    public FinanceAccountSetTaskDomainSupport(
            FinanceAccountSetMapper financeAccountSetMapper,
            FinanceAccountSetCodeRuleMapper financeAccountSetCodeRuleMapper,
            FinanceAccountSetTemplateMapper financeAccountSetTemplateMapper,
            FinanceAccountSetTemplateSubjectMapper financeAccountSetTemplateSubjectMapper,
            SystemCompanyMapper systemCompanyMapper,
            UserMapper userMapper,
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            FinanceAccountSetTaskWorker financeAccountSetTaskWorker,
            ObjectMapper objectMapper
    ) {
        super(
                financeAccountSetMapper,
                financeAccountSetCodeRuleMapper,
                financeAccountSetTemplateMapper,
                financeAccountSetTemplateSubjectMapper,
                systemCompanyMapper,
                userMapper,
                asyncTaskRecordMapper,
                financeAccountSetTaskWorker,
                objectMapper
        );
    }

    public FinanceAccountSetTaskStatusVO submitCreateTask(Long currentUserId, FinanceAccountSetCreateDTO dto) {
        FinanceAccountSetTaskPayload payload = normalizePayload(dto);
        validateBeforeSubmit(payload);

        AsyncTaskRecord task = new AsyncTaskRecord();
        task.setTaskNo(AsyncTaskSupport.buildTaskNo(AsyncTaskSupport.TASK_TYPE_FINANCE_ACCOUNT_SET_CREATE));
        task.setUserId(currentUserId);
        task.setCompanyId(payload.getTargetCompanyId());
        task.setTaskType(AsyncTaskSupport.TASK_TYPE_FINANCE_ACCOUNT_SET_CREATE);
        task.setBusinessType(AsyncTaskSupport.BUSINESS_TYPE_FINANCE_ACCOUNT_SET);
        task.setBusinessKey(payload.getTargetCompanyId());
        task.setDisplayName("新建账套");
        task.setStatus(AsyncTaskSupport.TASK_STATUS_PENDING);
        task.setProgress(0);
        task.setResultMessage("账套创建任务已提交");
        task.setResultPayload(writePayload(payload));
        asyncTaskRecordMapper().insert(task);

        financeAccountSetTaskWorker().runCreateAccountSetTask(task.getId());
        return toTaskStatus(task, null);
    }

    public FinanceAccountSetTaskStatusVO getTaskStatus(String taskNo) {
        String normalizedTaskNo = trimToNull(taskNo);
        if (normalizedTaskNo == null) {
            throw new IllegalArgumentException("任务号不能为空");
        }
        AsyncTaskRecord task = asyncTaskRecordMapper().selectOne(
                Wrappers.<AsyncTaskRecord>lambdaQuery()
                        .eq(AsyncTaskRecord::getTaskNo, normalizedTaskNo)
                        .last("limit 1")
        );
        if (task == null) {
            throw new IllegalStateException("任务不存在");
        }
        FinanceAccountSet accountSet = task.getCompanyId() == null ? null : financeAccountSetMapper().selectById(task.getCompanyId());
        return toTaskStatus(task, accountSet == null ? null : accountSet.getStatus());
    }

    private FinanceAccountSetTaskPayload normalizePayload(FinanceAccountSetCreateDTO dto) {
        FinanceAccountSetTaskPayload payload = new FinanceAccountSetTaskPayload();
        payload.setCreateMode(normalizeCreateMode(dto.getCreateMode()));
        payload.setReferenceCompanyId(trimToNull(dto.getReferenceCompanyId()));
        payload.setTargetCompanyId(requireText(dto.getTargetCompanyId(), "目标公司不能为空"));
        payload.setEnabledYearMonth(requireEnabledYearMonth(dto.getEnabledYearMonth()));
        payload.setTemplateCode(trimToNull(dto.getTemplateCode()));
        payload.setSupervisorUserId(dto.getSupervisorUserId());
        payload.setSubjectCodeScheme(normalizeScheme(dto.getSubjectCodeScheme()));
        return payload;
    }

    private void validateBeforeSubmit(FinanceAccountSetTaskPayload payload) {
        requireEnabledCompany(payload.getTargetCompanyId());
        requireSupervisor(payload.getSupervisorUserId());

        if (financeAccountSetMapper().selectById(payload.getTargetCompanyId()) != null) {
            throw new IllegalStateException("目标公司已存在账套");
        }

        AsyncTaskRecord activeTask = asyncTaskRecordMapper().selectOne(
                Wrappers.<AsyncTaskRecord>lambdaQuery()
                        .eq(AsyncTaskRecord::getTaskType, AsyncTaskSupport.TASK_TYPE_FINANCE_ACCOUNT_SET_CREATE)
                        .eq(AsyncTaskRecord::getBusinessKey, payload.getTargetCompanyId())
                        .in(AsyncTaskRecord::getStatus, AsyncTaskSupport.TASK_STATUS_PENDING, AsyncTaskSupport.TASK_STATUS_RUNNING)
                        .orderByDesc(AsyncTaskRecord::getCreatedAt, AsyncTaskRecord::getId)
                        .last("limit 1")
        );
        if (activeTask != null) {
            throw new IllegalStateException("当前公司已有建账套任务执行中");
        }

        if (CREATE_MODE_REFERENCE.equals(payload.getCreateMode())) {
            String referenceCompanyId = requireText(payload.getReferenceCompanyId(), "参照账套不能为空");
            if (referenceCompanyId.equals(payload.getTargetCompanyId())) {
                throw new IllegalArgumentException("目标公司与参照账套公司不能相同");
            }
            FinanceAccountSet referenceAccountSet = financeAccountSetMapper().selectById(referenceCompanyId);
            if (referenceAccountSet == null || !"ACTIVE".equalsIgnoreCase(referenceAccountSet.getStatus())) {
                throw new IllegalStateException("参照账套不存在或未启用");
            }
            requireEnabledCompany(referenceCompanyId);
            validateTemplate(referenceAccountSet.getTemplateCode());
            return;
        }

        String templateCode = requireText(payload.getTemplateCode(), "账套模板不能为空");
        validateTemplate(templateCode);
        validateScheme(payload.getSubjectCodeScheme());
    }

    private void validateTemplate(String templateCode) {
        String normalizedTemplateCode = requireText(templateCode, "账套模板不能为空");
        FinanceAccountSetTemplate template = financeAccountSetTemplateMapper().selectById(normalizedTemplateCode);
        if (template == null) {
            throw new IllegalStateException("账套模板不存在");
        }
        if (!Integer.valueOf(1).equals(template.getStatus())) {
            throw new IllegalStateException("账套模板已停用");
        }
    }
}
