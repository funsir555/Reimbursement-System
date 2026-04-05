package com.finex.auth.dto;

import lombok.Data;

@Data
public class ExpenseVoucherSubjectMappingVO {

    private Long id;
    private String companyId;
    private String companyName;
    private String templateCode;
    private String templateName;
    private String expenseTypeCode;
    private String expenseTypeName;
    private String debitAccountCode;
    private String debitAccountName;
    private Boolean enabled;
    private String updatedAt;
}
