// 业务域：异步任务
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 异步提交、查询和通知相关接口，下游会继续协调 任务记录、状态更新和通知消息。
// 风险提醒：改坏后最容易影响 任务重复提交、异步状态不准确和结果回传。

package com.finex.auth.service.impl.asynctask;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.AsyncTaskSubmitResultVO;
import com.finex.auth.dto.ExpenseExportSubmitDTO;
import com.finex.auth.dto.InvoiceTaskSubmitDTO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.entity.DownloadRecord;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.mapper.NotificationRecordMapper;
import com.finex.auth.service.impl.AsyncTaskWorker;
import com.finex.auth.support.AsyncTaskSupport;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * AsyncTaskSubmissionDomainSupport：领域规则支撑类。
 * 承接 异步任务提交的核心业务规则。
 * 改这里时，要特别关注 任务重复提交、异步状态不准确和结果回传是否会被一起带坏。
 */
public final class AsyncTaskSubmissionDomainSupport extends AbstractAsyncTaskDomainSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public AsyncTaskSubmissionDomainSupport(
            AsyncTaskRecordMapper asyncTaskRecordMapper,
            DownloadRecordMapper downloadRecordMapper,
            NotificationRecordMapper notificationRecordMapper,
            AsyncTaskWorker asyncTaskWorker,
            ObjectMapper objectMapper
    ) {
        super(asyncTaskRecordMapper, downloadRecordMapper, notificationRecordMapper, asyncTaskWorker, objectMapper);
    }

    /**
     * 提交发票Export。
     */
    public AsyncTaskSubmitResultVO submitInvoiceExport(Long userId) {
        DownloadRecord downloadRecord = new DownloadRecord();
        downloadRecord.setUserId(userId);
        downloadRecord.setFileName("发票列表导出-" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".xlsx");
        downloadRecord.setBusinessType("发票导出");
        downloadRecord.setStatus(AsyncTaskSupport.DOWNLOAD_STATUS_DOWNLOADING);
        downloadRecord.setProgress(0);
        downloadRecord.setFileSize("生成中");
        downloadRecordMapper().insert(downloadRecord);

        AsyncTaskRecord task = createTask(
                userId,
                AsyncTaskSupport.TASK_TYPE_EXPORT,
                AsyncTaskSupport.BUSINESS_TYPE_INVOICE_EXPORT,
                "invoice-list",
                "发票列表导出",
                null,
                downloadRecord.getId()
        );
        asyncTaskWorker().runExportTask(task.getId());
        return toSubmitResult(task, "导出任务已提交，请到下载中心查看进度");
    }

    /**
     * 提交报销单Export。
     */
    public AsyncTaskSubmitResultVO submitExpenseExport(Long userId, ExpenseExportSubmitDTO dto) {
        ExpenseExportSubmitDTO payload = normalizeExpenseExportPayload(dto);
        DownloadRecord downloadRecord = new DownloadRecord();
        downloadRecord.setUserId(userId);
        downloadRecord.setFileName(resolveExpenseExportFileName(payload));
        downloadRecord.setBusinessType(resolveExpenseExportBusinessType(payload));
        downloadRecord.setStatus(AsyncTaskSupport.DOWNLOAD_STATUS_DOWNLOADING);
        downloadRecord.setProgress(0);
        downloadRecord.setFileSize("生成中");
        downloadRecordMapper().insert(downloadRecord);

        AsyncTaskRecord task = createTask(
                userId,
                AsyncTaskSupport.TASK_TYPE_EXPORT,
                AsyncTaskSupport.BUSINESS_TYPE_EXPENSE_EXPORT,
                resolveExpenseExportBusinessKey(payload),
                resolveExpenseExportDisplayName(payload),
                writeResultPayload(payload),
                downloadRecord.getId()
        );
        asyncTaskWorker().runExpenseExportTask(task.getId());
        return toSubmitResult(task, "导出任务已提交，请到下载中心查看进度");
    }

    /**
     * 提交发票Verify。
     */
    public AsyncTaskSubmitResultVO submitInvoiceVerify(Long userId, InvoiceTaskSubmitDTO dto) {
        String businessKey = AsyncTaskSupport.buildInvoiceBusinessKey(dto.getCode(), dto.getNumber());
        AsyncTaskRecord existing = findActiveTask(userId, AsyncTaskSupport.TASK_TYPE_INVOICE_VERIFY, businessKey);
        if (existing != null) {
            return toSubmitResult(existing, "当前已有相同发票验真任务正在执行，请稍后查看结果");
        }

        AsyncTaskRecord task = createTask(
                userId,
                AsyncTaskSupport.TASK_TYPE_INVOICE_VERIFY,
                AsyncTaskSupport.BUSINESS_TYPE_INVOICE,
                businessKey,
                "发票验真",
                null,
                null
        );
        asyncTaskWorker().runInvoiceVerifyTask(task.getId());
        return toSubmitResult(task, "发票验真任务已提交");
    }

    /**
     * 提交发票Ocr。
     */
    public AsyncTaskSubmitResultVO submitInvoiceOcr(Long userId, InvoiceTaskSubmitDTO dto) {
        String businessKey = AsyncTaskSupport.buildInvoiceBusinessKey(dto.getCode(), dto.getNumber());
        AsyncTaskRecord existing = findActiveTask(userId, AsyncTaskSupport.TASK_TYPE_INVOICE_OCR, businessKey);
        if (existing != null) {
            return toSubmitResult(existing, "当前已有相同发票 OCR 任务正在执行，请稍后查看结果");
        }

        AsyncTaskRecord task = createTask(
                userId,
                AsyncTaskSupport.TASK_TYPE_INVOICE_OCR,
                AsyncTaskSupport.BUSINESS_TYPE_INVOICE,
                businessKey,
                "发票 OCR 识别",
                null,
                null
        );
        asyncTaskWorker().runInvoiceOcrTask(task.getId());
        return toSubmitResult(task, "发票 OCR 任务已提交");
    }

    private ExpenseExportSubmitDTO normalizeExpenseExportPayload(ExpenseExportSubmitDTO dto) {
        if (dto == null || dto.getScene() == null || dto.getScene().isBlank()) {
            throw new IllegalArgumentException("导出场景不能为空");
        }
        ExpenseExportSubmitDTO payload = new ExpenseExportSubmitDTO();
        payload.setScene(dto.getScene().trim().toUpperCase(Locale.ROOT));
        payload.setDocumentCodes(normalizeDocumentCodes(dto.getDocumentCodes()));
        payload.setTaskIds(normalizeTaskIds(dto.getTaskIds()));
        payload.setKind(dto.getKind() == null ? null : dto.getKind().trim().toUpperCase(Locale.ROOT));
        switch (payload.getScene()) {
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_MY_EXPENSES,
                 AsyncTaskSupport.EXPENSE_EXPORT_SCENE_DOCUMENT_QUERY -> {
                if (payload.getDocumentCodes().isEmpty()) {
                    throw new IllegalArgumentException("当前没有可导出的单据");
                }
            }
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_PENDING_APPROVAL -> {
                if (payload.getTaskIds().isEmpty()) {
                    throw new IllegalArgumentException("当前没有可导出的审批任务");
                }
            }
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_OUTSTANDING -> {
                if (payload.getDocumentCodes().isEmpty()) {
                    throw new IllegalArgumentException("当前没有可导出的待处理单据");
                }
                if (!"LOAN".equals(payload.getKind()) && !"PREPAY_REPORT".equals(payload.getKind())) {
                    throw new IllegalArgumentException("待处理导出类型不合法");
                }
            }
            default -> throw new IllegalArgumentException("不支持的导出场景");
        }
        return payload;
    }

    private List<String> normalizeDocumentCodes(List<String> documentCodes) {
        if (documentCodes == null || documentCodes.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String documentCode : documentCodes) {
            if (documentCode != null && !documentCode.isBlank()) {
                normalized.add(documentCode.trim());
            }
        }
        return List.copyOf(normalized);
    }

    private List<Long> normalizeTaskIds(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return List.of();
        }
        Set<Long> normalized = new LinkedHashSet<>();
        for (Long taskId : taskIds) {
            if (taskId != null) {
                normalized.add(taskId);
            }
        }
        return List.copyOf(normalized);
    }

    /**
     * 解析报销单ExportFileName。
     */
    private String resolveExpenseExportFileName(ExpenseExportSubmitDTO payload) {
        return switch (payload.getScene()) {
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_MY_EXPENSES -> "我的报销-" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".xlsx";
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_PENDING_APPROVAL -> "待我审批-" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".xlsx";
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_DOCUMENT_QUERY -> "单据查询-" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".xlsx";
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_OUTSTANDING -> (
                    "LOAN".equals(payload.getKind()) ? "待还款单据-" : "待核销单据-"
            ) + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".xlsx";
            default -> "报销导出-" + LocalDateTime.now().format(FILE_TIME_FORMATTER) + ".xlsx";
        };
    }

    /**
     * 解析报销单Export业务类型。
     */
    private String resolveExpenseExportBusinessType(ExpenseExportSubmitDTO payload) {
        return switch (payload.getScene()) {
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_MY_EXPENSES -> "我的报销导出";
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_PENDING_APPROVAL -> "待我审批导出";
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_DOCUMENT_QUERY -> "单据查询导出";
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_OUTSTANDING -> "LOAN".equals(payload.getKind()) ? "待还款导出" : "待核销导出";
            default -> "报销导出";
        };
    }

    /**
     * 解析报销单ExportDisplayName。
     */
    private String resolveExpenseExportDisplayName(ExpenseExportSubmitDTO payload) {
        return resolveExpenseExportBusinessType(payload);
    }

    /**
     * 解析报销单Export业务Key。
     */
    private String resolveExpenseExportBusinessKey(ExpenseExportSubmitDTO payload) {
        return payload.getScene() + "#" + LocalDateTime.now().format(FILE_TIME_FORMATTER);
    }

    private String writeResultPayload(ExpenseExportSubmitDTO payload) {
        try {
            return objectMapper().writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("导出任务参数序列化失败", ex);
        }
    }
}
