package com.finex.auth.service.impl.financequery;

import com.finex.auth.dto.FinanceBalanceSheetRowVO;
import com.finex.auth.dto.FinanceLedgerReportQueryDTO;
import com.finex.auth.service.FinanceLedgerReportService;
import com.finex.auth.service.port.FinanceAgentQueryPort;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import com.finex.auth.service.queryauth.FinanceLedgerReportPermissionCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All SQL projections here are fixed at compile time and scoped to an authorized account set. */
@Service
@RequiredArgsConstructor
public class FinanceAgentQueryService implements FinanceAgentQueryPort {
    private static final String ACCOUNT_SET_STATUS_ACTIVE = "ACTIVE";

    private final JdbcTemplate jdbc;
    private final FinanceLedgerReportService ledgerReports;
    private final FinanceAgentSupplierLedgerReadService supplierLedger;

    private record Projection(String table, String columns, String order) { }
    private static final Map<String, Projection> ARCHIVES = Map.of(
            "SUBJECT", new Projection("fin_account_subject",
                    "subject_code,subject_name,parent_subject_code,subject_level,balance_direction,subject_category,bperson,bcus,bsup,bdept,bitem,status",
                    "subject_code,id"),
            "PROJECT_CLASS", new Projection("fin_project_class",
                    "project_class_code,project_class_name,status", "project_class_code,id"),
            "PROJECT", new Projection("fin_project_archive",
                    "citemcode,citemname,citemccode,status,bclose", "citemccode,citemcode,id"),
            "CUSTOMER", new Projection("gl_Customer",
                    "cCusCode,cCusName,cCusAbbName,cCCCode,bCusState,dEndDate", "cCusCode"),
            "VENDOR", new Projection("gl_Vender",
                    "cVenCode,cVenName,cVenAbbName,cVCCode,dEndDate", "cVenCode")
    );
    private static final String ENTRY_COLUMNS = "i_id,iyperiod,ino_id,dbill_date,cbill,maker_user_id,ccheck,cbook,ibook,iflag,cdigest,ccode,ccode_name,cexch_name,md,mc,cdept_id,cperson_id,ccus_id,csup_id,citem_id,citem_class,cash_flow_item_id,cash_flow_item_name,cname,company_id";

    @Override
    public Page query(String kind, QueryAuthorization authorization, Map<String, Object> parameters) {
        if (authorization == null || !expectedPermission(kind).equals(authorization.permissionCode()))
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        String companyId = required(parameters, "companyId");
        if (!authorization.allows(companyId, null))
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        Integer active = jdbc.queryForObject("""
                SELECT COUNT(*) FROM fin_account_set a JOIN sys_company c ON c.company_id=a.company_id
                WHERE a.company_id=? AND a.status=? AND c.status=1
                """, Integer.class, companyId, ACCOUNT_SET_STATUS_ACTIVE);
        if (active == null || active != 1) throw new IllegalArgumentException("账套不存在或尚未就绪");
        int page = number(parameters, "page", 1);
        int size = number(parameters, "pageSize", 50);
        if (page < 1 || size < 1 || size > 200 || (long) (page - 1) * size > Integer.MAX_VALUE)
            throw new IllegalArgumentException("分页参数无效");
        if ("BALANCE".equals(kind)) return balance(authorization, parameters, companyId, page, size);
        if ("SEQUENCE".equals(kind)) return sequence(parameters, companyId, page, size);
        if ("SUPPLIER_LEDGER".equals(kind)) return supplierLedger.query(authorization, parameters, page, size);
        Projection projection = ARCHIVES.get(kind);
        if (projection == null) throw new IllegalArgumentException("未知财务查询类型");
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM " + projection.table() + " WHERE company_id=?",
                Long.class, companyId);
        List<Map<String, Object>> records = jdbc.queryForList("SELECT " + projection.columns()
                + " FROM " + projection.table() + " WHERE company_id=? ORDER BY " + projection.order()
                + " LIMIT ? OFFSET ?", companyId, size, (page - 1) * size);
        return new Page(total, page, size, records);
    }

    private String expectedPermission(String kind) {
        return switch (kind) {
            case "BALANCE" -> FinanceLedgerReportPermissionCodes.BALANCE;
            case "SEQUENCE" -> FinanceLedgerReportPermissionCodes.SEQUENCE;
            case "SUBJECT" -> "finance:agent:account_subjects:query";
            case "PROJECT_CLASS" -> "finance:agent:project_classes:query";
            case "PROJECT" -> "finance:agent:projects:query";
            case "CUSTOMER" -> "finance:agent:customers:query";
            case "VENDOR" -> "finance:agent:vendors:query";
            case "SUPPLIER_LEDGER" -> "finance:general_ledger:supplier_detail_ledger:view";
            default -> throw new IllegalArgumentException("未知财务查询类型");
        };
    }

    @Override
    public List<AccountSet> accountSets(QueryAuthorization authorization) {
        return supplierLedger.accountSets(authorization);
    }

    @Override
    public List<Supplier> suppliers(QueryAuthorization authorization, String companyId, String name) {
        return supplierLedger.suppliers(authorization, companyId, name);
    }

    @Override
    public List<Subject> subjects(QueryAuthorization authorization, String companyId, String nameOrCode) {
        if (authorization == null || !FinanceLedgerReportPermissionCodes.BALANCE.equals(authorization.permissionCode())
                || companyId == null || !authorization.allows(companyId, null))
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        String term = nameOrCode == null ? "" : nameOrCode.trim();
        if (term.isBlank() || term.length() > 100) return List.of();
        String pattern = "%" + term.replace("!", "!!").replace("%", "!%")
                .replace("_", "!_") + "%";
        return jdbc.query("""
                SELECT s.subject_code, s.subject_name FROM fin_account_subject s
                JOIN fin_account_set a ON a.company_id=s.company_id AND a.status='ACTIVE'
                JOIN sys_company c ON c.company_id=s.company_id AND c.status=1
                WHERE s.company_id=? AND (s.subject_code=? OR s.subject_name LIKE ? ESCAPE '!')
                ORDER BY s.subject_code LIMIT 51
                """, (rs, row) -> new Subject(rs.getString(1), rs.getString(2)),
                companyId, term, pattern);
    }

    private Page balance(QueryAuthorization authorization, Map<String, Object> values,
                         String companyId, int page, int size) {
        FinanceLedgerReportQueryDTO query = new FinanceLedgerReportQueryDTO();
        query.setCompanyId(companyId);
        query.setIyearFrom(number(values, "yearFrom", null));
        query.setIperiodFrom(number(values, "periodFrom", null));
        query.setIyearTo(number(values, "yearTo", null));
        query.setIperiodTo(number(values, "periodTo", null));
        requirePeriod(values);
        query.setAccountCodeFrom(optional(values, "accountCodeFrom"));
        query.setAccountCodeTo(optional(values, "accountCodeTo"));
        query.setBalanceAssistDisplay(optional(values, "assistDisplay") == null
                ? "NAME" : optional(values, "assistDisplay"));
        query.setSubjectLevelRange(optional(values, "subjectLevelRange") == null
                ? "1-n" : optional(values, "subjectLevelRange"));
        query.setIncludeUnposted(includeUnposted(values));
        query.setPage(page);
        query.setPageSize(size);
        var result = ledgerReports.queryBalanceSheet(authorization.requesterUserId(), query);
        List<Map<String, Object>> records = result.getItems().stream().map(this::balanceRecord).toList();
        return new Page(result.getTotal(), page, size, records);
    }

    private Map<String, Object> balanceRecord(FinanceBalanceSheetRowVO row) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("subjectCode", row.getSubjectCode());
        record.put("subjectName", row.getSubjectName());
        record.put("subjectLevel", row.getSubjectLevel());
        record.put("rowType", row.getRowType());
        record.put("subjectCategory", row.getSubjectCategory());
        record.put("beginDebit", row.getBeginDebit());
        record.put("beginCredit", row.getBeginCredit());
        record.put("periodDebit", row.getPeriodDebit());
        record.put("periodCredit", row.getPeriodCredit());
        record.put("endDebit", row.getEndDebit());
        record.put("endCredit", row.getEndCredit());
        return record;
    }

    private Page sequence(Map<String, Object> values, String companyId, int page, int size) {
        YearMonth[] period = requirePeriod(values);
        validateAccountSetPeriod(companyId, period);
        boolean include = includeUnposted(values);
        String filter = "company_id=? AND ((iyear>? OR (iyear=? AND iperiod>=?))"
                + " AND (iyear<? OR (iyear=? AND iperiod<=?)))"
                + " AND COALESCE(void_flag,0)=0 AND voided_at IS NULL"
                + " AND COALESCE(iflag,0)=0 AND COALESCE(bdelete,0)=0"
                + (include ? "" : " AND (ibook=1 OR posted_at IS NOT NULL)");
        Object[] args = {companyId, period[0].getYear(), period[0].getYear(), period[0].getMonthValue(),
                period[1].getYear(), period[1].getYear(), period[1].getMonthValue()};
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM gl_accvouch WHERE " + filter, Long.class, args);
        Object[] pageArgs = java.util.Arrays.copyOf(args, args.length + 2);
        pageArgs[args.length] = size;
        pageArgs[args.length + 1] = (page - 1) * size;
        List<Map<String, Object>> records = jdbc.queryForList("SELECT " + ENTRY_COLUMNS
                + " FROM gl_accvouch WHERE " + filter
                + " ORDER BY iyear,iperiod,dbill_date,ino_id,inid,i_id LIMIT ? OFFSET ?", pageArgs);
        return new Page(total, page, size, records);
    }

    private void validateAccountSetPeriod(String companyId, YearMonth[] period) {
        List<Map<String, Object>> accountSets = jdbc.queryForList("""
                SELECT enabled_year,enabled_period FROM fin_account_set
                WHERE company_id=? AND status=?
                """, companyId, ACCOUNT_SET_STATUS_ACTIVE);
        if (accountSets.size() != 1 || accountSets.get(0).get("enabled_year") == null
                || accountSets.get(0).get("enabled_period") == null)
            throw new IllegalArgumentException("当前公司尚未启用账套");
        YearMonth start = YearMonth.of(((Number) accountSets.get(0).get("enabled_year")).intValue(),
                ((Number) accountSets.get(0).get("enabled_period")).intValue());
        List<Map<String, Object>> closed = jdbc.queryForList("""
                SELECT iyear,iperiod FROM gl_period_close
                WHERE company_id=? AND status='CLOSED'
                ORDER BY iyperiod DESC LIMIT 1
                """, companyId);
        YearMonth end = closed.isEmpty() ? start : YearMonth.of(
                ((Number) closed.get(0).get("iyear")).intValue(),
                ((Number) closed.get(0).get("iperiod")).intValue()).plusMonths(1);
        if (start.isAfter(end) || period[0].isBefore(start) || period[1].isAfter(end))
            throw new IllegalArgumentException("会计期间超出账套可用范围");
    }

    private YearMonth[] requirePeriod(Map<String, Object> values) {
        Integer fromYear = number(values, "yearFrom", null);
        Integer fromMonth = number(values, "periodFrom", null);
        Integer toYear = number(values, "yearTo", null);
        Integer toMonth = number(values, "periodTo", null);
        if (fromYear == null || toYear == null || fromMonth == null || toMonth == null
                || fromYear < 1990 || toYear > 2090 || fromMonth < 1 || fromMonth > 12
                || toMonth < 1 || toMonth > 12)
            throw new IllegalArgumentException("会计期间范围不完整或无效");
        YearMonth from = YearMonth.of(fromYear, fromMonth);
        YearMonth to = YearMonth.of(toYear, toMonth);
        if (from.isAfter(to)) throw new IllegalArgumentException("起始期间不能晚于结束期间");
        return new YearMonth[]{from, to};
    }

    private boolean includeUnposted(Map<String, Object> values) {
        String status = required(values, "voucherStatus");
        if (!"POSTED_ONLY".equals(status) && !"INCLUDE_SAVED_UNPOSTED".equals(status))
            throw new IllegalArgumentException("请选择凭证范围");
        return "INCLUDE_SAVED_UNPOSTED".equals(status);
    }

    private String required(Map<String, Object> values, String key) {
        String value = optional(values, key);
        if (value == null) throw new IllegalArgumentException(key + " 不能为空");
        return value;
    }
    private String optional(Map<String, Object> values, String key) {
        Object raw = values == null ? null : values.get(key);
        return raw == null || String.valueOf(raw).isBlank() ? null : String.valueOf(raw).trim();
    }
    private Integer number(Map<String, Object> values, String key, Integer defaultValue) {
        String raw = optional(values, key);
        if (raw == null) return defaultValue;
        try { return Integer.valueOf(raw); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException(key + " 必须是整数"); }
    }
}
