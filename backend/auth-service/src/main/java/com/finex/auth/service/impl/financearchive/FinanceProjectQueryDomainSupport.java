// 业务域：财务档案
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

package com.finex.auth.service.impl.financearchive;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.finex.auth.dto.FinanceProjectDetailVO;
import com.finex.auth.dto.FinanceProjectSummaryVO;
import com.finex.auth.entity.FinanceProjectArchive;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;

import java.util.List;
import java.util.Map;

/**
 * FinanceProjectQueryDomainSupport：领域规则支撑类。
 * 承接 财务项目的核心业务规则。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
public class FinanceProjectQueryDomainSupport extends AbstractFinanceProjectArchiveSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FinanceProjectQueryDomainSupport(
            FinanceProjectClassMapper financeProjectClassMapper,
            FinanceProjectArchiveMapper financeProjectArchiveMapper,
            SystemCompanyMapper systemCompanyMapper,
            GlAccvouchMapper glAccvouchMapper
    ) {
        super(financeProjectClassMapper, financeProjectArchiveMapper, systemCompanyMapper, glAccvouchMapper);
    }

    /**
     * 查询项目列表。
     */
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

    /**
     * 获取项目明细。
     */
    public FinanceProjectDetailVO getProjectDetail(String companyId, String projectCode) {
        FinanceProjectArchive project = requireProject(companyId, projectCode);
        return toProjectDetail(project, requireProjectClass(project.getCompanyId(), project.getCitemccode()).getProjectClassName());
    }
}
