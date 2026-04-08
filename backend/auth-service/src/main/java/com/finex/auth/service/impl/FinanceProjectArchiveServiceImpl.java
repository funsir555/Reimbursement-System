package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.FinanceProjectArchiveMetaVO;
import com.finex.auth.dto.FinanceProjectArchiveOptionVO;
import com.finex.auth.dto.FinanceProjectClassSaveDTO;
import com.finex.auth.dto.FinanceProjectClassSummaryVO;
import com.finex.auth.dto.FinanceProjectCloseDTO;
import com.finex.auth.dto.FinanceProjectDetailVO;
import com.finex.auth.dto.FinanceProjectSaveDTO;
import com.finex.auth.dto.FinanceProjectStatusDTO;
import com.finex.auth.dto.FinanceProjectSummaryVO;
import com.finex.auth.entity.FinanceProjectArchive;
import com.finex.auth.entity.FinanceProjectClass;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.service.FinanceProjectArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FinanceProjectArchiveServiceImpl implements FinanceProjectArchiveService {

    private final FinanceProjectClassMapper financeProjectClassMapper;
    private final FinanceProjectArchiveMapper financeProjectArchiveMapper;
    private final SystemCompanyMapper systemCompanyMapper;
    private final GlAccvouchMapper glAccvouchMapper;

    @Override
    public FinanceProjectArchiveMetaVO getMeta(String companyId) {
        String normalizedCompanyId = requireCompanyId(companyId);
        requireEnabledCompany(normalizedCompanyId);

        FinanceProjectArchiveMetaVO meta = new FinanceProjectArchiveMetaVO();
        meta.setStatusOptions(List.of(option("1", "启用"), option("0", "停用")));
        meta.setCloseStatusOptions(List.of(option("0", "未封存"), option("1", "已封存")));
        meta.setProjectClassOptions(
                financeProjectClassMapper.selectList(
                                Wrappers.<FinanceProjectClass>lambdaQuery()
                                        .eq(FinanceProjectClass::getCompanyId, normalizedCompanyId)
                                        .eq(FinanceProjectClass::getStatus, 1)
                                        .orderByAsc(FinanceProjectClass::getSortOrder, FinanceProjectClass::getProjectClassCode, FinanceProjectClass::getId)
                        ).stream()
                        .map(item -> option(item.getProjectClassCode(), item.getProjectClassCode() + " / " + item.getProjectClassName()))
                        .toList()
        );
        return meta;
    }

    @Override
    public List<FinanceProjectClassSummaryVO> listProjectClasses(String companyId, String keyword, Integer status) {
        QueryWrapper<FinanceProjectClass> query = new QueryWrapper<>();
        query.eq("company_id", requireCompanyId(companyId));
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword != null) {
            query.and(wrapper -> wrapper
                    .like("project_class_code", normalizedKeyword)
                    .or()
                    .like("project_class_name", normalizedKeyword));
        }
        if (status != null) {
            query.eq("status", normalizeFlag(status, 1));
        }
        query.orderByAsc("sort_order", "project_class_code", "id");
        return financeProjectClassMapper.selectList(query).stream().map(this::toClassSummary).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceProjectClassSummaryVO createProjectClass(String companyId, FinanceProjectClassSaveDTO dto, String operatorName) {
        validateProjectClassPayload(dto);
        String normalizedCompanyId = requireCompanyId(companyId);
        requireEnabledCompany(normalizedCompanyId);

        String projectClassCode = requireText(dto.getProjectClassCode(), "项目分类编码不能为空");
        if (findProjectClass(normalizedCompanyId, projectClassCode) != null) {
            throw new IllegalStateException("当前公司下已存在相同项目分类编码");
        }

        LocalDateTime now = LocalDateTime.now();
        FinanceProjectClass entity = new FinanceProjectClass();
        entity.setCompanyId(normalizedCompanyId);
        entity.setProjectClassCode(projectClassCode);
        entity.setProjectClassName(requireText(dto.getProjectClassName(), "项目分类名称不能为空"));
        entity.setStatus(1);
        entity.setSortOrder(resolveNextProjectClassSortOrder(normalizedCompanyId));
        entity.setCreatedBy(normalize(operatorName, "system"));
        entity.setUpdatedBy(normalize(operatorName, "system"));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        financeProjectClassMapper.insert(entity);
        return toClassSummary(requireProjectClass(normalizedCompanyId, projectClassCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceProjectClassSummaryVO updateProjectClass(String companyId, String projectClassCode, FinanceProjectClassSaveDTO dto, String operatorName) {
        validateProjectClassPayload(dto);
        String normalizedCompanyId = requireCompanyId(companyId);
        FinanceProjectClass existing = requireProjectClass(normalizedCompanyId, projectClassCode);
        String targetCode = requireText(dto.getProjectClassCode(), "项目分类编码不能为空");
        if (!Objects.equals(existing.getProjectClassCode(), targetCode)) {
            if (hasProjects(normalizedCompanyId, existing.getProjectClassCode())) {
                throw new IllegalStateException("当前项目分类已被项目档案引用，不能修改分类编码");
            }
            if (findProjectClass(normalizedCompanyId, targetCode) != null) {
                throw new IllegalStateException("当前公司下已存在相同项目分类编码");
            }
            existing.setProjectClassCode(targetCode);
        }
        existing.setProjectClassName(requireText(dto.getProjectClassName(), "项目分类名称不能为空"));
        existing.setUpdatedBy(normalize(operatorName, "system"));
        existing.setUpdatedAt(LocalDateTime.now());
        financeProjectClassMapper.updateById(existing);
        return toClassSummary(requireProjectClass(normalizedCompanyId, targetCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateProjectClassStatus(String companyId, String projectClassCode, FinanceProjectStatusDTO dto, String operatorName) {
        FinanceProjectClass existing = requireProjectClass(companyId, projectClassCode);
        int nextStatus = normalizeFlag(dto == null ? null : dto.getStatus(), 1);
        if (nextStatus == 0 && hasActiveProjects(existing.getCompanyId(), existing.getProjectClassCode())) {
            throw new IllegalStateException("当前项目分类下仍存在启用中的项目档案，请先停用项目后再停用分类");
        }
        existing.setStatus(nextStatus);
        existing.setUpdatedBy(normalize(operatorName, "system"));
        existing.setUpdatedAt(LocalDateTime.now());
        financeProjectClassMapper.updateById(existing);
        return Boolean.TRUE;
    }

    @Override
    public List<FinanceProjectSummaryVO> listProjects(String companyId, String keyword, String projectClassCode, Integer status, Integer bclose) {
        String normalizedCompanyId = requireCompanyId(companyId);
        QueryWrapper<FinanceProjectArchive> query = new QueryWrapper<>();
        query.eq("company_id", normalizedCompanyId);
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword != null) {
            query.and(wrapper -> wrapper
                    .like("citemcode", normalizedKeyword)
                    .or()
                    .like("citemname", normalizedKeyword));
        }
        String normalizedProjectClassCode = trimToNull(projectClassCode);
        if (normalizedProjectClassCode != null) {
            query.eq("citemccode", normalizedProjectClassCode);
        }
        if (status != null) {
            query.eq("status", normalizeFlag(status, 1));
        }
        if (bclose != null) {
            query.eq("bclose", normalizeFlag(bclose, 0));
        }
        query.orderByAsc("sort_order", "citemcode", "id");

        Map<String, String> classNameMap = loadProjectClassNameMap(normalizedCompanyId);
        return financeProjectArchiveMapper.selectList(query).stream()
                .map(item -> toProjectSummary(item, classNameMap.get(item.getCitemccode())))
                .toList();
    }

    @Override
    public FinanceProjectDetailVO getProjectDetail(String companyId, String projectCode) {
        FinanceProjectArchive project = requireProject(companyId, projectCode);
        return toProjectDetail(project, requireProjectClass(project.getCompanyId(), project.getCitemccode()).getProjectClassName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceProjectDetailVO createProject(String companyId, FinanceProjectSaveDTO dto, String operatorName) {
        validateProjectPayload(dto);
        String normalizedCompanyId = requireCompanyId(companyId);
        requireEnabledCompany(normalizedCompanyId);

        String projectCode = requireText(dto.getCitemcode(), "项目编码不能为空");
        if (findProject(normalizedCompanyId, projectCode) != null) {
            throw new IllegalStateException("当前公司下已存在相同项目编码");
        }
        FinanceProjectClass projectClass = requireEnabledProjectClass(normalizedCompanyId, dto.getCitemccode());

        LocalDateTime now = LocalDateTime.now();
        FinanceProjectArchive entity = new FinanceProjectArchive();
        entity.setCompanyId(normalizedCompanyId);
        entity.setCitemcode(projectCode);
        entity.setCitemname(requireText(dto.getCitemname(), "项目名称不能为空"));
        entity.setCitemccode(projectClass.getProjectClassCode());
        entity.setIotherused(dto.getIotherused() == null ? 0 : Math.max(dto.getIotherused(), 0));
        entity.setDEndDate(dto.getDEndDate());
        entity.setStatus(1);
        entity.setBclose(0);
        entity.setSortOrder(resolveNextProjectSortOrder(normalizedCompanyId));
        entity.setCreatedBy(normalize(operatorName, "system"));
        entity.setUpdatedBy(normalize(operatorName, "system"));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        financeProjectArchiveMapper.insert(entity);
        return toProjectDetail(requireProject(normalizedCompanyId, projectCode), projectClass.getProjectClassName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceProjectDetailVO updateProject(String companyId, String projectCode, FinanceProjectSaveDTO dto, String operatorName) {
        validateProjectPayload(dto);
        FinanceProjectArchive existing = requireProject(companyId, projectCode);
        String normalizedProjectCode = requireText(dto.getCitemcode(), "项目编码不能为空");
        if (!Objects.equals(existing.getCitemcode(), normalizedProjectCode)) {
            throw new IllegalArgumentException("首版不支持修改项目编码");
        }

        FinanceProjectClass targetClass = requireProjectClass(existing.getCompanyId(), dto.getCitemccode());
        if ((isReferencedByVoucher(existing.getCompanyId(), existing.getCitemcode()) || normalizeNonNegative(existing.getIotherused()) > 0)
                && !Objects.equals(existing.getCitemccode(), targetClass.getProjectClassCode())) {
            throw new IllegalStateException("当前项目已被引用，不能修改受控字段");
        }
        if (!Objects.equals(existing.getCitemccode(), targetClass.getProjectClassCode())
                && !Objects.equals(normalizeFlag(targetClass.getStatus(), 1), 1)) {
            throw new IllegalStateException("项目分类未启用，不能调整到该分类");
        }

        existing.setCitemname(requireText(dto.getCitemname(), "项目名称不能为空"));
        existing.setCitemccode(targetClass.getProjectClassCode());
        existing.setIotherused(dto.getIotherused() == null ? normalizeNonNegative(existing.getIotherused()) : Math.max(dto.getIotherused(), 0));
        existing.setDEndDate(dto.getDEndDate());
        existing.setUpdatedBy(normalize(operatorName, "system"));
        existing.setUpdatedAt(LocalDateTime.now());
        financeProjectArchiveMapper.updateById(existing);
        return toProjectDetail(requireProject(existing.getCompanyId(), existing.getCitemcode()), targetClass.getProjectClassName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateProjectStatus(String companyId, String projectCode, FinanceProjectStatusDTO dto, String operatorName) {
        FinanceProjectArchive existing = requireProject(companyId, projectCode);
        int nextStatus = normalizeFlag(dto == null ? null : dto.getStatus(), 1);
        if (nextStatus == 1) {
            requireEnabledProjectClass(existing.getCompanyId(), existing.getCitemccode());
        }
        existing.setStatus(nextStatus);
        existing.setUpdatedBy(normalize(operatorName, "system"));
        existing.setUpdatedAt(LocalDateTime.now());
        financeProjectArchiveMapper.updateById(existing);
        return Boolean.TRUE;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateProjectCloseStatus(String companyId, String projectCode, FinanceProjectCloseDTO dto, String operatorName) {
        FinanceProjectArchive existing = requireProject(companyId, projectCode);
        int nextClose = normalizeFlag(dto == null ? null : dto.getBclose(), 0);
        if (nextClose == 0) {
            requireEnabledProjectClass(existing.getCompanyId(), existing.getCitemccode());
            if (!Objects.equals(normalizeFlag(existing.getStatus(), 1), 1)) {
                throw new IllegalStateException("停用项目不能直接解封，请先启用后再解封");
            }
        }
        existing.setBclose(nextClose);
        existing.setUpdatedBy(normalize(operatorName, "system"));
        existing.setUpdatedAt(LocalDateTime.now());
        financeProjectArchiveMapper.updateById(existing);
        return Boolean.TRUE;
    }

    private FinanceProjectClassSummaryVO toClassSummary(FinanceProjectClass entity) {
        FinanceProjectClassSummaryVO summary = new FinanceProjectClassSummaryVO();
        summary.setId(entity.getId());
        summary.setCompanyId(entity.getCompanyId());
        summary.setProjectClassCode(entity.getProjectClassCode());
        summary.setProjectClassName(entity.getProjectClassName());
        summary.setStatus(entity.getStatus());
        summary.setSortOrder(entity.getSortOrder());
        summary.setCreatedBy(entity.getCreatedBy());
        summary.setUpdatedBy(entity.getUpdatedBy());
        summary.setCreatedAt(entity.getCreatedAt());
        summary.setUpdatedAt(entity.getUpdatedAt());
        summary.setHasProjects(hasProjects(entity.getCompanyId(), entity.getProjectClassCode()));
        return summary;
    }

    private FinanceProjectSummaryVO toProjectSummary(FinanceProjectArchive entity, String projectClassName) {
        FinanceProjectSummaryVO summary = new FinanceProjectSummaryVO();
        summary.setId(entity.getId());
        summary.setCompanyId(entity.getCompanyId());
        summary.setCitemcode(entity.getCitemcode());
        summary.setCitemname(entity.getCitemname());
        summary.setBclose(entity.getBclose());
        summary.setCitemccode(entity.getCitemccode());
        summary.setProjectClassName(projectClassName);
        summary.setIotherused(entity.getIotherused());
        summary.setDEndDate(entity.getDEndDate());
        summary.setStatus(entity.getStatus());
        summary.setSortOrder(entity.getSortOrder());
        summary.setCreatedBy(entity.getCreatedBy());
        summary.setUpdatedBy(entity.getUpdatedBy());
        summary.setCreatedAt(entity.getCreatedAt());
        summary.setUpdatedAt(entity.getUpdatedAt());
        summary.setReferencedByVoucher(isReferencedByVoucher(entity.getCompanyId(), entity.getCitemcode()));
        return summary;
    }

    private FinanceProjectDetailVO toProjectDetail(FinanceProjectArchive entity, String projectClassName) {
        FinanceProjectDetailVO detail = new FinanceProjectDetailVO();
        detail.setId(entity.getId());
        detail.setCompanyId(entity.getCompanyId());
        detail.setCitemcode(entity.getCitemcode());
        detail.setCitemname(entity.getCitemname());
        detail.setBclose(entity.getBclose());
        detail.setCitemccode(entity.getCitemccode());
        detail.setProjectClassName(projectClassName);
        detail.setIotherused(entity.getIotherused());
        detail.setDEndDate(entity.getDEndDate());
        detail.setStatus(entity.getStatus());
        detail.setSortOrder(entity.getSortOrder());
        detail.setCreatedBy(entity.getCreatedBy());
        detail.setUpdatedBy(entity.getUpdatedBy());
        detail.setCreatedAt(entity.getCreatedAt());
        detail.setUpdatedAt(entity.getUpdatedAt());
        detail.setReferencedByVoucher(isReferencedByVoucher(entity.getCompanyId(), entity.getCitemcode()));
        return detail;
    }

    private FinanceProjectClass requireProjectClass(String companyId, String projectClassCode) {
        FinanceProjectClass entity = findProjectClass(requireCompanyId(companyId), projectClassCode);
        if (entity == null) {
            throw new IllegalStateException("项目分类不存在");
        }
        return entity;
    }

    private FinanceProjectClass requireEnabledProjectClass(String companyId, String projectClassCode) {
        FinanceProjectClass entity = requireProjectClass(companyId, projectClassCode);
        if (!Objects.equals(normalizeFlag(entity.getStatus(), 1), 1)) {
            throw new IllegalStateException("项目分类未启用，不能作为正式项目来源");
        }
        return entity;
    }

    private FinanceProjectArchive requireProject(String companyId, String projectCode) {
        FinanceProjectArchive entity = findProject(requireCompanyId(companyId), projectCode);
        if (entity == null) {
            throw new IllegalStateException("项目档案不存在");
        }
        return entity;
    }

    private FinanceProjectClass findProjectClass(String companyId, String projectClassCode) {
        String normalizedCompanyId = requireCompanyId(companyId);
        String normalizedCode = requireText(projectClassCode, "项目分类编码不能为空");
        return financeProjectClassMapper.selectOne(
                Wrappers.<FinanceProjectClass>lambdaQuery()
                        .eq(FinanceProjectClass::getCompanyId, normalizedCompanyId)
                        .eq(FinanceProjectClass::getProjectClassCode, normalizedCode)
                        .last("limit 1")
        );
    }

    private FinanceProjectArchive findProject(String companyId, String projectCode) {
        String normalizedCompanyId = requireCompanyId(companyId);
        String normalizedCode = requireText(projectCode, "项目编码不能为空");
        return financeProjectArchiveMapper.selectOne(
                Wrappers.<FinanceProjectArchive>lambdaQuery()
                        .eq(FinanceProjectArchive::getCompanyId, normalizedCompanyId)
                        .eq(FinanceProjectArchive::getCitemcode, normalizedCode)
                        .last("limit 1")
        );
    }

    private boolean hasProjects(String companyId, String projectClassCode) {
        Long count = financeProjectArchiveMapper.selectCount(
                Wrappers.<FinanceProjectArchive>lambdaQuery()
                        .eq(FinanceProjectArchive::getCompanyId, requireCompanyId(companyId))
                        .eq(FinanceProjectArchive::getCitemccode, requireText(projectClassCode, "项目分类编码不能为空"))
        );
        return count != null && count > 0;
    }

    private boolean hasActiveProjects(String companyId, String projectClassCode) {
        Long count = financeProjectArchiveMapper.selectCount(
                Wrappers.<FinanceProjectArchive>lambdaQuery()
                        .eq(FinanceProjectArchive::getCompanyId, requireCompanyId(companyId))
                        .eq(FinanceProjectArchive::getCitemccode, requireText(projectClassCode, "项目分类编码不能为空"))
                        .eq(FinanceProjectArchive::getStatus, 1)
                        .eq(FinanceProjectArchive::getBclose, 0)
        );
        return count != null && count > 0;
    }

    private boolean isReferencedByVoucher(String companyId, String projectCode) {
        Long count = glAccvouchMapper.selectCount(
                Wrappers.lambdaQuery(com.finex.auth.entity.GlAccvouch.class)
                        .eq(com.finex.auth.entity.GlAccvouch::getCompanyId, requireCompanyId(companyId))
                        .eq(com.finex.auth.entity.GlAccvouch::getCitemId, requireText(projectCode, "项目编码不能为空"))
        );
        return count != null && count > 0;
    }

    private String requireCompanyId(String companyId) {
        return requireText(companyId, "公司主体不能为空");
    }

    private void requireEnabledCompany(String companyId) {
        SystemCompany company = systemCompanyMapper.selectById(companyId);
        if (company == null || !Objects.equals(company.getStatus(), 1)) {
            throw new IllegalStateException("当前公司不存在或已停用");
        }
    }

    private void validateProjectClassPayload(FinanceProjectClassSaveDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("项目分类数据不能为空");
        }
        requireText(dto.getProjectClassCode(), "项目分类编码不能为空");
        requireText(dto.getProjectClassName(), "项目分类名称不能为空");
    }

    private void validateProjectPayload(FinanceProjectSaveDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("项目档案数据不能为空");
        }
        requireText(dto.getCitemcode(), "项目编码不能为空");
        requireText(dto.getCitemname(), "项目名称不能为空");
        requireText(dto.getCitemccode(), "项目分类不能为空");
    }

    private int resolveNextProjectClassSortOrder(String companyId) {
        Long count = financeProjectClassMapper.selectCount(
                Wrappers.<FinanceProjectClass>lambdaQuery()
                        .eq(FinanceProjectClass::getCompanyId, requireCompanyId(companyId))
        );
        return (count == null ? 0 : count.intValue()) + 1;
    }

    private int resolveNextProjectSortOrder(String companyId) {
        Long count = financeProjectArchiveMapper.selectCount(
                Wrappers.<FinanceProjectArchive>lambdaQuery()
                        .eq(FinanceProjectArchive::getCompanyId, requireCompanyId(companyId))
        );
        return (count == null ? 0 : count.intValue()) + 1;
    }

    private Map<String, String> loadProjectClassNameMap(String companyId) {
        Map<String, String> result = new LinkedHashMap<>();
        financeProjectClassMapper.selectList(
                        Wrappers.<FinanceProjectClass>lambdaQuery()
                                .eq(FinanceProjectClass::getCompanyId, requireCompanyId(companyId))
                ).forEach(item -> result.put(item.getProjectClassCode(), item.getProjectClassName()));
        return result;
    }

    private String requireText(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int normalizeFlag(Integer value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value == 0 ? 0 : 1;
    }

    private int normalizeNonNegative(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private String normalize(String value, String defaultValue) {
        String normalized = trimToNull(value);
        return normalized == null ? defaultValue : normalized;
    }

    private FinanceProjectArchiveOptionVO option(String value, String label) {
        FinanceProjectArchiveOptionVO option = new FinanceProjectArchiveOptionVO();
        option.setValue(value);
        option.setLabel(label);
        return option;
    }
}
