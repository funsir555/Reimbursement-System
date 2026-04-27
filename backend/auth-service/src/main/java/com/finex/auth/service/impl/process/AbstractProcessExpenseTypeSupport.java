package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessExpenseTypeConfigOptionVO;
import com.finex.auth.dto.ProcessExpenseTypeDetailVO;
import com.finex.auth.dto.ProcessExpenseTypeSaveDTO;
import com.finex.auth.dto.ProcessExpenseTypeTreeVO;
import com.finex.auth.entity.ProcessExpenseType;
import com.finex.auth.entity.ProcessTemplateScope;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

abstract class AbstractProcessExpenseTypeSupport extends AbstractProcessManagementSupport {

    protected AbstractProcessExpenseTypeSupport(
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
        super(
                categoryMapper,
                templateMapper,
                codeSequenceMapper,
                scopeMapper,
                customArchiveDesignMapper,
                customArchiveItemMapper,
                customArchiveRuleMapper,
                processExpenseTypeMapper,
                systemDepartmentMapper,
                userMapper,
                processFormDesignService,
                processExpenseDetailDesignService,
                processFlowDesignService,
                objectMapper
        );
    }

    protected List<ProcessExpenseType> loadAllExpenseTypes() {
        return getProcessExpenseTypeMapper().selectList(
                Wrappers.<ProcessExpenseType>lambdaQuery()
                        .orderByAsc(ProcessExpenseType::getExpenseCode, ProcessExpenseType::getId)
        );
    }

    protected List<ProcessExpenseType> loadEnabledExpenseTypes() {
        return getProcessExpenseTypeMapper().selectList(
                Wrappers.<ProcessExpenseType>lambdaQuery()
                        .eq(ProcessExpenseType::getStatus, 1)
                        .orderByAsc(ProcessExpenseType::getExpenseCode, ProcessExpenseType::getId)
        );
    }

    protected List<ProcessExpenseTypeTreeVO> loadEnabledExpenseTypeTree() {
        return buildExpenseTypeTree(loadEnabledExpenseTypes());
    }

    protected List<ProcessExpenseTypeTreeVO> buildExpenseTypeTree(List<ProcessExpenseType> expenseTypes) {
        if (expenseTypes.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, ProcessExpenseTypeTreeVO> nodeMap = new LinkedHashMap<>();
        List<ProcessExpenseTypeTreeVO> roots = new ArrayList<>();
        for (ProcessExpenseType expenseType : expenseTypes) {
            nodeMap.put(expenseType.getId(), toExpenseTypeTree(expenseType));
        }

        for (ProcessExpenseType expenseType : expenseTypes) {
            ProcessExpenseTypeTreeVO node = nodeMap.get(expenseType.getId());
            if (expenseType.getParentId() == null || !nodeMap.containsKey(expenseType.getParentId())) {
                roots.add(node);
                continue;
            }
            nodeMap.get(expenseType.getParentId()).getChildren().add(node);
        }
        return roots;
    }

    protected ProcessExpenseTypeTreeVO toExpenseTypeTree(ProcessExpenseType expenseType) {
        ProcessExpenseTypeTreeVO treeNode = new ProcessExpenseTypeTreeVO();
        treeNode.setId(expenseType.getId());
        treeNode.setParentId(expenseType.getParentId());
        treeNode.setExpenseCode(expenseType.getExpenseCode());
        treeNode.setExpenseName(expenseType.getExpenseName());
        treeNode.setStatus(expenseType.getStatus());
        return treeNode;
    }

    protected ProcessExpenseTypeDetailVO buildExpenseTypeDetail(ProcessExpenseType expenseType) {
        ProcessExpenseTypeDetailVO detail = new ProcessExpenseTypeDetailVO();
        detail.setId(expenseType.getId());
        detail.setParentId(expenseType.getParentId());
        detail.setExpenseCode(expenseType.getExpenseCode());
        detail.setExpenseName(expenseType.getExpenseName());
        detail.setExpenseDescription(expenseType.getExpenseDescription());
        detail.setCodeLevel(expenseType.getCodeLevel());
        detail.setCodePrefix(expenseType.getCodePrefix());
        detail.setScopeDeptIds(deserializeStringList(expenseType.getScopeDeptIds()));
        detail.setScopeUserIds(deserializeStringList(expenseType.getScopeUserIds()));
        detail.setInvoiceFreeMode(expenseType.getInvoiceFreeMode());
        detail.setTaxDeductionMode(expenseType.getTaxDeductionMode());
        detail.setTaxSeparationMode(expenseType.getTaxSeparationMode());
        detail.setStatus(expenseType.getStatus());
        return detail;
    }

    protected void validateExpenseType(ProcessExpenseTypeSaveDTO dto, ProcessExpenseType existing) {
        String expenseCode = trimToEmpty(dto.getExpenseCode());
        validatePmNameLength(dto.getExpenseName(), "\u8d39\u7528\u7c7b\u578b\u540d\u79f0");
        if (!expenseCode.matches("\\d{6}(\\d{2})?")) {
            throw new IllegalArgumentException("\u8d39\u7528\u7c7b\u578b\u7f16\u7801\u5fc5\u987b\u4e3a 6 \u4f4d\u6216 8 \u4f4d\u6570\u5b57");
        }

        if (!EXPENSE_TYPE_INVOICE_MODES.contains(trimToEmpty(dto.getInvoiceFreeMode()))) {
            throw new IllegalArgumentException("\u662f\u5426\u514d\u7968\u914d\u7f6e\u4e0d\u5408\u6cd5");
        }
        if (!EXPENSE_TYPE_TAX_MODES.contains(trimToEmpty(dto.getTaxDeductionMode()))) {
            throw new IllegalArgumentException("\u7a0e\u989d\u62b5\u6263\u4e0e\u8f6c\u51fa\u914d\u7f6e\u4e0d\u5408\u6cd5");
        }
        if (!EXPENSE_TYPE_SEPARATION_MODES.contains(trimToEmpty(dto.getTaxSeparationMode()))) {
            throw new IllegalArgumentException("\u4ef7\u7a0e\u5206\u79bb\u89c4\u5219\u914d\u7f6e\u4e0d\u5408\u6cd5");
        }

        ProcessExpenseType duplicated = findExpenseTypeByCode(expenseCode);
        if (duplicated != null && (existing == null || !Objects.equals(duplicated.getId(), existing.getId()))) {
            throw new IllegalArgumentException("\u8d39\u7528\u7c7b\u578b\u7f16\u7801\u5df2\u5b58\u5728");
        }

        ProcessExpenseType parentExpenseType = null;
        if (expenseCode.length() == 8) {
            parentExpenseType = findExpenseTypeByCode(expenseCode.substring(0, 6));
            if (parentExpenseType == null) {
                throw new IllegalArgumentException("8 \u4f4d\u8d39\u7528\u7c7b\u578b\u7f16\u7801\u5fc5\u987b\u5148\u5b58\u5728\u5bf9\u5e94\u7684 6 \u4f4d\u7236\u7ea7\u7f16\u7801");
            }
        }

        if (existing != null && !Objects.equals(existing.getExpenseCode(), expenseCode)) {
            if (hasExpenseTypeChildren(existing.getId())) {
                throw new IllegalStateException("\u5f53\u524d\u8d39\u7528\u7c7b\u578b\u5b58\u5728\u5b50\u7ea7\u8282\u70b9\uff0c\u4e0d\u80fd\u4fee\u6539\u7f16\u7801");
            }
            if (isExpenseTypeReferenced(existing)) {
                throw new IllegalStateException("\u5f53\u524d\u8d39\u7528\u7c7b\u578b\u5df2\u88ab\u6a21\u677f\u5f15\u7528\uff0c\u4e0d\u80fd\u4fee\u6539\u7f16\u7801");
            }
        }

        Integer targetStatus = normalizeStatus(dto.getStatus());
        if (targetStatus == 1 && parentExpenseType != null && !Objects.equals(parentExpenseType.getStatus(), 1)) {
            throw new IllegalStateException("\u7236\u7ea7\u8d39\u7528\u7c7b\u578b\u672a\u542f\u7528\uff0c\u5b50\u7ea7\u4e0d\u80fd\u76f4\u63a5\u542f\u7528");
        }

        validateSelectableIds(normalizeIdList(dto.getScopeDeptIds()), loadValidDepartmentIdSet(), "\u90e8\u95e8");
        validateSelectableIds(normalizeIdList(dto.getScopeUserIds()), loadValidUserIdSet(), "\u4eba\u5458");
    }

    protected void applyExpenseTypeBase(ProcessExpenseType expenseType, ProcessExpenseTypeSaveDTO dto) {
        String expenseCode = trimToEmpty(dto.getExpenseCode());
        ProcessExpenseType parentExpenseType = expenseCode.length() == 8 ? findExpenseTypeByCode(expenseCode.substring(0, 6)) : null;

        expenseType.setParentId(parentExpenseType == null ? null : parentExpenseType.getId());
        expenseType.setExpenseCode(expenseCode);
        expenseType.setExpenseName(trimToEmpty(dto.getExpenseName()));
        expenseType.setExpenseDescription(trimToNull(dto.getExpenseDescription()));
        expenseType.setCodeLevel(expenseCode.length() == 6 ? 1 : 2);
        expenseType.setCodePrefix(expenseCode.substring(0, 4));
        expenseType.setScopeDeptIds(serializeStringList(dto.getScopeDeptIds()));
        expenseType.setScopeUserIds(serializeStringList(dto.getScopeUserIds()));
        expenseType.setInvoiceFreeMode(trimToEmpty(dto.getInvoiceFreeMode()));
        expenseType.setTaxDeductionMode(trimToEmpty(dto.getTaxDeductionMode()));
        expenseType.setTaxSeparationMode(trimToEmpty(dto.getTaxSeparationMode()));
        expenseType.setStatus(normalizeStatus(dto.getStatus()));
    }

    protected void validateExpenseTypeStatus(ProcessExpenseType expenseType, Integer status) {
        if (status != 1 || expenseType.getParentId() == null) {
            return;
        }
        ProcessExpenseType parentExpenseType = getProcessExpenseTypeMapper().selectById(expenseType.getParentId());
        if (parentExpenseType != null && !Objects.equals(parentExpenseType.getStatus(), 1)) {
            throw new IllegalStateException("\u7236\u7ea7\u8d39\u7528\u7c7b\u578b\u672a\u542f\u7528\uff0c\u5b50\u7ea7\u4e0d\u80fd\u542f\u7528");
        }
    }

    protected void disableExpenseTypeChildren(Long parentId) {
        List<ProcessExpenseType> children = getProcessExpenseTypeMapper().selectList(
                Wrappers.<ProcessExpenseType>lambdaQuery()
                        .eq(ProcessExpenseType::getParentId, parentId)
        );
        for (ProcessExpenseType child : children) {
            if (!Objects.equals(child.getStatus(), 0)) {
                child.setStatus(0);
                getProcessExpenseTypeMapper().updateById(child);
            }
            disableExpenseTypeChildren(child.getId());
        }
    }

    protected ProcessExpenseType requireExpenseType(Long id) {
        ProcessExpenseType expenseType = getProcessExpenseTypeMapper().selectById(id);
        if (expenseType == null) {
            throw new IllegalStateException("\u8d39\u7528\u7c7b\u578b\u4e0d\u5b58\u5728");
        }
        return expenseType;
    }

    protected boolean hasExpenseTypeChildren(Long id) {
        Long count = getProcessExpenseTypeMapper().selectCount(
                Wrappers.<ProcessExpenseType>lambdaQuery()
                        .eq(ProcessExpenseType::getParentId, id)
        );
        return count != null && count > 0;
    }

    protected boolean isExpenseTypeReferenced(ProcessExpenseType expenseType) {
        Long count = getScopeMapper().selectCount(
                Wrappers.<ProcessTemplateScope>lambdaQuery()
                        .eq(ProcessTemplateScope::getOptionType, "EXPENSE_TYPE")
                        .eq(ProcessTemplateScope::getOptionCode, expenseType.getExpenseCode())
        );
        return count != null && count > 0;
    }

    protected ProcessExpenseType findExpenseTypeByCode(String expenseCode) {
        return getProcessExpenseTypeMapper().selectOne(
                Wrappers.<ProcessExpenseType>lambdaQuery()
                        .eq(ProcessExpenseType::getExpenseCode, expenseCode)
                        .last("limit 1")
        );
    }

    protected ProcessExpenseTypeConfigOptionVO configOption(String value, String label, String description) {
        ProcessExpenseTypeConfigOptionVO option = new ProcessExpenseTypeConfigOptionVO();
        option.setValue(value);
        option.setLabel(label);
        option.setDescription(description);
        return option;
    }
}
