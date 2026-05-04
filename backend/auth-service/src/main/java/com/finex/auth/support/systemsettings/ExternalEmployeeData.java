package com.finex.auth.support.systemsettings;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class ExternalEmployeeData {

    private String username;

    private String name;

    private String phone;

    private String email;

    private List<String> deptCodes = new ArrayList<>();

    private String position;

    private String laborRelationBelong;

    private String externalId;

    private Integer status;
}
