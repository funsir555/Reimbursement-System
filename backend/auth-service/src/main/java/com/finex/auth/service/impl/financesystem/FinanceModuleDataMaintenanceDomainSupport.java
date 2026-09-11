package com.finex.auth.service.impl.financesystem;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.FinanceModuleBackupDTO;
import com.finex.auth.dto.FinanceModuleBackupRecordVO;
import com.finex.auth.dto.FinanceModuleClearDTO;
import com.finex.auth.entity.FinanceAccountSetModuleBackupLog;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.FinanceAccountSetModuleBackupLogMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.support.FinanceModuleEnableSupport;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class FinanceModuleDataMaintenanceDomainSupport {

    private static final String BACKUP_STATUS_SUCCESS = "SUCCESS";
    private static final String BACKUP_STATUS_FAILED = "FAILED";
    private static final String CLEAR_BACKUP_REQUIRED_MESSAGE = "请先备份再清理";

    private final FinanceModuleCompanyContextSupport companyContextSupport;
    private final FinanceModuleEnableSupport financeModuleEnableSupport;
    private final FinanceModuleDataSupport financeModuleDataSupport;
    private final FinanceModuleBackupStorageService financeModuleBackupStorageService;
    private final FinanceAccountSetModuleBackupLogMapper financeAccountSetModuleBackupLogMapper;
    private final UserMapper userMapper;

    public FinanceModuleDataMaintenanceDomainSupport(
            FinanceModuleCompanyContextSupport companyContextSupport,
            FinanceModuleEnableSupport financeModuleEnableSupport,
            FinanceModuleDataSupport financeModuleDataSupport,
            FinanceModuleBackupStorageService financeModuleBackupStorageService,
            FinanceAccountSetModuleBackupLogMapper financeAccountSetModuleBackupLogMapper,
            UserMapper userMapper
    ) {
        this.companyContextSupport = companyContextSupport;
        this.financeModuleEnableSupport = financeModuleEnableSupport;
        this.financeModuleDataSupport = financeModuleDataSupport;
        this.financeModuleBackupStorageService = financeModuleBackupStorageService;
        this.financeAccountSetModuleBackupLogMapper = financeAccountSetModuleBackupLogMapper;
        this.userMapper = userMapper;
    }

    public FinanceModuleBackupRecordVO backupModuleData(Long currentUserId, FinanceModuleBackupDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("备份参数不能为空");
        }
        SystemCompany company = companyContextSupport.requireEnabledCompany(dto.getCompanyId());
        companyContextSupport.requireActiveAccountSet(company.getCompanyId());
        String moduleCode = normalizeModuleCode(dto.getModuleCode());
        validateBackupSupported(moduleCode);

        User operator = currentUserId == null ? null : userMapper.selectById(currentUserId);
        LocalDateTime startedAt = LocalDateTime.now();
        FinanceAccountSetModuleBackupLog log = new FinanceAccountSetModuleBackupLog();
        log.setCompanyId(company.getCompanyId());
        log.setModuleCode(moduleCode);
        log.setBackupStatus(BACKUP_STATUS_FAILED);
        log.setBackupStartedAt(startedAt);
        log.setBackupUserId(currentUserId);
        log.setBackupUserName(resolveOperatorName(currentUserId, operator));
        log.setRemark("模块备份生成中");
        financeAccountSetModuleBackupLogMapper.insert(log);

        try {
            String sqlContent = financeModuleDataSupport.buildBackupSql(company.getCompanyId(), moduleCode);
            Path filePath = financeModuleBackupStorageService.writeSql(company.getCompanyId(), moduleCode, startedAt, sqlContent);
            log.setBackupFileName(filePath.getFileName().toString());
            log.setBackupFilePath(filePath.toAbsolutePath().normalize().toString());
            log.setBackupStatus(BACKUP_STATUS_SUCCESS);
            log.setBackupFinishedAt(LocalDateTime.now());
            log.setFileSizeBytes(filePath.toFile().length());
            log.setRemark("模块备份成功");
            financeAccountSetModuleBackupLogMapper.updateById(log);
            return toRecord(log);
        } catch (RuntimeException ex) {
            log.setBackupFinishedAt(LocalDateTime.now());
            log.setBackupStatus(BACKUP_STATUS_FAILED);
            log.setRemark(limitRemark(ex.getMessage()));
            financeAccountSetModuleBackupLogMapper.updateById(log);
            throw ex;
        }
    }

    public List<FinanceModuleBackupRecordVO> listModuleBackupRecords(String companyId, String moduleCode) {
        SystemCompany company = companyContextSupport.requireEnabledCompany(companyId);
        companyContextSupport.requireActiveAccountSet(company.getCompanyId());
        String normalizedModuleCode = normalizeModuleCode(moduleCode);
        validateBackupSupported(normalizedModuleCode);
        return financeAccountSetModuleBackupLogMapper.selectList(
                        Wrappers.<FinanceAccountSetModuleBackupLog>lambdaQuery()
                                .eq(FinanceAccountSetModuleBackupLog::getCompanyId, company.getCompanyId())
                                .eq(FinanceAccountSetModuleBackupLog::getModuleCode, normalizedModuleCode)
                                .orderByDesc(FinanceAccountSetModuleBackupLog::getBackupStartedAt, FinanceAccountSetModuleBackupLog::getId)
                                .last("limit 50")
                ).stream()
                .map(this::toRecord)
                .toList();
    }

    public void clearModuleData(Long currentUserId, FinanceModuleClearDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("清理参数不能为空");
        }
        SystemCompany company = companyContextSupport.requireEnabledCompany(dto.getCompanyId());
        companyContextSupport.requireActiveAccountSet(company.getCompanyId());
        String moduleCode = normalizeModuleCode(dto.getModuleCode());
        validateClearSupported(moduleCode);
        requireRecentBackup(company.getCompanyId(), moduleCode);
        financeModuleDataSupport.clearModuleData(company.getCompanyId(), moduleCode);
    }

    private void requireRecentBackup(String companyId, String moduleCode) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<FinanceAccountSetModuleBackupLog> logs = financeAccountSetModuleBackupLogMapper.selectList(
                Wrappers.<FinanceAccountSetModuleBackupLog>lambdaQuery()
                        .eq(FinanceAccountSetModuleBackupLog::getCompanyId, companyId)
                        .eq(FinanceAccountSetModuleBackupLog::getModuleCode, moduleCode)
                        .eq(FinanceAccountSetModuleBackupLog::getBackupStatus, BACKUP_STATUS_SUCCESS)
                        .ge(FinanceAccountSetModuleBackupLog::getBackupStartedAt, threshold)
                        .orderByDesc(FinanceAccountSetModuleBackupLog::getBackupStartedAt, FinanceAccountSetModuleBackupLog::getId)
        );
        boolean hasValidBackup = logs.stream()
                .anyMatch(item -> financeModuleBackupStorageService.exists(item.getBackupFilePath()));
        if (!hasValidBackup) {
            throw new IllegalStateException(CLEAR_BACKUP_REQUIRED_MESSAGE);
        }
    }

    private void validateBackupSupported(String moduleCode) {
        if (!(Objects.equals(moduleCode, FinanceModuleEnableSupport.GENERAL_LEDGER)
                || Objects.equals(moduleCode, FinanceModuleEnableSupport.FIXED_ASSETS))) {
            throw new IllegalStateException("当前模块尚未开放数据备份");
        }
    }

    private void validateClearSupported(String moduleCode) {
        if (Objects.equals(moduleCode, FinanceModuleEnableSupport.GENERAL_LEDGER)) {
            throw new IllegalStateException(financeModuleDataSupport.resolveClearBlockedMessage(moduleCode));
        }
        if (!Objects.equals(moduleCode, FinanceModuleEnableSupport.FIXED_ASSETS)) {
            throw new IllegalStateException("当前模块尚未开放数据清理");
        }
    }

    private String normalizeModuleCode(String moduleCode) {
        return financeModuleEnableSupport.requireDefinition(moduleCode).code();
    }

    private String resolveOperatorName(Long currentUserId, User operator) {
        if (operator != null && companyContextSupport.trimToNull(operator.getName()) != null) {
            return operator.getName().trim();
        }
        if (currentUserId != null) {
            return "用户" + currentUserId;
        }
        return "未知用户";
    }

    private FinanceModuleBackupRecordVO toRecord(FinanceAccountSetModuleBackupLog item) {
        FinanceModuleBackupRecordVO record = new FinanceModuleBackupRecordVO();
        record.setId(item.getId());
        record.setCompanyId(item.getCompanyId());
        record.setModuleCode(item.getModuleCode());
        record.setModuleName(financeModuleEnableSupport.resolveModuleName(item.getModuleCode()));
        record.setBackupFileName(item.getBackupFileName());
        record.setBackupFilePath(item.getBackupFilePath());
        record.setBackupStatus(item.getBackupStatus());
        record.setBackupStartedAt(item.getBackupStartedAt());
        record.setBackupFinishedAt(item.getBackupFinishedAt());
        record.setBackupUserId(item.getBackupUserId());
        record.setBackupUserName(item.getBackupUserName());
        record.setFileSizeBytes(item.getFileSizeBytes());
        record.setRemark(item.getRemark());
        return record;
    }

    private String limitRemark(String value) {
        String normalized = companyContextSupport.trimToNull(value);
        if (normalized == null) {
            return "模块备份失败";
        }
        if (normalized.length() <= 500) {
            return normalized;
        }
        return normalized.substring(0, 500);
    }
}
