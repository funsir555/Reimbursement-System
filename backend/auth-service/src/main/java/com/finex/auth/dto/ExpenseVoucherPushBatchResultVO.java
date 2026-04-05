package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExpenseVoucherPushBatchResultVO {

    private String latestBatchNo;
    private Integer successCount;
    private Integer failureCount;
    private List<ExpenseVoucherPushResultVO> results = new ArrayList<>();
}
