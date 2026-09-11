package com.finex.auth.service.impl.financesystem;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.FinanceModuleEnableMetaVO;
import com.finex.auth.dto.FinanceModuleEnableSummaryVO;
import com.finex.auth.dto.FinanceModuleEnableToggleDTO;
import com.finex.auth.entity.FaAssetCard;
import com.finex.auth.entity.FaAssetCategory;
import com.finex.auth.entity.FaAssetChangeBill;
import com.finex.auth.entity.FaAssetChangeLine;
import com.finex.auth.entity.FaAssetDeprLine;
import com.finex.auth.entity.FaAssetDeprRun;
import com.finex.auth.entity.FaAssetDisposalBill;
import com.finex.auth.entity.FaAssetDisposalLine;
import com.finex.auth.entity.FaAssetOpeningImport;
import com.finex.auth.entity.FaAssetOpeningImportLine;
import com.finex.auth.entity.FaAssetPeriodClose;
import com.finex.auth.entity.FaAssetVoucherLink;
import com.finex.auth.entity.FinanceAccountSetModuleEnable;
import com.finex.auth.entity.FinanceOpeningBalanceState;
import com.finex.auth.entity.FinancePeriodClose;
import com.finex.auth.entity.FinancePeriodCloseLog;
import com.finex.auth.entity.FinancePostVoucherState;
import com.finex.auth.entity.GlAccass;
import com.finex.auth.entity.GlAccsum;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.entity.SystemCompany;
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
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSetModuleEnableMapper;
import com.finex.auth.mapper.FinanceOpeningBalanceStateMapper;
import com.finex.auth.mapper.FinancePeriodCloseLogMapper;
import com.finex.auth.mapper.FinancePeriodCloseMapper;
import com.finex.auth.mapper.FinancePostVoucherStateMapper;
import com.finex.auth.mapper.GlAccassMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.support.FinanceModuleEnableSupport;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class FinanceModuleEnableDomainSupport {

    private static final String DISABLE_BLOCKED_MESSAGE = "请备份数据后清除所有内容再关闭";
    private static final String BUILDING_MESSAGE = "建设中";
    private static final List<String> PLACEHOLDER_ACTIONS = List.of("BACKUP_DATA", "CLEAR_DATA");

    private final FinanceAccountSetModuleEnableMapper financeAccountSetModuleEnableMapper;
    private final FinanceModuleCompanyContextSupport companyContextSupport;
    private final FinanceModuleEnableSupport financeModuleEnableSupport;
    private final FinanceModuleDataSupport financeModuleDataSupport;

    public FinanceModuleEnableDomainSupport(
            SystemCompanyMapper systemCompanyMapper,
            FinanceAccountSetMapper financeAccountSetMapper,
            FinanceAccountSetModuleEnableMapper financeAccountSetModuleEnableMapper,
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
            FinanceModuleEnableSupport financeModuleEnableSupport
    ) {
        this.financeAccountSetModuleEnableMapper = financeAccountSetModuleEnableMapper;
        this.companyContextSupport = new FinanceModuleCompanyContextSupport(systemCompanyMapper, financeAccountSetMapper);
        this.financeModuleEnableSupport = financeModuleEnableSupport;
        this.financeModuleDataSupport = new FinanceModuleDataSupport(
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
    }

    public FinanceModuleEnableMetaVO getModuleEnableMeta(String companyId) {
        SystemCompany company = companyContextSupport.requireEnabledCompany(companyId);
        companyContextSupport.requireActiveAccountSet(company.getCompanyId());
        Map<String, FinanceAccountSetModuleEnable> recordMap = ensureModuleRecordMap(company.getCompanyId());

        FinanceModuleEnableMetaVO meta = new FinanceModuleEnableMetaVO();
        meta.setCompanyId(company.getCompanyId());
        meta.setCompanyName(company.getCompanyName());
        meta.setModules(financeModuleEnableSupport.moduleDefinitions().stream()
                .map(definition -> toSummary(company.getCompanyId(), definition, recordMap.get(definition.code())))
                .toList());
        return meta;
    }

    public FinanceModuleEnableMetaVO toggleModule(FinanceModuleEnableToggleDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("启停参数不能为空");
        }
        SystemCompany company = companyContextSupport.requireEnabledCompany(dto.getCompanyId());
        companyContextSupport.requireActiveAccountSet(company.getCompanyId());

        FinanceModuleEnableSupport.ModuleDefinition definition = financeModuleEnableSupport.requireDefinition(dto.getModuleCode());
        if (!definition.toggleable()) {
            throw new IllegalStateException("当前模块尚未开放启停");
        }
        boolean enableTarget = Boolean.TRUE.equals(dto.getEnabled());
        if (!enableTarget && hasModuleData(company.getCompanyId(), definition.code())) {
            throw new IllegalStateException(DISABLE_BLOCKED_MESSAGE);
        }

        Map<String, FinanceAccountSetModuleEnable> recordMap = ensureModuleRecordMap(company.getCompanyId());
        FinanceAccountSetModuleEnable record = recordMap.get(definition.code());
        if (record == null) {
            record = new FinanceAccountSetModuleEnable();
            record.setCompanyId(company.getCompanyId());
            record.setModuleCode(definition.code());
            record.setEnabled(enableTarget ? 1 : 0);
            financeAccountSetModuleEnableMapper.insert(record);
        } else {
            record.setEnabled(enableTarget ? 1 : 0);
            financeAccountSetModuleEnableMapper.updateById(record);
        }
        return getModuleEnableMeta(company.getCompanyId());
    }

    private FinanceModuleEnableSummaryVO toSummary(
            String companyId,
            FinanceModuleEnableSupport.ModuleDefinition definition,
            FinanceAccountSetModuleEnable record
    ) {
        boolean enabled = record != null && Objects.equals(record.getEnabled(), 1);
        boolean implemented = definition.implemented();
        boolean disableAllowed = implemented && enabled && !hasModuleData(companyId, definition.code());

        FinanceModuleEnableSummaryVO summary = new FinanceModuleEnableSummaryVO();
        summary.setCompanyId(companyId);
        summary.setModuleCode(definition.code());
        summary.setModuleName(definition.name());
        summary.setEnabled(enabled);
        summary.setImplemented(implemented);
        summary.setToggleAllowed(definition.toggleable());
        summary.setDisableAllowed(disableAllowed);
        summary.setBackupAllowed(implemented);
        summary.setBackupRecordAllowed(implemented);
        summary.setClearAllowed(Objects.equals(definition.code(), FinanceModuleEnableSupport.FIXED_ASSETS));
        summary.setPlaceholderActions(PLACEHOLDER_ACTIONS);
        summary.setClearBlockedMessage(financeModuleDataSupport.resolveClearBlockedMessage(definition.code()));
        if (!implemented) {
            summary.setBlockedMessage(BUILDING_MESSAGE);
        } else if (enabled && !disableAllowed) {
            summary.setBlockedMessage(DISABLE_BLOCKED_MESSAGE);
        } else {
            summary.setBlockedMessage(null);
        }
        return summary;
    }

    private Map<String, FinanceAccountSetModuleEnable> ensureModuleRecordMap(String companyId) {
        Map<String, FinanceAccountSetModuleEnable> recordMap = financeAccountSetModuleEnableMapper.selectList(
                        Wrappers.<FinanceAccountSetModuleEnable>lambdaQuery()
                                .eq(FinanceAccountSetModuleEnable::getCompanyId, companyId)
                                .orderByAsc(FinanceAccountSetModuleEnable::getId)
                ).stream()
                .collect(java.util.stream.Collectors.toMap(
                        FinanceAccountSetModuleEnable::getModuleCode,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        if (recordMap.size() == financeModuleEnableSupport.moduleDefinitions().size()) {
            return recordMap;
        }
        for (FinanceModuleEnableSupport.ModuleDefinition definition : financeModuleEnableSupport.moduleDefinitions()) {
            if (recordMap.containsKey(definition.code())) {
                continue;
            }
            FinanceAccountSetModuleEnable record = new FinanceAccountSetModuleEnable();
            record.setCompanyId(companyId);
            record.setModuleCode(definition.code());
            record.setEnabled(resolveHistoricalDefaultEnabled(companyId, definition.code()) ? 1 : 0);
            financeAccountSetModuleEnableMapper.insert(record);
            recordMap.put(definition.code(), record);
        }
        return recordMap;
    }

    private boolean resolveHistoricalDefaultEnabled(String companyId, String moduleCode) {
        if (Objects.equals(moduleCode, FinanceModuleEnableSupport.GENERAL_LEDGER)) {
            return true;
        }
        if (Objects.equals(moduleCode, FinanceModuleEnableSupport.FIXED_ASSETS)) {
            return financeModuleDataSupport.hasModuleData(companyId, moduleCode);
        }
        return false;
    }

    private boolean hasModuleData(String companyId, String moduleCode) {
        return financeModuleDataSupport.hasModuleData(companyId, moduleCode);
    }

    private boolean hasRows(Number value) {
        return value != null && value.longValue() > 0L;
    }
}
