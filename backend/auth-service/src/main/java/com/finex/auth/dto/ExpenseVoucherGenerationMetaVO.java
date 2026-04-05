package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ExpenseVoucherGenerationMetaVO {

    private List<FinanceVoucherOptionVO> companyOptions = new ArrayList<>();
    private List<FinanceVoucherOptionVO> templateOptions = new ArrayList<>();
    private List<FinanceVoucherOptionVO> expenseTypeOptions = new ArrayList<>();
    private List<FinanceVoucherOptionVO> accountOptions = new ArrayList<>();
    private List<FinanceVoucherOptionVO> voucherTypeOptions = new ArrayList<>();
    private List<FinanceVoucherOptionVO> pushStatusOptions = new ArrayList<>();
    private String defaultCompanyId;
    private String latestBatchNo;
    private Integer pendingPushCount;
    private Integer pushedVoucherCount;
    private Integer pushFailureCount;
    @MoneyValue
    private BigDecimal pendingPushAmount;
}
