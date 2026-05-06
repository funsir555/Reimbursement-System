package com.finex.auth.service.impl.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessExpenseTypeDetailVO;
import com.finex.auth.dto.ProcessExpenseTypeMetaVO;
import com.finex.auth.dto.ProcessExpenseTypeSaveDTO;
import com.finex.auth.dto.ProcessExpenseTypeTreeVO;
import com.finex.auth.mapper.CodeSequenceMapper;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessCustomArchiveItemMapper;
import com.finex.auth.mapper.ProcessCustomArchiveRuleMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessTemplateCategoryMapper;
import com.finex.auth.mapper.ProcessTemplateScopeMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.ProcessExpenseDetailDesignService;
import com.finex.auth.service.ProcessFlowDesignService;
import com.finex.auth.service.ProcessFormDesignService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public final class ProcessExpenseTypeLifecycleSupport extends AbstractProcessExpenseTypeSupport {

    public ProcessExpenseTypeLifecycleSupport(
            ProcessTemplateCategoryMapper categoryMapper,
            ProcessDocumentTemplateMapper templateMapper,
            CodeSequenceMapper codeSequenceMapper,
            ProcessTemplateScopeMapper scopeMapper,
            ProcessCustomArchiveDesignMapper customArchiveDesignMapper,
            ProcessCustomArchiveItemMapper customArchiveItemMapper,
            ProcessCustomArchiveRuleMapper customArchiveRuleMapper,
            ProcessExpenseTypeMapper processExpenseTypeMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper,
            ProcessFormDesignService processFormDesignService,
            ProcessExpenseDetailDesignService processExpenseDetailDesignService,
            ProcessFlowDesignService processFlowDesignService,
            ObjectMapper objectMapper
    ) {
        super(categoryMapper, templateMapper, codeSequenceMapper, scopeMapper, customArchiveDesignMapper, customArchiveItemMapper, customArchiveRuleMapper, processExpenseTypeMapper, systemDepartmentMapper, userMapper, processFormDesignService, processExpenseDetailDesignService, processFlowDesignService, objectMapper);
    }

    public List<ProcessExpenseTypeTreeVO> listExpenseTypeTree() {
        return buildExpenseTypeTree(loadAllExpenseTypes());
    }

    public ProcessExpenseTypeMetaVO getExpenseTypeMeta() {
        ProcessExpenseTypeMetaVO meta = new ProcessExpenseTypeMetaVO();
        meta.setDepartmentOptions(loadDepartmentOptions());
        meta.setUserOptions(loadUserOptions());
        meta.setEmployeeDirectory(loadEmployeeDirectory());
        meta.setInvoiceFreeOptions(List.of(
                configOption(EXPENSE_TYPE_INVOICE_FREE, "免票", "默认无需上传发票，且费用自动标记为免票"),
                configOption(EXPENSE_TYPE_INVOICE_REQUIRED, "不免票", "根据费用表单中发票组件的必填性进行判断")
        ));
        meta.setTaxDeductionOptions(List.of(
                configOption(EXPENSE_TYPE_TAX_DEFAULT, "遵循默认抵扣和转出逻辑", "沿用系统默认的抵扣与转出处理逻辑"),
                configOption(EXPENSE_TYPE_TAX_SPECIAL_NO_DEDUCT_NEED_OUT, "上传专票时默认不抵扣且需要转出，其他发票遵循默认逻辑", "适用于福利费开专票等场景"),
                configOption(EXPENSE_TYPE_TAX_SPECIAL_NO_DEDUCT_NEED_OUT_OTHERS_NONE, "上传专票时默认不抵扣且需要转出，其他发票不抵扣不转出", "适用于报销福利费等场景"),
                configOption(EXPENSE_TYPE_TAX_ALL_NO_DEDUCT_NO_OUT, "无论上传任何票种，默认不抵扣且无需转出", "适用于客户机票等场景"),
                configOption(EXPENSE_TYPE_TAX_HAS_DEDUCT_NO_DEDUCT_NEED_OUT, "上传有抵扣税额的发票时，默认不抵扣且需要转出", "适用于客户飞机火车福利费等场景")
        ));
        meta.setTaxSeparationOptions(List.of(
                configOption(EXPENSE_TYPE_TAX_SEPARATE, "价税分离", "费用金额与税额分开处理"),
                configOption(EXPENSE_TYPE_TAX_NOT_SEPARATE, "价税不分离", "费用金额与税额合并处理")
        ));
        return meta;
    }

    public ProcessExpenseTypeDetailVO getExpenseTypeDetail(Long id) {
        return buildExpenseTypeDetail(requireExpenseType(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessExpenseTypeDetailVO createExpenseType(ProcessExpenseTypeSaveDTO dto) {
        validateExpenseType(dto, null);

        com.finex.auth.entity.ProcessExpenseType expenseType = new com.finex.auth.entity.ProcessExpenseType();
        applyExpenseTypeBase(expenseType, dto);
        getProcessExpenseTypeMapper().insert(expenseType);
        return buildExpenseTypeDetail(requireExpenseType(expenseType.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessExpenseTypeDetailVO updateExpenseType(Long id, ProcessExpenseTypeSaveDTO dto) {
        com.finex.auth.entity.ProcessExpenseType expenseType = requireExpenseType(id);
        validateExpenseType(dto, expenseType);

        Integer targetStatus = normalizeStatus(dto.getStatus());
        applyExpenseTypeBase(expenseType, dto);
        getProcessExpenseTypeMapper().updateById(expenseType);
        if (targetStatus == 0) {
            disableExpenseTypeChildren(id);
        }
        return buildExpenseTypeDetail(requireExpenseType(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean updateExpenseTypeStatus(Long id, Integer status) {
        com.finex.auth.entity.ProcessExpenseType expenseType = requireExpenseType(id);
        Integer normalizedStatus = normalizeStatus(status);
        validateExpenseTypeStatus(expenseType, normalizedStatus);

        expenseType.setStatus(normalizedStatus);
        getProcessExpenseTypeMapper().updateById(expenseType);
        if (normalizedStatus == 0) {
            disableExpenseTypeChildren(id);
        }
        return Boolean.TRUE;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteExpenseType(Long id) {
        com.finex.auth.entity.ProcessExpenseType expenseType = requireExpenseType(id);
        if (hasExpenseTypeChildren(id)) {
            throw new IllegalStateException("当前费用类型下存在子级节点，不能删除");
        }
        if (isExpenseTypeReferenced(expenseType)) {
            throw new IllegalStateException("当前费用类型已被模板引用，不能删除");
        }
        getProcessExpenseTypeMapper().deleteById(id);
        return Boolean.TRUE;
    }
}
