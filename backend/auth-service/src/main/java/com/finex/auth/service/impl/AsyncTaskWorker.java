// 业务域：异步任务
// 文件角色：后台执行类
// 上下游关系：上游通常来自 异步提交、查询和通知相关接口，下游会继续协调 任务记录、状态更新和通知消息。
// 风险提醒：改坏后最容易影响 任务重复提交、异步状态不准确和结果回传。

package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseApprovalPendingItemVO;
import com.finex.auth.dto.ExpenseExportSubmitDTO;
import com.finex.auth.dto.ExpenseSummaryVO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.entity.DownloadRecord;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.NotificationService;
import com.finex.auth.support.AsyncTaskSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * AsyncTaskWorker：后台执行类。
 * 在后台真正执行 异步任务这一段耗时或批处理流程。
 * 改这里时，要特别关注 任务重复提交、异步状态不准确和结果回传是否会被一起带坏。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskWorker {

    private final AsyncTaskRecordMapper asyncTaskRecordMapper;
    private final DownloadRecordMapper downloadRecordMapper;
    private final NotificationService notificationService;
    private final ExpenseDocumentService expenseDocumentService;
    private final ObjectMapper objectMapper;
    private final DownloadStorageService downloadStorageService;

    /**
     * 执行Export任务。
     */
    @Async("finexAsyncExecutor")
    public void runExportTask(Long taskId) {
        AsyncTaskRecord task = requireTask(taskId);
        try {
            markRunning(task, "正在准备导出文件", 10);
            sleep(400);
            updateTask(task, 35, "正在生成导出内容");
            updateDownload(task.getDownloadRecordId(), 35, AsyncTaskSupport.DOWNLOAD_STATUS_DOWNLOADING, "生成中", null);
            sleep(500);
            updateTask(task, 70, "正在整理导出结果");
            updateDownload(task.getDownloadRecordId(), 70, AsyncTaskSupport.DOWNLOAD_STATUS_DOWNLOADING, "2.4 MB", null);
            sleep(500);
            finishSuccess(task, "导出文件已生成完成，可在下载中心查看");
            updateDownload(task.getDownloadRecordId(), 100, AsyncTaskSupport.DOWNLOAD_STATUS_COMPLETED, "2.4 MB", LocalDateTime.now());
            notificationService.sendAsyncNotification(
                    task.getUserId(),
                    AsyncTaskSupport.NOTIFICATION_TYPE_TASK,
                    "导出任务完成",
                    task.getDisplayName() + " 已处理完成，请前往下载中心查看结果。",
                    task.getTaskNo()
            );
        } catch (Exception ex) {
            log.error("导出任务执行失败, taskNo={}", task.getTaskNo(), ex);
            finishFailed(task, "导出任务执行失败，请稍后重试");
            updateDownload(task.getDownloadRecordId(), task.getProgress(), AsyncTaskSupport.DOWNLOAD_STATUS_FAILED, "-", LocalDateTime.now());
            notificationService.sendAsyncNotification(
                    task.getUserId(),
                    AsyncTaskSupport.NOTIFICATION_TYPE_TASK,
                    "导出任务失败",
                    task.getDisplayName() + " 执行失败，请稍后重试。",
                    task.getTaskNo()
            );
        }
    }

    /**
     * 执行报销单Export任务。
     */
    @Async("finexAsyncExecutor")
    public void runExpenseExportTask(Long taskId) {
        AsyncTaskRecord task = requireTask(taskId);
        Long downloadRecordId = task.getDownloadRecordId();
        try {
            ExpenseExportSubmitDTO payload = readExpenseExportPayload(task);
            markRunning(task, "正在准备导出数据", 15);
            updateDownload(downloadRecordId, 10, AsyncTaskSupport.DOWNLOAD_STATUS_DOWNLOADING, "生成中", null);

            byte[] workbookBytes;
            if (AsyncTaskSupport.EXPENSE_EXPORT_SCENE_PENDING_APPROVAL.equals(payload.getScene())) {
                updateTask(task, 45, "正在汇总审批任务");
                List<ExpenseApprovalPendingItemVO> rows = loadPendingApprovalRows(task.getUserId(), payload);
                workbookBytes = buildPendingApprovalWorkbook(resolveSheetName(payload), rows);
            } else {
                updateTask(task, 45, "正在汇总单据数据");
                List<ExpenseSummaryVO> rows = loadExpenseSummaryRows(task.getUserId(), payload);
                workbookBytes = buildExpenseSummaryWorkbook(resolveSheetName(payload), rows);
            }

            updateTask(task, 80, "正在写入 Excel 文件");
            Path path = downloadStorageService.writeWorkbook(downloadRecordId, workbookBytes);
            String fileSize = formatFileSize(Files.size(path));
            updateDownload(downloadRecordId, 90, AsyncTaskSupport.DOWNLOAD_STATUS_DOWNLOADING, fileSize, null);

            finishSuccess(task, "导出文件已生成完成，可在下载中心下载");
            updateDownload(downloadRecordId, 100, AsyncTaskSupport.DOWNLOAD_STATUS_COMPLETED, fileSize, LocalDateTime.now());
            notificationService.sendAsyncNotification(
                    task.getUserId(),
                    AsyncTaskSupport.NOTIFICATION_TYPE_TASK,
                    "报销导出完成",
                    task.getDisplayName() + " 已生成，请前往下载中心下载。",
                    task.getTaskNo()
            );
        } catch (Exception ex) {
            log.error("报销导出任务执行失败, taskNo={}", task.getTaskNo(), ex);
            downloadStorageService.deleteIfExists(downloadRecordId);
            finishFailed(task, ex.getMessage() == null || ex.getMessage().isBlank() ? "报销导出失败，请稍后重试" : ex.getMessage());
            updateDownload(downloadRecordId, task.getProgress(), AsyncTaskSupport.DOWNLOAD_STATUS_FAILED, "-", LocalDateTime.now());
            notificationService.sendAsyncNotification(
                    task.getUserId(),
                    AsyncTaskSupport.NOTIFICATION_TYPE_TASK,
                    "报销导出失败",
                    task.getDisplayName() + " 执行失败，请稍后重试。",
                    task.getTaskNo()
            );
        }
    }

    /**
     * 执行发票Verify任务。
     */
    @Async("finexAsyncExecutor")
    public void runInvoiceVerifyTask(Long taskId) {
        AsyncTaskRecord task = requireTask(taskId);
        try {
            markRunning(task, "正在提交发票验真请求", 20);
            sleep(350);
            updateTask(task, 65, "正在等待验真结果返回");
            sleep(450);

            boolean success = Math.abs(task.getBusinessKey().hashCode()) % 6 != 0;
            if (success) {
                finishSuccess(task, "发票验真完成");
                notificationService.sendAsyncNotification(
                        task.getUserId(),
                        AsyncTaskSupport.NOTIFICATION_TYPE_TASK,
                        "发票验真完成",
                        "发票 " + task.getBusinessKey() + " 验真已完成。",
                        task.getTaskNo()
                );
                return;
            }

            finishFailed(task, "发票验真失败，请稍后重试或检查发票信息");
            notificationService.sendAsyncNotification(
                    task.getUserId(),
                    AsyncTaskSupport.NOTIFICATION_TYPE_TASK,
                    "发票验真失败",
                    "发票 " + task.getBusinessKey() + " 验真失败，请稍后重试。",
                    task.getTaskNo()
            );
        } catch (Exception ex) {
            log.error("发票验真任务执行失败, taskNo={}", task.getTaskNo(), ex);
            finishFailed(task, "发票验真失败，请稍后重试");
        }
    }

    /**
     * 执行发票Ocr任务。
     */
    @Async("finexAsyncExecutor")
    public void runInvoiceOcrTask(Long taskId) {
        AsyncTaskRecord task = requireTask(taskId);
        try {
            markRunning(task, "正在提交 OCR 识别请求", 25);
            sleep(350);
            updateTask(task, 55, "正在解析发票图像");
            sleep(450);
            updateTask(task, 85, "正在整理识别结果");
            sleep(350);
            finishSuccess(task, "OCR 识别完成，请前往结果页查看");
            notificationService.sendAsyncNotification(
                    task.getUserId(),
                    AsyncTaskSupport.NOTIFICATION_TYPE_TASK,
                    "OCR 识别完成",
                    "发票 " + task.getBusinessKey() + " OCR 识别已完成。",
                    task.getTaskNo()
            );
        } catch (Exception ex) {
            log.error("OCR 任务执行失败, taskNo={}", task.getTaskNo(), ex);
            finishFailed(task, "OCR 识别失败，请稍后重试");
        }
    }

    private ExpenseExportSubmitDTO readExpenseExportPayload(AsyncTaskRecord task) {
        try {
            ExpenseExportSubmitDTO payload = objectMapper.readValue(task.getResultPayload(), ExpenseExportSubmitDTO.class);
            if (payload == null || payload.getScene() == null || payload.getScene().isBlank()) {
                throw new IllegalArgumentException("导出任务参数不完整");
            }
            return payload;
        } catch (IOException ex) {
            throw new IllegalStateException("读取导出任务参数失败", ex);
        }
    }

    /**
     * 加载报销单汇总Rows。
     */
    private List<ExpenseSummaryVO> loadExpenseSummaryRows(Long userId, ExpenseExportSubmitDTO payload) {
        Set<String> documentCodes = payload.getDocumentCodes() == null ? Set.of() : Set.copyOf(payload.getDocumentCodes());
        List<ExpenseSummaryVO> rows = switch (payload.getScene()) {
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_MY_EXPENSES -> expenseDocumentService.listExpenseSummaries(userId);
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_DOCUMENT_QUERY -> expenseDocumentService.listQueryDocumentSummaries(userId);
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_OUTSTANDING -> expenseDocumentService.listOutstandingDocuments(userId, payload.getKind());
            default -> throw new IllegalArgumentException("不支持的单据导出场景");
        };
        List<ExpenseSummaryVO> filtered = rows.stream()
                .filter(item -> documentCodes.contains(firstNonBlank(item.getDocumentCode(), item.getNo())))
                .toList();
        if (filtered.isEmpty()) {
            throw new IllegalArgumentException("当前没有可导出的数据");
        }
        return filtered;
    }

    /**
     * 加载Pending审批Rows。
     */
    private List<ExpenseApprovalPendingItemVO> loadPendingApprovalRows(Long userId, ExpenseExportSubmitDTO payload) {
        Set<Long> taskIds = payload.getTaskIds() == null ? Set.of() : Set.copyOf(payload.getTaskIds());
        List<ExpenseApprovalPendingItemVO> filtered = expenseDocumentService.listPendingApprovals(userId).stream()
                .filter(item -> taskIds.contains(item.getTaskId()))
                .toList();
        if (filtered.isEmpty()) {
            throw new IllegalArgumentException("当前没有可导出的数据");
        }
        return filtered;
    }

    /**
     * 组装报销单汇总Workbook。
     */
    private byte[] buildExpenseSummaryWorkbook(String sheetName, List<ExpenseSummaryVO> rows) {
        String[] headers = new String[] {
                "单据编号", "备用编号", "单据类型", "标题", "事由", "模板名称", "模板类型", "模板类型标签",
                "提单人", "提单部门", "当前节点", "单据状态编码", "单据状态名称",
                "单据金额", "待处理金额", "单据日期", "提交时间", "支付日期",
                "付款公司", "收款人", "往来单位", "承担部门", "标签"
        };
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            prepareSheet(sheet, headers, workbook);
            int rowIndex = 1;
            for (ExpenseSummaryVO item : rows) {
                Row row = sheet.createRow(rowIndex++);
                int cellIndex = 0;
                writeCell(row, cellIndex++, item.getDocumentCode());
                writeCell(row, cellIndex++, item.getNo());
                writeCell(row, cellIndex++, firstNonBlank(item.getType(), item.getTemplateTypeLabel()));
                writeCell(row, cellIndex++, item.getDocumentTitle());
                writeCell(row, cellIndex++, firstNonBlank(item.getDocumentReason(), item.getReason()));
                writeCell(row, cellIndex++, item.getTemplateName());
                writeCell(row, cellIndex++, item.getTemplateType());
                writeCell(row, cellIndex++, item.getTemplateTypeLabel());
                writeCell(row, cellIndex++, item.getSubmitterName());
                writeCell(row, cellIndex++, item.getSubmitterDeptName());
                writeCell(row, cellIndex++, item.getCurrentNodeName());
                writeCell(row, cellIndex++, firstNonBlank(item.getDocumentStatus(), item.getStatus()));
                writeCell(row, cellIndex++, item.getDocumentStatusLabel());
                writeCell(row, cellIndex++, decimalText(item.getAmount()));
                writeCell(row, cellIndex++, decimalText(item.getOutstandingAmount()));
                writeCell(row, cellIndex++, firstNonBlank(item.getDate(), item.getSubmittedAt()));
                writeCell(row, cellIndex++, item.getSubmittedAt());
                writeCell(row, cellIndex++, item.getPaymentDate());
                writeCell(row, cellIndex++, item.getPaymentCompanyName());
                writeCell(row, cellIndex++, item.getPayeeName());
                writeCell(row, cellIndex++, item.getCounterpartyName());
                writeCell(row, cellIndex++, joinValues(item.getUndertakeDepartmentNames()));
                writeCell(row, cellIndex, joinValues(item.getTagNames()));
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("生成报销导出 Excel 失败", ex);
        }
    }

    /**
     * 组装Pending审批Workbook。
     */
    private byte[] buildPendingApprovalWorkbook(String sheetName, List<ExpenseApprovalPendingItemVO> rows) {
        String[] headers = new String[] {
                "任务 ID", "单据编号", "标题", "事由", "模板名称", "模板类型", "模板类型标签",
                "提单人", "提单部门", "节点 Key", "节点名称",
                "单据金额", "单据状态编码", "单据状态名称", "提交时间", "任务创建时间",
                "付款公司", "收款人", "往来单位", "承担部门", "标签"
        };
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            prepareSheet(sheet, headers, workbook);
            int rowIndex = 1;
            for (ExpenseApprovalPendingItemVO item : rows) {
                Row row = sheet.createRow(rowIndex++);
                int cellIndex = 0;
                writeCell(row, cellIndex++, item.getTaskId());
                writeCell(row, cellIndex++, item.getDocumentCode());
                writeCell(row, cellIndex++, item.getDocumentTitle());
                writeCell(row, cellIndex++, item.getDocumentReason());
                writeCell(row, cellIndex++, item.getTemplateName());
                writeCell(row, cellIndex++, item.getTemplateType());
                writeCell(row, cellIndex++, item.getTemplateTypeLabel());
                writeCell(row, cellIndex++, item.getSubmitterName());
                writeCell(row, cellIndex++, item.getSubmitterDeptName());
                writeCell(row, cellIndex++, item.getNodeKey());
                writeCell(row, cellIndex++, item.getNodeName());
                writeCell(row, cellIndex++, decimalText(item.getAmount()));
                writeCell(row, cellIndex++, item.getDocumentStatus());
                writeCell(row, cellIndex++, item.getDocumentStatusLabel());
                writeCell(row, cellIndex++, item.getSubmittedAt());
                writeCell(row, cellIndex++, item.getTaskCreatedAt());
                writeCell(row, cellIndex++, item.getPaymentCompanyName());
                writeCell(row, cellIndex++, item.getPayeeName());
                writeCell(row, cellIndex++, item.getCounterpartyName());
                writeCell(row, cellIndex++, joinValues(item.getUndertakeDepartmentNames()));
                writeCell(row, cellIndex, joinValues(item.getTagNames()));
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("生成审批导出 Excel 失败", ex);
        }
    }

    private void prepareSheet(Sheet sheet, String[] headers, Workbook workbook) {
        sheet.createFreezePane(0, 1);
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        for (int index = 0; index < headers.length; index++) {
            Cell cell = headerRow.createCell(index);
            cell.setCellValue(headers[index]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(index, Math.min(40, Math.max(headers[index].length() + 6, 16)) * 256);
        }
    }

    private void writeCell(Row row, int cellIndex, Object value) {
        row.createCell(cellIndex).setCellValue(stringValue(value));
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            return decimalText(decimal);
        }
        return String.valueOf(value);
    }

    private String decimalText(BigDecimal decimal) {
        return decimal == null ? "" : decimal.stripTrailingZeros().toPlainString();
    }

    private String joinValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .filter(item -> item != null && !item.isBlank())
                .collect(Collectors.joining("、"));
    }

    /**
     * 解析SheetName。
     */
    private String resolveSheetName(ExpenseExportSubmitDTO payload) {
        return switch (payload.getScene()) {
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_MY_EXPENSES -> "我的报销";
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_PENDING_APPROVAL -> "待我审批";
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_DOCUMENT_QUERY -> "单据查询";
            case AsyncTaskSupport.EXPENSE_EXPORT_SCENE_OUTSTANDING -> "LOAN".equals(payload.getKind()) ? "待还款" : "待核销";
            default -> "导出结果";
        };
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private AsyncTaskRecord requireTask(Long taskId) {
        AsyncTaskRecord task = asyncTaskRecordMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        return task;
    }

    private void markRunning(AsyncTaskRecord task, String message, int progress) {
        task.setStatus(AsyncTaskSupport.TASK_STATUS_RUNNING);
        task.setProgress(progress);
        task.setResultMessage(message);
        task.setStartedAt(LocalDateTime.now());
        asyncTaskRecordMapper.updateById(task);
    }

    /**
     * 更新任务。
     */
    private void updateTask(AsyncTaskRecord task, int progress, String message) {
        task.setProgress(progress);
        task.setResultMessage(message);
        asyncTaskRecordMapper.updateById(task);
    }

    private void finishSuccess(AsyncTaskRecord task, String message) {
        task.setStatus(AsyncTaskSupport.TASK_STATUS_SUCCESS);
        task.setProgress(100);
        task.setResultMessage(message);
        task.setFinishedAt(LocalDateTime.now());
        asyncTaskRecordMapper.updateById(task);
    }

    private void finishFailed(AsyncTaskRecord task, String message) {
        task.setStatus(AsyncTaskSupport.TASK_STATUS_FAILED);
        task.setProgress(100);
        task.setResultMessage(message);
        task.setFinishedAt(LocalDateTime.now());
        asyncTaskRecordMapper.updateById(task);
    }

    /**
     * 更新下载。
     */
    private void updateDownload(Long downloadRecordId, Integer progress, String status, String fileSize, LocalDateTime finishedAt) {
        if (downloadRecordId == null) {
            return;
        }
        DownloadRecord record = downloadRecordMapper.selectById(downloadRecordId);
        if (record == null) {
            return;
        }
        record.setProgress(progress);
        record.setStatus(status);
        record.setFileSize(fileSize);
        record.setFinishedAt(finishedAt);
        downloadRecordMapper.updateById(record);
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.ROOT, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        return String.format(Locale.ROOT, "%.1f MB", mb);
    }

    private void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis + ThreadLocalRandom.current().nextInt(80, 180));
    }
}
