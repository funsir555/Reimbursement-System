package com.finex.auth.service.impl.voucher;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.finex.auth.dto.FinanceBalanceSheetRowVO;
import com.finex.auth.dto.FinanceContextCompanyOptionVO;
import com.finex.auth.dto.FinanceContextMetaVO;
import com.finex.auth.dto.FinanceDetailLedgerRowVO;
import com.finex.auth.dto.FinanceGeneralLedgerSectionVO;
import com.finex.auth.dto.FinanceLedgerReportMetaVO;
import com.finex.auth.dto.FinanceLedgerReportPageVO;
import com.finex.auth.dto.FinanceLedgerReportQueryDTO;
import com.finex.auth.dto.FinanceSequenceLedgerRowVO;
import com.finex.auth.dto.FinanceVoucherOptionVO;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.FinanceCustomer;
import com.finex.auth.entity.FinanceProjectArchive;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.GlAccass;
import com.finex.auth.entity.GlAccsum;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinanceCashFlowItemMapper;
import com.finex.auth.mapper.FinanceCustomerMapper;
import com.finex.auth.mapper.FinancePeriodCloseMapper;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.GlAccassMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.support.FinanceBalanceDirectionSupport;
import com.finex.auth.support.FinanceModuleEnableSupport;
import com.finex.auth.support.FinanceVoucherAmountSupport;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

public final class LedgerReportDomainSupport extends AbstractFinanceVoucherSupport {

    private static final String LEDGER_KIND_DETAIL = "DETAIL";
    private static final String LEDGER_KIND_PROJECT = "PROJECT";
    private static final String LEDGER_KIND_SUPPLIER = "SUPPLIER";
    private static final String LEDGER_KIND_CUSTOMER = "CUSTOMER";
    private static final String LEDGER_KIND_PERSONAL = "PERSONAL";
    private static final String LEDGER_KIND_QUANTITY_AMOUNT = "QUANTITY_AMOUNT";

    private final GlAccvouchMapper glAccvouchMapper;
    private final GlAccsumMapper glAccsumMapper;
    private final GlAccassMapper glAccassMapper;
    private final VoucherContextSupport voucherContextSupport;

    public LedgerReportDomainSupport(
            GlAccvouchMapper glAccvouchMapper,
            GlAccsumMapper glAccsumMapper,
            GlAccassMapper glAccassMapper,
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            FinanceCashFlowItemMapper financeCashFlowItemMapper,
            FinanceCustomerMapper financeCustomerMapper,
            FinanceVendorMapper financeVendorMapper,
            FinanceProjectClassMapper financeProjectClassMapper,
            FinanceProjectArchiveMapper financeProjectArchiveMapper,
            SystemCompanyMapper systemCompanyMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper,
            FinancePeriodCloseMapper financePeriodCloseMapper,
            FinanceModuleEnableSupport financeModuleEnableSupport,
            VoucherContextSupport voucherContextSupport
    ) {
        super(
                glAccvouchMapper,
                financeAccountSubjectMapper,
                financeCashFlowItemMapper,
                financeCustomerMapper,
                financeVendorMapper,
                financeProjectClassMapper,
                financeProjectArchiveMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                userMapper,
                financePeriodCloseMapper,
                financeModuleEnableSupport
        );
        this.glAccvouchMapper = glAccvouchMapper;
        this.glAccsumMapper = glAccsumMapper;
        this.glAccassMapper = glAccassMapper;
        this.voucherContextSupport = voucherContextSupport;
    }

    public FinanceLedgerReportMetaVO getMeta(Long currentUserId, String companyId, Integer iyear, Integer iperiod) {
        FinanceContextMetaVO contextMeta = voucherContextSupport.getMeta(currentUserId);
        String effectiveCompanyId = trimToNull(companyId);
        if (effectiveCompanyId == null) {
            effectiveCompanyId = trimToNull(contextMeta.getDefaultCompanyId());
        }
        if (effectiveCompanyId == null && !contextMeta.getCompanyOptions().isEmpty()) {
            effectiveCompanyId = trimToNull(contextMeta.getCompanyOptions().get(0).getCompanyId());
        }
        if (effectiveCompanyId == null) {
            throw new IllegalStateException("当前没有可用的财务公司");
        }
        String resolvedCompanyId = effectiveCompanyId;
        validateCompany(resolvedCompanyId);
        requireGeneralLedgerEnabled(resolvedCompanyId);

        FinanceContextCompanyOptionVO companyOption = contextMeta.getCompanyOptions().stream()
                .filter(item -> Objects.equals(item.getCompanyId(), resolvedCompanyId))
                .findFirst()
                .orElse(null);
        Integer effectiveYear = iyear != null
                ? iyear
                : (companyOption == null ? null : firstNonNull(companyOption.getPeriodEndYear(), companyOption.getPeriodStartYear(), companyOption.getEnabledYear()));
        Integer effectivePeriod = iperiod != null
                ? iperiod
                : (companyOption == null ? null : firstNonNull(companyOption.getPeriodEndMonth(), companyOption.getPeriodStartMonth(), companyOption.getEnabledPeriod()));
        if (effectiveYear == null || effectivePeriod == null) {
            throw new IllegalStateException("当前公司尚未启用账套");
        }

        FinanceLedgerReportMetaVO meta = new FinanceLedgerReportMetaVO();
        meta.setCompanyOptions(contextMeta.getCompanyOptions());
        meta.setDepartmentOptions(loadEnabledDepartments().stream().map(this::toDepartmentOption).toList());
        meta.setEmployeeOptions(loadEnabledUsers().stream().map(this::toEmployeeOption).toList());
        meta.setMakerOptions(loadMakerOptions(resolvedCompanyId, effectiveYear, effectivePeriod));
        meta.setVoucherTypeOptions(toOptions(VOUCHER_TYPE_SEEDS));
        meta.setAccountOptions(loadAccountOptions(resolvedCompanyId));
        meta.setCustomerOptions(loadCustomerOptions(resolvedCompanyId));
        meta.setSupplierOptions(loadSupplierOptions(resolvedCompanyId));
        meta.setProjectClassOptions(loadProjectClassOptions(resolvedCompanyId));
        meta.setProjectOptions(loadProjectOptions(resolvedCompanyId));
        meta.setDefaultCompanyId(resolvedCompanyId);
        meta.setDefaultYear(effectiveYear);
        meta.setDefaultPeriod(effectivePeriod);
        meta.setDefaultYearPeriod(buildYearPeriod(effectiveYear, effectivePeriod));
        if (companyOption != null) {
            meta.setPeriodStartYear(companyOption.getPeriodStartYear());
            meta.setPeriodStartMonth(companyOption.getPeriodStartMonth());
            meta.setPeriodEndYear(companyOption.getPeriodEndYear());
            meta.setPeriodEndMonth(companyOption.getPeriodEndMonth());
        }
        return meta;
    }

    public FinanceLedgerReportPageVO<FinanceBalanceSheetRowVO> queryBalanceSheet(FinanceLedgerReportQueryDTO dto) {
        QueryContext context = normalizeQuery(dto, false, true);
        return buildLedgerPage(loadBalanceSheetRows(context), context.page(), context.pageSize());
    }

    public FinanceLedgerReportPageVO<FinanceGeneralLedgerSectionVO> queryGeneralLedger(FinanceLedgerReportQueryDTO dto) {
        QueryContext context = normalizeQuery(dto, false, false);
        return buildLedgerPage(loadGeneralLedgerSections(context), context.page(), context.pageSize());
    }

    public FinanceLedgerReportPageVO<FinanceDetailLedgerRowVO> queryDetailLedger(FinanceLedgerReportQueryDTO dto) {
        QueryContext context = normalizeQuery(dto, true, false);
        return buildLedgerPage(loadDetailLedgerRows(context), context.page(), context.pageSize());
    }

    public FinanceLedgerReportPageVO<FinanceSequenceLedgerRowVO> querySequenceLedger(FinanceLedgerReportQueryDTO dto) {
        QueryContext context = normalizeQuery(dto, false, false);
        return buildLedgerPage(loadSequenceLedgerRows(context), context.page(), context.pageSize());
    }

    public byte[] exportBalanceSheet(FinanceLedgerReportQueryDTO dto) {
        QueryContext context = normalizeQuery(dto, false, true);
        return exportBalanceWorkbook(context, loadBalanceSheetRows(context));
    }

    public byte[] exportGeneralLedger(FinanceLedgerReportQueryDTO dto) {
        QueryContext context = normalizeQuery(dto, false, false);
        return exportGeneralLedgerWorkbook(context, loadGeneralLedgerSections(context));
    }

    public byte[] exportDetailLedger(FinanceLedgerReportQueryDTO dto) {
        QueryContext context = normalizeQuery(dto, true, false);
        return exportDetailLedgerWorkbook(context, loadDetailLedgerRows(context));
    }

    public byte[] exportSequenceLedger(FinanceLedgerReportQueryDTO dto) {
        QueryContext context = normalizeQuery(dto, false, false);
        return exportSequenceLedgerWorkbook(context, loadSequenceLedgerRows(context));
    }

    private QueryContext normalizeQuery(FinanceLedgerReportQueryDTO dto, boolean detailLedger, boolean balanceSheet) {
        FinanceLedgerReportQueryDTO source = dto == null ? new FinanceLedgerReportQueryDTO() : dto;
        String companyId = trimToNull(source.getCompanyId());
        if (companyId == null) {
            throw new IllegalArgumentException("公司不能为空");
        }
        validateCompany(companyId);
        requireGeneralLedgerEnabled(companyId);
        PeriodRange periodRange = balanceSheet
                ? normalizeBalancePeriodRange(source, companyId)
                : normalizeSinglePeriod(source);
        String ledgerKind = detailLedger ? normalizeLedgerKind(source.getLedgerKind()) : null;
        if (Objects.equals(ledgerKind, LEDGER_KIND_PROJECT)
                && trimToNull(source.getCitemClass()) == null
                && trimToNull(source.getCitemId()) == null) {
            throw new IllegalArgumentException("项目明细账至少需要选择项目分类或项目");
        }
        boolean includeUnposted = Boolean.TRUE.equals(source.getIncludeUnposted());
        int page = source.getPage() == null || source.getPage() < 1 ? DEFAULT_PAGE : source.getPage();
        int pageSize = source.getPageSize() == null || source.getPageSize() < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(source.getPageSize(), 500);
        return new QueryContext(
                companyId,
                periodRange.to().getYear(),
                periodRange.to().getMonthValue(),
                periodRange.from().getYear(),
                periodRange.from().getMonthValue(),
                periodRange.to().getYear(),
                periodRange.to().getMonthValue(),
                balanceSheet,
                ledgerKind,
                trimToNull(source.getAccountCodeFrom()),
                trimToNull(source.getAccountCodeTo()),
                trimToNull(source.getCdeptId()),
                trimToNull(source.getCpersonId()),
                trimToNull(source.getCcusId()),
                trimToNull(source.getCsupId()),
                trimToNull(source.getCitemClass()),
                trimToNull(source.getCitemId()),
                normalizeBalanceAssistDisplay(source.getBalanceAssistDisplay()),
                normalizeSubjectLevelRange(source.getSubjectLevelRange()),
                trimToNull(source.getVoucherNo()),
                trimToNull(source.getCsign()),
                trimToNull(source.getSummary()),
                trimToNull(source.getCbill()),
                includeUnposted,
                page,
                pageSize
        );
    }

    private PeriodRange normalizeSinglePeriod(FinanceLedgerReportQueryDTO source) {
        if (source.getIyear() == null) {
            throw new IllegalArgumentException("会计年度不能为空");
        }
        if (source.getIperiod() == null || source.getIperiod() < 1 || source.getIperiod() > 12) {
            throw new IllegalArgumentException("会计月份不合法");
        }
        YearMonth period = YearMonth.of(source.getIyear(), source.getIperiod());
        return new PeriodRange(period, period);
    }

    private PeriodRange normalizeBalancePeriodRange(FinanceLedgerReportQueryDTO source, String companyId) {
        boolean hasRangeValue = source.getIyearFrom() != null
                || source.getIperiodFrom() != null
                || source.getIyearTo() != null
                || source.getIperiodTo() != null;
        YearMonth from;
        YearMonth to;
        if (!hasRangeValue) {
            if (source.getIyear() == null) {
                throw new IllegalArgumentException("会计年度不能为空");
            }
            validateBalanceYear(source.getIyear());
            YearMonth legacyPeriod = normalizeSinglePeriod(source).from();
            from = legacyPeriod;
            to = legacyPeriod;
        } else {
            if (source.getIyearFrom() == null
                    || source.getIperiodFrom() == null
                    || source.getIyearTo() == null
                    || source.getIperiodTo() == null) {
                throw new IllegalArgumentException("余额表期间范围不完整");
            }
            validateBalanceYear(source.getIyearFrom());
            validateBalanceYear(source.getIyearTo());
            validateBalanceMonth(source.getIperiodFrom());
            validateBalanceMonth(source.getIperiodTo());
            from = YearMonth.of(source.getIyearFrom(), source.getIperiodFrom());
            to = YearMonth.of(source.getIyearTo(), source.getIperiodTo());
        }
        validateBalanceYear(from.getYear());
        validateBalanceYear(to.getYear());
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("余额表期间起不能晚于期间止");
        }

        FinanceContextMetaVO contextMeta = voucherContextSupport.getMeta(null);
        FinanceContextCompanyOptionVO companyOption = contextMeta.getCompanyOptions().stream()
                .filter(item -> Objects.equals(item.getCompanyId(), companyId))
                .findFirst()
                .orElse(null);
        if (companyOption == null
                || companyOption.getPeriodStartYear() == null
                || companyOption.getPeriodStartMonth() == null
                || companyOption.getPeriodEndYear() == null
                || companyOption.getPeriodEndMonth() == null) {
            throw new IllegalArgumentException("当前公司尚未启用账套");
        }
        YearMonth accountSetStart = YearMonth.of(
                companyOption.getPeriodStartYear(),
                companyOption.getPeriodStartMonth()
        );
        YearMonth accountSetEnd = YearMonth.of(
                companyOption.getPeriodEndYear(),
                companyOption.getPeriodEndMonth()
        );
        if (accountSetStart.isAfter(accountSetEnd)) {
            throw new IllegalArgumentException("当前账套期间范围不合法");
        }
        if (from.isBefore(accountSetStart)) {
            throw new IllegalArgumentException("期间起早于账套启用期间");
        }
        if (to.isAfter(accountSetEnd)) {
            throw new IllegalArgumentException("期间止超过账套当前可用期间");
        }
        return new PeriodRange(from, to);
    }

    private void validateBalanceYear(Integer year) {
        if (year == null || year < 1990 || year > 2090) {
            throw new IllegalArgumentException("余额表期间年份不合法");
        }
    }

    private void validateBalanceMonth(Integer month) {
        if (month == null || month < 1 || month > 12) {
            throw new IllegalArgumentException("余额表期间月份不合法");
        }
    }

    private String normalizeLedgerKind(String ledgerKind) {
        String normalized = trimToNull(ledgerKind);
        if (normalized == null) {
            return LEDGER_KIND_DETAIL;
        }
        String upper = normalized.toUpperCase();
        if (Set.of(
                LEDGER_KIND_DETAIL,
                LEDGER_KIND_PROJECT,
                LEDGER_KIND_SUPPLIER,
                LEDGER_KIND_CUSTOMER,
                LEDGER_KIND_PERSONAL,
                LEDGER_KIND_QUANTITY_AMOUNT
        ).contains(upper)) {
            return upper;
        }
        throw new IllegalArgumentException("账簿类型不合法");
    }

    private String normalizeBalanceAssistDisplay(String display) {
        String normalized = trimToNull(display);
        if (normalized == null) {
            return null;
        }
        String upper = normalized.toUpperCase();
        if (Set.of("NAME", "CODE", "CODE_NAME").contains(upper)) {
            return upper;
        }
        throw new IllegalArgumentException("辅助项展示方案不合法");
    }

    private String normalizeSubjectLevelRange(String range) {
        String normalized = trimToNull(range);
        if (normalized == null) {
            return null;
        }
        if (!normalized.matches("1-(?:[1-9]\\d*|n)")) {
            throw new IllegalArgumentException("科目级次不合法");
        }
        return normalized;
    }

    private List<FinanceBalanceSheetRowVO> loadBalanceSheetRows(QueryContext context) {
        Map<String, FinanceAccountSubject> subjectMap = loadSelectableAccountMap(context.companyId());
        Map<String, BalanceAccumulator> accumulators = new LinkedHashMap<>();
        LambdaQueryWrapper<GlAccsum> balanceQuery = Wrappers.<GlAccsum>lambdaQuery()
                .eq(GlAccsum::getCompanyId, context.companyId());
        applyPeriodRange(balanceQuery, context, GlAccsum::getIyear, GlAccsum::getIperiod);
        List<GlAccsum> balances = glAccsumMapper.selectList(balanceQuery);
        balances.stream()
                .filter(item -> matchesSubjectRange(item.getCcode(), context))
                .filter(item -> matchesSubjectLevel(item.getCcode(), subjectMap.get(item.getCcode()), context))
                .forEach(item -> {
                    FinanceAccountSubject subject = subjectMap.get(item.getCcode());
                    if (subject == null) {
                        return;
                    }
                    BalanceAccumulator accumulator = accumulators.computeIfAbsent(
                            subject.getSubjectCode(),
                            unused -> new BalanceAccumulator(subject)
                    );
                    if (isPeriod(item.getIyear(), item.getIperiod(), context.iyearFrom(), context.iperiodFrom())) {
                        accumulator.addOpeningBase(item.getMb());
                    }
                    accumulator.addPeriodBase(item.getMd(), item.getMc());
                    if (isPeriod(item.getIyear(), item.getIperiod(), context.iyearTo(), context.iperiodTo())) {
                        accumulator.addEndingBase(item.getMe());
                    }
                });

        Map<BalanceAssistKey, BalanceAccumulator> assistAccumulators = new LinkedHashMap<>();
        if (context.balanceAssistDisplay() != null) {
            LambdaQueryWrapper<GlAccass> assistBalanceQuery = Wrappers.<GlAccass>lambdaQuery()
                    .eq(GlAccass::getCompanyId, context.companyId());
            applyPeriodRange(assistBalanceQuery, context, GlAccass::getIyear, GlAccass::getIperiod);
            List<GlAccass> assistBalances = glAccassMapper.selectList(assistBalanceQuery);
            assistBalances.stream()
                    .filter(item -> matchesSubjectRange(item.getCcode(), context))
                    .filter(item -> matchesSubjectLevel(item.getCcode(), subjectMap.get(item.getCcode()), context))
                    .filter(this::hasAssistValues)
                    .forEach(item -> {
                        FinanceAccountSubject subject = subjectMap.get(item.getCcode());
                        if (subject == null) {
                            return;
                        }
                        BalanceAssistKey key = BalanceAssistKey.from(item);
                        BalanceAccumulator accumulator = assistAccumulators.computeIfAbsent(
                                key,
                                unused -> new BalanceAccumulator(subject)
                        );
                        if (isPeriod(item.getIyear(), item.getIperiod(), context.iyearFrom(), context.iperiodFrom())) {
                            accumulator.addOpeningBase(item.getMb());
                        }
                        accumulator.addPeriodBase(item.getMd(), item.getMc());
                        if (isPeriod(item.getIyear(), item.getIperiod(), context.iyearTo(), context.iperiodTo())) {
                            accumulator.addEndingBase(item.getMe());
                        }
                    });
        }

        if (context.includeUnposted()) {
            loadVoucherRowsForAugment(context, subjectMap).forEach(row -> {
                FinanceAccountSubject subject = subjectMap.get(row.getCcode());
                if (subject == null) {
                    return;
                }
                accumulators.computeIfAbsent(subject.getSubjectCode(), unused -> new BalanceAccumulator(subject))
                        .addUnposted(row);
                if (context.balanceAssistDisplay() != null && hasAssistValues(row)) {
                    BalanceAssistKey key = BalanceAssistKey.from(row);
                    assistAccumulators.computeIfAbsent(key, unused -> new BalanceAccumulator(subject))
                            .addUnposted(row);
                }
            });
        }

        List<FinanceBalanceSheetRowVO> subjectRows = accumulators.values().stream()
                .filter(item -> matchesSubjectLevel(item.subject().getSubjectCode(), item.subject(), context))
                .sorted(Comparator.comparing(item -> item.subject().getSubjectCode()))
                .map(this::toBalanceRow)
                .filter(this::hasBalanceContent)
                .toList();
        Map<String, List<FinanceBalanceSheetRowVO>> assistRowsBySubject = buildBalanceAssistRows(
                assistAccumulators,
                context
        );
        Map<String, FinanceBalanceSheetRowVO> categoryTotals = buildBalanceCategoryTotals(subjectRows).stream()
                .collect(Collectors.toMap(
                        row -> resolveBalanceCategory(row.getSubjectCategory()),
                        row -> row,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<FinanceBalanceSheetRowVO> result = new ArrayList<>();
        for (String category : List.of("ASSET", "LIABILITY", "EQUITY", "COST", "PROFIT")) {
            subjectRows.stream()
                    .filter(row -> Objects.equals(category, resolveBalanceCategory(row.getSubjectCategory())))
                    .forEach(subjectRow -> {
                        result.add(subjectRow);
                        result.addAll(assistRowsBySubject.getOrDefault(subjectRow.getSubjectCode(), List.of()));
                    });
            FinanceBalanceSheetRowVO totalRow = categoryTotals.get(category);
            if (totalRow != null) {
                result.add(totalRow);
            }
        }
        return result;
    }

    private Map<String, List<FinanceBalanceSheetRowVO>> buildBalanceAssistRows(
            Map<BalanceAssistKey, BalanceAccumulator> accumulators,
            QueryContext context
    ) {
        if (accumulators.isEmpty()) {
            return Map.of();
        }
        BalanceAssistLabels labels = loadBalanceAssistLabels(context.companyId());
        return accumulators.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    FinanceBalanceSheetRowVO row = toBalanceRow(entry.getValue());
                    row.setRowType("ASSIST");
                    row.setSubjectCode(entry.getKey().subjectCode());
                    row.setSubjectName(labels.format(entry.getKey(), context.balanceAssistDisplay()));
                    row.setSubjectLevel(null);
                    return row;
                })
                .filter(this::hasBalanceContent)
                .collect(Collectors.groupingBy(
                        FinanceBalanceSheetRowVO::getSubjectCode,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private List<FinanceBalanceSheetRowVO> buildBalanceCategoryTotals(List<FinanceBalanceSheetRowVO> subjectRows) {
        Map<String, BalanceTotalAccumulator> totals = new LinkedHashMap<>();
        for (FinanceBalanceSheetRowVO row : subjectRows) {
            if (!isTopLevelBalanceSubject(row)) {
                continue;
            }
            String category = resolveBalanceCategory(row.getSubjectCategory());
            BalanceTotalAccumulator total = totals.computeIfAbsent(category, unused -> new BalanceTotalAccumulator());
            total.addRow(row);
        }
        return List.of(
                toCategoryTotalRow("ASSET", "资产合计", totals.get("ASSET")),
                toCategoryTotalRow("LIABILITY", "负债合计", totals.get("LIABILITY")),
                toCategoryTotalRow("EQUITY", "权益合计", totals.get("EQUITY")),
                toCategoryTotalRow("COST", "成本合计", totals.get("COST")),
                toCategoryTotalRow("PROFIT", "损益合计", totals.get("PROFIT"))
        );
    }

    private boolean isTopLevelBalanceSubject(FinanceBalanceSheetRowVO row) {
        return row != null
                && "SUBJECT".equals(row.getRowType())
                && Objects.equals(row.getSubjectLevel(), 1);
    }

    private FinanceBalanceSheetRowVO toCategoryTotalRow(
            String category,
            String label,
            BalanceTotalAccumulator accumulator
    ) {
        FinanceBalanceSheetRowVO row = new FinanceBalanceSheetRowVO();
        row.setRowType("CATEGORY_TOTAL");
        row.setSubjectName(label);
        row.setSubjectCategory(category);
        if (accumulator != null) {
            row.setBeginDebit(accumulator.beginDebit());
            row.setBeginCredit(accumulator.beginCredit());
            row.setPeriodDebit(normalizeMoney(accumulator.periodDebit()));
            row.setPeriodCredit(normalizeMoney(accumulator.periodCredit()));
            row.setEndDebit(accumulator.endDebit());
            row.setEndCredit(accumulator.endCredit());
        } else {
            row.setBeginDebit(ZERO);
            row.setBeginCredit(ZERO);
            row.setPeriodDebit(ZERO);
            row.setPeriodCredit(ZERO);
            row.setEndDebit(ZERO);
            row.setEndCredit(ZERO);
        }
        return row;
    }

    private String resolveBalanceCategory(String category) {
        String normalized = trimToNull(category);
        return normalized == null ? "ASSET" : normalized;
    }

    private String resolveBalanceCategory(FinanceAccountSubject subject) {
        if (subject == null) {
            return "ASSET";
        }
        String category = trimToNull(subject.getSubjectCategory());
        if (category != null) {
            return category;
        }
        String code = trimToNull(subject.getSubjectCode());
        if (code == null) {
            return "ASSET";
        }
        return switch (code.charAt(0)) {
            case '1' -> "ASSET";
            case '2' -> "LIABILITY";
            case '3' -> "EQUITY";
            case '4' -> "COST";
            case '5', '6' -> "PROFIT";
            default -> "ASSET";
        };
    }

    private boolean matchesSubjectLevel(String subjectCode, FinanceAccountSubject subject, QueryContext context) {
        String range = context.subjectLevelRange();
        if (range == null || subject == null) {
            return true;
        }
        if ("1-n".equals(range)) {
            return true;
        }
        int maxLevel = Integer.parseInt(range.substring(2));
        return subject.getSubjectLevel() != null && subject.getSubjectLevel() <= maxLevel;
    }

    private boolean hasAssistValues(GlAccass row) {
        return trimToNull(row.getCcusId()) != null
                || trimToNull(row.getCsupId()) != null
                || trimToNull(row.getCitemClass()) != null
                || trimToNull(row.getCitemId()) != null
                || trimToNull(row.getCdeptId()) != null
                || trimToNull(row.getCpersonId()) != null;
    }

    private boolean hasAssistValues(GlAccvouch row) {
        return trimToNull(row.getCcusId()) != null
                || trimToNull(row.getCsupId()) != null
                || trimToNull(row.getCitemClass()) != null
                || trimToNull(row.getCitemId()) != null
                || trimToNull(row.getCdeptId()) != null
                || trimToNull(row.getCpersonId()) != null;
    }

    private BalanceAssistLabels loadBalanceAssistLabels(String companyId) {
        Map<String, FinanceVoucherOptionVO> customers = loadEnabledCustomerMap(companyId).values().stream()
                .collect(Collectors.toMap(
                        FinanceCustomer::getCCusCode,
                        item -> option(item.getCCusCode(), item.getCCusCode(), resolveCustomerName(item)),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Map<String, FinanceVoucherOptionVO> suppliers = loadEnabledSupplierMap(companyId).values().stream()
                .collect(Collectors.toMap(
                        FinanceVendor::getCVenCode,
                        item -> option(item.getCVenCode(), item.getCVenCode(), resolveVendorName(item)),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Map<String, FinanceVoucherOptionVO> projects = loadProjectOptions(companyId).stream()
                .collect(Collectors.toMap(FinanceVoucherOptionVO::getValue, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, FinanceVoucherOptionVO> projectClasses = loadProjectClassOptions(companyId).stream()
                .collect(Collectors.toMap(FinanceVoucherOptionVO::getValue, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, FinanceVoucherOptionVO> departments = loadEnabledDepartments().stream()
                .collect(Collectors.toMap(
                        item -> String.valueOf(item.getId()),
                        this::toDepartmentOption,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Map<String, FinanceVoucherOptionVO> employees = loadEnabledUsers().stream()
                .collect(Collectors.toMap(
                        item -> String.valueOf(item.getId()),
                        this::toEmployeeOption,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        return new BalanceAssistLabels(customers, suppliers, projects, projectClasses, departments, employees);
    }

    private FinanceBalanceSheetRowVO toBalanceRow(BalanceAccumulator accumulator) {
        FinanceBalanceSheetRowVO row = new FinanceBalanceSheetRowVO();
        row.setRowType("SUBJECT");
        row.setSubjectCode(accumulator.subject().getSubjectCode());
        row.setSubjectName(accumulator.subject().getSubjectName());
        row.setSubjectLevel(accumulator.subject().getSubjectLevel());
        row.setSubjectCategory(resolveBalanceCategory(accumulator.subject()));
        BalanceDisplay opening = splitSignedAmount(accumulator.subject().getBalanceDirection(), accumulator.beginSigned());
        BalanceDisplay ending = splitSignedAmount(accumulator.subject().getBalanceDirection(), accumulator.endSigned());
        row.setBeginDebit(opening.debit());
        row.setBeginCredit(opening.credit());
        row.setPeriodDebit(normalizeMoney(accumulator.periodDebit()));
        row.setPeriodCredit(normalizeMoney(accumulator.periodCredit()));
        row.setEndDebit(ending.debit());
        row.setEndCredit(ending.credit());
        return row;
    }

    private boolean hasBalanceContent(FinanceBalanceSheetRowVO row) {
        return isNonZero(row.getBeginDebit())
                || isNonZero(row.getBeginCredit())
                || isNonZero(row.getPeriodDebit())
                || isNonZero(row.getPeriodCredit())
                || isNonZero(row.getEndDebit())
                || isNonZero(row.getEndCredit());
    }

    private List<FinanceGeneralLedgerSectionVO> loadGeneralLedgerSections(QueryContext context) {
        Map<String, FinanceAccountSubject> subjectMap = loadSelectableAccountMap(context.companyId());
        Map<String, GroupBalanceSnapshot> openingMap = loadGroupOpeningMap(context, subjectMap);
        Map<String, List<GlAccvouch>> groupedRows = groupDetailRows(loadVisibleDetailVoucherRows(context, subjectMap), context, subjectMap);
        LinkedHashMap<String, FinanceGeneralLedgerSectionVO> sections = new LinkedHashMap<>();
        for (Map.Entry<String, List<GlAccvouch>> entry : groupedRows.entrySet()) {
            String groupKey = entry.getKey();
            List<GlAccvouch> rows = entry.getValue();
            if (rows.isEmpty()) {
                continue;
            }
            FinanceAccountSubject subject = subjectMap.get(rows.get(0).getCcode());
            if (subject == null) {
                continue;
            }
            GroupBalanceSnapshot opening = openingMap.getOrDefault(groupKey, GroupBalanceSnapshot.empty(subject));
            FinanceGeneralLedgerSectionVO section = new FinanceGeneralLedgerSectionVO();
            section.setSubjectCode(subject.getSubjectCode());
            section.setSubjectName(subject.getSubjectName());
            List<FinanceDetailLedgerRowVO> detailRows = buildLedgerRows(groupKey, rows, subject, opening, context);
            section.setRows(detailRows);
            section.setRowCount(detailRows.size());
            BalanceDisplay begin = splitSignedAmount(subject.getBalanceDirection(), opening.amount());
            BalanceDisplay end = computeEndingDisplay(detailRows, subject);
            section.setBeginDebit(begin.debit());
            section.setBeginCredit(begin.credit());
            section.setTotalDebit(sumDetailColumn(detailRows, "TOTAL", true));
            section.setTotalCredit(sumDetailColumn(detailRows, "TOTAL", false));
            section.setEndDebit(end.debit());
            section.setEndCredit(end.credit());
            sections.put(groupKey, section);
        }
        if (sections.isEmpty()) {
            openingMap.forEach((groupKey, opening) -> {
                if (!isNonZero(opening.amount())) {
                    return;
                }
                FinanceAccountSubject subject = opening.subject();
                FinanceGeneralLedgerSectionVO section = new FinanceGeneralLedgerSectionVO();
                section.setSubjectCode(subject.getSubjectCode());
                section.setSubjectName(subject.getSubjectName());
                List<FinanceDetailLedgerRowVO> rows = buildLedgerRows(groupKey, List.of(), subject, opening, context);
                section.setRows(rows);
                section.setRowCount(rows.size());
                BalanceDisplay begin = splitSignedAmount(subject.getBalanceDirection(), opening.amount());
                section.setBeginDebit(begin.debit());
                section.setBeginCredit(begin.credit());
                section.setTotalDebit(ZERO);
                section.setTotalCredit(ZERO);
                section.setEndDebit(begin.debit());
                section.setEndCredit(begin.credit());
                sections.put(groupKey, section);
            });
        }
        return sections.values().stream()
                .sorted(Comparator.comparing(FinanceGeneralLedgerSectionVO::getSubjectCode, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private List<FinanceDetailLedgerRowVO> loadDetailLedgerRows(QueryContext context) {
        Map<String, FinanceAccountSubject> subjectMap = loadSelectableAccountMap(context.companyId());
        Map<String, GroupBalanceSnapshot> openingMap = loadGroupOpeningMap(context, subjectMap);
        Map<String, List<GlAccvouch>> groupedRows = groupDetailRows(loadVisibleDetailVoucherRows(context, subjectMap), context, subjectMap);
        List<FinanceDetailLedgerRowVO> result = new ArrayList<>();
        Set<String> seen = new java.util.LinkedHashSet<>();
        for (Map.Entry<String, List<GlAccvouch>> entry : groupedRows.entrySet()) {
            List<GlAccvouch> rows = entry.getValue();
            if (rows.isEmpty()) {
                continue;
            }
            FinanceAccountSubject subject = subjectMap.get(rows.get(0).getCcode());
            if (subject == null) {
                continue;
            }
            GroupBalanceSnapshot opening = openingMap.getOrDefault(entry.getKey(), GroupBalanceSnapshot.empty(subject));
            result.addAll(buildLedgerRows(entry.getKey(), rows, subject, opening, context));
            seen.add(entry.getKey());
        }
        openingMap.forEach((groupKey, opening) -> {
            if (seen.contains(groupKey) || !isNonZero(opening.amount())) {
                return;
            }
            result.addAll(buildLedgerRows(groupKey, List.of(), opening.subject(), opening, context));
        });
        return result;
    }

    private List<FinanceSequenceLedgerRowVO> loadSequenceLedgerRows(QueryContext context) {
        Map<String, FinanceAccountSubject> subjectMap = loadSelectableAccountMap(context.companyId());
        List<GlAccvouch> rows = loadVisibleDetailVoucherRows(context, subjectMap);
        Map<SequenceVoucherKey, List<GlAccvouch>> grouped = rows.stream()
                .collect(Collectors.groupingBy(
                        row -> new SequenceVoucherKey(row.getCompanyId(), row.getIyear(), row.getIyperiod(), row.getIperiod(), row.getCsign(), row.getInoId()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        String voucherKeyword = lowerText(context.voucherNoKeyword());
        String summaryKeyword = lowerText(context.summaryKeyword());
        String makerKeyword = lowerText(context.makerKeyword());
        return grouped.values().stream()
                .map(this::toSequenceRow)
                .filter(item -> voucherKeyword == null
                        || lowerText(item.getDisplayVoucherNo()).contains(voucherKeyword)
                        || lowerText(item.getVoucherNo()).contains(voucherKeyword))
                .filter(item -> summaryKeyword == null || lowerText(item.getSummary()).contains(summaryKeyword))
                .filter(item -> makerKeyword == null || lowerText(item.getCbill()).contains(makerKeyword))
                .sorted(Comparator
                        .comparing(FinanceSequenceLedgerRowVO::getDbillDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(FinanceSequenceLedgerRowVO::getInoId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private FinanceSequenceLedgerRowVO toSequenceRow(List<GlAccvouch> rows) {
        GlAccvouch header = rows.get(0);
        FinanceSequenceLedgerRowVO row = new FinanceSequenceLedgerRowVO();
        row.setVoucherNo(buildVoucherNo(header.getCompanyId(), header.getIyear(), header.getIperiod(), header.getCsign(), header.getInoId()));
        row.setDisplayVoucherNo(buildDisplayVoucherNo(header.getCsign(), header.getInoId()));
        row.setCompanyId(header.getCompanyId());
        row.setIyear(header.getIyear());
        row.setIyperiod(header.getIyperiod());
        row.setIperiod(header.getIperiod());
        row.setCsign(header.getCsign());
        row.setVoucherTypeLabel(resolveVoucherTypeLabel(header.getCsign()));
        row.setInoId(header.getInoId());
        row.setDbillDate(formatDate(header.getDbillDate()));
        row.setSummary(resolveVoucherSummary(rows));
        row.setCbill(normalize(header.getCbill(), ""));
        row.setIdoc(header.getIdoc());
        row.setStatus(resolveStatus(header));
        row.setStatusLabel(resolveStatusLabel(row.getStatus()));
        row.setTotalDebit(sumAmount(rows, true));
        row.setTotalCredit(sumAmount(rows, false));
        return row;
    }

    private Map<String, GroupBalanceSnapshot> loadGroupOpeningMap(QueryContext context, Map<String, FinanceAccountSubject> subjectMap) {
        LinkedHashMap<String, GroupBalanceSnapshot> result = new LinkedHashMap<>();
        if (hasAssistFilter(context) || isAssistLedger(context)) {
            List<GlAccass> balances = glAccassMapper.selectList(
                    Wrappers.<GlAccass>lambdaQuery()
                            .eq(GlAccass::getCompanyId, context.companyId())
                            .eq(GlAccass::getIyear, context.iyear())
                            .eq(GlAccass::getIperiod, context.iperiod())
            );
            for (GlAccass item : balances) {
                FinanceAccountSubject subject = subjectMap.get(item.getCcode());
                if (subject == null
                        || !matchesSubjectRange(item.getCcode(), context)
                        || !matchesAssistFilters(item, context)
                        || !matchesLedgerKind(item, subject, context)) {
                    continue;
                }
                String groupKey = buildGroupKey(item.getCcode(), item.getCdeptId(), item.getCpersonId(), item.getCcusId(), item.getCsupId(), item.getCitemClass(), item.getCitemId(), context);
                GroupBalanceSnapshot snapshot = result.computeIfAbsent(groupKey, unused -> new GroupBalanceSnapshot(subject));
                snapshot.add(item.getMb(), item.getNbS(), item);
            }
        } else {
            List<GlAccsum> balances = glAccsumMapper.selectList(
                    Wrappers.<GlAccsum>lambdaQuery()
                            .eq(GlAccsum::getCompanyId, context.companyId())
                            .eq(GlAccsum::getIyear, context.iyear())
                            .eq(GlAccsum::getIperiod, context.iperiod())
            );
            for (GlAccsum item : balances) {
                FinanceAccountSubject subject = subjectMap.get(item.getCcode());
                if (subject == null || !matchesSubjectRange(item.getCcode(), context) || !matchesSubjectLedgerKind(subject, context)) {
                    continue;
                }
                String groupKey = buildGroupKey(item.getCcode(), null, null, null, null, null, null, context);
                GroupBalanceSnapshot snapshot = result.computeIfAbsent(groupKey, unused -> new GroupBalanceSnapshot(subject));
                snapshot.add(item.getMb(), item.getNbS(), null);
            }
        }
        return result;
    }

    private Map<String, List<GlAccvouch>> groupDetailRows(List<GlAccvouch> rows, QueryContext context, Map<String, FinanceAccountSubject> subjectMap) {
        return rows.stream()
                .collect(Collectors.groupingBy(
                        row -> buildGroupKey(
                                row.getCcode(),
                                row.getCdeptId(),
                                row.getCpersonId(),
                                row.getCcusId(),
                                row.getCsupId(),
                                row.getCitemClass(),
                                row.getCitemId(),
                                context
                        ),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private List<GlAccvouch> loadVisibleDetailVoucherRows(QueryContext context, Map<String, FinanceAccountSubject> subjectMap) {
        List<GlAccvouch> rows = loadVoucherRowsForContext(context);
        return rows.stream()
                .filter(row -> matchesSubjectRange(row.getCcode(), context))
                .filter(row -> matchesAssistFilters(row, context))
                .filter(row -> matchesLedgerKind(row, subjectMap.get(row.getCcode()), context))
                .sorted(Comparator
                        .comparing(GlAccvouch::getCcode, Comparator.nullsLast(String::compareTo))
                        .thenComparing(GlAccvouch::getDbillDate, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(GlAccvouch::getInoId, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(GlAccvouch::getInid, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(GlAccvouch::getId, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private List<GlAccvouch> loadVoucherRowsForAugment(QueryContext context, Map<String, FinanceAccountSubject> subjectMap) {
        return loadVoucherRowsForContext(context).stream()
                .filter(row -> {
                    String status = resolveStatus(row);
                    return Objects.equals(status, STATUS_UNPOSTED) || Objects.equals(status, STATUS_REVIEWED);
                })
                .filter(row -> matchesSubjectRange(row.getCcode(), context))
                .filter(row -> matchesAssistFilters(row, context))
                .filter(row -> matchesLedgerKind(row, subjectMap.get(row.getCcode()), context))
                .toList();
    }

    private List<GlAccvouch> loadVoucherRowsForContext(QueryContext context) {
        LambdaQueryWrapper<GlAccvouch> voucherQuery = Wrappers.<GlAccvouch>lambdaQuery()
                .eq(GlAccvouch::getCompanyId, context.companyId());
        if (context.balanceSheet()) {
            applyPeriodRange(voucherQuery, context, GlAccvouch::getIyear, GlAccvouch::getIperiod);
        } else {
            voucherQuery
                    .eq(GlAccvouch::getIyear, context.iyear())
                    .eq(GlAccvouch::getIperiod, context.iperiod());
        }
        List<GlAccvouch> rows = glAccvouchMapper.selectList(
                voucherQuery
                        .eq(trimToNull(context.csign()) != null, GlAccvouch::getCsign, context.csign())
                        .orderByAsc(GlAccvouch::getDbillDate, GlAccvouch::getInoId, GlAccvouch::getInid, GlAccvouch::getId)
        );
        return rows.stream()
                .filter(row -> includeRowByStatus(row, context.includeUnposted()))
                .toList();
    }

    private boolean includeRowByStatus(GlAccvouch row, boolean includeUnposted) {
        String status = resolveStatus(row);
        if (Objects.equals(status, STATUS_VOIDED) || Objects.equals(status, STATUS_ERROR)) {
            return false;
        }
        if (Objects.equals(status, STATUS_POSTED)) {
            return true;
        }
        return includeUnposted && (Objects.equals(status, STATUS_UNPOSTED) || Objects.equals(status, STATUS_REVIEWED));
    }

    private boolean matchesSubjectRange(String subjectCode, QueryContext context) {
        String normalizedSubjectCode = trimToNull(subjectCode);
        if (normalizedSubjectCode == null) {
            return false;
        }
        if (trimToNull(context.accountCodeFrom()) != null && normalizedSubjectCode.compareTo(context.accountCodeFrom()) < 0) {
            return false;
        }
        return trimToNull(context.accountCodeTo()) == null || normalizedSubjectCode.compareTo(context.accountCodeTo()) <= 0;
    }

    private boolean isPeriod(Integer year, Integer period, Integer targetYear, Integer targetPeriod) {
        return Objects.equals(year, targetYear) && Objects.equals(period, targetPeriod);
    }

    private <T> void applyPeriodRange(
            LambdaQueryWrapper<T> query,
            QueryContext context,
            SFunction<T, Integer> yearColumn,
            SFunction<T, Integer> periodColumn
    ) {
        query.and(item -> item
                        .gt(yearColumn, context.iyearFrom())
                        .or(inner -> inner
                                .eq(yearColumn, context.iyearFrom())
                                .ge(periodColumn, context.iperiodFrom())))
                .and(item -> item
                        .lt(yearColumn, context.iyearTo())
                        .or(inner -> inner
                                .eq(yearColumn, context.iyearTo())
                                .le(periodColumn, context.iperiodTo())));
    }

    private boolean matchesAssistFilters(GlAccass row, QueryContext context) {
        return matchesAssistFilters(
                row.getCdeptId(),
                row.getCpersonId(),
                row.getCcusId(),
                row.getCsupId(),
                row.getCitemClass(),
                row.getCitemId(),
                context
        );
    }

    private boolean matchesAssistFilters(GlAccvouch row, QueryContext context) {
        return matchesAssistFilters(
                row.getCdeptId(),
                row.getCpersonId(),
                row.getCcusId(),
                row.getCsupId(),
                row.getCitemClass(),
                row.getCitemId(),
                context
        );
    }

    private boolean matchesAssistFilters(
            String cdeptId,
            String cpersonId,
            String ccusId,
            String csupId,
            String citemClass,
            String citemId,
            QueryContext context
    ) {
        if (trimToNull(context.cdeptId()) != null && !Objects.equals(normalizeOptional(cdeptId), normalizeOptional(context.cdeptId()))) {
            return false;
        }
        if (trimToNull(context.cpersonId()) != null && !Objects.equals(normalizeOptional(cpersonId), normalizeOptional(context.cpersonId()))) {
            return false;
        }
        if (trimToNull(context.ccusId()) != null && !Objects.equals(normalizeOptional(ccusId), normalizeOptional(context.ccusId()))) {
            return false;
        }
        if (trimToNull(context.csupId()) != null && !Objects.equals(normalizeOptional(csupId), normalizeOptional(context.csupId()))) {
            return false;
        }
        if (trimToNull(context.citemClass()) != null && !Objects.equals(normalizeOptional(citemClass), normalizeOptional(context.citemClass()))) {
            return false;
        }
        return trimToNull(context.citemId()) == null || Objects.equals(normalizeOptional(citemId), normalizeOptional(context.citemId()));
    }

    private boolean matchesSubjectLedgerKind(FinanceAccountSubject subject, QueryContext context) {
        return matchesLedgerKind((String) null, (String) null, (String) null, (String) null, (String) null, subject, context);
    }

    private boolean matchesLedgerKind(GlAccass row, FinanceAccountSubject subject, QueryContext context) {
        return matchesLedgerKind(
                row == null ? null : row.getCpersonId(),
                row == null ? null : row.getCcusId(),
                row == null ? null : row.getCsupId(),
                row == null ? null : row.getCitemClass(),
                row == null ? null : row.getCitemId(),
                subject,
                context
        );
    }

    private boolean matchesLedgerKind(GlAccvouch row, FinanceAccountSubject subject, QueryContext context) {
        return matchesLedgerKind(
                row == null ? null : row.getCpersonId(),
                row == null ? null : row.getCcusId(),
                row == null ? null : row.getCsupId(),
                row == null ? null : row.getCitemClass(),
                row == null ? null : row.getCitemId(),
                subject,
                context
        );
    }

    private boolean matchesLedgerKind(
            String cpersonId,
            String ccusId,
            String csupId,
            String citemClass,
            String citemId,
            FinanceAccountSubject subject,
            QueryContext context
    ) {
        String ledgerKind = context.ledgerKind();
        if (ledgerKind == null || Objects.equals(ledgerKind, LEDGER_KIND_DETAIL)) {
            return true;
        }
        if (Objects.equals(ledgerKind, LEDGER_KIND_PROJECT)) {
            return trimToNull(citemId) != null
                    && (trimToNull(context.citemClass()) == null || Objects.equals(trimToNull(citemClass), context.citemClass()))
                    && (trimToNull(context.citemId()) == null || Objects.equals(trimToNull(citemId), context.citemId()));
        }
        if (Objects.equals(ledgerKind, LEDGER_KIND_SUPPLIER)) {
            return trimToNull(csupId) != null;
        }
        if (Objects.equals(ledgerKind, LEDGER_KIND_CUSTOMER)) {
            return trimToNull(ccusId) != null;
        }
        if (Objects.equals(ledgerKind, LEDGER_KIND_PERSONAL)) {
            return trimToNull(cpersonId) != null;
        }
        return Objects.equals(ledgerKind, LEDGER_KIND_QUANTITY_AMOUNT)
                && subject != null
                && trimToNull(subject.getCmeasure()) != null;
    }

    private boolean isAssistLedger(QueryContext context) {
        return Objects.equals(context.ledgerKind(), LEDGER_KIND_PROJECT)
                || Objects.equals(context.ledgerKind(), LEDGER_KIND_SUPPLIER)
                || Objects.equals(context.ledgerKind(), LEDGER_KIND_CUSTOMER)
                || Objects.equals(context.ledgerKind(), LEDGER_KIND_PERSONAL);
    }

    private boolean hasAssistFilter(QueryContext context) {
        return trimToNull(context.cdeptId()) != null
                || trimToNull(context.cpersonId()) != null
                || trimToNull(context.ccusId()) != null
                || trimToNull(context.csupId()) != null
                || trimToNull(context.citemClass()) != null
                || trimToNull(context.citemId()) != null;
    }

    private List<FinanceDetailLedgerRowVO> buildLedgerRows(
            String groupKey,
            List<GlAccvouch> rows,
            FinanceAccountSubject subject,
            GroupBalanceSnapshot opening,
            QueryContext context
    ) {
        List<FinanceDetailLedgerRowVO> result = new ArrayList<>();
        BigDecimal runningAmount = normalizeMoney(opening.amount());
        BigDecimal runningQuantity = normalizeQuantity(opening.quantity());
        result.add(buildStaticLedgerRow(
                context.ledgerKind(),
                groupKey,
                "OPENING",
                subject,
                resolveLedgerAssistLabel(rows, opening, context),
                "期初余额",
                null,
                null,
                null,
                null,
                null,
                splitSignedAmount(subject.getBalanceDirection(), runningAmount),
                ZERO,
                ZERO,
                runningQuantity,
                trimToNull(subject.getCmeasure())
        ));

        BigDecimal totalDebit = ZERO;
        BigDecimal totalCredit = ZERO;
        BigDecimal totalQuantityDebit = ZERO;
        BigDecimal totalQuantityCredit = ZERO;
        for (GlAccvouch row : rows) {
            BigDecimal debit = FinanceVoucherAmountSupport.effectiveDebit(row.getMd(), row.getMc());
            BigDecimal credit = FinanceVoucherAmountSupport.effectiveCredit(row.getMd(), row.getMc());
            BigDecimal quantityDebit = positivePortion(row.getNdS());
            BigDecimal quantityCredit = positivePortion(row.getNcS());
            runningAmount = applyMovement(subject.getBalanceDirection(), runningAmount, debit, credit);
            runningQuantity = runningQuantity.add(quantityDebit).subtract(quantityCredit).setScale(2, RoundingMode.HALF_UP);
            totalDebit = totalDebit.add(debit).setScale(2, RoundingMode.HALF_UP);
            totalCredit = totalCredit.add(credit).setScale(2, RoundingMode.HALF_UP);
            totalQuantityDebit = totalQuantityDebit.add(quantityDebit).setScale(2, RoundingMode.HALF_UP);
            totalQuantityCredit = totalQuantityCredit.add(quantityCredit).setScale(2, RoundingMode.HALF_UP);
            result.add(buildEntryLedgerRow(
                    context.ledgerKind(),
                    groupKey,
                    row,
                    subject,
                    resolveAssistLabel(row, context),
                    runningAmount,
                    quantityDebit,
                    quantityCredit,
                    runningQuantity
            ));
        }

        result.add(buildStaticLedgerRow(
                context.ledgerKind(),
                groupKey,
                "TOTAL",
                subject,
                resolveLedgerAssistLabel(rows, opening, context),
                "本月合计",
                null,
                null,
                null,
                normalizeMoney(totalDebit),
                normalizeMoney(totalCredit),
                splitSignedAmount(subject.getBalanceDirection(), runningAmount),
                normalizeQuantity(totalQuantityDebit),
                normalizeQuantity(totalQuantityCredit),
                runningQuantity,
                trimToNull(subject.getCmeasure())
        ));
        result.add(buildStaticLedgerRow(
                context.ledgerKind(),
                groupKey,
                "ENDING",
                subject,
                resolveLedgerAssistLabel(rows, opening, context),
                "期末余额",
                null,
                null,
                null,
                null,
                null,
                splitSignedAmount(subject.getBalanceDirection(), runningAmount),
                ZERO,
                ZERO,
                runningQuantity,
                trimToNull(subject.getCmeasure())
        ));
        return result;
    }

    private FinanceDetailLedgerRowVO buildEntryLedgerRow(
            String ledgerKind,
            String groupKey,
            GlAccvouch row,
            FinanceAccountSubject subject,
            String assistLabel,
            BigDecimal runningAmount,
            BigDecimal quantityDebit,
            BigDecimal quantityCredit,
            BigDecimal quantityBalance
    ) {
        FinanceDetailLedgerRowVO item = new FinanceDetailLedgerRowVO();
        item.setLedgerKind(normalize(ledgerKind, LEDGER_KIND_DETAIL));
        item.setGroupKey(groupKey);
        item.setRowType("ENTRY");
        item.setSubjectCode(subject.getSubjectCode());
        item.setSubjectName(subject.getSubjectName());
        item.setAssistLabel(assistLabel);
        item.setDbillDate(formatDate(row.getDbillDate()));
        item.setVoucherNo(buildVoucherNo(row.getCompanyId(), row.getIyear(), row.getIperiod(), row.getCsign(), row.getInoId()));
        item.setDisplayVoucherNo(buildDisplayVoucherNo(row.getCsign(), row.getInoId()));
        item.setSummary(trimToNull(row.getCdigest()));
        item.setVoucherTypeLabel(resolveVoucherTypeLabel(row.getCsign()));
        item.setMakerName(trimToNull(row.getCbill()));
        item.setDebit(FinanceVoucherAmountSupport.effectiveDebit(row.getMd(), row.getMc()));
        item.setCredit(FinanceVoucherAmountSupport.effectiveCredit(row.getMd(), row.getMc()));
        item.setBalance(FinanceBalanceDirectionSupport.displayAmount(runningAmount));
        item.setBalanceDirection(FinanceBalanceDirectionSupport.resolveActualDirectionLabel(subject.getBalanceDirection(), runningAmount));
        item.setQuantityDebit(normalizeQuantity(quantityDebit));
        item.setQuantityCredit(normalizeQuantity(quantityCredit));
        item.setQuantityBalance(normalizeQuantity(quantityBalance));
        item.setMeasureUnit(trimToNull(subject.getCmeasure()));
        return item;
    }

    private String resolveLedgerAssistLabel(List<GlAccvouch> rows, GroupBalanceSnapshot opening, QueryContext context) {
        if (!rows.isEmpty()) {
            return resolveAssistLabel(rows.get(0), context);
        }
        return resolveAssistLabel(opening.sample(), context);
    }

    private FinanceDetailLedgerRowVO buildStaticLedgerRow(
            String ledgerKind,
            String groupKey,
            String rowType,
            FinanceAccountSubject subject,
            String assistLabel,
            String summary,
            String dbillDate,
            String voucherNo,
            String displayVoucherNo,
            BigDecimal debit,
            BigDecimal credit,
            BalanceDisplay balanceDisplay,
            BigDecimal quantityDebit,
            BigDecimal quantityCredit,
            BigDecimal quantityBalance,
            String measureUnit
    ) {
        FinanceDetailLedgerRowVO row = new FinanceDetailLedgerRowVO();
        row.setLedgerKind(normalize(ledgerKind, LEDGER_KIND_DETAIL));
        row.setGroupKey(groupKey);
        row.setRowType(rowType);
        row.setSubjectCode(subject.getSubjectCode());
        row.setSubjectName(subject.getSubjectName());
        row.setAssistLabel(assistLabel);
        row.setSummary(summary);
        row.setDbillDate(dbillDate);
        row.setVoucherNo(voucherNo);
        row.setDisplayVoucherNo(displayVoucherNo);
        row.setDebit(debit == null ? ZERO : normalizeMoney(debit));
        row.setCredit(credit == null ? ZERO : normalizeMoney(credit));
        row.setBalance(balanceDisplay.amount());
        row.setBalanceDirection(balanceDisplay.directionLabel());
        row.setQuantityDebit(normalizeQuantity(quantityDebit));
        row.setQuantityCredit(normalizeQuantity(quantityCredit));
        row.setQuantityBalance(normalizeQuantity(quantityBalance));
        row.setMeasureUnit(measureUnit);
        return row;
    }

    private BalanceDisplay computeEndingDisplay(List<FinanceDetailLedgerRowVO> rows, FinanceAccountSubject subject) {
        FinanceDetailLedgerRowVO last = rows.isEmpty() ? null : rows.get(rows.size() - 1);
        if (last == null) {
            return splitSignedAmount(subject.getBalanceDirection(), ZERO);
        }
        BigDecimal signedAmount = restoreSignedAmount(subject.getBalanceDirection(), last.getBalanceDirection(), last.getBalance());
        return splitSignedAmount(subject.getBalanceDirection(), signedAmount);
    }

    private BigDecimal restoreSignedAmount(String defaultBalanceDirection, String actualDirectionLabel, BigDecimal displayAmount) {
        BigDecimal normalized = normalizeMoney(displayAmount);
        if (!isNonZero(normalized)) {
            return ZERO;
        }
        String expectedPositiveDirection = FinanceBalanceDirectionSupport.isDebitDirection(defaultBalanceDirection) ? "借" : "贷";
        return Objects.equals(expectedPositiveDirection, trimToNull(actualDirectionLabel))
                ? normalized
                : normalized.negate().setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumDetailColumn(List<FinanceDetailLedgerRowVO> rows, String rowType, boolean debit) {
        return rows.stream()
                .filter(item -> Objects.equals(item.getRowType(), rowType))
                .map(item -> debit ? item.getDebit() : item.getCredit())
                .map(this::normalizeMoney)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String buildGroupKey(
            String subjectCode,
            String cdeptId,
            String cpersonId,
            String ccusId,
            String csupId,
            String citemClass,
            String citemId,
            QueryContext context
    ) {
        String ledgerKind = normalize(context.ledgerKind(), LEDGER_KIND_DETAIL);
        if (Objects.equals(ledgerKind, LEDGER_KIND_PROJECT)) {
            return subjectCode + "|" + normalize(citemClass, "") + "|" + normalize(citemId, "");
        }
        if (Objects.equals(ledgerKind, LEDGER_KIND_SUPPLIER)) {
            return subjectCode + "|" + normalize(csupId, "");
        }
        if (Objects.equals(ledgerKind, LEDGER_KIND_CUSTOMER)) {
            return subjectCode + "|" + normalize(ccusId, "");
        }
        if (Objects.equals(ledgerKind, LEDGER_KIND_PERSONAL)) {
            return subjectCode + "|" + normalize(cpersonId, "");
        }
        return subjectCode;
    }

    private String resolveAssistLabel(GlAccvouch row, QueryContext context) {
        if (row == null) {
            return "";
        }
        return resolveAssistLabel(
                row.getCdeptId(),
                row.getCpersonId(),
                row.getCcusId(),
                row.getCsupId(),
                row.getCitemClass(),
                row.getCitemId(),
                context
        );
    }

    private String resolveAssistLabel(GlAccass row, QueryContext context) {
        if (row == null) {
            return "";
        }
        return resolveAssistLabel(
                row.getCdeptId(),
                row.getCpersonId(),
                row.getCcusId(),
                row.getCsupId(),
                row.getCitemClass(),
                row.getCitemId(),
                context
        );
    }

    private String resolveAssistLabel(
            String cdeptId,
            String cpersonId,
            String ccusId,
            String csupId,
            String citemClass,
            String citemId,
            QueryContext context
    ) {
        String ledgerKind = normalize(context.ledgerKind(), LEDGER_KIND_DETAIL);
        if (Objects.equals(ledgerKind, LEDGER_KIND_PROJECT)) {
            Map<String, String> projectLabels = loadProjectOptions(context.companyId()).stream()
                    .collect(Collectors.toMap(FinanceVoucherOptionVO::getValue, FinanceVoucherOptionVO::getLabel, (left, right) -> left));
            return normalize(projectLabels.get(citemId), normalize(citemId, ""));
        }
        if (Objects.equals(ledgerKind, LEDGER_KIND_SUPPLIER)) {
            Map<String, FinanceVendor> vendors = loadEnabledSupplierMap(context.companyId());
            FinanceVendor vendor = vendors.get(csupId);
            return vendor == null ? normalize(csupId, "") : resolveVendorName(vendor);
        }
        if (Objects.equals(ledgerKind, LEDGER_KIND_CUSTOMER)) {
            Map<String, FinanceCustomer> customers = loadEnabledCustomerMap(context.companyId());
            FinanceCustomer customer = customers.get(ccusId);
            return customer == null ? normalize(ccusId, "") : resolveCustomerName(customer);
        }
        if (Objects.equals(ledgerKind, LEDGER_KIND_PERSONAL)) {
            Map<String, String> users = loadEnabledUsers().stream()
                    .collect(Collectors.toMap(item -> String.valueOf(item.getId()), this::resolveUserName, (left, right) -> left));
            return normalize(users.get(cpersonId), normalize(cpersonId, ""));
        }
        if (trimToNull(cdeptId) != null) {
            Map<String, String> departments = loadEnabledDepartments().stream()
                    .collect(Collectors.toMap(item -> String.valueOf(item.getId()), item -> normalize(item.getDeptName(), ""), (left, right) -> left));
            return normalize(departments.get(cdeptId), cdeptId);
        }
        return "";
    }

    private BalanceDisplay splitSignedAmount(String balanceDirection, BigDecimal signedAmount) {
        BigDecimal normalized = normalizeMoney(signedAmount);
        if (!isNonZero(normalized)) {
            return new BalanceDisplay(ZERO, ZERO, "");
        }
        String direction = FinanceBalanceDirectionSupport.resolveActualDirectionLabel(balanceDirection, normalized);
        if (Objects.equals(direction, "贷")) {
            return new BalanceDisplay(ZERO, FinanceBalanceDirectionSupport.displayAmount(normalized), direction);
        }
        return new BalanceDisplay(FinanceBalanceDirectionSupport.displayAmount(normalized), ZERO, direction);
    }

    private BigDecimal applyMovement(String balanceDirection, BigDecimal current, BigDecimal debit, BigDecimal credit) {
        BigDecimal base = normalizeMoney(current);
        BigDecimal debitAmount = normalizeMoney(debit);
        BigDecimal creditAmount = normalizeMoney(credit);
        if (FinanceBalanceDirectionSupport.isDebitDirection(balanceDirection)) {
            return base.add(debitAmount).subtract(creditAmount).setScale(2, RoundingMode.HALF_UP);
        }
        return base.subtract(debitAmount).add(creditAmount).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeQuantity(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal positivePortion(BigDecimal value) {
        BigDecimal normalized = normalizeQuantity(value);
        return normalized.compareTo(BigDecimal.ZERO) > 0 ? normalized : ZERO;
    }

    private boolean isNonZero(BigDecimal value) {
        return normalizeMoney(value).compareTo(BigDecimal.ZERO) != 0;
    }

    private String lowerText(String value) {
        return value == null ? null : value.toLowerCase();
    }

    private String normalizeOptional(String value) {
        return trimToNull(value);
    }

    private <T> FinanceLedgerReportPageVO<T> buildLedgerPage(List<T> items, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, 500);
        int total = items == null ? 0 : items.size();
        int start = Math.min((safePage - 1) * safePageSize, total);
        int end = Math.min(start + safePageSize, total);
        FinanceLedgerReportPageVO<T> result = new FinanceLedgerReportPageVO<>();
        result.setTotal(total);
        result.setPage(safePage);
        result.setPageSize(safePageSize);
        result.setItems(items == null ? List.of() : new ArrayList<>(items.subList(start, end)));
        return result;
    }

    private byte[] exportBalanceWorkbook(QueryContext context, List<FinanceBalanceSheetRowVO> rows) {
        return exportWorkbook("余额表", workbook -> {
            Sheet sheet = workbook.createSheet("余额表");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle amountStyle = createAmountStyle(workbook);
            CellStyle totalStyle = createBoldStyle(workbook);
            String[] headers = {"科目编码", "科目名称", "级次", "期初借方", "期初贷方", "本期借方", "本期贷方", "期末借方", "期末贷方"};
            writeHeaders(sheet, headers, headerStyle);
            int rowIndex = 1;
            for (FinanceBalanceSheetRowVO item : rows) {
                Row row = sheet.createRow(rowIndex++);
                CellStyle textStyle = "CATEGORY_TOTAL".equals(item.getRowType()) ? totalStyle : null;
                setTextCell(row, 0, item.getSubjectCode(), textStyle);
                setTextCell(row, 1, item.getSubjectName(), textStyle);
                setIntegerCell(row, 2, item.getSubjectLevel(), textStyle);
                setAmountCell(row, 3, item.getBeginDebit(), amountStyle);
                setAmountCell(row, 4, item.getBeginCredit(), amountStyle);
                setAmountCell(row, 5, item.getPeriodDebit(), amountStyle);
                setAmountCell(row, 6, item.getPeriodCredit(), amountStyle);
                setAmountCell(row, 7, item.getEndDebit(), amountStyle);
                setAmountCell(row, 8, item.getEndCredit(), amountStyle);
            }
            autosize(sheet, headers.length);
        });
    }

    private byte[] exportGeneralLedgerWorkbook(QueryContext context, List<FinanceGeneralLedgerSectionVO> sections) {
        return exportWorkbook("总分类账", workbook -> {
            Sheet sheet = workbook.createSheet("总分类账");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle amountStyle = createAmountStyle(workbook);
            String[] headers = {"科目编码", "科目名称", "行类型", "日期", "凭证号", "摘要", "借方", "贷方", "余额", "方向"};
            writeHeaders(sheet, headers, headerStyle);
            int rowIndex = 1;
            for (FinanceGeneralLedgerSectionVO section : sections) {
                for (FinanceDetailLedgerRowVO item : section.getRows()) {
                    Row row = sheet.createRow(rowIndex++);
                    setTextCell(row, 0, item.getSubjectCode());
                    setTextCell(row, 1, item.getSubjectName());
                    setTextCell(row, 2, item.getRowType());
                    setTextCell(row, 3, item.getDbillDate());
                    setTextCell(row, 4, item.getDisplayVoucherNo());
                    setTextCell(row, 5, item.getSummary());
                    setAmountCell(row, 6, item.getDebit(), amountStyle);
                    setAmountCell(row, 7, item.getCredit(), amountStyle);
                    setAmountCell(row, 8, item.getBalance(), amountStyle);
                    setTextCell(row, 9, item.getBalanceDirection());
                }
            }
            autosize(sheet, headers.length);
        });
    }

    private byte[] exportDetailLedgerWorkbook(QueryContext context, List<FinanceDetailLedgerRowVO> rows) {
        return exportWorkbook("明细账", workbook -> {
            Sheet sheet = workbook.createSheet("明细账");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle amountStyle = createAmountStyle(workbook);
            String[] headers = {"分组", "科目编码", "科目名称", "辅助项", "行类型", "日期", "凭证号", "摘要", "借方", "贷方", "余额", "方向", "数量借方", "数量贷方", "数量余额", "计量单位"};
            writeHeaders(sheet, headers, headerStyle);
            int rowIndex = 1;
            for (FinanceDetailLedgerRowVO item : rows) {
                Row row = sheet.createRow(rowIndex++);
                setTextCell(row, 0, item.getGroupKey());
                setTextCell(row, 1, item.getSubjectCode());
                setTextCell(row, 2, item.getSubjectName());
                setTextCell(row, 3, item.getAssistLabel());
                setTextCell(row, 4, item.getRowType());
                setTextCell(row, 5, item.getDbillDate());
                setTextCell(row, 6, item.getDisplayVoucherNo());
                setTextCell(row, 7, item.getSummary());
                setAmountCell(row, 8, item.getDebit(), amountStyle);
                setAmountCell(row, 9, item.getCredit(), amountStyle);
                setAmountCell(row, 10, item.getBalance(), amountStyle);
                setTextCell(row, 11, item.getBalanceDirection());
                setAmountCell(row, 12, item.getQuantityDebit(), amountStyle);
                setAmountCell(row, 13, item.getQuantityCredit(), amountStyle);
                setAmountCell(row, 14, item.getQuantityBalance(), amountStyle);
                setTextCell(row, 15, item.getMeasureUnit());
            }
            autosize(sheet, headers.length);
        });
    }

    private byte[] exportSequenceLedgerWorkbook(QueryContext context, List<FinanceSequenceLedgerRowVO> rows) {
        return exportWorkbook("序时账", workbook -> {
            Sheet sheet = workbook.createSheet("序时账");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle amountStyle = createAmountStyle(workbook);
            String[] headers = {"凭证号", "凭证类型", "制单日期", "会计期间", "摘要", "制单人", "附件张数", "借方合计", "贷方合计", "状态"};
            writeHeaders(sheet, headers, headerStyle);
            int rowIndex = 1;
            for (FinanceSequenceLedgerRowVO item : rows) {
                Row row = sheet.createRow(rowIndex++);
                setTextCell(row, 0, item.getDisplayVoucherNo());
                setTextCell(row, 1, item.getVoucherTypeLabel());
                setTextCell(row, 2, item.getDbillDate());
                setTextCell(row, 3, item.getIyperiod() == null ? "" : String.valueOf(item.getIyperiod()));
                setTextCell(row, 4, item.getSummary());
                setTextCell(row, 5, item.getCbill());
                setIntegerCell(row, 6, item.getIdoc());
                setAmountCell(row, 7, item.getTotalDebit(), amountStyle);
                setAmountCell(row, 8, item.getTotalCredit(), amountStyle);
                setTextCell(row, 9, item.getStatusLabel());
            }
            autosize(sheet, headers.length);
        });
    }

    private byte[] exportWorkbook(String sheetName, WorkbookWriter writer) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.setCompressTempFiles(true);
            writer.write(workbook);
            workbook.write(outputStream);
            workbook.dispose();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("导出" + sheetName + "失败");
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createAmountStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        style.setDataFormat(dataFormat.getFormat("#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private void writeHeaders(Sheet sheet, String[] headers, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int index = 0; index < headers.length; index += 1) {
            Cell cell = headerRow.createCell(index);
            cell.setCellValue(headers[index]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void setTextCell(Row row, int columnIndex, String value) {
        row.createCell(columnIndex).setCellValue(value == null ? "" : value);
    }

    private void setTextCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value == null ? "" : value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void setIntegerCell(Row row, int columnIndex, Integer value) {
        if (value == null) {
            row.createCell(columnIndex).setCellValue("");
            return;
        }
        row.createCell(columnIndex).setCellValue(value);
    }

    private void setIntegerCell(Row row, int columnIndex, Integer value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (value == null) {
            cell.setCellValue("");
        } else {
            cell.setCellValue(value);
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void setAmountCell(Row row, int columnIndex, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellStyle(style);
        cell.setCellValue(normalizeMoney(value).doubleValue());
    }

    private void autosize(Sheet sheet, int columnCount) {
        for (int index = 0; index < columnCount; index += 1) {
            sheet.setColumnWidth(index, 18 * 256);
        }
    }

    private Integer firstNonNull(Integer... values) {
        if (values == null) {
            return null;
        }
        for (Integer value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @FunctionalInterface
    private interface WorkbookWriter {
        void write(Workbook workbook);
    }

    private record QueryContext(
            String companyId,
            Integer iyear,
            Integer iperiod,
            Integer iyearFrom,
            Integer iperiodFrom,
            Integer iyearTo,
            Integer iperiodTo,
            boolean balanceSheet,
            String ledgerKind,
            String accountCodeFrom,
            String accountCodeTo,
            String cdeptId,
            String cpersonId,
            String ccusId,
            String csupId,
            String citemClass,
            String citemId,
            String balanceAssistDisplay,
            String subjectLevelRange,
            String voucherNoKeyword,
            String csign,
            String summaryKeyword,
            String makerKeyword,
            boolean includeUnposted,
            int page,
            int pageSize
    ) {
    }

    private record PeriodRange(YearMonth from, YearMonth to) {
    }

    private record BalanceDisplay(BigDecimal debit, BigDecimal credit, String directionLabel) {
        private BigDecimal amount() {
            return debit.compareTo(BigDecimal.ZERO) > 0 ? debit : credit;
        }
    }

    private record SequenceVoucherKey(
            String companyId,
            Integer iyear,
            Integer iyperiod,
            Integer iperiod,
            String csign,
            Integer inoId
    ) {
    }

    private record BalanceAssistKey(
            String subjectCode,
            String ccusId,
            String csupId,
            String citemClass,
            String citemId,
            String cdeptId,
            String cpersonId
    ) implements Comparable<BalanceAssistKey> {
        private static BalanceAssistKey from(GlAccass row) {
            return new BalanceAssistKey(
                    trim(row.getCcode()),
                    trim(row.getCcusId()),
                    trim(row.getCsupId()),
                    trim(row.getCitemClass()),
                    trim(row.getCitemId()),
                    trim(row.getCdeptId()),
                    trim(row.getCpersonId())
            );
        }

        private static BalanceAssistKey from(GlAccvouch row) {
            return new BalanceAssistKey(
                    trim(row.getCcode()),
                    trim(row.getCcusId()),
                    trim(row.getCsupId()),
                    trim(row.getCitemClass()),
                    trim(row.getCitemId()),
                    trim(row.getCdeptId()),
                    trim(row.getCpersonId())
            );
        }

        @Override
        public int compareTo(BalanceAssistKey other) {
            return Comparator
                    .comparing(BalanceAssistKey::subjectCode, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(BalanceAssistKey::ccusId, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(BalanceAssistKey::csupId, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(BalanceAssistKey::citemClass, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(BalanceAssistKey::citemId, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(BalanceAssistKey::cdeptId, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(BalanceAssistKey::cpersonId, Comparator.nullsFirst(String::compareTo))
                    .compare(this, other);
        }

        private static String trim(String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }
    }

    private record BalanceAssistLabels(
            Map<String, FinanceVoucherOptionVO> customers,
            Map<String, FinanceVoucherOptionVO> suppliers,
            Map<String, FinanceVoucherOptionVO> projects,
            Map<String, FinanceVoucherOptionVO> projectClasses,
            Map<String, FinanceVoucherOptionVO> departments,
            Map<String, FinanceVoucherOptionVO> employees
    ) {
        private String format(BalanceAssistKey key, String displayMode) {
            List<String> parts = new ArrayList<>();
            add(parts, "客户", customers.get(key.ccusId()), key.ccusId(), displayMode);
            add(parts, "供应商", suppliers.get(key.csupId()), key.csupId(), displayMode);
            FinanceVoucherOptionVO project = projects.get(key.citemId());
            if (project == null && key.citemClass != null) {
                project = projectClasses.get(key.citemClass);
            }
            add(parts, "项目", project, firstNonBlank(key.citemId(), key.citemClass), displayMode);
            add(parts, "部门", departments.get(key.cdeptId()), key.cdeptId(), displayMode);
            add(parts, "个人", employees.get(key.cpersonId()), key.cpersonId(), displayMode);
            return String.join("*", parts);
        }

        private void add(
                List<String> parts,
                String dimension,
                FinanceVoucherOptionVO option,
                String fallback,
                String displayMode
        ) {
            String value = switch (normalizeMode(displayMode)) {
                case "CODE" -> option == null ? fallback : firstNonBlank(option.getCode(), option.getValue(), fallback);
                case "CODE_NAME" -> option == null
                        ? fallback
                        : joinCodeName(option.getCode(), option.getName(), option.getValue());
                default -> option == null ? fallback : firstNonBlank(option.getName(), option.getLabel(), fallback);
            };
            if (value != null) {
                parts.add(dimension + "：" + value);
            }
        }

        private static String normalizeMode(String displayMode) {
            return displayMode == null ? "NAME" : displayMode;
        }

        private static String joinCodeName(String code, String name, String fallback) {
            String normalizedCode = firstNonBlank(code, fallback);
            String normalizedName = trim(name);
            if (normalizedCode == null) {
                return normalizedName;
            }
            if (normalizedName == null || normalizedName.equals(normalizedCode)) {
                return normalizedCode;
            }
            return normalizedCode + " " + normalizedName;
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
    }

    private static final class BalanceTotalAccumulator {
        private BigDecimal beginDebit = ZERO;
        private BigDecimal beginCredit = ZERO;
        private BigDecimal periodDebit = ZERO;
        private BigDecimal periodCredit = ZERO;
        private BigDecimal endDebit = ZERO;
        private BigDecimal endCredit = ZERO;

        private void addRow(FinanceBalanceSheetRowVO row) {
            beginDebit = add(beginDebit, row.getBeginDebit());
            beginCredit = add(beginCredit, row.getBeginCredit());
            periodDebit = add(periodDebit, row.getPeriodDebit());
            periodCredit = add(periodCredit, row.getPeriodCredit());
            endDebit = add(endDebit, row.getEndDebit());
            endCredit = add(endCredit, row.getEndCredit());
        }

        private BigDecimal beginDebit() {
            return beginDebit;
        }

        private BigDecimal beginCredit() {
            return beginCredit;
        }

        private BigDecimal periodDebit() {
            return periodDebit;
        }

        private BigDecimal periodCredit() {
            return periodCredit;
        }

        private BigDecimal endDebit() {
            return endDebit;
        }

        private BigDecimal endCredit() {
            return endCredit;
        }

        private static BigDecimal add(BigDecimal left, BigDecimal right) {
            return left.add(right == null ? ZERO : right).setScale(2, RoundingMode.HALF_UP);
        }
    }

    private static final class BalanceAccumulator {
        private final FinanceAccountSubject subject;
        private BigDecimal beginSigned = ZERO;
        private BigDecimal periodDebit = ZERO;
        private BigDecimal periodCredit = ZERO;
        private BigDecimal endSigned = ZERO;

        private BalanceAccumulator(FinanceAccountSubject subject) {
            this.subject = subject;
        }

        private void addOpeningBase(BigDecimal mb) {
            beginSigned = normalizeSigned(beginSigned, mb);
        }

        private void addPeriodBase(BigDecimal md, BigDecimal mc) {
            periodDebit = normalizeSigned(periodDebit, md);
            periodCredit = normalizeSigned(periodCredit, mc);
        }

        private void addEndingBase(BigDecimal me) {
            endSigned = normalizeSigned(endSigned, me);
        }

        private void addUnposted(GlAccvouch row) {
            BigDecimal debit = FinanceVoucherAmountSupport.effectiveDebit(row.getMd(), row.getMc());
            BigDecimal credit = FinanceVoucherAmountSupport.effectiveCredit(row.getMd(), row.getMc());
            periodDebit = periodDebit.add(debit).setScale(2, RoundingMode.HALF_UP);
            periodCredit = periodCredit.add(credit).setScale(2, RoundingMode.HALF_UP);
            if (FinanceBalanceDirectionSupport.isDebitDirection(subject.getBalanceDirection())) {
                endSigned = endSigned.add(debit).subtract(credit).setScale(2, RoundingMode.HALF_UP);
            } else {
                endSigned = endSigned.subtract(debit).add(credit).setScale(2, RoundingMode.HALF_UP);
            }
        }

        private FinanceAccountSubject subject() {
            return subject;
        }

        private BigDecimal beginSigned() {
            return beginSigned;
        }

        private BigDecimal periodDebit() {
            return periodDebit;
        }

        private BigDecimal periodCredit() {
            return periodCredit;
        }

        private BigDecimal endSigned() {
            return endSigned;
        }

        private static BigDecimal normalizeSigned(BigDecimal current, BigDecimal addition) {
            BigDecimal left = current == null ? ZERO : current.setScale(2, RoundingMode.HALF_UP);
            BigDecimal right = addition == null ? ZERO : addition.setScale(2, RoundingMode.HALF_UP);
            return left.add(right).setScale(2, RoundingMode.HALF_UP);
        }
    }

    private static final class GroupBalanceSnapshot {
        private final FinanceAccountSubject subject;
        private BigDecimal amount = ZERO;
        private BigDecimal quantity = ZERO;
        private GlAccass sample;

        private GroupBalanceSnapshot(FinanceAccountSubject subject) {
            this.subject = subject;
        }

        private void add(BigDecimal value, BigDecimal quantityValue, GlAccass source) {
            amount = amount.add(value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP)).setScale(2, RoundingMode.HALF_UP);
            quantity = quantity.add(quantityValue == null ? ZERO : quantityValue.setScale(2, RoundingMode.HALF_UP)).setScale(2, RoundingMode.HALF_UP);
            if (sample == null) {
                sample = source;
            }
        }

        private FinanceAccountSubject subject() {
            return subject;
        }

        private BigDecimal amount() {
            return amount;
        }

        private BigDecimal quantity() {
            return quantity;
        }

        private GlAccass sample() {
            return sample;
        }

        private static GroupBalanceSnapshot empty(FinanceAccountSubject subject) {
            return new GroupBalanceSnapshot(subject);
        }
    }
}
