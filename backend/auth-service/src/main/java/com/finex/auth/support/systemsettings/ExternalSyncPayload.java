package com.finex.auth.support.systemsettings;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ExternalSyncPayload {

    private List<ExternalDepartmentData> departments;

    private List<ExternalEmployeeData> employees;
}
