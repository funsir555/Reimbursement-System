// 业务域：系统设置
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 组织、角色、公司信息和基础设置页面，下游会继续协调 公司、组织、角色、同步任务和系统参数。
// 风险提醒：改坏后最容易影响 权限体系、组织架构和历史单据可用性。

package com.finex.auth.service.impl.settings;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.CompanyBankAccountSaveDTO;
import com.finex.auth.dto.CompanyBankAccountVO;
import com.finex.auth.dto.CompanySaveDTO;
import com.finex.auth.dto.CompanyVO;
import com.finex.auth.dto.DepartmentSaveDTO;
import com.finex.auth.dto.DepartmentTreeNodeVO;
import com.finex.auth.dto.EmployeeQueryDTO;
import com.finex.auth.dto.EmployeeSaveDTO;
import com.finex.auth.dto.EmployeeVO;
import com.finex.auth.dto.PermissionTreeNodeVO;
import com.finex.auth.dto.RolePermissionAssignDTO;
import com.finex.auth.dto.RoleSaveDTO;
import com.finex.auth.dto.RoleVO;
import com.finex.auth.dto.SyncConnectorSaveDTO;
import com.finex.auth.dto.SyncConnectorVO;
import com.finex.auth.dto.SyncJobVO;
import com.finex.auth.dto.SyncRunDTO;
import com.finex.auth.dto.SystemSettingsBootstrapVO;
import com.finex.auth.dto.UserProfileVO;
import com.finex.auth.dto.UserRoleAssignDTO;
import com.finex.auth.entity.SystemCompanyBankAccount;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.SystemPermission;
import com.finex.auth.entity.SystemRole;
import com.finex.auth.entity.SystemRolePermission;
import com.finex.auth.entity.SystemSyncConnector;
import com.finex.auth.entity.SystemSyncJob;
import com.finex.auth.entity.SystemSyncJobDetail;
import com.finex.auth.entity.SystemUserRole;
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
import com.finex.auth.support.systemsettings.ExternalDepartmentData;
import com.finex.auth.support.systemsettings.ExternalEmployeeData;
import com.finex.auth.support.systemsettings.ExternalSyncPayload;
import com.finex.auth.support.systemsettings.OrganizationSyncAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AbstractSystemSettingsDomainSupport：领域规则支撑类。
 * 承接 系统系统设置的核心业务规则。
 * 改这里时，要特别关注 权限体系、组织架构和历史单据可用性是否会被一起带坏。
 */
@RequiredArgsConstructor
abstract class AbstractSystemSettingsDomainSupport {

    private static final String BANK_DIRECTORY_REQUIRED_MESSAGE = "请选择开户银行、开户省、开户市与开户网点后再保存";
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String PLATFORM_DINGTALK = "DINGTALK";
    private static final String PLATFORM_WECOM = "WECOM";
    private static final String PLATFORM_FEISHU = "FEISHU";
    private static final String JOB_STATUS_SUCCESS = "SUCCESS";
    private static final String JOB_STATUS_FAILED = "FAILED";
    private static final String JOB_STATUS_RUNNING = "RUNNING";
    private static final String DETAIL_STATUS_SUCCESS = "SUCCESS";
    private static final String DETAIL_STATUS_SKIPPED = "SKIPPED";
    private static final String DETAIL_STATUS_FAILED = "FAILED";
    private static final String DETAIL_STATUS_DELETED = "DELETED";
    private static final String DETAIL_TYPE_DEPARTMENT = "DEPARTMENT";
    private static final String DETAIL_TYPE_EMPLOYEE = "EMPLOYEE";
    private static final DateTimeFormatter JOB_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String DEPARTMENT_CODE_PREFIX = "DEPT";
    private static final String COMPANY_ID_PREFIX = "COMPANY";
    private static final String COMPANY_CODE_PREFIX = "COMP";
    private static final String ROLE_CODE_PREFIX = "RL";
    private static final String SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN";
    private static final int CODE_GENERATION_MAX_RETRY = 20;

    private final UserService userService;
    private final AccessControlService accessControlService;
    private final UserMapper userMapper;
    private final SystemDepartmentMapper systemDepartmentMapper;
    private final SystemCompanyBankAccountMapper systemCompanyBankAccountMapper;
    private final SystemCompanyMapper systemCompanyMapper;
    private final SystemRoleMapper systemRoleMapper;
    private final SystemPermissionMapper systemPermissionMapper;
    private final SystemRolePermissionMapper systemRolePermissionMapper;
    private final SystemUserRoleMapper systemUserRoleMapper;
    private final SystemSyncConnectorMapper systemSyncConnectorMapper;
    private final SystemSyncJobMapper systemSyncJobMapper;
    private final SystemSyncJobDetailMapper systemSyncJobDetailMapper;
    private final ObjectMapper objectMapper;
    private final List<OrganizationSyncAdapter> syncAdapters;

    /**
     * 获取初始化。
     */
    protected SystemSettingsBootstrapVO getBootstrap(Long currentUserId) {
        SystemSettingsBootstrapVO bootstrap = new SystemSettingsBootstrapVO();
        bootstrap.setCurrentUser(buildCurrentUser(currentUserId));
        bootstrap.setDepartments(listDepartments());
        bootstrap.setEmployees(listEmployees(new EmployeeQueryDTO()));
        bootstrap.setRoles(listRoles());
        bootstrap.setPermissions(getPermissionTree());
        bootstrap.setCompanies(listCompanies());
        bootstrap.setCompanyBankAccounts(listCompanyBankAccounts());
        bootstrap.setConnectors(listSyncConnectors());
        bootstrap.setJobs(listSyncJobs());
        return bootstrap;
    }

    /**
     * 查询Departments列表。
     */
    protected List<DepartmentTreeNodeVO> listDepartments() {
        List<SystemDepartment> departments = systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .orderByAsc(SystemDepartment::getSortOrder, SystemDepartment::getId)
        );
        return buildDepartmentTree(departments);
    }

    /**
     * 创建Department。
     */
    @Transactional(rollbackFor = Exception.class)
    protected DepartmentTreeNodeVO createDepartment(DepartmentSaveDTO dto) {
        if (dto.getParentId() != null) {
            requireDepartment(dto.getParentId());
        }
        validateDepartmentLeader(dto.getLeaderUserId());

        SystemDepartment department = new SystemDepartment();
        department.setCompanyId(trimToNull(dto.getCompanyId()));
        department.setDeptCode(generateManualDepartmentCode());
        department.setLeaderUserId(dto.getLeaderUserId());
        department.setDeptName(dto.getDeptName().trim());
        department.setParentId(dto.getParentId());
        department.setSyncSource(SOURCE_MANUAL);
        department.setSyncEnabled(dto.getSyncEnabled() != null && dto.getSyncEnabled() == 0 ? 0 : 1);
        department.setSyncManaged(0);
        department.setSyncStatus("MANUAL");
        department.setSyncRemark("\u624b\u5de5\u7ef4\u62a4");
        applyDepartmentStatBelong(department, dto);
        department.setStatus(normalizeStatus(dto.getStatus()));
        department.setSortOrder(dto.getSortOrder() == null ? nextDepartmentSortOrder() : dto.getSortOrder());
        systemDepartmentMapper.insert(department);
        return findDepartmentNode(department.getId());
    }

    /**
     * 更新Department。
     */
    @Transactional(rollbackFor = Exception.class)
    protected DepartmentTreeNodeVO updateDepartment(Long id, DepartmentSaveDTO dto) {
        SystemDepartment department = requireDepartment(id);
        boolean manualDepartment = SOURCE_MANUAL.equalsIgnoreCase(department.getSyncSource()) || !isSyncManaged(department.getSyncManaged());
        if (manualDepartment) {
            if (dto.getParentId() != null && Objects.equals(dto.getParentId(), id)) {
                throw new IllegalArgumentException("\u4e0a\u7ea7\u90e8\u95e8\u4e0d\u80fd\u9009\u62e9\u81ea\u5df1");
            }
            if (dto.getParentId() != null) {
                requireDepartment(dto.getParentId());
            }
            validateDepartmentLeader(dto.getLeaderUserId());

            department.setDeptName(dto.getDeptName().trim());
            department.setParentId(dto.getParentId());
            department.setCompanyId(trimToNull(dto.getCompanyId()));
            department.setLeaderUserId(dto.getLeaderUserId());
            department.setSyncEnabled(dto.getSyncEnabled() != null && dto.getSyncEnabled() == 0 ? 0 : 1);
            department.setStatus(normalizeStatus(dto.getStatus()));
            department.setSortOrder(dto.getSortOrder() == null ? department.getSortOrder() : dto.getSortOrder());
            department.setSyncRemark("\u624b\u5de5\u7ef4\u62a4");
            department.setSyncStatus("MANUAL");
        }
        applyDepartmentStatBelong(department, dto);
        systemDepartmentMapper.updateById(department);
        return findDepartmentNode(id);
    }

    /**
     * 删除Department。
     */
    @Transactional(rollbackFor = Exception.class)
    protected Boolean deleteDepartment(Long id) {
        SystemDepartment department = requireDepartment(id);
        if (!SOURCE_MANUAL.equalsIgnoreCase(department.getSyncSource())) {
            throw new IllegalArgumentException("\u540c\u6b65\u90e8\u95e8\u4e0d\u80fd\u76f4\u63a5\u5220\u9664\uff0c\u8bf7\u901a\u8fc7\u540c\u6b65\u6e05\u7406");
        }
        long childCount = systemDepartmentMapper.selectCount(
                Wrappers.<SystemDepartment>lambdaQuery().eq(SystemDepartment::getParentId, id)
        );
        if (childCount > 0) {
            throw new IllegalArgumentException("\u8bf7\u5148\u5220\u9664\u4e0b\u7ea7\u90e8\u95e8");
        }
        long employeeCount = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getDeptId, id)
        );
        if (employeeCount > 0) {
            throw new IllegalArgumentException("\u8bf7\u5148\u8fc1\u79fb\u8be5\u90e8\u95e8\u4e0b\u5458\u5de5");
        }
        systemDepartmentMapper.deleteById(id);
        return Boolean.TRUE;
    }

    /**
     * 查询Employees列表。
     */
    protected List<EmployeeVO> listEmployees(EmployeeQueryDTO query) {
        EmployeeQueryDTO payload = query == null ? new EmployeeQueryDTO() : query;
        LambdaQueryWrapper<User> wrapper = Wrappers.<User>lambdaQuery()
                .orderByAsc(User::getId);
        if (StrUtil.isNotBlank(payload.getKeyword())) {
            String keyword = payload.getKeyword().trim();
            wrapper.and(item -> item.like(User::getUsername, keyword)
                    .or().like(User::getName, keyword)
                    .or().like(User::getPhone, keyword)
                    .or().like(User::getEmail, keyword));
        }
        if (StrUtil.isNotBlank(payload.getCompanyId())) {
            wrapper.eq(User::getCompanyId, payload.getCompanyId().trim());
        }
        if (payload.getDeptId() != null) {
            wrapper.eq(User::getDeptId, payload.getDeptId());
        }
        if (payload.getStatus() != null) {
            wrapper.eq(User::getStatus, payload.getStatus());
        }

        List<User> users = userMapper.selectList(wrapper);
        if (users.isEmpty()) {
            return List.of();
        }

        Set<String> companyIds = users.stream()
                .map(User::getCompanyId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        Map<String, String> companyNameMap = companyIds.isEmpty()
                ? Map.of()
                : systemCompanyMapper.selectList(
                Wrappers.<SystemCompany>lambdaQuery()
                        .in(SystemCompany::getCompanyId, companyIds)
        ).stream().collect(Collectors.toMap(SystemCompany::getCompanyId, SystemCompany::getCompanyName, (left, right) -> left));

        Set<Long> departmentIds = users.stream()
                .map(User::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> departmentNameMap = departmentIds.isEmpty()
                ? Map.of()
                : systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .in(SystemDepartment::getId, departmentIds)
        ).stream().collect(Collectors.toMap(SystemDepartment::getId, SystemDepartment::getDeptName, (left, right) -> left));

        Map<Long, List<String>> roleCodesByUserId = buildUserRoleCodeMap(users.stream().map(User::getId).toList());

        return users.stream().map(user -> {
            EmployeeVO vo = new EmployeeVO();
            vo.setUserId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setName(StrUtil.blankToDefault(user.getName(), user.getUsername()));
            vo.setPhone(user.getPhone());
            vo.setEmail(user.getEmail());
            vo.setCompanyId(user.getCompanyId());
            vo.setCompanyName(resolveMapValue(companyNameMap, user.getCompanyId(), ""));
            vo.setDeptId(user.getDeptId());
            vo.setDeptName(resolveMapValue(departmentNameMap, user.getDeptId(), ""));
            vo.setPosition(StrUtil.blankToDefault(user.getPosition(), ""));
            vo.setLaborRelationBelong(StrUtil.blankToDefault(user.getLaborRelationBelong(), ""));
            vo.setStatDepartmentBelong(StrUtil.blankToDefault(user.getStatDepartmentBelong(), ""));
            vo.setStatRegionBelong(StrUtil.blankToDefault(user.getStatRegionBelong(), ""));
            vo.setStatAreaBelong(StrUtil.blankToDefault(user.getStatAreaBelong(), ""));
            vo.setStatus(user.getStatus());
            vo.setSourceType(StrUtil.blankToDefault(user.getSourceType(), SOURCE_MANUAL));
            vo.setSyncManaged(isSyncManaged(user.getSyncManaged()));
            vo.setLastSyncAt(user.getLastSyncAt());
            vo.setRoleCodes(roleCodesByUserId.getOrDefault(user.getId(), List.of()));
            return vo;
        }).toList();
    }

    /**
     * 创建Employee。
     */
    @Transactional(rollbackFor = Exception.class)
    protected EmployeeVO createEmployee(EmployeeSaveDTO dto) {
        ensureUsernameUnique(dto.getUsername(), null);

        User user = new User();
        user.setUsername(dto.getUsername().trim());
        user.setPassword(DigestUtil.md5Hex("123456"));
        user.setName(dto.getName().trim());
        user.setPhone(trimToNull(dto.getPhone()));
        user.setEmail(normalizeEmail(dto.getEmail()));
        user.setCompanyId(trimToNull(dto.getCompanyId()));
        user.setDeptId(dto.getDeptId());
        user.setPosition(trimToNull(dto.getPosition()));
        user.setLaborRelationBelong(trimToNull(dto.getLaborRelationBelong()));
        applyEmployeeStatBelong(user, dto);
        user.setStatus(normalizeStatus(dto.getStatus()));
        user.setSourceType(SOURCE_MANUAL);
        user.setSyncManaged(0);
        userMapper.insert(user);
        return findEmployee(user.getId());
    }

    /**
     * 更新Employee。
     */
    @Transactional(rollbackFor = Exception.class)
    protected EmployeeVO updateEmployee(Long id, EmployeeSaveDTO dto) {
        User user = requireUser(id);
        if (isManualEmployee(user)) {
            ensureUsernameUnique(dto.getUsername(), id);
            user.setUsername(dto.getUsername().trim());
            user.setName(dto.getName().trim());
            user.setPhone(trimToNull(dto.getPhone()));
            user.setEmail(normalizeEmail(dto.getEmail()));
            user.setCompanyId(trimToNull(dto.getCompanyId()));
            user.setDeptId(dto.getDeptId());
            user.setPosition(trimToNull(dto.getPosition()));
            user.setLaborRelationBelong(trimToNull(dto.getLaborRelationBelong()));
            user.setStatus(normalizeStatus(dto.getStatus()));
        }
        applyEmployeeStatBelong(user, dto);
        userMapper.updateById(user);
        return findEmployee(id);
    }

    /**
     * 删除Employee。
     */
    @Transactional(rollbackFor = Exception.class)
    protected Boolean deleteEmployee(Long id) {
        User user = requireUser(id);
        if (!isManualEmployee(user)) {
            throw new IllegalArgumentException("\u540c\u6b65\u5458\u5de5\u4e0d\u80fd\u76f4\u63a5\u5220\u9664\uff0c\u8bf7\u901a\u8fc7\u540c\u6b65\u6e05\u7406");
        }
        long departmentLeaderCount = systemDepartmentMapper.selectCount(
                Wrappers.<SystemDepartment>lambdaQuery().eq(SystemDepartment::getLeaderUserId, id)
        );
        if (departmentLeaderCount > 0) {
            throw new IllegalArgumentException("\u8bf7\u5148\u8c03\u6574\u8be5\u5458\u5de5\u8d1f\u8d23\u7684\u90e8\u95e8\u8d1f\u8d23\u4eba");
        }
        systemUserRoleMapper.delete(Wrappers.<SystemUserRole>lambdaQuery().eq(SystemUserRole::getUserId, id));
        userMapper.deleteById(id);
        return Boolean.TRUE;
    }

    /**
     * 查询角色列表。
     */
    protected List<RoleVO> listRoles() {
        List<SystemRole> roles = systemRoleMapper.selectList(
                Wrappers.<SystemRole>lambdaQuery()
                        .orderByAsc(SystemRole::getId)
        );
        if (roles.isEmpty()) {
            return List.of();
        }

        Map<Long, List<String>> permissionCodesByRoleId = buildRolePermissionCodeMap(roles.stream().map(SystemRole::getId).toList());
        Map<Long, List<Long>> userIdsByRoleId = buildRoleUserIdMap(roles.stream().map(SystemRole::getId).toList());
        Set<Long> roleUserIds = userIdsByRoleId.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        Map<Long, String> userNameMap = roleUserIds.isEmpty()
                ? Map.of()
                : userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .in(User::getId, roleUserIds)
        ).stream().collect(Collectors.toMap(User::getId, user -> StrUtil.blankToDefault(user.getName(), user.getUsername()), (left, right) -> left));

        return roles.stream().map(role -> {
            RoleVO vo = new RoleVO();
            vo.setId(role.getId());
            vo.setRoleCode(role.getRoleCode());
            vo.setRoleName(role.getRoleName());
            vo.setRoleDescription(role.getRoleDescription());
            vo.setStatus(role.getStatus());
            vo.setPermissionCodes(permissionCodesByRoleId.getOrDefault(role.getId(), List.of()));
            List<Long> userIds = userIdsByRoleId.getOrDefault(role.getId(), List.of());
            vo.setUserIds(userIds);
            vo.setUserNames(userIds.stream().map(userId -> userNameMap.getOrDefault(userId, "\u7528\u6237#" + userId)).toList());
            return vo;
        }).toList();
    }

    /**
     * 创建角色。
     */
    @Transactional(rollbackFor = Exception.class)
    protected RoleVO createRole(RoleSaveDTO dto) {
        SystemRole role = new SystemRole();
        role.setRoleCode(generateRoleCode());
        role.setRoleName(dto.getRoleName().trim());
        role.setRoleDescription(trimToNull(dto.getRoleDescription()));
        role.setStatus(normalizeStatus(dto.getStatus()));
        systemRoleMapper.insert(role);
        return findRole(role.getId());
    }

    /**
     * 更新角色。
     */
    @Transactional(rollbackFor = Exception.class)
    protected RoleVO updateRole(Long id, RoleSaveDTO dto) {
        SystemRole role = requireRole(id);
        if (isSuperAdminRole(role)) {
            throw new IllegalArgumentException("\u8d85\u7ea7\u7ba1\u7406\u5458\u89d2\u8272\u4ec5\u5141\u8bb8\u901a\u8fc7\u6570\u636e\u5e93\u76f4\u63a5\u7ef4\u62a4\uff0c\u4e0d\u80fd\u5728\u6b64\u4fee\u6539\u3002");
        }
        role.setRoleName(dto.getRoleName().trim());
        role.setRoleDescription(trimToNull(dto.getRoleDescription()));
        role.setStatus(normalizeStatus(dto.getStatus()));
        systemRoleMapper.updateById(role);
        return findRole(id);
    }

    /**
     * 删除角色。
     */
    @Transactional(rollbackFor = Exception.class)
    protected Boolean deleteRole(Long id) {
        SystemRole role = requireRole(id);
        if (isSuperAdminRole(role)) {
            throw new IllegalArgumentException("\u8d85\u7ea7\u7ba1\u7406\u5458\u89d2\u8272\u4ec5\u5141\u8bb8\u901a\u8fc7\u6570\u636e\u5e93\u76f4\u63a5\u7ef4\u62a4\uff0c\u4e0d\u80fd\u5728\u6b64\u5220\u9664\u3002");
        }
        systemRolePermissionMapper.delete(Wrappers.<SystemRolePermission>lambdaQuery().eq(SystemRolePermission::getRoleId, id));
        systemUserRoleMapper.delete(Wrappers.<SystemUserRole>lambdaQuery().eq(SystemUserRole::getRoleId, id));
        systemRoleMapper.deleteById(id);
        return Boolean.TRUE;
    }

    /**
     * 处理系统系统设置中的这一步。
     */
    @Transactional(rollbackFor = Exception.class)
    protected Boolean assignRolePermissions(Long roleId, RolePermissionAssignDTO dto, Long currentUserId) {
        SystemRole role = requireRole(roleId);
        if (isSuperAdminRole(role) && !currentUserIsSuperAdmin(currentUserId)) {
            throw new IllegalArgumentException("\u8d85\u7ea7\u7ba1\u7406\u5458\u89d2\u8272\u6743\u9650\u4ec5\u5141\u8bb8\u8d85\u7ea7\u7ba1\u7406\u5458\u8c03\u6574\u3002");
        }
        systemRolePermissionMapper.delete(Wrappers.<SystemRolePermission>lambdaQuery().eq(SystemRolePermission::getRoleId, roleId));
        List<String> permissionCodes = dto == null || dto.getPermissionCodes() == null ? List.of() : dto.getPermissionCodes();
        if (permissionCodes.isEmpty()) {
            return Boolean.TRUE;
        }
        Map<String, SystemPermission> permissionMap = systemPermissionMapper.selectList(
                Wrappers.<SystemPermission>lambdaQuery().in(SystemPermission::getPermissionCode, permissionCodes)
        ).stream().collect(Collectors.toMap(SystemPermission::getPermissionCode, Function.identity(), (left, right) -> left));
        for (String permissionCode : permissionCodes) {
            SystemPermission permission = permissionMap.get(permissionCode);
            if (permission == null) {
                continue;
            }
            SystemRolePermission rolePermission = new SystemRolePermission();
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permission.getId());
            systemRolePermissionMapper.insert(rolePermission);
        }
        return Boolean.TRUE;
    }

    /**
     * 处理系统系统设置中的这一步。
     */
    @Transactional(rollbackFor = Exception.class)
    protected Boolean assignUserRoles(Long userId, UserRoleAssignDTO dto) {
        requireUser(userId);
        Long superAdminRoleId = findSuperAdminRoleId();
        List<Long> roleIds = dto == null || dto.getRoleIds() == null
                ? List.of()
                : dto.getRoleIds().stream().filter(Objects::nonNull).distinct().toList();
        if (superAdminRoleId != null && roleIds.contains(superAdminRoleId)) {
            throw new IllegalArgumentException("\u8d85\u7ea7\u7ba1\u7406\u5458\u89d2\u8272\u4ec5\u5141\u8bb8\u901a\u8fc7\u6570\u636e\u5e93\u76f4\u63a5\u7ef4\u62a4\uff0c\u4e0d\u80fd\u5728\u6b64\u5206\u914d\u3002");
        }
        if (superAdminRoleId != null) {
            systemUserRoleMapper.delete(
                    Wrappers.<SystemUserRole>lambdaQuery()
                            .eq(SystemUserRole::getUserId, userId)
                            .ne(SystemUserRole::getRoleId, superAdminRoleId)
            );
        } else {
            systemUserRoleMapper.delete(Wrappers.<SystemUserRole>lambdaQuery().eq(SystemUserRole::getUserId, userId));
        }
        for (Long roleId : roleIds) {
            SystemRole role = requireRole(roleId);
            if (isSuperAdminRole(role)) {
                throw new IllegalArgumentException("\u8d85\u7ea7\u7ba1\u7406\u5458\u89d2\u8272\u4ec5\u5141\u8bb8\u901a\u8fc7\u6570\u636e\u5e93\u76f4\u63a5\u7ef4\u62a4\uff0c\u4e0d\u80fd\u5728\u6b64\u5206\u914d\u3002");
            }
            SystemUserRole userRole = new SystemUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            systemUserRoleMapper.insert(userRole);
        }
        return Boolean.TRUE;
    }

    /**
     * 获取权限Tree。
     */
    protected List<PermissionTreeNodeVO> getPermissionTree() {
        List<SystemPermission> permissions = systemPermissionMapper.selectList(
                Wrappers.<SystemPermission>lambdaQuery()
                        .eq(SystemPermission::getStatus, 1)
                        .orderByAsc(SystemPermission::getSortOrder, SystemPermission::getId)
        );
        if (permissions.isEmpty()) {
            return List.of();
        }
        Map<Long, PermissionTreeNodeVO> nodeMap = new LinkedHashMap<>();
        List<PermissionTreeNodeVO> roots = new ArrayList<>();
        for (SystemPermission permission : permissions) {
            PermissionTreeNodeVO node = new PermissionTreeNodeVO();
            node.setId(permission.getId());
            node.setPermissionCode(permission.getPermissionCode());
            node.setPermissionName(permission.getPermissionName());
            node.setPermissionType(permission.getPermissionType());
            node.setParentId(permission.getParentId());
            node.setModuleCode(permission.getModuleCode());
            node.setRoutePath(permission.getRoutePath());
            node.setSortOrder(permission.getSortOrder());
            nodeMap.put(node.getId(), node);
        }
        for (PermissionTreeNodeVO node : nodeMap.values()) {
            if (node.getParentId() == null || !nodeMap.containsKey(node.getParentId())) {
                roots.add(node);
            } else {
                nodeMap.get(node.getParentId()).getChildren().add(node);
            }
        }
        return roots;
    }

    /**
     * 查询Companies列表。
     */
    protected List<CompanyVO> listCompanies() {
        List<SystemCompany> companies = systemCompanyMapper.selectList(
                Wrappers.<SystemCompany>lambdaQuery()
                        .orderByAsc(SystemCompany::getCompanyCode)
        );
        return buildCompanyList(companies);
    }

    /**
     * 创建公司。
     */
    @Transactional(rollbackFor = Exception.class)
    protected CompanyVO createCompany(CompanySaveDTO dto) {
        CompanyCodeBundle codeBundle = generateCompanyCodes();
        SystemCompany company = new SystemCompany();
        company.setCompanyId(codeBundle.companyId());
        company.setCompanyCode(codeBundle.companyCode());
        applyCompany(company, dto);
        systemCompanyMapper.insert(company);
        return findCompany(company.getCompanyId());
    }

    /**
     * 更新公司。
     */
    @Transactional(rollbackFor = Exception.class)
    protected CompanyVO updateCompany(String companyId, CompanySaveDTO dto) {
        SystemCompany company = requireCompany(companyId);
        applyCompany(company, dto);
        company.setCompanyId(companyId);
        systemCompanyMapper.updateById(company);
        return findCompany(companyId);
    }

    /**
     * 删除公司。
     */
    @Transactional(rollbackFor = Exception.class)
    protected Boolean deleteCompany(String companyId) {
        requireCompany(companyId);
        long departmentCount = systemDepartmentMapper.selectCount(
                Wrappers.<SystemDepartment>lambdaQuery().eq(SystemDepartment::getCompanyId, companyId)
        );
        if (departmentCount > 0) {
            throw new IllegalArgumentException("\u8bf7\u5148\u5220\u9664\u8be5\u516c\u53f8\u4e0b\u90e8\u95e8");
        }
        long userCount = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getCompanyId, companyId)
        );
        if (userCount > 0) {
            throw new IllegalArgumentException("\u8bf7\u5148\u8fc1\u79fb\u8be5\u516c\u53f8\u4e0b\u5458\u5de5");
        }
        long companyBankAccountCount = systemCompanyBankAccountMapper.selectCount(
                Wrappers.<SystemCompanyBankAccount>lambdaQuery().eq(SystemCompanyBankAccount::getCompanyId, companyId)
        );
        if (companyBankAccountCount > 0) {
            throw new IllegalArgumentException("\u8bf7\u5148\u5220\u9664\u8be5\u516c\u53f8\u4e0b\u94f6\u884c\u8d26\u6237");
        }
        systemCompanyMapper.deleteById(companyId);
        return Boolean.TRUE;
    }

    /**
     * 查询公司银行账户列表。
     */
    protected List<CompanyBankAccountVO> listCompanyBankAccounts() {
        List<SystemCompanyBankAccount> accounts = systemCompanyBankAccountMapper.selectList(
                Wrappers.<SystemCompanyBankAccount>lambdaQuery()
                        .orderByAsc(SystemCompanyBankAccount::getCompanyId)
                        .orderByDesc(SystemCompanyBankAccount::getDefaultAccount)
                        .orderByAsc(SystemCompanyBankAccount::getId)
        );
        if (accounts.isEmpty()) {
            return List.of();
        }
        Map<String, String> companyNameMap = buildCompanyNameMap(accounts.stream()
                .map(SystemCompanyBankAccount::getCompanyId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet()));
        return accounts.stream()
                .map(account -> toCompanyBankAccountVo(account, companyNameMap))
                .toList();
    }

    /**
     * 创建公司银行账户。
     */
    @Transactional(rollbackFor = Exception.class)
    protected CompanyBankAccountVO createCompanyBankAccount(CompanyBankAccountSaveDTO dto) {
        SystemCompanyBankAccount account = new SystemCompanyBankAccount();
        applyCompanyBankAccount(account, dto);
        ensureCompanyBankAccountUnique(account.getCompanyId(), account.getAccountNo(), null);
        systemCompanyBankAccountMapper.insert(account);
        if (isDefaultAccount(account)) {
            clearOtherDefaultCompanyBankAccounts(account.getCompanyId(), account.getId());
        }
        return findCompanyBankAccount(account.getId());
    }

    /**
     * 更新公司银行账户。
     */
    @Transactional(rollbackFor = Exception.class)
    protected CompanyBankAccountVO updateCompanyBankAccount(Long id, CompanyBankAccountSaveDTO dto) {
        SystemCompanyBankAccount account = requireCompanyBankAccount(id);
        applyCompanyBankAccount(account, dto);
        ensureCompanyBankAccountUnique(account.getCompanyId(), account.getAccountNo(), id);
        systemCompanyBankAccountMapper.updateById(account);
        if (isDefaultAccount(account)) {
            clearOtherDefaultCompanyBankAccounts(account.getCompanyId(), account.getId());
        }
        return findCompanyBankAccount(id);
    }

    /**
     * 删除公司银行账户。
     */
    @Transactional(rollbackFor = Exception.class)
    protected Boolean deleteCompanyBankAccount(Long id) {
        requireCompanyBankAccount(id);
        systemCompanyBankAccountMapper.deleteById(id);
        return Boolean.TRUE;
    }

    /**
     * 查询同步Connectors列表。
     */
    protected List<SyncConnectorVO> listSyncConnectors() {
        ensureDefaultConnectors();
        return systemSyncConnectorMapper.selectList(
                Wrappers.<SystemSyncConnector>lambdaQuery().orderByAsc(SystemSyncConnector::getId)
        ).stream().map(this::toSyncConnectorVo).toList();
    }

    /**
     * 更新同步Connector。
     */
    @Transactional(rollbackFor = Exception.class)
    protected SyncConnectorVO updateSyncConnector(SyncConnectorSaveDTO dto) {
        ensureDefaultConnectors();
        String platformCode = dto.getPlatformCode().trim().toUpperCase(Locale.ROOT);
        validateConnectorSave(platformCode, dto);
        SystemSyncConnector connector = systemSyncConnectorMapper.selectOne(
                Wrappers.<SystemSyncConnector>lambdaQuery()
                        .eq(SystemSyncConnector::getPlatformCode, platformCode)
                        .last("limit 1")
        );
        if (connector == null) {
            connector = new SystemSyncConnector();
            connector.setPlatformCode(platformCode);
            connector.setPlatformName(resolvePlatformName(platformCode));
            systemSyncConnectorMapper.insert(connector);
        }
        connector.setEnabled(dto.getEnabled() != null && dto.getEnabled() == 0 ? 0 : 1);
        connector.setAutoSyncEnabled(dto.getAutoSyncEnabled() != null && dto.getAutoSyncEnabled() == 1 ? 1 : 0);
        connector.setSyncIntervalMinutes(dto.getSyncIntervalMinutes() == null || dto.getSyncIntervalMinutes() <= 0 ? 60 : dto.getSyncIntervalMinutes());
        connector.setConfigJson(writeConnectorConfig(platformCode, dto));
        connector.setPlatformName(resolvePlatformName(platformCode));
        systemSyncConnectorMapper.updateById(connector);
        return toSyncConnectorVo(requireConnector(platformCode));
    }

    /**
     * 查询同步Jobs列表。
     */
    protected List<SyncJobVO> listSyncJobs() {
        return systemSyncJobMapper.selectList(
                Wrappers.<SystemSyncJob>lambdaQuery()
                        .orderByDesc(SystemSyncJob::getCreatedAt, SystemSyncJob::getId)
                        .last("limit 20")
        ).stream().map(this::toSyncJobVo).toList();
    }

    /**
     * 执行同步。
     */
    protected SyncJobVO runSync(SyncRunDTO dto, String operator) {
        ensureDefaultConnectors();
        List<String> platformCodes = dto == null || dto.getPlatformCodes() == null || dto.getPlatformCodes().isEmpty()
                ? List.of(PLATFORM_DINGTALK, PLATFORM_WECOM, PLATFORM_FEISHU)
                : dto.getPlatformCodes().stream()
                .filter(StrUtil::isNotBlank)
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();

        SyncJobVO lastJob = null;
        for (String platformCode : platformCodes) {
            lastJob = executeSync(requireConnector(platformCode), normalizeTriggerType(dto == null ? null : dto.getTriggerType()), operator);
        }
        if (lastJob == null) {
            throw new IllegalArgumentException("\u672a\u627e\u5230\u53ef\u6267\u884c\u7684\u540c\u6b65\u5e73\u53f0");
        }
        return lastJob;
    }

    /**
     * 执行Due同步Jobs。
     */
    protected void runDueSyncJobs() {
        ensureDefaultConnectors();
        List<SystemSyncConnector> connectors = systemSyncConnectorMapper.selectList(
                Wrappers.<SystemSyncConnector>lambdaQuery()
                        .eq(SystemSyncConnector::getEnabled, 1)
                        .eq(SystemSyncConnector::getAutoSyncEnabled, 1)
        );
        LocalDateTime now = LocalDateTime.now();
        for (SystemSyncConnector connector : connectors) {
            int intervalMinutes = connector.getSyncIntervalMinutes() == null || connector.getSyncIntervalMinutes() <= 0
                    ? 60
                    : connector.getSyncIntervalMinutes();
            if (connector.getLastSyncAt() != null && connector.getLastSyncAt().plusMinutes(intervalMinutes).isAfter(now)) {
                continue;
            }
            executeSync(connector, "AUTO", "system-scheduler");
        }
    }

    /**
     * 执行同步。
     */
    private SyncJobVO executeSync(SystemSyncConnector connector, String triggerType, String operator) {
        if (connector.getEnabled() == null || connector.getEnabled() != 1) {
            throw new IllegalArgumentException(resolvePlatformName(connector.getPlatformCode()) + " \u540c\u6b65\u672a\u542f\u7528");
        }

        SystemSyncJob job = new SystemSyncJob();
        job.setJobNo(buildJobNo(connector.getPlatformCode()));
        job.setPlatformCode(connector.getPlatformCode());
        job.setTriggerType(triggerType);
        job.setStatus(JOB_STATUS_RUNNING);
        job.setStartedAt(LocalDateTime.now());
        job.setSuccessCount(0);
        job.setSkippedCount(0);
        job.setFailedCount(0);
        job.setDeletedCount(0);
        job.setSummary("\u540c\u6b65\u6267\u884c\u4e2d");
        systemSyncJobMapper.insert(job);

        SyncStats stats = new SyncStats();
        try {
            validateConnectorRunConfig(connector.getPlatformCode(), readConnectorConfig(connector.getConfigJson()));
            OrganizationSyncAdapter adapter = requireSyncAdapter(connector.getPlatformCode());
            ExternalSyncPayload payload = adapter.pull(connector);
            processDepartments(job.getId(), connector.getPlatformCode(), payload.getDepartments(), stats);
            processEmployees(job.getId(), connector.getPlatformCode(), payload.getEmployees(), stats);
            cleanupMissingEmployees(job.getId(), connector.getPlatformCode(), payload.getEmployees(), stats);
            cleanupMissingDepartments(job.getId(), connector.getPlatformCode(), payload.getDepartments(), stats);

            connector.setLastSyncAt(LocalDateTime.now());
            connector.setLastSyncStatus(JOB_STATUS_SUCCESS);
            connector.setLastSyncMessage(buildJobSummary(stats));
            systemSyncConnectorMapper.updateById(connector);

            job.setFinishedAt(LocalDateTime.now());
            job.setStatus(JOB_STATUS_SUCCESS);
            job.setSuccessCount(stats.successCount.get());
            job.setSkippedCount(stats.skippedCount.get());
            job.setFailedCount(stats.failedCount.get());
            job.setDeletedCount(stats.deletedCount.get());
            job.setSummary(buildJobSummary(stats));
            systemSyncJobMapper.updateById(job);
            return toSyncJobVo(job);
        } catch (Exception ex) {
            connector.setLastSyncAt(LocalDateTime.now());
            connector.setLastSyncStatus(JOB_STATUS_FAILED);
            connector.setLastSyncMessage(ex.getMessage());
            systemSyncConnectorMapper.updateById(connector);

            job.setFinishedAt(LocalDateTime.now());
            job.setStatus(JOB_STATUS_FAILED);
            job.setSuccessCount(stats.successCount.get());
            job.setSkippedCount(stats.skippedCount.get());
            job.setFailedCount(stats.failedCount.get() + 1);
            job.setDeletedCount(stats.deletedCount.get());
            job.setSummary(StrUtil.blankToDefault(ex.getMessage(), "\u540c\u6b65\u6267\u884c\u5931\u8d25"));
            systemSyncJobMapper.updateById(job);
            appendJobDetail(job.getId(), "JOB", "FAIL", connector.getPlatformCode(), DETAIL_STATUS_FAILED, ex.getMessage());
            return toSyncJobVo(job);
        }
    }

    private void processDepartments(Long jobId, String platformCode, List<ExternalDepartmentData> externalDepartments, SyncStats stats) {
        List<SystemDepartment> existingDepartments = systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .orderByAsc(SystemDepartment::getSortOrder, SystemDepartment::getId)
        );
        Map<String, SystemDepartment> existingByCode = existingDepartments.stream()
                .filter(department -> StrUtil.isNotBlank(department.getDeptCode()))
                .collect(Collectors.toMap(SystemDepartment::getDeptCode, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        Map<String, SystemDepartment> workingMap = new LinkedHashMap<>(existingByCode);
        for (ExternalDepartmentData externalDepartment : externalDepartments) {
            if (StrUtil.isBlank(externalDepartment.getDeptCode())) {
                stats.failedCount.incrementAndGet();
                appendJobDetail(jobId, DETAIL_TYPE_DEPARTMENT, "UPSERT", "", DETAIL_STATUS_FAILED, "\u90e8\u95e8\u7f16\u7801\u4e0d\u80fd\u4e3a\u7a7a\uff0c\u5df2\u6807\u8bb0\u4e3a\u5931\u8d25");
                continue;
            }
            SystemDepartment existing = workingMap.get(externalDepartment.getDeptCode());
            if (existing != null && SOURCE_MANUAL.equalsIgnoreCase(existing.getSyncSource()) && !isSyncManaged(existing.getSyncManaged())) {
                stats.skippedCount.incrementAndGet();
                appendJobDetail(jobId, DETAIL_TYPE_DEPARTMENT, "UPSERT", externalDepartment.getDeptCode(), DETAIL_STATUS_SKIPPED, "\u624b\u5de5\u7ef4\u62a4\u90e8\u95e8\u5df2\u5b58\u5728\uff0c\u5df2\u8df3\u8fc7\u81ea\u52a8\u540c\u6b65\u8986\u76d6");
                continue;
            }

            SystemDepartment target = existing == null ? new SystemDepartment() : existing;
            target.setDeptCode(externalDepartment.getDeptCode());
            target.setDeptName(externalDepartment.getDeptName());
            target.setSyncSource(platformCode);
            target.setSyncEnabled(1);
            target.setSyncManaged(1);
            target.setSyncStatus(JOB_STATUS_SUCCESS);
            target.setSyncRemark("\u6765\u6e90\u4e8e " + resolvePlatformName(platformCode) + " \u81ea\u52a8\u540c\u6b65");
            target.setLastSyncAt(LocalDateTime.now());
            target.setStatus(normalizeStatus(externalDepartment.getStatus()));
            target.setSortOrder(target.getSortOrder() == null ? nextDepartmentSortOrder() : target.getSortOrder());
            applyDepartmentExternalId(target, platformCode, externalDepartment.getExternalId());

            if (target.getId() == null) {
                systemDepartmentMapper.insert(target);
                workingMap.put(target.getDeptCode(), target);
            } else {
                systemDepartmentMapper.updateById(target);
            }
            stats.successCount.incrementAndGet();
            appendJobDetail(jobId, DETAIL_TYPE_DEPARTMENT, "UPSERT", target.getDeptCode(), DETAIL_STATUS_SUCCESS, "\u90e8\u95e8\u540c\u6b65\u6210\u529f");
        }

        Map<String, SystemDepartment> latestByCode = systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .orderByAsc(SystemDepartment::getSortOrder, SystemDepartment::getId)
        ).stream().filter(item -> StrUtil.isNotBlank(item.getDeptCode()))
                .collect(Collectors.toMap(SystemDepartment::getDeptCode, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        for (ExternalDepartmentData externalDepartment : externalDepartments) {
            SystemDepartment current = latestByCode.get(externalDepartment.getDeptCode());
            if (current == null) {
                continue;
            }
            Long parentId = null;
            if (StrUtil.isNotBlank(externalDepartment.getParentDeptCode())) {
                SystemDepartment parent = latestByCode.get(externalDepartment.getParentDeptCode());
                if (parent != null) {
                    parentId = parent.getId();
                }
            }
            if (!Objects.equals(current.getParentId(), parentId)) {
                current.setParentId(parentId);
                systemDepartmentMapper.updateById(current);
            }
        }
    }

    private void processEmployees(Long jobId, String platformCode, List<ExternalEmployeeData> externalEmployees, SyncStats stats) {
        List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery().orderByAsc(User::getId));
        Map<String, List<User>> phoneMap = users.stream()
                .filter(user -> StrUtil.isNotBlank(user.getPhone()))
                .collect(Collectors.groupingBy(user -> normalizePhone(user.getPhone())));
        Map<String, List<User>> emailMap = users.stream()
                .filter(user -> StrUtil.isNotBlank(user.getEmail()))
                .collect(Collectors.groupingBy(user -> normalizeEmail(user.getEmail())));
        Map<String, Long> departmentIdByCode = systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery().orderByAsc(SystemDepartment::getId)
        ).stream().filter(item -> StrUtil.isNotBlank(item.getDeptCode()))
                .collect(Collectors.toMap(SystemDepartment::getDeptCode, SystemDepartment::getId, (left, right) -> left));

        for (ExternalEmployeeData externalEmployee : externalEmployees) {
            MatchResult matchResult = matchEmployee(phoneMap, emailMap, externalEmployee);
            if (matchResult.conflictMessage != null) {
                stats.failedCount.incrementAndGet();
                appendJobDetail(jobId, DETAIL_TYPE_EMPLOYEE, "UPSERT", resolveEmployeeBusinessKey(externalEmployee), DETAIL_STATUS_FAILED, matchResult.conflictMessage);
                continue;
            }

            User matchedUser = matchResult.user;
            if (matchedUser != null && SOURCE_MANUAL.equalsIgnoreCase(matchedUser.getSourceType()) && !isSyncManaged(matchedUser.getSyncManaged())) {
                stats.skippedCount.incrementAndGet();
                appendJobDetail(jobId, DETAIL_TYPE_EMPLOYEE, "UPSERT", resolveEmployeeBusinessKey(externalEmployee), DETAIL_STATUS_SKIPPED, "\u624b\u5de5\u7ef4\u62a4\u5458\u5de5\u5df2\u5b58\u5728\uff0c\u5df2\u8df3\u8fc7\u81ea\u52a8\u540c\u6b65\u8986\u76d6");
                continue;
            }

            User target = matchedUser == null ? new User() : matchedUser;
            if (target.getId() == null) {
                target.setUsername(buildSyncUsername(externalEmployee, platformCode));
                target.setPassword(DigestUtil.md5Hex("123456"));
            }
            target.setName(StrUtil.blankToDefault(externalEmployee.getName(), target.getUsername()));
            target.setPhone(trimToNull(externalEmployee.getPhone()));
            target.setEmail(normalizeEmail(externalEmployee.getEmail()));
            target.setDeptId(departmentIdByCode.get(externalEmployee.getDeptCode()));
            target.setPosition(trimToNull(externalEmployee.getPosition()));
            target.setLaborRelationBelong(trimToNull(externalEmployee.getLaborRelationBelong()));
            target.setStatus(normalizeStatus(externalEmployee.getStatus()));
            target.setSourceType(platformCode);
            target.setSyncManaged(1);
            target.setLastSyncAt(LocalDateTime.now());
            applyUserExternalId(target, platformCode, externalEmployee.getExternalId());

            if (target.getId() == null) {
                userMapper.insert(target);
                users.add(target);
                if (StrUtil.isNotBlank(target.getPhone())) {
                    phoneMap.computeIfAbsent(normalizePhone(target.getPhone()), key -> new ArrayList<>()).add(target);
                }
                if (StrUtil.isNotBlank(target.getEmail())) {
                    emailMap.computeIfAbsent(normalizeEmail(target.getEmail()), key -> new ArrayList<>()).add(target);
                }
            } else {
                userMapper.updateById(target);
            }
            stats.successCount.incrementAndGet();
            appendJobDetail(jobId, DETAIL_TYPE_EMPLOYEE, "UPSERT", resolveEmployeeBusinessKey(externalEmployee), DETAIL_STATUS_SUCCESS, "\u5458\u5de5\u540c\u6b65\u6210\u529f");
        }
    }

    private void cleanupMissingEmployees(Long jobId, String platformCode, List<ExternalEmployeeData> externalEmployees, SyncStats stats) {
        Set<String> externalIds = externalEmployees.stream()
                .map(ExternalEmployeeData::getExternalId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        List<User> managedUsers = userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getSourceType, platformCode)
                        .eq(User::getSyncManaged, 1)
        );
        for (User user : managedUsers) {
            String externalId = getUserExternalId(user, platformCode);
            if (StrUtil.isBlank(externalId) || externalIds.contains(externalId)) {
                continue;
            }
            systemUserRoleMapper.delete(Wrappers.<SystemUserRole>lambdaQuery().eq(SystemUserRole::getUserId, user.getId()));
            userMapper.deleteById(user.getId());
            stats.deletedCount.incrementAndGet();
            appendJobDetail(jobId, DETAIL_TYPE_EMPLOYEE, "DELETE", String.valueOf(user.getId()), DETAIL_STATUS_DELETED, "\u6765\u6e90\u4e8e " + resolvePlatformName(platformCode) + " \u81ea\u52a8\u540c\u6b65\u7684\u5458\u5de5\u4e0d\u5b58\u5728\u4e8e\u4e0a\u6e38\uff0c\u5df2\u81ea\u52a8\u5220\u9664");
        }
    }

    private void cleanupMissingDepartments(Long jobId, String platformCode, List<ExternalDepartmentData> externalDepartments, SyncStats stats) {
        Set<String> departmentCodes = externalDepartments.stream()
                .map(ExternalDepartmentData::getDeptCode)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        List<SystemDepartment> managedDepartments = systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .eq(SystemDepartment::getSyncSource, platformCode)
                        .eq(SystemDepartment::getSyncManaged, 1)
                        .orderByDesc(SystemDepartment::getId)
        );
        for (SystemDepartment department : managedDepartments) {
            if (departmentCodes.contains(department.getDeptCode())) {
                continue;
            }
            boolean deleted = deleteManagedDepartmentRecursively(jobId, department, stats);
            if (!deleted) {
                stats.skippedCount.incrementAndGet();
            }
        }
    }

    /**
     * 删除ManagedDepartmentRecursively。
     */
    private boolean deleteManagedDepartmentRecursively(Long jobId, SystemDepartment department, SyncStats stats) {
        List<SystemDepartment> children = systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery().eq(SystemDepartment::getParentId, department.getId())
        );
        for (SystemDepartment child : children) {
            if (SOURCE_MANUAL.equalsIgnoreCase(child.getSyncSource()) && !isSyncManaged(child.getSyncManaged())) {
                appendJobDetail(jobId, DETAIL_TYPE_DEPARTMENT, "DELETE", department.getDeptCode(), DETAIL_STATUS_SKIPPED, "\u90e8\u95e8\u4e0b\u5b58\u5728\u624b\u5de5\u7ef4\u62a4\u7684\u4e0b\u7ea7\u90e8\u95e8\uff0c\u5df2\u8df3\u8fc7\u5220\u9664");
                return false;
            }
            if (!deleteManagedDepartmentRecursively(jobId, child, stats)) {
                return false;
            }
        }

        List<User> users = userMapper.selectList(
            Wrappers.<User>lambdaQuery().eq(User::getDeptId, department.getId())
        );
        for (User user : users) {
            if (SOURCE_MANUAL.equalsIgnoreCase(user.getSourceType()) && !isSyncManaged(user.getSyncManaged())) {
                appendJobDetail(jobId, DETAIL_TYPE_DEPARTMENT, "DELETE", department.getDeptCode(), DETAIL_STATUS_SKIPPED, "\u90e8\u95e8\u4e0b\u5b58\u5728\u624b\u5de5\u7ef4\u62a4\u5458\u5de5\uff0c\u5df2\u8df3\u8fc7\u5220\u9664");
                return false;
            }
        }
        for (User user : users) {
            systemUserRoleMapper.delete(Wrappers.<SystemUserRole>lambdaQuery().eq(SystemUserRole::getUserId, user.getId()));
            userMapper.deleteById(user.getId());
            stats.deletedCount.incrementAndGet();
            appendJobDetail(jobId, DETAIL_TYPE_EMPLOYEE, "DELETE", String.valueOf(user.getId()), DETAIL_STATUS_DELETED, "\u5220\u9664\u90e8\u95e8\u65f6\u5df2\u540c\u6b65\u5220\u9664\u6240\u5c5e\u5458\u5de5");
        }

        systemDepartmentMapper.deleteById(department.getId());
        stats.deletedCount.incrementAndGet();
        appendJobDetail(jobId, DETAIL_TYPE_DEPARTMENT, "DELETE", department.getDeptCode(), DETAIL_STATUS_DELETED, "\u6765\u6e90\u4e8e " + resolvePlatformName(department.getSyncSource()) + " \u81ea\u52a8\u540c\u6b65\u7684\u90e8\u95e8\u4e0d\u5b58\u5728\u4e8e\u4e0a\u6e38\uff0c\u5df2\u81ea\u52a8\u5220\u9664");
        return true;
    }

    /**
     * 组装当前用户。
     */
    private UserProfileVO buildCurrentUser(Long userId) {
        User user = requireUser(userId);
        UserProfileVO currentUser = new UserProfileVO();
        currentUser.setUserId(user.getId());
        currentUser.setUsername(user.getUsername());
        currentUser.setName(StrUtil.blankToDefault(user.getName(), user.getUsername()));
        currentUser.setPhone(user.getPhone());
        currentUser.setEmail(user.getEmail());
        currentUser.setPosition(StrUtil.blankToDefault(user.getPosition(), "\u672a\u8bbe\u7f6e\u5c97\u4f4d"));
        currentUser.setLaborRelationBelong(StrUtil.blankToDefault(user.getLaborRelationBelong(), ""));
        currentUser.setCompanyId(user.getCompanyId());
        currentUser.setRoles(accessControlService.getRoleCodes(userId));
        currentUser.setPermissionCodes(accessControlService.getPermissionCodes(userId));
        return currentUser;
    }

    /**
     * 组装DepartmentTree。
     */
    private List<DepartmentTreeNodeVO> buildDepartmentTree(List<SystemDepartment> departments) {
        if (departments == null || departments.isEmpty()) {
            return List.of();
        }
        Map<Long, String> leaderNameMap = buildUserDisplayNameMap(
                departments.stream()
                        .map(SystemDepartment::getLeaderUserId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        );
        Map<Long, DepartmentTreeNodeVO> nodeMap = new LinkedHashMap<>();
        List<DepartmentTreeNodeVO> roots = new ArrayList<>();
        for (SystemDepartment department : departments) {
            nodeMap.put(department.getId(), toDepartmentNode(department, leaderNameMap));
        }
        for (SystemDepartment department : departments) {
            DepartmentTreeNodeVO node = nodeMap.get(department.getId());
            if (department.getParentId() == null || !nodeMap.containsKey(department.getParentId())) {
                roots.add(node);
            } else {
                nodeMap.get(department.getParentId()).getChildren().add(node);
            }
        }
        return roots;
    }

    /**
     * 组装公司列表。
     */
    private List<CompanyVO> buildCompanyList(List<SystemCompany> companies) {
        if (companies == null || companies.isEmpty()) {
            return List.of();
        }
        List<CompanyVO> result = new ArrayList<>();
        for (SystemCompany company : companies) {
            CompanyVO vo = new CompanyVO();
            vo.setCompanyId(company.getCompanyId());
            vo.setCompanyCode(company.getCompanyCode());
            vo.setCompanyName(company.getCompanyName());
            vo.setInvoiceTitle(company.getInvoiceTitle());
            vo.setTaxNo(company.getTaxNo());
            vo.setStatus(company.getStatus());
            vo.setChildren(new ArrayList<>());
            result.add(vo);
        }
        return result;
    }

    /**
     * 组装公司Name映射。
     */
    private Map<String, String> buildCompanyNameMap(Set<String> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Map.of();
        }
        return systemCompanyMapper.selectList(
                Wrappers.<SystemCompany>lambdaQuery().in(SystemCompany::getCompanyId, companyIds)
        ).stream().collect(Collectors.toMap(
                SystemCompany::getCompanyId,
                SystemCompany::getCompanyName,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private CompanyBankAccountVO toCompanyBankAccountVo(SystemCompanyBankAccount account, Map<String, String> companyNameMap) {
        CompanyBankAccountVO vo = new CompanyBankAccountVO();
        vo.setId(account.getId());
        vo.setCompanyId(account.getCompanyId());
        vo.setCompanyName(resolveMapValue(companyNameMap, account.getCompanyId(), ""));
        vo.setBankName(account.getBankName());
        vo.setProvince(account.getProvince());
        vo.setCity(account.getCity());
        vo.setBranchName(account.getBranchName());
        vo.setBankCode(account.getBankCode());
        vo.setBranchCode(account.getBranchCode());
        vo.setCnapsCode(account.getCnapsCode());
        vo.setAccountName(account.getAccountName());
        vo.setAccountNo(account.getAccountNo());
        vo.setAccountType(account.getAccountType());
        vo.setAccountUsage(account.getAccountUsage());
        vo.setCurrencyCode(account.getCurrencyCode());
        vo.setDefaultAccount(account.getDefaultAccount());
        vo.setStatus(account.getStatus());
        vo.setRemark(account.getRemark());
        vo.setDirectConnectEnabled(account.getDirectConnectEnabled());
        vo.setDirectConnectProvider(account.getDirectConnectProvider());
        vo.setDirectConnectChannel(account.getDirectConnectChannel());
        vo.setDirectConnectProtocol(account.getDirectConnectProtocol());
        vo.setDirectConnectCustomerNo(account.getDirectConnectCustomerNo());
        vo.setDirectConnectAppId(account.getDirectConnectAppId());
        vo.setDirectConnectAccountAlias(account.getDirectConnectAccountAlias());
        vo.setDirectConnectAuthMode(account.getDirectConnectAuthMode());
        vo.setDirectConnectApiBaseUrl(account.getDirectConnectApiBaseUrl());
        vo.setDirectConnectCertRef(account.getDirectConnectCertRef());
        vo.setDirectConnectSecretRef(account.getDirectConnectSecretRef());
        vo.setDirectConnectSignType(account.getDirectConnectSignType());
        vo.setDirectConnectEncryptType(account.getDirectConnectEncryptType());
        vo.setDirectConnectLastSyncAt(account.getDirectConnectLastSyncAt());
        vo.setDirectConnectLastSyncStatus(account.getDirectConnectLastSyncStatus());
        vo.setDirectConnectLastErrorMsg(account.getDirectConnectLastErrorMsg());
        vo.setDirectConnectExtJson(account.getDirectConnectExtJson());
        vo.setCreatedAt(account.getCreatedAt());
        vo.setUpdatedAt(account.getUpdatedAt());
        return vo;
    }

    private DepartmentTreeNodeVO toDepartmentNode(SystemDepartment department, Map<Long, String> leaderNameMap) {
        DepartmentTreeNodeVO node = new DepartmentTreeNodeVO();
        node.setId(department.getId());
        node.setCompanyId(department.getCompanyId());
        node.setDeptCode(department.getDeptCode());
        node.setLeaderUserId(department.getLeaderUserId());
        node.setLeaderName(resolveMapValue(leaderNameMap, department.getLeaderUserId(), ""));
        node.setDeptName(department.getDeptName());
        node.setParentId(department.getParentId());
        node.setSyncSource(StrUtil.blankToDefault(department.getSyncSource(), SOURCE_MANUAL));
        node.setSyncManaged(isSyncManaged(department.getSyncManaged()));
        node.setSyncEnabled(department.getSyncEnabled() == null || department.getSyncEnabled() == 1);
        node.setSyncStatus(department.getSyncStatus());
        node.setSyncRemark(department.getSyncRemark());
        node.setStatDepartmentBelong(StrUtil.blankToDefault(department.getStatDepartmentBelong(), ""));
        node.setStatRegionBelong(StrUtil.blankToDefault(department.getStatRegionBelong(), ""));
        node.setStatAreaBelong(StrUtil.blankToDefault(department.getStatAreaBelong(), ""));
        node.setStatus(department.getStatus());
        node.setSortOrder(department.getSortOrder());
        node.setLastSyncAt(department.getLastSyncAt());
        return node;
    }

    /**
     * 组装用户DisplayName映射。
     */
    private Map<Long, String> buildUserDisplayNameMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectList(
                Wrappers.<User>lambdaQuery().in(User::getId, userIds)
        ).stream().collect(Collectors.toMap(
                User::getId,
                user -> StrUtil.blankToDefault(user.getName(), user.getUsername())
                        + " (" + user.getUsername() + ")",
                (left, right) -> left
        ));
    }

    /**
     * 解析映射Value。
     */
    private <K, V> V resolveMapValue(Map<K, V> source, K key, V defaultValue) {
        if (source == null || source.isEmpty() || key == null) {
            return defaultValue;
        }
        return source.getOrDefault(key, defaultValue);
    }

    private SyncConnectorVO toSyncConnectorVo(SystemSyncConnector connector) {
        Map<String, String> config = readConnectorConfig(connector.getConfigJson());
        SyncConnectorVO vo = new SyncConnectorVO();
        vo.setId(connector.getId());
        vo.setPlatformCode(connector.getPlatformCode());
        vo.setPlatformName(resolvePlatformName(connector.getPlatformCode()));
        vo.setEnabled(connector.getEnabled() != null && connector.getEnabled() == 1);
        vo.setAutoSyncEnabled(connector.getAutoSyncEnabled() != null && connector.getAutoSyncEnabled() == 1);
        vo.setSyncIntervalMinutes(connector.getSyncIntervalMinutes());
        vo.setAppKey(config.getOrDefault("appKey", ""));
        vo.setAppSecret(config.getOrDefault("appSecret", ""));
        vo.setAppId(config.getOrDefault("appId", ""));
        vo.setCorpId(config.getOrDefault("corpId", ""));
        vo.setAgentId(config.getOrDefault("agentId", ""));
        vo.setLastSyncAt(connector.getLastSyncAt());
        vo.setLastSyncStatus(connector.getLastSyncStatus());
        vo.setLastSyncMessage(connector.getLastSyncMessage());
        return vo;
    }

    private SyncJobVO toSyncJobVo(SystemSyncJob job) {
        SyncJobVO vo = new SyncJobVO();
        vo.setId(job.getId());
        vo.setJobNo(job.getJobNo());
        vo.setPlatformCode(job.getPlatformCode());
        vo.setTriggerType(job.getTriggerType());
        vo.setStatus(job.getStatus());
        vo.setSuccessCount(job.getSuccessCount());
        vo.setSkippedCount(job.getSkippedCount());
        vo.setFailedCount(job.getFailedCount());
        vo.setDeletedCount(job.getDeletedCount());
        vo.setSummary(job.getSummary());
        vo.setStartedAt(job.getStartedAt());
        vo.setFinishedAt(job.getFinishedAt());
        return vo;
    }

    private String writeConnectorConfig(String platformCode, SyncConnectorSaveDTO dto) {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("appKey", PLATFORM_WECOM.equals(platformCode) ? null : trimToNull(dto.getAppKey()));
        config.put("appSecret", trimToNull(dto.getAppSecret()));
        config.put("appId", PLATFORM_WECOM.equals(platformCode) ? null : trimToNull(dto.getAppId()));
        config.put("corpId", trimToNull(dto.getCorpId()));
        config.put("agentId", trimToNull(dto.getAgentId()));
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception ex) {
            throw new IllegalArgumentException("\u540c\u6b65\u8fde\u63a5\u914d\u7f6e\u5e8f\u5217\u5316\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u8f93\u5165\u5185\u5bb9");
        }
    }

    private Map<String, String> readConnectorConfig(String configJson) {
        if (StrUtil.isBlank(configJson)) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(configJson, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return new HashMap<>();
        }
    }

    /**
     * 校验ConnectorSave。
     */
    private void validateConnectorSave(String platformCode, SyncConnectorSaveDTO dto) {
        if (!PLATFORM_WECOM.equals(platformCode)) {
            return;
        }
        requireConnectorField(dto.getCorpId(), "\u4f01\u5fae\u4f01\u4e1a ID");
        requireConnectorField(dto.getAppSecret(), "\u4f01\u5fae\u901a\u8baf\u5f55 Secret");
    }

    /**
     * 校验Connector执行Config。
     */
    private void validateConnectorRunConfig(String platformCode, Map<String, String> config) {
        if (!PLATFORM_WECOM.equals(platformCode)) {
            return;
        }
        requireConnectorField(config.get("corpId"), "\u4f01\u5fae\u4f01\u4e1a ID");
        requireConnectorField(config.get("appSecret"), "\u4f01\u5fae\u901a\u8baf\u5f55 Secret");
    }

    private void requireConnectorField(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(fieldName + "\u4e0d\u80fd\u4e3a\u7a7a");
        }
    }

    private OrganizationSyncAdapter requireSyncAdapter(String platformCode) {
        return syncAdapters.stream()
                .filter(adapter -> platformCode.equalsIgnoreCase(adapter.getPlatformCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("\u672a\u627e\u5230 " + resolvePlatformName(platformCode) + " \u540c\u6b65\u9002\u914d\u5668"));
    }

    private MatchResult matchEmployee(Map<String, List<User>> phoneMap, Map<String, List<User>> emailMap, ExternalEmployeeData externalEmployee) {
        String phoneKey = normalizePhone(externalEmployee.getPhone());
        String emailKey = normalizeEmail(externalEmployee.getEmail());
        List<User> phoneMatches = StrUtil.isBlank(phoneKey) ? List.of() : phoneMap.getOrDefault(phoneKey, List.of());
        List<User> emailMatches = StrUtil.isBlank(emailKey) ? List.of() : emailMap.getOrDefault(emailKey, List.of());
        if (phoneMatches.size() > 1) {
            return MatchResult.conflict("\u624b\u673a\u53f7\u5339\u914d\u5230\u591a\u540d\u5458\u5de5\uff0c\u8bf7\u5148\u6e05\u7406\u91cd\u590d\u624b\u673a\u53f7\u540e\u518d\u540c\u6b65");
        }
        if (emailMatches.size() > 1) {
            return MatchResult.conflict("\u90ae\u7bb1\u5339\u914d\u5230\u591a\u540d\u5458\u5de5\uff0c\u8bf7\u5148\u6e05\u7406\u91cd\u590d\u90ae\u7bb1\u540e\u518d\u540c\u6b65");
        }
        User phoneUser = phoneMatches.isEmpty() ? null : phoneMatches.get(0);
        User emailUser = emailMatches.isEmpty() ? null : emailMatches.get(0);
        if (phoneUser != null && emailUser != null && !Objects.equals(phoneUser.getId(), emailUser.getId())) {
            return MatchResult.conflict("\u624b\u673a\u53f7\u4e0e\u90ae\u7bb1\u5339\u914d\u5230\u4e0d\u540c\u5458\u5de5\uff0c\u8bf7\u5148\u5904\u7406\u5458\u5de5\u552f\u4e00\u6027\u540e\u518d\u540c\u6b65");
        }
        return MatchResult.success(phoneUser != null ? phoneUser : emailUser);
    }

    /**
     * 组装同步用户名。
     */
    private String buildSyncUsername(ExternalEmployeeData externalEmployee, String platformCode) {
        String preferred = trimToNull(externalEmployee.getUsername());
        if (preferred == null) {
            preferred = trimToNull(externalEmployee.getPhone());
        }
        if (preferred == null) {
            preferred = trimToNull(externalEmployee.getEmail());
        }
        if (preferred == null) {
            preferred = platformCode.toLowerCase(Locale.ROOT) + "_" + StrUtil.blankToDefault(externalEmployee.getExternalId(), "user");
        }
        preferred = preferred.replaceAll("[^A-Za-z0-9._-]", "_");
        String candidate = preferred;
        int suffix = 1;
        while (userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, candidate)) > 0) {
            candidate = preferred + "_" + suffix++;
        }
        return candidate;
    }

    /**
     * 组装JobNo。
     */
    private String buildJobNo(String platformCode) {
        return platformCode + "-" + LocalDateTime.now().format(JOB_NO_FORMATTER);
    }

    /**
     * 组装Job汇总。
     */
    private String buildJobSummary(SyncStats stats) {
        return "\u6210\u529f " + stats.successCount.get()
                + "\uff0c\u8df3\u8fc7 " + stats.skippedCount.get()
                + "\uff0c\u5931\u8d25 " + stats.failedCount.get()
                + "\uff0c\u5220\u9664 " + stats.deletedCount.get();
    }

    private void appendJobDetail(Long jobId, String detailType, String actionType, String businessKey, String status, String message) {
        SystemSyncJobDetail detail = new SystemSyncJobDetail();
        detail.setJobId(jobId);
        detail.setDetailType(detailType);
        detail.setActionType(actionType);
        detail.setBusinessKey(businessKey);
        detail.setDetailStatus(status);
        detail.setDetailMessage(StrUtil.maxLength(message, 500));
        systemSyncJobDetailMapper.insert(detail);
    }

    private void ensureDefaultConnectors() {
        Set<String> existingCodes = systemSyncConnectorMapper.selectList(
                Wrappers.<SystemSyncConnector>lambdaQuery().orderByAsc(SystemSyncConnector::getId)
        ).stream().map(SystemSyncConnector::getPlatformCode).collect(Collectors.toSet());
        for (String platformCode : List.of(PLATFORM_DINGTALK, PLATFORM_WECOM, PLATFORM_FEISHU)) {
            if (existingCodes.contains(platformCode)) {
                continue;
            }
            SystemSyncConnector connector = new SystemSyncConnector();
            connector.setPlatformCode(platformCode);
            connector.setPlatformName(resolvePlatformName(platformCode));
            connector.setEnabled(1);
            connector.setAutoSyncEnabled(0);
            connector.setSyncIntervalMinutes(60);
            connector.setConfigJson("{}");
            connector.setLastSyncStatus("IDLE");
            connector.setLastSyncMessage("\u5c1a\u672a\u6267\u884c\u540c\u6b65");
            systemSyncConnectorMapper.insert(connector);
        }
    }

    /**
     * 组装用户角色编码映射。
     */
    private Map<Long, List<String>> buildUserRoleCodeMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<SystemUserRole> userRoles = systemUserRoleMapper.selectList(
                Wrappers.<SystemUserRole>lambdaQuery().in(SystemUserRole::getUserId, userIds)
        );
        if (userRoles.isEmpty()) {
            return Map.of();
        }
        Set<Long> roleIds = userRoles.stream()
                .map(SystemUserRole::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> roleCodeMap = systemRoleMapper.selectList(
                Wrappers.<SystemRole>lambdaQuery()
                        .in(SystemRole::getId, roleIds)
        ).stream().collect(Collectors.toMap(SystemRole::getId, SystemRole::getRoleCode, (left, right) -> left));
        return userRoles.stream().collect(Collectors.groupingBy(
                SystemUserRole::getUserId,
                LinkedHashMap::new,
                Collectors.mapping(item -> roleCodeMap.getOrDefault(item.getRoleId(), ""),
                        Collectors.filtering(StrUtil::isNotBlank, Collectors.toList()))
        ));
    }

    /**
     * 组装角色权限编码映射。
     */
    private Map<Long, List<String>> buildRolePermissionCodeMap(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Map.of();
        }
        List<SystemRolePermission> rolePermissions = systemRolePermissionMapper.selectList(
                Wrappers.<SystemRolePermission>lambdaQuery().in(SystemRolePermission::getRoleId, roleIds)
        );
        if (rolePermissions.isEmpty()) {
            return Map.of();
        }
        Set<Long> permissionIds = rolePermissions.stream()
                .map(SystemRolePermission::getPermissionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (permissionIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> permissionCodeMap = systemPermissionMapper.selectList(
                Wrappers.<SystemPermission>lambdaQuery()
                        .in(SystemPermission::getId, permissionIds)
        ).stream().collect(Collectors.toMap(SystemPermission::getId, SystemPermission::getPermissionCode, (left, right) -> left));
        return rolePermissions.stream().collect(Collectors.groupingBy(
                SystemRolePermission::getRoleId,
                LinkedHashMap::new,
                Collectors.mapping(item -> permissionCodeMap.getOrDefault(item.getPermissionId(), ""),
                        Collectors.filtering(StrUtil::isNotBlank, Collectors.toList()))
        ));
    }

    /**
     * 组装角色用户Id映射。
     */
    private Map<Long, List<Long>> buildRoleUserIdMap(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Map.of();
        }
        return systemUserRoleMapper.selectList(
                Wrappers.<SystemUserRole>lambdaQuery().in(SystemUserRole::getRoleId, roleIds)
        ).stream().collect(Collectors.groupingBy(
                SystemUserRole::getRoleId,
                LinkedHashMap::new,
                Collectors.mapping(SystemUserRole::getUserId, Collectors.toList())
        ));
    }

    private String normalizeTriggerType(String triggerType) {
        return StrUtil.isBlank(triggerType) ? "MANUAL" : triggerType.trim().toUpperCase(Locale.ROOT);
    }

    private int nextDepartmentSortOrder() {
        Integer maxSort = systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .select(SystemDepartment::getSortOrder)
                        .orderByDesc(SystemDepartment::getSortOrder)
                        .last("limit 1")
        ).stream().findFirst().map(SystemDepartment::getSortOrder).orElse(0);
        return maxSort + 10;
    }

    private void applyDepartmentExternalId(SystemDepartment department, String platformCode, String externalId) {
        if (PLATFORM_DINGTALK.equals(platformCode)) {
            department.setDingtalkDepartmentId(externalId);
        } else if (PLATFORM_WECOM.equals(platformCode)) {
            department.setWecomDepartmentId(externalId);
        } else if (PLATFORM_FEISHU.equals(platformCode)) {
            department.setFeishuDepartmentId(externalId);
        }
    }

    private void applyUserExternalId(User user, String platformCode, String externalId) {
        if (PLATFORM_DINGTALK.equals(platformCode)) {
            user.setDingtalkUserId(externalId);
        } else if (PLATFORM_WECOM.equals(platformCode)) {
            user.setWecomUserId(externalId);
        } else if (PLATFORM_FEISHU.equals(platformCode)) {
            user.setFeishuUserId(externalId);
        }
    }

    /**
     * 获取用户ExternalId。
     */
    private String getUserExternalId(User user, String platformCode) {
        if (PLATFORM_DINGTALK.equals(platformCode)) {
            return user.getDingtalkUserId();
        }
        if (PLATFORM_WECOM.equals(platformCode)) {
            return user.getWecomUserId();
        }
        if (PLATFORM_FEISHU.equals(platformCode)) {
            return user.getFeishuUserId();
        }
        return null;
    }

    /**
     * 解析Employee业务Key。
     */
    private String resolveEmployeeBusinessKey(ExternalEmployeeData externalEmployee) {
        if (StrUtil.isNotBlank(externalEmployee.getPhone())) {
            return normalizePhone(externalEmployee.getPhone());
        }
        if (StrUtil.isNotBlank(externalEmployee.getEmail())) {
            return normalizeEmail(externalEmployee.getEmail());
        }
        return StrUtil.blankToDefault(externalEmployee.getExternalId(), StrUtil.blankToDefault(externalEmployee.getUsername(), "unknown"));
    }

    private String normalizePhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return "";
        }
        return phone.replaceAll("[^0-9]", "");
    }

    private String normalizeEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }

    private void applyEmployeeStatBelong(User user, EmployeeSaveDTO dto) {
        user.setStatDepartmentBelong(trimToNull(dto.getStatDepartmentBelong()));
        user.setStatRegionBelong(trimToNull(dto.getStatRegionBelong()));
        user.setStatAreaBelong(trimToNull(dto.getStatAreaBelong()));
    }

    private void applyDepartmentStatBelong(SystemDepartment department, DepartmentSaveDTO dto) {
        department.setStatDepartmentBelong(trimToNull(dto.getStatDepartmentBelong()));
        department.setStatRegionBelong(trimToNull(dto.getStatRegionBelong()));
        department.setStatAreaBelong(trimToNull(dto.getStatAreaBelong()));
    }

    /**
     * 判断ManualEmployee是否成立。
     */
    private boolean isManualEmployee(User user) {
        return SOURCE_MANUAL.equalsIgnoreCase(user.getSourceType()) || !isSyncManaged(user.getSyncManaged());
    }

    /**
     * 判断同步Managed是否成立。
     */
    private boolean isSyncManaged(Integer syncManaged) {
        return syncManaged != null && syncManaged == 1;
    }

    private Integer normalizeStatus(Integer status) {
        return status != null && status == 0 ? 0 : 1;
    }

    private Integer normalizeFlag(Integer value) {
        return value != null && value == 1 ? 1 : 0;
    }

    /**
     * 解析PlatformName。
     */
    private String resolvePlatformName(String platformCode) {
        return switch (platformCode) {
            case PLATFORM_DINGTALK -> "\u9489\u9489";
            case PLATFORM_WECOM -> "\u4f01\u5fae";
            case PLATFORM_FEISHU -> "\u98de\u4e66";
            default -> platformCode;
        };
    }

    private void applyCompany(SystemCompany company, CompanySaveDTO dto) {
        company.setCompanyName(dto.getCompanyName().trim());
        company.setInvoiceTitle(trimToNull(dto.getInvoiceTitle()));
        company.setTaxNo(trimToNull(dto.getTaxNo()));
        company.setStatus(normalizeStatus(dto.getStatus()));
    }

    private void applyCompanyBankAccount(SystemCompanyBankAccount account, CompanyBankAccountSaveDTO dto) {
        String previousBankCode = trimToNull(account.getBankCode());
        String previousProvince = trimToNull(account.getProvince());
        String previousCity = trimToNull(account.getCity());
        String previousBranchCode = trimToNull(account.getBranchCode());
        String previousBranchName = trimToNull(account.getBranchName());
        String previousCnapsCode = trimToNull(account.getCnapsCode());

        String companyId = trimToNull(dto.getCompanyId());
        String bankName = trimToNull(dto.getBankName());
        String bankCode = trimToNull(dto.getBankCode());
        String province = trimToNull(dto.getProvince());
        String city = trimToNull(dto.getCity());
        String branchName = trimToNull(dto.getBranchName());
        String branchCode = trimToNull(dto.getBranchCode());
        String accountName = trimToNull(dto.getAccountName());
        String accountNo = trimToNull(dto.getAccountNo());
        if (companyId == null) {
            throw new IllegalArgumentException("\u516c\u53f8\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (bankName == null || bankCode == null || province == null || city == null || branchName == null || branchCode == null) {
            throw new IllegalArgumentException(BANK_DIRECTORY_REQUIRED_MESSAGE);
        }
        if (accountName == null) {
            throw new IllegalArgumentException("\u8d26\u6237\u540d\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (accountNo == null) {
            throw new IllegalArgumentException("\u94f6\u884c\u8d26\u53f7\u4e0d\u80fd\u4e3a\u7a7a");
        }
        requireCompany(companyId);
        account.setCompanyId(companyId);
        account.setBankName(bankName);
        account.setBankCode(bankCode);
        account.setProvince(province);
        account.setCity(city);
        account.setBranchName(branchName);
        account.setBranchCode(branchCode);
        boolean branchSelectionChanged = !Objects.equals(previousBankCode, bankCode)
                || !Objects.equals(previousProvince, province)
                || !Objects.equals(previousCity, city)
                || !Objects.equals(previousBranchCode, branchCode)
                || !Objects.equals(previousBranchName, branchName);
        account.setCnapsCode(resolveWeakCnapsCode(previousCnapsCode, trimToNull(dto.getCnapsCode()), branchSelectionChanged));
        account.setAccountName(accountName);
        account.setAccountNo(accountNo);
        account.setAccountType(trimToNull(dto.getAccountType()));
        account.setAccountUsage(trimToNull(dto.getAccountUsage()));
        account.setCurrencyCode(trimToNull(dto.getCurrencyCode()));
        account.setStatus(normalizeStatus(dto.getStatus()));
        account.setDefaultAccount(account.getStatus() != null && account.getStatus() == 0 ? 0 : normalizeFlag(dto.getDefaultAccount()));
        account.setRemark(trimToNull(dto.getRemark()));
        account.setDirectConnectEnabled(normalizeFlag(dto.getDirectConnectEnabled()));
        account.setDirectConnectProvider(trimToNull(dto.getDirectConnectProvider()));
        account.setDirectConnectChannel(trimToNull(dto.getDirectConnectChannel()));
        account.setDirectConnectProtocol(trimToNull(dto.getDirectConnectProtocol()));
        account.setDirectConnectCustomerNo(trimToNull(dto.getDirectConnectCustomerNo()));
        account.setDirectConnectAppId(trimToNull(dto.getDirectConnectAppId()));
        account.setDirectConnectAccountAlias(trimToNull(dto.getDirectConnectAccountAlias()));
        account.setDirectConnectAuthMode(trimToNull(dto.getDirectConnectAuthMode()));
        account.setDirectConnectApiBaseUrl(trimToNull(dto.getDirectConnectApiBaseUrl()));
        account.setDirectConnectCertRef(trimToNull(dto.getDirectConnectCertRef()));
        account.setDirectConnectSecretRef(trimToNull(dto.getDirectConnectSecretRef()));
        account.setDirectConnectSignType(trimToNull(dto.getDirectConnectSignType()));
        account.setDirectConnectEncryptType(trimToNull(dto.getDirectConnectEncryptType()));
        account.setDirectConnectLastSyncAt(dto.getDirectConnectLastSyncAt());
        account.setDirectConnectLastSyncStatus(trimToNull(dto.getDirectConnectLastSyncStatus()));
        account.setDirectConnectLastErrorMsg(trimToNull(dto.getDirectConnectLastErrorMsg()));
        account.setDirectConnectExtJson(trimToNull(dto.getDirectConnectExtJson()));
    }

    private String resolveWeakCnapsCode(String previousCnapsCode, String submittedCnapsCode, boolean branchSelectionChanged) {
        if (submittedCnapsCode != null) {
            return submittedCnapsCode;
        }
        if (branchSelectionChanged) {
            return null;
        }
        return previousCnapsCode;
    }

    private void ensureCompanyBankAccountUnique(String companyId, String accountNo, Long excludeId) {
        LambdaQueryWrapper<SystemCompanyBankAccount> wrapper = Wrappers.<SystemCompanyBankAccount>lambdaQuery()
                .eq(SystemCompanyBankAccount::getCompanyId, companyId)
                .eq(SystemCompanyBankAccount::getAccountNo, accountNo);
        if (excludeId != null) {
            wrapper.ne(SystemCompanyBankAccount::getId, excludeId);
        }
        if (systemCompanyBankAccountMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("\u540c\u4e00\u516c\u53f8\u4e0b\u94f6\u884c\u8d26\u53f7\u5df2\u5b58\u5728");
        }
    }

    /**
     * 清理Other默认公司银行账户。
     */
    private void clearOtherDefaultCompanyBankAccounts(String companyId, Long currentId) {
        List<SystemCompanyBankAccount> companyAccounts = systemCompanyBankAccountMapper.selectList(
                Wrappers.<SystemCompanyBankAccount>lambdaQuery()
                        .eq(SystemCompanyBankAccount::getCompanyId, companyId)
                        .eq(SystemCompanyBankAccount::getDefaultAccount, 1)
        );
        for (SystemCompanyBankAccount companyAccount : companyAccounts) {
            if (Objects.equals(companyAccount.getId(), currentId)) {
                continue;
            }
            companyAccount.setDefaultAccount(0);
            systemCompanyBankAccountMapper.updateById(companyAccount);
        }
    }

    /**
     * 判断默认账户是否成立。
     */
    private boolean isDefaultAccount(SystemCompanyBankAccount account) {
        return account.getDefaultAccount() != null && account.getDefaultAccount() == 1;
    }

    /**
     * 查询Employee。
     */
    private EmployeeVO findEmployee(Long userId) {
        return listEmployees(new EmployeeQueryDTO()).stream()
                .filter(item -> Objects.equals(item.getUserId(), userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("\u5458\u5de5\u4e0d\u5b58\u5728"));
    }

    /**
     * 查询DepartmentNode。
     */
    private DepartmentTreeNodeVO findDepartmentNode(Long departmentId) {
        return flattenDepartmentNodes(listDepartments()).stream()
                .filter(item -> Objects.equals(item.getId(), departmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("\u90e8\u95e8\u4e0d\u5b58\u5728"));
    }

    /**
     * 查询角色。
     */
    private RoleVO findRole(Long roleId) {
        return listRoles().stream()
                .filter(item -> Objects.equals(item.getId(), roleId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("\u89d2\u8272\u4e0d\u5b58\u5728"));
    }

    /**
     * 查询公司。
     */
    private CompanyVO findCompany(String companyId) {
        Deque<CompanyVO> queue = new ArrayDeque<>(listCompanies());
        while (!queue.isEmpty()) {
            CompanyVO current = queue.pollFirst();
            if (Objects.equals(current.getCompanyId(), companyId)) {
                return current;
            }
            queue.addAll(current.getChildren());
        }
        throw new IllegalArgumentException("\u516c\u53f8\u4e0d\u5b58\u5728");
    }

    /**
     * 查询公司银行账户。
     */
    private CompanyBankAccountVO findCompanyBankAccount(Long id) {
        SystemCompanyBankAccount account = requireCompanyBankAccount(id);
        return toCompanyBankAccountVo(account, buildCompanyNameMap(Set.of(account.getCompanyId())));
    }

    private SystemDepartment requireDepartment(Long id) {
        SystemDepartment department = systemDepartmentMapper.selectById(id);
        if (department == null) {
            throw new IllegalArgumentException("\u90e8\u95e8\u4e0d\u5b58\u5728");
        }
        return department;
    }

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("\u5458\u5de5\u4e0d\u5b58\u5728");
        }
        return user;
    }

    private SystemRole requireRole(Long id) {
        SystemRole role = systemRoleMapper.selectById(id);
        if (role == null) {
            throw new IllegalArgumentException("\u89d2\u8272\u4e0d\u5b58\u5728");
        }
        return role;
    }

    private SystemCompany requireCompany(String companyId) {
        SystemCompany company = systemCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("\u516c\u53f8\u4e0d\u5b58\u5728");
        }
        return company;
    }

    private SystemCompanyBankAccount requireCompanyBankAccount(Long id) {
        SystemCompanyBankAccount account = systemCompanyBankAccountMapper.selectById(id);
        if (account == null) {
            throw new IllegalArgumentException("\u516c\u53f8\u94f6\u884c\u8d26\u6237\u4e0d\u5b58\u5728");
        }
        return account;
    }

    private SystemSyncConnector requireConnector(String platformCode) {
        SystemSyncConnector connector = systemSyncConnectorMapper.selectOne(
                Wrappers.<SystemSyncConnector>lambdaQuery()
                        .eq(SystemSyncConnector::getPlatformCode, platformCode)
                        .last("limit 1")
        );
        if (connector == null) {
            throw new IllegalArgumentException("\u540c\u6b65\u8fde\u63a5\u5668\u4e0d\u5b58\u5728");
        }
        return connector;
    }

    private List<DepartmentTreeNodeVO> flattenDepartmentNodes(List<DepartmentTreeNodeVO> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        return nodes.stream()
                .flatMap(node -> java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(node),
                        flattenDepartmentNodes(node.getChildren()).stream()
                ))
                .toList();
    }

    /**
     * 校验Department上级。
     */
    private void validateDepartmentLeader(Long leaderUserId) {
        if (leaderUserId != null) {
            requireUser(leaderUserId);
        }
    }

    private void ensureDepartmentCodeUnique(String deptCode, Long excludeId) {
        LambdaQueryWrapper<SystemDepartment> wrapper = Wrappers.<SystemDepartment>lambdaQuery()
                .eq(SystemDepartment::getDeptCode, deptCode.trim());
        if (excludeId != null) {
            wrapper.ne(SystemDepartment::getId, excludeId);
        }
        if (systemDepartmentMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("\u90e8\u95e8\u7f16\u7801\u5df2\u5b58\u5728");
        }
    }

    private void ensureUsernameUnique(String username, Long excludeId) {
        LambdaQueryWrapper<User> wrapper = Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, username.trim());
        if (excludeId != null) {
            wrapper.ne(User::getId, excludeId);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("\u7528\u6237\u540d\u5df2\u5b58\u5728");
        }
    }

    private void ensureRoleCodeUnique(String roleCode, Long excludeId) {
        if (StrUtil.isBlank(roleCode)) {
            throw new IllegalArgumentException("\u89d2\u8272\u7f16\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        }
        LambdaQueryWrapper<SystemRole> wrapper = Wrappers.<SystemRole>lambdaQuery()
                .eq(SystemRole::getRoleCode, roleCode.trim());
        if (excludeId != null) {
            wrapper.ne(SystemRole::getId, excludeId);
        }
        if (systemRoleMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("\u89d2\u8272\u7f16\u7801\u5df2\u5b58\u5728");
        }
    }

    private void ensureCompanyIdUnique(String companyId, String excludeCompanyId) {
        LambdaQueryWrapper<SystemCompany> wrapper = Wrappers.<SystemCompany>lambdaQuery()
                .eq(SystemCompany::getCompanyId, companyId.trim());
        if (excludeCompanyId != null) {
            wrapper.ne(SystemCompany::getCompanyId, excludeCompanyId);
        }
        if (systemCompanyMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("\u516c\u53f8ID\u5df2\u5b58\u5728");
        }
    }

    private void ensureCompanyCodeUnique(String companyCode, String excludeCompanyId) {
        LambdaQueryWrapper<SystemCompany> wrapper = Wrappers.<SystemCompany>lambdaQuery()
                .eq(SystemCompany::getCompanyCode, companyCode.trim());
        if (excludeCompanyId != null) {
            wrapper.ne(SystemCompany::getCompanyId, excludeCompanyId);
        }
        if (systemCompanyMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("\u516c\u53f8\u7f16\u7801\u5df2\u5b58\u5728");
        }
    }

    /**
     * 生成ManualDepartment编码。
     */
    private String generateManualDepartmentCode() {
        String prefix = DEPARTMENT_CODE_PREFIX + LocalDate.now().format(CODE_DATE_FORMATTER);
        for (int attempt = 0; attempt < CODE_GENERATION_MAX_RETRY; attempt++) {
            Long count = systemDepartmentMapper.selectCount(
                    Wrappers.<SystemDepartment>lambdaQuery()
                            .likeRight(SystemDepartment::getDeptCode, prefix)
            );
            long nextSequence = (count == null ? 1L : count + 1L) + attempt;
            String deptCode = prefix + String.format("%04d", nextSequence);
            if (systemDepartmentMapper.selectCount(
                    Wrappers.<SystemDepartment>lambdaQuery().eq(SystemDepartment::getDeptCode, deptCode)
            ) == 0) {
                return deptCode;
            }
        }
        throw new IllegalStateException("\u90e8\u95e8\u7f16\u7801\u81ea\u52a8\u751f\u6210\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5");
    }

    /**
     * 生成角色编码。
     */
    private String generateRoleCode() {
        for (int attempt = 0; attempt < CODE_GENERATION_MAX_RETRY; attempt++) {
            int maxSequence = systemRoleMapper.selectList(
                    Wrappers.<SystemRole>lambdaQuery()
                            .likeRight(SystemRole::getRoleCode, ROLE_CODE_PREFIX)
            ).stream()
                    .map(SystemRole::getRoleCode)
                    .mapToInt(this::parseRoleSequence)
                    .max()
                    .orElse(0);
            int nextSequence = maxSequence + 1 + attempt;
            String roleCode = ROLE_CODE_PREFIX + String.format("%06d", nextSequence);
            if (systemRoleMapper.selectCount(
                    Wrappers.<SystemRole>lambdaQuery().eq(SystemRole::getRoleCode, roleCode)
            ) == 0) {
                return roleCode;
            }
        }
        throw new IllegalStateException("\u89d2\u8272\u7f16\u7801\u81ea\u52a8\u751f\u6210\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5");
    }

    private int parseRoleSequence(String roleCode) {
        if (StrUtil.isBlank(roleCode) || !roleCode.startsWith(ROLE_CODE_PREFIX)) {
            return 0;
        }
        String sequencePart = roleCode.substring(ROLE_CODE_PREFIX.length());
        if (!sequencePart.matches("\\d{6}")) {
            return 0;
        }
        return Integer.parseInt(sequencePart);
    }

    /**
     * 判断SuperAdmin角色是否成立。
     */
    private boolean isSuperAdminRole(SystemRole role) {
        return role != null && SUPER_ADMIN_ROLE_CODE.equalsIgnoreCase(StrUtil.blankToDefault(role.getRoleCode(), ""));
    }

    private boolean currentUserIsSuperAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        return accessControlService.getRoleCodes(userId).stream()
                .anyMatch(roleCode -> SUPER_ADMIN_ROLE_CODE.equalsIgnoreCase(roleCode));
    }

    /**
     * 查询SuperAdmin角色Id。
     */
    private Long findSuperAdminRoleId() {
        return systemRoleMapper.selectList(
                Wrappers.<SystemRole>lambdaQuery()
                        .eq(SystemRole::getRoleCode, SUPER_ADMIN_ROLE_CODE)
                        .last("limit 1")
        ).stream().findFirst().map(SystemRole::getId).orElse(null);
    }

    /**
     * 生成公司编码。
     */
    private CompanyCodeBundle generateCompanyCodes() {
        String datePart = LocalDate.now().format(CODE_DATE_FORMATTER);
        String companyIdPrefix = COMPANY_ID_PREFIX + datePart;
        for (int attempt = 0; attempt < CODE_GENERATION_MAX_RETRY; attempt++) {
            Long count = systemCompanyMapper.selectCount(
                    Wrappers.<SystemCompany>lambdaQuery()
                            .likeRight(SystemCompany::getCompanyId, companyIdPrefix)
            );
            long nextSequence = (count == null ? 1L : count + 1L) + attempt;
            String suffix = String.format("%04d", nextSequence);
            String companyId = companyIdPrefix + suffix;
            String companyCode = COMPANY_CODE_PREFIX + datePart + suffix;
            if (systemCompanyMapper.selectCount(
                    Wrappers.<SystemCompany>lambdaQuery()
                            .eq(SystemCompany::getCompanyId, companyId)
                            .or()
                            .eq(SystemCompany::getCompanyCode, companyCode)
            ) == 0) {
                return new CompanyCodeBundle(companyId, companyCode);
            }
        }
        throw new IllegalStateException("\u516c\u53f8ID\u548c\u7f16\u7801\u81ea\u52a8\u751f\u6210\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5");
    }

    private record CompanyCodeBundle(String companyId, String companyCode) {
    }

    private static class SyncStats {
        private final AtomicInteger successCount = new AtomicInteger();
        private final AtomicInteger skippedCount = new AtomicInteger();
        private final AtomicInteger failedCount = new AtomicInteger();
        private final AtomicInteger deletedCount = new AtomicInteger();
    }

    private static class MatchResult {
        private final User user;
        private final String conflictMessage;

        private MatchResult(User user, String conflictMessage) {
            this.user = user;
            this.conflictMessage = conflictMessage;
        }

        private static MatchResult success(User user) {
            return new MatchResult(user, null);
        }

        private static MatchResult conflict(String conflictMessage) {
            return new MatchResult(null, conflictMessage);
        }
    }
}
