// 业务域：财务档案
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

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

/**
 * FinanceProjectArchiveServiceImpl：service 入口实现。
 * 接住上层请求，并把 财务项目档案相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
@Service
public class FinanceProjectArchiveServiceImpl implements FinanceProjectArchiveService {

    private final FinanceProjectArchiveMetaSupport financeProjectArchiveMetaSupport;
    private final FinanceProjectClassDomainSupport financeProjectClassDomainSupport;
    private final FinanceProjectQueryDomainSupport financeProjectQueryDomainSupport;
    private final FinanceProjectMutationDomainSupport financeProjectMutationDomainSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
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

    /**
     * 获取元数据。
     */
    @Override
    public FinanceProjectArchiveMetaVO getMeta(String companyId) {
        return financeProjectArchiveMetaSupport.getMeta(companyId);
    }

    /**
     * 查询项目Classes列表。
     */
    @Override
    public List<FinanceProjectClassSummaryVO> listProjectClasses(String companyId, String keyword, Integer status) {
        return financeProjectClassDomainSupport.listProjectClasses(companyId, keyword, status);
    }

    /**
     * 创建项目Class。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceProjectClassSummaryVO createProjectClass(String companyId, FinanceProjectClassSaveDTO dto, String operatorName) {
        return financeProjectClassDomainSupport.createProjectClass(companyId, dto, operatorName);
    }

    /**
     * 更新项目Class。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceProjectClassSummaryVO updateProjectClass(String companyId, String projectClassCode, FinanceProjectClassSaveDTO dto, String operatorName) {
        return financeProjectClassDomainSupport.updateProjectClass(companyId, projectClassCode, dto, operatorName);
    }

    /**
     * 更新项目ClassStatus。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateProjectClassStatus(String companyId, String projectClassCode, FinanceProjectStatusDTO dto, String operatorName) {
        return financeProjectClassDomainSupport.updateProjectClassStatus(companyId, projectClassCode, dto, operatorName);
    }

    /**
     * 查询项目列表。
     */
    @Override
    public List<FinanceProjectSummaryVO> listProjects(String companyId, String keyword, String projectClassCode, Integer status, Integer bclose) {
        return financeProjectQueryDomainSupport.listProjects(companyId, keyword, projectClassCode, status, bclose);
    }

    /**
     * 获取项目明细。
     */
    @Override
    public FinanceProjectDetailVO getProjectDetail(String companyId, String projectCode) {
        return financeProjectQueryDomainSupport.getProjectDetail(companyId, projectCode);
    }

    /**
     * 创建项目。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceProjectDetailVO createProject(String companyId, FinanceProjectSaveDTO dto, String operatorName) {
        return financeProjectMutationDomainSupport.createProject(companyId, dto, operatorName);
    }

    /**
     * 更新项目。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceProjectDetailVO updateProject(String companyId, String projectCode, FinanceProjectSaveDTO dto, String operatorName) {
        return financeProjectMutationDomainSupport.updateProject(companyId, projectCode, dto, operatorName);
    }

    /**
     * 更新项目Status。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateProjectStatus(String companyId, String projectCode, FinanceProjectStatusDTO dto, String operatorName) {
        return financeProjectMutationDomainSupport.updateProjectStatus(companyId, projectCode, dto, operatorName);
    }

    /**
     * 更新项目CloseStatus。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateProjectCloseStatus(String companyId, String projectCode, FinanceProjectCloseDTO dto, String operatorName) {
        return financeProjectMutationDomainSupport.updateProjectCloseStatus(companyId, projectCode, dto, operatorName);
    }
}
