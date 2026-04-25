package com.finex.auth.service.impl.expense;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessCustomArchiveItemMapper;
import com.finex.auth.mapper.ProcessDocumentActionLogMapper;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessTemplateScopeMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;

final class ExpenseSummarySupportContext {

    final ProcessDocumentActionLogMapper processDocumentActionLogMapper;
    final ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    final ProcessDocumentTemplateMapper templateMapper;
    final ProcessTemplateScopeMapper processTemplateScopeMapper;
    final ProcessCustomArchiveDesignMapper customArchiveDesignMapper;
    final ProcessCustomArchiveItemMapper customArchiveItemMapper;
    final UserMapper userMapper;
    final SystemCompanyMapper systemCompanyMapper;
    final FinanceVendorMapper financeVendorMapper;
    final SystemDepartmentMapper systemDepartmentMapper;
    final ObjectMapper objectMapper;

    ExpenseSummarySupportContext(
            ProcessDocumentActionLogMapper processDocumentActionLogMapper,
            ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper,
            ProcessDocumentTemplateMapper templateMapper,
            ProcessTemplateScopeMapper processTemplateScopeMapper,
            ProcessCustomArchiveDesignMapper customArchiveDesignMapper,
            ProcessCustomArchiveItemMapper customArchiveItemMapper,
            UserMapper userMapper,
            SystemCompanyMapper systemCompanyMapper,
            FinanceVendorMapper financeVendorMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            ObjectMapper objectMapper
    ) {
        this.processDocumentActionLogMapper = processDocumentActionLogMapper;
        this.processDocumentExpenseDetailMapper = processDocumentExpenseDetailMapper;
        this.templateMapper = templateMapper;
        this.processTemplateScopeMapper = processTemplateScopeMapper;
        this.customArchiveDesignMapper = customArchiveDesignMapper;
        this.customArchiveItemMapper = customArchiveItemMapper;
        this.userMapper = userMapper;
        this.systemCompanyMapper = systemCompanyMapper;
        this.financeVendorMapper = financeVendorMapper;
        this.systemDepartmentMapper = systemDepartmentMapper;
        this.objectMapper = objectMapper;
    }
}