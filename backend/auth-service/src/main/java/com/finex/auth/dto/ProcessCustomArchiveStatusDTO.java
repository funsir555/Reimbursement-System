package com.finex.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProcessCustomArchiveStatusDTO {

    @NotNull(message = "鐘舵€佷笉鑳戒负绌?")
    private Integer status;
}
