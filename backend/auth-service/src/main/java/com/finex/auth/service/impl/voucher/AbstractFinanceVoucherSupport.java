package com.finex.auth.service.impl.voucher;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.FinanceVoucherDetailVO;
import com.finex.auth.dto.FinanceVoucherEntryDTO;
import com.finex.auth.dto.FinanceVoucherEntryVO;
import com.finex.auth.dto.FinanceVoucherMetaVO;
import com.finex.auth.dto.FinanceVoucherOptionVO;
import com.finex.auth.dto.FinanceVoucherPageVO;
import com.finex.auth.dto.FinanceVoucherQueryDTO;
import com.finex.auth.dto.FinanceVoucherSaveDTO;
import com.finex.auth.dto.FinanceVoucherSaveResultVO;
import com.finex.auth.dto.FinanceVoucherSummaryVO;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.FinanceCustomer;
import com.finex.auth.entity.FinanceProjectArchive;
import com.finex.auth.entity.FinanceProjectClass;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinanceCustomerMapper;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class AbstractFinanceVoucherSupport {

    protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    protected static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    protected static final String DEFAULT_VOUCHER_TYPE = "记";
    protected static final String DEFAULT_CURRENCY = "CNY";
    protected static final BigDecimal DEFAULT_RATE = BigDecimal.ONE;
    protected static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    protected static final String STATUS_UNPOSTED = "UNPOSTED";
    protected static final String STATUS_REVIEWED = "REVIEWED";
    protected static final String STATUS_POSTED = "POSTED";
    protected static final String VOUCHER_NO_SEPARATOR = "~";
    protected static final int DEFAULT_PAGE = 1;
    protected static final int DEFAULT_PAGE_SIZE = 20;
    protected static final int MAX_PAGE_SIZE = 200;

    protected static final List<OptionSeed> VOUCHER_TYPE_SEEDS = List.of(
            new OptionSeed("记", "记账凭证"),
            new OptionSeed("收", "收款凭证"),
            new OptionSeed("付", "付款凭证"),
            new OptionSeed("转", "转账凭证")
    );

    protected static final List<OptionSeed> CURRENCY_SEEDS = List.of(
            new OptionSeed("CNY", "人民币"),
            new OptionSeed("USD", "美元"),
            new OptionSeed("EUR", "欧元")
    );

    private final GlAccvouchMapper glAccvouchMapper;
    private final FinanceAccountSubjectMapper financeAccountSubjectMapper;
    private final FinanceCustomerMapper financeCustomerMapper;
    private final FinanceVendorMapper financeVendorMapper;
    private final FinanceProjectClassMapper financeProjectClassMapper;
    private final FinanceProjectArchiveMapper financeProjectArchiveMapper;
    private final SystemCompanyMapper systemCompanyMapper;
    private final SystemDepartmentMapper systemDepartmentMapper;
    private final UserMapper userMapper;

    protected final ConcurrentHashMap<String, Object> voucherNoLocks = new ConcurrentHashMap<>();

    protected FinanceVoucherMetaVO getMeta(
            Long currentUserId,
            String currentUsername,
            String companyId,
            String billDate,
            String csign
    ) {
        User currentUser = requireUser(currentUserId);
        List<SystemCompany> companies = loadEnabledCompanies();
        String effectiveCompanyId = resolveDefaultCompanyId(companyId, currentUser, companies);
        List<SystemDepartment> departments = loadEnabledDepartments();
        List<User> employees = loadEnabledUsers();
        LocalDate effectiveBillDate = parseDateOrDefault(billDate, LocalDate.now());
        String effectiveVoucherType = normalize(csign, DEFAULT_VOUCHER_TYPE);

        FinanceVoucherMetaVO meta = new FinanceVoucherMetaVO();
        meta.setCompanyOptions(companies.stream().map(this::toCompanyOption).toList());
        meta.setDepartmentOptions(departments.stream().map(this::toDepartmentOption).toList());
        meta.setEmployeeOptions(employees.stream().map(this::toEmployeeOption).toList());
        meta.setVoucherTypeOptions(toOptions(VOUCHER_TYPE_SEEDS));
        meta.setCurrencyOptions(toOptions(CURRENCY_SEEDS));
        meta.setAccountOptions(loadAccountOptions(effectiveCompanyId));
        meta.setCustomerOptions(loadCustomerOptions(effectiveCompanyId));
        meta.setSupplierOptions(loadSupplierOptions(effectiveCompanyId));
        meta.setProjectClassOptions(loadProjectClassOptions(effectiveCompanyId));
        meta.setProjectOptions(loadProjectOptions(effectiveCompanyId));
        meta.setDefaultCompanyId(effectiveCompanyId);
        meta.setDefaultBillDate(effectiveBillDate.format(DATE_FORMATTER));
        meta.setDefaultPeriod(effectiveBillDate.getMonthValue());
        meta.setDefaultVoucherType(effectiveVoucherType);
        meta.setSuggestedVoucherNo(nextVoucherNo(effectiveCompanyId, effectiveBillDate.getMonthValue(), effectiveVoucherType));
        meta.setDefaultMaker(resolveMakerName(currentUser, currentUsername));
        meta.setDefaultAttachedDocCount(0);
        meta.setDefaultCurrency(DEFAULT_CURRENCY);
        return meta;
    }

    protected FinanceVoucherPageVO<FinanceVoucherSummaryVO> queryVouchers(FinanceVoucherQueryDTO dto) {
        List<FinanceVoucherSummaryVO> summaries = loadVoucherSummaries(dto);
        return buildPage(summaries, dto == null ? null : dto.getPage(), dto == null ? null : dto.getPageSize());
    }

    protected FinanceVoucherDetailVO getDetail(String companyId, String voucherNo) {
        VoucherKey voucherKey = parseVoucherNo(voucherNo);
        validateVoucherCompany(companyId, voucherKey);
        List<GlAccvouch> rows = glAccvouchMapper.selectList(
                Wrappers.<GlAccvouch>lambdaQuery()
                        .eq(GlAccvouch::getCompanyId, voucherKey.companyId())
                        .eq(GlAccvouch::getIperiod, voucherKey.iperiod())
                        .eq(GlAccvouch::getCsign, voucherKey.csign())
                        .eq(GlAccvouch::getInoId, voucherKey.inoId())
                        .orderByAsc(GlAccvouch::getInid, GlAccvouch::getId)
        );
        if (rows.isEmpty()) {
            throw new IllegalStateException("凭证不存在");
        }

        GlAccvouch headerRow = rows.get(0);
        FinanceVoucherDetailVO detail = new FinanceVoucherDetailVO();
        detail.setVoucherNo(buildVoucherNo(
                headerRow.getCompanyId(),
                headerRow.getIperiod(),
                headerRow.getCsign(),
                headerRow.getInoId()
        ));
        detail.setDisplayVoucherNo(buildDisplayVoucherNo(headerRow.getCsign(), headerRow.getInoId()));
        detail.setCompanyId(headerRow.getCompanyId());
        detail.setIperiod(headerRow.getIperiod());
        detail.setCsign(headerRow.getCsign());
        detail.setVoucherTypeLabel(resolveVoucherTypeLabel(headerRow.getCsign()));
        detail.setInoId(headerRow.getInoId());
        detail.setDbillDate(formatDate(headerRow.getDbillDate()));
        detail.setIdoc(headerRow.getIdoc());
        detail.setCbill(headerRow.getCbill());
        detail.setCtext1(headerRow.getCtext1());
        detail.setCtext2(headerRow.getCtext2());
        detail.setStatus(resolveStatus(headerRow));
        detail.setStatusLabel(resolveStatusLabel(detail.getStatus()));
        detail.setEditable(isEditableStatus(detail.getStatus()));

        Map<String, String> accountNameMap = loadAccountNameMap(headerRow.getCompanyId());
        List<FinanceVoucherEntryVO> entries = rows.stream()
                .map(row -> toEntryVO(row, accountNameMap))
                .toList();
        detail.setEntries(entries);
        detail.setTotalDebit(sumAmount(rows, true));
        detail.setTotalCredit(sumAmount(rows, false));
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    protected FinanceVoucherSaveResultVO saveVoucher(FinanceVoucherSaveDTO dto, Long currentUserId, String currentUsername) {
        User currentUser = requireUser(currentUserId);
        String companyId = normalize(dto.getCompanyId(), null);
        if (companyId == null) {
            throw new IllegalArgumentException("公司主体不能为空");
        }

        LocalDate billDate = parseDateOrThrow(dto.getDbillDate());
        Integer period = normalizePeriod(dto.getIperiod());
        String voucherType = normalize(dto.getCsign(), DEFAULT_VOUCHER_TYPE);
        List<FinanceVoucherEntryDTO> normalizedEntries = normalizeEntries(dto.getEntries());

        validateCompany(companyId);
        validateVoucherType(voucherType);
        validateEntries(companyId, normalizedEntries);
        Map<String, FinanceAccountSubject> accountSubjects = loadSelectableAccountMap(companyId);

        String makerName = resolveMakerName(currentUser, currentUsername);
        int attachedDocCount = dto.getIdoc() == null ? 0 : Math.max(dto.getIdoc(), 0);
        String lockKey = companyId + "#" + period + "#" + voucherType;
        Object lock = voucherNoLocks.computeIfAbsent(lockKey, unused -> new Object());

        synchronized (lock) {
            int nextVoucherNo = nextVoucherNo(companyId, period, voucherType);
            int finalVoucherNo = nextVoucherNo;
            if (dto.getInoId() != null && dto.getInoId() >= nextVoucherNo) {
                finalVoucherNo = dto.getInoId();
            }

            LocalDateTime billDateTime = billDate.atStartOfDay();
            int signSeq = resolveVoucherTypeSequence(voucherType);

            for (int index = 0; index < normalizedEntries.size(); index++) {
                GlAccvouch row = buildVoucherRow(
                        companyId,
                        period,
                        voucherType,
                        finalVoucherNo,
                        signSeq,
                        billDateTime,
                        attachedDocCount,
                        makerName,
                        dto,
                        normalizedEntries.get(index),
                        index + 1,
                        accountSubjects
                );
                glAccvouchMapper.insert(row);
            }

            postVoucher(companyId, period, voucherType, finalVoucherNo);

            FinanceVoucherSaveResultVO result = new FinanceVoucherSaveResultVO();
            result.setVoucherNo(buildVoucherNo(companyId, period, voucherType, finalVoucherNo));
            result.setCompanyId(companyId);
            result.setIperiod(period);
            result.setCsign(voucherType);
            result.setInoId(finalVoucherNo);
            result.setEntryCount(normalizedEntries.size());
            result.setTotalDebit(sumAmount(normalizedEntries, FinanceVoucherEntryDTO::getMd));
            result.setTotalCredit(sumAmount(normalizedEntries, FinanceVoucherEntryDTO::getMc));
            result.setStatus(STATUS_UNPOSTED);
            return result;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    protected FinanceVoucherSaveResultVO updateVoucher(
            String companyId,
            String voucherNo,
            FinanceVoucherSaveDTO dto,
            Long currentUserId,
            String currentUsername
    ) {
        VoucherKey voucherKey = parseVoucherNo(voucherNo);
        validateVoucherCompany(companyId, voucherKey);

        List<GlAccvouch> existingRows = glAccvouchMapper.selectList(
                Wrappers.<GlAccvouch>lambdaQuery()
                        .eq(GlAccvouch::getCompanyId, voucherKey.companyId())
                        .eq(GlAccvouch::getIperiod, voucherKey.iperiod())
                        .eq(GlAccvouch::getCsign, voucherKey.csign())
                        .eq(GlAccvouch::getInoId, voucherKey.inoId())
                        .orderByAsc(GlAccvouch::getInid, GlAccvouch::getId)
        );
        if (existingRows.isEmpty()) {
            throw new IllegalStateException("凭证不存在");
        }

        GlAccvouch headerRow = existingRows.get(0);
        String status = resolveStatus(headerRow);
        if (!isEditableStatus(status)) {
            throw new IllegalStateException("当前凭证状态不允许修改");
        }

        validateImmutableHeader(dto, voucherKey);
        LocalDate billDate = parseDateOrThrow(dto.getDbillDate());
        if (billDate.getMonthValue() != voucherKey.iperiod()) {
            throw new IllegalArgumentException("修改后的制单日期必须保持在原会计期间内");
        }

        List<FinanceVoucherEntryDTO> normalizedEntries = normalizeEntries(dto.getEntries());
        validateCompany(voucherKey.companyId());
        validateVoucherType(voucherKey.csign());
        validateEntries(voucherKey.companyId(), normalizedEntries);
        Map<String, FinanceAccountSubject> accountSubjects = loadSelectableAccountMap(voucherKey.companyId());

        String makerName = trimToNull(headerRow.getCbill()) == null
                ? resolveMakerName(requireUser(currentUserId), currentUsername)
                : headerRow.getCbill();
        int attachedDocCount = dto.getIdoc() == null ? 0 : Math.max(dto.getIdoc(), 0);
        LocalDateTime billDateTime = billDate.atStartOfDay();
        int signSeq = resolveVoucherTypeSequence(voucherKey.csign());

        glAccvouchMapper.delete(
                Wrappers.<GlAccvouch>lambdaQuery()
                        .eq(GlAccvouch::getCompanyId, voucherKey.companyId())
                        .eq(GlAccvouch::getIperiod, voucherKey.iperiod())
                        .eq(GlAccvouch::getCsign, voucherKey.csign())
                        .eq(GlAccvouch::getInoId, voucherKey.inoId())
        );

        for (int index = 0; index < normalizedEntries.size(); index++) {
            GlAccvouch row = buildVoucherRow(
                    voucherKey.companyId(),
                    voucherKey.iperiod(),
                    voucherKey.csign(),
                    voucherKey.inoId(),
                    signSeq,
                    billDateTime,
                    attachedDocCount,
                    makerName,
                    dto,
                    normalizedEntries.get(index),
                    index + 1,
                    accountSubjects
            );
            glAccvouchMapper.insert(row);
        }

        FinanceVoucherSaveResultVO result = new FinanceVoucherSaveResultVO();
        result.setVoucherNo(buildVoucherNo(voucherKey.companyId(), voucherKey.iperiod(), voucherKey.csign(), voucherKey.inoId()));
        result.setCompanyId(voucherKey.companyId());
        result.setIperiod(voucherKey.iperiod());
        result.setCsign(voucherKey.csign());
        result.setInoId(voucherKey.inoId());
        result.setEntryCount(normalizedEntries.size());
        result.setTotalDebit(sumAmount(normalizedEntries, FinanceVoucherEntryDTO::getMd));
        result.setTotalCredit(sumAmount(normalizedEntries, FinanceVoucherEntryDTO::getMc));
        result.setStatus(STATUS_UNPOSTED);
        return result;
    }

    protected byte[] exportVouchers(FinanceVoucherQueryDTO dto) {
        List<FinanceVoucherSummaryVO> rows = loadVoucherSummaries(dto);
        if (rows.isEmpty()) {
            throw new IllegalStateException("当前没有可导出的凭证");
        }

        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("凭证号,凭证类型,制单日期,会计期间,摘要,制单人,附件张数,借方合计,贷方合计,状态").append('\n');
        for (FinanceVoucherSummaryVO row : rows) {
            builder.append(csvValue(row.getDisplayVoucherNo())).append(',')
                    .append(csvValue(row.getVoucherTypeLabel())).append(',')
                    .append(csvValue(row.getDbillDate())).append(',')
                    .append(csvValue(row.getIperiod() == null ? "" : String.valueOf(row.getIperiod()))).append(',')
                    .append(csvValue(row.getSummary())).append(',')
                    .append(csvValue(row.getCbill())).append(',')
                    .append(csvValue(row.getIdoc() == null ? "" : String.valueOf(row.getIdoc()))).append(',')
                    .append(csvValue(formatAmountText(row.getTotalDebit()))).append(',')
                    .append(csvValue(formatAmountText(row.getTotalCredit()))).append(',')
                    .append(csvValue(row.getStatusLabel()))
                    .append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    protected List<SystemCompany> loadEnabledCompanies() {
        return systemCompanyMapper.selectList(
                Wrappers.<SystemCompany>lambdaQuery()
                        .eq(SystemCompany::getStatus, 1)
                        .orderByAsc(SystemCompany::getCompanyCode, SystemCompany::getCompanyId)
        );
    }

    protected List<FinanceVoucherOptionVO> loadAccountOptions(String companyId) {
        return loadSelectableAccountMap(companyId).values().stream()
                .map(item -> option(item.getSubjectCode(), item.getSubjectCode(), item.getSubjectName()))
                .toList();
    }

    protected Set<String> loadAccountCodeSet(String companyId) {
        return loadSelectableAccountMap(companyId).keySet();
    }

    protected Map<String, FinanceAccountSubject> loadSelectableAccountMap(String companyId) {
        String normalizedCompanyId = trimToNull(companyId);
        if (normalizedCompanyId == null) {
            return Map.of();
        }
        return financeAccountSubjectMapper.selectList(
                        Wrappers.<FinanceAccountSubject>lambdaQuery()
                                .eq(FinanceAccountSubject::getCompanyId, normalizedCompanyId)
                                .eq(FinanceAccountSubject::getStatus, 1)
                                .eq(FinanceAccountSubject::getBclose, 0)
                                .orderByAsc(FinanceAccountSubject::getSubjectCode, FinanceAccountSubject::getId)
                ).stream()
                .filter(item -> trimToNull(item.getSubjectCode()) != null)
                .collect(Collectors.toMap(
                        FinanceAccountSubject::getSubjectCode,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    protected Map<String, String> loadAccountNameMap(String companyId) {
        return loadSelectableAccountMap(companyId).values().stream()
                .collect(Collectors.toMap(
                        FinanceAccountSubject::getSubjectCode,
                        item -> trimToNull(item.getSubjectName()),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    protected List<FinanceVoucherOptionVO> loadCustomerOptions(String companyId) {
        return loadEnabledCustomerMap(companyId).values().stream()
                .map(item -> option(item.getCCusCode(), item.getCCusCode(), resolveCustomerName(item)))
                .toList();
    }

    protected Map<String, FinanceCustomer> loadEnabledCustomerMap(String companyId) {
        String normalizedCompanyId = trimToNull(companyId);
        if (normalizedCompanyId == null) {
            return Map.of();
        }
        return financeCustomerMapper.selectList(
                        Wrappers.<FinanceCustomer>lambdaQuery()
                                .eq(FinanceCustomer::getCompanyId, normalizedCompanyId)
                                .isNull(FinanceCustomer::getDEndDate)
                                .orderByAsc(FinanceCustomer::getCCusCode)
                ).stream()
                .filter(item -> trimToNull(item.getCCusCode()) != null)
                .collect(Collectors.toMap(
                        FinanceCustomer::getCCusCode,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    protected List<FinanceVoucherOptionVO> loadSupplierOptions(String companyId) {
        return loadEnabledSupplierMap(companyId).values().stream()
                .map(item -> option(item.getCVenCode(), item.getCVenCode(), resolveVendorName(item)))
                .toList();
    }

    protected Map<String, FinanceVendor> loadEnabledSupplierMap(String companyId) {
        String normalizedCompanyId = trimToNull(companyId);
        if (normalizedCompanyId == null) {
            return Map.of();
        }
        return financeVendorMapper.selectList(
                        Wrappers.<FinanceVendor>lambdaQuery()
                                .eq(FinanceVendor::getCompanyId, normalizedCompanyId)
                                .isNull(FinanceVendor::getDEndDate)
                                .orderByAsc(FinanceVendor::getCVenCode)
                ).stream()
                .filter(item -> trimToNull(item.getCVenCode()) != null)
                .collect(Collectors.toMap(
                        FinanceVendor::getCVenCode,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    protected List<FinanceVoucherOptionVO> loadProjectClassOptions(String companyId) {
        String normalizedCompanyId = trimToNull(companyId);
        if (normalizedCompanyId == null) {
            return List.of();
        }
        return financeProjectClassMapper.selectList(
                        Wrappers.<FinanceProjectClass>lambdaQuery()
                                .eq(FinanceProjectClass::getCompanyId, normalizedCompanyId)
                                .eq(FinanceProjectClass::getStatus, 1)
                                .orderByAsc(FinanceProjectClass::getSortOrder, FinanceProjectClass::getProjectClassCode, FinanceProjectClass::getId)
                ).stream()
                .map(item -> option(item.getProjectClassCode(), item.getProjectClassCode(), item.getProjectClassName()))
                .toList();
    }

    protected Map<String, FinanceProjectClass> loadEnabledProjectClassMap(String companyId) {
        String normalizedCompanyId = trimToNull(companyId);
        if (normalizedCompanyId == null) {
            return Map.of();
        }
        return financeProjectClassMapper.selectList(
                        Wrappers.<FinanceProjectClass>lambdaQuery()
                                .eq(FinanceProjectClass::getCompanyId, normalizedCompanyId)
                                .eq(FinanceProjectClass::getStatus, 1)
                ).stream()
                .collect(Collectors.toMap(
                        FinanceProjectClass::getProjectClassCode,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    protected List<FinanceVoucherOptionVO> loadProjectOptions(String companyId) {
        Map<String, FinanceProjectClass> classMap = loadEnabledProjectClassMap(companyId);
        if (classMap.isEmpty()) {
            return List.of();
        }
        return loadSelectableProjects(companyId).values().stream()
                .filter(item -> classMap.containsKey(item.getCitemccode()))
                .map(item -> option(item.getCitemcode(), item.getCitemcode(), item.getCitemname(), item.getCitemccode()))
                .toList();
    }

    protected Map<String, FinanceProjectArchive> loadSelectableProjects(String companyId) {
        String normalizedCompanyId = trimToNull(companyId);
        if (normalizedCompanyId == null) {
            return Map.of();
        }
        return financeProjectArchiveMapper.selectList(
                        Wrappers.<FinanceProjectArchive>lambdaQuery()
                                .eq(FinanceProjectArchive::getCompanyId, normalizedCompanyId)
                                .eq(FinanceProjectArchive::getStatus, 1)
                                .eq(FinanceProjectArchive::getBclose, 0)
                                .orderByAsc(FinanceProjectArchive::getSortOrder, FinanceProjectArchive::getCitemcode, FinanceProjectArchive::getId)
                ).stream()
                .collect(Collectors.toMap(
                        FinanceProjectArchive::getCitemcode,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    protected List<SystemDepartment> loadEnabledDepartments() {
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .eq(SystemDepartment::getStatus, 1)
                        .orderByAsc(SystemDepartment::getSortOrder, SystemDepartment::getId)
        );
    }

    protected List<User> loadEnabledUsers() {
        return userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getStatus, 1)
                        .orderByAsc(User::getId)
        );
    }

    protected List<FinanceVoucherSummaryVO> loadVoucherSummaries(FinanceVoucherQueryDTO dto) {
        FinanceVoucherQueryDTO normalizedDto = dto == null ? new FinanceVoucherQueryDTO() : dto;
        String companyId = normalize(normalizedDto.getCompanyId(), null);
        if (companyId == null) {
            throw new IllegalArgumentException("公司主体不能为空");
        }
        validateCompany(companyId);
        if (trimToNull(normalizedDto.getCsign()) != null) {
            validateVoucherType(normalizedDto.getCsign());
        }

        MonthRange monthRange = resolveMonthRange(normalizedDto);
        List<GlAccvouch> rows = glAccvouchMapper.selectList(
                Wrappers.<GlAccvouch>lambdaQuery()
                        .eq(GlAccvouch::getCompanyId, companyId)
                        .eq(trimToNull(normalizedDto.getCsign()) != null, GlAccvouch::getCsign, trimToNull(normalizedDto.getCsign()))
                        .ge(monthRange != null, GlAccvouch::getDbillDate, monthRange == null ? null : monthRange.start())
                        .lt(monthRange != null, GlAccvouch::getDbillDate, monthRange == null ? null : monthRange.endExclusive())
                        .orderByDesc(GlAccvouch::getDbillDate, GlAccvouch::getInoId)
                        .orderByAsc(GlAccvouch::getInid, GlAccvouch::getId)
        );

        Map<VoucherKey, List<GlAccvouch>> grouped = rows.stream()
                .collect(Collectors.groupingBy(
                        item -> new VoucherKey(item.getCompanyId(), item.getIperiod(), item.getCsign(), item.getInoId()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        String voucherNoKeyword = lower(trimToNull(normalizedDto.getVoucherNo()));
        String summaryKeyword = lower(trimToNull(normalizedDto.getSummary()));

        return grouped.values().stream()
                .filter(items -> summaryKeyword == null || containsSummary(items, summaryKeyword))
                .map(this::toSummaryVO)
                .filter(item -> voucherNoKeyword == null || lower(item.getDisplayVoucherNo()).contains(voucherNoKeyword))
                .sorted(Comparator
                        .comparing(FinanceVoucherSummaryVO::getDbillDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(FinanceVoucherSummaryVO::getInoId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    protected FinanceVoucherSummaryVO toSummaryVO(List<GlAccvouch> rows) {
        GlAccvouch headerRow = rows.get(0);
        FinanceVoucherSummaryVO summary = new FinanceVoucherSummaryVO();
        String status = resolveStatus(headerRow);
        summary.setVoucherNo(buildVoucherNo(headerRow.getCompanyId(), headerRow.getIperiod(), headerRow.getCsign(), headerRow.getInoId()));
        summary.setDisplayVoucherNo(buildDisplayVoucherNo(headerRow.getCsign(), headerRow.getInoId()));
        summary.setCompanyId(headerRow.getCompanyId());
        summary.setIperiod(headerRow.getIperiod());
        summary.setCsign(headerRow.getCsign());
        summary.setVoucherTypeLabel(resolveVoucherTypeLabel(headerRow.getCsign()));
        summary.setInoId(headerRow.getInoId());
        summary.setDbillDate(formatDate(headerRow.getDbillDate()));
        summary.setSummary(resolveVoucherSummary(rows));
        summary.setCbill(headerRow.getCbill());
        summary.setIdoc(headerRow.getIdoc());
        summary.setStatus(status);
        summary.setStatusLabel(resolveStatusLabel(status));
        summary.setEditable(isEditableStatus(status));
        summary.setEntryCount(rows.size());
        summary.setTotalDebit(sumAmount(rows, true));
        summary.setTotalCredit(sumAmount(rows, false));
        return summary;
    }

    protected FinanceVoucherOptionVO toCompanyOption(SystemCompany company) {
        String companyCode = trimToNull(company.getCompanyCode());
        String companyName = trimToNull(company.getCompanyName());
        if (companyCode == null) {
            return option(company.getCompanyId(), company.getCompanyId(), companyName);
        }
        return option(company.getCompanyId(), companyCode, companyName);
    }

    protected FinanceVoucherOptionVO toDepartmentOption(SystemDepartment department) {
        String deptCode = trimToNull(department.getDeptCode());
        String deptName = trimToNull(department.getDeptName());
        return option(String.valueOf(department.getId()), normalize(deptCode, String.valueOf(department.getId())), deptName);
    }

    protected FinanceVoucherOptionVO toEmployeeOption(User user) {
        String displayName = trimToNull(user.getName()) == null ? normalize(user.getUsername(), "未命名员工") : user.getName();
        return option(String.valueOf(user.getId()), String.valueOf(user.getId()), displayName);
    }

    protected List<FinanceVoucherOptionVO> toOptions(List<OptionSeed> seeds) {
        return seeds.stream().map(seed -> option(seed.value(), seed.label())).toList();
    }

    protected FinanceVoucherOptionVO option(String value, String label) {
        return option(value, null, null, label, null);
    }

    protected FinanceVoucherOptionVO option(String value, String code, String name) {
        return option(value, code, name, null, null);
    }

    protected FinanceVoucherOptionVO option(String value, String code, String name, String parentValue) {
        return option(value, code, name, null, parentValue);
    }

    protected FinanceVoucherOptionVO option(String value, String code, String name, String explicitLabel, String parentValue) {
        FinanceVoucherOptionVO option = new FinanceVoucherOptionVO();
        option.setValue(value);
        option.setCode(code);
        option.setName(name);
        option.setLabel(buildOptionLabel(code, name, explicitLabel, value));
        option.setParentValue(parentValue);
        return option;
    }

    protected String buildOptionLabel(String code, String name, String explicitLabel, String fallbackValue) {
        if (trimToNull(explicitLabel) != null) {
            return explicitLabel;
        }
        if (trimToNull(code) != null && trimToNull(name) != null) {
            return code + "  " + name;
        }
        if (trimToNull(name) != null) {
            return name;
        }
        if (trimToNull(code) != null) {
            return code;
        }
        return normalize(fallbackValue, "");
    }

    protected User requireUser(Long currentUserId) {
        User user = userMapper.selectById(currentUserId);
        if (user == null) {
            throw new IllegalStateException("当前登录用户不存在");
        }
        return user;
    }

    protected String resolveDefaultCompanyId(String companyId, User currentUser, List<SystemCompany> companies) {
        String normalizedCompanyId = trimToNull(companyId);
        Set<String> companyIds = companies.stream().map(SystemCompany::getCompanyId).collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedCompanyId != null && companyIds.contains(normalizedCompanyId)) {
            return normalizedCompanyId;
        }
        String userCompanyId = trimToNull(currentUser.getCompanyId());
        if (userCompanyId != null && companyIds.contains(userCompanyId)) {
            return userCompanyId;
        }
        return companies.isEmpty() ? null : companies.get(0).getCompanyId();
    }

    protected <T> FinanceVoucherPageVO<T> buildPage(List<T> rows, Integer page, Integer pageSize) {
        int safePage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int safePageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        int total = rows == null ? 0 : rows.size();
        int start = Math.min((safePage - 1) * safePageSize, total);
        int end = Math.min(start + safePageSize, total);

        FinanceVoucherPageVO<T> result = new FinanceVoucherPageVO<>();
        result.setTotal(total);
        result.setPage(safePage);
        result.setPageSize(safePageSize);
        result.setItems(rows == null ? List.of() : new ArrayList<>(rows.subList(start, end)));
        return result;
    }

    protected int nextVoucherNo(String companyId, Integer period, String voucherType) {
        if (trimToNull(companyId) == null || period == null || trimToNull(voucherType) == null) {
            return 1;
        }
        List<Object> values = glAccvouchMapper.selectObjs(
                Wrappers.<GlAccvouch>query()
                        .select("ino_id")
                        .eq("company_id", companyId)
                        .eq("iperiod", period)
                        .eq("csign", voucherType)
                        .orderByDesc("ino_id")
                        .last("limit 1")
        );
        if (values.isEmpty() || values.get(0) == null) {
            return 1;
        }
        return ((Number) values.get(0)).intValue() + 1;
    }

    protected GlAccvouch buildVoucherRow(
            String companyId,
            Integer period,
            String voucherType,
            Integer voucherNo,
            int signSeq,
            LocalDateTime billDateTime,
            int attachedDocCount,
            String makerName,
            FinanceVoucherSaveDTO dto,
            FinanceVoucherEntryDTO entry,
            int rowNo,
            Map<String, FinanceAccountSubject> accountSubjects
    ) {
        GlAccvouch row = new GlAccvouch();
        FinanceAccountSubject accountSubject = accountSubjects.get(trimToNull(entry.getCcode()));
        row.setCompanyId(companyId);
        row.setIperiod(period);
        row.setCsign(voucherType);
        row.setIsignseq(signSeq);
        row.setInoId(voucherNo);
        row.setInid(rowNo);
        row.setDbillDate(billDateTime);
        row.setIdoc(attachedDocCount);
        row.setCbill(makerName);
        row.setCcheck(null);
        row.setCbook(null);
        row.setIbook(0);
        row.setIflag(0);
        row.setCtext1(trimToNull(dto.getCtext1()));
        row.setCtext2(trimToNull(dto.getCtext2()));
        row.setCdigest(trimToNull(entry.getCdigest()));
        row.setCcode(trimToNull(entry.getCcode()));
        row.setCcodeName(accountSubject == null ? null : trimToNull(accountSubject.getSubjectName()));
        row.setCdeptId(trimToNull(entry.getCdeptId()));
        row.setCpersonId(trimToNull(entry.getCpersonId()));
        row.setCcusId(trimToNull(entry.getCcusId()));
        row.setCsupId(trimToNull(entry.getCsupId()));
        row.setCitemClass(trimToNull(entry.getCitemClass()));
        row.setCitemId(trimToNull(entry.getCitemId()));
        row.setCexchName(normalize(entry.getCexchName(), DEFAULT_CURRENCY));
        row.setNfrat(defaultDecimal(entry.getNfrat(), DEFAULT_RATE));
        row.setMd(normalizeAmount(entry.getMd()));
        row.setMc(normalizeAmount(entry.getMc()));
        row.setMdF(normalizeAmount(entry.getMd()));
        row.setMcF(normalizeAmount(entry.getMc()));
        row.setNdS(normalizeNullableQuantity(entry.getNdS()));
        row.setNcS(normalizeNullableQuantity(entry.getNcS()));
        return row;
    }

    protected VoucherKey parseVoucherNo(String voucherNo) {
        String normalizedVoucherNo = trimToNull(voucherNo);
        if (normalizedVoucherNo == null) {
            throw new IllegalArgumentException("凭证号不能为空");
        }
        String[] parts = normalizedVoucherNo.split(VOUCHER_NO_SEPARATOR);
        if (parts.length != 4) {
            throw new IllegalArgumentException("凭证号格式不正确");
        }
        try {
            return new VoucherKey(parts[0], Integer.parseInt(parts[1]), parts[2], Integer.parseInt(parts[3]));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("凭证号格式不正确");
        }
    }

    protected String buildVoucherNo(String companyId, Integer period, String voucherType, Integer inoId) {
        return companyId + VOUCHER_NO_SEPARATOR + period + VOUCHER_NO_SEPARATOR + voucherType + VOUCHER_NO_SEPARATOR + inoId;
    }

    protected String buildDisplayVoucherNo(String voucherType, Integer inoId) {
        int number = inoId == null ? 0 : inoId;
        return normalize(voucherType, DEFAULT_VOUCHER_TYPE) + "-" + String.format("%04d", Math.max(number, 0));
    }

    protected FinanceVoucherEntryVO toEntryVO(GlAccvouch row, Map<String, String> accountNameMap) {
        FinanceVoucherEntryVO entry = new FinanceVoucherEntryVO();
        entry.setInid(row.getInid());
        entry.setCdigest(row.getCdigest());
        entry.setCcode(row.getCcode());
        entry.setCcodeName(normalize(row.getCcodeName(), accountNameMap.get(row.getCcode())));
        entry.setCdeptId(row.getCdeptId());
        entry.setCpersonId(row.getCpersonId());
        entry.setCcusId(row.getCcusId());
        entry.setCsupId(row.getCsupId());
        entry.setCitemClass(row.getCitemClass());
        entry.setCitemId(row.getCitemId());
        entry.setCexchName(normalize(row.getCexchName(), DEFAULT_CURRENCY));
        entry.setNfrat(defaultDecimal(row.getNfrat(), DEFAULT_RATE));
        entry.setMd(normalizeAmount(row.getMd()));
        entry.setMc(normalizeAmount(row.getMc()));
        entry.setNdS(normalizeNullableQuantity(row.getNdS()));
        entry.setNcS(normalizeNullableQuantity(row.getNcS()));
        return entry;
    }

    protected String resolveStatus(GlAccvouch row) {
        if (Objects.equals(row.getIbook(), 1)) {
            return STATUS_POSTED;
        }
        if (trimToNull(row.getCcheck()) != null) {
            return STATUS_REVIEWED;
        }
        return STATUS_UNPOSTED;
    }

    protected String resolveStatusLabel(String status) {
        return switch (normalize(status, STATUS_UNPOSTED)) {
            case STATUS_POSTED -> "已记账";
            case STATUS_REVIEWED -> "已审核";
            default -> "未记账";
        };
    }

    protected boolean isEditableStatus(String status) {
        return Objects.equals(normalize(status, STATUS_UNPOSTED), STATUS_UNPOSTED);
    }

    protected String resolveVoucherTypeLabel(String voucherType) {
        return VOUCHER_TYPE_SEEDS.stream()
                .filter(item -> Objects.equals(item.value(), voucherType))
                .map(OptionSeed::label)
                .findFirst()
                .orElse(normalize(voucherType, DEFAULT_VOUCHER_TYPE));
    }

    protected String resolveVoucherSummary(List<GlAccvouch> rows) {
        List<String> digests = rows.stream()
                .map(GlAccvouch::getCdigest)
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (digests.isEmpty()) {
            return "-";
        }
        if (digests.size() == 1) {
            return digests.get(0);
        }
        return digests.get(0) + " 等" + digests.size() + "条分录";
    }

    protected boolean containsSummary(List<GlAccvouch> rows, String keyword) {
        return rows.stream()
                .map(GlAccvouch::getCdigest)
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .map(this::lower)
                .anyMatch(item -> item.contains(keyword));
    }

    protected BigDecimal sumAmount(List<GlAccvouch> rows, boolean debit) {
        return rows.stream()
                .map(item -> debit ? item.getMd() : item.getMc())
                .map(this::normalizeAmount)
                .reduce(ZERO, BigDecimal::add);
    }

    protected BigDecimal sumAmount(List<FinanceVoucherEntryDTO> rows, AmountGetter getter) {
        return rows.stream()
                .map(getter::get)
                .map(this::normalizeAmount)
                .reduce(ZERO, BigDecimal::add);
    }

    protected void validateCompany(String companyId) {
        Long count = systemCompanyMapper.selectCount(
                Wrappers.<SystemCompany>lambdaQuery()
                        .eq(SystemCompany::getCompanyId, companyId)
                        .eq(SystemCompany::getStatus, 1)
        );
        if (count == null || count == 0) {
            throw new IllegalArgumentException("公司主体不存在或已停用");
        }
    }

    protected void validateVoucherType(String voucherType) {
        if (VOUCHER_TYPE_SEEDS.stream().noneMatch(item -> Objects.equals(item.value(), voucherType))) {
            throw new IllegalArgumentException("凭证类别不合法");
        }
    }

    protected List<FinanceVoucherEntryDTO> normalizeEntries(List<FinanceVoucherEntryDTO> entries) {
        if (entries == null) {
            return List.of();
        }
        List<FinanceVoucherEntryDTO> normalizedEntries = new ArrayList<>();
        for (FinanceVoucherEntryDTO entry : entries) {
            if (entry == null || isEmptyEntry(entry)) {
                continue;
            }
            FinanceVoucherEntryDTO normalizedEntry = new FinanceVoucherEntryDTO();
            normalizedEntry.setInid(entry.getInid());
            normalizedEntry.setCdigest(trimToNull(entry.getCdigest()));
            normalizedEntry.setCcode(trimToNull(entry.getCcode()));
            normalizedEntry.setCdeptId(trimToNull(entry.getCdeptId()));
            normalizedEntry.setCpersonId(trimToNull(entry.getCpersonId()));
            normalizedEntry.setCcusId(trimToNull(entry.getCcusId()));
            normalizedEntry.setCsupId(trimToNull(entry.getCsupId()));
            normalizedEntry.setCitemClass(trimToNull(entry.getCitemClass()));
            normalizedEntry.setCitemId(trimToNull(entry.getCitemId()));
            normalizedEntry.setCexchName(normalize(entry.getCexchName(), DEFAULT_CURRENCY));
            normalizedEntry.setNfrat(defaultDecimal(entry.getNfrat(), DEFAULT_RATE));
            normalizedEntry.setMd(normalizeNullableAmount(entry.getMd()));
            normalizedEntry.setMc(normalizeNullableAmount(entry.getMc()));
            normalizedEntry.setNdS(normalizeNullableQuantity(entry.getNdS()));
            normalizedEntry.setNcS(normalizeNullableQuantity(entry.getNcS()));
            normalizedEntries.add(normalizedEntry);
        }
        return normalizedEntries;
    }

    protected boolean isEmptyEntry(FinanceVoucherEntryDTO entry) {
        return trimToNull(entry.getCdigest()) == null
                && trimToNull(entry.getCcode()) == null
                && trimToNull(entry.getCdeptId()) == null
                && trimToNull(entry.getCpersonId()) == null
                && trimToNull(entry.getCcusId()) == null
                && trimToNull(entry.getCsupId()) == null
                && trimToNull(entry.getCitemClass()) == null
                && trimToNull(entry.getCitemId()) == null
                && trimToNull(entry.getCexchName()) == null
                && isNullOrZero(entry.getNfrat())
                && isNullOrZero(entry.getMd())
                && isNullOrZero(entry.getMc())
                && isNullOrZero(entry.getNdS())
                && isNullOrZero(entry.getNcS());
    }

    protected void validateEntries(String companyId, List<FinanceVoucherEntryDTO> entries) {
        if (entries.size() < 2) {
            throw new IllegalArgumentException("至少需要两条有效分录");
        }

        Map<String, String> departments = loadEnabledDepartments().stream()
                .collect(Collectors.toMap(item -> String.valueOf(item.getId()), SystemDepartment::getDeptName));
        Map<String, String> employees = loadEnabledUsers().stream()
                .collect(Collectors.toMap(item -> String.valueOf(item.getId()), this::resolveUserName));
        Set<String> accounts = loadAccountCodeSet(companyId);
        Set<String> customers = loadEnabledCustomerMap(companyId).keySet();
        Set<String> suppliers = loadEnabledSupplierMap(companyId).keySet();
        Map<String, FinanceProjectClass> projectClasses = loadEnabledProjectClassMap(companyId);
        Map<String, FinanceProjectArchive> projects = loadSelectableProjects(companyId);
        Set<String> currencies = CURRENCY_SEEDS.stream().map(OptionSeed::value).collect(Collectors.toCollection(LinkedHashSet::new));

        BigDecimal totalDebit = ZERO;
        BigDecimal totalCredit = ZERO;
        for (int index = 0; index < entries.size(); index++) {
            FinanceVoucherEntryDTO entry = entries.get(index);
            int rowNo = index + 1;
            if (trimToNull(entry.getCdigest()) == null) {
                throw new IllegalArgumentException("第 " + rowNo + " 行摘要不能为空");
            }
            if (trimToNull(entry.getCcode()) == null || !accounts.contains(entry.getCcode())) {
                throw new IllegalArgumentException("第 " + rowNo + " 行科目不存在");
            }
            validateSelectable(entry.getCdeptId(), departments.keySet(), "部门", rowNo);
            validateSelectable(entry.getCpersonId(), employees.keySet(), "人员", rowNo);
            validateSelectable(entry.getCcusId(), customers, "客户", rowNo);
            validateSelectable(entry.getCsupId(), suppliers, "供应商", rowNo);
            validateSelectable(entry.getCitemClass(), projectClasses.keySet(), "项目分类", rowNo);
            validateSelectable(entry.getCitemId(), projects.keySet(), "项目", rowNo);
            validateProjectSelection(entry, projectClasses, projects, rowNo);

            if (!currencies.contains(normalize(entry.getCexchName(), DEFAULT_CURRENCY))) {
                throw new IllegalArgumentException("第 " + rowNo + " 行币种不合法");
            }
            if (defaultDecimal(entry.getNfrat(), DEFAULT_RATE).compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("第 " + rowNo + " 行汇率必须大于 0");
            }

            BigDecimal debit = normalizeAmount(entry.getMd());
            BigDecimal credit = normalizeAmount(entry.getMc());
            if (debit.compareTo(BigDecimal.ZERO) > 0 && credit.compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalArgumentException("第 " + rowNo + " 行借贷不能同时填写");
            }
            if (debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0) {
                throw new IllegalArgumentException("第 " + rowNo + " 行借方或贷方至少填写一项");
            }

            BigDecimal qtyDebit = normalizeNullableQuantity(entry.getNdS());
            BigDecimal qtyCredit = normalizeNullableQuantity(entry.getNcS());
            if (qtyDebit != null && qtyDebit.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("第 " + rowNo + " 行数量借方不能为负数");
            }
            if (qtyCredit != null && qtyCredit.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("第 " + rowNo + " 行数量贷方不能为负数");
            }
            if (qtyDebit != null && qtyCredit != null && qtyDebit.compareTo(BigDecimal.ZERO) > 0 && qtyCredit.compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalArgumentException("第 " + rowNo + " 行数量借贷不能同时填写");
            }

            totalDebit = totalDebit.add(debit);
            totalCredit = totalCredit.add(credit);
        }

        if (totalDebit.compareTo(totalCredit) != 0) {
            throw new IllegalArgumentException("凭证借贷不平衡，无法保存");
        }
    }

    protected void validateSelectable(String value, Collection<String> validValues, String fieldName, int rowNo) {
        String normalizedValue = trimToNull(value);
        if (normalizedValue == null) {
            return;
        }
        if (!validValues.contains(normalizedValue)) {
            throw new IllegalArgumentException("第 " + rowNo + " 行" + fieldName + "不存在或当前不可用");
        }
    }

    protected void validateProjectSelection(
            FinanceVoucherEntryDTO entry,
            Map<String, FinanceProjectClass> projectClasses,
            Map<String, FinanceProjectArchive> projects,
            int rowNo
    ) {
        String projectClassCode = trimToNull(entry.getCitemClass());
        String projectCode = trimToNull(entry.getCitemId());
        if (projectCode == null) {
            return;
        }
        if (projectClassCode == null) {
            throw new IllegalArgumentException("第 " + rowNo + " 行选择项目时必须同时选择项目分类");
        }
        FinanceProjectArchive project = projects.get(projectCode);
        if (project == null) {
            throw new IllegalArgumentException("第 " + rowNo + " 行项目不存在");
        }
        if (!projectClasses.containsKey(projectClassCode)) {
            throw new IllegalArgumentException("第 " + rowNo + " 行项目分类不存在");
        }
        if (!Objects.equals(trimToNull(project.getCitemccode()), projectClassCode)) {
            throw new IllegalArgumentException("第 " + rowNo + " 行项目分类与项目归属不匹配");
        }
    }

    protected String resolveUserName(User user) {
        return trimToNull(user.getName()) == null ? normalize(user.getUsername(), "未命名员工") : user.getName();
    }

    protected String resolveCustomerName(FinanceCustomer customer) {
        if (trimToNull(customer.getCCusName()) != null) {
            return customer.getCCusName().trim();
        }
        return normalize(customer.getCCusAbbName(), customer.getCCusCode());
    }

    protected String resolveVendorName(FinanceVendor vendor) {
        if (trimToNull(vendor.getCVenName()) != null) {
            return vendor.getCVenName().trim();
        }
        return normalize(vendor.getCVenAbbName(), vendor.getCVenCode());
    }

    protected String resolveMakerName(User currentUser, String currentUsername) {
        if (trimToNull(currentUser.getName()) != null) {
            return currentUser.getName().trim();
        }
        return normalize(currentUsername, "财务制单员");
    }

    protected void validateVoucherCompany(String companyId, VoucherKey voucherKey) {
        String normalizedCompanyId = normalize(companyId, null);
        if (normalizedCompanyId == null) {
            throw new IllegalArgumentException("公司主体不能为空");
        }
        if (!Objects.equals(normalizedCompanyId, voucherKey.companyId())) {
            throw new IllegalArgumentException("当前公司上下文与凭证不匹配");
        }
    }

    protected void validateImmutableHeader(FinanceVoucherSaveDTO dto, VoucherKey voucherKey) {
        if (dto == null) {
            throw new IllegalArgumentException("凭证数据不能为空");
        }
        String payloadCompanyId = normalize(dto.getCompanyId(), voucherKey.companyId());
        if (!Objects.equals(payloadCompanyId, voucherKey.companyId())) {
            throw new IllegalArgumentException("修改时不允许变更公司");
        }
        Integer payloadPeriod = dto.getIperiod() == null ? voucherKey.iperiod() : dto.getIperiod();
        if (!Objects.equals(payloadPeriod, voucherKey.iperiod())) {
            throw new IllegalArgumentException("修改时不允许变更会计期间");
        }
        String payloadVoucherType = normalize(dto.getCsign(), voucherKey.csign());
        if (!Objects.equals(payloadVoucherType, voucherKey.csign())) {
            throw new IllegalArgumentException("修改时不允许变更凭证类型");
        }
        Integer payloadVoucherNo = dto.getInoId() == null ? voucherKey.inoId() : dto.getInoId();
        if (!Objects.equals(payloadVoucherNo, voucherKey.inoId())) {
            throw new IllegalArgumentException("修改时不允许变更凭证编号");
        }
    }

    protected LocalDate parseDateOrDefault(String value, LocalDate defaultValue) {
        if (trimToNull(value) == null) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            return defaultValue;
        }
    }

    protected LocalDate parseDateOrThrow(String value) {
        if (trimToNull(value) == null) {
            throw new IllegalArgumentException("制单日期不能为空");
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("制单日期格式不正确");
        }
    }

    protected YearMonth parseMonthOrThrow(String value, String fieldName) {
        String normalizedValue = trimToNull(value);
        if (normalizedValue == null) {
            return null;
        }
        try {
            return YearMonth.parse(normalizedValue, MONTH_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(fieldName + "格式不正确，正确格式为 yyyy-MM");
        }
    }

    protected MonthRange resolveMonthRange(FinanceVoucherQueryDTO dto) {
        if (dto == null) {
            return null;
        }
        YearMonth exactMonth = parseMonthOrThrow(dto.getBillMonth(), "制单月份");
        if (exactMonth != null) {
            return new MonthRange(exactMonth.atDay(1).atStartOfDay(), exactMonth.plusMonths(1).atDay(1).atStartOfDay());
        }
        YearMonth monthFrom = parseMonthOrThrow(dto.getBillMonthFrom(), "制单月份起");
        YearMonth monthTo = parseMonthOrThrow(dto.getBillMonthTo(), "制单月份止");
        if (monthFrom == null && monthTo == null) {
            return null;
        }
        if (monthFrom != null && monthTo != null && monthFrom.isAfter(monthTo)) {
            throw new IllegalArgumentException("制单月份起不能晚于制单月份止");
        }
        YearMonth startMonth = monthFrom == null ? monthTo : monthFrom;
        YearMonth endMonth = monthTo == null ? monthFrom : monthTo;
        return new MonthRange(startMonth.atDay(1).atStartOfDay(), endMonth.plusMonths(1).atDay(1).atStartOfDay());
    }

    protected Integer normalizePeriod(Integer period) {
        if (period == null || period < 1 || period > 12) {
            throw new IllegalArgumentException("会计期间必须在 1 到 12 之间");
        }
        return period;
    }

    protected int resolveVoucherTypeSequence(String voucherType) {
        for (int index = 0; index < VOUCHER_TYPE_SEEDS.size(); index++) {
            if (Objects.equals(VOUCHER_TYPE_SEEDS.get(index).value(), voucherType)) {
                return index + 1;
            }
        }
        return 1;
    }

    protected BigDecimal normalizeAmount(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    protected BigDecimal normalizeNullableAmount(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金额不能为负数");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    protected BigDecimal normalizeNullableQuantity(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }

    protected BigDecimal defaultDecimal(BigDecimal value, BigDecimal defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    protected boolean isNullOrZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    protected String normalize(String value, String defaultValue) {
        String normalizedValue = trimToNull(value);
        return normalizedValue == null ? defaultValue : normalizedValue;
    }

    protected String lower(String value) {
        return value == null ? null : value.toLowerCase();
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    protected String csvValue(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    protected String formatDate(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.toLocalDate().format(DATE_FORMATTER);
    }

    protected String formatAmountText(BigDecimal value) {
        return normalizeAmount(value).toPlainString();
    }

    // Placeholder for the future posting pipeline.
    protected void postVoucher(String companyId, Integer period, String voucherType, Integer inoId) {
        // Intentionally left blank in phase one.
    }

    private record MonthRange(LocalDateTime start, LocalDateTime endExclusive) {
    }

    private record VoucherKey(String companyId, Integer iperiod, String csign, Integer inoId) {
    }

    private record OptionSeed(String value, String label) {
    }

    @FunctionalInterface
    private interface AmountGetter {
        BigDecimal get(FinanceVoucherEntryDTO entry);
    }
}
