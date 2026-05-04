package com.finex.auth.service.impl.process;

import com.finex.auth.dto.ProcessCustomArchiveRuleFieldVO;
import com.finex.auth.dto.ProcessFlowConditionFieldVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ProcessExpenseConditionFieldSupport {

    static final String VALUE_TYPE_TEXT = "text";
    static final String VALUE_TYPE_NUMBER = "number";
    static final String VALUE_TYPE_DEPARTMENT = "department";
    static final String VALUE_TYPE_USER = "user";
    static final String VALUE_TYPE_COMPANY = "company";
    static final String VALUE_TYPE_EXPENSE_TYPE = "expenseType";
    static final String VALUE_TYPE_ARCHIVE = "archive";
    static final String VALUE_TYPE_SHARED_ARCHIVE_PREFIX = "sharedArchive:";

    static final String FIELD_SUBMITTER_DEPT_WITH_CHILDREN = "submitterDeptId";
    static final String FIELD_SUBMITTER_DEPT_EXACT = "submitterDeptIds";
    static final String FIELD_SUBMITTER_USER_ID = "submitterUserId";
    static final String FIELD_EXPENSE_TYPE_CODE = "expenseTypeCode";
    static final String FIELD_DOCUMENT_TYPE = "documentType";
    static final String FIELD_AMOUNT = "amount";
    static final String FIELD_SUBMITTER_POSITION = "submitterPosition";
    static final String FIELD_LABOR_RELATION_BELONG = "laborRelationBelong";
    static final String FIELD_TAG_ARCHIVE_CODE = "tagArchiveCode";
    static final String FIELD_INSTALLMENT_ARCHIVE_CODE = "installmentArchiveCode";

    private static final List<String> DEPARTMENT_OPERATORS = List.of("EQ", "NE", "IN", "NOT_IN");
    private static final List<String> SET_OPERATORS = List.of("IN", "NOT_IN");
    private static final List<String> TEXT_OPERATORS = List.of("EQ", "NE", "IN", "NOT_IN", "CONTAINS");
    private static final List<String> SIMPLE_TEXT_OPERATORS = List.of("EQ", "NE", "IN", "NOT_IN");
    private static final List<String> AMOUNT_OPERATORS = List.of("EQ", "NE", "GT", "GE", "LT", "LE", "BETWEEN");
    private static final List<String> PAYMENT_AMOUNT_OPERATORS = List.of("GT", "GE", "EQ", "NE", "LT", "LE");

    private static final List<ConditionFieldDefinition> BRANCH_FIELD_DEFINITIONS = List.of(
            definition(FIELD_SUBMITTER_DEPT_WITH_CHILDREN, "提单人部门（含下级）", VALUE_TYPE_DEPARTMENT, DEPARTMENT_OPERATORS),
            definition(FIELD_SUBMITTER_DEPT_EXACT, "提单人部门（不含下级）", VALUE_TYPE_DEPARTMENT, DEPARTMENT_OPERATORS),
            definition(FIELD_SUBMITTER_USER_ID, "提单人", VALUE_TYPE_USER, DEPARTMENT_OPERATORS),
            definition(ProcessUserGroupScopeSupport.FIELD_PAYMENT_COMPANY_ID, "公司抬头", VALUE_TYPE_COMPANY, SET_OPERATORS),
            definition(ProcessUserGroupScopeSupport.FIELD_UNDERTAKE_DEPT_WITH_CHILDREN, "承担部门（含下级）", VALUE_TYPE_DEPARTMENT, SET_OPERATORS),
            definition(ProcessUserGroupScopeSupport.FIELD_UNDERTAKE_DEPT_EXACT, "承担部门（不含下级）", VALUE_TYPE_DEPARTMENT, SET_OPERATORS),
            definition(FIELD_EXPENSE_TYPE_CODE, "费用类型", VALUE_TYPE_EXPENSE_TYPE, SIMPLE_TEXT_OPERATORS),
            definition(FIELD_DOCUMENT_TYPE, "单据类型", VALUE_TYPE_TEXT, SIMPLE_TEXT_OPERATORS),
            definition(FIELD_AMOUNT, "金额区间", VALUE_TYPE_NUMBER, AMOUNT_OPERATORS),
            definition(FIELD_TAG_ARCHIVE_CODE, "标签档案", VALUE_TYPE_ARCHIVE, SIMPLE_TEXT_OPERATORS),
            definition(FIELD_INSTALLMENT_ARCHIVE_CODE, "分期付款档案", VALUE_TYPE_ARCHIVE, SIMPLE_TEXT_OPERATORS)
    );

    private static final List<ConditionFieldDefinition> SCOPE_FIELD_DEFINITIONS = List.of(
            definition(ProcessUserGroupScopeSupport.FIELD_UNDERTAKE_DEPT_WITH_CHILDREN, "承担部门（含下级）", VALUE_TYPE_DEPARTMENT, SET_OPERATORS),
            definition(ProcessUserGroupScopeSupport.FIELD_UNDERTAKE_DEPT_EXACT, "承担部门（不含下级）", VALUE_TYPE_DEPARTMENT, SET_OPERATORS),
            definition(ProcessUserGroupScopeSupport.FIELD_SUBMITTER_DEPT_WITH_CHILDREN, "提单人部门（含下级）", VALUE_TYPE_DEPARTMENT, SET_OPERATORS),
            definition(ProcessUserGroupScopeSupport.FIELD_SUBMITTER_DEPT_EXACT, "提单人部门（不含下级）", VALUE_TYPE_DEPARTMENT, SET_OPERATORS),
            definition(ProcessUserGroupScopeSupport.FIELD_PAYMENT_COMPANY_ID, "公司抬头", VALUE_TYPE_COMPANY, SET_OPERATORS),
            definition(ProcessUserGroupScopeSupport.FIELD_ACTUAL_PAYMENT_AMOUNT, "实际支付金额", VALUE_TYPE_NUMBER, PAYMENT_AMOUNT_OPERATORS)
    );

    private static final List<ConditionFieldDefinition> CUSTOM_ARCHIVE_FIELD_DEFINITIONS = List.of(
            definition(FIELD_SUBMITTER_DEPT_WITH_CHILDREN, "提单人部门（含下级）", VALUE_TYPE_DEPARTMENT, DEPARTMENT_OPERATORS),
            definition(FIELD_SUBMITTER_DEPT_EXACT, "提单人部门（不含下级）", VALUE_TYPE_DEPARTMENT, DEPARTMENT_OPERATORS),
            definition(ProcessUserGroupScopeSupport.FIELD_UNDERTAKE_DEPT_WITH_CHILDREN, "承担部门（含下级）", VALUE_TYPE_DEPARTMENT, SET_OPERATORS),
            definition(ProcessUserGroupScopeSupport.FIELD_UNDERTAKE_DEPT_EXACT, "承担部门（不含下级）", VALUE_TYPE_DEPARTMENT, SET_OPERATORS),
            definition(FIELD_SUBMITTER_USER_ID, "提单人", VALUE_TYPE_USER, DEPARTMENT_OPERATORS),
            definition(ProcessUserGroupScopeSupport.FIELD_PAYMENT_COMPANY_ID, "公司抬头", VALUE_TYPE_COMPANY, SET_OPERATORS),
            definition(FIELD_EXPENSE_TYPE_CODE, "费用类型", VALUE_TYPE_EXPENSE_TYPE, SIMPLE_TEXT_OPERATORS),
            definition(FIELD_DOCUMENT_TYPE, "单据类型", VALUE_TYPE_TEXT, SIMPLE_TEXT_OPERATORS),
            definition(FIELD_SUBMITTER_POSITION, "提单人岗位", VALUE_TYPE_TEXT, TEXT_OPERATORS),
            definition(FIELD_LABOR_RELATION_BELONG, "劳动关系归属", VALUE_TYPE_TEXT, TEXT_OPERATORS),
            definition(FIELD_AMOUNT, "金额", VALUE_TYPE_NUMBER, AMOUNT_OPERATORS),
            definition(ProcessUserGroupScopeSupport.FIELD_ACTUAL_PAYMENT_AMOUNT, "实际支付金额", VALUE_TYPE_NUMBER, PAYMENT_AMOUNT_OPERATORS),
            definition(FIELD_TAG_ARCHIVE_CODE, "标签档案", VALUE_TYPE_ARCHIVE, SIMPLE_TEXT_OPERATORS),
            definition(FIELD_INSTALLMENT_ARCHIVE_CODE, "分期付款档案", VALUE_TYPE_ARCHIVE, SIMPLE_TEXT_OPERATORS)
    );

    private ProcessExpenseConditionFieldSupport() {
    }

    static List<ProcessFlowConditionFieldVO> buildBranchConditionFields() {
        return toFlowConditionFields(BRANCH_FIELD_DEFINITIONS);
    }

    static ProcessFlowConditionFieldVO buildSharedArchiveBranchField(String archiveCode, String archiveName) {
        return toFlowConditionField(definition(
                archiveCode,
                archiveName,
                sharedArchiveValueType(archiveCode),
                SIMPLE_TEXT_OPERATORS
        ));
    }

    static String sharedArchiveValueType(String archiveCode) {
        return VALUE_TYPE_SHARED_ARCHIVE_PREFIX + archiveCode;
    }

    static List<ProcessFlowConditionFieldVO> buildScopeConditionFields() {
        return toFlowConditionFields(SCOPE_FIELD_DEFINITIONS);
    }

    static Map<String, List<String>> buildScopeFieldOperatorMap() {
        return toFieldOperatorMap(SCOPE_FIELD_DEFINITIONS);
    }

    static Set<String> scopeSupportedFieldKeys() {
        return new LinkedHashSet<>(buildScopeFieldOperatorMap().keySet());
    }

    static List<ProcessCustomArchiveRuleFieldVO> buildCustomArchiveRuleFields() {
        return CUSTOM_ARCHIVE_FIELD_DEFINITIONS.stream().map(ProcessExpenseConditionFieldSupport::toCustomArchiveRuleField).toList();
    }

    static Map<String, ConditionFieldDefinition> buildCustomArchiveRuleFieldMap() {
        LinkedHashMap<String, ConditionFieldDefinition> result = new LinkedHashMap<>();
        for (ConditionFieldDefinition definition : CUSTOM_ARCHIVE_FIELD_DEFINITIONS) {
            result.put(definition.key(), definition);
        }
        return result;
    }

    private static List<ProcessFlowConditionFieldVO> toFlowConditionFields(List<ConditionFieldDefinition> definitions) {
        return definitions.stream().map(ProcessExpenseConditionFieldSupport::toFlowConditionField).toList();
    }

    private static Map<String, List<String>> toFieldOperatorMap(List<ConditionFieldDefinition> definitions) {
        LinkedHashMap<String, List<String>> result = new LinkedHashMap<>();
        for (ConditionFieldDefinition definition : definitions) {
            result.put(definition.key(), new ArrayList<>(definition.operatorKeys()));
        }
        return result;
    }

    private static ProcessFlowConditionFieldVO toFlowConditionField(ConditionFieldDefinition definition) {
        ProcessFlowConditionFieldVO item = new ProcessFlowConditionFieldVO();
        item.setKey(definition.key());
        item.setLabel(definition.label());
        item.setValueType(definition.valueType());
        item.setOperatorKeys(new ArrayList<>(definition.operatorKeys()));
        return item;
    }

    private static ProcessCustomArchiveRuleFieldVO toCustomArchiveRuleField(ConditionFieldDefinition definition) {
        ProcessCustomArchiveRuleFieldVO item = new ProcessCustomArchiveRuleFieldVO();
        item.setKey(definition.key());
        item.setLabel(definition.label());
        item.setValueType(definition.valueType());
        item.setOperatorKeys(new ArrayList<>(definition.operatorKeys()));
        return item;
    }

    private static ConditionFieldDefinition definition(String key, String label, String valueType, List<String> operatorKeys) {
        return new ConditionFieldDefinition(key, label, valueType, List.copyOf(operatorKeys));
    }

    record ConditionFieldDefinition(
            String key,
            String label,
            String valueType,
            List<String> operatorKeys
    ) {
    }
}
