// 业务域：财务档案
// 文件角色：service 接口
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

package com.finex.auth.service;

import com.finex.auth.dto.FinanceBankBranchVO;
import com.finex.auth.dto.FinanceBankOptionVO;

import java.util.List;

/**
 * FinanceBankCatalogService：service 接口。
 * 定义财务银行目录这块对外提供的业务入口能力。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
public interface FinanceBankCatalogService {

    /**
     * 查询银行列表。
     */
    List<FinanceBankOptionVO> listBanks(String keyword, String businessScope);

    List<String> listProvinces(String bankCode, String businessScope);

    List<String> listCities(String bankCode, String province, String businessScope);

    /**
     * 查询银行Branches列表。
     */
    List<FinanceBankBranchVO> listBankBranches(String bankCode, String province, String city, String keyword, String businessScope);

    FinanceBankBranchVO lookupBranchByCnaps(String cnapsCode);
}
