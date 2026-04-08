package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class FinanceAccountSetCreateDTO {

    @NotBlank
    private String createMode;

    private String referenceCompanyId;

    @NotBlank
    private String targetCompanyId;

    @NotBlank
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "启用年月格式必须为 YYYY-MM")
    private String enabledYearMonth;

    private String templateCode;

    @NotNull
    private Long supervisorUserId;

    private String subjectCodeScheme;
}

