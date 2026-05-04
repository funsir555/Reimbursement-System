package com.finex.auth.service.impl.expense;

import com.finex.auth.dto.ExpenseCreateTemplateDetailVO;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseDetailDesign;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.support.UserDepartmentSupport;
import com.finex.auth.service.impl.ExpenseDetailSystemFieldSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
class ExpenseDocumentTemplateDetailSupport {

    private final AbstractExpenseDocumentSupport support;
    private final ExpenseDetailSystemFieldSupport expenseDetailSystemFieldSupport;
    private final UserMapper userMapper;
    private final SystemCompanyMapper systemCompanyMapper;
    private final SystemDepartmentMapper systemDepartmentMapper;

    ExpenseCreateTemplateDetailVO getTemplateDetail(Long userId, String templateCode) {
        ProcessDocumentTemplate template = support.requireTemplate(templateCode);
        return buildTemplateDetail(userId, template);
    }

    ExpenseCreateTemplateDetailVO getDocumentTemplateDetail(Long userId, String templateCode) {
        ProcessDocumentTemplate template = support.requireTemplateForDocument(templateCode);
        return buildTemplateDetail(userId, template);
    }

    private ExpenseCreateTemplateDetailVO buildTemplateDetail(Long userId, ProcessDocumentTemplate template) {
        ExpenseCreateTemplateDetailVO detail = new ExpenseCreateTemplateDetailVO();
        detail.setTemplateCode(template.getTemplateCode());
        detail.setTemplateName(template.getTemplateName());
        detail.setTemplateType(template.getTemplateType());
        detail.setTemplateTypeLabel(support.resolveTemplateTypeLabel(template.getTemplateType(), template.getTemplateTypeLabel()));
        detail.setCategoryCode(template.getCategoryCode());
        detail.setTemplateDescription(template.getTemplateDescription());
        detail.setFormDesignCode(template.getFormDesignCode());
        detail.setApprovalFlowCode(template.getApprovalFlow());
        detail.setFlowName(template.getFlowName());
        detail.setExpenseDetailDesignCode(template.getExpenseDetailDesignCode());
        detail.setExpenseDetailModeDefault(template.getExpenseDetailModeDefault());
        detail.setFlowSnapshot(support.readMap(support.resolveFlowSnapshotJson(template)));

        ProcessFormDesign formDesign = support.loadFormDesign(template.getFormDesignCode());
        if (formDesign != null) {
            detail.setFormName(formDesign.getFormName());
            detail.setSchema(support.readSchema(formDesign.getSchemaJson()));
            detail.setSharedArchives(support.loadSharedArchives(detail.getSchema()));
        } else {
            detail.setSchema(support.defaultSchema());
            detail.setSharedArchives(Collections.emptyList());
        }

        ProcessExpenseDetailDesign expenseDetailDesign = support.loadExpenseDetailDesign(template.getExpenseDetailDesignCode());
        if (expenseDetailDesign != null) {
            detail.setExpenseDetailDesignName(expenseDetailDesign.getDetailName());
            detail.setExpenseDetailType(expenseDetailDesign.getDetailType());
            detail.setExpenseDetailTypeLabel(support.resolveExpenseDetailTypeLabel(expenseDetailDesign.getDetailType()));
            detail.setExpenseDetailSchema(expenseDetailSystemFieldSupport.readSchema(expenseDetailDesign.getSchemaJson(), expenseDetailDesign.getDetailType()));
            detail.setExpenseDetailSharedArchives(support.loadSharedArchives(detail.getExpenseDetailSchema()));
        } else {
            detail.setExpenseDetailType(support.resolveExpenseDetailType(template, null));
            detail.setExpenseDetailTypeLabel(support.resolveExpenseDetailTypeLabel(detail.getExpenseDetailType()));
            detail.setExpenseDetailSchema(support.defaultSchema());
            detail.setExpenseDetailSharedArchives(Collections.emptyList());
        }
        detail.setCompanyOptions(support.loadCompanyOptions());
        detail.setDepartmentOptions(support.loadDepartmentOptions());
        detail.setUserOptions(support.loadUserOptions(detail.getFlowSnapshot()));
        detail.setExpenseTypeOptions(expenseDetailSystemFieldSupport.loadExpenseTypeOptions());
        detail.setExpenseTypeInvoiceFreeModeMap(expenseDetailSystemFieldSupport.loadExpenseTypeInvoiceFreeModeMap());
        User currentUser = userId == null ? null : userMapper.selectById(userId);
        if (currentUser != null) {
            detail.setCurrentUserCompanyId(support.trimToNull(currentUser.getCompanyId()));
            if (support.trimToNull(currentUser.getCompanyId()) != null) {
                SystemCompany company = systemCompanyMapper.selectById(currentUser.getCompanyId());
                if (company != null) {
                    detail.setCurrentUserCompanyName(support.trimToNull(company.getCompanyName()));
                }
            }
        }
        if (currentUser != null && currentUser.getId() != null) {
            List<com.finex.auth.dto.EmployeeDepartmentRefVO> departments =
                    UserDepartmentSupport.loadDepartmentRefsByUserId(
                            userMapper,
                            systemDepartmentMapper,
                            List.of(currentUser.getId())
                    ).getOrDefault(currentUser.getId(), Collections.emptyList());
            if (departments.isEmpty() && currentUser.getDeptId() != null) {
                com.finex.auth.dto.EmployeeDepartmentRefVO fallbackDepartment = new com.finex.auth.dto.EmployeeDepartmentRefVO();
                fallbackDepartment.setDeptId(currentUser.getDeptId());
                com.finex.auth.entity.SystemDepartment department = systemDepartmentMapper.selectById(currentUser.getDeptId());
                fallbackDepartment.setDeptName(department == null ? "" : department.getDeptName());
                departments = List.of(fallbackDepartment);
            }
            detail.setCurrentUserDeptIds(
                    departments.stream().map(item -> String.valueOf(item.getDeptId())).toList()
            );
            detail.setCurrentUserDeptNames(
                    departments.stream().map(item -> support.trimToNull(item.getDeptName())).filter(java.util.Objects::nonNull).toList()
            );
            Long primaryDeptId = UserDepartmentSupport.resolvePrimaryDepartmentId(departments);
            if (primaryDeptId != null) {
                detail.setCurrentUserDeptId(String.valueOf(primaryDeptId));
            }
            detail.setCurrentUserDeptName(UserDepartmentSupport.joinDepartmentNames(departments));
        }
        return detail;
    }
}
