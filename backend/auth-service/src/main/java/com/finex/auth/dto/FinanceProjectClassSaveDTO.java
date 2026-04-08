package com.finex.auth.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FinanceProjectClassSaveDTO {

    @NotBlank(message = "项目分类编码不能为空")
    private String projectClassCode;

    @NotBlank(message = "项目分类名称不能为空")
    private String projectClassName;
}
