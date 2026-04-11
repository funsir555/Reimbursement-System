package com.finex.auth.service.impl.financearchive;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.FinanceProjectArchiveMetaVO;
import com.finex.auth.dto.FinanceProjectArchiveOptionVO;
import com.finex.auth.dto.FinanceProjectClassSaveDTO;
import com.finex.auth.dto.FinanceProjectClassSummaryVO;
import com.finex.auth.dto.FinanceProjectDetailVO;
import com.finex.auth.dto.FinanceProjectSaveDTO;
import com.finex.auth.dto.FinanceProjectSummaryVO;
import com.finex.auth.entity.FinanceProjectArchive;
import com.finex.auth.entity.FinanceProjectClass;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public abstract class AbstractFinanceProjectArchiveSupport {

    protected static final Pattern PROJECT_CLASS_CODE_PATTERN = Pattern.compile("^\\d{2}$");
    protected static final Pattern PROJECT_CODE_PATTERN = Pattern.compile("^\\d{6}$");

    protected final FinanceProjectClassMapper financeProjectClassMapper;
    protected final FinanceProjectArchiveMapper financeProjectArchiveMapper;
    protected final SystemCompanyMapper systemCompanyMapper;
    protected final GlAccvouchMapper glAccvouchMapper;

    protected AbstractFinanceProjectArchiveSupport(
            FinanceProjectClassMapper financeProjectClassMapper,
            FinanceProjectArchiveMapper financeProjectArchiveMapper,
            SystemCompanyMapper systemCompanyMapper,
            GlAccvouchMapper glAccvouchMapper
    ) {
        this.financeProjectClassMapper = financeProjectClassMapper;
        this.financeProjectArchiveMapper = financeProjectArchiveMapper;
        this.systemCompanyMapper = systemCompanyMapper;
        this.glAccvouchMapper = glAccvouchMapper;
    }

    protected FinanceProjectClassSummaryVO toClassSummary(FinanceProjectClass entity) {
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

    protected FinanceProjectSummaryVO toProjectSummary(FinanceProjectArchive entity, String projectClassName) {
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

    protected FinanceProjectDetailVO toProjectDetail(FinanceProjectArchive entity, String projectClassName) {
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

    protected FinanceProjectClass requireProjectClass(String companyId, String projectClassCode) {
        FinanceProjectClass entity = findProjectClass(requireCompanyId(companyId), projectClassCode);
        if (entity == null) {
            throw new IllegalStateException("项目分类不存在");
        }
        return entity;
    }

    protected FinanceProjectClass requireEnabledProjectClass(String companyId, String projectClassCode) {
        FinanceProjectClass entity = requireProjectClass(companyId, projectClassCode);
        if (!Objects.equals(normalizeFlag(entity.getStatus(), 1), 1)) {
            throw new IllegalStateException("项目分类未启用，不能作为正式项目来源");
        }
        return entity;
    }

    protected FinanceProjectArchive requireProject(String companyId, String projectCode) {
        FinanceProjectArchive entity = findProject(requireCompanyId(companyId), projectCode);
        if (entity == null) {
            throw new IllegalStateException("项目档案不存在");
        }
        return entity;
    }

    protected FinanceProjectClass findProjectClass(String companyId, String projectClassCode) {
        String normalizedCompanyId = requireCompanyId(companyId);
        String normalizedCode = requireText(projectClassCode, "项目分类编码不能为空");
        return financeProjectClassMapper.selectOne(
                Wrappers.<FinanceProjectClass>lambdaQuery()
                        .eq(FinanceProjectClass::getCompanyId, normalizedCompanyId)
                        .eq(FinanceProjectClass::getProjectClassCode, normalizedCode)
                        .last("limit 1")
        );
    }

    protected FinanceProjectArchive findProject(String companyId, String projectCode) {
        String normalizedCompanyId = requireCompanyId(companyId);
        String normalizedCode = requireText(projectCode, "项目编码不能为空");
        return financeProjectArchiveMapper.selectOne(
                Wrappers.<FinanceProjectArchive>lambdaQuery()
                        .eq(FinanceProjectArchive::getCompanyId, normalizedCompanyId)
                        .eq(FinanceProjectArchive::getCitemcode, normalizedCode)
                        .last("limit 1")
        );
    }

    protected boolean hasProjects(String companyId, String projectClassCode) {
        Long count = financeProjectArchiveMapper.selectCount(
                Wrappers.<FinanceProjectArchive>lambdaQuery()
                        .eq(FinanceProjectArchive::getCompanyId, requireCompanyId(companyId))
                        .eq(FinanceProjectArchive::getCitemccode, requireText(projectClassCode, "项目分类编码不能为空"))
        );
        return count != null && count > 0;
    }

    protected boolean hasActiveProjects(String companyId, String projectClassCode) {
        Long count = financeProjectArchiveMapper.selectCount(
                Wrappers.<FinanceProjectArchive>lambdaQuery()
                        .eq(FinanceProjectArchive::getCompanyId, requireCompanyId(companyId))
                        .eq(FinanceProjectArchive::getCitemccode, requireText(projectClassCode, "项目分类编码不能为空"))
                        .eq(FinanceProjectArchive::getStatus, 1)
                        .eq(FinanceProjectArchive::getBclose, 0)
        );
        return count != null && count > 0;
    }

    protected boolean isReferencedByVoucher(String companyId, String projectCode) {
        Long count = glAccvouchMapper.selectCount(
                Wrappers.<GlAccvouch>lambdaQuery()
                        .eq(GlAccvouch::getCompanyId, requireCompanyId(companyId))
                        .eq(GlAccvouch::getCitemId, requireText(projectCode, "项目编码不能为空"))
        );
        return count != null && count > 0;
    }

    protected String requireCompanyId(String companyId) {
        return requireText(companyId, "公司主体不能为空");
    }

    protected void requireEnabledCompany(String companyId) {
        SystemCompany company = systemCompanyMapper.selectById(companyId);
        if (company == null || !Objects.equals(company.getStatus(), 1)) {
            throw new IllegalStateException("当前公司不存在或已停用");
        }
    }

    protected void validateProjectClassPayload(FinanceProjectClassSaveDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("项目分类数据不能为空");
        }
        String projectClassCode = requireText(dto.getProjectClassCode(), "项目分类编码不能为空");
        if (!PROJECT_CLASS_CODE_PATTERN.matcher(projectClassCode).matches()) {
            throw new IllegalArgumentException("项目分类编码必须为2位数字文本");
        }
        requireText(dto.getProjectClassName(), "项目分类名称不能为空");
    }

    protected void validateProjectPayload(FinanceProjectSaveDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("项目档案数据不能为空");
        }
        String projectCode = requireText(dto.getCitemcode(), "项目编码不能为空");
        if (!PROJECT_CODE_PATTERN.matcher(projectCode).matches()) {
            throw new IllegalArgumentException("项目编码必须为6位数字文本");
        }
        requireText(dto.getCitemname(), "项目名称不能为空");
        String projectClassCode = requireText(dto.getCitemccode(), "项目分类不能为空");
        if (!PROJECT_CLASS_CODE_PATTERN.matcher(projectClassCode).matches()) {
            throw new IllegalArgumentException("项目分类编码必须为2位数字文本");
        }
    }

    protected int resolveNextProjectClassSortOrder(String companyId) {
        Long count = financeProjectClassMapper.selectCount(
                Wrappers.<FinanceProjectClass>lambdaQuery()
                        .eq(FinanceProjectClass::getCompanyId, requireCompanyId(companyId))
        );
        return (count == null ? 0 : count.intValue()) + 1;
    }

    protected int resolveNextProjectSortOrder(String companyId) {
        Long count = financeProjectArchiveMapper.selectCount(
                Wrappers.<FinanceProjectArchive>lambdaQuery()
                        .eq(FinanceProjectArchive::getCompanyId, requireCompanyId(companyId))
        );
        return (count == null ? 0 : count.intValue()) + 1;
    }

    protected Map<String, String> loadProjectClassNameMap(String companyId) {
        Map<String, String> result = new LinkedHashMap<>();
        financeProjectClassMapper.selectList(
                Wrappers.<FinanceProjectClass>lambdaQuery()
                        .eq(FinanceProjectClass::getCompanyId, requireCompanyId(companyId))
        ).forEach(item -> result.put(item.getProjectClassCode(), item.getProjectClassName()));
        return result;
    }

    protected String requireText(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    protected int normalizeFlag(Integer value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value == 0 ? 0 : 1;
    }

    protected int normalizeNonNegative(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    protected String normalize(String value, String defaultValue) {
        String normalized = trimToNull(value);
        return normalized == null ? defaultValue : normalized;
    }

    protected FinanceProjectArchiveOptionVO option(String value, String label) {
        FinanceProjectArchiveOptionVO option = new FinanceProjectArchiveOptionVO();
        option.setValue(value);
        option.setLabel(label);
        return option;
    }

    protected List<FinanceProjectArchiveOptionVO> buildStatusOptions() {
        return List.of(option("1", "启用"), option("0", "停用"));
    }

    protected List<FinanceProjectArchiveOptionVO> buildCloseStatusOptions() {
        return List.of(option("0", "未封存"), option("1", "已封存"));
    }

    protected FinanceProjectArchiveMetaVO createMetaContainer() {
        return new FinanceProjectArchiveMetaVO();
    }
}