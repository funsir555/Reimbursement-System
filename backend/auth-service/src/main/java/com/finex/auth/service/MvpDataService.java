// 业务域：首页看板与当前用户
// 文件角色：service 接口
// 上下游关系：上游通常来自 MvpController 和首页页面请求，下游会继续协调 用户信息、待办汇总和发票等首页数据。
// 风险提醒：改坏后最容易影响 首页统计、个人信息与待办展示。

package com.finex.auth.service;

import com.finex.auth.dto.DashboardVO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.dto.InvoiceSummaryVO;
import com.finex.auth.dto.UserProfileVO;

import java.util.List;

/**
 * MvpDataService：service 接口。
 * 定义数据这块对外提供的业务入口能力。
 * 改这里时，要特别关注 首页统计、个人信息与待办展示是否会被一起带坏。
 */
public interface MvpDataService {

    /**
     * 获取当前用户。
     */
    UserProfileVO getCurrentUser(Long userId);

    /**
     * 获取首页看板。
     */
    DashboardVO getDashboard(Long userId);

    /**
     * 查询报销单列表。
     */
    List<ExpenseSummaryVO> listExpenses(Long userId);

    /**
     * 查询发票列表。
     */
    List<InvoiceSummaryVO> listInvoices(Long userId);
}
