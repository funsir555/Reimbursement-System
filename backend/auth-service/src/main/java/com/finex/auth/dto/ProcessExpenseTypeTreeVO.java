package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessExpenseTypeTreeVO {

    private Long id;

    private Long parentId;

    private String expenseCode;

    private String expenseName;

    private Integer status;

    private List<ProcessExpenseTypeTreeVO> children = new ArrayList<>();
}
