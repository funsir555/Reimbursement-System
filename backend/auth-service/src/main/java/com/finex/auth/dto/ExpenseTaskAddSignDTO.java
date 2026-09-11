package com.finex.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExpenseTaskAddSignDTO {

    public static final String POSITION_BEFORE = "BEFORE";
    public static final String POSITION_AFTER = "AFTER";

    @NotNull
    private Long targetUserId;

    private String remark;

    /**
     * 加签位置。未传时按历史行为兼容为在当前审批人之前。
     */
    private String position = POSITION_BEFORE;
}
