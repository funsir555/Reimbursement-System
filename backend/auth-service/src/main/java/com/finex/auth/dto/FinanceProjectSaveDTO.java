package com.finex.auth.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FinanceProjectSaveDTO {

    @NotBlank(message = "项目编码不能为空")
    private String citemcode;

    @NotBlank(message = "项目名称不能为空")
    private String citemname;

    @NotBlank(message = "项目分类不能为空")
    private String citemccode;

    private Integer iotherused;

    private LocalDateTime dEndDate;
}
