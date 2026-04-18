package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseApprovalLogVO;
import com.finex.auth.dto.ExpenseApprovalTaskVO;
import com.finex.auth.dto.ExpenseDetailInstanceDetailVO;
import com.finex.auth.dto.ExpenseDocumentBankPaymentVO;
import com.finex.auth.dto.ExpenseDocumentBankReceiptVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class ExpenseDocumentPdfRenderer {
    private static final String FONT_FAMILY = "Noto Sans SC";
    private static final String FONT_RESOURCE_PATH = "fonts/NotoSansSC-VF.ttf";
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat FILE_SIZE_FORMAT = new DecimalFormat("0.0");
    private static final List<String> DETAIL_AMOUNT_FIELD_KEYS = List.of("invoiceAmount", "actualPaymentAmount", "amount", "detailAmount");
    private static final List<String> DETAIL_VERIFY_FIELD_KEYS = List.of("invoiceVerifyStatus", "verifyStatus", "invoiceVerificationResult", "invoiceCheckResult");
    private static final List<String> DETAIL_EXCEPTION_FIELD_KEYS = List.of("abnormalDisplay", "exceptionDisplay", "invoiceExceptionDisplay", "exceptionReason");
    private static final Set<String> TIMELINE_ACTION_TYPES = Set.of("SUBMIT", "RECALL", "RESUBMIT", "APPROVE", "REJECT", "MODIFY", "TRANSFER", "ADD_SIGN", "PAYMENT_START", "PAYMENT_COMPLETE", "PAYMENT_EXCEPTION", "FINISH", "EXCEPTION");

    private volatile File cachedFontFile;

    public byte[] renderDocuments(List<PrintDocumentBundle> documents, ExpenseDocumentPrintOrientation orientation) {
        if (documents == null || documents.isEmpty()) {
            throw new IllegalArgumentException("缺少可打印的单据数据");
        }
        try (java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(buildHtml(documents, orientation), null);
            builder.useFont(resolveFontFile(), FONT_FAMILY);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("生成报销单 PDF 失败", ex);
        }
    }

    private String buildHtml(List<PrintDocumentBundle> documents, ExpenseDocumentPrintOrientation orientation) {
        StringBuilder html = new StringBuilder(32 * 1024);
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'/><style>");
        html.append(buildStyles(orientation));
        html.append("</style></head><body>");
        for (int index = 0; index < documents.size(); index++) {
            appendDocument(html, documents.get(index), index > 0);
        }
        html.append("</body></html>");
        return html.toString();
    }

    private String buildStyles(ExpenseDocumentPrintOrientation orientation) {
        return """
                @page {
                  size: A4 __SIZE__;
                  margin: 2.5cm 0.5cm 1cm 0.5cm;
                  @bottom-center {
                    content: '第 ' counter(page) ' 页 共 ' counter(pages) ' 页';
                    color: #6b7280;
                    font-size: 10pt;
                    padding-top: 0.15cm;
                  }
                }
                * { box-sizing: border-box; }
                html, body { margin: 0; padding: 0; background: #fff; }
                body { font-family: '__FONT__'; color: #1f2937; font-size: 10pt; line-height: 1.6; }
                .document { page-break-inside: auto; }
                .document--break { page-break-before: always; }
                .header-table, .field-grid, .detail-summary, .receipt-table, .timeline-table, .account-grid { width: 100%; border-collapse: collapse; table-layout: fixed; }
                .document-header { margin-bottom: 16pt; padding-top: 4pt; border-top: 4pt solid #6f8195; border-bottom: 1.5pt solid #d5dde7; padding-bottom: 14pt; }
                .header-table { height: 108pt; }
                .header-main { padding-right: 14pt; vertical-align: top; height: 108pt; }
                .header-side { width: 196pt; vertical-align: top; height: 108pt; }
                .header-main-panel { height: 108pt; }
                .header-kicker { font-size: 8.5pt; color: #6f8195; letter-spacing: 1.2pt; text-transform: uppercase; }
                .header-title { margin: 4pt 0 0; font-size: 20pt; line-height: 1.25; font-weight: 700; color: #111827; }
                .header-meta { margin-top: 7pt; font-size: 9pt; color: #475569; }
                .header-submeta { margin-top: 4pt; font-size: 8.6pt; color: #64748b; }
                .amount-panel { border: 1pt solid #d6dee8; background: #f6f8fb; padding: 10pt 12pt; text-align: right; border-radius: 8pt; height: 108pt; }
                .amount-label { display: block; font-size: 8.5pt; color: #64748b; }
                .amount-value { display: block; margin-top: 6pt; font-size: 18pt; font-weight: 700; color: #0f172a; }
                .amount-meta { margin-top: 10pt; padding-top: 8pt; border-top: 1pt solid #dbe3ec; font-size: 8.4pt; line-height: 1.55; color: #64748b; }
                .amount-meta-row { margin-top: 2pt; }
                .amount-meta-row:first-child { margin-top: 0; }
                .amount-meta-label { display: inline-block; min-width: 42pt; color: #475569; text-align: left; }
                .amount-meta-value { color: #64748b; }
                .section { margin-top: 14pt; }
                .section-group--followup { page-break-before: always; }
                .section-title { margin: 0 0 9pt; padding-bottom: 5pt; border-bottom: 1pt solid #dbe3ec; font-size: 11.5pt; font-weight: 700; color: #314255; }
                .section-title::before { content: ''; display: inline-block; width: 8pt; height: 8pt; margin-right: 6pt; border-radius: 50%; background: #6f8195; vertical-align: 1pt; }
                .section-subtitle { margin: 8pt 0 4pt; font-size: 9.5pt; font-weight: 700; color: #475569; }
                .field-grid td { width: 50%; padding: 0 12pt 8pt 0; vertical-align: top; }
                .field-grid td.full { width: 100%; padding-right: 0; }
                .field-pair { border-bottom: 1pt solid #e6ebf1; padding-bottom: 6pt; min-height: 36pt; }
                .field-pair--compact { min-height: 28pt; }
                .field-label { display: block; margin-bottom: 2pt; font-size: 8.4pt; color: #6b7280; }
                .field-value { display: block; font-size: 10pt; color: #111827; white-space: pre-wrap; word-break: break-word; }
                .field-value--normal { font-weight: 500; color: #223041; }
                .field-value--strong { font-weight: 600; }
                .field-note { display: block; margin-top: 4pt; font-size: 8.6pt; color: #64748b; }
                .account-grid td { width: 33.33%; padding-right: 8pt; vertical-align: top; }
                .account-item { border: 1pt solid #e2e8f0; background: #f8fafc; padding: 6pt 8pt; border-radius: 6pt; min-height: 44pt; }
                .account-item__label { display: block; font-size: 8pt; color: #64748b; }
                .account-item__value { display: block; margin-top: 3pt; font-size: 9.2pt; color: #1f2937; font-weight: 600; white-space: pre-wrap; word-break: break-word; }
                .detail-block { margin-top: 10pt; padding-top: 10pt; border-top: 1pt solid #dbe3ec; page-break-inside: avoid; }
                .detail-block:first-child { margin-top: 0; padding-top: 0; border-top: none; }
                .detail-summary td { width: 25%; padding: 0 10pt 8pt 0; vertical-align: top; }
                .summary-item { border-bottom: 1pt solid #dbe3ec; padding-bottom: 6pt; min-height: 34pt; }
                .summary-label { display: block; font-size: 8.2pt; color: #64748b; }
                .summary-value { display: block; margin-top: 2pt; font-size: 9.7pt; font-weight: 600; color: #111827; white-space: pre-wrap; word-break: break-word; }
                .receipt-table th { text-align: left; padding: 4pt 8pt 5pt 0; border-bottom: 1pt solid #dbe3ec; font-size: 8.4pt; font-weight: 700; color: #64748b; }
                .receipt-table td { padding: 6pt 8pt 6pt 0; border-bottom: 1pt solid #edf2f7; font-size: 9.2pt; color: #1f2937; vertical-align: top; }
                .timeline-table td { vertical-align: top; padding: 0 0 8pt; }
                .timeline-time { width: 108pt; padding-right: 12pt; font-size: 8.4pt; color: #64748b; }
                .timeline-content { border-left: 1.5pt solid #cfdae5; padding-left: 10pt; }
                .timeline-title { font-size: 9.8pt; font-weight: 600; color: #111827; }
                .timeline-desc { margin-top: 2pt; font-size: 8.6pt; color: #475569; white-space: pre-wrap; word-break: break-word; }
                .empty-state { padding: 4pt 0; font-size: 9pt; color: #6b7280; }
                """.replace("__SIZE__", orientation.toCssSize()).replace("__FONT__", FONT_FAMILY);
    }

    private void appendDocument(StringBuilder html, PrintDocumentBundle bundle, boolean pageBreakBefore) {
        ExpenseDocumentDetailVO detail = bundle.getDetail();
        html.append("<section class='document");
        if (pageBreakBefore) {
            html.append(" document--break");
        }
        html.append("'>");
        appendHeader(html, detail);
        appendSummarySection(html, detail);
        appendFormSection(html, detail);
        html.append("<div class='section-group--followup'>");
        appendExpenseDetailsSection(html, bundle.getExpenseDetails());
        appendBankSection(html, detail.getBankPayment(), detail.getBankReceipts());
        appendTimelineSection(html, detail);
        html.append("</div>");
        html.append("</section>");
    }

    private void appendHeader(StringBuilder html, ExpenseDocumentDetailVO detail) {
        html.append("<header class='document-header'><table class='header-table'><tr><td class='header-main'><div class='header-main-panel'>");
        html.append("<div class='header-kicker'>Expense Document</div>");
        html.append("<h1 class='header-title'>").append(escapeHtml(defaultText(detail.getDocumentTitle(), detail.getDocumentCode(), "报销打印单"))).append("</h1>");
        html.append("<div class='header-meta'>单据编号：").append(escapeHtml(defaultText(detail.getDocumentCode(), "-"))).append("　　模板：").append(escapeHtml(defaultText(detail.getTemplateName(), "-"))).append("　　状态：").append(escapeHtml(defaultText(detail.getStatusLabel(), detail.getStatus(), "-"))).append("</div>");
        html.append("<div class='header-submeta'>提单人：").append(escapeHtml(defaultText(detail.getSubmitterName(), "-"))).append("　　提交时间：").append(escapeHtml(defaultText(detail.getSubmittedAt(), "-"))).append("</div>");
        html.append("</div></td><td class='header-side'><div class='amount-panel'><span class='amount-label'>单据金额</span><span class='amount-value'>¥").append(escapeHtml(formatMoney(detail.getTotalAmount()))).append("</span><div class='amount-meta'>");
        appendAmountMetaRow(html, "当前节点", defaultText(detail.getCurrentNodeName(), "-"));
        appendAmountMetaRow(html, "完成时间", defaultText(detail.getFinishedAt(), "-"));
        html.append("</div></div></td></tr></table></header>");
    }

    private void appendAmountMetaRow(StringBuilder html, String label, String value) {
        html.append("<div class='amount-meta-row'><span class='amount-meta-label'>")
                .append(escapeHtml(label))
                .append("</span><span class='amount-meta-value'>")
                .append(escapeHtml(defaultText(value, "-")))
                .append("</span></div>");
    }

    private void appendSummarySection(StringBuilder html, ExpenseDocumentDetailVO detail) {
        html.append("<section class='section'><h2 class='section-title'>单据概览</h2>");
        List<PrintField> fields = new ArrayList<>();
        fields.add(PrintField.normal("单据状态", defaultText(detail.getStatusLabel(), detail.getStatus(), "-")));
        fields.add(PrintField.fullWidth("事由", defaultText(detail.getDocumentReason(), "-")));
        appendFieldTable(html, fields, false);
        html.append("</section>");
    }

    private void appendFormSection(StringBuilder html, ExpenseDocumentDetailVO detail) {
        html.append("<section class='section'><h2 class='section-title'>单据表单</h2>");
        List<PrintField> fields = buildDocumentFields(detail.getFormSchemaSnapshot(), detail.getFormData(), buildOptionMap(detail.getCompanyOptions()), buildOptionMap(detail.getDepartmentOptions()));
        if (fields.isEmpty()) {
            html.append("<div class='empty-state'>暂无表单数据</div>");
        } else {
            appendFieldTable(html, fields, false);
        }
        html.append("</section>");
    }

    private void appendExpenseDetailsSection(StringBuilder html, List<ExpenseDetailInstanceDetailVO> expenseDetails) {
        html.append("<section class='section'><h2 class='section-title'>费用明细</h2>");
        if (expenseDetails == null || expenseDetails.isEmpty()) {
            html.append("<div class='empty-state'>暂无费用明细</div></section>");
            return;
        }
        for (ExpenseDetailInstanceDetailVO detail : expenseDetails) {
            html.append("<div class='detail-block'>");
            appendDetailSummaryTable(html, detail);
            html.append("</div>");
        }
        html.append("</section>");
    }

    private void appendDetailSummaryTable(StringBuilder html, ExpenseDetailInstanceDetailVO detail) {
        html.append("<table class='detail-summary'><tr>");
        appendSummaryCell(html, "费用类型", resolveExpenseTypeLabel(detail));
        appendSummaryCell(html, "金额", resolveDetailAmount(detail));
        appendSummaryCell(html, "发票验真结果", resolveFirstText(detail.getFormData(), DETAIL_VERIFY_FIELD_KEYS, "-"));
        appendSummaryCell(html, "异常显示", resolveFirstText(detail.getFormData(), DETAIL_EXCEPTION_FIELD_KEYS, "-"));
        html.append("</tr></table>");
    }

    private void appendSummaryCell(StringBuilder html, String label, String value) {
        html.append("<td><div class='summary-item'><span class='summary-label'>").append(escapeHtml(label)).append("</span><span class='summary-value'>").append(escapeHtml(defaultText(value, "-"))).append("</span></div></td>");
    }

    private void appendBankSection(StringBuilder html, ExpenseDocumentBankPaymentVO bankPayment, List<ExpenseDocumentBankReceiptVO> bankReceipts) {
        boolean hasPayment = bankPayment != null;
        boolean hasReceipts = bankReceipts != null && !bankReceipts.isEmpty();
        if (!hasPayment && !hasReceipts) {
            return;
        }
        html.append("<section class='section'><h2 class='section-title'>支付与回单</h2>");
        if (hasPayment) {
            List<PrintField> paymentFields = new ArrayList<>();
            paymentFields.add(PrintField.normal("支付状态", defaultText(bankPayment.getPaymentStatusLabel(), bankPayment.getPaymentStatusCode(), "-")));
            paymentFields.add(PrintField.normal("支付方式", bankPayment.isManualPaid() ? "手动支付" : "银行回调"));
            paymentFields.add(PrintField.normal("支付时间", defaultText(bankPayment.getPaidAt(), "-")));
            paymentFields.add(PrintField.normal("回单状态", defaultText(bankPayment.getReceiptStatusLabel(), "-")));
            paymentFields.add(PrintField.normal("直连账户", defaultText(bankPayment.getCompanyBankAccountName(), "-")));
            paymentFields.add(PrintField.normal("银行渠道", defaultText(joinNonBlank(bankPayment.getBankProvider(), bankPayment.getBankChannel(), " / "), "-")));
            paymentFields.add(PrintField.normal("银行流水号", defaultText(bankPayment.getBankFlowNo(), "-")));
            paymentFields.add(PrintField.normal("银行订单号", defaultText(bankPayment.getBankOrderNo(), "-")));
            if (notBlank(bankPayment.getReceiptReceivedAt())) {
                paymentFields.add(PrintField.normal("回单接收时间", bankPayment.getReceiptReceivedAt()));
            }
            if (notBlank(bankPayment.getLastErrorMessage())) {
                paymentFields.add(PrintField.fullWidth("失败原因", bankPayment.getLastErrorMessage()));
            }
            appendFieldTable(html, paymentFields, false);
        }
        html.append("<div class='section-subtitle'>银行回单</div>");
        if (!hasReceipts) {
            html.append("<div class='empty-state'>暂无银行回单</div>");
        } else {
            appendReceiptTable(html, bankReceipts);
        }
        html.append("</section>");
    }

    private void appendReceiptTable(StringBuilder html, List<ExpenseDocumentBankReceiptVO> bankReceipts) {
        html.append("<table class='receipt-table'><thead><tr><th style='width:42%;'>文件名称</th><th style='width:24%;'>接收时间</th><th style='width:18%;'>文件类型</th><th style='width:16%;'>文件大小</th></tr></thead><tbody>");
        for (ExpenseDocumentBankReceiptVO receipt : bankReceipts) {
            html.append("<tr><td>").append(escapeHtml(defaultText(receipt.getFileName(), "-"))).append("</td><td>").append(escapeHtml(defaultText(receipt.getReceivedAt(), "-"))).append("</td><td>").append(escapeHtml(defaultText(receipt.getContentType(), "-"))).append("</td><td>").append(escapeHtml(formatFileSize(receipt.getFileSize()))).append("</td></tr>");
        }
        html.append("</tbody></table>");
    }

    private void appendTimelineSection(StringBuilder html, ExpenseDocumentDetailVO detail) {
        html.append("<section class='section'><h2 class='section-title'>审批轨迹</h2>");
        List<TimelineItem> items = buildTimelineItems(detail);
        if (items.isEmpty()) {
            html.append("<div class='empty-state'>暂无审批轨迹</div></section>");
            return;
        }
        html.append("<table class='timeline-table'><tbody>");
        for (TimelineItem item : items) {
            html.append("<tr><td class='timeline-time'>").append(escapeHtml(defaultText(item.timestamp, "-"))).append("</td><td class='timeline-content'><div class='timeline-title'>").append(escapeHtml(defaultText(item.title, "-"))).append("</div>");
            if (notBlank(item.description)) {
                html.append("<div class='timeline-desc'>").append(escapeHtml(item.description)).append("</div>");
            }
            html.append("</td></tr>");
        }
        html.append("</tbody></table></section>");
    }
    private List<PrintField> buildDocumentFields(Map<String, Object> schema, Map<String, Object> formData, Map<String, String> companyMap, Map<String, String> departmentMap) {
        List<PrintField> fields = new ArrayList<>();
        Map<String, Object> data = formData == null ? Collections.emptyMap() : formData;
        for (Map<String, Object> block : extractBlocks(schema)) {
            String label = trimToNull(stringValue(block.get("label")));
            String fieldKey = trimToNull(stringValue(block.get("fieldKey")));
            Map<String, Object> props = asMap(block.get("props"));
            String kind = trimToNull(stringValue(block.get("kind")));
            String controlType = trimToNull(stringValue(props.get("controlType")));
            String componentCode = trimToNull(stringValue(props.get("componentCode")));
            Object rawValue = fieldKey == null ? null : data.get(fieldKey);
            boolean fullWidth = normalizeSpan(block.get("span")) == 2;
            if (Objects.equals(controlType, "SECTION")) {
                String content = trimToNull(stringValue(props.get("content")));
                fields.add(PrintField.fullWidth(defaultText(label, "分组说明"), defaultText(content, stringValue(block.get("helpText")), "-")));
                continue;
            }
            if (Objects.equals(kind, "BUSINESS_COMPONENT") && Objects.equals(componentCode, "payee-account")) {
                fields.add(buildPayeeAccountField(defaultText(label, "收款账户"), rawValue));
                continue;
            }
            String displayValue = resolveBlockDisplayValue(rawValue, props, companyMap, departmentMap, componentCode, controlType);
            fields.add(fullWidth ? PrintField.fullWidth(defaultText(label, fieldKey, "字段"), displayValue) : PrintField.normal(defaultText(label, fieldKey, "字段"), displayValue));
        }
        return fields;
    }

    private void appendFieldTable(StringBuilder html, List<PrintField> fields, boolean compact) {
        if (fields == null || fields.isEmpty()) {
            return;
        }
        html.append("<table class='field-grid'><tbody>");
        List<PrintField> rowBuffer = new ArrayList<>(2);
        for (PrintField field : fields) {
            if (field == null) {
                continue;
            }
            if (field.isFullWidth()) {
                if (!rowBuffer.isEmpty()) {
                    appendFieldRow(html, rowBuffer, compact);
                    rowBuffer.clear();
                }
                appendFieldRow(html, List.of(field), compact);
                continue;
            }
            rowBuffer.add(field);
            if (rowBuffer.size() == 2) {
                appendFieldRow(html, rowBuffer, compact);
                rowBuffer = new ArrayList<>(2);
            }
        }
        if (!rowBuffer.isEmpty()) {
            appendFieldRow(html, rowBuffer, compact);
        }
        html.append("</tbody></table>");
    }

    private void appendFieldRow(StringBuilder html, List<PrintField> fields, boolean compact) {
        html.append("<tr>");
        if (fields.size() == 1) {
            appendFieldCell(html, fields.get(0), true, compact);
        } else {
            appendFieldCell(html, fields.get(0), false, compact);
            appendFieldCell(html, fields.get(1), false, compact);
        }
        html.append("</tr>");
    }

    private void appendFieldCell(StringBuilder html, PrintField field, boolean fullWidth, boolean compact) {
        html.append("<td");
        if (fullWidth || field.isFullWidth()) {
            html.append(" colspan='2' class='full'");
        }
        html.append("><div class='field-pair");
        if (compact) {
            html.append(" field-pair--compact");
        }
        html.append("'><span class='field-label'>").append(escapeHtml(field.getLabel())).append("</span>");
        if (field.hasMiniItems()) {
            if (notBlank(field.getValue())) {
                html.append("<span class='field-value field-value--strong'>").append(escapeHtml(field.getValue())).append("</span>");
            }
            if (notBlank(field.getNote())) {
                html.append("<span class='field-note'>").append(escapeHtml(field.getNote())).append("</span>");
            }
            html.append("<table class='account-grid'><tr>");
            for (MiniItem item : field.getMiniItems()) {
                html.append("<td><div class='account-item'><span class='account-item__label'>").append(escapeHtml(item.label)).append("</span><span class='account-item__value'>").append(escapeHtml(defaultText(item.value, "-"))).append("</span></div></td>");
            }
            html.append("</tr></table>");
        } else {
            html.append("<span class='field-value field-value--normal'>").append(escapeHtml(defaultText(field.getValue(), "-"))).append("</span>");
            if (notBlank(field.getNote())) {
                html.append("<span class='field-note'>").append(escapeHtml(field.getNote())).append("</span>");
            }
        }
        html.append("</div></td>");
    }

    private PrintField buildPayeeAccountField(String label, Object rawValue) {
        Map<String, Object> accountValue = asMap(rawValue);
        List<MiniItem> items = List.of(
                new MiniItem("账户名称", firstNonBlank(accountValue.get("ownerName"), accountValue.get("accountName"), accountValue.get("label"), accountValue.get("value"))),
                new MiniItem("银行账号", firstNonBlank(accountValue.get("accountNoMasked"), accountValue.get("accountNo"))),
                new MiniItem("开户行", firstNonBlank(accountValue.get("bankName"), accountValue.get("bankBranchName")))
        );
        return PrintField.mini(label, null, null, items);
    }

    private List<TimelineItem> buildTimelineItems(ExpenseDocumentDetailVO detail) {
        List<TimelineItem> items = new ArrayList<>();
        List<ExpenseApprovalLogVO> logs = detail.getActionLogs() == null ? List.of() : detail.getActionLogs();
        for (ExpenseApprovalLogVO log : logs) {
            if (!shouldDisplayTimelineLog(log.getActionType())) {
                continue;
            }
            items.add(new TimelineItem(defaultText(log.getCreatedAt(), ""), buildTimelineTitle(log, detail), buildTimelineDescription(log)));
        }
        Map<String, TimelineItem> pendingItems = new LinkedHashMap<>();
        List<ExpenseApprovalTaskVO> tasks = detail.getCurrentTasks() == null ? List.of() : detail.getCurrentTasks();
        for (ExpenseApprovalTaskVO task : tasks) {
            String assigneeName = defaultText(task.getAssigneeName(), "待分配处理人");
            String dedupeKey = defaultText(task.getNodeKey(), "pending") + "::" + assigneeName;
            if (pendingItems.containsKey(dedupeKey)) {
                continue;
            }
            String title = Objects.equals(trimToNull(task.getNodeType()), "PAYMENT") ? assigneeName + " 待支付处理" : defaultText(task.getNodeName(), "审批节点") + " · " + assigneeName + " 待处理";
            pendingItems.put(dedupeKey, new TimelineItem(defaultText(task.getCreatedAt(), ""), title, defaultText(task.getActionComment(), "")));
        }
        items.addAll(pendingItems.values());
        return items;
    }

    private boolean shouldDisplayTimelineLog(String actionType) { return TIMELINE_ACTION_TYPES.contains(defaultText(actionType, "")); }

    private String buildTimelineTitle(ExpenseApprovalLogVO log, ExpenseDocumentDetailVO detail) {
        String actorName = defaultText(log.getActorName(), "处理人");
        String nodeName = defaultText(log.getNodeName(), "审批节点");
        return switch (defaultText(log.getActionType(), "")) {
            case "SUBMIT" -> defaultText(detail.getSubmitterName(), log.getActorName(), "提单人") + " 提交单据";
            case "RECALL" -> actorName + " 召回单据";
            case "RESUBMIT" -> actorName + " 重新提交";
            case "APPROVE" -> nodeName + " · " + actorName + " 审批通过";
            case "REJECT" -> nodeName + " · " + actorName + " 审批驳回";
            case "MODIFY" -> actorName + " 修改单据";
            case "TRANSFER" -> actorName + " 转交审批";
            case "ADD_SIGN" -> actorName + " 发起加签";
            case "PAYMENT_START" -> actorName + " 发起支付";
            case "PAYMENT_COMPLETE" -> actorName + " 确认已支付";
            case "PAYMENT_EXCEPTION" -> actorName + " 标记支付异常";
            case "FINISH" -> "流程已完成";
            case "EXCEPTION" -> "流程异常";
            default -> defaultText(log.getActionType(), "操作记录");
        };
    }

    private String buildTimelineDescription(ExpenseApprovalLogVO log) {
        String actionType = defaultText(log.getActionType(), "");
        List<String> parts = new ArrayList<>();
        if (Set.of("SUBMIT", "APPROVE", "REJECT", "PAYMENT_COMPLETE", "PAYMENT_EXCEPTION", "RECALL", "RESUBMIT", "MODIFY").contains(actionType)) {
            String comment = defaultText(log.getActionComment(), "");
            if (notBlank(comment) && !isRedundantTimelineComment(actionType, comment)) {
                parts.add(comment);
            }
        } else if (notBlank(log.getActionComment())) {
            parts.add(log.getActionComment());
        }
        String targetUserName = trimToNull(stringValue(asMap(log.getPayload()).get("targetUserName")));
        if (Objects.equals(actionType, "TRANSFER") && targetUserName != null) {
            parts.add("转交给：" + targetUserName);
        }
        if (Objects.equals(actionType, "ADD_SIGN") && targetUserName != null) {
            parts.add("加签给：" + targetUserName);
        }
        return String.join(" / ", parts);
    }
    private boolean isRedundantTimelineComment(String actionType, String comment) {
        String normalized = defaultText(comment, "").replaceAll("\\s+", "");
        if (normalized.isEmpty()) {
            return false;
        }
        Map<String, List<String>> redundantMap = Map.of(
                "APPROVE", List.of("通过", "审批通过", "同意"),
                "REJECT", List.of("驳回", "审批驳回"),
                "PAYMENT_COMPLETE", List.of("已支付", "确认已支付"),
                "PAYMENT_EXCEPTION", List.of("支付异常", "标记支付异常")
        );
        return redundantMap.getOrDefault(actionType, List.of()).contains(normalized);
    }

    private String resolveBlockDisplayValue(Object rawValue, Map<String, Object> props, Map<String, String> companyMap, Map<String, String> departmentMap, String componentCode, String controlType) {
        if (Objects.equals(componentCode, "payment-company")) {
            return joinValues(rawValue, companyMap);
        }
        if (Objects.equals(componentCode, "undertake-department")) {
            return joinValues(rawValue, departmentMap);
        }
        if (Objects.equals(componentCode, "related-document") || Objects.equals(componentCode, "writeoff-document")) {
            return formatRelatedDocuments(rawValue);
        }
        if (Objects.equals(controlType, "SWITCH")) {
            return truthy(rawValue) ? "是" : "否";
        }
        if (Objects.equals(controlType, "DATE_RANGE") && rawValue instanceof Collection<?> values) {
            return joinCollection(values, " 至 ");
        }
        if (List.of("SELECT", "MULTI_SELECT", "RADIO", "CHECKBOX").contains(defaultText(controlType, ""))) {
            return joinValues(rawValue, buildBlockOptionMap(props));
        }
        return formatGenericValue(rawValue);
    }

    private String resolveExpenseTypeLabel(ExpenseDetailInstanceDetailVO detail) {
        Map<String, Object> formData = detail.getFormData() == null ? Collections.emptyMap() : detail.getFormData();
        String expenseTypeCode = trimToNull(stringValue(formData.get("expenseTypeCode")));
        if (expenseTypeCode != null) {
            for (Map<String, Object> block : extractBlocks(detail.getSchemaSnapshot())) {
                if (!Objects.equals(trimToNull(stringValue(block.get("fieldKey"))), "expenseTypeCode")) {
                    continue;
                }
                String label = buildBlockOptionMap(asMap(block.get("props"))).get(expenseTypeCode);
                if (notBlank(label)) {
                    return label;
                }
            }
        }
        return defaultText(firstNonBlank(detail.getDetailTitle(), detail.getDetailTypeLabel(), detail.getDetailType()), "-");
    }

    private String resolveDetailAmount(ExpenseDetailInstanceDetailVO detail) {
        for (String fieldKey : DETAIL_AMOUNT_FIELD_KEYS) {
            String amount = formatMoneyValue(detail.getFormData() == null ? null : detail.getFormData().get(fieldKey));
            if (notBlank(amount)) {
                return amount;
            }
        }
        return "-";
    }

    private String resolveFirstText(Map<String, Object> formData, List<String> keys, String fallback) {
        if (formData == null || formData.isEmpty()) {
            return fallback;
        }
        for (String key : keys) {
            String value = trimToNull(stringValue(formData.get(key)));
            if (value != null) {
                return value;
            }
        }
        return fallback;
    }

    private Map<String, String> buildOptionMap(List<ProcessFormOptionVO> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> optionMap = new LinkedHashMap<>();
        for (ProcessFormOptionVO option : options) {
            String value = trimToNull(option == null ? null : option.getValue());
            String label = trimToNull(option == null ? null : option.getLabel());
            if (value != null && label != null) {
                optionMap.put(value, label);
            }
        }
        return optionMap;
    }

    private Map<String, String> buildBlockOptionMap(Map<String, Object> props) {
        Object rawOptions = props.get("options");
        if (!(rawOptions instanceof Collection<?> options) || options.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> optionMap = new LinkedHashMap<>();
        for (Object option : options) {
            Map<String, Object> optionValue = asMap(option);
            String value = trimToNull(stringValue(optionValue.get("value")));
            String label = trimToNull(stringValue(optionValue.get("label")));
            if (value != null && label != null) {
                optionMap.put(value, label);
            }
        }
        return optionMap;
    }

    private String joinValues(Object rawValue, Map<String, String> labelMap) {
        if (rawValue instanceof Collection<?> collection) {
            List<String> values = new ArrayList<>();
            for (Object item : collection) {
                values.add(resolveMappedValue(item, labelMap));
            }
            return values.isEmpty() ? "-" : String.join("、", values);
        }
        return defaultText(resolveMappedValue(rawValue, labelMap), "-");
    }

    private String resolveMappedValue(Object rawValue, Map<String, String> labelMap) {
        String key = trimToNull(resolveLookupKey(rawValue));
        return key != null && labelMap.containsKey(key) ? labelMap.get(key) : formatGenericValue(rawValue);
    }

    private String formatRelatedDocuments(Object rawValue) {
        if (!(rawValue instanceof Collection<?> documents) || documents.isEmpty()) {
            return formatGenericValue(rawValue);
        }
        List<String> values = new ArrayList<>();
        for (Object document : documents) {
            Map<String, Object> item = asMap(document);
            String documentCode = firstNonBlank(item.get("documentCode"), item.get("value"));
            String documentTitle = firstNonBlank(item.get("documentTitle"), item.get("label"));
            String amount = formatMoneyValue(item.get("writeOffAmount"));
            String text = defaultText(documentTitle, documentCode, "关联单据");
            if (notBlank(documentCode) && !Objects.equals(documentCode, documentTitle)) {
                text = text + "（" + documentCode + "）";
            }
            if (notBlank(amount)) {
                text = text + " / 核销金额：" + amount;
            }
            values.add(text);
        }
        return values.isEmpty() ? "-" : String.join("\n", values);
    }

    private String formatGenericValue(Object value) {
        if (value == null) {
            return "-";
        }
        if (value instanceof String text) {
            return defaultText(trimToNull(text), "-");
        }
        if (value instanceof Number number) {
            return number.toString();
        }
        if (value instanceof Boolean bool) {
            return bool ? "是" : "否";
        }
        if (value instanceof Collection<?> values) {
            List<String> items = new ArrayList<>();
            for (Object item : values) {
                items.add(formatGenericValue(item));
            }
            return items.isEmpty() ? "-" : String.join("、", items);
        }
        Map<String, Object> mapValue = asMap(value);
        if (!mapValue.isEmpty()) {
            String label = firstNonBlank(mapValue.get("label"), mapValue.get("ownerName"), mapValue.get("accountName"), mapValue.get("value"), mapValue.get("fileName"));
            return defaultText(label, mapValue.toString(), "-");
        }
        return defaultText(value.toString(), "-");
    }

    private String resolveLookupKey(Object rawValue) {
        if (rawValue instanceof String text) {
            return text;
        }
        Map<String, Object> valueMap = asMap(rawValue);
        return firstNonBlank(valueMap.get("value"), valueMap.get("code"), valueMap.get("id"), valueMap.get("label"));
    }

    private String formatMoneyValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            BigDecimal amount = new BigDecimal(stringValue(value)).setScale(2, RoundingMode.HALF_UP);
            return "¥" + MONEY_FORMAT.format(amount);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String formatFileSize(Long fileSize) {
        if (fileSize == null || fileSize <= 0) {
            return "-";
        }
        double size = fileSize.doubleValue();
        if (size < 1024) {
            return ((long) size) + " B";
        }
        size = size / 1024;
        if (size < 1024) {
            return FILE_SIZE_FORMAT.format(size) + " KB";
        }
        return FILE_SIZE_FORMAT.format(size / 1024) + " MB";
    }

    private String joinNonBlank(String first, String second, String separator) {
        String firstValue = trimToNull(first);
        String secondValue = trimToNull(second);
        if (firstValue != null && secondValue != null) {
            return firstValue + separator + secondValue;
        }
        return defaultText(firstValue, secondValue, null);
    }

    private int normalizeSpan(Object rawValue) {
        if (rawValue instanceof Number number) {
            return number.intValue() == 2 ? 2 : 1;
        }
        return rawValue instanceof String text && Objects.equals(text.trim(), "2") ? 2 : 1;
    }

    private List<Map<String, Object>> extractBlocks(Map<String, Object> schema) {
        Object rawBlocks = schema == null ? null : schema.get("blocks");
        if (!(rawBlocks instanceof Collection<?> blocks) || blocks.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object block : blocks) {
            Map<String, Object> item = asMap(block);
            if (!item.isEmpty()) {
                result.add(item);
            }
        }
        return result;
    }

    private Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map<?, ?> rawMap) || rawMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private String joinCollection(Collection<?> values, String separator) {
        List<String> items = new ArrayList<>();
        for (Object value : values) {
            String text = trimToNull(stringValue(value));
            if (text != null) {
                items.add(text);
            }
        }
        return items.isEmpty() ? "-" : String.join(separator, items);
    }

    private boolean truthy(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = trimToNull(stringValue(value));
        return Objects.equals(text, "true") || Objects.equals(text, "1") || Objects.equals(text, "是");
    }

    private String stringValue(Object value) { return value == null ? "" : String.valueOf(value); }
    private String firstNonBlank(Object... values) { for (Object value : values) { String text = trimToNull(stringValue(value)); if (text != null) { return text; } } return null; }
    private String defaultText(String value, String fallback) { return defaultText(value, fallback, ""); }
    private String defaultText(String first, String second, String third, String fallback) { return defaultText(defaultText(first, second, null), third, fallback); }
    private String defaultText(String first, String second, String fallback) { String firstValue = trimToNull(first); if (firstValue != null) { return firstValue; } String secondValue = trimToNull(second); return secondValue != null ? secondValue : fallback; }
    private boolean notBlank(String value) { return trimToNull(value) != null; }
    private String trimToNull(String value) { if (value == null) { return null; } String trimmed = value.trim(); return trimmed.isEmpty() ? null : trimmed; }
    private String formatMoney(BigDecimal amount) { return amount == null ? "0.00" : MONEY_FORMAT.format(amount.setScale(2, RoundingMode.HALF_UP)); }

    private String escapeHtml(String value) {
        String normalized = defaultText(value, "-");
        return normalized.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;").replace("\r\n", "\n").replace("\n", "<br/>");
    }

    private File resolveFontFile() {
        File existing = cachedFontFile;
        if (existing != null && existing.isFile()) {
            return existing;
        }
        synchronized (this) {
            existing = cachedFontFile;
            if (existing != null && existing.isFile()) {
                return existing;
            }
            try {
                ClassPathResource resource = new ClassPathResource(FONT_RESOURCE_PATH);
                File tempFile = File.createTempFile("expense-print-font-", ".ttf");
                tempFile.deleteOnExit();
                try (InputStream inputStream = resource.getInputStream(); OutputStream outputStream = Files.newOutputStream(tempFile.toPath())) {
                    inputStream.transferTo(outputStream);
                }
                cachedFontFile = tempFile;
                return tempFile;
            } catch (IOException ex) {
                throw new IllegalStateException("加载 PDF 中文字体失败", ex);
            }
        }
    }

    public static final class PrintDocumentBundle {
        private final ExpenseDocumentDetailVO detail;
        private final List<ExpenseDetailInstanceDetailVO> expenseDetails;
        public PrintDocumentBundle(ExpenseDocumentDetailVO detail, List<ExpenseDetailInstanceDetailVO> expenseDetails) { this.detail = detail; this.expenseDetails = expenseDetails == null ? List.of() : List.copyOf(expenseDetails); }
        public ExpenseDocumentDetailVO getDetail() { return detail; }
        public List<ExpenseDetailInstanceDetailVO> getExpenseDetails() { return expenseDetails; }
    }

    private static final class PrintField {
        private final String label;
        private final String value;
        private final String note;
        private final boolean fullWidth;
        private final List<MiniItem> miniItems;
        private PrintField(String label, String value, String note, boolean fullWidth, List<MiniItem> miniItems) { this.label = label; this.value = value; this.note = note; this.fullWidth = fullWidth; this.miniItems = miniItems == null ? List.of() : miniItems; }
        static PrintField normal(String label, String value) { return new PrintField(label, value, null, false, List.of()); }
        static PrintField fullWidth(String label, String value) { return new PrintField(label, value, null, true, List.of()); }
        static PrintField mini(String label, String value, String note, List<MiniItem> miniItems) { return new PrintField(label, value, note, true, miniItems); }
        public String getLabel() { return label; }
        public String getValue() { return value; }
        public String getNote() { return note; }
        public boolean isFullWidth() { return fullWidth; }
        public List<MiniItem> getMiniItems() { return miniItems; }
        public boolean hasMiniItems() { return !miniItems.isEmpty(); }
    }

    private static final class MiniItem {
        private final String label;
        private final String value;
        private MiniItem(String label, String value) { this.label = label; this.value = value; }
    }

    private static final class TimelineItem {
        private final String timestamp;
        private final String title;
        private final String description;
        private TimelineItem(String timestamp, String title, String description) { this.timestamp = timestamp; this.title = title; this.description = description; }
    }
}
