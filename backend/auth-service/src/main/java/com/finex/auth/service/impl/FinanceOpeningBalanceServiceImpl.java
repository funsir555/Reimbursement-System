package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.OpeningBalanceAssistSaveDTO;
import com.finex.auth.dto.OpeningBalanceMetaVO;
import com.finex.auth.dto.OpeningBalanceReconcileResultVO;
import com.finex.auth.dto.OpeningBalanceRowVO;
import com.finex.auth.dto.OpeningBalanceSaveDTO;
import com.finex.auth.dto.OpeningBalanceTaskRequestDTO;
import com.finex.auth.dto.OpeningBalanceTrialResultVO;
import com.finex.auth.dto.OpeningAssistBalanceLineVO;
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
import com.finex.auth.service.FinanceOpeningBalanceService;
import com.finex.auth.service.impl.openingbalance.OpeningBalanceMetaSupport;
import com.finex.auth.service.impl.openingbalance.OpeningBalanceMutationSupport;
import com.finex.auth.service.impl.openingbalance.OpeningBalanceQuerySupport;
import com.finex.auth.service.impl.openingbalance.OpeningBalanceTaskSupport;
import com.finex.auth.service.impl.openingbalance.OpeningBalanceTaskWorker;
import com.finex.auth.service.impl.openingbalance.OpeningBalanceTrialReconcileSupport;
import com.finex.auth.service.impl.openingbalance.SharedOpeningBalanceSupport;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceOpeningBalanceServiceImpl implements FinanceOpeningBalanceService {

    private final OpeningBalanceMetaSupport openingBalanceMetaSupport;
    private final OpeningBalanceQuerySupport openingBalanceQuerySupport;
    private final OpeningBalanceMutationSupport openingBalanceMutationSupport;
    private final OpeningBalanceTrialReconcileSupport openingBalanceTrialReconcileSupport;
    private final OpeningBalanceTaskSupport openingBalanceTaskSupport;

    public FinanceOpeningBalanceServiceImpl(
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            FinanceCustomerMapper financeCustomerMapper,
            FinanceVendorMapper financeVendorMapper,
            FinanceProjectClassMapper financeProjectClassMapper,
            FinanceProjectArchiveMapper financeProjectArchiveMapper,
            SystemCompanyMapper systemCompanyMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper,
            GlAccsumMapper glAccsumMapper,
            GlAccassMapper glAccassMapper,
            FinanceOpeningBalanceStateMapper financeOpeningBalanceStateMapper,
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            ObjectMapper objectMapper,
            OpeningBalanceTaskWorker openingBalanceTaskWorker
    ) {
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
        this.openingBalanceMetaSupport = new OpeningBalanceMetaSupport(support);
        this.openingBalanceQuerySupport = new OpeningBalanceQuerySupport(support);
        this.openingBalanceMutationSupport = new OpeningBalanceMutationSupport(support);
        this.openingBalanceTrialReconcileSupport = new OpeningBalanceTrialReconcileSupport(support);
        this.openingBalanceTaskSupport = new OpeningBalanceTaskSupport(asyncTaskRecordMapper, objectMapper, openingBalanceTaskWorker);
    }

    @Override
    public OpeningBalanceMetaVO getMeta(Long currentUserId, String currentUsername, String companyId, Integer iyear, Integer iperiod) {
        return openingBalanceMetaSupport.getMeta(currentUserId, currentUsername, companyId, iyear, iperiod);
    }

    @Override
    public List<OpeningBalanceRowVO> listRows(String companyId, Integer iyear, Integer iperiod) {
        return openingBalanceQuerySupport.listRows(companyId, iyear, iperiod);
    }

    @Override
    public List<OpeningAssistBalanceLineVO> getAssistBalances(String companyId, Integer iyear, Integer iperiod, String subjectCode) {
        return openingBalanceQuerySupport.getAssistBalances(companyId, iyear, iperiod, subjectCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OpeningBalanceRowVO> saveRows(OpeningBalanceSaveDTO dto, String operatorName) {
        return openingBalanceMutationSupport.saveRows(dto, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<OpeningAssistBalanceLineVO> saveAssistBalances(String subjectCode, OpeningBalanceAssistSaveDTO dto, String operatorName) {
        return openingBalanceMutationSupport.saveAssistBalances(subjectCode, dto, operatorName);
    }

    @Override
    public AsyncTaskSubmitResultVO openBook(Long currentUserId, String operatorName, OpeningBalanceTaskRequestDTO dto) {
        return openingBalanceTaskSupport.openBook(currentUserId, operatorName, dto);
    }

    @Override
    public AsyncTaskSubmitResultVO carryForward(Long currentUserId, String operatorName, OpeningBalanceTaskRequestDTO dto) {
        return openingBalanceTaskSupport.carryForward(currentUserId, operatorName, dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpeningBalanceTrialResultVO trialBalance(String companyId, Integer iyear, Integer iperiod, String operatorName) {
        return openingBalanceTrialReconcileSupport.trialBalance(companyId, iyear, iperiod, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpeningBalanceReconcileResultVO reconcile(String companyId, Integer iyear, Integer iperiod, String operatorName) {
        return openingBalanceTrialReconcileSupport.reconcile(companyId, iyear, iperiod, operatorName);
    }
}
