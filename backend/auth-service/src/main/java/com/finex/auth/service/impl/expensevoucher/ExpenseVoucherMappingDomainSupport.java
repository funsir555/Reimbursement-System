package com.finex.auth.service.impl.expensevoucher;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ExpenseVoucherPageVO;
import com.finex.auth.dto.ExpenseVoucherSubjectMappingSaveDTO;
import com.finex.auth.dto.ExpenseVoucherSubjectMappingVO;
import com.finex.auth.dto.ExpenseVoucherTemplatePolicySaveDTO;
import com.finex.auth.dto.ExpenseVoucherTemplatePolicyVO;
import com.finex.auth.entity.ExpVoucherSubjectMapping;
import com.finex.auth.entity.ExpVoucherTemplatePolicy;

import java.util.List;
import java.util.Map;

public class ExpenseVoucherMappingDomainSupport extends AbstractExpenseVoucherGenerationSupport {

    public ExpenseVoucherMappingDomainSupport(Dependencies dependencies) {
        super(dependencies);
    }

    public ExpenseVoucherPageVO<ExpenseVoucherTemplatePolicyVO> getTemplatePolicies(String companyId, String templateCode, Integer enabled, Integer page, Integer pageSize) {
        Map<String, String> companyMap = companyNameMap();
        List<ExpenseVoucherTemplatePolicyVO> rows = templatePolicyMapper.selectList(
                        Wrappers.<ExpVoucherTemplatePolicy>lambdaQuery()
                                .eq(hasText(companyId), ExpVoucherTemplatePolicy::getCompanyId, trim(companyId))
                                .eq(hasText(templateCode), ExpVoucherTemplatePolicy::getTemplateCode, trim(templateCode))
                                .eq(enabled != null, ExpVoucherTemplatePolicy::getEnabled, enabled)
                                .orderByDesc(ExpVoucherTemplatePolicy::getUpdatedAt, ExpVoucherTemplatePolicy::getId)
                ).stream()
                .map(item -> toTemplatePolicyVO(item, companyMap))
                .toList();
        return buildPage(rows, page, pageSize);
    }

    public ExpenseVoucherPageVO<ExpenseVoucherSubjectMappingVO> getSubjectMappings(String companyId, String templateCode, String expenseTypeCode, Integer enabled, Integer page, Integer pageSize) {
        Map<String, String> companyMap = companyNameMap();
        List<ExpenseVoucherSubjectMappingVO> rows = subjectMappingMapper.selectList(
                        Wrappers.<ExpVoucherSubjectMapping>lambdaQuery()
                                .eq(hasText(companyId), ExpVoucherSubjectMapping::getCompanyId, trim(companyId))
                                .eq(hasText(templateCode), ExpVoucherSubjectMapping::getTemplateCode, trim(templateCode))
                                .eq(hasText(expenseTypeCode), ExpVoucherSubjectMapping::getExpenseTypeCode, trim(expenseTypeCode))
                                .eq(enabled != null, ExpVoucherSubjectMapping::getEnabled, enabled)
                                .orderByDesc(ExpVoucherSubjectMapping::getUpdatedAt, ExpVoucherSubjectMapping::getId)
                ).stream()
                .map(item -> toSubjectMappingVO(item, companyMap))
                .toList();
        return buildPage(rows, page, pageSize);
    }

    public ExpenseVoucherTemplatePolicyVO createTemplatePolicy(ExpenseVoucherTemplatePolicySaveDTO dto, Long currentUserId, String currentUsername) {
        validateTemplatePolicy(dto, null);
        ExpVoucherTemplatePolicy entity = new ExpVoucherTemplatePolicy();
        applyTemplatePolicy(dto, entity, currentUsername);
        entity.setCreatedBy(currentUsername);
        templatePolicyMapper.insert(entity);
        return toTemplatePolicyVO(entity, companyNameMap());
    }

    public ExpenseVoucherTemplatePolicyVO updateTemplatePolicy(Long id, ExpenseVoucherTemplatePolicySaveDTO dto, Long currentUserId, String currentUsername) {
        ExpVoucherTemplatePolicy entity = requireTemplatePolicy(id);
        validateTemplatePolicy(dto, id);
        applyTemplatePolicy(dto, entity, currentUsername);
        templatePolicyMapper.updateById(entity);
        return toTemplatePolicyVO(entity, companyNameMap());
    }

    public ExpenseVoucherSubjectMappingVO createSubjectMapping(ExpenseVoucherSubjectMappingSaveDTO dto, Long currentUserId, String currentUsername) {
        validateSubjectMapping(dto, null);
        ExpVoucherSubjectMapping entity = new ExpVoucherSubjectMapping();
        applySubjectMapping(dto, entity, currentUsername);
        entity.setCreatedBy(currentUsername);
        subjectMappingMapper.insert(entity);
        return toSubjectMappingVO(entity, companyNameMap());
    }

    public ExpenseVoucherSubjectMappingVO updateSubjectMapping(Long id, ExpenseVoucherSubjectMappingSaveDTO dto, Long currentUserId, String currentUsername) {
        ExpVoucherSubjectMapping entity = requireSubjectMapping(id);
        validateSubjectMapping(dto, id);
        applySubjectMapping(dto, entity, currentUsername);
        subjectMappingMapper.updateById(entity);
        return toSubjectMappingVO(entity, companyNameMap());
    }
}
