package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProcessCustomArchiveRuleDTO {

    private Long id;

    @NotNull(message = "瑙勫垯缁勪笉鑳戒负绌?")
    private Integer groupNo;

    @NotBlank(message = "瑙勫垯瀛楁涓嶈兘涓虹┖")
    private String fieldKey;

    @NotBlank(message = "瑙勫垯鎿嶄綔绗︿笉鑳戒负绌?")
    private String operator;

    private Object compareValue;
}
