package com.finex.auth.service.impl.process;

import com.finex.auth.dto.ProcessFlowConditionFieldVO;
import com.finex.auth.dto.ProcessFormOptionVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ProcessUserGroupScopeSupport {

    static final String FIELD_UNDERTAKE_DEPT_WITH_CHILDREN = "undertakeDeptIdWithChildren";
    static final String FIELD_UNDERTAKE_DEPT_EXACT = "undertakeDeptIdExact";
    static final String FIELD_SUBMITTER_DEPT_WITH_CHILDREN = "submitterDeptIdWithChildren";
    static final String FIELD_SUBMITTER_DEPT_EXACT = "submitterDeptIdExact";
    static final String FIELD_PAYMENT_COMPANY_ID = "paymentCompanyId";
    static final String FIELD_ACTUAL_PAYMENT_AMOUNT = "actualPaymentAmount";

    private static final String VALUE_TYPE_DEPARTMENT = "department";
    private static final String VALUE_TYPE_COMPANY = "company";
    private static final String VALUE_TYPE_NUMBER = "number";

    private static final List<String> SET_OPERATORS = List.of("IN", "NOT_IN");
    private static final List<String> NUMBER_OPERATORS = List.of("GT", "GE", "EQ", "NE", "LT", "LE");

    private static final Map<String, List<String>> FIELD_OPERATOR_MAP = Map.of(
            FIELD_UNDERTAKE_DEPT_WITH_CHILDREN, SET_OPERATORS,
            FIELD_UNDERTAKE_DEPT_EXACT, SET_OPERATORS,
            FIELD_SUBMITTER_DEPT_WITH_CHILDREN, SET_OPERATORS,
            FIELD_SUBMITTER_DEPT_EXACT, SET_OPERATORS,
            FIELD_PAYMENT_COMPANY_ID, SET_OPERATORS,
            FIELD_ACTUAL_PAYMENT_AMOUNT, NUMBER_OPERATORS
    );

    private ProcessUserGroupScopeSupport() {
    }

    static List<ProcessFlowConditionFieldVO> buildConditionFields() {
        return List.of(
                conditionField(FIELD_UNDERTAKE_DEPT_WITH_CHILDREN, "\u627f\u62c5\u90e8\u95e8\uff08\u542b\u4e0b\u7ea7\uff09", VALUE_TYPE_DEPARTMENT, SET_OPERATORS),
                conditionField(FIELD_UNDERTAKE_DEPT_EXACT, "\u627f\u62c5\u90e8\u95e8\uff08\u4e0d\u542b\u4e0b\u7ea7\uff09", VALUE_TYPE_DEPARTMENT, SET_OPERATORS),
                conditionField(FIELD_SUBMITTER_DEPT_WITH_CHILDREN, "\u63d0\u5355\u4eba\u90e8\u95e8\uff08\u542b\u4e0b\u7ea7\uff09", VALUE_TYPE_DEPARTMENT, SET_OPERATORS),
                conditionField(FIELD_SUBMITTER_DEPT_EXACT, "\u63d0\u5355\u4eba\u90e8\u95e8\uff08\u4e0d\u542b\u4e0b\u7ea7\uff09", VALUE_TYPE_DEPARTMENT, SET_OPERATORS),
                conditionField(FIELD_PAYMENT_COMPANY_ID, "\u516c\u53f8\u62ac\u5934", VALUE_TYPE_COMPANY, SET_OPERATORS),
                conditionField(FIELD_ACTUAL_PAYMENT_AMOUNT, "\u5b9e\u9645\u652f\u4ed8\u91d1\u989d", VALUE_TYPE_NUMBER, NUMBER_OPERATORS)
        );
    }

    static List<ProcessFormOptionVO> buildOperatorOptions() {
        List<ProcessFormOptionVO> options = new ArrayList<>();
        options.add(option("IN", "\u5c5e\u4e8e"));
        options.add(option("NOT_IN", "\u4e0d\u5c5e\u4e8e"));
        options.add(option("GT", "\u5927\u4e8e"));
        options.add(option("GE", "\u5927\u4e8e\u6216\u7b49\u4e8e"));
        options.add(option("EQ", "\u7b49\u4e8e"));
        options.add(option("NE", "\u4e0d\u7b49\u4e8e"));
        options.add(option("LT", "\u5c0f\u4e8e"));
        options.add(option("LE", "\u5c0f\u4e8e\u6216\u7b49\u4e8e"));
        return options;
    }

    static boolean supportsField(String fieldKey) {
        return FIELD_OPERATOR_MAP.containsKey(fieldKey);
    }

    static boolean supportsOperator(String fieldKey, String operator) {
        return FIELD_OPERATOR_MAP.getOrDefault(fieldKey, List.of()).contains(operator);
    }

    static boolean isNumberField(String fieldKey) {
        return FIELD_ACTUAL_PAYMENT_AMOUNT.equals(fieldKey);
    }

    static Set<String> supportedFieldKeys() {
        return new LinkedHashSet<>(FIELD_OPERATOR_MAP.keySet());
    }

    static Map<String, List<String>> fieldOperatorMap() {
        return new LinkedHashMap<>(FIELD_OPERATOR_MAP);
    }

    private static ProcessFlowConditionFieldVO conditionField(String key, String label, String valueType, List<String> operators) {
        ProcessFlowConditionFieldVO item = new ProcessFlowConditionFieldVO();
        item.setKey(key);
        item.setLabel(label);
        item.setValueType(valueType);
        item.setOperatorKeys(new ArrayList<>(operators));
        return item;
    }

    private static ProcessFormOptionVO option(String value, String label) {
        ProcessFormOptionVO item = new ProcessFormOptionVO();
        item.setValue(value);
        item.setLabel(label);
        return item;
    }
}
