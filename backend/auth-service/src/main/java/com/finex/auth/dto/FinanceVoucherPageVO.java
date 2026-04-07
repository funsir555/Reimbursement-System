package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FinanceVoucherPageVO<T> {

    private Integer total;

    private Integer page;

    private Integer pageSize;

    private List<T> items = new ArrayList<>();
}
