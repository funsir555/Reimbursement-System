package com.finex.auth.service.impl.ocr;

public record InvoiceOcrRequest(
        String fileName,
        String contentType,
        byte[] fileBytes,
        Integer pageNo
) {
}
