package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SystemSettingsBootstrapVO {

    private UserProfileVO currentUser;

    private List<DepartmentTreeNodeVO> departments = new ArrayList<>();

    private List<EmployeeVO> employees = new ArrayList<>();

    private List<RoleVO> roles = new ArrayList<>();

    private List<PermissionTreeNodeVO> permissions = new ArrayList<>();

    private List<CompanyVO> companies = new ArrayList<>();

    private List<CompanyBankAccountVO> companyBankAccounts = new ArrayList<>();

    private List<SyncConnectorVO> connectors = new ArrayList<>();

    private List<SyncJobVO> jobs = new ArrayList<>();
}
