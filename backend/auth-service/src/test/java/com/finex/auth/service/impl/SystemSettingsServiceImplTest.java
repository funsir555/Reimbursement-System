package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.CompanyBankAccountSaveDTO;
import com.finex.auth.dto.CompanyBankAccountVO;
import com.finex.auth.dto.DepartmentSaveDTO;
import com.finex.auth.dto.DepartmentTreeNodeVO;
import com.finex.auth.dto.EmployeeSaveDTO;
import com.finex.auth.dto.EmployeeVO;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.SystemCompanyBankAccount;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.SystemSyncConnector;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.SystemCompanyBankAccountMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.SystemPermissionMapper;
import com.finex.auth.mapper.SystemRoleMapper;
import com.finex.auth.mapper.SystemRolePermissionMapper;
import com.finex.auth.mapper.SystemSyncConnectorMapper;
import com.finex.auth.mapper.SystemSyncJobDetailMapper;
import com.finex.auth.mapper.SystemSyncJobMapper;
import com.finex.auth.mapper.SystemUserRoleMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.AccessControlService;
import com.finex.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemSettingsServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private SystemDepartmentMapper systemDepartmentMapper;
    @Mock
    private SystemCompanyBankAccountMapper systemCompanyBankAccountMapper;
    @Mock
    private SystemCompanyMapper systemCompanyMapper;
    @Mock
    private SystemRoleMapper systemRoleMapper;
    @Mock
    private SystemPermissionMapper systemPermissionMapper;
    @Mock
    private SystemRolePermissionMapper systemRolePermissionMapper;
    @Mock
    private SystemUserRoleMapper systemUserRoleMapper;
    @Mock
    private SystemSyncConnectorMapper systemSyncConnectorMapper;
    @Mock
    private SystemSyncJobMapper systemSyncJobMapper;
    @Mock
    private SystemSyncJobDetailMapper systemSyncJobDetailMapper;

    private SystemSettingsServiceImpl systemSettingsService;

    @BeforeEach
    void setUp() {
        systemSettingsService = new SystemSettingsServiceImpl(
                userService,
                accessControlService,
                userMapper,
                systemDepartmentMapper,
                systemCompanyBankAccountMapper,
                systemCompanyMapper,
                systemRoleMapper,
                systemPermissionMapper,
                systemRolePermissionMapper,
                systemUserRoleMapper,
                systemSyncConnectorMapper,
                systemSyncJobMapper,
                systemSyncJobDetailMapper,
                new ObjectMapper(),
                List.of()
        );
    }

    @Test
    void updateCompanyBankAccountClearsOtherDefaultsInSameCompany() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setCompanyName("Company A");

        SystemCompanyBankAccount current = new SystemCompanyBankAccount();
        current.setId(1L);
        current.setCompanyId("COMPANY_A");
        current.setBankName("Bank A");
        current.setAccountName("Primary Account");
        current.setAccountNo("622200001");
        current.setStatus(1);
        current.setDefaultAccount(0);

        SystemCompanyBankAccount otherDefault = new SystemCompanyBankAccount();
        otherDefault.setId(2L);
        otherDefault.setCompanyId("COMPANY_A");
        otherDefault.setBankName("Bank B");
        otherDefault.setAccountName("Backup Account");
        otherDefault.setAccountNo("622200002");
        otherDefault.setStatus(1);
        otherDefault.setDefaultAccount(1);

        CompanyBankAccountSaveDTO dto = new CompanyBankAccountSaveDTO();
        dto.setCompanyId("COMPANY_A");
        dto.setBankName("Bank A");
        dto.setBankCode("BANK_A");
        dto.setProvince("广东省");
        dto.setCity("深圳市");
        dto.setBranchName("深圳支行");
        dto.setBranchCode("BANK_A_SZ");
        dto.setAccountName("Primary Account");
        dto.setAccountNo("622200001");
        dto.setStatus(1);
        dto.setDefaultAccount(1);

        when(systemCompanyBankAccountMapper.selectById(1L)).thenReturn(current);
        when(systemCompanyBankAccountMapper.selectCount(any())).thenReturn(0L);
        when(systemCompanyBankAccountMapper.selectList(any())).thenReturn(List.of(current, otherDefault));
        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(company));

        CompanyBankAccountVO result = systemSettingsService.updateCompanyBankAccount(1L, dto);

        ArgumentCaptor<SystemCompanyBankAccount> captor = ArgumentCaptor.forClass(SystemCompanyBankAccount.class);
        verify(systemCompanyBankAccountMapper, atLeastOnce()).updateById(captor.capture());

        List<SystemCompanyBankAccount> updatedAccounts = captor.getAllValues();
        assertEquals(1, updatedAccounts.get(0).getDefaultAccount());
        assertEquals(0, updatedAccounts.get(updatedAccounts.size() - 1).getDefaultAccount());
        assertEquals("COMPANY_A", result.getCompanyId());
    }

    @Test
    void updateCompanyBankAccountClearsDefaultWhenDisabled() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setCompanyName("Company A");

        SystemCompanyBankAccount current = new SystemCompanyBankAccount();
        current.setId(1L);
        current.setCompanyId("COMPANY_A");
        current.setBankName("Bank A");
        current.setAccountName("Primary Account");
        current.setAccountNo("622200001");
        current.setStatus(1);
        current.setDefaultAccount(1);

        CompanyBankAccountSaveDTO dto = new CompanyBankAccountSaveDTO();
        dto.setCompanyId("COMPANY_A");
        dto.setBankName("Bank A");
        dto.setBankCode("BANK_A");
        dto.setProvince("广东省");
        dto.setCity("深圳市");
        dto.setBranchName("深圳支行");
        dto.setBranchCode("BANK_A_SZ");
        dto.setAccountName("Primary Account");
        dto.setAccountNo("622200001");
        dto.setStatus(0);
        dto.setDefaultAccount(1);

        when(systemCompanyBankAccountMapper.selectById(1L)).thenReturn(current);
        when(systemCompanyBankAccountMapper.selectCount(any())).thenReturn(0L);
        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(company));

        systemSettingsService.updateCompanyBankAccount(1L, dto);

        ArgumentCaptor<SystemCompanyBankAccount> captor = ArgumentCaptor.forClass(SystemCompanyBankAccount.class);
        verify(systemCompanyBankAccountMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
        assertEquals(0, captor.getValue().getDefaultAccount());
    }

    @Test
    void updateCompanyBankAccountPreservesCnapsWhenBranchSelectionIsUnchanged() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setCompanyName("Company A");

        SystemCompanyBankAccount current = new SystemCompanyBankAccount();
        current.setId(3L);
        current.setCompanyId("COMPANY_A");
        current.setBankName("Bank A");
        current.setBankCode("BANK_A");
        current.setProvince("广东省");
        current.setCity("深圳市");
        current.setBranchName("深圳支行");
        current.setBranchCode("BANK_A_SZ");
        current.setCnapsCode("308584000013");
        current.setAccountName("Primary Account");
        current.setAccountNo("622200001");
        current.setStatus(1);
        current.setDefaultAccount(0);

        CompanyBankAccountSaveDTO dto = new CompanyBankAccountSaveDTO();
        dto.setCompanyId("COMPANY_A");
        dto.setBankName("Bank A");
        dto.setBankCode("BANK_A");
        dto.setProvince("广东省");
        dto.setCity("深圳市");
        dto.setBranchName("深圳支行");
        dto.setBranchCode("BANK_A_SZ");
        dto.setAccountName("Primary Account");
        dto.setAccountNo("622200001");
        dto.setStatus(1);
        dto.setDefaultAccount(0);

        when(systemCompanyBankAccountMapper.selectById(3L)).thenReturn(current);
        when(systemCompanyBankAccountMapper.selectCount(any())).thenReturn(0L);
        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(company));

        systemSettingsService.updateCompanyBankAccount(3L, dto);

        ArgumentCaptor<SystemCompanyBankAccount> captor = ArgumentCaptor.forClass(SystemCompanyBankAccount.class);
        verify(systemCompanyBankAccountMapper).updateById(captor.capture());
        assertEquals("308584000013", captor.getValue().getCnapsCode());
    }

    @Test
    void updateCompanyBankAccountClearsCnapsWhenBranchSelectionChanges() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setCompanyName("Company A");

        SystemCompanyBankAccount current = new SystemCompanyBankAccount();
        current.setId(4L);
        current.setCompanyId("COMPANY_A");
        current.setBankName("Bank A");
        current.setBankCode("BANK_A");
        current.setProvince("广东省");
        current.setCity("深圳市");
        current.setBranchName("深圳支行");
        current.setBranchCode("BANK_A_SZ");
        current.setCnapsCode("308584000013");
        current.setAccountName("Primary Account");
        current.setAccountNo("622200001");
        current.setStatus(1);
        current.setDefaultAccount(0);

        CompanyBankAccountSaveDTO dto = new CompanyBankAccountSaveDTO();
        dto.setCompanyId("COMPANY_A");
        dto.setBankName("Bank A");
        dto.setBankCode("BANK_A");
        dto.setProvince("广东省");
        dto.setCity("深圳市");
        dto.setBranchName("南山支行");
        dto.setBranchCode("BANK_A_NS");
        dto.setAccountName("Primary Account");
        dto.setAccountNo("622200001");
        dto.setStatus(1);
        dto.setDefaultAccount(0);

        when(systemCompanyBankAccountMapper.selectById(4L)).thenReturn(current);
        when(systemCompanyBankAccountMapper.selectCount(any())).thenReturn(0L);
        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(company));

        systemSettingsService.updateCompanyBankAccount(4L, dto);

        ArgumentCaptor<SystemCompanyBankAccount> captor = ArgumentCaptor.forClass(SystemCompanyBankAccount.class);
        verify(systemCompanyBankAccountMapper).updateById(captor.capture());
        assertNull(captor.getValue().getCnapsCode());
    }

    @Test
    void updateCompanyBankAccountUsesUnifiedBankDirectoryValidationMessage() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setCompanyName("Company A");

        SystemCompanyBankAccount current = new SystemCompanyBankAccount();
        current.setId(5L);
        current.setCompanyId("COMPANY_A");
        current.setStatus(1);
        current.setDefaultAccount(0);

        CompanyBankAccountSaveDTO dto = new CompanyBankAccountSaveDTO();
        dto.setCompanyId("COMPANY_A");
        dto.setBankName("Bank A");
        dto.setBankCode("BANK_A");
        dto.setProvince("广东省");
        dto.setCity("深圳市");
        dto.setBranchCode("BANK_A_SZ");
        dto.setAccountName("Primary Account");
        dto.setAccountNo("622200001");
        dto.setStatus(1);
        dto.setDefaultAccount(0);

        when(systemCompanyBankAccountMapper.selectById(5L)).thenReturn(current);
        when(systemCompanyBankAccountMapper.selectCount(any())).thenReturn(0L);
        when(systemCompanyMapper.selectById("COMPANY_A")).thenReturn(company);
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(company));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemSettingsService.updateCompanyBankAccount(5L, dto)
        );

        assertEquals("请选择开户银行、开户省、开户市与开户网点后再保存", exception.getMessage());
    }

    @Test
    void updateEmployeeForSyncedUserOnlyChangesStatFields() {
        User syncedUser = new User();
        syncedUser.setId(10L);
        syncedUser.setUsername("sync_user");
        syncedUser.setName("Synced User");
        syncedUser.setPhone("13800000000");
        syncedUser.setEmail("before@example.com");
        syncedUser.setCompanyId("COMPANY_A");
        syncedUser.setDeptId(8L);
        syncedUser.setPosition("Accountant");
        syncedUser.setLaborRelationBelong("Formal");
        syncedUser.setStatDepartmentBelong("Old Department");
        syncedUser.setStatRegionBelong("Old Region");
        syncedUser.setStatAreaBelong("Old Area");
        syncedUser.setStatus(1);
        syncedUser.setSourceType("WECOM");
        syncedUser.setSyncManaged(1);

        EmployeeSaveDTO dto = new EmployeeSaveDTO();
        dto.setUsername("manual_override");
        dto.setName("Manual Override");
        dto.setPhone("13999999999");
        dto.setEmail("after@example.com");
        dto.setCompanyId("COMPANY_B");
        dto.setDeptId(99L);
        dto.setPosition("Changed Position");
        dto.setLaborRelationBelong("External");
        dto.setStatDepartmentBelong("New Department");
        dto.setStatRegionBelong("New Region");
        dto.setStatAreaBelong("New Area");
        dto.setStatus(0);

        when(userMapper.selectById(10L)).thenReturn(syncedUser);
        when(userMapper.selectList(any())).thenReturn(List.of(syncedUser));
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of());
        when(systemDepartmentMapper.selectList(any())).thenReturn(List.of());
        when(systemUserRoleMapper.selectList(any())).thenReturn(List.of());

        EmployeeVO result = systemSettingsService.updateEmployee(10L, dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());

        User updated = captor.getValue();
        assertEquals("sync_user", updated.getUsername());
        assertEquals("Synced User", updated.getName());
        assertEquals("13800000000", updated.getPhone());
        assertEquals("before@example.com", updated.getEmail());
        assertEquals("COMPANY_A", updated.getCompanyId());
        assertEquals(8L, updated.getDeptId());
        assertEquals("Accountant", updated.getPosition());
        assertEquals("Formal", updated.getLaborRelationBelong());
        assertEquals(1, updated.getStatus());
        assertEquals("New Department", updated.getStatDepartmentBelong());
        assertEquals("New Region", updated.getStatRegionBelong());
        assertEquals("New Area", updated.getStatAreaBelong());

        assertEquals("sync_user", result.getUsername());
        assertEquals("Synced User", result.getName());
        assertEquals("New Department", result.getStatDepartmentBelong());
        assertEquals("New Region", result.getStatRegionBelong());
        assertEquals("New Area", result.getStatAreaBelong());
    }

    @Test
    void deleteEmployeeRejectsSyncedUser() {
        User syncedUser = new User();
        syncedUser.setId(12L);
        syncedUser.setSourceType("FEISHU");
        syncedUser.setSyncManaged(1);

        when(userMapper.selectById(12L)).thenReturn(syncedUser);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> systemSettingsService.deleteEmployee(12L)
        );

        assertEquals("\u540c\u6b65\u5458\u5de5\u4e0d\u80fd\u76f4\u63a5\u5220\u9664\uff0c\u8bf7\u901a\u8fc7\u540c\u6b65\u6e05\u7406", error.getMessage());
        verify(systemUserRoleMapper, never()).delete(any());
        verify(userMapper, never()).deleteById(12L);
    }

    @Test
    void updateDepartmentForSyncedDepartmentOnlyChangesStatFields() {
        SystemDepartment syncedDepartment = new SystemDepartment();
        syncedDepartment.setId(20L);
        syncedDepartment.setDeptCode("D_SYNC");
        syncedDepartment.setDeptName("Synced Department");
        syncedDepartment.setParentId(3L);
        syncedDepartment.setCompanyId("COMPANY_A");
        syncedDepartment.setLeaderUserId(11L);
        syncedDepartment.setSyncSource("WECOM");
        syncedDepartment.setSyncManaged(1);
        syncedDepartment.setSyncEnabled(1);
        syncedDepartment.setSyncStatus("SYNCED");
        syncedDepartment.setSyncRemark("from wecom");
        syncedDepartment.setStatus(1);
        syncedDepartment.setSortOrder(8);
        syncedDepartment.setStatDepartmentBelong("Old Department");
        syncedDepartment.setStatRegionBelong("Old Region");
        syncedDepartment.setStatAreaBelong("Old Area");

        DepartmentSaveDTO dto = new DepartmentSaveDTO();
        dto.setDeptName("Manual Override");
        dto.setParentId(99L);
        dto.setCompanyId("COMPANY_B");
        dto.setLeaderUserId(18L);
        dto.setSyncEnabled(0);
        dto.setStatus(0);
        dto.setSortOrder(1);
        dto.setStatDepartmentBelong("New Department");
        dto.setStatRegionBelong("New Region");
        dto.setStatAreaBelong("New Area");

        when(systemDepartmentMapper.selectById(20L)).thenReturn(syncedDepartment);
        when(systemDepartmentMapper.selectList(any())).thenReturn(List.of(syncedDepartment));
        when(userMapper.selectList(any())).thenReturn(List.of());

        DepartmentTreeNodeVO result = systemSettingsService.updateDepartment(20L, dto);

        ArgumentCaptor<SystemDepartment> captor = ArgumentCaptor.forClass(SystemDepartment.class);
        verify(systemDepartmentMapper).updateById(captor.capture());

        SystemDepartment updated = captor.getValue();
        assertEquals("Synced Department", updated.getDeptName());
        assertEquals(3L, updated.getParentId());
        assertEquals("COMPANY_A", updated.getCompanyId());
        assertEquals(11L, updated.getLeaderUserId());
        assertEquals(1, updated.getSyncEnabled());
        assertEquals(1, updated.getStatus());
        assertEquals(8, updated.getSortOrder());
        assertEquals("SYNCED", updated.getSyncStatus());
        assertEquals("from wecom", updated.getSyncRemark());
        assertEquals("New Department", updated.getStatDepartmentBelong());
        assertEquals("New Region", updated.getStatRegionBelong());
        assertEquals("New Area", updated.getStatAreaBelong());

        assertEquals("Synced Department", result.getDeptName());
        assertEquals("New Department", result.getStatDepartmentBelong());
        assertEquals("New Region", result.getStatRegionBelong());
        assertEquals("New Area", result.getStatAreaBelong());
    }

    @Test
    void listSyncConnectorsResolvesChinesePlatformNames() {
        SystemSyncConnector dingtalk = new SystemSyncConnector();
        dingtalk.setId(1L);
        dingtalk.setPlatformCode("DINGTALK");
        dingtalk.setPlatformName("????");
        dingtalk.setEnabled(1);
        dingtalk.setAutoSyncEnabled(0);
        dingtalk.setSyncIntervalMinutes(60);
        dingtalk.setConfigJson("{}");

        SystemSyncConnector wecom = new SystemSyncConnector();
        wecom.setId(2L);
        wecom.setPlatformCode("WECOM");
        wecom.setPlatformName("????");
        wecom.setEnabled(1);
        wecom.setAutoSyncEnabled(0);
        wecom.setSyncIntervalMinutes(60);
        wecom.setConfigJson("{}");

        SystemSyncConnector feishu = new SystemSyncConnector();
        feishu.setId(3L);
        feishu.setPlatformCode("FEISHU");
        feishu.setPlatformName("????");
        feishu.setEnabled(1);
        feishu.setAutoSyncEnabled(0);
        feishu.setSyncIntervalMinutes(60);
        feishu.setConfigJson("{}");

        when(systemSyncConnectorMapper.selectList(any())).thenReturn(List.of(dingtalk, wecom, feishu));

        List<String> platformNames = systemSettingsService.listSyncConnectors().stream()
                .map(item -> item.getPlatformName())
                .toList();

        assertEquals(List.of("\u9489\u9489", "\u4f01\u5fae", "\u98de\u4e66"), platformNames);
    }
    @Test
    void updateCompanyBankAccountUsesUnifiedOutletValidationMessage() {
        SystemCompany company = new SystemCompany();
        company.setCompanyId("COMPANY_A");
        company.setCompanyName("Company A");

        SystemCompanyBankAccount current = new SystemCompanyBankAccount();
        current.setId(5L);
        current.setCompanyId("COMPANY_A");
        current.setBankName("Bank A");
        current.setBankCode("BANK_A");
        current.setProvince("广东省");
        current.setCity("深圳市");
        current.setBranchName("深圳支行");
        current.setBranchCode("BANK_A_SZ");
        current.setAccountName("Primary Account");
        current.setAccountNo("622200001");
        current.setStatus(1);

        CompanyBankAccountSaveDTO dto = new CompanyBankAccountSaveDTO();
        dto.setCompanyId("COMPANY_A");
        dto.setBankName("Bank A");
        dto.setBankCode("BANK_A");
        dto.setProvince("广东省");
        dto.setCity("深圳市");
        dto.setBranchCode("BANK_A_SZ");
        dto.setAccountName("Primary Account");
        dto.setAccountNo("622200001");
        dto.setStatus(1);

        when(systemCompanyBankAccountMapper.selectById(5L)).thenReturn(current);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> systemSettingsService.updateCompanyBankAccount(5L, dto)
        );

        assertEquals("\u5f00\u6237\u7f51\u70b9\u4e0d\u80fd\u4e3a\u7a7a", exception.getMessage());
    }
}
