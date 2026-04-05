package com.finex.auth.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.finex.auth.service.SystemSettingsService;
import com.finex.auth.service.UserService;
import com.finex.auth.support.systemsettings.ExternalDepartmentData;
import com.finex.auth.support.systemsettings.ExternalEmployeeData;
import com.finex.auth.support.systemsettings.ExternalSyncPayload;
import com.finex.auth.support.systemsettings.OrganizationSyncAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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

@Service
@RequiredArgsConstructor
public class SystemSettingsServiceImpl implements SystemSettingsService {

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

    @Override
    public SystemSettingsBootstrapVO getBootstrap(Long currentUserId) {
        SystemSettingsBootstrapVO bootstrap = new SystemSettingsBootstrapVO();
        bootstrap.setCurrentUser(buildCurrentUser(currentUserId));
        bootstrap.setDepartments(listDepartments());
        bootstrap.setEmployees(listEmployees(new EmployeeQueryDTO()));
        bootstrap.setRoles(listRoles());
        bootstrap.setPermissions(getPermissionTree());
        bootstrap.setCompanies(listCompanies());
        bootstrap.setConnectors(listSyncConnectors());
        bootstrap.setJobs(listSyncJobs());
        return bootstrap;
    }

    @Override
    public List<DepartmentTreeNodeVO> listDepartments() {
        List<SystemDepartment> departments = systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .orderByAsc(SystemDepartment::getSortOrder, SystemDepartment::getId)
        );
        return buildDepartmentTree(departments);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentTreeNodeVO createDepartment(DepartmentSaveDTO dto) {
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
        department.setSyncRemark("手工维护");
        department.setStatus(normalizeStatus(dto.getStatus()));
        department.setSortOrder(dto.getSortOrder() == null ? nextDepartmentSortOrder() : dto.getSortOrder());
        systemDepartmentMapper.insert(department);
        return findDepartmentNode(department.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DepartmentTreeNodeVO updateDepartment(Long id, DepartmentSaveDTO dto) {
        SystemDepartment department = requireDepartment(id);
        if (dto.getParentId() != null && Objects.equals(dto.getParentId(), id)) {
            throw new IllegalArgumentException("上级部门不能选择自己");
        }
        if (dto.getParentId() != null) {
            requireDepartment(dto.getParentId());
        }
        validateDepartmentLeader(dto.getLeaderUserId());

        boolean manualDepartment = SOURCE_MANUAL.equalsIgnoreCase(department.getSyncSource()) || !isSyncManaged(department.getSyncManaged());
        if (!manualDepartment) {
            throw new IllegalArgumentException("同步部门暂不支持手工编辑");
        }

        department.setDeptName(dto.getDeptName().trim());
        department.setParentId(dto.getParentId());
        department.setCompanyId(trimToNull(dto.getCompanyId()));
        department.setLeaderUserId(dto.getLeaderUserId());
        department.setSyncEnabled(dto.getSyncEnabled() != null && dto.getSyncEnabled() == 0 ? 0 : 1);
        department.setStatus(normalizeStatus(dto.getStatus()));
        department.setSortOrder(dto.getSortOrder() == null ? department.getSortOrder() : dto.getSortOrder());
        department.setSyncRemark("手工维护");
        department.setSyncStatus("MANUAL");
        systemDepartmentMapper.updateById(department);
        return findDepartmentNode(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteDepartment(Long id) {
        SystemDepartment department = requireDepartment(id);
        if (!SOURCE_MANUAL.equalsIgnoreCase(department.getSyncSource())) {
            throw new IllegalArgumentException("同步部门不能直接删除，请通过同步清理");
        }
        long childCount = systemDepartmentMapper.selectCount(
                Wrappers.<SystemDepartment>lambdaQuery().eq(SystemDepartment::getParentId, id)
        );
        if (childCount > 0) {
            throw new IllegalArgumentException("请先删除下级部门");
        }
        long employeeCount = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getDeptId, id)
        );
        if (employeeCount > 0) {
            throw new IllegalArgumentException("请先迁移该部门下员工");
        }
        systemDepartmentMapper.deleteById(id);
        return Boolean.TRUE;
    }

    @Override
    public List<EmployeeVO> listEmployees(EmployeeQueryDTO query) {
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
            vo.setStatus(user.getStatus());
            vo.setSourceType(StrUtil.blankToDefault(user.getSourceType(), SOURCE_MANUAL));
            vo.setSyncManaged(isSyncManaged(user.getSyncManaged()));
            vo.setLastSyncAt(user.getLastSyncAt());
            vo.setRoleCodes(roleCodesByUserId.getOrDefault(user.getId(), List.of()));
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeVO createEmployee(EmployeeSaveDTO dto) {
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
        user.setStatus(normalizeStatus(dto.getStatus()));
        user.setSourceType(SOURCE_MANUAL);
        user.setSyncManaged(0);
        userMapper.insert(user);
        return findEmployee(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeVO updateEmployee(Long id, EmployeeSaveDTO dto) {
        User user = requireUser(id);
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
        userMapper.updateById(user);
        return findEmployee(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteEmployee(Long id) {
        requireUser(id);
        long departmentLeaderCount = systemDepartmentMapper.selectCount(
                Wrappers.<SystemDepartment>lambdaQuery().eq(SystemDepartment::getLeaderUserId, id)
        );
        if (departmentLeaderCount > 0) {
            throw new IllegalArgumentException("请先调整该员工负责的部门负责人");
        }
        systemUserRoleMapper.delete(Wrappers.<SystemUserRole>lambdaQuery().eq(SystemUserRole::getUserId, id));
        userMapper.deleteById(id);
        return Boolean.TRUE;
    }

    @Override
    public List<RoleVO> listRoles() {
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
            vo.setUserNames(userIds.stream().map(userId -> userNameMap.getOrDefault(userId, "用户#" + userId)).toList());
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO createRole(RoleSaveDTO dto) {
        SystemRole role = new SystemRole();
        role.setRoleCode(generateRoleCode());
        role.setRoleName(dto.getRoleName().trim());
        role.setRoleDescription(trimToNull(dto.getRoleDescription()));
        role.setStatus(normalizeStatus(dto.getStatus()));
        systemRoleMapper.insert(role);
        return findRole(role.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO updateRole(Long id, RoleSaveDTO dto) {
        SystemRole role = requireRole(id);
        if (isSuperAdminRole(role)) {
            throw new IllegalArgumentException("超级管理员角色为系统保留角色，不能通过前端修改");
        }
        role.setRoleName(dto.getRoleName().trim());
        role.setRoleDescription(trimToNull(dto.getRoleDescription()));
        role.setStatus(normalizeStatus(dto.getStatus()));
        systemRoleMapper.updateById(role);
        return findRole(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteRole(Long id) {
        SystemRole role = requireRole(id);
        if (isSuperAdminRole(role)) {
            throw new IllegalArgumentException("超级管理员角色为系统保留角色，不能删除");
        }
        systemRolePermissionMapper.delete(Wrappers.<SystemRolePermission>lambdaQuery().eq(SystemRolePermission::getRoleId, id));
        systemUserRoleMapper.delete(Wrappers.<SystemUserRole>lambdaQuery().eq(SystemUserRole::getRoleId, id));
        systemRoleMapper.deleteById(id);
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean assignRolePermissions(Long roleId, RolePermissionAssignDTO dto, Long currentUserId) {
        SystemRole role = requireRole(roleId);
        if (isSuperAdminRole(role) && !currentUserIsSuperAdmin(currentUserId)) {
            throw new IllegalArgumentException("超级管理员权限仅可由超级管理员修改");
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean assignUserRoles(Long userId, UserRoleAssignDTO dto) {
        requireUser(userId);
        Long superAdminRoleId = findSuperAdminRoleId();
        List<Long> roleIds = dto == null || dto.getRoleIds() == null
                ? List.of()
                : dto.getRoleIds().stream().filter(Objects::nonNull).distinct().toList();
        if (superAdminRoleId != null && roleIds.contains(superAdminRoleId)) {
            throw new IllegalArgumentException("超级管理员只能通过数据库维护");
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
                throw new IllegalArgumentException("超级管理员只能通过数据库维护");
            }
            SystemUserRole userRole = new SystemUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            systemUserRoleMapper.insert(userRole);
        }
        return Boolean.TRUE;
    }

    @Override
    public List<PermissionTreeNodeVO> getPermissionTree() {
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

    @Override
    public List<CompanyVO> listCompanies() {
        List<SystemCompany> companies = systemCompanyMapper.selectList(
                Wrappers.<SystemCompany>lambdaQuery()
                        .orderByAsc(SystemCompany::getCompanyCode)
        );
        return buildCompanyList(companies);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanyVO createCompany(CompanySaveDTO dto) {
        CompanyCodeBundle codeBundle = generateCompanyCodes();
        SystemCompany company = new SystemCompany();
        company.setCompanyId(codeBundle.companyId());
        company.setCompanyCode(codeBundle.companyCode());
        applyCompany(company, dto);
        systemCompanyMapper.insert(company);
        return findCompany(company.getCompanyId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompanyVO updateCompany(String companyId, CompanySaveDTO dto) {
        SystemCompany company = requireCompany(companyId);
        applyCompany(company, dto);
        company.setCompanyId(companyId);
        systemCompanyMapper.updateById(company);
        return findCompany(companyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCompany(String companyId) {
        requireCompany(companyId);
        long departmentCount = systemDepartmentMapper.selectCount(
                Wrappers.<SystemDepartment>lambdaQuery().eq(SystemDepartment::getCompanyId, companyId)
        );
        if (departmentCount > 0) {
            throw new IllegalArgumentException("请先解除部门公司归属");
        }
        long userCount = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getCompanyId, companyId)
        );
        if (userCount > 0) {
            throw new IllegalArgumentException("请先解除员工公司归属");
        }
        systemCompanyMapper.deleteById(companyId);
        return Boolean.TRUE;
    }

    @Override
    public List<SyncConnectorVO> listSyncConnectors() {
        ensureDefaultConnectors();
        return systemSyncConnectorMapper.selectList(
                Wrappers.<SystemSyncConnector>lambdaQuery().orderByAsc(SystemSyncConnector::getId)
        ).stream().map(this::toSyncConnectorVo).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SyncConnectorVO updateSyncConnector(SyncConnectorSaveDTO dto) {
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

    @Override
    public List<SyncJobVO> listSyncJobs() {
        return systemSyncJobMapper.selectList(
                Wrappers.<SystemSyncJob>lambdaQuery()
                        .orderByDesc(SystemSyncJob::getCreatedAt, SystemSyncJob::getId)
                        .last("limit 20")
        ).stream().map(this::toSyncJobVo).toList();
    }

    @Override
    public SyncJobVO runSync(SyncRunDTO dto, String operator) {
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
            throw new IllegalArgumentException("未找到可执行的同步平台");
        }
        return lastJob;
    }

    @Override
    public void runDueSyncJobs() {
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

    private SyncJobVO executeSync(SystemSyncConnector connector, String triggerType, String operator) {
        if (connector.getEnabled() == null || connector.getEnabled() != 1) {
            throw new IllegalArgumentException(resolvePlatformName(connector.getPlatformCode()) + " 同步未启用");
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
        job.setSummary("同步执行中");
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
            job.setSummary(StrUtil.blankToDefault(ex.getMessage(), "同步执行失败"));
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
                appendJobDetail(jobId, DETAIL_TYPE_DEPARTMENT, "UPSERT", "", DETAIL_STATUS_FAILED, "部门编码为空，已跳过");
                continue;
            }
            SystemDepartment existing = workingMap.get(externalDepartment.getDeptCode());
            if (existing != null && SOURCE_MANUAL.equalsIgnoreCase(existing.getSyncSource()) && !isSyncManaged(existing.getSyncManaged())) {
                stats.skippedCount.incrementAndGet();
                appendJobDetail(jobId, DETAIL_TYPE_DEPARTMENT, "UPSERT", externalDepartment.getDeptCode(), DETAIL_STATUS_SKIPPED, "手工部门受保护，未覆盖");
                continue;
            }

            SystemDepartment target = existing == null ? new SystemDepartment() : existing;
            target.setDeptCode(externalDepartment.getDeptCode());
            target.setDeptName(externalDepartment.getDeptName());
            target.setSyncSource(platformCode);
            target.setSyncEnabled(1);
            target.setSyncManaged(1);
            target.setSyncStatus(JOB_STATUS_SUCCESS);
            target.setSyncRemark("来源于 " + resolvePlatformName(platformCode) + " 自动同步");
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
            appendJobDetail(jobId, DETAIL_TYPE_DEPARTMENT, "UPSERT", target.getDeptCode(), DETAIL_STATUS_SUCCESS, "部门同步成功");
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
                appendJobDetail(jobId, DETAIL_TYPE_EMPLOYEE, "UPSERT", resolveEmployeeBusinessKey(externalEmployee), DETAIL_STATUS_SKIPPED, "手工员工受保护，未覆盖");
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
            appendJobDetail(jobId, DETAIL_TYPE_EMPLOYEE, "UPSERT", resolveEmployeeBusinessKey(externalEmployee), DETAIL_STATUS_SUCCESS, "员工同步成功");
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
            appendJobDetail(jobId, DETAIL_TYPE_EMPLOYEE, "DELETE", String.valueOf(user.getId()), DETAIL_STATUS_DELETED, "来源缺失，已删除同步员工");
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

    private boolean deleteManagedDepartmentRecursively(Long jobId, SystemDepartment department, SyncStats stats) {
        List<SystemDepartment> children = systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery().eq(SystemDepartment::getParentId, department.getId())
        );
        for (SystemDepartment child : children) {
            if (SOURCE_MANUAL.equalsIgnoreCase(child.getSyncSource()) && !isSyncManaged(child.getSyncManaged())) {
                appendJobDetail(jobId, DETAIL_TYPE_DEPARTMENT, "DELETE", department.getDeptCode(), DETAIL_STATUS_SKIPPED, "存在手工子部门，跳过删除");
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
                appendJobDetail(jobId, DETAIL_TYPE_DEPARTMENT, "DELETE", department.getDeptCode(), DETAIL_STATUS_SKIPPED, "存在手工员工，跳过删除");
                return false;
            }
        }
        for (User user : users) {
            systemUserRoleMapper.delete(Wrappers.<SystemUserRole>lambdaQuery().eq(SystemUserRole::getUserId, user.getId()));
            userMapper.deleteById(user.getId());
            stats.deletedCount.incrementAndGet();
            appendJobDetail(jobId, DETAIL_TYPE_EMPLOYEE, "DELETE", String.valueOf(user.getId()), DETAIL_STATUS_DELETED, "部门来源缺失，联动删除同步员工");
        }

        systemDepartmentMapper.deleteById(department.getId());
        stats.deletedCount.incrementAndGet();
        appendJobDetail(jobId, DETAIL_TYPE_DEPARTMENT, "DELETE", department.getDeptCode(), DETAIL_STATUS_DELETED, "来源缺失，已删除同步部门");
        return true;
    }

    private UserProfileVO buildCurrentUser(Long userId) {
        User user = requireUser(userId);
        UserProfileVO currentUser = new UserProfileVO();
        currentUser.setUserId(user.getId());
        currentUser.setUsername(user.getUsername());
        currentUser.setName(StrUtil.blankToDefault(user.getName(), user.getUsername()));
        currentUser.setPhone(user.getPhone());
        currentUser.setEmail(user.getEmail());
        currentUser.setPosition(StrUtil.blankToDefault(user.getPosition(), "员工"));
        currentUser.setLaborRelationBelong(StrUtil.blankToDefault(user.getLaborRelationBelong(), ""));
        currentUser.setCompanyId(user.getCompanyId());
        currentUser.setRoles(accessControlService.getRoleCodes(userId));
        currentUser.setPermissionCodes(accessControlService.getPermissionCodes(userId));
        return currentUser;
    }

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
            vo.setBankName(company.getBankName());
            vo.setBankAccountName(company.getBankAccountName());
            vo.setBankAccountNo(company.getBankAccountNo());
            vo.setStatus(company.getStatus());
            vo.setChildren(new ArrayList<>());
            result.add(vo);
        }
        return result;
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
        node.setStatus(department.getStatus());
        node.setSortOrder(department.getSortOrder());
        node.setLastSyncAt(department.getLastSyncAt());
        return node;
    }

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
            throw new IllegalArgumentException("同步连接配置序列化失败");
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

    private void validateConnectorSave(String platformCode, SyncConnectorSaveDTO dto) {
        if (!PLATFORM_WECOM.equals(platformCode)) {
            return;
        }
        requireConnectorField(dto.getCorpId(), "企微企业 ID");
        requireConnectorField(dto.getAppSecret(), "企微通讯录 Secret");
    }

    private void validateConnectorRunConfig(String platformCode, Map<String, String> config) {
        if (!PLATFORM_WECOM.equals(platformCode)) {
            return;
        }
        requireConnectorField(config.get("corpId"), "企微企业 ID");
        requireConnectorField(config.get("appSecret"), "企微通讯录 Secret");
    }

    private void requireConnectorField(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
    }

    private OrganizationSyncAdapter requireSyncAdapter(String platformCode) {
        return syncAdapters.stream()
                .filter(adapter -> platformCode.equalsIgnoreCase(adapter.getPlatformCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到 " + resolvePlatformName(platformCode) + " 同步适配器"));
    }

    private MatchResult matchEmployee(Map<String, List<User>> phoneMap, Map<String, List<User>> emailMap, ExternalEmployeeData externalEmployee) {
        String phoneKey = normalizePhone(externalEmployee.getPhone());
        String emailKey = normalizeEmail(externalEmployee.getEmail());
        List<User> phoneMatches = StrUtil.isBlank(phoneKey) ? List.of() : phoneMap.getOrDefault(phoneKey, List.of());
        List<User> emailMatches = StrUtil.isBlank(emailKey) ? List.of() : emailMap.getOrDefault(emailKey, List.of());
        if (phoneMatches.size() > 1) {
            return MatchResult.conflict("手机号命中多条员工记录，已跳过");
        }
        if (emailMatches.size() > 1) {
            return MatchResult.conflict("邮箱命中多条员工记录，已跳过");
        }
        User phoneUser = phoneMatches.isEmpty() ? null : phoneMatches.get(0);
        User emailUser = emailMatches.isEmpty() ? null : emailMatches.get(0);
        if (phoneUser != null && emailUser != null && !Objects.equals(phoneUser.getId(), emailUser.getId())) {
            return MatchResult.conflict("手机号与邮箱命中不同员工，已跳过");
        }
        return MatchResult.success(phoneUser != null ? phoneUser : emailUser);
    }

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

    private String buildJobNo(String platformCode) {
        return platformCode + "-" + LocalDateTime.now().format(JOB_NO_FORMATTER);
    }

    private String buildJobSummary(SyncStats stats) {
        return "成功 " + stats.successCount.get()
                + "，跳过 " + stats.skippedCount.get()
                + "，失败 " + stats.failedCount.get()
                + "，删除 " + stats.deletedCount.get();
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
            connector.setLastSyncMessage("尚未执行同步");
            systemSyncConnectorMapper.insert(connector);
        }
    }

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

    private boolean isSyncManaged(Integer syncManaged) {
        return syncManaged != null && syncManaged == 1;
    }

    private Integer normalizeStatus(Integer status) {
        return status != null && status == 0 ? 0 : 1;
    }

    private String resolvePlatformName(String platformCode) {
        return switch (platformCode) {
            case PLATFORM_DINGTALK -> "钉钉";
            case PLATFORM_WECOM -> "企微";
            case PLATFORM_FEISHU -> "飞书";
            default -> platformCode;
        };
    }

    private void applyCompany(SystemCompany company, CompanySaveDTO dto) {
        company.setCompanyName(dto.getCompanyName().trim());
        company.setInvoiceTitle(trimToNull(dto.getInvoiceTitle()));
        company.setTaxNo(trimToNull(dto.getTaxNo()));
        company.setBankName(trimToNull(dto.getBankName()));
        company.setBankAccountName(trimToNull(dto.getBankAccountName()));
        company.setBankAccountNo(trimToNull(dto.getBankAccountNo()));
        company.setStatus(normalizeStatus(dto.getStatus()));
    }

    private EmployeeVO findEmployee(Long userId) {
        return listEmployees(new EmployeeQueryDTO()).stream()
                .filter(item -> Objects.equals(item.getUserId(), userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("员工不存在"));
    }

    private DepartmentTreeNodeVO findDepartmentNode(Long departmentId) {
        return flattenDepartmentNodes(listDepartments()).stream()
                .filter(item -> Objects.equals(item.getId(), departmentId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("部门不存在"));
    }

    private RoleVO findRole(Long roleId) {
        return listRoles().stream()
                .filter(item -> Objects.equals(item.getId(), roleId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("角色不存在"));
    }

    private CompanyVO findCompany(String companyId) {
        Deque<CompanyVO> queue = new ArrayDeque<>(listCompanies());
        while (!queue.isEmpty()) {
            CompanyVO current = queue.pollFirst();
            if (Objects.equals(current.getCompanyId(), companyId)) {
                return current;
            }
            queue.addAll(current.getChildren());
        }
        throw new IllegalArgumentException("公司不存在");
    }

    private SystemDepartment requireDepartment(Long id) {
        SystemDepartment department = systemDepartmentMapper.selectById(id);
        if (department == null) {
            throw new IllegalArgumentException("部门不存在");
        }
        return department;
    }

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("员工不存在");
        }
        return user;
    }

    private SystemRole requireRole(Long id) {
        SystemRole role = systemRoleMapper.selectById(id);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        return role;
    }

    private SystemCompany requireCompany(String companyId) {
        SystemCompany company = systemCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("公司不存在");
        }
        return company;
    }

    private SystemSyncConnector requireConnector(String platformCode) {
        SystemSyncConnector connector = systemSyncConnectorMapper.selectOne(
                Wrappers.<SystemSyncConnector>lambdaQuery()
                        .eq(SystemSyncConnector::getPlatformCode, platformCode)
                        .last("limit 1")
        );
        if (connector == null) {
            throw new IllegalArgumentException("同步连接器不存在");
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
            throw new IllegalArgumentException("部门编码已存在");
        }
    }

    private void ensureUsernameUnique(String username, Long excludeId) {
        LambdaQueryWrapper<User> wrapper = Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, username.trim());
        if (excludeId != null) {
            wrapper.ne(User::getId, excludeId);
        }
        if (userMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
    }

    private void ensureRoleCodeUnique(String roleCode, Long excludeId) {
        if (StrUtil.isBlank(roleCode)) {
            throw new IllegalArgumentException("角色编码不能为空");
        }
        LambdaQueryWrapper<SystemRole> wrapper = Wrappers.<SystemRole>lambdaQuery()
                .eq(SystemRole::getRoleCode, roleCode.trim());
        if (excludeId != null) {
            wrapper.ne(SystemRole::getId, excludeId);
        }
        if (systemRoleMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("角色编码已存在");
        }
    }

    private void ensureCompanyIdUnique(String companyId, String excludeCompanyId) {
        LambdaQueryWrapper<SystemCompany> wrapper = Wrappers.<SystemCompany>lambdaQuery()
                .eq(SystemCompany::getCompanyId, companyId.trim());
        if (excludeCompanyId != null) {
            wrapper.ne(SystemCompany::getCompanyId, excludeCompanyId);
        }
        if (systemCompanyMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("公司主体编码已存在");
        }
    }

    private void ensureCompanyCodeUnique(String companyCode, String excludeCompanyId) {
        LambdaQueryWrapper<SystemCompany> wrapper = Wrappers.<SystemCompany>lambdaQuery()
                .eq(SystemCompany::getCompanyCode, companyCode.trim());
        if (excludeCompanyId != null) {
            wrapper.ne(SystemCompany::getCompanyId, excludeCompanyId);
        }
        if (systemCompanyMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("公司编号已存在");
        }
    }

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
        throw new IllegalStateException("部门编码自动生成失败");
    }

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
        throw new IllegalStateException("角色编码自动生成失败");
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

    private Long findSuperAdminRoleId() {
        return systemRoleMapper.selectList(
                Wrappers.<SystemRole>lambdaQuery()
                        .eq(SystemRole::getRoleCode, SUPER_ADMIN_ROLE_CODE)
                        .last("limit 1")
        ).stream().findFirst().map(SystemRole::getId).orElse(null);
    }

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
        throw new IllegalStateException("公司编码自动生成失败");
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
