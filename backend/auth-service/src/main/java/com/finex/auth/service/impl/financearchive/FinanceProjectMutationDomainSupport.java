// 业务域：财务档案
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

package com.finex.auth.service.impl.financearchive;

import com.finex.auth.dto.FinanceProjectCloseDTO;
import com.finex.auth.dto.FinanceProjectDetailVO;
import com.finex.auth.dto.FinanceProjectSaveDTO;
import com.finex.auth.dto.FinanceProjectStatusDTO;
import com.finex.auth.entity.FinanceProjectArchive;
import com.finex.auth.entity.FinanceProjectClass;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * FinanceProjectMutationDomainSupport：领域规则支撑类。
 * 承接 财务项目的核心业务规则。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
public class FinanceProjectMutationDomainSupport extends AbstractFinanceProjectArchiveSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FinanceProjectMutationDomainSupport(
            FinanceProjectClassMapper financeProjectClassMapper,
            FinanceProjectArchiveMapper financeProjectArchiveMapper,
            SystemCompanyMapper systemCompanyMapper,
            GlAccvouchMapper glAccvouchMapper
    ) {
        super(financeProjectClassMapper, financeProjectArchiveMapper, systemCompanyMapper, glAccvouchMapper);
    }

    /**
     * 创建项目。
     */
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

    /**
     * 更新项目。
     */
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

    /**
     * 更新项目Status。
     */
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

    /**
     * 更新项目CloseStatus。
     */
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
}
