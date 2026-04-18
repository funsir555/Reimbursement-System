package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseDetailInstanceDetailVO;
import com.finex.auth.dto.ExpenseDocumentDetailVO;
import com.finex.auth.service.impl.ExpenseDocumentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExpenseDocumentPrintService {

    private final ExpenseDocumentQueryService expenseDocumentQueryService;
    private final ExpenseDocumentPdfRenderer expenseDocumentPdfRenderer;

    public ExpensePrintPdfResult generateSinglePdf(Long userId, String documentCode, boolean allowCrossView, ExpenseDocumentPrintOrientation orientation) {
        ExpenseDocumentPdfRenderer.PrintDocumentBundle bundle = loadBundle(userId, documentCode, allowCrossView);
        byte[] content = expenseDocumentPdfRenderer.renderDocuments(List.of(bundle), orientation);
        return new ExpensePrintPdfResult(content, buildSingleFileName(documentCode));
    }

    public ExpensePrintPdfResult generateBatchPdf(Long userId, List<String> documentCodes, boolean allowCrossView, ExpenseDocumentPrintOrientation orientation) {
        List<String> normalizedCodes = normalizeDocumentCodes(documentCodes);
        if (normalizedCodes.isEmpty()) {
            throw new IllegalArgumentException("缺少可打印的单据编号");
        }
        List<ExpenseDocumentPdfRenderer.PrintDocumentBundle> bundles = new ArrayList<>();
        for (String documentCode : normalizedCodes) {
            bundles.add(loadBundle(userId, documentCode, allowCrossView));
        }
        byte[] content = expenseDocumentPdfRenderer.renderDocuments(bundles, orientation);
        return new ExpensePrintPdfResult(content, buildBatchFileName(normalizedCodes));
    }

    private ExpenseDocumentPdfRenderer.PrintDocumentBundle loadBundle(Long userId, String documentCode, boolean allowCrossView) {
        ExpenseDocumentDetailVO detail = expenseDocumentQueryService.getDocumentDetail(userId, documentCode, allowCrossView);
        List<ExpenseDetailInstanceDetailVO> expenseDetails = new ArrayList<>();
        if (detail.getExpenseDetails() != null) {
            for (var summary : detail.getExpenseDetails()) {
                expenseDetails.add(expenseDocumentQueryService.getExpenseDetail(userId, documentCode, summary.getDetailNo(), allowCrossView));
            }
        }
        return new ExpenseDocumentPdfRenderer.PrintDocumentBundle(detail, expenseDetails);
    }

    private List<String> normalizeDocumentCodes(List<String> documentCodes) {
        if (documentCodes == null || documentCodes.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String documentCode : documentCodes) {
            String value = trimToNull(documentCode);
            if (value != null) {
                normalized.add(value);
            }
        }
        return List.copyOf(normalized);
    }

    private String buildSingleFileName(String documentCode) {
        return "expense-document-" + sanitizeFilePart(documentCode) + ".pdf";
    }

    private String buildBatchFileName(List<String> documentCodes) {
        if (documentCodes.size() == 1) {
            return buildSingleFileName(documentCodes.get(0));
        }
        return "expense-documents-batch-" + documentCodes.size() + ".pdf";
    }

    private String sanitizeFilePart(String rawValue) {
        String value = trimToNull(rawValue);
        if (value == null) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9._-]+", "_");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static final class ExpensePrintPdfResult {
        private final byte[] content;
        private final String fileName;

        public ExpensePrintPdfResult(byte[] content, String fileName) {
            this.content = content;
            this.fileName = fileName;
        }

        public byte[] getContent() {
            return content;
        }

        public String getFileName() {
            return fileName;
        }
    }
}
