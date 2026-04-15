// 业务域：财务档案
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

package com.finex.auth.service.impl.financearchive;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.finex.auth.dto.FinanceProjectClassSaveDTO;
import com.finex.auth.dto.FinanceProjectClassSummaryVO;
import com.finex.auth.dto.FinanceProjectStatusDTO;
import com.finex.auth.entity.FinanceProjectClass;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * FinanceProjectClassDomainSupport：领域规则支撑类。
 * 承接 财务项目Class的核心业务规则。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
public class FinanceProjectClassDomainSupport extends AbstractFinanceProjectArchiveSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FinanceProjectClassDomainSupport(
            FinanceProjectClassMapper financeProjectClassMapper,
            FinanceProjectArchiveMapper financeProjectArchiveMapper,
            SystemCompanyMapper systemCompanyMapper,
            GlAccvouchMapper glAccvouchMapper
    ) {
        super(financeProjectClassMapper, financeProjectArchiveMapper, systemCompanyMapper, glAccvouchMapper);
    }

    /**
     * 查询项目Classes列表。
     */
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

    /**
     * 创建项目Class。
     */
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

    /**
     * 更新项目Class。
     */
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

    /**
     * 更新项目ClassStatus。
     */
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
}
