package com.finex.auth.service.impl.process;

import com.finex.auth.dto.ProcessFlowConditionFieldVO;
import com.finex.auth.dto.ProcessFormOptionVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ProcessUserGroupScopeSupport {

    static final String FIELD_UNDERTAKE_DEPT_WITH_CHILDREN = "undertakeDeptIdWithChildren";
    static final String FIELD_UNDERTAKE_DEPT_EXACT = "undertakeDeptIdExact";
    static final String FIELD_SUBMITTER_DEPT_WITH_CHILDREN = "submitterDeptIdWithChildren";
    static final String FIELD_SUBMITTER_DEPT_EXACT = "submitterDeptIdExact";
    static final String FIELD_PAYMENT_COMPANY_ID = "paymentCompanyId";
    static final String FIELD_ACTUAL_PAYMENT_AMOUNT = "actualPaymentAmount";

    private ProcessUserGroupScopeSupport() {
    }

    static List<ProcessFlowConditionFieldVO> buildConditionFields() {
        return ProcessExpenseConditionFieldSupport.buildScopeConditionFields();
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
        return ProcessExpenseConditionFieldSupport.scopeSupportedFieldKeys().contains(fieldKey);
    }

    static boolean supportsOperator(String fieldKey, String operator) {
        return ProcessExpenseConditionFieldSupport.buildScopeFieldOperatorMap()
                .getOrDefault(fieldKey, List.of())
                .contains(operator);
    }

    static boolean isNumberField(String fieldKey) {
        return FIELD_ACTUAL_PAYMENT_AMOUNT.equals(fieldKey);
    }

    static java.util.Set<String> supportedFieldKeys() {
        return ProcessExpenseConditionFieldSupport.scopeSupportedFieldKeys();
    }

    static Map<String, List<String>> fieldOperatorMap() {
        return ProcessExpenseConditionFieldSupport.buildScopeFieldOperatorMap();
    }

    private static ProcessFormOptionVO option(String value, String label) {
        ProcessFormOptionVO item = new ProcessFormOptionVO();
        item.setValue(value);
        item.setLabel(label);
        return item;
    }
}
