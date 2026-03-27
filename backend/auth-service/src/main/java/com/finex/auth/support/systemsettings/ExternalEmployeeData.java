package com.finex.auth.support.systemsettings;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExternalEmployeeData {

    private String username;

    private String name;

    private String phone;

    private String email;

    private String deptCode;

    private String position;

    private String laborRelationBelong;

    private String externalId;

    private Integer status;
}
