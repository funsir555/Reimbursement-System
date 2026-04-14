package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    @Size(max = 32, message = "模板编码长度不能超过32个字符")
    private String templateCode;

    @NotNull
    private Long supervisorUserId;

    @Size(max = 32, message = "科目编码规则长度不能超过32个字符")
    private String subjectCodeScheme;
}

