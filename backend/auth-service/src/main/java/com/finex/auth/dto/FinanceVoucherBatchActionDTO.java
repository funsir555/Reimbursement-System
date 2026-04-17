package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FinanceVoucherBatchActionDTO {

    @NotBlank(message = "公司主体不能为空")
    private String companyId;

    @NotBlank(message = "批量动作不能为空")
    private String action;

    @NotEmpty(message = "凭证集合不能为空")
    private List<String> voucherNos = new ArrayList<>();
}
