package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.FinancePostVoucherMetaVO;
import com.finex.auth.dto.FinancePostVoucherTaskRequestDTO;
import com.finex.auth.dto.FinancePostVoucherTaskStatusVO;
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
import com.finex.auth.service.FinancePostVoucherService;
import com.finex.auth.service.impl.postvoucher.PostVoucherMetaSupport;
import com.finex.auth.service.impl.postvoucher.PostVoucherMutationSupport;
import com.finex.auth.service.impl.postvoucher.PostVoucherTaskSupport;
import com.finex.auth.service.impl.postvoucher.PostVoucherTaskWorker;
import com.finex.auth.service.impl.postvoucher.PostVoucherValidationSupport;
import com.finex.auth.service.impl.postvoucher.SharedPostVoucherSupport;
import org.springframework.stereotype.Service;

@Service
public class FinancePostVoucherServiceImpl implements FinancePostVoucherService {

    private final PostVoucherMetaSupport postVoucherMetaSupport;
    private final PostVoucherValidationSupport postVoucherValidationSupport;
    private final PostVoucherMutationSupport postVoucherMutationSupport;
    private final PostVoucherTaskSupport postVoucherTaskSupport;

    public FinancePostVoucherServiceImpl(
            FinanceAccountSetMapper financeAccountSetMapper,
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            FinanceOpeningBalanceStateMapper financeOpeningBalanceStateMapper,
            FinancePostVoucherStateMapper financePostVoucherStateMapper,
            FinancePeriodCloseMapper financePeriodCloseMapper,
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            GlAccvouchMapper glAccvouchMapper,
            GlAccsumMapper glAccsumMapper,
            GlAccassMapper glAccassMapper,
            SystemCompanyMapper systemCompanyMapper,
            UserMapper userMapper,
            ObjectMapper objectMapper,
            PostVoucherTaskWorker postVoucherTaskWorker
    ) {
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
        this.postVoucherMetaSupport = new PostVoucherMetaSupport(support);
        this.postVoucherValidationSupport = new PostVoucherValidationSupport(support);
        this.postVoucherMutationSupport = new PostVoucherMutationSupport(support);
        this.postVoucherTaskSupport = new PostVoucherTaskSupport(asyncTaskRecordMapper, objectMapper, postVoucherTaskWorker);
    }

    @Override
    public FinancePostVoucherMetaVO getMeta(Long currentUserId, String companyId, Integer iyear, Integer iperiod) {
        return postVoucherMetaSupport.getMeta(currentUserId, companyId, iyear, iperiod);
    }

    @Override
    public AsyncTaskSubmitResultVO runPosting(Long currentUserId, String operatorName, FinancePostVoucherTaskRequestDTO dto) {
        postVoucherValidationSupport.prepareRun(dto.getCompanyId(), dto.getIyear(), dto.getIperiod());
        return postVoucherTaskSupport.runPosting(
                currentUserId,
                postVoucherValidationSupport.resolveOperatorName(currentUserId, operatorName),
                dto
        );
    }

    @Override
    public FinancePostVoucherTaskStatusVO getTaskStatus(String taskNo) {
        return postVoucherMutationSupport.getTaskStatus(taskNo);
    }
}
