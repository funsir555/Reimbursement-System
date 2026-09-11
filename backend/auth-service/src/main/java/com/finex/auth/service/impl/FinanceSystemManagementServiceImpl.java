// 业务域：财务系统管理
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 财务系统设置和账套相关接口，下游会继续协调 账套、同步任务和财务上下文基础数据。
// 风险提醒：改坏后最容易影响 账套切换、基础数据同步和下游系统连接。

package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSetCreateDTO;
import com.finex.auth.dto.FinanceAccountSetMetaVO;
import com.finex.auth.dto.FinanceAccountSetSummaryVO;
import com.finex.auth.dto.FinanceAccountSetTaskStatusVO;
import com.finex.auth.dto.FinanceModuleBackupDTO;
import com.finex.auth.dto.FinanceModuleBackupRecordVO;
import com.finex.auth.dto.FinanceModuleClearDTO;
import com.finex.auth.dto.FinanceModuleEnableMetaVO;
import com.finex.auth.dto.FinanceModuleEnableToggleDTO;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.FaAssetCardMapper;
import com.finex.auth.mapper.FaAssetCategoryMapper;
import com.finex.auth.mapper.FaAssetChangeBillMapper;
import com.finex.auth.mapper.FaAssetChangeLineMapper;
import com.finex.auth.mapper.FaAssetDeprLineMapper;
import com.finex.auth.mapper.FaAssetDeprRunMapper;
import com.finex.auth.mapper.FaAssetDisposalBillMapper;
import com.finex.auth.mapper.FaAssetDisposalLineMapper;
import com.finex.auth.mapper.FaAssetOpeningImportLineMapper;
import com.finex.auth.mapper.FaAssetOpeningImportMapper;
import com.finex.auth.mapper.FaAssetPeriodCloseMapper;
import com.finex.auth.mapper.FaAssetVoucherLinkMapper;
import com.finex.auth.mapper.FinanceAccountSetCodeRuleMapper;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSetModuleBackupLogMapper;
import com.finex.auth.mapper.FinanceAccountSetModuleEnableMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateSubjectMapper;
import com.finex.auth.mapper.FinanceOpeningBalanceStateMapper;
import com.finex.auth.mapper.FinancePeriodCloseLogMapper;
import com.finex.auth.mapper.FinancePeriodCloseMapper;
import com.finex.auth.mapper.FinancePostVoucherStateMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.FinanceSystemManagementService;
import com.finex.auth.service.impl.financesystem.FinanceAccountSetMetaSupport;
import com.finex.auth.service.impl.financesystem.FinanceAccountSetQueryDomainSupport;
import com.finex.auth.service.impl.financesystem.FinanceAccountSetTaskDomainSupport;
import com.finex.auth.service.impl.financesystem.FinanceModuleBackupStorageService;
import com.finex.auth.service.impl.financesystem.FinanceModuleCompanyContextSupport;
import com.finex.auth.service.impl.financesystem.FinanceModuleDataMaintenanceDomainSupport;
import com.finex.auth.service.impl.financesystem.FinanceModuleDataSupport;
import com.finex.auth.service.impl.financesystem.FinanceModuleEnableDomainSupport;
import com.finex.auth.mapper.GlAccassMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.support.FinanceModuleEnableSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * FinanceSystemManagementServiceImpl：service 入口实现。
 * 接住上层请求，并把 财务系统管理相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 账套切换、基础数据同步和下游系统连接是否会被一起带坏。
 */
@Service
public class FinanceSystemManagementServiceImpl implements FinanceSystemManagementService {

    private final FinanceAccountSetMetaSupport financeAccountSetMetaSupport;
    private final FinanceAccountSetQueryDomainSupport financeAccountSetQueryDomainSupport;
    private final FinanceAccountSetTaskDomainSupport financeAccountSetTaskDomainSupport;
    private final FinanceModuleEnableDomainSupport financeModuleEnableDomainSupport;
    private final FinanceModuleDataMaintenanceDomainSupport financeModuleDataMaintenanceDomainSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FinanceSystemManagementServiceImpl(
            FinanceAccountSetMapper financeAccountSetMapper,
            FinanceAccountSetCodeRuleMapper financeAccountSetCodeRuleMapper,
            FinanceAccountSetTemplateMapper financeAccountSetTemplateMapper,
            FinanceAccountSetTemplateSubjectMapper financeAccountSetTemplateSubjectMapper,
            SystemCompanyMapper systemCompanyMapper,
            UserMapper userMapper,
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            FinanceAccountSetTaskWorker financeAccountSetTaskWorker,
            ObjectMapper objectMapper,
            FinanceAccountSetModuleEnableMapper financeAccountSetModuleEnableMapper,
            FinanceAccountSetModuleBackupLogMapper financeAccountSetModuleBackupLogMapper,
            GlAccvouchMapper glAccvouchMapper,
            GlAccsumMapper glAccsumMapper,
            GlAccassMapper glAccassMapper,
            FinanceOpeningBalanceStateMapper financeOpeningBalanceStateMapper,
            FinancePostVoucherStateMapper financePostVoucherStateMapper,
            FinancePeriodCloseMapper financePeriodCloseMapper,
            FinancePeriodCloseLogMapper financePeriodCloseLogMapper,
            FaAssetCategoryMapper faAssetCategoryMapper,
            FaAssetCardMapper faAssetCardMapper,
            FaAssetChangeBillMapper faAssetChangeBillMapper,
            FaAssetChangeLineMapper faAssetChangeLineMapper,
            FaAssetDeprRunMapper faAssetDeprRunMapper,
            FaAssetDeprLineMapper faAssetDeprLineMapper,
            FaAssetDisposalBillMapper faAssetDisposalBillMapper,
            FaAssetDisposalLineMapper faAssetDisposalLineMapper,
            FaAssetOpeningImportMapper faAssetOpeningImportMapper,
            FaAssetOpeningImportLineMapper faAssetOpeningImportLineMapper,
            FaAssetPeriodCloseMapper faAssetPeriodCloseMapper,
            FaAssetVoucherLinkMapper faAssetVoucherLinkMapper,
            FinanceModuleBackupStorageService financeModuleBackupStorageService
    ) {
        this.financeAccountSetMetaSupport = new FinanceAccountSetMetaSupport(
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
        this.financeAccountSetQueryDomainSupport = new FinanceAccountSetQueryDomainSupport(
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
        this.financeAccountSetTaskDomainSupport = new FinanceAccountSetTaskDomainSupport(
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
        FinanceModuleEnableSupport financeModuleEnableSupport =
                new FinanceModuleEnableSupport(financeAccountSetModuleEnableMapper);
        FinanceModuleCompanyContextSupport financeModuleCompanyContextSupport =
                new FinanceModuleCompanyContextSupport(systemCompanyMapper, financeAccountSetMapper);
        FinanceModuleDataSupport financeModuleDataSupport = new FinanceModuleDataSupport(
                glAccvouchMapper,
                glAccsumMapper,
                glAccassMapper,
                financeOpeningBalanceStateMapper,
                financePostVoucherStateMapper,
                financePeriodCloseMapper,
                financePeriodCloseLogMapper,
                faAssetCategoryMapper,
                faAssetCardMapper,
                faAssetChangeBillMapper,
                faAssetChangeLineMapper,
                faAssetDeprRunMapper,
                faAssetDeprLineMapper,
                faAssetDisposalBillMapper,
                faAssetDisposalLineMapper,
                faAssetOpeningImportMapper,
                faAssetOpeningImportLineMapper,
                faAssetPeriodCloseMapper,
                faAssetVoucherLinkMapper
        );
        this.financeModuleEnableDomainSupport = new FinanceModuleEnableDomainSupport(
                systemCompanyMapper,
                financeAccountSetMapper,
                financeAccountSetModuleEnableMapper,
                glAccvouchMapper,
                glAccsumMapper,
                glAccassMapper,
                financeOpeningBalanceStateMapper,
                financePostVoucherStateMapper,
                financePeriodCloseMapper,
                financePeriodCloseLogMapper,
                faAssetCategoryMapper,
                faAssetCardMapper,
                faAssetChangeBillMapper,
                faAssetChangeLineMapper,
                faAssetDeprRunMapper,
                faAssetDeprLineMapper,
                faAssetDisposalBillMapper,
                faAssetDisposalLineMapper,
                faAssetOpeningImportMapper,
                faAssetOpeningImportLineMapper,
                faAssetPeriodCloseMapper,
                faAssetVoucherLinkMapper,
                financeModuleEnableSupport
        );
        this.financeModuleDataMaintenanceDomainSupport = new FinanceModuleDataMaintenanceDomainSupport(
                financeModuleCompanyContextSupport,
                financeModuleEnableSupport,
                financeModuleDataSupport,
                financeModuleBackupStorageService,
                financeAccountSetModuleBackupLogMapper,
                userMapper
        );
    }

    /**
     * 获取元数据。
     */
    @Override
    public FinanceAccountSetMetaVO getMeta() {
        return financeAccountSetMetaSupport.getMeta();
    }

    /**
     * 查询账户Sets列表。
     */
    @Override
    public List<FinanceAccountSetSummaryVO> listAccountSets() {
        return financeAccountSetQueryDomainSupport.listAccountSets();
    }

    /**
     * 提交创建任务。
     */
    @Override
    public FinanceAccountSetTaskStatusVO submitCreateTask(Long currentUserId, FinanceAccountSetCreateDTO dto) {
        return financeAccountSetTaskDomainSupport.submitCreateTask(currentUserId, dto);
    }

    /**
     * 获取任务Status。
     */
    @Override
    public FinanceAccountSetTaskStatusVO getTaskStatus(String taskNo) {
        return financeAccountSetTaskDomainSupport.getTaskStatus(taskNo);
    }

    @Override
    public FinanceModuleEnableMetaVO getModuleEnableMeta(String companyId) {
        return financeModuleEnableDomainSupport.getModuleEnableMeta(companyId);
    }

    @Override
    public FinanceModuleEnableMetaVO toggleModuleEnable(FinanceModuleEnableToggleDTO dto) {
        return financeModuleEnableDomainSupport.toggleModule(dto);
    }

    @Override
    public FinanceModuleBackupRecordVO backupModuleData(Long currentUserId, FinanceModuleBackupDTO dto) {
        return financeModuleDataMaintenanceDomainSupport.backupModuleData(currentUserId, dto);
    }

    @Override
    public List<FinanceModuleBackupRecordVO> listModuleBackupRecords(String companyId, String moduleCode) {
        return financeModuleDataMaintenanceDomainSupport.listModuleBackupRecords(companyId, moduleCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceModuleEnableMetaVO clearModuleData(Long currentUserId, FinanceModuleClearDTO dto) {
        financeModuleDataMaintenanceDomainSupport.clearModuleData(currentUserId, dto);
        return financeModuleEnableDomainSupport.getModuleEnableMeta(dto.getCompanyId());
    }
}
