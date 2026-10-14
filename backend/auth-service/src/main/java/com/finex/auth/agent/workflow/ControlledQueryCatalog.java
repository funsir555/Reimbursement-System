package com.finex.auth.agent.workflow;

import com.finex.auth.service.queryauth.FinanceLedgerReportPermissionCodes;
import com.finex.auth.service.queryauth.QueryAuthorizationService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fixed, reviewed tool contracts. No client supplied table, SQL or permission code is accepted. */
public final class ControlledQueryCatalog {
    public record Field(String key, String label, String type, boolean required) { }
    public record Definition(String code, String label, String domain, String entry,
                             String permission, String template, String description,
                             String kind, List<Field> fields) {
        /** FinWork reads this contract; execution still accepts only the fixed tool fields. */
        public List<Map<String, Object>> semanticInputs() {
            List<Field> inputs = "EXPENSE".equals(kind) ? List.of(
                    new Field("scope", "报销单范围", "enum", false),
                    new Field("status", "报销单状态", "enum", false),
                    new Field("keyword", "单据关键词", "text", false),
                    new Field("startDate", "起始日期", "date", false),
                    new Field("endDate", "结束日期", "date", false),
                    new Field("minAmount", "最低金额", "decimal", false),
                    new Field("maxAmount", "最高金额", "decimal", false),
                    new Field("limit", "展示条数", "integer", false)) : fields;
            List<Field> described = new java.util.ArrayList<>(inputs);
            if (inputs.stream().anyMatch(field -> "companyId".equals(field.key())))
                described.add(new Field("companyName", "账套名称或简称", "text", false));
            if (inputs.stream().anyMatch(field -> "accountCodeFrom".equals(field.key()))) {
                described.add(new Field("accountNameFrom", "起始科目名称", "text", false));
                described.add(new Field("accountNameTo", "结束科目名称", "text", false));
            }
            return described.stream().map(field -> {
                Map<String, Object> input = new LinkedHashMap<>();
                input.put("key", field.key());
                input.put("label", field.label());
                input.put("type", field.type());
                input.put("required", field.required());
                input.put("meaning", semanticMeaning(field.key()));
                return input;
            }).toList();
        }

        private String semanticMeaning(String key) {
            return switch (key) {
                case "companyId" -> "账套内部编码。用户可说公司全称或简称；先按授权账套名称匹配，不能猜编码。";
                case "companyName" -> "用户提到的账套或公司名称、简称，原样保留供授权账套匹配。";
                case "supplierCode" -> "供应商内部编码。用户通常说名称或简称；请另给 supplierName 供服务端授权档案匹配。";
                case "supplierName" -> "用户提到的供应商名称或简称，原样保留；仅供应商明细账使用。";
                case "yearFrom", "periodFrom", "yearTo", "periodTo" ->
                        "会计期间起止。明确全部期间才按账套启用月到最新可查月；仅说期初期末仍需澄清。可识别去年、本季度等相对表达，但不得猜缺失年份。";
                case "year", "period" -> "未结账公司数的单个会计年月，范围为 1990 至 2090 年、1 至 12 月。";
                case "voucherStatus" -> "仅 POSTED_ONLY（仅已记账）或 INCLUDE_SAVED_UNPOSTED（包含已保存未记账）；未表达时留空，不默认。注意否定词。";
                case "accountCodeFrom", "accountCodeTo" -> "余额表科目编码边界。用户说科目名称时保留名称供授权科目档案解析，不能猜编码。";
                case "accountNameFrom", "accountNameTo" -> "用户提到的科目名称，原样保留供授权科目档案匹配。";
                case "assistDisplay" -> "余额表辅助项展示：NAME、CODE、CODE_NAME；未说明则工具默认 NAME。";
                case "subjectLevelRange" -> "余额表科目级次，仅 1-数字 或 1-n，例如 1-3、1-n；未说明则工具默认 1-n。";
                case "page" -> "页码，从 1 开始；用户未指定时不填写。";
                case "pageSize" -> "每页条数，1 至 200；未指定时不填写。一页并非全部结果。";
                case "scope" -> "报销范围：SUBMITTED_BY_ME、ASSIGNED_TO_ME、VISIBLE；按用户说的我的单据、待我处理或有权查看区分。";
                case "status" -> "报销单状态：DRAFT 草稿、PENDING_APPROVAL 待审批、APPROVED 已审批、REJECTED 已驳回、PENDING_PAYMENT 待付款、PAYING 付款中、PAYMENT_COMPLETED 已付款、PAYMENT_FINISHED 付款完成、EXCEPTION 异常、PAYMENT_EXCEPTION 付款异常；无法确定时澄清。";
                case "startDate", "endDate" -> "报销单日期边界，YYYY-MM-DD；相对日期按服务端当前日期和 Asia/Shanghai 时区理解。";
                case "minAmount", "maxAmount" -> "报销金额下限或上限，非负数字；严格大于/小于无法精确表达时澄清。";
                case "limit" -> "报销单展示条数，1 至 50；不改变授权结果的合计。";
                default -> "仅在用户明确表达该条件时填写，值必须符合工具类型与业务范围。";
            };
        }
        public Map<String, Object> metadata() {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("toolCode", code);
            value.put("label", label);
            value.put("category", "BUSINESS");
            value.put("available", true);
            value.put("domain", domain);
            value.put("entry", entry);
            value.put("permissionCode", permission);
            value.put("queryTemplateCode", template);
            value.put("description", description);
            value.put("testFields", fields.stream().map(field -> Map.of(
                    "key", field.key(), "label", field.label(), "type", field.type(),
                    "required", field.required())).toList());
            return value;
        }
    }

    private static final Field COMPANY = new Field("companyId", "账套", "company", true);
    private static final Field FROM_YEAR = new Field("yearFrom", "起始年度", "integer", true);
    private static final Field FROM_PERIOD = new Field("periodFrom", "起始月份", "integer", true);
    private static final Field TO_YEAR = new Field("yearTo", "结束年度", "integer", true);
    private static final Field TO_PERIOD = new Field("periodTo", "结束月份", "integer", true);
    private static final Field STATUS = new Field("voucherStatus", "凭证范围", "voucherStatus", true);
    private static final Field PAGE = new Field("page", "页码", "integer", false);
    private static final Field SIZE = new Field("pageSize", "每页条数", "integer", false);
    private static final List<Field> CATALOG_FIELDS = List.of(COMPANY, PAGE, SIZE);
    private static final List<Field> LEDGER_FIELDS = List.of(COMPANY, FROM_YEAR, FROM_PERIOD,
            TO_YEAR, TO_PERIOD, STATUS, PAGE, SIZE);

    public static final List<Definition> TOOLS = List.of(
            new Definition("expense.query_my_expenses", "受控查询：报销单与待办", "报销", "ExpenseAgentQueryPort",
                    QueryAuthorizationService.EXPENSE_QUERY, "expense.accessible_expenses.v2",
                    "按当前用户权限查询报销单与待办，并计算授权结果的数量和金额。", "EXPENSE", List.of()),
            new Definition("finance.query_unclosed_companies", "受控查询：未结账公司数", "财务", "FinanceUnclosedCompanyQueryPort",
                    QueryAuthorizationService.FINANCE_CLOSE_QUERY, "finance.unclosed_companies.v1",
                    "统计指定期间授权公司中尚未结账的公司数。", "CLOSE",
                    List.of(new Field("year", "会计年度", "integer", true),
                            new Field("period", "会计期间", "integer", true))),
            new Definition("finance.query_balance_sheet", "受控查询：余额表", "财务", "FinanceAgentQueryPort",
                    FinanceLedgerReportPermissionCodes.BALANCE, "finance.balance_sheet.v1",
                    "按账套和会计期间查询余额表；空科目范围为全部，辅助项默认名称。", "BALANCE",
                    List.of(COMPANY, FROM_YEAR, FROM_PERIOD, TO_YEAR, TO_PERIOD, STATUS,
                            new Field("accountCodeFrom", "起始科目", "text", false),
                            new Field("accountCodeTo", "结束科目", "text", false),
                            new Field("assistDisplay", "辅助项展示", "assistDisplay", false),
                            new Field("subjectLevelRange", "科目级次", "text", false), PAGE, SIZE)),
            new Definition("finance.query_account_subjects", "受控查询：会计科目", "财务", "FinanceAgentQueryPort",
                    "finance:agent:account_subjects:query", "finance.account_subjects.v1", "查询账套内各状态的科目业务字段。", "SUBJECT", CATALOG_FIELDS),
            new Definition("finance.query_project_classes", "受控查询：项目分类", "财务", "FinanceAgentQueryPort",
                    "finance:agent:project_classes:query", "finance.project_classes.v1", "查询账套内各状态的项目分类。", "PROJECT_CLASS", CATALOG_FIELDS),
            new Definition("finance.query_projects", "受控查询：项目名称", "财务", "FinanceAgentQueryPort",
                    "finance:agent:projects:query", "finance.projects.v1", "查询账套内各状态的项目档案。", "PROJECT", CATALOG_FIELDS),
            new Definition("finance.query_customers", "受控查询：客户档案", "财务", "FinanceAgentQueryPort",
                    "finance:agent:customers:query", "finance.customers.v1", "仅查询客户编码、名称、分类和状态等业务字段。", "CUSTOMER", CATALOG_FIELDS),
            new Definition("finance.query_vendors", "受控查询：供应商档案", "财务", "FinanceAgentQueryPort",
                    "finance:agent:vendors:query", "finance.vendors.v1", "仅查询供应商编码、名称、分类和状态等业务字段。", "VENDOR", CATALOG_FIELDS),
            new Definition("system.query_people", "受控查询：人员信息", "系统", "SystemAgentQueryPort",
                    "system:agent:people:query", "system.people.v1", "按姓名包含匹配，仅返回四项指定字段。", "PEOPLE",
                    List.of(new Field("name", "姓名", "text", true), PAGE, SIZE)),
            new Definition("system.query_departments", "受控查询：部门档案", "系统", "SystemAgentQueryPort",
                    "system:agent:departments:query", "system.departments.v1", "查询账套公司的部门层级和状态。", "DEPARTMENT", CATALOG_FIELDS),
            new Definition("finance.query_sequence_entries", "受控查询：序时账明细", "财务", "FinanceAgentQueryPort",
                    FinanceLedgerReportPermissionCodes.SEQUENCE, "finance.sequence_entries.v1",
                    "按会计期间范围查询凭证明细，只返回约定的字段。", "SEQUENCE", LEDGER_FIELDS),
            new Definition("finance.query_supplier_detail_ledger", "受控查询：供应商明细账", "财务", "FinanceAgentQueryPort",
                    "finance:general_ledger:supplier_detail_ledger:view", "finance.supplier_detail_ledger.v1",
                    "按账套、供应商、会计期间和凭证范围查询供应商辅助核算明细账。", "SUPPLIER_LEDGER",
                    List.of(COMPANY, new Field("supplierCode", "供应商", "supplier", true),
                            new Field("supplierName", "供应商名称", "text", false),
                            FROM_YEAR, FROM_PERIOD, TO_YEAR, TO_PERIOD, STATUS, PAGE, SIZE))
    );

    private static final Map<String, Definition> BY_CODE = TOOLS.stream().collect(
            java.util.stream.Collectors.toUnmodifiableMap(Definition::code, item -> item));

    private ControlledQueryCatalog() { }
    public static Definition require(String code) {
        Definition definition = BY_CODE.get(code);
        if (definition == null) throw new IllegalArgumentException("未注册的受控查询工具");
        return definition;
    }
    public static boolean isRegistered(String code) { return BY_CODE.containsKey(code); }
}
