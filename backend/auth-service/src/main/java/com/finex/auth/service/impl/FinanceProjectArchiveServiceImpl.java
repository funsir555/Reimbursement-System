package com.finex.auth.service.impl;

import com.finex.auth.dto.FinanceProjectArchiveMetaVO;
import com.finex.auth.dto.FinanceProjectClassSaveDTO;
import com.finex.auth.dto.FinanceProjectClassSummaryVO;
import com.finex.auth.dto.FinanceProjectCloseDTO;
import com.finex.auth.dto.FinanceProjectDetailVO;
import com.finex.auth.dto.FinanceProjectSaveDTO;
import com.finex.auth.dto.FinanceProjectStatusDTO;
import com.finex.auth.dto.FinanceProjectSummaryVO;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.service.FinanceProjectArchiveService;
import com.finex.auth.service.impl.financearchive.FinanceProjectArchiveMetaSupport;
import com.finex.auth.service.impl.financearchive.FinanceProjectClassDomainSupport;
import com.finex.auth.service.impl.financearchive.FinanceProjectMutationDomainSupport;
import com.finex.auth.service.impl.financearchive.FinanceProjectQueryDomainSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FinanceProjectArchiveServiceImpl implements FinanceProjectArchiveService {

    private final FinanceProjectArchiveMetaSupport financeProjectArchiveMetaSupport;
    private final FinanceProjectClassDomainSupport financeProjectClassDomainSupport;
    private final FinanceProjectQueryDomainSupport financeProjectQueryDomainSupport;
    private final FinanceProjectMutationDomainSupport financeProjectMutationDomainSupport;

    public FinanceProjectArchiveServiceImpl(
            FinanceProjectClassMapper financeProjectClassMapper,
            FinanceProjectArchiveMapper financeProjectArchiveMapper,
            SystemCompanyMapper systemCompanyMapper,
            GlAccvouchMapper glAccvouchMapper
    ) {
        this.financeProjectArchiveMetaSupport = new FinanceProjectArchiveMetaSupport(
                financeProjectClassMapper,
                financeProjectArchiveMapper,
                systemCompanyMapper,
                glAccvouchMapper
        );
        this.financeProjectClassDomainSupport = new FinanceProjectClassDomainSupport(
                financeProjectClassMapper,
                financeProjectArchiveMapper,
                systemCompanyMapper,
                glAccvouchMapper
        );
        this.financeProjectQueryDomainSupport = new FinanceProjectQueryDomainSupport(
                financeProjectClassMapper,
                financeProjectArchiveMapper,
                systemCompanyMapper,
                glAccvouchMapper
        );
        this.financeProjectMutationDomainSupport = new FinanceProjectMutationDomainSupport(
                financeProjectClassMapper,
                financeProjectArchiveMapper,
                systemCompanyMapper,
                glAccvouchMapper
        );
    }

    @Override
    public FinanceProjectArchiveMetaVO getMeta(String companyId) {
        return financeProjectArchiveMetaSupport.getMeta(companyId);
    }

    @Override
    public List<FinanceProjectClassSummaryVO> listProjectClasses(String companyId, String keyword, Integer status) {
        return financeProjectClassDomainSupport.listProjectClasses(companyId, keyword, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceProjectClassSummaryVO createProjectClass(String companyId, FinanceProjectClassSaveDTO dto, String operatorName) {
        return financeProjectClassDomainSupport.createProjectClass(companyId, dto, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceProjectClassSummaryVO updateProjectClass(String companyId, String projectClassCode, FinanceProjectClassSaveDTO dto, String operatorName) {
        return financeProjectClassDomainSupport.updateProjectClass(companyId, projectClassCode, dto, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateProjectClassStatus(String companyId, String projectClassCode, FinanceProjectStatusDTO dto, String operatorName) {
        return financeProjectClassDomainSupport.updateProjectClassStatus(companyId, projectClassCode, dto, operatorName);
    }

    @Override
    public List<FinanceProjectSummaryVO> listProjects(String companyId, String keyword, String projectClassCode, Integer status, Integer bclose) {
        return financeProjectQueryDomainSupport.listProjects(companyId, keyword, projectClassCode, status, bclose);
    }

    @Override
    public FinanceProjectDetailVO getProjectDetail(String companyId, String projectCode) {
        return financeProjectQueryDomainSupport.getProjectDetail(companyId, projectCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceProjectDetailVO createProject(String companyId, FinanceProjectSaveDTO dto, String operatorName) {
        return financeProjectMutationDomainSupport.createProject(companyId, dto, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceProjectDetailVO updateProject(String companyId, String projectCode, FinanceProjectSaveDTO dto, String operatorName) {
        return financeProjectMutationDomainSupport.updateProject(companyId, projectCode, dto, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateProjectStatus(String companyId, String projectCode, FinanceProjectStatusDTO dto, String operatorName) {
        return financeProjectMutationDomainSupport.updateProjectStatus(companyId, projectCode, dto, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateProjectCloseStatus(String companyId, String projectCode, FinanceProjectCloseDTO dto, String operatorName) {
        return financeProjectMutationDomainSupport.updateProjectCloseStatus(companyId, projectCode, dto, operatorName);
    }
}