package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CompanyVO {

    private String companyId;

    private String companyCode;

    private String companyName;

    private String invoiceTitle;

    private String taxNo;

    private String bankName;

    private String bankAccountName;

    private String bankAccountNo;

    private Integer status;

    private List<CompanyVO> children = new ArrayList<>();
}
