package com.finex.auth.dto;

import lombok.Data;

@Data
public class ExpenseDocumentBankReceiptVO {

    private String attachmentId;

    private String fileName;

    private String contentType;

    private Long fileSize;

    private String previewUrl;

    private String receivedAt;
}
