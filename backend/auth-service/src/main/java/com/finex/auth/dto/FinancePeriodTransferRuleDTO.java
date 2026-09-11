package com.finex.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class FinancePeriodTransferRuleDTO {

    private Long id;

    @NotBlank(message = "公司主体不能为空")
    private String companyId;

    @NotBlank(message = "规则类型不能为空")
    private String ruleType;

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 128, message = "规则名称长度不能超过 128 个字符")
    private String ruleName;

    private Boolean enabled;

    @Size(max = 32, message = "凭证类别字长度不能超过 32 个字符")
    private String voucherType;

    private Boolean includeUnposted;

    @Size(max = 32, message = "结转粒度长度不能超过 32 个字符")
    private String transferGranularity;

    private Integer subjectLevel;

    @Size(max = 64, message = "默认转出科目编码长度不能超过 64 个字符")
    private String sourceSubjectCode;

    @Size(max = 128, message = "默认转出科目名称长度不能超过 128 个字符")
    private String sourceSubjectName;

    @Size(max = 64, message = "默认转入科目编码长度不能超过 64 个字符")
    private String targetSubjectCode;

    @Size(max = 128, message = "默认转入科目名称长度不能超过 128 个字符")
    private String targetSubjectName;

    @Size(max = 32, message = "分配方式长度不能超过 32 个字符")
    private String allocationMode;

    @Valid
    private List<FinancePeriodTransferRuleLineDTO> lines = new ArrayList<>();
}
