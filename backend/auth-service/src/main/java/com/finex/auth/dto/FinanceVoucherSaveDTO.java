package com.finex.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FinanceVoucherSaveDTO {

    @NotBlank(message = "公司主体不能为空")
    private String companyId;

    @NotNull(message = "会计期间不能为空")
    private Integer iperiod;

    @NotBlank(message = "凭证类别不能为空")
    private String csign;

    private Integer inoId;

    @NotBlank(message = "制单日期不能为空")
    private String dbillDate;

    private Integer idoc;

    private String cbill;

    private String ctext1;

    private String ctext2;

    @Valid
    @NotEmpty(message = "凭证分录不能为空")
    private List<FinanceVoucherEntryDTO> entries = new ArrayList<>();
}
