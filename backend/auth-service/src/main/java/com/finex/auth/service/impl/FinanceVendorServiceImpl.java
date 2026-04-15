// 业务域：财务档案
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.FinanceVendorDetailVO;
import com.finex.auth.dto.FinanceVendorSaveDTO;
import com.finex.auth.dto.FinanceVendorSummaryVO;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.FinanceVendorService;
import com.finex.auth.service.impl.financearchive.FinanceVendorMutationDomainSupport;
import com.finex.auth.service.impl.financearchive.FinanceVendorOptionDomainSupport;
import com.finex.auth.service.impl.financearchive.FinanceVendorQueryDomainSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * FinanceVendorServiceImpl：service 入口实现。
 * 接住上层请求，并把 财务供应商相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
@Service
public class FinanceVendorServiceImpl implements FinanceVendorService {

    private final FinanceVendorQueryDomainSupport financeVendorQueryDomainSupport;
    private final FinanceVendorMutationDomainSupport financeVendorMutationDomainSupport;
    private final FinanceVendorOptionDomainSupport financeVendorOptionDomainSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FinanceVendorServiceImpl(FinanceVendorMapper financeVendorMapper, UserMapper userMapper, ObjectMapper objectMapper) {
        this.financeVendorQueryDomainSupport = new FinanceVendorQueryDomainSupport(financeVendorMapper, userMapper, objectMapper);
        this.financeVendorMutationDomainSupport = new FinanceVendorMutationDomainSupport(financeVendorMapper, userMapper, objectMapper);
        this.financeVendorOptionDomainSupport = new FinanceVendorOptionDomainSupport(financeVendorMapper, userMapper, objectMapper);
    }

    /**
     * 查询供应商列表。
     */
    @Override
    public List<FinanceVendorSummaryVO> listVendors(String companyId, String keyword, Boolean includeDisabled) {
        return financeVendorQueryDomainSupport.listVendors(companyId, keyword, includeDisabled);
    }

    /**
     * 获取供应商明细。
     */
    @Override
    public FinanceVendorDetailVO getVendorDetail(String companyId, String vendorCode) {
        return financeVendorQueryDomainSupport.getVendorDetail(companyId, vendorCode);
    }

    /**
     * 创建供应商。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVendorDetailVO createVendor(String companyId, FinanceVendorSaveDTO dto, String operatorName, boolean paymentInfoRequired) {
        return financeVendorMutationDomainSupport.createVendor(companyId, dto, operatorName, paymentInfoRequired);
    }

    /**
     * 创建供应商。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVendorDetailVO createVendor(Long currentUserId, FinanceVendorSaveDTO dto, String operatorName, boolean paymentInfoRequired) {
        return financeVendorMutationDomainSupport.createVendor(currentUserId, dto, operatorName, paymentInfoRequired);
    }

    /**
     * 更新供应商。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVendorDetailVO updateVendor(String companyId, String vendorCode, FinanceVendorSaveDTO dto, String operatorName, boolean paymentInfoRequired) {
        return financeVendorMutationDomainSupport.updateVendor(companyId, vendorCode, dto, operatorName, paymentInfoRequired);
    }

    /**
     * 处理财务供应商中的这一步。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean disableVendor(String companyId, String vendorCode, String operatorName) {
        return financeVendorMutationDomainSupport.disableVendor(companyId, vendorCode, operatorName);
    }

    /**
     * 查询Active供应商选项。
     */
    @Override
    public List<ExpenseCreateVendorOptionVO> listActiveVendorOptions(String companyId, String keyword, Boolean includeDisabled) {
        return financeVendorOptionDomainSupport.listActiveVendorOptions(companyId, keyword, includeDisabled);
    }
}
