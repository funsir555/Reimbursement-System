package com.finex.auth.service.impl.ocr;

public class InvoiceOcrException extends RuntimeException {

    private final String status;

    public InvoiceOcrException(String status, String message) {
        super(message);
        this.status = status;
    }

    public InvoiceOcrException(String status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
