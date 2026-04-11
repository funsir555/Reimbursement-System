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

public class FinanceAccountSubjectQueryDomainSupport extends AbstractFinanceAccountSubjectArchiveSupport {

    public FinanceAccountSubjectQueryDomainSupport(
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            SystemCompanyMapper systemCompanyMapper,
            GlAccvouchMapper glAccvouchMapper,
            ObjectMapper objectMapper
    ) {
        super(financeAccountSubjectMapper, systemCompanyMapper, glAccvouchMapper, objectMapper);
    }

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

    public FinanceAccountSubjectDetailVO getSubjectDetail(String companyId, String subjectCode) {
        return toDetail(requireSubject(companyId, subjectCode));
    }
}
