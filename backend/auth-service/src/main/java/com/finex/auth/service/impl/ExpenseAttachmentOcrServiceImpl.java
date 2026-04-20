package com.finex.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import com.finex.auth.dto.ExpenseAttachmentOcrResultVO;
import com.finex.auth.service.ExpenseAttachmentOcrService;
import com.finex.auth.service.ExpenseAttachmentService;
import com.finex.auth.service.impl.ocr.InvoiceOcrException;
import com.finex.auth.service.impl.ocr.InvoiceOcrRequest;
import com.finex.auth.service.impl.settings.SettingsApiInterfaceDomainSupport;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Locale;

@Service
public class ExpenseAttachmentOcrServiceImpl implements ExpenseAttachmentOcrService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String JPEG_CONTENT_TYPE = "image/jpeg";
    private static final String PNG_CONTENT_TYPE = "image/png";

    private final ExpenseAttachmentService expenseAttachmentService;
    private final SettingsApiInterfaceDomainSupport settingsApiInterfaceDomainSupport;

    public ExpenseAttachmentOcrServiceImpl(
            ExpenseAttachmentService expenseAttachmentService,
            SettingsApiInterfaceDomainSupport settingsApiInterfaceDomainSupport
    ) {
        this.expenseAttachmentService = expenseAttachmentService;
        this.settingsApiInterfaceDomainSupport = settingsApiInterfaceDomainSupport;
    }

    @Override
    public ExpenseAttachmentOcrResultVO recognizeAttachment(String attachmentId) {
        SettingsApiInterfaceDomainSupport.ResolvedOcrProvider resolved = settingsApiInterfaceDomainSupport.resolveEnabledProvider();
        if (resolved == null) {
            return buildFailure("UNCONFIGURED", null, null, "未配置 OCR 服务商");
        }
        if (!resolved.isConfigured()) {
            return buildFailure("UNCONFIGURED", resolved.providerCode(), resolved.providerName(), resolved.providerName() + " OCR 待接入");
        }

        ExpenseAttachmentService.StoredExpenseAttachment attachment;
        try {
            attachment = expenseAttachmentService.loadAttachment(attachmentId);
        } catch (Exception ex) {
            return buildFailure("FAILED", resolved.providerCode(), resolved.providerName(), StrUtil.blankToDefault(ex.getMessage(), "附件不存在或读取失败"));
        }

        String contentType = normalizeContentType(attachment.contentType(), attachment.fileName());
        if (!isSupportedFile(contentType)) {
            return buildFailure("UNSUPPORTED_FILE", resolved.providerCode(), resolved.providerName(), "发票附件仅支持 PDF、PNG、JPG、JPEG 文件");
        }

        try {
            byte[] fileBytes = readBytes(attachment.resource());
            Integer pageNo = null;
            if (PDF_CONTENT_TYPE.equals(contentType)) {
                int pages = countPdfPages(fileBytes);
                if (pages > 1) {
                    return buildFailure("UNSUPPORTED_FILE", resolved.providerCode(), resolved.providerName(), "首期仅支持单页 PDF 发票");
                }
                pageNo = 1;
            }
            InvoiceOcrRequest request = new InvoiceOcrRequest(attachment.fileName(), contentType, fileBytes, pageNo);
            return resolved.runtime().provider().recognize(request, resolved.runtime().config());
        } catch (InvoiceOcrException ex) {
            return buildFailure(ex.getStatus(), resolved.providerCode(), resolved.providerName(), ex.getMessage());
        } catch (Exception ex) {
            return buildFailure("FAILED", resolved.providerCode(), resolved.providerName(), StrUtil.blankToDefault(ex.getMessage(), "OCR 识别失败，请稍后重试"));
        }
    }

    private byte[] readBytes(Resource resource) throws Exception {
        try (InputStream inputStream = resource.getInputStream()) {
            return inputStream.readAllBytes();
        }
    }

    private int countPdfPages(byte[] pdfBytes) throws Exception {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            return document.getNumberOfPages();
        }
    }

    private boolean isSupportedFile(String contentType) {
        return PDF_CONTENT_TYPE.equals(contentType)
                || PNG_CONTENT_TYPE.equals(contentType)
                || JPEG_CONTENT_TYPE.equals(contentType);
    }

    private String normalizeContentType(String contentType, String fileName) {
        String normalized = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        if (StrUtil.isNotBlank(normalized)) {
            return normalized.startsWith("image/jpg") ? JPEG_CONTENT_TYPE : normalized;
        }
        String lowerName = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".pdf")) {
            return PDF_CONTENT_TYPE;
        }
        if (lowerName.endsWith(".png")) {
            return PNG_CONTENT_TYPE;
        }
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return JPEG_CONTENT_TYPE;
        }
        return normalized;
    }

    private ExpenseAttachmentOcrResultVO buildFailure(String status, String providerCode, String providerName, String message) {
        ExpenseAttachmentOcrResultVO result = new ExpenseAttachmentOcrResultVO();
        result.setStatus(status);
        result.setProviderCode(providerCode);
        result.setProviderName(providerName);
        result.setMessage(message);
        return result;
    }
}
