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
        task.setDisplayName("鏂板缓璐﹀");
        task.setStatus(AsyncTaskSupport.TASK_STATUS_PENDING);
        task.setProgress(0);
        task.setResultMessage("璐﹀鍒涘缓浠诲姟宸叉彁浜?");
        task.setResultPayload(writePayload(payload));
        asyncTaskRecordMapper().insert(task);

        financeAccountSetTaskWorker().runCreateAccountSetTask(task.getId());
        return toTaskStatus(task, null);
    }

    public FinanceAccountSetTaskStatusVO getTaskStatus(String taskNo) {
        String normalizedTaskNo = trimToNull(taskNo);
        if (normalizedTaskNo == null) {
            throw new IllegalArgumentException("浠诲姟鍙蜂笉鑳戒负绌?");
        }
        AsyncTaskRecord task = asyncTaskRecordMapper().selectOne(
                Wrappers.<AsyncTaskRecord>lambdaQuery()
                        .eq(AsyncTaskRecord::getTaskNo, normalizedTaskNo)
                        .last("limit 1")
        );
        if (task == null) {
            throw new IllegalStateException("璐﹀浠诲姟涓嶅瓨鍦?");
        }
        FinanceAccountSet accountSet = task.getCompanyId() == null ? null : financeAccountSetMapper().selectById(task.getCompanyId());
        return toTaskStatus(task, accountSet == null ? null : accountSet.getStatus());
    }

    private FinanceAccountSetTaskPayload normalizePayload(FinanceAccountSetCreateDTO dto) {
        FinanceAccountSetTaskPayload payload = new FinanceAccountSetTaskPayload();
        payload.setCreateMode(normalizeCreateMode(dto.getCreateMode()));
        payload.setReferenceCompanyId(trimToNull(dto.getReferenceCompanyId()));
        payload.setTargetCompanyId(requireText(dto.getTargetCompanyId(), "鐩爣鍏徃涓嶈兘涓虹┖"));
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
            throw new IllegalStateException("鐩爣鍏徃宸插瓨鍦ㄨ处濂楋紝涓嶈兘閲嶅鍒涘缓");
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
            throw new IllegalStateException("褰撳墠鍏徃宸叉湁璐﹀鍒涘缓浠诲姟姝ｅ湪鎵ц");
        }

        if (CREATE_MODE_REFERENCE.equals(payload.getCreateMode())) {
            String referenceCompanyId = requireText(payload.getReferenceCompanyId(), "鍙傜収璐﹀涓嶈兘涓虹┖");
            if (referenceCompanyId.equals(payload.getTargetCompanyId())) {
                throw new IllegalArgumentException("鐩爣鍏徃涓庡弬鐓ц处濂楀叕鍙镐笉鑳界浉鍚?");
            }
            FinanceAccountSet referenceAccountSet = financeAccountSetMapper().selectById(referenceCompanyId);
            if (referenceAccountSet == null || !"ACTIVE".equalsIgnoreCase(referenceAccountSet.getStatus())) {
                throw new IllegalStateException("鍙傜収璐﹀涓嶅瓨鍦ㄦ垨灏氭湭鍚敤");
            }
            requireEnabledCompany(referenceCompanyId);
            return;
        }

        String templateCode = requireText(payload.getTemplateCode(), "璐﹀妯℃澘涓嶈兘涓虹┖");
        FinanceAccountSetTemplate template = financeAccountSetTemplateMapper().selectById(templateCode);
        if (template == null || !Integer.valueOf(1).equals(template.getStatus())) {
            throw new IllegalStateException("璐﹀妯℃澘涓嶅瓨鍦?");
        }
        validateScheme(payload.getSubjectCodeScheme());
    }
}
