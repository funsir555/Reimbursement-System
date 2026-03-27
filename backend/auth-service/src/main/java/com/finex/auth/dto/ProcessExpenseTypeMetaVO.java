package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessExpenseTypeMetaVO {

    private List<ProcessExpenseTypeConfigOptionVO> invoiceFreeOptions = new ArrayList<>();

    private List<ProcessExpenseTypeConfigOptionVO> taxDeductionOptions = new ArrayList<>();

    private List<ProcessExpenseTypeConfigOptionVO> taxSeparationOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> departmentOptions = new ArrayList<>();

    private List<ProcessFormOptionVO> userOptions = new ArrayList<>();
}
