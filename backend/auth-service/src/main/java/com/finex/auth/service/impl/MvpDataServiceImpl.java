package com.finex.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ApprovalSummaryVO;
import com.finex.auth.dto.DashboardVO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.dto.InvoiceAlertVO;
import com.finex.auth.dto.InvoiceSummaryVO;
import com.finex.auth.dto.UserProfileVO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.service.MvpDataService;
import com.finex.auth.service.UserService;
import com.finex.auth.support.AsyncTaskSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    @Override
    public UserProfileVO getCurrentUser(Long userId) {
        User user = requireUser(userId);
        UserProfileVO profile = new UserProfileVO();
        profile.setUserId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setName(getDisplayName(user));
        profile.setPhone(user.getPhone());
        profile.setEmail(user.getEmail());
        profile.setPosition(StrUtil.blankToDefault(user.getPosition(), "员工"));
        profile.setLaborRelationBelong(StrUtil.blankToDefault(user.getLaborRelationBelong(), "总部"));
        profile.setCompanyId(user.getCompanyId());
        profile.setRoles(userService.getRoleCodes(userId));
        profile.setPermissionCodes(userService.getPermissionCodes(userId));
        return profile;
    }

    @Override
    public DashboardVO getDashboard(Long userId) {
        User user = requireUser(userId);
        List<ExpenseSummaryVO> expenses = listExpenses(userId);
        List<InvoiceSummaryVO> invoices = listInvoices(userId);

        int userFactor = Math.max(1, userId.intValue());

        DashboardVO dashboard = new DashboardVO();
        dashboard.setUser(getCurrentUser(userId));
        dashboard.setPendingApprovalCount(2 + userFactor);
        dashboard.setPendingApprovalDelta(userFactor % 3 + 1);
        dashboard.setMonthlyExpenseAmount(6800D + userFactor * 860D);
        dashboard.setMonthlyExpenseCount(expenses.size());
        dashboard.setInvoiceCount(invoices.size() * 12);
        dashboard.setMonthlyInvoiceCount(invoices.size());
        dashboard.setBudgetRemaining(42000D - userFactor * 1200D);
        dashboard.setBudgetUsageRate(28 + userFactor * 4);
        dashboard.setRecentExpenses(expenses.subList(0, Math.min(4, expenses.size())));
        dashboard.setPendingApprovals(buildPendingApprovals(user));
        dashboard.setInvoiceAlerts(buildInvoiceAlerts(user));
        return dashboard;
    }

    @Override
    public List<ExpenseSummaryVO> listExpenses(Long userId) {
        User user = requireUser(userId);
        int userFactor = Math.max(1, userId.intValue());
        LocalDate today = LocalDate.now();

        return List.of(
                expense("BX" + today.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "001", "差旅费",
                        getDisplayName(user) + "华东出差费用", 1800D + userFactor * 350D, today.minusDays(1), "审批中"),
                expense("BX" + today.minusDays(2).format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "002", "办公费",
                        getDisplayName(user) + "办公用品采购", 320D + userFactor * 68D, today.minusDays(2), "已通过"),
                expense("BX" + today.minusDays(4).format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "003", "招待费",
                        getDisplayName(user) + "客户接待费用", 960D + userFactor * 120D, today.minusDays(4), "已通过"),
                expense("BX" + today.minusDays(6).format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "004", "交通费",
                        getDisplayName(user) + "市内交通", 48D + userFactor * 12D, today.minusDays(6), "已驳回"),
                expense("BX" + today.minusDays(9).format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "005", "差旅费",
                        getDisplayName(user) + "华北出差费用", 3200D + userFactor * 420D, today.minusDays(9), "已通过")
        );
    }

    @Override
    public List<InvoiceSummaryVO> listInvoices(Long userId) {
        User user = requireUser(userId);
        int userFactor = Math.max(1, userId.intValue());
        LocalDate today = LocalDate.now();

        List<InvoiceSummaryVO> invoices = List.of(
                invoice("011001900211", "12345678", "增值税普通发票", getDisplayName(user) + "科技有限公司",
                        1600D + userFactor * 240D, today.minusDays(3), "已验真", "已识别"),
                invoice("031001900211", "87654321", "增值税专用发票", getDisplayName(user) + "贸易有限公司",
                        3200D + userFactor * 360D, today.minusDays(5), "已验真", "已识别"),
                invoice("011001900212", "11112222", "电子发票", getDisplayName(user) + "服务公司",
                        680D + userFactor * 100D, today.minusDays(7), "待验真", "待识别"),
                invoice("031001900213", "33334444", "增值税普通发票", getDisplayName(user) + "电子公司",
                        420D + userFactor * 80D, today.minusDays(12), "验真失败", "识别失败")
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

    private List<ApprovalSummaryVO> buildPendingApprovals(User user) {
        String displayName = getDisplayName(user);
        return List.of(
                approval(1L, "上海出差费用报销", displayName, "10分钟前", 3245D, 1),
                approval(2L, "客户招待费用", displayName, "30分钟前", 1580D, 2),
                approval(3L, "季度办公用品采购", displayName, "1小时前", 8920D, 3)
        );
    }

    private List<InvoiceAlertVO> buildInvoiceAlerts(User user) {
        return List.of(
                alert(1L, "发票重复报销", "用户 " + getDisplayName(user) + " 名下存在疑似重复报销发票，请尽快核对。", "30分钟前"),
                alert(2L, "发票验真失败", "本月 1 张发票调用税务验真失败，建议稍后重试。", "1小时前"),
                alert(3L, "发票即将过期", "本月 3 张发票将在 7 天内超出报销时限。", "2小时前")
        );
    }

    private ExpenseSummaryVO expense(String no, String type, String reason, Double amount, LocalDate date, String status) {
        ExpenseSummaryVO summary = new ExpenseSummaryVO();
        summary.setNo(no);
        summary.setType(type);
        summary.setReason(reason);
        summary.setAmount(amount);
        summary.setDate(date.format(DATE_FORMATTER));
        summary.setStatus(status);
        return summary;
    }

    private InvoiceSummaryVO invoice(String code, String number, String type, String seller,
                                     Double amount, LocalDate date, String status, String ocrStatus) {
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
            invoice.setStatus("验真中");
            return;
        }
        if (AsyncTaskSupport.TASK_STATUS_SUCCESS.equalsIgnoreCase(task.getStatus())) {
            invoice.setStatus("已验真");
            return;
        }
        invoice.setStatus("验真失败");
    }

    private void applyOcrStatus(InvoiceSummaryVO invoice, AsyncTaskRecord task) {
        if (task == null) {
            return;
        }
        if (AsyncTaskSupport.isActive(task.getStatus())) {
            invoice.setOcrStatus("识别中");
            return;
        }
        if (AsyncTaskSupport.TASK_STATUS_SUCCESS.equalsIgnoreCase(task.getStatus())) {
            invoice.setOcrStatus("已识别");
            return;
        }
        invoice.setOcrStatus("识别失败");
    }

    private ApprovalSummaryVO approval(Long id, String title, String submitter, String time, Double amount, int avatarSeed) {
        ApprovalSummaryVO summary = new ApprovalSummaryVO();
        summary.setId(id);
        summary.setTitle(title);
        summary.setSubmitter(submitter);
        summary.setTime(time);
        summary.setAmount(amount);
        summary.setAvatar("https://api.dicebear.com/7.x/avataaars/svg?seed=" + avatarSeed);
        return summary;
    }

    private InvoiceAlertVO alert(Long id, String title, String desc, String time) {
        InvoiceAlertVO alert = new InvoiceAlertVO();
        alert.setId(id);
        alert.setTitle(title);
        alert.setDesc(desc);
        alert.setTime(time);
        return alert;
    }

    private User requireUser(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("当前用户不存在");
        }
        return user;
    }

    private String getDisplayName(User user) {
        return StrUtil.blankToDefault(user.getName(), user.getUsername());
    }
}
