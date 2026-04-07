package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FinanceContextMetaVO {

    private List<FinanceContextCompanyOptionVO> companyOptions = new ArrayList<>();

    private String currentUserCompanyId;

    private String defaultCompanyId;
}
