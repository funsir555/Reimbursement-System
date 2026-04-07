package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceCustomerDetailVO;
import com.finex.auth.dto.FinanceCustomerSaveDTO;
import com.finex.auth.dto.FinanceCustomerSummaryVO;
import com.finex.auth.entity.FinanceCustomer;
import com.finex.auth.mapper.FinanceCustomerMapper;
import com.finex.auth.service.FinanceCustomerService;
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
public class FinanceCustomerServiceImpl implements FinanceCustomerService {

    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final FinanceCustomerMapper financeCustomerMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<FinanceCustomerSummaryVO> listCustomers(String companyId, String keyword, Boolean includeDisabled) {
        QueryWrapper<FinanceCustomer> query = new QueryWrapper<>();
        query.eq("company_id", requireCompanyId(companyId));
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword != null) {
            query.and(wrapper -> wrapper
                    .like("cCusCode", normalizedKeyword)
                    .or()
                    .like("cCusName", normalizedKeyword)
                    .or()
                    .like("cCusAbbName", normalizedKeyword));
        }
        if (!Boolean.TRUE.equals(includeDisabled)) {
            query.isNull("dEndDate");
        }
        query.orderByAsc("dEndDate").orderByAsc("cCusName").orderByAsc("cCusCode");
        return financeCustomerMapper.selectList(query).stream().map(this::toSummary).toList();
    }

    @Override
    public FinanceCustomerDetailVO getCustomerDetail(String companyId, String customerCode) {
        return toDetail(requireCustomer(companyId, customerCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceCustomerDetailVO createCustomer(String companyId, FinanceCustomerSaveDTO dto, String operatorName) {
        validateSave(dto, null);
        String normalizedCompanyId = requireCompanyId(companyId);

        FinanceCustomer customer = objectMapper.convertValue(dto, FinanceCustomer.class);
        customer.setCCusCode(trimToNull(dto.getCCusCode()) == null ? buildCustomerCode() : dto.getCCusCode().trim());
        customer.setCompanyId(normalizedCompanyId);
        customer.setUpdatedAt(LocalDateTime.now());
        customer.setCreatedAt(LocalDateTime.now());

        if (financeCustomerMapper.selectById(customer.getCCusCode()) != null) {
            throw new IllegalStateException("瀹㈡埛缂栫爜宸插瓨鍦?");
        }

        financeCustomerMapper.insert(customer);
        return toDetail(requireCustomer(normalizedCompanyId, customer.getCCusCode()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceCustomerDetailVO updateCustomer(String companyId, String customerCode, FinanceCustomerSaveDTO dto, String operatorName) {
        String normalizedCompanyId = requireCompanyId(companyId);
        FinanceCustomer existing = requireCustomer(normalizedCompanyId, customerCode);
        validateSave(dto, existing);

        FinanceCustomer next = objectMapper.convertValue(dto, FinanceCustomer.class);
        BeanUtils.copyProperties(next, existing, "cCusCode", "createdAt", "updatedAt");
        existing.setCCusCode(customerCode);
        existing.setCompanyId(normalizedCompanyId);
        existing.setUpdatedAt(LocalDateTime.now());
        financeCustomerMapper.updateById(existing);
        return toDetail(requireCustomer(normalizedCompanyId, customerCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disableCustomer(String companyId, String customerCode, String operatorName) {
        FinanceCustomer customer = requireCustomer(companyId, customerCode);
        customer.setDEndDate(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        financeCustomerMapper.updateById(customer);
        return Boolean.TRUE;
    }

    private void validateSave(FinanceCustomerSaveDTO dto, FinanceCustomer existing) {
        if (dto == null) {
            throw new IllegalArgumentException("瀹㈡埛淇℃伅涓嶈兘涓虹┖");
        }
        if (trimToNull(dto.getCCusName()) == null) {
            throw new IllegalArgumentException("瀹㈡埛鍚嶇О涓嶈兘涓虹┖");
        }
        if (existing == null && trimToNull(dto.getCCusCode()) != null) {
            QueryWrapper<FinanceCustomer> query = new QueryWrapper<>();
            query.eq("cCusCode", dto.getCCusCode().trim()).last("limit 1");
            if (financeCustomerMapper.selectOne(query) != null) {
                throw new IllegalStateException("瀹㈡埛缂栫爜宸插瓨鍦?");
            }
        }
    }

    private FinanceCustomer requireCustomer(String companyId, String customerCode) {
        String normalizedCompanyId = requireCompanyId(companyId);
        String normalizedCode = trimToNull(customerCode);
        if (normalizedCode == null) {
            throw new IllegalArgumentException("瀹㈡埛缂栫爜涓嶈兘涓虹┖");
        }
        FinanceCustomer customer = financeCustomerMapper.selectById(normalizedCode);
        if (customer == null) {
            throw new IllegalStateException("瀹㈡埛涓嶅瓨鍦?");
        }
        if (!normalizedCompanyId.equals(trimToNull(customer.getCompanyId()))) {
            throw new SecurityException("鏃犳潈璁块棶褰撳墠瀹㈡埛");
        }
        return customer;
    }

    private FinanceCustomerSummaryVO toSummary(FinanceCustomer customer) {
        FinanceCustomerSummaryVO summary = new FinanceCustomerSummaryVO();
        summary.setCCusCode(customer.getCCusCode());
        summary.setCCusName(customer.getCCusName());
        summary.setCCusAbbName(customer.getCCusAbbName());
        summary.setCCusPerson(customer.getCCusPerson());
        summary.setCCusHand(customer.getCCusHand());
        summary.setCCusBank(customer.getCCusBank());
        summary.setCCusAccount(customer.getCCusAccount());
        summary.setIARMoney(customer.getIARMoney());
        summary.setCompanyId(customer.getCompanyId());
        summary.setDEndDate(customer.getDEndDate());
        summary.setUpdatedAt(customer.getUpdatedAt());
        summary.setActive(customer.getDEndDate() == null);
        return summary;
    }

    private FinanceCustomerDetailVO toDetail(FinanceCustomer customer) {
        FinanceCustomerDetailVO detail = objectMapper.convertValue(customer, FinanceCustomerDetailVO.class);
        detail.setActive(customer.getDEndDate() == null);
        return detail;
    }

    private String buildCustomerCode() {
        String prefix = "CUS" + LocalDate.now().format(CODE_DATE_FORMATTER);
        QueryWrapper<FinanceCustomer> query = new QueryWrapper<>();
        query.likeRight("cCusCode", prefix);
        Long count = financeCustomerMapper.selectCount(query);
        long next = count == null ? 1L : count + 1L;
        return prefix + String.format("%04d", next);
    }

    private String requireCompanyId(String companyId) {
        String normalized = trimToNull(companyId);
        if (normalized == null) {
            throw new IllegalArgumentException("鍏徃涓讳綋涓嶈兘涓虹┖");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
