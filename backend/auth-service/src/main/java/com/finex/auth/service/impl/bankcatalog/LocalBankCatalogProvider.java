package com.finex.auth.service.impl.bankcatalog;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.finex.auth.dto.FinanceBankBranchVO;
import com.finex.auth.dto.FinanceBankOptionVO;
import com.finex.auth.entity.SystemBankBranchCatalog;
import com.finex.auth.entity.SystemBankCatalog;
import com.finex.auth.mapper.SystemBankBranchCatalogMapper;
import com.finex.auth.mapper.SystemBankCatalogMapper;
import com.finex.auth.service.bankcatalog.BankCatalogProvider;
import com.finex.auth.service.bankcatalog.BankCatalogProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class LocalBankCatalogProvider implements BankCatalogProvider {

    private final SystemBankCatalogMapper systemBankCatalogMapper;
    private final SystemBankBranchCatalogMapper systemBankBranchCatalogMapper;

    @Override
    public BankCatalogProviderType getType() {
        return BankCatalogProviderType.LOCAL;
    }

    @Override
    public List<FinanceBankOptionVO> listBanks(String keyword, String businessScope) {
        QueryWrapper<SystemBankCatalog> query = new QueryWrapper<>();
        String normalizedKeyword = trimToNull(keyword);
        String normalizedScope = normalizeBusinessScope(businessScope);
        query.eq("status", 1);
        applyBusinessScopeFilter(query, normalizedScope);
        if (normalizedKeyword != null) {
            query.and(wrapper -> wrapper
                    .like("bank_code", normalizedKeyword)
                    .or()
                    .like("bank_name", normalizedKeyword));
        }
        query.orderByAsc("sort_order").orderByAsc("bank_code").last("limit 80");
        return systemBankCatalogMapper.selectList(query).stream().map(this::toBankOption).toList();
    }

    @Override
    public List<String> listProvinces(String bankCode, String businessScope) {
        String normalizedBankCode = trimToNull(bankCode);
        if (normalizedBankCode == null || !bankAllowedForScope(normalizedBankCode, businessScope)) {
            return List.of();
        }
        QueryWrapper<SystemBankBranchCatalog> query = new QueryWrapper<>();
        query.select("province");
        query.eq("status", 1).eq("bank_code", normalizedBankCode);
        query.orderByAsc("province");
        return uniqueNonBlank(systemBankBranchCatalogMapper.selectObjs(query));
    }

    @Override
    public List<String> listCities(String bankCode, String province, String businessScope) {
        String normalizedBankCode = trimToNull(bankCode);
        String normalizedProvince = trimToNull(province);
        if (normalizedBankCode == null || normalizedProvince == null || !bankAllowedForScope(normalizedBankCode, businessScope)) {
            return List.of();
        }
        QueryWrapper<SystemBankBranchCatalog> query = new QueryWrapper<>();
        query.select("city");
        query.eq("status", 1)
                .eq("bank_code", normalizedBankCode)
                .eq("province", normalizedProvince);
        query.orderByAsc("city");
        return uniqueNonBlank(systemBankBranchCatalogMapper.selectObjs(query));
    }

    @Override
    public List<FinanceBankBranchVO> listBankBranches(String bankCode, String province, String city, String keyword, String businessScope) {
        String normalizedBankCode = trimToNull(bankCode);
        String normalizedProvince = trimToNull(province);
        String normalizedCity = trimToNull(city);
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedBankCode != null && !bankAllowedForScope(normalizedBankCode, businessScope)) {
            return List.of();
        }
        QueryWrapper<SystemBankBranchCatalog> query = new QueryWrapper<>();
        query.eq("status", 1);
        if (normalizedBankCode != null) {
            query.eq("bank_code", normalizedBankCode);
        }
        if (normalizedProvince != null) {
            query.eq("province", normalizedProvince);
        }
        if (normalizedCity != null) {
            query.eq("city", normalizedCity);
        }
        if (normalizedKeyword != null) {
            query.and(wrapper -> wrapper
                    .like("branch_name", normalizedKeyword)
                    .or()
                    .like("branch_code", normalizedKeyword)
                    .or()
                    .like("cnaps_code", normalizedKeyword)
                    .or()
                    .like("city", normalizedKeyword)
                    .or()
                    .like("province", normalizedKeyword));
        }
        query.orderByAsc("sort_order")
                .orderByAsc("bank_code")
                .orderByAsc("province")
                .orderByAsc("city")
                .orderByAsc("branch_name");
        query.last("limit 500");
        return systemBankBranchCatalogMapper.selectList(query).stream().map(this::toBranchOption).toList();
    }

    @Override
    public FinanceBankBranchVO lookupBranchByCnaps(String cnapsCode) {
        String normalizedCnapsCode = trimToNull(cnapsCode);
        if (normalizedCnapsCode == null) {
            return null;
        }
        QueryWrapper<SystemBankBranchCatalog> query = new QueryWrapper<>();
        query.eq("status", 1).eq("cnaps_code", normalizedCnapsCode).last("limit 1");
        SystemBankBranchCatalog branch = systemBankBranchCatalogMapper.selectOne(query);
        return branch == null ? null : toBranchOption(branch);
    }

    private boolean bankAllowedForScope(String bankCode, String businessScope) {
        String normalizedScope = normalizeBusinessScope(businessScope);
        if (normalizedScope == null) {
            return true;
        }
        QueryWrapper<SystemBankCatalog> query = new QueryWrapper<>();
        query.eq("status", 1).eq("bank_code", bankCode);
        applyBusinessScopeFilter(query, normalizedScope);
        query.last("limit 1");
        return systemBankCatalogMapper.selectCount(query) > 0;
    }

    private void applyBusinessScopeFilter(QueryWrapper<SystemBankCatalog> query, String businessScope) {
        if (businessScope == null) {
            return;
        }
        query.and(wrapper -> wrapper.eq("business_scope", businessScope).or().eq("business_scope", "BOTH"));
    }

    private FinanceBankOptionVO toBankOption(SystemBankCatalog bank) {
        FinanceBankOptionVO option = new FinanceBankOptionVO();
        option.setBankCode(bank.getBankCode());
        option.setBankName(bank.getBankName());
        option.setBusinessScope(defaultScope(bank.getBusinessScope()));
        option.setValue(bank.getBankCode());
        option.setLabel(bank.getBankName());
        return option;
    }

    private FinanceBankBranchVO toBranchOption(SystemBankBranchCatalog branch) {
        FinanceBankBranchVO option = new FinanceBankBranchVO();
        option.setId(branch.getId());
        option.setBankCode(branch.getBankCode());
        option.setBankName(branch.getBankName());
        option.setProvince(branch.getProvince());
        option.setCity(branch.getCity());
        option.setBranchCode(branch.getBranchCode());
        option.setBranchName(branch.getBranchName());
        option.setCnapsCode(branch.getCnapsCode());
        option.setValue(branch.getBranchCode());
        option.setLabel(buildBranchLabel(branch));
        return option;
    }

    private String buildBranchLabel(SystemBankBranchCatalog branch) {
        String area = List.of(branch.getProvince(), branch.getCity()).stream()
                .map(this::trimToNull)
                .filter(item -> item != null)
                .distinct()
                .reduce((left, right) -> left + " / " + right)
                .orElse("");
        if (!area.isEmpty()) {
            return branch.getBranchName() + " (" + area + ")";
        }
        return branch.getBranchName();
    }

    private List<String> uniqueNonBlank(List<Object> values) {
        Set<String> unique = new LinkedHashSet<>();
        for (Object value : values) {
            String normalized = value == null ? null : trimToNull(String.valueOf(value));
            if (normalized != null) {
                unique.add(normalized);
            }
        }
        return List.copyOf(unique);
    }

    private String normalizeBusinessScope(String businessScope) {
        String normalized = trimToNull(businessScope);
        if (normalized == null) {
            return null;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "PRIVATE", "PUBLIC", "BOTH" -> upper;
            default -> null;
        };
    }

    private String defaultScope(String businessScope) {
        String normalized = normalizeBusinessScope(businessScope);
        return normalized == null ? "BOTH" : normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
