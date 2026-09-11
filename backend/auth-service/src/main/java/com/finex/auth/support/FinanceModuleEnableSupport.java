package com.finex.auth.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.FinanceAccountSetModuleEnable;
import com.finex.auth.mapper.FinanceAccountSetModuleEnableMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class FinanceModuleEnableSupport {

    public static final String GENERAL_LEDGER = "GENERAL_LEDGER";
    public static final String FIXED_ASSETS = "FIXED_ASSETS";
    public static final String CASH_MANAGEMENT = "CASH_MANAGEMENT";
    public static final String RECEIVABLE_MANAGEMENT = "RECEIVABLE_MANAGEMENT";
    public static final String COST_MANAGEMENT = "COST_MANAGEMENT";
    public static final String DISABLED_MESSAGE = "系统未启用";

    private static final List<ModuleDefinition> MODULE_DEFINITIONS = List.of(
            new ModuleDefinition(GENERAL_LEDGER, "总账", true, true),
            new ModuleDefinition(FIXED_ASSETS, "固定资产", true, true),
            new ModuleDefinition(CASH_MANAGEMENT, "出纳管理", false, false),
            new ModuleDefinition(RECEIVABLE_MANAGEMENT, "应收管理", false, false),
            new ModuleDefinition(COST_MANAGEMENT, "成本管理", false, false)
    );

    private final FinanceAccountSetModuleEnableMapper financeAccountSetModuleEnableMapper;

    public FinanceModuleEnableSupport(FinanceAccountSetModuleEnableMapper financeAccountSetModuleEnableMapper) {
        this.financeAccountSetModuleEnableMapper = financeAccountSetModuleEnableMapper;
    }

    public List<ModuleDefinition> moduleDefinitions() {
        return MODULE_DEFINITIONS;
    }

    public ModuleDefinition requireDefinition(String moduleCode) {
        return moduleDefinitions().stream()
                .filter(item -> Objects.equals(item.code(), normalizeModuleCode(moduleCode)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的模块编码"));
    }

    public boolean isImplemented(String moduleCode) {
        return requireDefinition(moduleCode).implemented();
    }

    public boolean isToggleable(String moduleCode) {
        return requireDefinition(moduleCode).toggleable();
    }

    public String resolveModuleName(String moduleCode) {
        return requireDefinition(moduleCode).name();
    }

    public boolean isEnabled(String companyId, String moduleCode) {
        String normalizedCompanyId = normalizeText(companyId);
        String normalizedModuleCode = normalizeModuleCode(moduleCode);
        if (normalizedCompanyId == null || normalizedModuleCode == null) {
            return false;
        }
        FinanceAccountSetModuleEnable record = financeAccountSetModuleEnableMapper.selectOne(
                Wrappers.<FinanceAccountSetModuleEnable>lambdaQuery()
                        .eq(FinanceAccountSetModuleEnable::getCompanyId, normalizedCompanyId)
                        .eq(FinanceAccountSetModuleEnable::getModuleCode, normalizedModuleCode)
                        .last("limit 1")
        );
        return record != null && Objects.equals(record.getEnabled(), 1);
    }

    public void requireEnabled(String companyId, String moduleCode) {
        if (!isEnabled(companyId, moduleCode)) {
            throw new IllegalStateException(DISABLED_MESSAGE);
        }
    }

    public Map<String, List<String>> loadEnabledModuleMap(Collection<String> companyIds) {
        List<String> normalizedCompanyIds = companyIds == null
                ? List.of()
                : companyIds.stream()
                .map(this::normalizeText)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedCompanyIds.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> enabledMap = new LinkedHashMap<>();
        for (FinanceAccountSetModuleEnable item : financeAccountSetModuleEnableMapper.selectList(
                Wrappers.<FinanceAccountSetModuleEnable>lambdaQuery()
                        .in(FinanceAccountSetModuleEnable::getCompanyId, normalizedCompanyIds)
                        .eq(FinanceAccountSetModuleEnable::getEnabled, 1)
                        .orderByAsc(FinanceAccountSetModuleEnable::getCompanyId, FinanceAccountSetModuleEnable::getId)
        )) {
            enabledMap.computeIfAbsent(item.getCompanyId(), key -> new ArrayList<>())
                    .add(item.getModuleCode());
        }
        return enabledMap;
    }

    public List<String> loadEnabledModules(String companyId) {
        return loadEnabledModuleMap(List.of(companyId)).getOrDefault(normalizeText(companyId), List.of());
    }

    public List<FinanceAccountSetModuleEnable> buildDefaultModules(String companyId) {
        String normalizedCompanyId = normalizeText(companyId);
        if (normalizedCompanyId == null) {
            return List.of();
        }
        return moduleDefinitions().stream()
                .map(definition -> {
                    FinanceAccountSetModuleEnable record = new FinanceAccountSetModuleEnable();
                    record.setCompanyId(normalizedCompanyId);
                    record.setModuleCode(definition.code());
                    record.setEnabled(definition.defaultEnabled() ? 1 : 0);
                    return record;
                })
                .collect(Collectors.toList());
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeModuleCode(String value) {
        String trimmed = normalizeText(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    public record ModuleDefinition(String code, String name, boolean implemented, boolean toggleable) {
        public boolean defaultEnabled() {
            return Objects.equals(code, GENERAL_LEDGER);
        }
    }
}
