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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseVoucherGenerationServiceImpl implements ExpenseVoucherGenerationService {

    private final ExpenseVoucherMetaSupport expenseVoucherMetaSupport;
    private final ExpenseVoucherMappingDomainSupport expenseVoucherMappingDomainSupport;
    private final ExpenseVoucherPushDomainSupport expenseVoucherPushDomainSupport;
    private final ExpenseVoucherRecordQuerySupport expenseVoucherRecordQuerySupport;

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

    @Override
    public ExpenseVoucherGenerationMetaVO getMeta(Long currentUserId) {
        return expenseVoucherMetaSupport.getMeta(currentUserId);
    }

    @Override
    public ExpenseVoucherPageVO<ExpenseVoucherTemplatePolicyVO> getTemplatePolicies(String companyId, String templateCode, Integer enabled, Integer page, Integer pageSize) {
        return expenseVoucherMappingDomainSupport.getTemplatePolicies(companyId, templateCode, enabled, page, pageSize);
    }

    @Override
    public ExpenseVoucherPageVO<ExpenseVoucherSubjectMappingVO> getSubjectMappings(String companyId, String templateCode, String expenseTypeCode, Integer enabled, Integer page, Integer pageSize) {
        return expenseVoucherMappingDomainSupport.getSubjectMappings(companyId, templateCode, expenseTypeCode, enabled, page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseVoucherTemplatePolicyVO createTemplatePolicy(ExpenseVoucherTemplatePolicySaveDTO dto, Long currentUserId, String currentUsername) {
        return expenseVoucherMappingDomainSupport.createTemplatePolicy(dto, currentUserId, currentUsername);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseVoucherTemplatePolicyVO updateTemplatePolicy(Long id, ExpenseVoucherTemplatePolicySaveDTO dto, Long currentUserId, String currentUsername) {
        return expenseVoucherMappingDomainSupport.updateTemplatePolicy(id, dto, currentUserId, currentUsername);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseVoucherSubjectMappingVO createSubjectMapping(ExpenseVoucherSubjectMappingSaveDTO dto, Long currentUserId, String currentUsername) {
        return expenseVoucherMappingDomainSupport.createSubjectMapping(dto, currentUserId, currentUsername);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseVoucherSubjectMappingVO updateSubjectMapping(Long id, ExpenseVoucherSubjectMappingSaveDTO dto, Long currentUserId, String currentUsername) {
        return expenseVoucherMappingDomainSupport.updateSubjectMapping(id, dto, currentUserId, currentUsername);
    }

    @Override
    public ExpenseVoucherPageVO<ExpenseVoucherPushDocumentVO> getPushDocuments(String companyId, String templateCode, String keyword, String pushStatus, String dateFrom, String dateTo, Integer page, Integer pageSize) {
        return expenseVoucherPushDomainSupport.getPushDocuments(companyId, templateCode, keyword, pushStatus, dateFrom, dateTo, page, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExpenseVoucherPushBatchResultVO pushDocuments(ExpenseVoucherPushDTO dto, Long currentUserId, String currentUsername) {
        return expenseVoucherPushDomainSupport.pushDocuments(dto, currentUserId, currentUsername);
    }

    @Override
    public ExpenseVoucherPageVO<ExpenseVoucherGeneratedRecordVO> getGeneratedVouchers(String companyId, String templateCode, String documentCode, String voucherNo, String pushStatus, String dateFrom, String dateTo, Integer page, Integer pageSize) {
        return expenseVoucherRecordQuerySupport.getGeneratedVouchers(companyId, templateCode, documentCode, voucherNo, pushStatus, dateFrom, dateTo, page, pageSize);
    }

    @Override
    public ExpenseVoucherGeneratedRecordDetailVO getGeneratedVoucherDetail(Long id) {
        return expenseVoucherRecordQuerySupport.getGeneratedVoucherDetail(id);
    }
}
