package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FinanceVoucherBatchActionResultVO {

    private String action;

    private Integer successCount;

    private List<String> voucherNos = new ArrayList<>();
}
