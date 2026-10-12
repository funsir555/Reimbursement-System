package com.finex.auth.service.impl.financequery;

import com.finex.auth.dto.FinanceDetailLedgerRowVO;
import com.finex.auth.dto.FinanceLedgerReportQueryDTO;
import com.finex.auth.service.FinanceLedgerReportService;
import com.finex.auth.service.port.FinanceAgentQueryPort;
import com.finex.auth.service.queryauth.QueryAuthorization;
import com.finex.auth.service.queryauth.QueryAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Finance-owned account and supplier resolution for the controlled supplier ledger. */
@Service
@RequiredArgsConstructor
public class FinanceAgentSupplierLedgerReadService {
    private static final String PERMISSION = "finance:general_ledger:supplier_detail_ledger:view";
    private final JdbcTemplate jdbc;
    private final FinanceLedgerReportService reports;

    public List<FinanceAgentQueryPort.AccountSet> accountSets(QueryAuthorization authorization) {
        if (authorization == null || authorization.isEmpty())
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
        List<String> companies = authorization.companyIds().stream().sorted().toList();
        String placeholders = String.join(",", java.util.Collections.nCopies(companies.size(), "?"));
        return jdbc.query("""
                SELECT a.company_id,c.company_name,a.enabled_year,a.enabled_period,
                  p.iyear AS closed_year,p.iperiod AS closed_period
                FROM fin_account_set a JOIN sys_company c ON c.company_id=a.company_id
                LEFT JOIN gl_period_close p ON p.company_id=a.company_id AND p.status='CLOSED'
                  AND p.iyperiod=(SELECT MAX(x.iyperiod) FROM gl_period_close x
                    WHERE x.company_id=a.company_id AND x.status='CLOSED')
                WHERE a.status='ACTIVE' AND c.status=1 AND a.company_id IN (""" + placeholders + ")" + """
                ORDER BY c.company_name,a.company_id
                """, (rs, index) -> {
            if (rs.getObject("enabled_year") == null || rs.getObject("enabled_period") == null) return null;
            YearMonth start = YearMonth.of(rs.getInt("enabled_year"), rs.getInt("enabled_period"));
            YearMonth end = rs.getObject("closed_year") == null ? start
                    : YearMonth.of(rs.getInt("closed_year"), rs.getInt("closed_period")).plusMonths(1);
            return new FinanceAgentQueryPort.AccountSet(rs.getString("company_id"),
                    rs.getString("company_name"), start.getYear(), start.getMonthValue(),
                    end.getYear(), end.getMonthValue());
        }, companies.toArray()).stream()
                .filter(item -> item != null && authorization.allows(item.companyId(), null)).toList();
    }

    public List<FinanceAgentQueryPort.Supplier> suppliers(QueryAuthorization authorization,
                                                          String companyId, String name) {
        requireCompany(authorization, companyId);
        if (name == null || name.isBlank() || name.length() > 128) return List.of();
        String needle = name.trim();
        List<FinanceAgentQueryPort.Supplier> exact = jdbc.query("""
                SELECT cVenCode,cVenName FROM gl_Vender
                WHERE company_id=? AND (cVenName=? OR cVenCode=? OR cVenAbbName=?)
                ORDER BY cVenName,cVenCode LIMIT 21
                """, (rs, index) -> new FinanceAgentQueryPort.Supplier(
                rs.getString("cVenCode"), rs.getString("cVenName")),
                companyId, needle, needle, needle);
        if (!exact.isEmpty()) return exact;
        return jdbc.query("""
                SELECT cVenCode,cVenName FROM gl_Vender
                WHERE company_id=? AND (LOCATE(?,cVenName)>0 OR LOCATE(?,cVenAbbName)>0)
                ORDER BY cVenName,cVenCode LIMIT 21
                """, (rs, index) -> new FinanceAgentQueryPort.Supplier(
                rs.getString("cVenCode"), rs.getString("cVenName")),
                companyId, needle, needle);
    }

    public FinanceAgentQueryPort.Page query(QueryAuthorization authorization, Map<String, Object> values,
                                           int page, int size) {
        String companyId = text(values.get("companyId"));
        requireCompany(authorization, companyId);
        String supplierCode = text(values.get("supplierCode"));
        if (supplierCode.isBlank() || suppliersByCode(companyId, supplierCode) != 1)
            throw new IllegalArgumentException("供应商不存在于所选账套");
        String status = text(values.get("voucherStatus"));
        if (!"POSTED_ONLY".equals(status) && !"INCLUDE_SAVED_UNPOSTED".equals(status))
            throw new IllegalArgumentException("请选择凭证范围");
        FinanceLedgerReportQueryDTO dto = new FinanceLedgerReportQueryDTO();
        dto.setCompanyId(companyId);
        dto.setLedgerKind("SUPPLIER");
        dto.setCsupId(supplierCode);
        dto.setIyearFrom(integer(values, "yearFrom"));
        dto.setIperiodFrom(integer(values, "periodFrom"));
        dto.setIyearTo(integer(values, "yearTo"));
        dto.setIperiodTo(integer(values, "periodTo"));
        dto.setIncludeUnposted("INCLUDE_SAVED_UNPOSTED".equals(status));
        dto.setPage(page);
        dto.setPageSize(size);
        var result = reports.queryDetailLedger(authorization.requesterUserId(), dto);
        return new FinanceAgentQueryPort.Page(result.getTotal(), page, size,
                result.getItems().stream().map(this::record).toList());
    }

    private Map<String, Object> record(FinanceDetailLedgerRowVO row) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("rowType", row.getRowType());
        value.put("subjectCode", row.getSubjectCode());
        value.put("subjectName", row.getSubjectName());
        value.put("supplierName", row.getAssistLabel());
        value.put("billDate", row.getDbillDate());
        value.put("voucherNo", row.getVoucherNo());
        value.put("summary", row.getSummary());
        value.put("debit", row.getDebit());
        value.put("credit", row.getCredit());
        value.put("balance", row.getBalance());
        value.put("balanceDirection", row.getBalanceDirection());
        return value;
    }

    private int suppliersByCode(String companyId, String code) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM gl_Vender WHERE company_id=? AND cVenCode=?",
                Integer.class, companyId, code);
        return count == null ? 0 : count;
    }

    private int integer(Map<String, Object> values, String key) {
        try { return Integer.parseInt(text(values.get(key))); }
        catch (NumberFormatException exception) { throw new IllegalArgumentException("会计期间范围不完整或无效"); }
    }

    private String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }

    private void requirePermission(QueryAuthorization authorization) {
        if (authorization == null || !PERMISSION.equals(authorization.permissionCode()))
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
    }

    private void requireCompany(QueryAuthorization authorization, String companyId) {
        requirePermission(authorization);
        if (!authorization.allows(companyId, null))
            throw new SecurityException(QueryAuthorizationService.NO_PERMISSION);
    }
}
