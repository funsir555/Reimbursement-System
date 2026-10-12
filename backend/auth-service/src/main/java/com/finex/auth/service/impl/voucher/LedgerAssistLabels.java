package com.finex.auth.service.impl.voucher;

import com.finex.auth.dto.FinanceVoucherOptionVO;
import com.finex.auth.entity.GlAccass;
import com.finex.auth.entity.GlAccvouch;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Shared auxiliary display policy for balance sheets and detail ledgers. */
record LedgerAssistLabels(
        Map<String, FinanceVoucherOptionVO> customers,
        Map<String, FinanceVoucherOptionVO> suppliers,
        Map<String, FinanceVoucherOptionVO> projects,
        Map<String, FinanceVoucherOptionVO> projectClasses,
        Map<String, FinanceVoucherOptionVO> departments,
        Map<String, FinanceVoucherOptionVO> employees
) {
    String format(Key key, String displayMode) {
        List<String> parts = new ArrayList<>();
        add(parts, "客户", customers.get(key.ccusId()), key.ccusId(), displayMode);
        add(parts, "供应商", suppliers.get(key.csupId()), key.csupId(), displayMode);
        FinanceVoucherOptionVO project = projects.get(key.citemId());
        if (project == null && key.citemClass() != null) {
            project = projectClasses.get(key.citemClass());
        }
        add(parts, "项目", project, firstNonBlank(key.citemId(), key.citemClass()), displayMode);
        add(parts, "部门", departments.get(key.cdeptId()), key.cdeptId(), displayMode);
        add(parts, "个人", employees.get(key.cpersonId()), key.cpersonId(), displayMode);
        return String.join("*", parts);
    }

    private void add(List<String> parts, String dimension, FinanceVoucherOptionVO option,
                     String fallback, String displayMode) {
        String value = switch (displayMode == null ? "NAME" : displayMode) {
            case "CODE" -> option == null ? fallback : firstNonBlank(option.getCode(), option.getValue(), fallback);
            case "CODE_NAME" -> option == null ? fallback : joinCodeName(option.getCode(), option.getName(), option.getValue());
            default -> option == null ? fallback : firstNonBlank(option.getName(), option.getLabel(), fallback);
        };
        if (value != null) {
            parts.add(dimension + "：" + value);
        }
    }

    private static String joinCodeName(String code, String name, String fallback) {
        String normalizedCode = firstNonBlank(code, fallback);
        String normalizedName = trim(name);
        if (normalizedCode == null) {
            return normalizedName;
        }
        return normalizedName == null || normalizedName.equals(normalizedCode)
                ? normalizedCode : normalizedCode + " " + normalizedName;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trim(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    record Key(String subjectCode, String ccusId, String csupId, String citemClass,
               String citemId, String cdeptId, String cpersonId) implements Comparable<Key> {
        static Key from(GlAccass row) {
            return new Key(trim(row.getCcode()), trim(row.getCcusId()), trim(row.getCsupId()),
                    trim(row.getCitemClass()), trim(row.getCitemId()), trim(row.getCdeptId()), trim(row.getCpersonId()));
        }

        static Key from(GlAccvouch row) {
            return new Key(trim(row.getCcode()), trim(row.getCcusId()), trim(row.getCsupId()),
                    trim(row.getCitemClass()), trim(row.getCitemId()), trim(row.getCdeptId()), trim(row.getCpersonId()));
        }

        @Override
        public int compareTo(Key other) {
            return Comparator.comparing(Key::subjectCode, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(Key::ccusId, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(Key::csupId, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(Key::citemClass, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(Key::citemId, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(Key::cdeptId, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(Key::cpersonId, Comparator.nullsFirst(String::compareTo))
                    .compare(this, other);
        }
    }
}
