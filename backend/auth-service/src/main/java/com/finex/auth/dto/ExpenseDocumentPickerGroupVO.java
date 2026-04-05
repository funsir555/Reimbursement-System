package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExpenseDocumentPickerGroupVO {

    private String templateType;

    private String templateTypeLabel;

    private Integer total;

    private Integer page;

    private Integer pageSize;

    private List<ExpenseDocumentPickerItemVO> items = new ArrayList<>();
}
