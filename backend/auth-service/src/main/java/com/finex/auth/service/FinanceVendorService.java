// 业务域：财务档案
// 文件角色：service 接口
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

package com.finex.auth.service;

import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.FinanceVendorDetailVO;
import com.finex.auth.dto.FinanceVendorSaveDTO;
import com.finex.auth.dto.FinanceVendorSummaryVO;

import java.util.List;

/**
 * FinanceVendorService：service 接口。
 * 定义财务供应商这块对外提供的业务入口能力。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
public interface FinanceVendorService {

    /**
     * 查询供应商列表。
     */
    List<FinanceVendorSummaryVO> listVendors(String companyId, String keyword, Boolean includeDisabled);

    /**
     * 获取供应商明细。
     */
    FinanceVendorDetailVO getVendorDetail(String companyId, String vendorCode);

    /**
     * 创建供应商。
     */
    FinanceVendorDetailVO createVendor(String companyId, FinanceVendorSaveDTO dto, String operatorName, boolean paymentInfoRequired);

    /**
     * 创建供应商。
     */
    FinanceVendorDetailVO createVendor(Long currentUserId, FinanceVendorSaveDTO dto, String operatorName, boolean paymentInfoRequired);

    /**
     * 更新供应商。
     */
    FinanceVendorDetailVO updateVendor(String companyId, String vendorCode, FinanceVendorSaveDTO dto, String operatorName, boolean paymentInfoRequired);

    Boolean disableVendor(String companyId, String vendorCode, String operatorName);

    /**
     * 查询Active供应商选项。
     */
    List<ExpenseCreateVendorOptionVO> listActiveVendorOptions(String companyId, String keyword, Boolean includeDisabled);
}
