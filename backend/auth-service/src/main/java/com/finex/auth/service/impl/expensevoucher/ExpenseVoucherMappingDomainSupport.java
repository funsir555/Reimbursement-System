// 业务域：报销凭证生成与推送
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 报销单凭证生成接口和财务操作入口，下游会继续协调 凭证映射、推送记录和报销单凭证状态。
// 风险提醒：改坏后最容易影响 重复生成凭证、凭证内容错误和推送记录不一致。

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

/**
 * ExpenseVoucherMappingDomainSupport：领域规则支撑类。
 * 承接 报销单凭证映射的核心业务规则。
 * 改这里时，要特别关注 重复生成凭证、凭证内容错误和推送记录不一致是否会被一起带坏。
 */
public class ExpenseVoucherMappingDomainSupport extends AbstractExpenseVoucherGenerationSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public ExpenseVoucherMappingDomainSupport(Dependencies dependencies) {
        super(dependencies);
    }

    /**
     * 获取模板Policies。
     */
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

    /**
     * 获取科目映射。
     */
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

    /**
     * 创建模板Policy。
     */
    public ExpenseVoucherTemplatePolicyVO createTemplatePolicy(ExpenseVoucherTemplatePolicySaveDTO dto, Long currentUserId, String currentUsername) {
        validateTemplatePolicy(dto, null);
        ExpVoucherTemplatePolicy entity = new ExpVoucherTemplatePolicy();
        applyTemplatePolicy(dto, entity, currentUsername);
        entity.setCreatedBy(currentUsername);
        templatePolicyMapper.insert(entity);
        return toTemplatePolicyVO(entity, companyNameMap());
    }

    /**
     * 更新模板Policy。
     */
    public ExpenseVoucherTemplatePolicyVO updateTemplatePolicy(Long id, ExpenseVoucherTemplatePolicySaveDTO dto, Long currentUserId, String currentUsername) {
        ExpVoucherTemplatePolicy entity = requireTemplatePolicy(id);
        validateTemplatePolicy(dto, id);
        applyTemplatePolicy(dto, entity, currentUsername);
        templatePolicyMapper.updateById(entity);
        return toTemplatePolicyVO(entity, companyNameMap());
    }

    /**
     * 创建科目映射。
     */
    public ExpenseVoucherSubjectMappingVO createSubjectMapping(ExpenseVoucherSubjectMappingSaveDTO dto, Long currentUserId, String currentUsername) {
        validateSubjectMapping(dto, null);
        ExpVoucherSubjectMapping entity = new ExpVoucherSubjectMapping();
        applySubjectMapping(dto, entity, currentUsername);
        entity.setCreatedBy(currentUsername);
        subjectMappingMapper.insert(entity);
        return toSubjectMappingVO(entity, companyNameMap());
    }

    /**
     * 更新科目映射。
     */
    public ExpenseVoucherSubjectMappingVO updateSubjectMapping(Long id, ExpenseVoucherSubjectMappingSaveDTO dto, Long currentUserId, String currentUsername) {
        ExpVoucherSubjectMapping entity = requireSubjectMapping(id);
        validateSubjectMapping(dto, id);
        applySubjectMapping(dto, entity, currentUsername);
        subjectMappingMapper.updateById(entity);
        return toSubjectMappingVO(entity, companyNameMap());
    }
}
