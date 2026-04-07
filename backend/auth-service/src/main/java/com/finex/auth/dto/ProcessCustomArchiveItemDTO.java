package com.finex.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessCustomArchiveItemDTO {

    private Long id;

    private String itemCode;

    @NotBlank(message = "缁撴灉椤瑰悕绉颁笉鑳戒负绌?")
    private String itemName;

    private Integer priority;

    private Integer status;

    @Valid
    private List<ProcessCustomArchiveRuleDTO> rules = new ArrayList<>();
}
