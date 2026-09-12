package com.finex.auth.dto;

import lombok.Data;

@Data
public class FinanceAccountSubjectAuxiliaryVO {

    private String subjectCode;
    private String subjectName;
    private Boolean bperson;
    private Boolean bsup;
    private Boolean bdept;
    private Boolean bitem;
    private Boolean bcus;
}
