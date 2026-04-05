package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExpenseDocumentPickerVO {

    private String relationType;

    private List<ExpenseDocumentPickerGroupVO> groups = new ArrayList<>();
}
