package com.finex.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.DashboardVO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.dto.InvoiceSummaryVO;
import com.finex.auth.dto.UserProfileVO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.MvpDataService;
import com.finex.auth.service.UserService;
import com.finex.auth.support.AsyncTaskSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MvpDataServiceImpl implements MvpDataService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final UserService userService;
    private final AsyncTaskRecordMapper asyncTaskRecordMapper;
    private final ExpenseDocumentService expenseDocumentService;

    @Override
    public UserProfileVO getCurrentUser(Long userId) {
        User user = requireUser(userId);
        UserProfileVO profile = new UserProfileVO();
        profile.setUserId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setName(getDisplayName(user));
        profile.setPhone(user.getPhone());
        profile.setEmail(user.getEmail());
        profile.setPosition(StrUtil.blankToDefault(user.getPosition(), "Employee"));
        profile.setLaborRelationBelong(StrUtil.blankToDefault(user.getLaborRelationBelong(), "Headquarters"));
        profile.setCompanyId(user.getCompanyId());
        profile.setRoles(userService.getRoleCodes(userId));
        profile.setPermissionCodes(userService.getPermissionCodes(userId));
        return profile;
    }

    @Override
    public DashboardVO getDashboard(Long userId) {
        List<ExpenseSummaryVO> recentExpenses = expenseDocumentService.listExpenseSummaries(userId).stream()
                .filter(item -> "report".equalsIgnoreCase(StrUtil.blankToDefault(item.getTemplateType(), "")))
                .limit(6)
                .toList();

        DashboardVO dashboard = new DashboardVO();
        dashboard.setUser(getCurrentUser(userId));
        dashboard.setPendingApprovalCount(expenseDocumentService.listPendingApprovals(userId).size());
        dashboard.setPendingApprovalDelta(0);
        dashboard.setPendingRepaymentCount(expenseDocumentService.listOutstandingDocuments(userId, "LOAN").size());
        dashboard.setPendingPrepayWriteOffCount(expenseDocumentService.listOutstandingDocuments(userId, "PREPAY_REPORT").size());
        dashboard.setUnusedApplicationCount(0);
        dashboard.setUnpaidContractCount(0);
        dashboard.setMonthlyExpenseAmount(BigDecimal.ZERO);
        dashboard.setMonthlyExpenseCount(0);
        dashboard.setInvoiceCount(0);
        dashboard.setMonthlyInvoiceCount(0);
        dashboard.setBudgetRemaining(BigDecimal.ZERO);
        dashboard.setBudgetUsageRate(0);
        dashboard.setRecentExpenses(recentExpenses);
        dashboard.setPendingApprovals(List.of());
        dashboard.setInvoiceAlerts(List.of());
        return dashboard;
    }

    @Override
    public List<ExpenseSummaryVO> listExpenses(Long userId) {
        return expenseDocumentService.listExpenseSummaries(userId);
    }

    @Override
    public List<InvoiceSummaryVO> listInvoices(Long userId) {
        User user = requireUser(userId);
        int userFactor = Math.max(1, userId.intValue());
        LocalDate today = LocalDate.now();

        List<InvoiceSummaryVO> invoices = List.of(
                invoice("011001900211", "12345678", "VAT invoice", getDisplayName(user) + " Technology Co.",
                        BigDecimal.valueOf(1600L).add(BigDecimal.valueOf(userFactor).multiply(BigDecimal.valueOf(240L))), today.minusDays(3), "Verified", "Recognized"),
                invoice("031001900211", "87654321", "VAT special invoice", getDisplayName(user) + " Trading Co.",
                        BigDecimal.valueOf(3200L).add(BigDecimal.valueOf(userFactor).multiply(BigDecimal.valueOf(360L))), today.minusDays(5), "Verified", "Recognized"),
                invoice("011001900212", "11112222", "Electronic invoice", getDisplayName(user) + " Service Co.",
                        BigDecimal.valueOf(680L).add(BigDecimal.valueOf(userFactor).multiply(BigDecimal.valueOf(100L))), today.minusDays(7), "Pending verify", "Pending OCR"),
                invoice("031001900213", "33334444", "VAT invoice", getDisplayName(user) + " Digital Co.",
                        BigDecimal.valueOf(420L).add(BigDecimal.valueOf(userFactor).multiply(BigDecimal.valueOf(80L))), today.minusDays(12), "Verify failed", "OCR failed")
        );

        Map<String, AsyncTaskRecord> verifyTasks = latestTaskMap(userId, AsyncTaskSupport.TASK_TYPE_INVOICE_VERIFY);
        Map<String, AsyncTaskRecord> ocrTasks = latestTaskMap(userId, AsyncTaskSupport.TASK_TYPE_INVOICE_OCR);

        invoices.forEach(invoice -> {
            String businessKey = AsyncTaskSupport.buildInvoiceBusinessKey(invoice.getCode(), invoice.getNumber());
            applyVerifyStatus(invoice, verifyTasks.get(businessKey));
            applyOcrStatus(invoice, ocrTasks.get(businessKey));
        });
        return invoices;
    }

    private InvoiceSummaryVO invoice(String code, String number, String type, String seller,
                                     BigDecimal amount, LocalDate date, String status, String ocrStatus) {
        InvoiceSummaryVO summary = new InvoiceSummaryVO();
        summary.setCode(code);
        summary.setNumber(number);
        summary.setType(type);
        summary.setSeller(seller);
        summary.setAmount(amount);
        summary.setDate(date.format(DATE_FORMATTER));
        summary.setStatus(status);
        summary.setOcrStatus(ocrStatus);
        return summary;
    }

    private Map<String, AsyncTaskRecord> latestTaskMap(Long userId, String taskType) {
        List<AsyncTaskRecord> records = asyncTaskRecordMapper.selectList(
                Wrappers.<AsyncTaskRecord>lambdaQuery()
                        .eq(AsyncTaskRecord::getUserId, userId)
                        .eq(AsyncTaskRecord::getTaskType, taskType)
                        .orderByDesc(AsyncTaskRecord::getCreatedAt, AsyncTaskRecord::getId)
        );

        Map<String, AsyncTaskRecord> latestMap = new LinkedHashMap<>();
        for (AsyncTaskRecord record : records) {
            if (record.getBusinessKey() != null && !latestMap.containsKey(record.getBusinessKey())) {
                latestMap.put(record.getBusinessKey(), record);
            }
        }
        return latestMap;
    }

    private void applyVerifyStatus(InvoiceSummaryVO invoice, AsyncTaskRecord task) {
        if (task == null) {
            return;
        }
        if (AsyncTaskSupport.isActive(task.getStatus())) {
            invoice.setStatus("Verifying");
            return;
        }
        if (AsyncTaskSupport.TASK_STATUS_SUCCESS.equalsIgnoreCase(task.getStatus())) {
            invoice.setStatus("Verified");
            return;
        }
        invoice.setStatus("Verify failed");
    }

    private void applyOcrStatus(InvoiceSummaryVO invoice, AsyncTaskRecord task) {
        if (task == null) {
            return;
        }
        if (AsyncTaskSupport.isActive(task.getStatus())) {
            invoice.setOcrStatus("Recognizing");
            return;
        }
        if (AsyncTaskSupport.TASK_STATUS_SUCCESS.equalsIgnoreCase(task.getStatus())) {
            invoice.setOcrStatus("Recognized");
            return;
        }
        invoice.setOcrStatus("OCR failed");
    }

    private User requireUser(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Current user does not exist");
        }
        return user;
    }

    private String getDisplayName(User user) {
        return StrUtil.blankToDefault(user.getName(), user.getUsername());
    }
}
