package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ExpenseDocumentDetailVO {

    private String documentCode;

    private String documentTitle;

    private String documentReason;

    private String status;

    private String statusLabel;

    @MoneyValue
    private BigDecimal totalAmount;

    private Long submitterUserId;

    private String submitterName;

    private String templateName;

    private String templateType;

    private String currentNodeKey;

    private String currentNodeName;

    private String currentTaskType;

    private String submittedAt;

    private String finishedAt;

    private Map<String, Object> templateSnapshot = new LinkedHashMap<>();

    private Map<String, Object> formSchemaSnapshot = new LinkedHashMap<>();

    private Map<String, Object> formData = new LinkedHashMap<>();

    private Map<String, Object> flowSnapshot = new LinkedHashMap<>();

    private List<ProcessFormOptionVO> companyOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> departmentOptions = new ArrayList<>();

    private List<ExpenseDetailInstanceSummaryVO> expenseDetails = new ArrayList<>();

    private List<ExpenseApprovalTaskVO> currentTasks = new ArrayList<>();

    private List<ExpenseApprovalLogVO> actionLogs = new ArrayList<>();

    private ExpenseDocumentBankPaymentVO bankPayment;

    private List<ExpenseDocumentBankReceiptVO> bankReceipts = new ArrayList<>();
}
