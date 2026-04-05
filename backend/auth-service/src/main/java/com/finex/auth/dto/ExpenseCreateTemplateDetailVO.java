package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ExpenseCreateTemplateDetailVO {

    private String templateCode;
    private String templateName;
    private String templateType;
    private String templateTypeLabel;
    private String categoryCode;
    private String templateDescription;
    private String formDesignCode;
    private String approvalFlowCode;
    private String flowName;
    private String formName;
    private Map<String, Object> schema = new LinkedHashMap<>();
    private String expenseDetailDesignCode;
    private String expenseDetailDesignName;
    private String expenseDetailType;
    private String expenseDetailTypeLabel;
    private String expenseDetailModeDefault;
    private Map<String, Object> expenseDetailSchema = new LinkedHashMap<>();
    private List<ProcessCustomArchiveDetailVO> sharedArchives = new ArrayList<>();
    private List<ProcessCustomArchiveDetailVO> expenseDetailSharedArchives = new ArrayList<>();
    private List<ProcessFormOptionVO> companyOptions = new ArrayList<>();
    private List<ProcessFormOptionVO> departmentOptions = new ArrayList<>();
    private List<ProcessFormOptionVO> expenseTypeOptions = new ArrayList<>();
    private Map<String, String> expenseTypeInvoiceFreeModeMap = new LinkedHashMap<>();
    private String currentUserDeptId;
    private String currentUserDeptName;
}
