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

public class FinanceProjectQueryDomainSupport extends AbstractFinanceProjectArchiveSupport {

    public FinanceProjectQueryDomainSupport(
            FinanceProjectClassMapper financeProjectClassMapper,
            FinanceProjectArchiveMapper financeProjectArchiveMapper,
            SystemCompanyMapper systemCompanyMapper,
            GlAccvouchMapper glAccvouchMapper
    ) {
        super(financeProjectClassMapper, financeProjectArchiveMapper, systemCompanyMapper, glAccvouchMapper);
    }

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

    public FinanceProjectDetailVO getProjectDetail(String companyId, String projectCode) {
        FinanceProjectArchive project = requireProject(companyId, projectCode);
        return toProjectDetail(project, requireProjectClass(project.getCompanyId(), project.getCitemccode()).getProjectClassName());
    }
}