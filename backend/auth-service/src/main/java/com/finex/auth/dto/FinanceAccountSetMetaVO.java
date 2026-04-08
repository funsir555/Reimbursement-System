package com.finex.auth.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class FinanceAccountSetMetaVO {

    private List<FinanceContextCompanyOptionVO> companyOptions = new ArrayList<>();

    private List<FinanceAccountSetOptionVO> supervisorOptions = new ArrayList<>();

    private List<FinanceAccountSetTemplateSummaryVO> templateOptions = new ArrayList<>();

    private List<FinanceAccountSetReferenceOptionVO> referenceOptions = new ArrayList<>();

    private String defaultSubjectCodeScheme;
}
