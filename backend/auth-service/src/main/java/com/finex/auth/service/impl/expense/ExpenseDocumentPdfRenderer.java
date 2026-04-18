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
    private static final List<String> DETAIL_AMOUNT_FIELD_KEYS = List.of("invoiceAmount", "actualPaymentAmount", "amount", "detailAmount");
    private static final List<String> DETAIL_VERIFY_FIELD_KEYS = List.of("invoiceVerifyStatus", "verifyStatus", "invoiceVerificationResult", "invoiceCheckResult");
    private static final List<String> DETAIL_EXCEPTION_FIELD_KEYS = List.of("abnormalDisplay", "exceptionDisplay", "invoiceExceptionDisplay", "exceptionReason");

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
        StringBuilder html = new StringBuilder(24 * 1024);
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8' />");
        html.append("<style>").append(buildStyles(orientation)).append("</style>");
        html.append("</head><body>");
        for (int index = 0; index < documents.size(); index++) {
            appendDocument(html, documents.get(index), index > 0);
        }
        html.append("</body></html>");
        return html.toString();
    }

    private String buildStyles(ExpenseDocumentPrintOrientation orientation) {
        return String.join("\n",
                "@page {",
                "  size: A4 " + orientation.toCssSize() + ";",
                "  margin: 2.5cm 0.5cm 1cm 0.5cm;",
                "  @bottom-center {",
                "    content: '第 ' counter(page) ' 页 共 ' counter(pages) ' 页';",
                "    color: #64748b;",
                "    font-size: 10pt;",
                "    padding-top: 0.1cm;",
                "  }",
                "}",
                "* { box-sizing: border-box; }",
                "html, body { margin: 0; padding: 0; }",
                "body { font-family: '" + FONT_FAMILY + "'; color: #0f172a; font-size: 10pt; line-height: 1.55; }",
                ".document { page-break-inside: auto; }",
                ".document--break { page-break-before: always; }",
                ".header-card, .section-card, .detail-card { border: 1px solid #d9e2ec; border-radius: 14px; background: #fff; }",
                ".header-card { padding: 16px 18px; }",
                ".header-table, .summary-table, .field-table, .detail-field-table, .bank-table, .timeline-table { width: 100%; border-collapse: separate; border-spacing: 0; }",
                ".header-title { margin: 0; font-size: 22pt; font-weight: 700; line-height: 1.2; }",
                ".header-subtitle { margin-top: 6pt; color: #475569; font-size: 10pt; }",
                ".amount-box { text-align: right; border: 1px solid #d9e2ec; border-radius: 14px; background: #f8fafc; padding: 10pt 12pt; }",
                ".amount-label { display: block; font-size: 8.5pt; color: #64748b; }",
                ".amount-value { display: block; margin-top: 6pt; font-size: 20pt; font-weight: 700; }",
                ".section { margin-top: 14pt; page-break-inside: avoid; }",
                ".section-title { margin: 0 0 8pt; font-size: 13pt; font-weight: 700; }",
                ".field-table { border-spacing: 0 6pt; }",
                ".field-table td { width: 50%; vertical-align: top; padding-right: 6pt; }",
                ".field-table td.full { width: 100%; padding-right: 0; }",
                ".field-box { border: 1px solid #d9e2ec; border-radius: 12px; background: #fff; padding: 8pt 10pt; min-height: 40pt; }",
                ".label { display: inline-block; margin-right: 6pt; color: #64748b; font-size: 8.5pt; }",
                ".value { display: block; margin-top: 4pt; color: #0f172a; font-size: 10pt; font-weight: 600; white-space: pre-wrap; word-break: break-word; }",
                ".value--normal { font-weight: 500; }",
                ".mini-table { width: 100%; border-spacing: 0 4pt; margin-top: 4pt; }",
                ".mini-table td { width: 33.33%; vertical-align: top; }",
                ".mini-box { border: 1px solid #e2e8f0; border-radius: 10px; background: #f8fafc; padding: 7pt 8pt; }",
                ".mini-label { display: block; color: #64748b; font-size: 8pt; }",
                ".mini-value { display: block; margin-top: 3pt; font-size: 9.5pt; font-weight: 600; word-break: break-word; }",
                ".summary-table { border-spacing: 0 6pt; }",
                ".summary-table td { width: 25%; vertical-align: top; padding-right: 6pt; }",
                ".summary-box { border: 1px solid #d9e2ec; border-radius: 12px; background: #f8fafc; padding: 8pt 10pt; min-height: 38pt; }",
                ".summary-label { display: block; color: #64748b; font-size: 8.5pt; }",
                ".summary-value { display: block; margin-top: 4pt; font-size: 10pt; font-weight: 700; }",
                ".detail-card { padding: 12pt; margin-top: 10pt; page-break-inside: avoid; }",
                ".detail-head { width: 100%; border-collapse: collapse; margin-bottom: 8pt; }",
                ".detail-title { font-size: 12pt; font-weight: 700; }",
                ".detail-meta { margin-top: 4pt; color: #64748b; font-size: 8.5pt; }",
                ".detail-tag { display: inline-block; border: 1px solid #cbd5e1; border-radius: 999px; padding: 2pt 8pt; font-size: 8pt; color: #334155; background: #f8fafc; margin-left: 6pt; }",
                ".detail-tag--warning { border-color: #f59e0b; color: #92400e; background: #fef3c7; }",
                ".detail-summary-table { border-spacing: 0 6pt; }",
                ".detail-summary-table td { width: 25%; vertical-align: top; padding-right: 6pt; }",
                ".detail-field-table { border-spacing: 0 5pt; margin-top: 8pt; }",
                ".detail-field-table td { width: 50%; vertical-align: top; padding-right: 6pt; }",
                ".bank-table { border-spacing: 0 6pt; }",
                ".bank-table td { width: 50%; vertical-align: top; padding-right: 6pt; }",
                ".timeline-table { border-spacing: 0 6pt; }",
                ".timeline-time { width: 120pt; color: #64748b; font-size: 8.5pt; vertical-align: top; padding-top: 2pt; }",
                ".timeline-content { border-left: 2px solid #bfdbfe; padding-left: 10pt; }",
                ".timeline-title { font-size: 10pt; font-weight: 700; }",
                ".timeline-desc { margin-top: 3pt; color: #475569; font-size: 8.8pt; white-space: pre-wrap; word-break: break-word; }",
                ".empty-box { border: 1px dashed #cbd5e1; border-radius: 12px; background: #f8fafc; padding: 10pt; color: #64748b; }"
        );
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
        appendExpenseDetailsSection(html, bundle.getExpenseDetails());
        appendBankSection(html, detail.getBankPayment(), detail.getBankReceipts());
        appendTimelineSection(html, detail);
        html.append("</section>");
    }

    private void appendHeader(StringBuilder html, ExpenseDocumentDetailVO detail) {
        html.append("<div class='header-card'><table class='header-table'><tr><td>");
        html.append("<div class='header-title'>").append(escapeHtml(defaultText(detail.getDocumentTitle(), detail.getDocumentCode(), "报销单"))).append("</div>");
        html.append("<div class='header-subtitle'>单据编号：").append(escapeHtml(defaultText(detail.getDocumentCode(), "-"))).append("　/　模板：").append(escapeHtml(defaultText(detail.getTemplateName(), "-"))).append("</div>");
        html.append("</td><td style='width: 170pt;'><div class='amount-box'><span class='amount-label'>金额</span><span class='amount-value'>￥ ").append(escapeHtml(formatMoney(detail.getTotalAmount()))).append("</span></div></td></tr></table></div>");
    }

    private void appendSummarySection(StringBuilder html, ExpenseDocumentDetailVO detail) {
        html.append("<section class='section'><h2 class='section-title'>单据摘要</h2>");
        List<PrintField> fields = List.of(
                PrintField.normal("单据状态", defaultText(detail.getStatusLabel(), detail.getStatus(), "-")),
                PrintField.normal("提单人", defaultText(detail.getSubmitterName(), "-")),
                PrintField.normal("提交时间", defaultText(detail.getSubmittedAt(), "-")),
                PrintField.normal("当前节点", defaultText(detail.getCurrentNodeName(), "-")),
                PrintField.normal("模板类型", defaultText(detail.getTemplateType(), "-")),
                PrintField.normal("完成时间", defaultText(detail.getFinishedAt(), "-"))
        );
        appendFieldTable(html, fields);
        if (notBlank(detail.getDocumentReason())) {
            appendFieldTable(html, List.of(PrintField.fullWidth("事由", detail.getDocumentReason())));
        }
        html.append("</section>");
    }

    private void appendFormSection(StringBuilder html, ExpenseDocumentDetailVO detail) {
        html.append("<section class='section'><h2 class='section-title'>单据表单</h2>");
        List<PrintField> fields = buildDocumentFields(
                detail.getFormSchemaSnapshot(),
                detail.getFormData(),
                buildOptionMap(detail.getCompanyOptions()),
                buildOptionMap(detail.getDepartmentOptions())
        );
        if (fields.isEmpty()) {
            html.append("<div class='empty-box'>暂无表单数据</div>");
        } else {
            appendFieldTable(html, fields);
        }
        html.append("</section>");
    }

    private void appendExpenseDetailsSection(StringBuilder html, List<ExpenseDetailInstanceDetailVO> expenseDetails) {
        html.append("<section class='section'><h2 class='section-title'>费用明细</h2>");
        if (expenseDetails == null || expenseDetails.isEmpty()) {
            html.append("<div class='empty-box'>暂无费用明细</div></section>");
            return;
        }
        for (ExpenseDetailInstanceDetailVO item : expenseDetails) {
            html.append("<div class='detail-card'>");
            html.append("<table class='detail-head'><tr><td>");
            html.append("<div class='detail-title'>").append(escapeHtml(defaultText(item.getDetailTitle(), item.getDetailNo(), "费用明细"))).append("</div>");
            html.append("<div class='detail-meta'>明细编号：").append(escapeHtml(defaultText(item.getDetailNo(), "-"))).append("　/　更新时间：").append(escapeHtml(defaultText(item.getUpdatedAt(), item.getCreatedAt(), "-"))).append("</div>");
            html.append("</td><td style='text-align: right;'>");
            html.append("<span class='detail-tag'>").append(escapeHtml(defaultText(item.getDetailTypeLabel(), item.getDetailType(), "费用明细"))).append("</span>");
            if (notBlank(item.getEnterpriseModeLabel())) {
                html.append("<span class='detail-tag detail-tag--warning'>").append(escapeHtml(item.getEnterpriseModeLabel())).append("</span>");
            }
            html.append("</td></tr></table>");

            appendDetailSummaryTable(html, item);

            List<PrintField> detailFields = buildDetailFields(item);
            if (!detailFields.isEmpty()) {
                appendDetailFieldTable(html, detailFields);
            }
            html.append("</div>");
        }
        html.append("</section>");
    }

    private void appendDetailSummaryTable(StringBuilder html, ExpenseDetailInstanceDetailVO detail) {
        html.append("<table class='detail-summary-table summary-table'><tr>");
        appendSummaryCell(html, "费用类型", resolveExpenseTypeLabel(detail));
        appendSummaryCell(html, "金额", resolveDetailAmount(detail));
        appendSummaryCell(html, "发票验真结果", resolveFirstText(detail.getFormData(), DETAIL_VERIFY_FIELD_KEYS, "-"));
        appendSummaryCell(html, "异常显示", resolveFirstText(detail.getFormData(), DETAIL_EXCEPTION_FIELD_KEYS, "-"));
        html.append("</tr></table>");
    }

    private void appendSummaryCell(StringBuilder html, String label, String value) {
        html.append("<td><div class='summary-box'><span class='summary-label'>")
                .append(escapeHtml(label))
                .append("</span><span class='summary-value'>")
                .append(escapeHtml(defaultText(value, "-")))
                .append("</span></div></td>");
    }

    private void appendBankSection(StringBuilder html, ExpenseDocumentBankPaymentVO bankPayment, List<ExpenseDocumentBankReceiptVO> bankReceipts) {
        boolean hasPayment = bankPayment != null;
        boolean hasReceipts = bankReceipts != null && !bankReceipts.isEmpty();
        if (!hasPayment && !hasReceipts) {
            return;
        }
        html.append("<section class='section'><h2 class='section-title'>支付与回单</h2>");
        if (hasPayment) {
            List<PrintField> fields = List.of(
                    PrintField.normal("支付状态", defaultText(bankPayment.getPaymentStatusLabel(), bankPayment.getPaymentStatusCode(), "-")),
                    PrintField.normal("直连账户", defaultText(bankPayment.getCompanyBankAccountName(), "-")),
                    PrintField.normal("回单状态", defaultText(bankPayment.getReceiptStatusLabel(), "-")),
                    PrintField.normal("支付时间", defaultText(bankPayment.getPaidAt(), "-")),
                    PrintField.normal("银行流水号", defaultText(bankPayment.getBankFlowNo(), "-")),
                    PrintField.normal("支付方式", bankPayment.isManualPaid() ? "手动支付" : "银行回调")
            );
            appendFieldTable(html, fields);
        }
        html.append("<h3 class='section-title' style='font-size: 11pt; margin-top: 10pt;'>银行回单</h3>");
        if (!hasReceipts) {
            html.append("<div class='empty-box'>暂无银行回单</div>");
        } else {
            html.append("<table class='bank-table'><tbody>");
            List<ExpenseDocumentBankReceiptVO> receipts = bankReceipts == null ? List.of() : bankReceipts;
            for (int index = 0; index < receipts.size(); index += 2) {
                html.append("<tr>");
                appendReceiptCell(html, receipts.get(index));
                if (index + 1 < receipts.size()) {
                    appendReceiptCell(html, receipts.get(index + 1));
                } else {
                    html.append("<td></td>");
                }
                html.append("</tr>");
            }
            html.append("</tbody></table>");
        }
        html.append("</section>");
    }

    private void appendReceiptCell(StringBuilder html, ExpenseDocumentBankReceiptVO receipt) {
        html.append("<td><div class='field-box'><span class='label'>回单文件</span><span class='value'>")
                .append(escapeHtml(defaultText(receipt.getFileName(), "-")))
                .append("</span><span class='value value--normal'>")
                .append(escapeHtml(defaultText(receipt.getReceivedAt(), "-")))
                .append("　/　")
                .append(escapeHtml(defaultText(receipt.getContentType(), "-")))
                .append("</span></div></td>");
    }

    private void appendTimelineSection(StringBuilder html, ExpenseDocumentDetailVO detail) {
        html.append("<section class='section'><h2 class='section-title'>审批轨迹</h2>");
        List<TimelineItem> items = buildTimelineItems(detail);
        if (items.isEmpty()) {
            html.append("<div class='empty-box'>暂无审批轨迹</div></section>");
            return;
        }
        html.append("<table class='timeline-table'><tbody>");
        for (TimelineItem item : items) {
            html.append("<tr><td class='timeline-time'>")
                    .append(escapeHtml(defaultText(item.timestamp, "-")))
                    .append("</td><td class='timeline-content'><div class='timeline-title'>")
                    .append(escapeHtml(defaultText(item.title, "-")))
                    .append("</div>");
            if (notBlank(item.description)) {
                html.append("<div class='timeline-desc'>").append(escapeHtml(item.description)).append("</div>");
            }
            html.append("</td></tr>");
        }
        html.append("</tbody></table></section>");
    }

    private List<PrintField> buildDocumentFields(
            Map<String, Object> schema,
            Map<String, Object> formData,
            Map<String, String> companyMap,
            Map<String, String> departmentMap
    ) {
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
            fields.add(fullWidth ? PrintField.fullWidth(defaultText(label, fieldKey, "字段"), displayValue)
                    : PrintField.normal(defaultText(label, fieldKey, "字段"), displayValue));
        }
        return fields;
    }

    private List<PrintField> buildDetailFields(ExpenseDetailInstanceDetailVO detail) {
        Map<String, Object> formData = detail.getFormData() == null ? Collections.emptyMap() : detail.getFormData();
        List<PrintField> fields = new ArrayList<>();
        Set<String> skippedKeys = new LinkedHashSet<>();
        skippedKeys.add("expenseTypeCode");
        skippedKeys.addAll(DETAIL_AMOUNT_FIELD_KEYS);
        skippedKeys.addAll(DETAIL_VERIFY_FIELD_KEYS);
        skippedKeys.addAll(DETAIL_EXCEPTION_FIELD_KEYS);

        for (Map<String, Object> block : extractBlocks(detail.getSchemaSnapshot())) {
            if (!isDetailBlockVisible(block, detail)) {
                continue;
            }
            String label = trimToNull(stringValue(block.get("label")));
            String fieldKey = trimToNull(stringValue(block.get("fieldKey")));
            if (fieldKey == null || skippedKeys.contains(fieldKey)) {
                continue;
            }
            Map<String, Object> props = asMap(block.get("props"));
            String controlType = trimToNull(stringValue(props.get("controlType")));
            if (Objects.equals(controlType, "SECTION")) {
                continue;
            }
            String componentCode = trimToNull(stringValue(props.get("componentCode")));
            Object rawValue = formData.get(fieldKey);
            String value = resolveBlockDisplayValue(rawValue, props, Collections.emptyMap(), Collections.emptyMap(), componentCode, controlType);
            fields.add(PrintField.normal(defaultText(label, fieldKey, "字段"), value));
        }
        return fields;
    }

    private boolean isDetailBlockVisible(Map<String, Object> block, ExpenseDetailInstanceDetailVO detail) {
        Map<String, Object> props = asMap(block.get("props"));
        Object rawVisibleSceneModes = props.get("visibleSceneModes");
        if (!(rawVisibleSceneModes instanceof Collection<?> visibleSceneModes) || visibleSceneModes.isEmpty()) {
            return true;
        }
        String currentMode = trimToNull(detail.getBusinessSceneMode());
        if (currentMode == null) {
            return false;
        }
        for (Object visibleSceneMode : visibleSceneModes) {
            if (Objects.equals(currentMode, trimToNull(stringValue(visibleSceneMode)))) {
                return true;
            }
        }
        return false;
    }

    private void appendFieldTable(StringBuilder html, List<PrintField> fields) {
        if (fields == null || fields.isEmpty()) {
            return;
        }
        html.append("<table class='field-table'><tbody>");
        List<PrintField> rowBuffer = new ArrayList<>(2);
        for (PrintField field : fields) {
            if (field == null) {
                continue;
            }
            if (field.isFullWidth()) {
                if (!rowBuffer.isEmpty()) {
                    appendFieldRow(html, rowBuffer);
                    rowBuffer.clear();
                }
                appendFieldRow(html, List.of(field));
                continue;
            }
            rowBuffer.add(field);
            if (rowBuffer.size() == 2) {
                appendFieldRow(html, rowBuffer);
                rowBuffer = new ArrayList<>(2);
            }
        }
        if (!rowBuffer.isEmpty()) {
            appendFieldRow(html, rowBuffer);
        }
        html.append("</tbody></table>");
    }

    private void appendDetailFieldTable(StringBuilder html, List<PrintField> fields) {
        html.append("<table class='detail-field-table'><tbody>");
        List<PrintField> rowBuffer = new ArrayList<>(2);
        for (PrintField field : fields) {
            rowBuffer.add(field);
            if (rowBuffer.size() == 2) {
                appendDetailFieldRow(html, rowBuffer);
                rowBuffer = new ArrayList<>(2);
            }
        }
        if (!rowBuffer.isEmpty()) {
            appendDetailFieldRow(html, rowBuffer);
        }
        html.append("</tbody></table>");
    }

    private void appendFieldRow(StringBuilder html, List<PrintField> fields) {
        html.append("<tr>");
        if (fields.size() == 1) {
            appendFieldCell(html, fields.get(0), true);
        } else {
            appendFieldCell(html, fields.get(0), false);
            appendFieldCell(html, fields.get(1), false);
        }
        html.append("</tr>");
    }

    private void appendDetailFieldRow(StringBuilder html, List<PrintField> fields) {
        html.append("<tr>");
        appendDetailFieldCell(html, fields.get(0));
        if (fields.size() > 1) {
            appendDetailFieldCell(html, fields.get(1));
        } else {
            html.append("<td></td>");
        }
        html.append("</tr>");
    }

    private void appendFieldCell(StringBuilder html, PrintField field, boolean fullWidth) {
        html.append("<td class='");
        if (fullWidth || field.isFullWidth()) {
            html.append("full");
        }
        html.append("'><div class='field-box'><span class='label'>")
                .append(escapeHtml(field.getLabel()))
                .append("</span>");
        if (field.hasMiniItems()) {
            html.append("<span class='value'>")
                    .append(escapeHtml(defaultText(field.getValue(), "-")))
                    .append("</span><table class='mini-table'><tr>");
            for (MiniItem item : field.getMiniItems()) {
                html.append("<td><div class='mini-box'><span class='mini-label'>")
                        .append(escapeHtml(item.label))
                        .append("</span><span class='mini-value'>")
                        .append(escapeHtml(defaultText(item.value, "-")))
                        .append("</span></div></td>");
            }
            html.append("</tr></table>");
        } else {
            html.append("<span class='value value--normal'>")
                    .append(escapeHtml(defaultText(field.getValue(), "-")))
                    .append("</span>");
        }
        html.append("</div></td>");
    }

    private void appendDetailFieldCell(StringBuilder html, PrintField field) {
        html.append("<td><div class='field-box'><span class='label'>")
                .append(escapeHtml(field.getLabel()))
                .append("</span><span class='value value--normal'>")
                .append(escapeHtml(defaultText(field.getValue(), "-")))
                .append("</span></div></td>");
    }

    private PrintField buildPayeeAccountField(String label, Object rawValue) {
        Map<String, Object> accountValue = asMap(rawValue);
        List<MiniItem> items = List.of(
                new MiniItem("账户名称", firstNonBlank(accountValue.get("ownerName"), accountValue.get("accountName"), accountValue.get("label"), accountValue.get("value"))),
                new MiniItem("银行账号", firstNonBlank(accountValue.get("accountNoMasked"), accountValue.get("accountNo"))),
                new MiniItem("开户行", firstNonBlank(accountValue.get("bankName"), accountValue.get("bankBranchName")))
        );
        return PrintField.mini(label, defaultText(stringValue(rawValue), "已按账户信息展示"), items);
    }

    private String resolveBlockDisplayValue(
            Object rawValue,
            Map<String, Object> props,
            Map<String, String> companyMap,
            Map<String, String> departmentMap,
            String componentCode,
            String controlType
    ) {
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
                String fieldKey = trimToNull(stringValue(block.get("fieldKey")));
                if (!Objects.equals(fieldKey, "expenseTypeCode")) {
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
            Object value = detail.getFormData() == null ? null : detail.getFormData().get(fieldKey);
            String amount = formatMoneyValue(value);
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
            String assigneeName = defaultText(task.getAssigneeName(), "未分配处理人");
            String dedupeKey = defaultText(task.getNodeKey(), "pending") + "::" + assigneeName;
            if (pendingItems.containsKey(dedupeKey)) {
                continue;
            }
            String title = Objects.equals(trimToNull(task.getNodeType()), "PAYMENT")
                    ? assigneeName + " 待支付"
                    : defaultText(task.getNodeName(), "节点") + " " + assigneeName + " 审批中";
            pendingItems.put(dedupeKey, new TimelineItem(defaultText(task.getCreatedAt(), ""), title, ""));
        }
        items.addAll(pendingItems.values());
        return items;
    }

    private boolean shouldDisplayTimelineLog(String actionType) {
        return Set.of("SUBMIT", "RECALL", "RESUBMIT", "APPROVE", "REJECT", "MODIFY", "COMMENT", "TRANSFER", "ADD_SIGN", "PAYMENT_START", "PAYMENT_COMPLETE", "PAYMENT_EXCEPTION", "FINISH", "EXCEPTION")
                .contains(defaultText(actionType, ""));
    }

    private String buildTimelineTitle(ExpenseApprovalLogVO log, ExpenseDocumentDetailVO detail) {
        String actorName = defaultText(log.getActorName(), "审批人");
        String nodeName = defaultText(log.getNodeName(), "节点");
        return switch (defaultText(log.getActionType(), "")) {
            case "SUBMIT" -> defaultText(detail.getSubmitterName(), log.getActorName(), "提单人") + " 提交单据";
            case "RECALL" -> actorName + " 召回单据";
            case "RESUBMIT" -> actorName + " 重新提交";
            case "APPROVE" -> nodeName + " " + actorName + " 审批通过";
            case "REJECT" -> nodeName + " " + actorName + " 审批驳回";
            case "MODIFY" -> actorName + " 修改单据";
            case "COMMENT" -> actorName + " 发表评论";
            case "TRANSFER" -> actorName + " 转交审批";
            case "ADD_SIGN" -> actorName + " 发起加签";
            case "PAYMENT_START" -> actorName + " 发起支付";
            case "PAYMENT_COMPLETE" -> actorName + " 确认已支付";
            case "PAYMENT_EXCEPTION" -> actorName + " 标记支付异常";
            case "FINISH" -> "审批完成";
            case "EXCEPTION" -> "流程异常";
            default -> defaultText(log.getActionType(), "操作记录");
        };
    }

    private String buildTimelineDescription(ExpenseApprovalLogVO log) {
        String actionType = defaultText(log.getActionType(), "");
        if (Objects.equals(actionType, "COMMENT")) {
            return defaultText(stringValue(asMap(log.getPayload()).get("comment")), log.getActionComment(), "");
        }
        if (Set.of("SUBMIT", "APPROVE", "REJECT", "PAYMENT_COMPLETE", "PAYMENT_EXCEPTION").contains(actionType)) {
            String comment = defaultText(log.getActionComment(), "");
            return isRedundantTimelineComment(actionType, comment) ? "" : comment;
        }
        List<String> parts = new ArrayList<>();
        if (notBlank(log.getNodeName()) && !Set.of("APPROVE", "REJECT", "RECALL", "RESUBMIT", "COMMENT").contains(actionType)) {
            parts.add(log.getNodeName());
        }
        if (notBlank(log.getActionComment())) {
            parts.add(log.getActionComment());
        }
        String targetUserName = stringValue(asMap(log.getPayload()).get("targetUserName"));
        if (Objects.equals(actionType, "TRANSFER") && notBlank(targetUserName)) {
            parts.add("转交给 " + targetUserName);
        }
        if (Objects.equals(actionType, "ADD_SIGN") && notBlank(targetUserName)) {
            parts.add("加签给 " + targetUserName);
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
            Map<String, Object> optionMapValue = asMap(option);
            String value = trimToNull(stringValue(optionMapValue.get("value")));
            String label = trimToNull(stringValue(optionMapValue.get("label")));
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
        if (key != null && labelMap.containsKey(key)) {
            return labelMap.get(key);
        }
        return formatGenericValue(rawValue);
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
            BigDecimal amount = new BigDecimal(stringValue(value));
            amount = amount.setScale(2, RoundingMode.HALF_UP);
            return "￥ " + MONEY_FORMAT.format(amount);
        } catch (Exception ignored) {
            return null;
        }
    }

    private int normalizeSpan(Object rawValue) {
        if (rawValue instanceof Number number) {
            return number.intValue() == 2 ? 2 : 1;
        }
        if (rawValue instanceof String text && Objects.equals(text.trim(), "2")) {
            return 2;
        }
        return 1;
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

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            String text = trimToNull(stringValue(value));
            if (text != null) {
                return text;
            }
        }
        return null;
    }

    private String defaultText(String value, String fallback) {
        return defaultText(value, fallback, "");
    }

    private String defaultText(String first, String second, String fallback) {
        String firstValue = trimToNull(first);
        if (firstValue != null) {
            return firstValue;
        }
        String secondValue = trimToNull(second);
        if (secondValue != null) {
            return secondValue;
        }
        return fallback;
    }

    private boolean notBlank(String value) {
        return trimToNull(value) != null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return MONEY_FORMAT.format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    private String escapeHtml(String value) {
        String normalized = defaultText(value, "-");
        return normalized
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\r\n", "\n")
                .replace("\n", "<br/>");
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

        public PrintDocumentBundle(ExpenseDocumentDetailVO detail, List<ExpenseDetailInstanceDetailVO> expenseDetails) {
            this.detail = detail;
            this.expenseDetails = expenseDetails == null ? List.of() : List.copyOf(expenseDetails);
        }

        public ExpenseDocumentDetailVO getDetail() {
            return detail;
        }

        public List<ExpenseDetailInstanceDetailVO> getExpenseDetails() {
            return expenseDetails;
        }
    }

    private static final class PrintField {
        private final String label;
        private final String value;
        private final boolean fullWidth;
        private final List<MiniItem> miniItems;

        private PrintField(String label, String value, boolean fullWidth, List<MiniItem> miniItems) {
            this.label = label;
            this.value = value;
            this.fullWidth = fullWidth;
            this.miniItems = miniItems == null ? List.of() : miniItems;
        }

        static PrintField normal(String label, String value) {
            return new PrintField(label, value, false, List.of());
        }

        static PrintField fullWidth(String label, String value) {
            return new PrintField(label, value, true, List.of());
        }

        static PrintField mini(String label, String value, List<MiniItem> miniItems) {
            return new PrintField(label, value, true, miniItems);
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }

        public boolean isFullWidth() {
            return fullWidth;
        }

        public List<MiniItem> getMiniItems() {
            return miniItems;
        }

        public boolean hasMiniItems() {
            return !miniItems.isEmpty();
        }
    }

    private static final class MiniItem {
        private final String label;
        private final String value;

        private MiniItem(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    private static final class TimelineItem {
        private final String timestamp;
        private final String title;
        private final String description;

        private TimelineItem(String timestamp, String title, String description) {
            this.timestamp = timestamp;
            this.title = title;
            this.description = description;
        }
    }
}
