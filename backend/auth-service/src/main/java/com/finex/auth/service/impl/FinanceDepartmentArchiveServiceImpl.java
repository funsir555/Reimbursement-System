package com.finex.auth.service.impl;

import com.finex.auth.dto.CompanyVO;
import com.finex.auth.dto.DepartmentTreeNodeVO;
import com.finex.auth.dto.FinanceDepartmentArchiveMetaVO;
import com.finex.auth.dto.FinanceDepartmentArchiveOptionVO;
import com.finex.auth.dto.FinanceDepartmentQueryDTO;
import com.finex.auth.dto.FinanceDepartmentVO;
import com.finex.auth.service.FinanceDepartmentArchiveService;
import com.finex.auth.service.SystemSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinanceDepartmentArchiveServiceImpl implements FinanceDepartmentArchiveService {

    private final SystemSettingsService systemSettingsService;

    @Override
    public FinanceDepartmentArchiveMetaVO getMeta() {
        FinanceDepartmentArchiveMetaVO meta = new FinanceDepartmentArchiveMetaVO();
        meta.setDepartments(systemSettingsService.listDepartments());
        meta.setStatusOptions(buildStatusOptions());
        return meta;
    }

    @Override
    public List<FinanceDepartmentVO> queryDepartments(FinanceDepartmentQueryDTO query) {
        FinanceDepartmentQueryDTO safeQuery = query == null ? new FinanceDepartmentQueryDTO() : query;
        List<DepartmentTreeNodeVO> departmentTree = systemSettingsService.listDepartments();
        List<DepartmentTreeNodeVO> flattenedDepartments = flattenDepartments(departmentTree);
        Map<Long, DepartmentTreeNodeVO> departmentIndex = buildDepartmentIndex(flattenedDepartments);
        Map<String, String> companyNameMap = buildCompanyNameMap(systemSettingsService.listCompanies());

        List<FinanceDepartmentVO> result = new ArrayList<>();
        for (DepartmentTreeNodeVO department : flattenedDepartments) {
            if (!matchesQuery(department, safeQuery)) {
                continue;
            }
            result.add(toDepartmentVO(department, departmentIndex, companyNameMap));
        }
        return result;
    }

    private List<FinanceDepartmentArchiveOptionVO> buildStatusOptions() {
        FinanceDepartmentArchiveOptionVO enabled = new FinanceDepartmentArchiveOptionVO();
        enabled.setValue("1");
        enabled.setLabel("启用");

        FinanceDepartmentArchiveOptionVO disabled = new FinanceDepartmentArchiveOptionVO();
        disabled.setValue("0");
        disabled.setLabel("停用");

        return List.of(enabled, disabled);
    }

    private List<DepartmentTreeNodeVO> flattenDepartments(List<DepartmentTreeNodeVO> departments) {
        List<DepartmentTreeNodeVO> result = new ArrayList<>();
        for (DepartmentTreeNodeVO department : departments) {
            flattenDepartmentNode(department, result);
        }
        return result;
    }

    private void flattenDepartmentNode(DepartmentTreeNodeVO department, List<DepartmentTreeNodeVO> result) {
        result.add(department);
        if (department.getChildren() == null || department.getChildren().isEmpty()) {
            return;
        }
        for (DepartmentTreeNodeVO child : department.getChildren()) {
            flattenDepartmentNode(child, result);
        }
    }

    private Map<Long, DepartmentTreeNodeVO> buildDepartmentIndex(List<DepartmentTreeNodeVO> departments) {
        Map<Long, DepartmentTreeNodeVO> index = new LinkedHashMap<>();
        for (DepartmentTreeNodeVO department : departments) {
            if (department.getId() != null) {
                index.put(department.getId(), department);
            }
        }
        return index;
    }

    private Map<String, String> buildCompanyNameMap(List<CompanyVO> companies) {
        Map<String, String> companyNameMap = new LinkedHashMap<>();
        for (CompanyVO company : companies) {
            companyNameMap.put(company.getCompanyId(), company.getCompanyName());
        }
        return companyNameMap;
    }

    private boolean matchesQuery(DepartmentTreeNodeVO department, FinanceDepartmentQueryDTO query) {
        if (query.getParentId() != null && !query.getParentId().equals(department.getParentId())) {
            return false;
        }
        if (query.getStatus() != null && !query.getStatus().equals(department.getStatus())) {
            return false;
        }
        String keyword = normalizeText(query.getKeyword());
        if (keyword.isEmpty()) {
            return true;
        }
        return containsIgnoreCase(department.getDeptCode(), keyword)
                || containsIgnoreCase(department.getDeptName(), keyword)
                || containsIgnoreCase(department.getLeaderName(), keyword);
    }

    private FinanceDepartmentVO toDepartmentVO(
            DepartmentTreeNodeVO department,
            Map<Long, DepartmentTreeNodeVO> departmentIndex,
            Map<String, String> companyNameMap
    ) {
        FinanceDepartmentVO vo = new FinanceDepartmentVO();
        vo.setId(department.getId());
        vo.setDeptCode(department.getDeptCode());
        vo.setDeptName(department.getDeptName());
        vo.setParentId(department.getParentId());
        vo.setParentDeptName(resolveParentDeptName(department.getParentId(), departmentIndex));
        vo.setCompanyId(department.getCompanyId());
        vo.setCompanyName(resolveCompanyName(department.getCompanyId(), companyNameMap));
        vo.setLeaderUserId(department.getLeaderUserId());
        vo.setLeaderName(department.getLeaderName());
        vo.setStatus(department.getStatus());
        vo.setSyncSource(department.getSyncSource());
        vo.setSyncManaged(department.getSyncManaged());
        vo.setSyncEnabled(department.getSyncEnabled());
        vo.setSyncStatus(department.getSyncStatus());
        vo.setSyncRemark(department.getSyncRemark());
        vo.setSortOrder(department.getSortOrder());
        vo.setLastSyncAt(department.getLastSyncAt());
        vo.setStatDepartmentBelong(department.getStatDepartmentBelong());
        vo.setStatRegionBelong(department.getStatRegionBelong());
        vo.setStatAreaBelong(department.getStatAreaBelong());
        return vo;
    }

    private String resolveParentDeptName(Long parentId, Map<Long, DepartmentTreeNodeVO> departmentIndex) {
        if (parentId == null) {
            return null;
        }
        DepartmentTreeNodeVO parent = departmentIndex.get(parentId);
        return parent == null ? null : parent.getDeptName();
    }

    private String resolveCompanyName(String companyId, Map<String, String> companyNameMap) {
        if (companyId == null || companyId.isBlank()) {
            return null;
        }
        return companyNameMap.getOrDefault(companyId, companyId);
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        if (source == null || source.isBlank()) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
