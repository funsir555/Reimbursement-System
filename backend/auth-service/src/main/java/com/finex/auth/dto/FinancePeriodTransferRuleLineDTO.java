package com.finex.auth.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class FinancePeriodTransferRuleLineDTO {

    private Long id;

    private Integer lineNo;

    @Size(max = 64, message = "转出科目编码长度不能超过 64 个字符")
    private String sourceSubjectCode;

    @Size(max = 128, message = "转出科目名称长度不能超过 128 个字符")
    private String sourceSubjectName;

    @Size(max = 64, message = "转入科目编码长度不能超过 64 个字符")
    private String targetSubjectCode;

    @Size(max = 128, message = "转入科目名称长度不能超过 128 个字符")
    private String targetSubjectName;

    @Size(max = 32, message = "取数指标长度不能超过 32 个字符")
    private String metricType;

    @DecimalMin(value = "0.0", inclusive = false, message = "比例系数必须大于 0")
    private BigDecimal ratio;

    @Size(max = 2, message = "项目分类编码长度不能超过 2 个字符")
    private String allocationProjectClass;

    @Size(max = 6, message = "项目编码长度不能超过 6 个字符")
    private String allocationProjectId;

    @Size(max = 128, message = "项目名称长度不能超过 128 个字符")
    private String allocationProjectName;

    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String remark;
}
