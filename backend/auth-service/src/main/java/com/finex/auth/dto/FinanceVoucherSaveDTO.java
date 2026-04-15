package com.finex.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Size(max = 64, message = "\u5236\u5355\u4eba\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26")
    private String cbill;

    private String ctext1;

    private String ctext2;

    @Valid
    @NotEmpty(message = "凭证分录不能为空")
    private List<FinanceVoucherEntryDTO> entries = new ArrayList<>();
}
