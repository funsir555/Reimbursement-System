package com.finex.auth.dto;

import lombok.Data;

@Data
public class ExpenseDocumentRelationBindingVO {

    private String direction;

    private String fieldKey;

    private String documentCode;

    private String documentTitle;

    private String templateType;

    private String templateTypeLabel;

    private String templateName;

    private String status;

    private String statusLabel;

    private String submitterName;
}
