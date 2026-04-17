// 业务域：财务档案
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSubjectCloseDTO;
import com.finex.auth.dto.FinanceAccountSubjectDerivedDefaultsVO;
import com.finex.auth.dto.FinanceAccountSubjectDetailVO;
import com.finex.auth.dto.FinanceAccountSubjectMetaVO;
import com.finex.auth.dto.FinanceAccountSubjectSaveDTO;
import com.finex.auth.dto.FinanceAccountSubjectStatusDTO;
import com.finex.auth.dto.FinanceAccountSubjectSummaryVO;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinanceAccountSetTemplateSubjectMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.service.FinanceAccountSubjectArchiveService;
import com.finex.auth.service.impl.financearchive.FinanceAccountSubjectMetaSupport;
import com.finex.auth.service.impl.financearchive.FinanceAccountSubjectMutationDomainSupport;
import com.finex.auth.service.impl.financearchive.FinanceAccountSubjectQueryDomainSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * FinanceAccountSubjectArchiveServiceImpl：service 入口实现。
 * 接住上层请求，并把 财务账户科目档案相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
@Service
public class FinanceAccountSubjectArchiveServiceImpl implements FinanceAccountSubjectArchiveService {

    private final FinanceAccountSubjectMetaSupport financeAccountSubjectMetaSupport;
    private final FinanceAccountSubjectQueryDomainSupport financeAccountSubjectQueryDomainSupport;
    private final FinanceAccountSubjectMutationDomainSupport financeAccountSubjectMutationDomainSupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FinanceAccountSubjectArchiveServiceImpl(
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            SystemCompanyMapper systemCompanyMapper,
            GlAccvouchMapper glAccvouchMapper,
            ObjectMapper objectMapper,
            FinanceAccountSetMapper financeAccountSetMapper,
            FinanceAccountSetTemplateSubjectMapper financeAccountSetTemplateSubjectMapper
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
                objectMapper,
                financeAccountSetMapper,
                financeAccountSetTemplateSubjectMapper
        );
    }

    /**
     * 获取元数据。
     */
    @Override
    public FinanceAccountSubjectMetaVO getMeta() {
        return financeAccountSubjectMetaSupport.getMeta();
    }

    /**
     * 查询科目列表。
     */
    @Override
    public List<FinanceAccountSubjectSummaryVO> listSubjects(String companyId, String keyword, String subjectCategory, Integer status, Integer bclose) {
        return financeAccountSubjectQueryDomainSupport.listSubjects(companyId, keyword, subjectCategory, status, bclose);
    }

    /**
     * 获取科目明细。
     */
    @Override
    public FinanceAccountSubjectDetailVO getSubjectDetail(String companyId, String subjectCode) {
        return financeAccountSubjectQueryDomainSupport.getSubjectDetail(companyId, subjectCode);
    }

    @Override
    public FinanceAccountSubjectDerivedDefaultsVO getDerivedDefaults(String companyId, String subjectCode) {
        return financeAccountSubjectMutationDomainSupport.getDerivedDefaults(companyId, subjectCode);
    }

    /**
     * 创建科目。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceAccountSubjectDetailVO createSubject(String companyId, FinanceAccountSubjectSaveDTO dto, String operatorName) {
        return financeAccountSubjectMutationDomainSupport.createSubject(companyId, dto, operatorName);
    }

    /**
     * 更新科目。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceAccountSubjectDetailVO updateSubject(String companyId, String subjectCode, FinanceAccountSubjectSaveDTO dto, String operatorName) {
        return financeAccountSubjectMutationDomainSupport.updateSubject(companyId, subjectCode, dto, operatorName);
    }

    /**
     * 更新Status。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateStatus(String companyId, String subjectCode, FinanceAccountSubjectStatusDTO dto, String operatorName) {
        return financeAccountSubjectMutationDomainSupport.updateStatus(companyId, subjectCode, dto, operatorName);
    }

    /**
     * 更新CloseStatus。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCloseStatus(String companyId, String subjectCode, FinanceAccountSubjectCloseDTO dto, String operatorName) {
        return financeAccountSubjectMutationDomainSupport.updateCloseStatus(companyId, subjectCode, dto, operatorName);
    }
}
