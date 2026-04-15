// 业务域：财务档案
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

package com.finex.auth.service.impl.financearchive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceVendorDetailVO;
import com.finex.auth.dto.FinanceVendorSaveDTO;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.UserMapper;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

/**
 * FinanceVendorMutationDomainSupport：领域规则支撑类。
 * 承接 财务供应商的核心业务规则。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
public class FinanceVendorMutationDomainSupport extends AbstractFinanceVendorArchiveSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FinanceVendorMutationDomainSupport(
            FinanceVendorMapper financeVendorMapper,
            UserMapper userMapper,
            ObjectMapper objectMapper
    ) {
        super(financeVendorMapper, userMapper, objectMapper);
    }

    /**
     * 创建供应商。
     */
    public FinanceVendorDetailVO createVendor(String companyId, FinanceVendorSaveDTO dto, String operatorName, boolean paymentInfoRequired) {
        validateSave(dto, null, paymentInfoRequired);
        String normalizedCompanyId = requireCompanyId(companyId);

        FinanceVendor vendor = objectMapper.convertValue(dto, FinanceVendor.class);
        normalizePaymentInfoFields(vendor, dto);
        vendor.setCVenCode(trimToNull(dto.getCVenCode()) == null ? buildVendorCode() : dto.getCVenCode().trim());
        vendor.setCompanyId(normalizedCompanyId);
        vendor.setCCreatePerson(defaultOperator(operatorName));
        vendor.setCModifyPerson(defaultOperator(operatorName));
        vendor.setDModifyDate(LocalDateTime.now());
        vendor.setUpdatedAt(LocalDateTime.now());
        vendor.setCreatedAt(LocalDateTime.now());

        if (financeVendorMapper.selectById(vendor.getCVenCode()) != null) {
            throw new IllegalStateException("Supplier code already exists");
        }

        financeVendorMapper.insert(vendor);
        return toDetail(requireVendor(normalizedCompanyId, vendor.getCVenCode()));
    }

    /**
     * 创建供应商。
     */
    public FinanceVendorDetailVO createVendor(Long currentUserId, FinanceVendorSaveDTO dto, String operatorName, boolean paymentInfoRequired) {
        return createVendor(requireCurrentUserCompanyId(currentUserId), dto, operatorName, paymentInfoRequired);
    }

    /**
     * 更新供应商。
     */
    public FinanceVendorDetailVO updateVendor(String companyId, String vendorCode, FinanceVendorSaveDTO dto, String operatorName, boolean paymentInfoRequired) {
        String normalizedCompanyId = requireCompanyId(companyId);
        FinanceVendor existing = requireVendor(normalizedCompanyId, vendorCode);
        validateSave(dto, existing, paymentInfoRequired);

        FinanceVendor next = objectMapper.convertValue(dto, FinanceVendor.class);
        String createdBy = existing.getCCreatePerson();
        LocalDateTime createdAt = existing.getCreatedAt();
        normalizePaymentInfoFields(next, dto);
        BeanUtils.copyProperties(next, existing, "cVenCode", "createdAt", "updatedAt", "cCreatePerson");
        existing.setCVenCode(vendorCode);
        existing.setCompanyId(normalizedCompanyId);
        existing.setCCreatePerson(createdBy);
        existing.setCreatedAt(createdAt);
        existing.setCModifyPerson(defaultOperator(operatorName));
        existing.setDModifyDate(LocalDateTime.now());
        existing.setUpdatedAt(LocalDateTime.now());
        financeVendorMapper.updateById(existing);
        return toDetail(requireVendor(normalizedCompanyId, vendorCode));
    }

    /**
     * 处理财务供应商中的这一步。
     */
    public Boolean disableVendor(String companyId, String vendorCode, String operatorName) {
        FinanceVendor vendor = requireVendor(companyId, vendorCode);
        vendor.setDEndDate(LocalDateTime.now());
        vendor.setCModifyPerson(defaultOperator(operatorName));
        vendor.setDModifyDate(LocalDateTime.now());
        vendor.setUpdatedAt(LocalDateTime.now());
        financeVendorMapper.updateById(vendor);
        return Boolean.TRUE;
    }
}
