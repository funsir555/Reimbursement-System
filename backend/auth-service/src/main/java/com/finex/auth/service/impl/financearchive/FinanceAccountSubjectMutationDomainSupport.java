// 业务域：财务档案
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

package com.finex.auth.service.impl.financearchive;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSubjectCloseDTO;
import com.finex.auth.dto.FinanceAccountSubjectDerivedDefaultsVO;
import com.finex.auth.dto.FinanceAccountSubjectDetailVO;
import com.finex.auth.dto.FinanceAccountSubjectSaveDTO;
import com.finex.auth.dto.FinanceAccountSubjectStatusDTO;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateSubjectMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;

import java.util.Objects;

/**
 * FinanceAccountSubjectMutationDomainSupport：领域规则支撑类。
 * 承接 财务账户科目的核心业务规则。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
public class FinanceAccountSubjectMutationDomainSupport extends AbstractFinanceAccountSubjectArchiveSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FinanceAccountSubjectMutationDomainSupport(
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            SystemCompanyMapper systemCompanyMapper,
            GlAccvouchMapper glAccvouchMapper,
            ObjectMapper objectMapper,
            FinanceAccountSetMapper financeAccountSetMapper,
            FinanceAccountSetTemplateSubjectMapper financeAccountSetTemplateSubjectMapper
    ) {
        super(
                financeAccountSubjectMapper,
                systemCompanyMapper,
                glAccvouchMapper,
                objectMapper,
                financeAccountSetMapper,
                financeAccountSetTemplateSubjectMapper
        );
    }

    /**
     * 创建科目。
     */
    public FinanceAccountSubjectDetailVO createSubject(String companyId, FinanceAccountSubjectSaveDTO dto, String operatorName) {
        normalizeMutableTextFields(dto);
        validateSavePayload(dto);
        String normalizedCompanyId = requireCompanyId(companyId);
        requireEnabledCompany(normalizedCompanyId);

        String subjectCode = requireText(dto.getSubjectCode(), "科目编码不能为空");
        if (financeAccountSubjectMapper.selectOne(
                Wrappers.<FinanceAccountSubject>lambdaQuery()
                        .eq(FinanceAccountSubject::getCompanyId, normalizedCompanyId)
                        .eq(FinanceAccountSubject::getSubjectCode, subjectCode)
                        .last("limit 1")
        ) != null) {
            throw new IllegalStateException("当前公司下已存在相同科目编码");
        }

        FinanceAccountSubject parent = null;
        if (!isRootSubjectCode(subjectCode)) {
            parent = requireMatchedParentForCreate(normalizedCompanyId, subjectCode);
            if (!isEnabled(parent.getStatus()) || isClosed(parent.getBclose())) {
                throw new IllegalStateException("\u4e0a\u7ea7\u79d1\u76ee\u672a\u542f\u7528\u6216\u5df2\u5c01\u5b58\uff0c\u4e0d\u80fd\u65b0\u589e\u5b50\u79d1\u76ee");
            }
        }

        FinanceAccountSubject subject = new FinanceAccountSubject();
        subject.setCompanyId(normalizedCompanyId);
        subject.setSubjectCode(subjectCode);
        applyDerivedStructure(subject, parent);
        subject.setTemplateCode(null);
        subject.setSortOrder(resolveNextSortOrder(normalizedCompanyId, parent == null ? null : parent.getSubjectCode()));
        applyMutableFields(subject, dto, true);
        applyDerivedControlledFields(subject, true);
        if (parent != null) {
            parent.setLeafFlag(0);
            financeAccountSubjectMapper.updateById(parent);
        }
        financeAccountSubjectMapper.insert(subject);
        return toDetail(requireSubject(normalizedCompanyId, subjectCode));
    }

    /**
     * 更新科目。
     */
    public FinanceAccountSubjectDetailVO updateSubject(String companyId, String subjectCode, FinanceAccountSubjectSaveDTO dto, String operatorName) {
        normalizeMutableTextFields(dto);
        validateSavePayload(dto);
        String normalizedCompanyId = requireCompanyId(companyId);
        String normalizedSubjectCode = requireText(subjectCode, "科目编码不能为空");
        FinanceAccountSubject existing = requireSubject(normalizedCompanyId, normalizedSubjectCode);

        if (!Objects.equals(normalizedSubjectCode, requireText(dto.getSubjectCode(), "科目编码不能为空"))) {
            throw new IllegalArgumentException("首版不支持修改科目编码");
        }
        FinanceAccountSubject snapshot = cloneSubject(existing);
        FinanceAccountSubject parent = null;
        String existingParentCode = trimToNull(existing.getParentSubjectCode());
        if (existingParentCode != null) {
            parent = requireSubject(normalizedCompanyId, existingParentCode);
        }
        applyDerivedStructure(existing, parent);
        applyMutableFields(existing, dto, false);
        applyDerivedControlledFields(existing, false);
        validateControlledFieldChanges(snapshot, existing);
        financeAccountSubjectMapper.updateById(existing);
        return toDetail(requireSubject(normalizedCompanyId, normalizedSubjectCode));
    }

    /**
     * 更新Status。
     */
    public FinanceAccountSubjectDerivedDefaultsVO getDerivedDefaults(String companyId, String subjectCode) {
        String normalizedCompanyId = requireCompanyId(companyId);
        requireEnabledCompany(normalizedCompanyId);
        return deriveDefaults(normalizedCompanyId, subjectCode, null);
    }

    public Boolean updateStatus(String companyId, String subjectCode, FinanceAccountSubjectStatusDTO dto, String operatorName) {
        FinanceAccountSubject subject = requireSubject(companyId, subjectCode);
        int nextStatus = normalizeFlag(dto == null ? null : dto.getStatus(), 1);
        if (nextStatus == 1) {
            validateParentAvailableForEnable(subject);
        } else if (hasEnabledChildren(subject.getCompanyId(), subject.getSubjectCode())) {
            throw new IllegalStateException("请先停用下级科目，再停用当前科目");
        }
        subject.setStatus(nextStatus);
        financeAccountSubjectMapper.updateById(subject);
        return Boolean.TRUE;
    }

    /**
     * 更新CloseStatus。
     */
    public Boolean updateCloseStatus(String companyId, String subjectCode, FinanceAccountSubjectCloseDTO dto, String operatorName) {
        FinanceAccountSubject subject = requireSubject(companyId, subjectCode);
        int nextClose = normalizeFlag(dto == null ? null : dto.getBclose(), 0);
        if (nextClose == 0) {
            validateParentAvailableForEnable(subject);
        } else if (hasOpenChildren(subject.getCompanyId(), subject.getSubjectCode())) {
            throw new IllegalStateException("请先封存下级科目，再封存当前科目");
        }
        subject.setBclose(nextClose);
        financeAccountSubjectMapper.updateById(subject);
        return Boolean.TRUE;
    }
}
