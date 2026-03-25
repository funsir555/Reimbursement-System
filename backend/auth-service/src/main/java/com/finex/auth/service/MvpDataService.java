package com.finex.auth.service;

import com.finex.auth.dto.DashboardVO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.dto.InvoiceSummaryVO;
import com.finex.auth.dto.UserProfileVO;

import java.util.List;

/**
 * MVP 动态数据服务
 */
public interface MvpDataService {

    UserProfileVO getCurrentUser(Long userId);

    DashboardVO getDashboard(Long userId);

    List<ExpenseSummaryVO> listExpenses(Long userId);

    List<InvoiceSummaryVO> listInvoices(Long userId);
}
