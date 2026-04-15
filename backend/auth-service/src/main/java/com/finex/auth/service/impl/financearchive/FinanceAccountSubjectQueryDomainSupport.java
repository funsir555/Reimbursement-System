// 业务域：财务档案
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

package com.finex.auth.service.impl.financearchive;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSubjectDetailVO;
import com.finex.auth.dto.FinanceAccountSubjectSummaryVO;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;

import java.util.List;

/**
 * FinanceAccountSubjectQueryDomainSupport：领域规则支撑类。
 * 承接 财务账户科目的核心业务规则。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
public class FinanceAccountSubjectQueryDomainSupport extends AbstractFinanceAccountSubjectArchiveSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FinanceAccountSubjectQueryDomainSupport(
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            SystemCompanyMapper systemCompanyMapper,
            GlAccvouchMapper glAccvouchMapper,
            ObjectMapper objectMapper
    ) {
        super(financeAccountSubjectMapper, systemCompanyMapper, glAccvouchMapper, objectMapper);
    }

    /**
     * 查询科目列表。
     */
    public List<FinanceAccountSubjectSummaryVO> listSubjects(String companyId, String keyword, String subjectCategory, Integer status, Integer bclose) {
        QueryWrapper<FinanceAccountSubject> query = new QueryWrapper<>();
        query.eq("company_id", requireCompanyId(companyId));
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword != null) {
            query.and(wrapper -> wrapper
                    .like("subject_code", normalizedKeyword)
                    .or()
                    .like("subject_name", normalizedKeyword)
                    .or()
                    .like("chelp", normalizedKeyword));
        }
        String normalizedCategory = trimToNull(subjectCategory);
        if (normalizedCategory != null) {
            query.eq("subject_category", normalizedCategory);
        }
        if (status != null) {
            query.eq("status", normalizeFlag(status, 1));
        }
        if (bclose != null) {
            query.eq("bclose", normalizeFlag(bclose, 0));
        }
        query.orderByAsc("subject_level", "sort_order", "subject_code", "id");
        return buildTree(financeAccountSubjectMapper.selectList(query));
    }

    /**
     * 获取科目明细。
     */
    public FinanceAccountSubjectDetailVO getSubjectDetail(String companyId, String subjectCode) {
        return toDetail(requireSubject(companyId, subjectCode));
    }
}
