package com.finex.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DashboardWriteOffBindingDTO {

    @NotBlank(message = "鐩爣鍗曟嵁缂栫爜涓嶈兘涓虹┖")
    private String targetDocumentCode;

    @NotBlank(message = "鏉ユ簮鎶ラ攢鍗曠紪鐮佷笉鑳戒负绌?")
    private String sourceReportDocumentCode;
}
