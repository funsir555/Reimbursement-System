package com.finex.auth.service.impl.financearchive;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSubjectCloseDTO;
import com.finex.auth.dto.FinanceAccountSubjectDetailVO;
import com.finex.auth.dto.FinanceAccountSubjectSaveDTO;
import com.finex.auth.dto.FinanceAccountSubjectStatusDTO;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;

import java.util.Objects;

public class FinanceAccountSubjectMutationDomainSupport extends AbstractFinanceAccountSubjectArchiveSupport {

    public FinanceAccountSubjectMutationDomainSupport(
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            SystemCompanyMapper systemCompanyMapper,
            GlAccvouchMapper glAccvouchMapper,
            ObjectMapper objectMapper
    ) {
        super(financeAccountSubjectMapper, systemCompanyMapper, glAccvouchMapper, objectMapper);
    }

    public FinanceAccountSubjectDetailVO createSubject(String companyId, FinanceAccountSubjectSaveDTO dto, String operatorName) {
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
        String parentSubjectCode = trimToNull(dto.getParentSubjectCode());
        if (parentSubjectCode != null) {
            parent = requireSubject(normalizedCompanyId, parentSubjectCode);
            if (!subjectCode.startsWith(parent.getSubjectCode())) {
                throw new IllegalArgumentException("子科目编码必须以前级科目编码为前缀");
            }
            if (!isEnabled(parent.getStatus()) || isClosed(parent.getBclose())) {
                throw new IllegalStateException("上级科目未启用或已封存，不能新增子科目");
            }
        }

        FinanceAccountSubject subject = new FinanceAccountSubject();
        subject.setCompanyId(normalizedCompanyId);
        subject.setSubjectCode(subjectCode);
        subject.setParentSubjectCode(parentSubjectCode);
        subject.setSubjectLevel(resolveSubjectLevel(dto, parent));
        subject.setSubjectName(requireText(dto.getSubjectName(), "科目名称不能为空"));
        subject.setSubjectCategory(resolveCategory(dto.getSubjectCategory()));
        subject.setBalanceDirection(resolveBalanceDirection(subjectCode, subject.getSubjectCategory(), parent));
        subject.setTemplateCode(null);
        subject.setSortOrder(resolveNextSortOrder(normalizedCompanyId, parentSubjectCode));
        applyMutableFields(subject, dto, true);
        if (parent != null) {
            parent.setLeafFlag(0);
            financeAccountSubjectMapper.updateById(parent);
        }
        financeAccountSubjectMapper.insert(subject);
        return toDetail(requireSubject(normalizedCompanyId, subjectCode));
    }

    public FinanceAccountSubjectDetailVO updateSubject(String companyId, String subjectCode, FinanceAccountSubjectSaveDTO dto, String operatorName) {
        validateSavePayload(dto);
        String normalizedCompanyId = requireCompanyId(companyId);
        String normalizedSubjectCode = requireText(subjectCode, "科目编码不能为空");
        FinanceAccountSubject existing = requireSubject(normalizedCompanyId, normalizedSubjectCode);

        if (!Objects.equals(normalizedSubjectCode, requireText(dto.getSubjectCode(), "科目编码不能为空"))) {
            throw new IllegalArgumentException("首版不支持修改科目编码");
        }
        String payloadParentCode = trimToNull(dto.getParentSubjectCode());
        if (!Objects.equals(trimToNull(existing.getParentSubjectCode()), payloadParentCode)) {
            throw new IllegalArgumentException("首版不支持修改上级科目");
        }
        Integer payloadLevel = dto.getSubjectLevel();
        if (payloadLevel != null && !Objects.equals(existing.getSubjectLevel(), payloadLevel)) {
            throw new IllegalArgumentException("首版不支持修改科目级次");
        }

        FinanceAccountSubject snapshot = cloneSubject(existing);
        applyMutableFields(existing, dto, false);
        if (hasChildren(normalizedCompanyId, normalizedSubjectCode)) {
            existing.setLeafFlag(0);
        }
        validateControlledFieldChanges(snapshot, existing);
        financeAccountSubjectMapper.updateById(existing);
        return toDetail(requireSubject(normalizedCompanyId, normalizedSubjectCode));
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
