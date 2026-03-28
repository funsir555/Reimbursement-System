package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.FinanceVoucherDetailVO;
import com.finex.auth.dto.FinanceVoucherEntryDTO;
import com.finex.auth.dto.FinanceVoucherEntryVO;
import com.finex.auth.dto.FinanceVoucherMetaVO;
import com.finex.auth.dto.FinanceVoucherOptionVO;
import com.finex.auth.dto.FinanceVoucherSaveDTO;
import com.finex.auth.dto.FinanceVoucherSaveResultVO;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.FinanceVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanceVoucherServiceImpl implements FinanceVoucherService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String DEFAULT_VOUCHER_TYPE = "记";
    private static final String DEFAULT_CURRENCY = "CNY";
    private static final BigDecimal DEFAULT_RATE = BigDecimal.ONE;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);
    private static final String STATUS_UNPOSTED = "UNPOSTED";
    private static final String VOUCHER_NO_SEPARATOR = "~";

    private static final List<OptionSeed> VOUCHER_TYPE_SEEDS = List.of(
            new OptionSeed("记", "记账凭证"),
            new OptionSeed("收", "收款凭证"),
            new OptionSeed("付", "付款凭证"),
            new OptionSeed("转", "转账凭证")
    );

    private static final List<OptionSeed> CURRENCY_SEEDS = List.of(
            new OptionSeed("CNY", "人民币"),
            new OptionSeed("USD", "美元"),
            new OptionSeed("EUR", "欧元")
    );

    private static final List<OptionSeed> ACCOUNT_SEEDS = List.of(
            new OptionSeed("1001", "1001 库存现金"),
            new OptionSeed("1002", "1002 银行存款"),
            new OptionSeed("1122", "1122 应收账款"),
            new OptionSeed("2202", "2202 应付账款"),
            new OptionSeed("2221", "2221 应交税费"),
            new OptionSeed("5001", "5001 主营业务收入"),
            new OptionSeed("5601", "5601 销售费用"),
            new OptionSeed("6602", "6602 管理费用")
    );

    private static final List<OptionSeed> CUSTOMER_SEEDS = List.of(
            new OptionSeed("CUST001", "华东渠道客户"),
            new OptionSeed("CUST002", "总部直营客户"),
            new OptionSeed("CUST003", "战略合作客户")
    );

    private static final List<OptionSeed> SUPPLIER_SEEDS = List.of(
            new OptionSeed("SUP001", "上海办公采购供应商"),
            new OptionSeed("SUP002", "差旅服务供应商"),
            new OptionSeed("SUP003", "市场活动供应商")
    );

    private static final List<OptionSeed> PROJECT_CLASS_SEEDS = List.of(
            new OptionSeed("MARKET", "市场项目"),
            new OptionSeed("DELIVERY", "交付项目"),
            new OptionSeed("INTERNAL", "内部管理项目")
    );

    private static final List<OptionSeed> PROJECT_SEEDS = List.of(
            new OptionSeed("PRJ001", "华东增长专项"),
            new OptionSeed("PRJ002", "渠道优化项目"),
            new OptionSeed("PRJ003", "总部数字化项目")
    );

    private final GlAccvouchMapper glAccvouchMapper;
    private final SystemCompanyMapper systemCompanyMapper;
    private final SystemDepartmentMapper systemDepartmentMapper;
    private final UserMapper userMapper;

    private final ConcurrentHashMap<String, Object> voucherNoLocks = new ConcurrentHashMap<>();

    @Override
    public FinanceVoucherMetaVO getMeta(
            Long currentUserId,
            String currentUsername,
            String companyId,
            String billDate,
            String csign
    ) {
        User currentUser = requireUser(currentUserId);
        List<SystemCompany> companies = loadEnabledCompanies();
        List<SystemDepartment> departments = loadEnabledDepartments();
        List<User> employees = loadEnabledUsers();

        String effectiveCompanyId = resolveDefaultCompanyId(companyId, currentUser, companies);
        LocalDate effectiveBillDate = parseDateOrDefault(billDate, LocalDate.now());
        String effectiveVoucherType = normalize(csign, DEFAULT_VOUCHER_TYPE);

        FinanceVoucherMetaVO meta = new FinanceVoucherMetaVO();
        meta.setCompanyOptions(companies.stream().map(this::toCompanyOption).toList());
        meta.setDepartmentOptions(departments.stream().map(this::toDepartmentOption).toList());
        meta.setEmployeeOptions(employees.stream().map(this::toEmployeeOption).toList());
        meta.setVoucherTypeOptions(toOptions(VOUCHER_TYPE_SEEDS));
        meta.setCurrencyOptions(toOptions(CURRENCY_SEEDS));
        meta.setAccountOptions(toOptions(ACCOUNT_SEEDS));
        meta.setCustomerOptions(toOptions(CUSTOMER_SEEDS));
        meta.setSupplierOptions(toOptions(SUPPLIER_SEEDS));
        meta.setProjectClassOptions(toOptions(PROJECT_CLASS_SEEDS));
        meta.setProjectOptions(toOptions(PROJECT_SEEDS));
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

    @Override
    public FinanceVoucherDetailVO getDetail(String voucherNo) {
        VoucherKey voucherKey = parseVoucherNo(voucherNo);
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
        detail.setCompanyId(headerRow.getCompanyId());
        detail.setIperiod(headerRow.getIperiod());
        detail.setCsign(headerRow.getCsign());
        detail.setInoId(headerRow.getInoId());
        detail.setDbillDate(formatDate(headerRow.getDbillDate()));
        detail.setIdoc(headerRow.getIdoc());
        detail.setCbill(headerRow.getCbill());
        detail.setCtext1(headerRow.getCtext1());
        detail.setCtext2(headerRow.getCtext2());
        detail.setStatus(resolveStatus(headerRow));

        List<FinanceVoucherEntryVO> entries = rows.stream().map(this::toEntryVO).toList();
        detail.setEntries(entries);
        detail.setTotalDebit(sumAmount(rows, true));
        detail.setTotalCredit(sumAmount(rows, false));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FinanceVoucherSaveResultVO saveVoucher(FinanceVoucherSaveDTO dto, Long currentUserId, String currentUsername) {
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
        validateEntries(normalizedEntries);

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
                FinanceVoucherEntryDTO entry = normalizedEntries.get(index);
                GlAccvouch row = new GlAccvouch();
                row.setCompanyId(companyId);
                row.setIperiod(period);
                row.setCsign(voucherType);
                row.setIsignseq(signSeq);
                row.setInoId(finalVoucherNo);
                row.setInid(index + 1);
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

    private List<SystemCompany> loadEnabledCompanies() {
        return systemCompanyMapper.selectList(
                Wrappers.<SystemCompany>lambdaQuery()
                        .eq(SystemCompany::getStatus, 1)
                        .orderByAsc(SystemCompany::getCompanyCode, SystemCompany::getCompanyId)
        );
    }

    private List<SystemDepartment> loadEnabledDepartments() {
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .eq(SystemDepartment::getStatus, 1)
                        .orderByAsc(SystemDepartment::getSortOrder, SystemDepartment::getId)
        );
    }

    private List<User> loadEnabledUsers() {
        return userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getStatus, 1)
                        .orderByAsc(User::getId)
        );
    }

    private FinanceVoucherOptionVO toCompanyOption(SystemCompany company) {
        String label = trimToNull(company.getCompanyCode()) == null
                ? company.getCompanyName()
                : company.getCompanyCode() + " - " + company.getCompanyName();
        return option(company.getCompanyId(), label);
    }

    private FinanceVoucherOptionVO toDepartmentOption(SystemDepartment department) {
        String label = trimToNull(department.getDeptCode()) == null
                ? department.getDeptName()
                : department.getDeptCode() + " - " + department.getDeptName();
        return option(String.valueOf(department.getId()), label);
    }

    private FinanceVoucherOptionVO toEmployeeOption(User user) {
        String displayName = trimToNull(user.getName()) == null ? normalize(user.getUsername(), "未命名员工") : user.getName();
        String label = trimToNull(user.getUsername()) == null
                ? displayName
                : displayName + " (" + user.getUsername() + ")";
        return option(String.valueOf(user.getId()), label);
    }

    private List<FinanceVoucherOptionVO> toOptions(List<OptionSeed> seeds) {
        return seeds.stream().map(seed -> option(seed.value(), seed.label())).toList();
    }

    private FinanceVoucherOptionVO option(String value, String label) {
        FinanceVoucherOptionVO option = new FinanceVoucherOptionVO();
        option.setValue(value);
        option.setLabel(label);
        return option;
    }

    private User requireUser(Long currentUserId) {
        User user = userMapper.selectById(currentUserId);
        if (user == null) {
            throw new IllegalStateException("当前登录用户不存在");
        }
        return user;
    }

    private String resolveDefaultCompanyId(String companyId, User currentUser, List<SystemCompany> companies) {
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

    private int nextVoucherNo(String companyId, Integer period, String voucherType) {
        if (trimToNull(companyId) == null || period == null || trimToNull(voucherType) == null) {
            return 1;
        }
        List<Object> values = glAccvouchMapper.selectObjs(
                Wrappers.<GlAccvouch>lambdaQuery()
                        .select(GlAccvouch::getInoId)
                        .eq(GlAccvouch::getCompanyId, companyId)
                        .eq(GlAccvouch::getIperiod, period)
                        .eq(GlAccvouch::getCsign, voucherType)
                        .orderByDesc(GlAccvouch::getInoId)
                        .last("limit 1")
        );
        if (values.isEmpty() || values.get(0) == null) {
            return 1;
        }
        return ((Number) values.get(0)).intValue() + 1;
    }

    private VoucherKey parseVoucherNo(String voucherNo) {
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

    private String buildVoucherNo(String companyId, Integer period, String voucherType, Integer inoId) {
        return companyId + VOUCHER_NO_SEPARATOR + period + VOUCHER_NO_SEPARATOR + voucherType + VOUCHER_NO_SEPARATOR + inoId;
    }

    private FinanceVoucherEntryVO toEntryVO(GlAccvouch row) {
        FinanceVoucherEntryVO entry = new FinanceVoucherEntryVO();
        entry.setInid(row.getInid());
        entry.setCdigest(row.getCdigest());
        entry.setCcode(row.getCcode());
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

    private String resolveStatus(GlAccvouch row) {
        if (Objects.equals(row.getIbook(), 1)) {
            return "POSTED";
        }
        if (trimToNull(row.getCcheck()) != null) {
            return "REVIEWED";
        }
        return STATUS_UNPOSTED;
    }

    private BigDecimal sumAmount(List<GlAccvouch> rows, boolean debit) {
        return rows.stream()
                .map(item -> debit ? item.getMd() : item.getMc())
                .map(this::normalizeAmount)
                .reduce(ZERO, BigDecimal::add);
    }

    private BigDecimal sumAmount(List<FinanceVoucherEntryDTO> rows, AmountGetter getter) {
        return rows.stream()
                .map(getter::get)
                .map(this::normalizeAmount)
                .reduce(ZERO, BigDecimal::add);
    }

    private void validateCompany(String companyId) {
        Long count = systemCompanyMapper.selectCount(
                Wrappers.<SystemCompany>lambdaQuery()
                        .eq(SystemCompany::getCompanyId, companyId)
                        .eq(SystemCompany::getStatus, 1)
        );
        if (count == null || count == 0) {
            throw new IllegalArgumentException("公司主体不存在或已停用");
        }
    }

    private void validateVoucherType(String voucherType) {
        if (VOUCHER_TYPE_SEEDS.stream().noneMatch(item -> Objects.equals(item.value(), voucherType))) {
            throw new IllegalArgumentException("凭证类别不合法");
        }
    }

    private List<FinanceVoucherEntryDTO> normalizeEntries(List<FinanceVoucherEntryDTO> entries) {
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

    private boolean isEmptyEntry(FinanceVoucherEntryDTO entry) {
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

    private void validateEntries(List<FinanceVoucherEntryDTO> entries) {
        if (entries.size() < 2) {
            throw new IllegalArgumentException("至少需要两条有效分录");
        }

        Map<String, String> departments = loadEnabledDepartments().stream()
                .collect(Collectors.toMap(item -> String.valueOf(item.getId()), SystemDepartment::getDeptName));
        Map<String, String> employees = loadEnabledUsers().stream()
                .collect(Collectors.toMap(item -> String.valueOf(item.getId()), this::resolveUserName));
        Set<String> accounts = ACCOUNT_SEEDS.stream().map(OptionSeed::value).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> customers = CUSTOMER_SEEDS.stream().map(OptionSeed::value).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> suppliers = SUPPLIER_SEEDS.stream().map(OptionSeed::value).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> projectClasses = PROJECT_CLASS_SEEDS.stream().map(OptionSeed::value).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> projects = PROJECT_SEEDS.stream().map(OptionSeed::value).collect(Collectors.toCollection(LinkedHashSet::new));
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
            validateSelectable(entry.getCpersonId(), employees.keySet(), "职员", rowNo);
            validateSelectable(entry.getCcusId(), customers, "客户", rowNo);
            validateSelectable(entry.getCsupId(), suppliers, "供应商", rowNo);
            validateSelectable(entry.getCitemClass(), projectClasses, "项目大类", rowNo);
            validateSelectable(entry.getCitemId(), projects, "项目", rowNo);

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

    private void validateSelectable(String value, Collection<String> validValues, String fieldName, int rowNo) {
        String normalizedValue = trimToNull(value);
        if (normalizedValue == null) {
            return;
        }
        if (!validValues.contains(normalizedValue)) {
            throw new IllegalArgumentException("第 " + rowNo + " 行" + fieldName + "不存在");
        }
    }

    private String resolveUserName(User user) {
        return trimToNull(user.getName()) == null ? normalize(user.getUsername(), "未命名员工") : user.getName();
    }

    private String resolveMakerName(User currentUser, String currentUsername) {
        if (trimToNull(currentUser.getName()) != null) {
            return currentUser.getName().trim();
        }
        return normalize(currentUsername, "财务制单员");
    }

    private LocalDate parseDateOrDefault(String value, LocalDate defaultValue) {
        if (trimToNull(value) == null) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            return defaultValue;
        }
    }

    private LocalDate parseDateOrThrow(String value) {
        if (trimToNull(value) == null) {
            throw new IllegalArgumentException("制单日期不能为空");
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("制单日期格式不正确");
        }
    }

    private Integer normalizePeriod(Integer period) {
        if (period == null || period < 1 || period > 12) {
            throw new IllegalArgumentException("会计期间必须在 1 到 12 之间");
        }
        return period;
    }

    private int resolveVoucherTypeSequence(String voucherType) {
        for (int index = 0; index < VOUCHER_TYPE_SEEDS.size(); index++) {
            if (Objects.equals(VOUCHER_TYPE_SEEDS.get(index).value(), voucherType)) {
                return index + 1;
            }
        }
        return 1;
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeNullableAmount(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金额不能为负数");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeNullableQuantity(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultDecimal(BigDecimal value, BigDecimal defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    private boolean isNullOrZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    private String normalize(String value, String defaultValue) {
        String normalizedValue = trimToNull(value);
        return normalizedValue == null ? defaultValue : normalizedValue;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String formatDate(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.toLocalDate().format(DATE_FORMATTER);
    }

    // Placeholder for the future posting pipeline.
    private void postVoucher(String companyId, Integer period, String voucherType, Integer inoId) {
        // Intentionally left blank in phase one.
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
