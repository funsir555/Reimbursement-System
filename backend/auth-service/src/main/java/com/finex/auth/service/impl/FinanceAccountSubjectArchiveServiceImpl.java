package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSubjectCloseDTO;
import com.finex.auth.dto.FinanceAccountSubjectDetailVO;
import com.finex.auth.dto.FinanceAccountSubjectMetaVO;
import com.finex.auth.dto.FinanceAccountSubjectSaveDTO;
import com.finex.auth.dto.FinanceAccountSubjectStatusDTO;
import com.finex.auth.dto.FinanceAccountSubjectSummaryVO;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.service.FinanceAccountSubjectArchiveService;
import com.finex.auth.service.impl.financearchive.FinanceAccountSubjectMetaSupport;
import com.finex.auth.service.impl.financearchive.FinanceAccountSubjectMutationDomainSupport;
import com.finex.auth.service.impl.financearchive.FinanceAccountSubjectQueryDomainSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FinanceAccountSubjectArchiveServiceImpl implements FinanceAccountSubjectArchiveService {

    private final FinanceAccountSubjectMetaSupport financeAccountSubjectMetaSupport;
    private final FinanceAccountSubjectQueryDomainSupport financeAccountSubjectQueryDomainSupport;
    private final FinanceAccountSubjectMutationDomainSupport financeAccountSubjectMutationDomainSupport;

    public FinanceAccountSubjectArchiveServiceImpl(
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            SystemCompanyMapper systemCompanyMapper,
            GlAccvouchMapper glAccvouchMapper,
            ObjectMapper objectMapper
    ) {
        this.financeAccountSubjectMetaSupport = new FinanceAccountSubjectMetaSupport(
                financeAccountSubjectMapper,
                systemCompanyMapper,
                glAccvouchMapper,
                objectMapper
        );
        this.financeAccountSubjectQueryDomainSupport = new FinanceAccountSubjectQueryDomainSupport(
                financeAccountSubjectMapper,
                systemCompanyMapper,
                glAccvouchMapper,
                objectMapper
        );
        this.financeAccountSubjectMutationDomainSupport = new FinanceAccountSubjectMutationDomainSupport(
                financeAccountSubjectMapper,
                systemCompanyMapper,
                glAccvouchMapper,
                objectMapper
        );
    }

    @Override
    public FinanceAccountSubjectMetaVO getMeta() {
        return financeAccountSubjectMetaSupport.getMeta();
    }

    @Override
    public List<FinanceAccountSubjectSummaryVO> listSubjects(String companyId, String keyword, String subjectCategory, Integer status, Integer bclose) {
        return financeAccountSubjectQueryDomainSupport.listSubjects(companyId, keyword, subjectCategory, status, bclose);
    }

    @Override
    public FinanceAccountSubjectDetailVO getSubjectDetail(String companyId, String subjectCode) {
        return financeAccountSubjectQueryDomainSupport.getSubjectDetail(companyId, subjectCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceAccountSubjectDetailVO createSubject(String companyId, FinanceAccountSubjectSaveDTO dto, String operatorName) {
        return financeAccountSubjectMutationDomainSupport.createSubject(companyId, dto, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceAccountSubjectDetailVO updateSubject(String companyId, String subjectCode, FinanceAccountSubjectSaveDTO dto, String operatorName) {
        return financeAccountSubjectMutationDomainSupport.updateSubject(companyId, subjectCode, dto, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateStatus(String companyId, String subjectCode, FinanceAccountSubjectStatusDTO dto, String operatorName) {
        return financeAccountSubjectMutationDomainSupport.updateStatus(companyId, subjectCode, dto, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCloseStatus(String companyId, String subjectCode, FinanceAccountSubjectCloseDTO dto, String operatorName) {
        return financeAccountSubjectMutationDomainSupport.updateCloseStatus(companyId, subjectCode, dto, operatorName);
    }
}
