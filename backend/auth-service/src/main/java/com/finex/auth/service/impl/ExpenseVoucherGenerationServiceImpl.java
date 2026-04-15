// 业务域：报销凭证生成与推送
// 文件角色：service 入口实现
// 上下游关系：上游通常来自 报销单凭证生成接口和财务操作入口，下游会继续协调 凭证映射、推送记录和报销单凭证状态。
// 风险提醒：改坏后最容易影响 重复生成凭证、凭证内容错误和推送记录不一致。

package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseVoucherGeneratedRecordDetailVO;
import com.finex.auth.dto.ExpenseVoucherGeneratedRecordVO;
import com.finex.auth.dto.ExpenseVoucherGenerationMetaVO;
import com.finex.auth.dto.ExpenseVoucherPageVO;
import com.finex.auth.dto.ExpenseVoucherPushBatchResultVO;
import com.finex.auth.dto.ExpenseVoucherPushDTO;
import com.finex.auth.dto.ExpenseVoucherPushDocumentVO;
import com.finex.auth.dto.ExpenseVoucherSubjectMappingSaveDTO;
import com.finex.auth.dto.ExpenseVoucherSubjectMappingVO;
import com.finex.auth.dto.ExpenseVoucherTemplatePolicySaveDTO;
import com.finex.auth.dto.ExpenseVoucherTemplatePolicyVO;
import com.finex.auth.mapper.ExpVoucherPushBatchMapper;
import com.finex.auth.mapper.ExpVoucherPushDocumentMapper;
import com.finex.auth.mapper.ExpVoucherPushEntryMapper;
import com.finex.auth.mapper.ExpVoucherSubjectMappingMapper;
import com.finex.auth.mapper.ExpVoucherTemplatePolicyMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.ExpenseVoucherGenerationService;
import com.finex.auth.service.FinanceVoucherService;
import com.finex.auth.service.impl.expensevoucher.AbstractExpenseVoucherGenerationSupport;
import com.finex.auth.service.impl.expensevoucher.ExpenseVoucherMappingDomainSupport;
import com.finex.auth.service.impl.expensevoucher.ExpenseVoucherMetaSupport;
import com.finex.auth.service.impl.expensevoucher.ExpenseVoucherPushDomainSupport;
import com.finex.auth.service.impl.expensevoucher.ExpenseVoucherRecordQuerySupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ExpenseVoucherGenerationServiceImpl：service 入口实现。
 * 接住上层请求，并把 报销单凭证Generation相关流程分发到更细的规则组件。
 * 改这里时，要特别关注 重复生成凭证、凭证内容错误和推送记录不一致是否会被一起带坏。
 */
@Service
public class ExpenseVoucherGenerationServiceImpl implements ExpenseVoucherGenerationService {

    private final ExpenseVoucherMetaSupport expenseVoucherMetaSupport;
    private final ExpenseVoucherMappingDomainSupport expenseVoucherMappingDomainSupport;
    private final ExpenseVoucherPushDomainSupport expenseVoucherPushDomainSupport;
    private final ExpenseVoucherRecordQuerySupport expenseVoucherRecordQuerySupport;

    /**
     * 初始化这个类所需的依赖组件。
     */
    @Autowired
    public ExpenseVoucherGenerationServiceImpl(
            ExpVoucherTemplatePolicyMapper templatePolicyMapper,
            ExpVoucherSubjectMappingMapper subjectMappingMapper,
            ExpVoucherPushBatchMapper pushBatchMapper,
            ExpVoucherPushDocumentMapper pushDocumentMapper,
            ExpVoucherPushEntryMapper pushEntryMapper,
            ProcessDocumentInstanceMapper documentInstanceMapper,
            ProcessDocumentExpenseDetailMapper expenseDetailMapper,
            ProcessDocumentTemplateMapper documentTemplateMapper,
            ProcessExpenseTypeMapper expenseTypeMapper,
            SystemCompanyMapper systemCompanyMapper,
            UserMapper userMapper,
            GlAccvouchMapper glAccvouchMapper,
            FinanceVoucherService financeVoucherService,
            ObjectMapper objectMapper
    ) {
        AbstractExpenseVoucherGenerationSupport.Dependencies dependencies = AbstractExpenseVoucherGenerationSupport.dependencies(
                templatePolicyMapper,
                subjectMappingMapper,
                pushBatchMapper,
                pushDocumentMapper,
                pushEntryMapper,
                documentInstanceMapper,
                expenseDetailMapper,
                documentTemplateMapper,
                expenseTypeMapper,
                systemCompanyMapper,
                userMapper,
                glAccvouchMapper,
                financeVoucherService,
                objectMapper
        );
        this.expenseVoucherMetaSupport = new ExpenseVoucherMetaSupport(dependencies);
        this.expenseVoucherMappingDomainSupport = new ExpenseVoucherMappingDomainSupport(dependencies);
        this.expenseVoucherPushDomainSupport = new ExpenseVoucherPushDomainSupport(dependencies);
        this.expenseVoucherRecordQuerySupport = new ExpenseVoucherRecordQuerySupport(dependencies);
    }

    /**
     * 初始化这个类所需的依赖组件。
     */
    ExpenseVoucherGenerationServiceImpl(
            ExpenseVoucherMetaSupport expenseVoucherMetaSupport,
            ExpenseVoucherMappingDomainSupport expenseVoucherMappingDomainSupport,
            ExpenseVoucherPushDomainSupport expenseVoucherPushDomainSupport,
            ExpenseVoucherRecordQuerySupport expenseVoucherRecordQuerySupport
    ) {
        this.expenseVoucherMetaSupport = expenseVoucherMetaSupport;
        this.expenseVoucherMappingDomainSupport = expenseVoucherMappingDomainSupport;
        this.expenseVoucherPushDomainSupport = expenseVoucherPushDomainSupport;
        this.expenseVoucherRecordQuerySupport = expenseVoucherRecordQuerySupport;
    }

    /**
     * 获取元数据。
     */
    @Override
    public ExpenseVoucherGenerationMetaVO getMeta(Long currentUserId) {
        return expenseVoucherMetaSupport.getMeta(currentUserId);
    }

    /**
     * 获取模板Policies。
     */
    @Override
    public ExpenseVoucherPageVO<ExpenseVoucherTemplatePolicyVO> getTemplatePolicies(String companyId, String templateCode, Integer enabled, Integer page, Integer pageSize) {
        return expenseVoucherMappingDomainSupport.getTemplatePolicies(companyId, templateCode, enabled, page, pageSize);
    }

    /**
     * 获取科目映射。
     */
    @Override
    public ExpenseVoucherPageVO<ExpenseVoucherSubjectMappingVO> getSubjectMappings(String companyId, String templateCode, String expenseTypeCode, Integer enabled, Integer page, Integer pageSize) {
        return expenseVoucherMappingDomainSupport.getSubjectMappings(companyId, templateCode, expenseTypeCode, enabled, page, pageSize);
    }

    /**
     * 创建模板Policy。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseVoucherTemplatePolicyVO createTemplatePolicy(ExpenseVoucherTemplatePolicySaveDTO dto, Long currentUserId, String currentUsername) {
        return expenseVoucherMappingDomainSupport.createTemplatePolicy(dto, currentUserId, currentUsername);
    }

    /**
     * 更新模板Policy。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseVoucherTemplatePolicyVO updateTemplatePolicy(Long id, ExpenseVoucherTemplatePolicySaveDTO dto, Long currentUserId, String currentUsername) {
        return expenseVoucherMappingDomainSupport.updateTemplatePolicy(id, dto, currentUserId, currentUsername);
    }

    /**
     * 创建科目映射。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseVoucherSubjectMappingVO createSubjectMapping(ExpenseVoucherSubjectMappingSaveDTO dto, Long currentUserId, String currentUsername) {
        return expenseVoucherMappingDomainSupport.createSubjectMapping(dto, currentUserId, currentUsername);
    }

    /**
     * 更新科目映射。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseVoucherSubjectMappingVO updateSubjectMapping(Long id, ExpenseVoucherSubjectMappingSaveDTO dto, Long currentUserId, String currentUsername) {
        return expenseVoucherMappingDomainSupport.updateSubjectMapping(id, dto, currentUserId, currentUsername);
    }

    /**
     * 获取推送单据。
     */
    @Override
    public ExpenseVoucherPageVO<ExpenseVoucherPushDocumentVO> getPushDocuments(String companyId, String templateCode, String keyword, String pushStatus, String dateFrom, String dateTo, Integer page, Integer pageSize) {
        return expenseVoucherPushDomainSupport.getPushDocuments(companyId, templateCode, keyword, pushStatus, dateFrom, dateTo, page, pageSize);
    }

    /**
     * 推送单据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseVoucherPushBatchResultVO pushDocuments(ExpenseVoucherPushDTO dto, Long currentUserId, String currentUsername) {
        return expenseVoucherPushDomainSupport.pushDocuments(dto, currentUserId, currentUsername);
    }

    /**
     * 获取Generated凭证。
     */
    @Override
    public ExpenseVoucherPageVO<ExpenseVoucherGeneratedRecordVO> getGeneratedVouchers(String companyId, String templateCode, String documentCode, String voucherNo, String pushStatus, String dateFrom, String dateTo, Integer page, Integer pageSize) {
        return expenseVoucherRecordQuerySupport.getGeneratedVouchers(companyId, templateCode, documentCode, voucherNo, pushStatus, dateFrom, dateTo, page, pageSize);
    }

    /**
     * 获取Generated凭证明细。
     */
    @Override
    public ExpenseVoucherGeneratedRecordDetailVO getGeneratedVoucherDetail(Long id) {
        return expenseVoucherRecordQuerySupport.getGeneratedVoucherDetail(id);
    }
}
