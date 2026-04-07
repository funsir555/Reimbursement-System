package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.FinanceVendorDetailVO;
import com.finex.auth.dto.FinanceVendorSaveDTO;
import com.finex.auth.dto.FinanceVendorSummaryVO;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.FinanceVendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceVendorServiceImpl implements FinanceVendorService {

    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final FinanceVendorMapper financeVendorMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<FinanceVendorSummaryVO> listVendors(String companyId, String keyword, Boolean includeDisabled) {
        QueryWrapper<FinanceVendor> query = new QueryWrapper<>();
        query.eq("company_id", requireCompanyId(companyId));
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword != null) {
            query.and(wrapper -> wrapper
                    .like("cVenCode", normalizedKeyword)
                    .or()
                    .like("cVenName", normalizedKeyword)
                    .or()
                    .like("cVenAbbName", normalizedKeyword));
        }
        if (!Boolean.TRUE.equals(includeDisabled)) {
            query.isNull("dEndDate");
        }
        query.orderByAsc("dEndDate").orderByAsc("cVenName").orderByAsc("cVenCode");
        return financeVendorMapper.selectList(query).stream().map(this::toSummary).toList();
    }

    @Override
    public FinanceVendorDetailVO getVendorDetail(String companyId, String vendorCode) {
        return toDetail(requireVendor(companyId, vendorCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVendorDetailVO createVendor(String companyId, FinanceVendorSaveDTO dto, String operatorName) {
        validateSave(dto, null);
        String normalizedCompanyId = requireCompanyId(companyId);

        FinanceVendor vendor = objectMapper.convertValue(dto, FinanceVendor.class);
        vendor.setCVenCode(trimToNull(dto.getCVenCode()) == null ? buildVendorCode() : dto.getCVenCode().trim());
        vendor.setCompanyId(normalizedCompanyId);
        vendor.setCCreatePerson(defaultOperator(operatorName));
        vendor.setCModifyPerson(defaultOperator(operatorName));
        vendor.setDModifyDate(LocalDateTime.now());
        vendor.setUpdatedAt(LocalDateTime.now());
        vendor.setCreatedAt(LocalDateTime.now());

        if (financeVendorMapper.selectById(vendor.getCVenCode()) != null) {
            throw new IllegalStateException("渚涘簲鍟嗙紪鐮佸凡瀛樺湪");
        }

        financeVendorMapper.insert(vendor);
        return toDetail(requireVendor(normalizedCompanyId, vendor.getCVenCode()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVendorDetailVO createVendor(Long currentUserId, FinanceVendorSaveDTO dto, String operatorName) {
        return createVendor(requireCurrentUserCompanyId(currentUserId), dto, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVendorDetailVO updateVendor(String companyId, String vendorCode, FinanceVendorSaveDTO dto, String operatorName) {
        String normalizedCompanyId = requireCompanyId(companyId);
        FinanceVendor existing = requireVendor(normalizedCompanyId, vendorCode);
        validateSave(dto, existing);

        FinanceVendor next = objectMapper.convertValue(dto, FinanceVendor.class);
        BeanUtils.copyProperties(next, existing, "cVenCode", "createdAt", "updatedAt", "cCreatePerson");
        existing.setCVenCode(vendorCode);
        existing.setCompanyId(normalizedCompanyId);
        existing.setCModifyPerson(defaultOperator(operatorName));
        existing.setDModifyDate(LocalDateTime.now());
        existing.setUpdatedAt(LocalDateTime.now());
        financeVendorMapper.updateById(existing);
        return toDetail(requireVendor(normalizedCompanyId, vendorCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disableVendor(String companyId, String vendorCode, String operatorName) {
        FinanceVendor vendor = requireVendor(companyId, vendorCode);
        vendor.setDEndDate(LocalDateTime.now());
        vendor.setCModifyPerson(defaultOperator(operatorName));
        vendor.setDModifyDate(LocalDateTime.now());
        vendor.setUpdatedAt(LocalDateTime.now());
        financeVendorMapper.updateById(vendor);
        return Boolean.TRUE;
    }

    @Override
    public List<ExpenseCreateVendorOptionVO> listActiveVendorOptions(String companyId, String keyword) {
        String normalizedCompanyId = requireCompanyId(companyId);
        QueryWrapper<FinanceVendor> query = new QueryWrapper<>();
        String normalizedKeyword = trimToNull(keyword);
        query.eq("company_id", normalizedCompanyId);
        if (normalizedKeyword != null) {
            query.and(wrapper -> wrapper
                    .like("cVenCode", normalizedKeyword)
                    .or()
                    .like("cVenName", normalizedKeyword)
                    .or()
                    .like("cVenAbbName", normalizedKeyword));
        }
        query.isNull("dEndDate").orderByAsc("cVenName").orderByAsc("cVenCode");
        return financeVendorMapper.selectList(query).stream()
                .filter(item -> normalizedCompanyId.equals(trimToNull(item.getCompanyId())))
                .map(this::toSummary).map(item -> {
            ExpenseCreateVendorOptionVO option = new ExpenseCreateVendorOptionVO();
            option.setValue(item.getCVenCode());
            option.setLabel(item.getCVenName());
            option.setSecondaryLabel(buildVendorSecondaryLabel(item.getCVenCode(), item.getCVenAbbName()));
            option.setCVenCode(item.getCVenCode());
            option.setCVenName(item.getCVenName());
            option.setCVenAbbName(item.getCVenAbbName());
            return option;
        }).toList();
    }

    private void validateSave(FinanceVendorSaveDTO dto, FinanceVendor existing) {
        if (trimToNull(dto.getCVenName()) == null) {
            throw new IllegalArgumentException("渚涘簲鍟嗗悕绉颁笉鑳戒负绌?");
        }
        if (existing == null && trimToNull(dto.getCVenCode()) != null) {
            QueryWrapper<FinanceVendor> query = new QueryWrapper<>();
            query.eq("cVenCode", dto.getCVenCode().trim()).last("limit 1");
            if (financeVendorMapper.selectOne(query) != null) {
                throw new IllegalStateException("渚涘簲鍟嗙紪鐮佸凡瀛樺湪");
            }
        }
    }

    private FinanceVendor requireVendor(String companyId, String vendorCode) {
        String normalizedCompanyId = requireCompanyId(companyId);
        String normalizedCode = trimToNull(vendorCode);
        if (normalizedCode == null) {
            throw new IllegalArgumentException("渚涘簲鍟嗙紪鐮佷笉鑳戒负绌?");
        }
        FinanceVendor vendor = financeVendorMapper.selectById(normalizedCode);
        if (vendor == null) {
            throw new IllegalStateException("渚涘簲鍟嗕笉瀛樺湪");
        }
        if (!normalizedCompanyId.equals(trimToNull(vendor.getCompanyId()))) {
            throw new SecurityException("鏃犳潈璁块棶褰撳墠渚涘簲鍟?");
        }
        return vendor;
    }

    private FinanceVendorSummaryVO toSummary(FinanceVendor vendor) {
        FinanceVendorSummaryVO summary = new FinanceVendorSummaryVO();
        summary.setCVenCode(vendor.getCVenCode());
        summary.setCVenName(vendor.getCVenName());
        summary.setCVenAbbName(vendor.getCVenAbbName());
        summary.setCVCCode(vendor.getCVCCode());
        summary.setCVenPerson(vendor.getCVenPerson());
        summary.setCVenPhone(vendor.getCVenPhone());
        summary.setCVenBank(vendor.getCVenBank());
        summary.setCVenAccount(vendor.getCVenAccount());
        summary.setCompanyId(vendor.getCompanyId());
        summary.setDEndDate(vendor.getDEndDate());
        summary.setUpdatedAt(vendor.getUpdatedAt());
        summary.setActive(vendor.getDEndDate() == null);
        return summary;
    }

    private FinanceVendorDetailVO toDetail(FinanceVendor vendor) {
        FinanceVendorDetailVO detail = objectMapper.convertValue(vendor, FinanceVendorDetailVO.class);
        detail.setActive(vendor.getDEndDate() == null);
        return detail;
    }

    private String buildVendorCode() {
        String prefix = "VEN" + LocalDate.now().format(CODE_DATE_FORMATTER);
        QueryWrapper<FinanceVendor> query = new QueryWrapper<>();
        query.likeRight("cVenCode", prefix);
        Long count = financeVendorMapper.selectCount(query);
        long next = count == null ? 1L : count + 1L;
        return prefix + String.format("%04d", next);
    }

    private String buildVendorSecondaryLabel(String vendorCode, String vendorAbbName) {
        String code = trimToNull(vendorCode);
        String abbName = trimToNull(vendorAbbName);
        if (code != null && abbName != null) {
            return code + " / " + abbName;
        }
        return code != null ? code : (abbName == null ? "" : abbName);
    }

    private String defaultOperator(String operatorName) {
        String normalized = trimToNull(operatorName);
        return normalized == null ? "system" : normalized;
    }

    private String requireCompanyId(String companyId) {
        String normalized = trimToNull(companyId);
        if (normalized == null) {
            throw new IllegalArgumentException("鍏徃涓讳綋涓嶈兘涓虹┖");
        }
        return normalized;
    }

    private String requireCurrentUserCompanyId(Long currentUserId) {
        if (currentUserId == null) {
            throw new IllegalArgumentException("瑜版挸澧犻悽銊﹀煕娑撳秷鍏樻稉铏光敄");
        }
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            throw new IllegalStateException("瑜版挸澧犻悽銊﹀煕娑撳秴鐡ㄩ崷?");
        }
        String companyId = trimToNull(currentUser.getCompanyId());
        if (companyId == null) {
            throw new IllegalStateException("瑜版挸澧犻悽銊﹀煕閺堫亞绮︾€规艾鍙曢崣闀愬瘜娴?");
        }
        return companyId;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
