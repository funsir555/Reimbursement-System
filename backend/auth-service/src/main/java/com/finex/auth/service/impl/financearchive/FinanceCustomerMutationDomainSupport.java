// 业务域：财务档案
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

package com.finex.auth.service.impl.financearchive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceCustomerDetailVO;
import com.finex.auth.dto.FinanceCustomerSaveDTO;
import com.finex.auth.entity.FinanceCustomer;
import com.finex.auth.mapper.FinanceCustomerMapper;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

/**
 * FinanceCustomerMutationDomainSupport：领域规则支撑类。
 * 承接 财务客户的核心业务规则。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
public class FinanceCustomerMutationDomainSupport extends AbstractFinanceCustomerArchiveSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FinanceCustomerMutationDomainSupport(
            FinanceCustomerMapper financeCustomerMapper,
            ObjectMapper objectMapper
    ) {
        super(financeCustomerMapper, objectMapper);
    }

    /**
     * 创建客户。
     */
    public FinanceCustomerDetailVO createCustomer(String companyId, FinanceCustomerSaveDTO dto, String operatorName) {
        validateSave(dto, null);
        String normalizedCompanyId = requireCompanyId(companyId);

        FinanceCustomer customer = objectMapper.convertValue(dto, FinanceCustomer.class);
        customer.setCCusCode(trimToNull(dto.getCCusCode()) == null ? buildCustomerCode() : dto.getCCusCode().trim());
        customer.setCompanyId(normalizedCompanyId);
        customer.setUpdatedAt(LocalDateTime.now());
        customer.setCreatedAt(LocalDateTime.now());

        if (financeCustomerMapper.selectById(customer.getCCusCode()) != null) {
            throw new IllegalStateException("????????????????");
        }

        financeCustomerMapper.insert(customer);
        return toDetail(requireCustomer(normalizedCompanyId, customer.getCCusCode()));
    }

    /**
     * 更新客户。
     */
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

    /**
     * 处理财务客户中的这一步。
     */
    public Boolean disableCustomer(String companyId, String customerCode, String operatorName) {
        FinanceCustomer customer = requireCustomer(companyId, customerCode);
        customer.setDEndDate(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        financeCustomerMapper.updateById(customer);
        return Boolean.TRUE;
    }
}
