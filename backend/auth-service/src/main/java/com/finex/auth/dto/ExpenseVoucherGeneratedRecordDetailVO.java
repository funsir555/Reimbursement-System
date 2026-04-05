package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExpenseVoucherGeneratedRecordDetailVO {

    private ExpenseVoucherGeneratedRecordVO record;
    private FinanceVoucherDetailVO voucherDetail;
    private List<ExpenseVoucherEntrySnapshotVO> entries = new ArrayList<>();
}
