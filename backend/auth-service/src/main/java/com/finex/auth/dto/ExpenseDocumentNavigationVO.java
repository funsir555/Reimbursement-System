package com.finex.auth.dto;

import lombok.Data;

@Data
public class ExpenseDocumentNavigationVO {

    private String prevDocumentCode;

    private String nextDocumentCode;
}
