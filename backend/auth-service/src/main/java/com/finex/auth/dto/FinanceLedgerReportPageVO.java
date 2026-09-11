package com.finex.auth.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class FinanceLedgerReportPageVO<T> {

    private long total;

    private int page;

    private int pageSize;

    private List<T> items = new ArrayList<>();
}
