package com.finex.auth.service.impl.ocr;

import com.finex.auth.dto.ExpenseAttachmentOcrResultVO;

public interface CloudInvoiceOcrProvider {

    String getProviderCode();

    ExpenseAttachmentOcrResultVO recognize(InvoiceOcrRequest request, OcrProviderRuntimeConfig config);
}
