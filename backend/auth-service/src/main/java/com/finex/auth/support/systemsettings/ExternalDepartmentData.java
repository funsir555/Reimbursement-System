package com.finex.auth.support.systemsettings;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExternalDepartmentData {

    private String deptCode;

    private String deptName;

    private String parentDeptCode;

    private String externalId;

    private Integer status;
}
