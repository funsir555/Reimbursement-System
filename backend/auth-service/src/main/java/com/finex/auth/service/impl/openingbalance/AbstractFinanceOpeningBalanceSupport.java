package com.finex.auth.service.impl.openingbalance;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.FinanceVoucherOptionVO;
import com.finex.auth.dto.OpeningAssistBalanceLineDTO;
import com.finex.auth.dto.OpeningAssistBalanceLineVO;
import com.finex.auth.dto.OpeningBalanceMetaVO;
import com.finex.auth.dto.OpeningBalanceReconcileResultVO;
import com.finex.auth.dto.OpeningBalanceRowSaveDTO;
import com.finex.auth.dto.OpeningBalanceRowVO;
import com.finex.auth.dto.OpeningBalanceTrialResultVO;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.FinanceCustomer;
import com.finex.auth.entity.FinanceOpeningBalanceState;
import com.finex.auth.entity.FinanceProjectArchive;
import com.finex.auth.entity.FinanceProjectClass;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.GlAccass;
import com.finex.auth.entity.GlAccsum;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinanceCustomerMapper;
import com.finex.auth.mapper.FinanceOpeningBalanceStateMapper;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.GlAccassMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

abstract class AbstractFinanceOpeningBalanceSupport {

    protected static final String STATUS_NOT_OPENED = "NOT_OPENED";
    protected static final String STATUS_OPENING = "OPENING";
    protected static final String STATUS_OPENED = "OPENED";
    protected static final String STATUS_CARRYING = "CARRYING";
    protected static final String STATUS_FAILED = "FAILED";
    protected static final String SOURCE_MANUAL = "MANUAL";
    protected static final String SOURCE_CARRY_FORWARD = "CARRY_FORWARD";
    protected static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    protected static final BigDecimal ZERO_QTY = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

    private final FinanceAccountSubjectMapper financeAccountSubjectMapper;
    private final FinanceCustomerMapper financeCustomerMapper;
    private final FinanceVendorMapper financeVendorMapper;
    private final FinanceProjectClassMapper financeProjectClassMapper;
    private final FinanceProjectArchiveMapper financeProjectArchiveMapper;
    private final SystemCompanyMapper systemCompanyMapper;
    private final SystemDepartmentMapper systemDepartmentMapper;
    private final UserMapper userMapper;
    private final GlAccsumMapper glAccsumMapper;
    private final GlAccassMapper glAccassMapper;
    private final FinanceOpeningBalanceStateMapper financeOpeningBalanceStateMapper;

    protected AbstractFinanceOpeningBalanceSupport(
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            FinanceCustomerMapper financeCustomerMapper,
            FinanceVendorMapper financeVendorMapper,
            FinanceProjectClassMapper financeProjectClassMapper,
            FinanceProjectArchiveMapper financeProjectArchiveMapper,
            SystemCompanyMapper systemCompanyMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper,
            GlAccsumMapper glAccsumMapper,
            GlAccassMapper glAccassMapper,
            FinanceOpeningBalanceStateMapper financeOpeningBalanceStateMapper
    ) {
        this.financeAccountSubjectMapper = financeAccountSubjectMapper;
        this.financeCustomerMapper = financeCustomerMapper;
        this.financeVendorMapper = financeVendorMapper;
        this.financeProjectClassMapper = financeProjectClassMapper;
        this.financeProjectArchiveMapper = financeProjectArchiveMapper;
        this.systemCompanyMapper = systemCompanyMapper;
        this.systemDepartmentMapper = systemDepartmentMapper;
        this.userMapper = userMapper;
        this.glAccsumMapper = glAccsumMapper;
        this.glAccassMapper = glAccassMapper;
        this.financeOpeningBalanceStateMapper = financeOpeningBalanceStateMapper;
    }

    protected OpeningBalanceMetaVO buildMeta(Long currentUserId, String companyId, Integer iyear, Integer iperiod) {
        List<SystemCompany> companies = loadEnabledCompanies();
        String effectiveCompanyId = resolveEffectiveCompanyId(currentUserId, companyId, companies);
        int effectiveYear = normalizeYear(iyear);
        int effectivePeriod = normalizePeriod(iperiod);
        FinanceOpeningBalanceState state = findState(effectiveCompanyId, effectiveYear, effectivePeriod);

        OpeningBalanceMetaVO meta = new OpeningBalanceMetaVO();
        meta.setCompanyOptions(companies.stream().map(this::toCompanyOption).toList());
        meta.setDepartmentOptions(loadEnabledDepartments().stream().map(this::toDepartmentOption).toList());
        meta.setEmployeeOptions(loadEnabledUsers().stream().map(this::toUserOption).toList());
        meta.setCustomerOptions(loadCustomers(effectiveCompanyId).stream().map(this::toCustomerOption).toList());
        meta.setSupplierOptions(loadVendors(effectiveCompanyId).stream().map(this::toVendorOption).toList());
        meta.setProjectClassOptions(loadProjectClasses(effectiveCompanyId).stream().map(this::toProjectClassOption).toList());
        meta.setProjectOptions(loadProjects(effectiveCompanyId).stream().map(this::toProjectOption).toList());
        meta.setDefaultCompanyId(effectiveCompanyId);
        meta.setDefaultYear(effectiveYear);
        meta.setDefaultPeriod(effectivePeriod);
        meta.setDefaultYearPeriod(buildYearPeriod(effectiveYear, effectivePeriod));
        meta.setStatus(state == null ? STATUS_NOT_OPENED : state.getStatus());
        meta.setStatusLabel(resolveStatusLabel(meta.getStatus()));
        meta.setOpened(Objects.equals(meta.getStatus(), STATUS_OPENED));
        return meta;
    }

    protected List<OpeningBalanceRowVO> buildRows(String companyId, Integer iyear, Integer iperiod) {
        String effectiveCompanyId = requireCompanyId(companyId);
        int effectiveYear = normalizeYear(iyear);
        int effectivePeriod = normalizePeriod(iperiod);
        Map<String, GlAccsum> sumRowMap = loadAccsumMap(effectiveCompanyId, effectiveYear, effectivePeriod);
        return loadEnabledSubjects(effectiveCompanyId).stream()
                .map(subject -> toRowVO(subject, sumRowMap.get(subject.getSubjectCode())))
                .toList();
    }

    protected List<OpeningAssistBalanceLineVO> buildAssistLines(String companyId, Integer iyear, Integer iperiod, String subjectCode) {
        FinanceAccountSubject subject = requireSubject(companyId, subjectCode);
        requireLeafSubject(subject);
        requireAssistSubject(subject);
        return loadAssistRows(companyId, normalizeYear(iyear), normalizePeriod(iperiod), subjectCode).stream()
                .map(this::toAssistLineVO)
                .toList();
    }

    protected List<OpeningBalanceRowVO> saveSimpleRows(String companyId, Integer iyear, Integer iperiod, List<OpeningBalanceRowSaveDTO> rows) {
        String effectiveCompanyId = requireCompanyId(companyId);
        int effectiveYear = normalizeYear(iyear);
        int effectivePeriod = normalizePeriod(iperiod);
        requireOpenedState(effectiveCompanyId, effectiveYear, effectivePeriod);
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("请至少提交一条期初余额");
        }
        Map<String, FinanceAccountSubject> subjectMap = loadEnabledSubjects(effectiveCompanyId).stream()
                .collect(Collectors.toMap(FinanceAccountSubject::getSubjectCode, item -> item, (left, right) -> left, LinkedHashMap::new));
        for (OpeningBalanceRowSaveDTO row : rows) {
            FinanceAccountSubject subject = subjectMap.get(trimToNull(row.getSubjectCode()));
            if (subject == null) {
                throw new IllegalArgumentException("科目不存在: " + row.getSubjectCode());
            }
            requireLeafSubject(subject);
            if (hasAssist(subject)) {
                throw new IllegalArgumentException("科目【" + subject.getSubjectCode() + " " + subject.getSubjectName() + "】启用了辅助核算，请改用辅助核算窗口录入");
            }
            saveSubjectOpeningSum(subject, effectiveCompanyId, effectiveYear, effectivePeriod, sanitizeAmount(row.getMb()), sanitizeAmount(row.getMbF()), sanitizeQty(row.getNbS()));
        }
        recalculateAncestorRows(effectiveCompanyId, effectiveYear, effectivePeriod);
        return buildRows(effectiveCompanyId, effectiveYear, effectivePeriod);
    }

    protected List<OpeningAssistBalanceLineVO> saveAssistLines(String subjectCode, String companyId, Integer iyear, Integer iperiod, List<OpeningAssistBalanceLineDTO> lines) {
        String effectiveCompanyId = requireCompanyId(companyId);
        int effectiveYear = normalizeYear(iyear);
        int effectivePeriod = normalizePeriod(iperiod);
        requireOpenedState(effectiveCompanyId, effectiveYear, effectivePeriod);
        FinanceAccountSubject subject = requireSubject(effectiveCompanyId, subjectCode);
        requireLeafSubject(subject);
        requireAssistSubject(subject);
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("启用辅助核算的科目必须录入辅助期初明细");
        }

        glAccassMapper.delete(Wrappers.<GlAccass>lambdaQuery()
                .eq(GlAccass::getCompanyId, effectiveCompanyId)
                .eq(GlAccass::getIyear, effectiveYear)
                .eq(GlAccass::getIperiod, effectivePeriod)
                .eq(GlAccass::getCcode, subject.getSubjectCode()));

        BigDecimal totalMb = ZERO;
        BigDecimal totalMbF = ZERO;
        BigDecimal totalNbS = ZERO_QTY;
        for (OpeningAssistBalanceLineDTO line : lines) {
            validateAssistLine(subject, effectiveCompanyId, line);
            BigDecimal lineMb = sanitizeAmount(line.getMb());
            BigDecimal lineMbF = sanitizeAmount(line.getMbF());
            BigDecimal lineNbS = sanitizeQty(line.getNbS());
            if (isAllZero(lineMb, lineMbF, lineNbS)) {
                continue;
            }
            GlAccass entity = new GlAccass();
            entity.setCompanyId(effectiveCompanyId);
            entity.setIyear(effectiveYear);
            entity.setIperiod(effectivePeriod);
            entity.setIyperiod(buildYearPeriod(effectiveYear, effectivePeriod));
            entity.setCcode(subject.getSubjectCode());
            entity.setCdeptId(trimToNull(line.getCdeptId()));
            entity.setCpersonId(trimToNull(line.getCpersonId()));
            entity.setCcusId(trimToNull(line.getCcusId()));
            entity.setCsupId(trimToNull(line.getCsupId()));
            entity.setCitemClass(trimToNull(line.getCitemClass()));
            entity.setCitemId(trimToNull(line.getCitemId()));
            fillOpeningAmounts(entity, subject, lineMb, lineMbF, lineNbS, null, null, null);
            glAccassMapper.insert(entity);
            totalMb = totalMb.add(lineMb);
            totalMbF = totalMbF.add(lineMbF);
            totalNbS = totalNbS.add(lineNbS);
        }
        saveSubjectOpeningSum(subject, effectiveCompanyId, effectiveYear, effectivePeriod, totalMb, totalMbF, totalNbS);
        recalculateAncestorRows(effectiveCompanyId, effectiveYear, effectivePeriod);
        return buildAssistLines(effectiveCompanyId, effectiveYear, effectivePeriod, subjectCode);
    }

    protected OpeningBalanceTrialResultVO buildTrialResult(String companyId, Integer iyear, Integer iperiod) {
        String effectiveCompanyId = requireCompanyId(companyId);
        int effectiveYear = normalizeYear(iyear);
        int effectivePeriod = normalizePeriod(iperiod);
        requireOpenedState(effectiveCompanyId, effectiveYear, effectivePeriod);

        Map<String, GlAccsum> sumMap = loadAccsumMap(effectiveCompanyId, effectiveYear, effectivePeriod);
        List<OpeningBalanceRowVO> abnormal = new ArrayList<>();
        BigDecimal debit = ZERO;
        BigDecimal credit = ZERO;
        for (FinanceAccountSubject subject : loadEnabledSubjects(effectiveCompanyId)) {
            if (!isLeaf(subject)) {
                continue;
            }
            BigDecimal amount = money(sumMap.get(subject.getSubjectCode()) == null ? null : sumMap.get(subject.getSubjectCode()).getMb());
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                abnormal.add(toRowVO(subject, sumMap.get(subject.getSubjectCode())));
            }
            if (isDebitDirection(subject.getBalanceDirection())) {
                debit = debit.add(amount);
            } else {
                credit = credit.add(amount);
            }
        }

        FinanceOpeningBalanceState state = findState(effectiveCompanyId, effectiveYear, effectivePeriod);
        if (state != null) {
            state.setLastTrialAt(LocalDateTime.now());
            financeOpeningBalanceStateMapper.updateById(state);
        }

        OpeningBalanceTrialResultVO result = new OpeningBalanceTrialResultVO();
        result.setTotalDebit(debit);
        result.setTotalCredit(credit);
        result.setDifference(debit.subtract(credit).setScale(2, RoundingMode.HALF_UP));
        result.setBalanced(result.getDifference().compareTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)) == 0 && abnormal.isEmpty());
        result.setAbnormalSubjects(abnormal);
        return result;
    }

    protected OpeningBalanceReconcileResultVO buildReconcileResult(String companyId, Integer iyear, Integer iperiod) {
        String effectiveCompanyId = requireCompanyId(companyId);
        int effectiveYear = normalizeYear(iyear);
        int effectivePeriod = normalizePeriod(iperiod);
        requireOpenedState(effectiveCompanyId, effectiveYear, effectivePeriod);
        Map<String, GlAccsum> sumMap = loadAccsumMap(effectiveCompanyId, effectiveYear, effectivePeriod);
        List<OpeningBalanceRowVO> differenceSubjects = new ArrayList<>();
        List<OpeningBalanceRowVO> missingAssistSubjects = new ArrayList<>();
        List<String> illegalMessages = new ArrayList<>();
        List<FinanceProjectArchive> projects = loadProjects(effectiveCompanyId);

        for (FinanceAccountSubject subject : loadEnabledSubjects(effectiveCompanyId)) {
            if (!isLeaf(subject) || !hasAssist(subject)) {
                continue;
            }
            List<GlAccass> lines = loadAssistRows(effectiveCompanyId, effectiveYear, effectivePeriod, subject.getSubjectCode());
            BigDecimal assistTotal = lines.stream().map(GlAccass::getMb).filter(Objects::nonNull).reduce(ZERO, BigDecimal::add);
            BigDecimal sumTotal = money(sumMap.get(subject.getSubjectCode()) == null ? null : sumMap.get(subject.getSubjectCode()).getMb());
            if (!lines.isEmpty() && assistTotal.compareTo(sumTotal) != 0) {
                differenceSubjects.add(toRowVO(subject, sumMap.get(subject.getSubjectCode())));
            }
            if (lines.isEmpty() && sumTotal.compareTo(BigDecimal.ZERO) != 0) {
                missingAssistSubjects.add(toRowVO(subject, sumMap.get(subject.getSubjectCode())));
            }
            for (GlAccass line : lines) {
                String issue = validateAssistLineForReconcile(subject, line, projects);
                if (issue != null) {
                    illegalMessages.add(issue);
                }
            }
        }

        FinanceOpeningBalanceState state = findState(effectiveCompanyId, effectiveYear, effectivePeriod);
        if (state != null) {
            state.setLastReconcileAt(LocalDateTime.now());
            financeOpeningBalanceStateMapper.updateById(state);
        }

        OpeningBalanceReconcileResultVO result = new OpeningBalanceReconcileResultVO();
        result.setDifferenceSubjects(differenceSubjects);
        result.setMissingAssistSubjects(missingAssistSubjects);
        result.setIllegalAssistMessages(illegalMessages);
        result.setMatched(differenceSubjects.isEmpty() && missingAssistSubjects.isEmpty() && illegalMessages.isEmpty());
        return result;
    }

    protected void performOpenBook(String companyId, Integer iyear, Integer iperiod, String operatorName) {
        String effectiveCompanyId = requireCompanyId(companyId);
        int effectiveYear = normalizeYear(iyear);
        int effectivePeriod = normalizePeriod(iperiod);
        FinanceOpeningBalanceState existing = findState(effectiveCompanyId, effectiveYear, effectivePeriod);
        if (existing != null && Objects.equals(existing.getStatus(), STATUS_OPENED)) {
            throw new IllegalStateException("当前公司本年度本期间已经开账，不能重复开账");
        }

        FinanceOpeningBalanceState state = existing == null ? new FinanceOpeningBalanceState() : existing;
        state.setCompanyId(effectiveCompanyId);
        state.setIyear(effectiveYear);
        state.setIperiod(effectivePeriod);
        state.setIyperiod(buildYearPeriod(effectiveYear, effectivePeriod));
        state.setStatus(STATUS_OPENING);
        state.setSourceType(SOURCE_MANUAL);
        if (existing == null) {
            financeOpeningBalanceStateMapper.insert(state);
        } else {
            financeOpeningBalanceStateMapper.updateById(state);
        }

        for (FinanceAccountSubject subject : loadEnabledSubjects(effectiveCompanyId)) {
            saveSubjectOpeningSum(subject, effectiveCompanyId, effectiveYear, effectivePeriod, ZERO, ZERO, ZERO_QTY);
        }
        recalculateAncestorRows(effectiveCompanyId, effectiveYear, effectivePeriod);

        state.setStatus(STATUS_OPENED);
        state.setOpenedBy(operatorName);
        state.setOpenedAt(LocalDateTime.now());
        financeOpeningBalanceStateMapper.updateById(state);
    }

    protected void performCarryForward(String companyId, Integer iyear, Integer iperiod, String operatorName) {
        String effectiveCompanyId = requireCompanyId(companyId);
        int effectiveYear = normalizeYear(iyear);
        int effectivePeriod = normalizePeriod(iperiod);
        if (effectivePeriod != 1) {
            throw new IllegalArgumentException("年度期初结转只支持结转到 1 月");
        }
        FinanceOpeningBalanceState existing = findState(effectiveCompanyId, effectiveYear, effectivePeriod);
        if (existing != null && Objects.equals(existing.getStatus(), STATUS_OPENED)) {
            throw new IllegalStateException("目标年度 1 月已经开账，不能重复结转");
        }
        int sourceYear = effectiveYear - 1;
        int sourcePeriod = 12;
        Map<String, FinanceAccountSubject> subjectMap = loadEnabledSubjects(effectiveCompanyId).stream()
                .collect(Collectors.toMap(FinanceAccountSubject::getSubjectCode, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<GlAccsum> sourceSums = glAccsumMapper.selectList(Wrappers.<GlAccsum>lambdaQuery()
                .eq(GlAccsum::getCompanyId, effectiveCompanyId)
                .eq(GlAccsum::getIyear, sourceYear)
                .eq(GlAccsum::getIperiod, sourcePeriod));

        FinanceOpeningBalanceState state = existing == null ? new FinanceOpeningBalanceState() : existing;
        state.setCompanyId(effectiveCompanyId);
        state.setIyear(effectiveYear);
        state.setIperiod(effectivePeriod);
        state.setIyperiod(buildYearPeriod(effectiveYear, effectivePeriod));
        state.setStatus(STATUS_CARRYING);
        state.setSourceType(SOURCE_CARRY_FORWARD);
        if (existing == null) {
            financeOpeningBalanceStateMapper.insert(state);
        } else {
            financeOpeningBalanceStateMapper.updateById(state);
        }

        for (GlAccsum source : sourceSums) {
            FinanceAccountSubject subject = subjectMap.get(source.getCcode());
            if (subject == null) {
                continue;
            }
            saveSubjectOpeningSum(subject, effectiveCompanyId, effectiveYear, effectivePeriod, money(source.getMe()), money(source.getMeF()), qty(source.getNeS()));
        }

        glAccassMapper.delete(Wrappers.<GlAccass>lambdaQuery()
                .eq(GlAccass::getCompanyId, effectiveCompanyId)
                .eq(GlAccass::getIyear, effectiveYear)
                .eq(GlAccass::getIperiod, effectivePeriod));
        List<GlAccass> sourceAssist = glAccassMapper.selectList(Wrappers.<GlAccass>lambdaQuery()
                .eq(GlAccass::getCompanyId, effectiveCompanyId)
                .eq(GlAccass::getIyear, sourceYear)
                .eq(GlAccass::getIperiod, sourcePeriod));
        for (GlAccass source : sourceAssist) {
            FinanceAccountSubject subject = subjectMap.get(source.getCcode());
            if (subject == null) {
                continue;
            }
            GlAccass target = new GlAccass();
            target.setCompanyId(effectiveCompanyId);
            target.setIyear(effectiveYear);
            target.setIperiod(effectivePeriod);
            target.setIyperiod(buildYearPeriod(effectiveYear, effectivePeriod));
            target.setCcode(source.getCcode());
            target.setCdeptId(source.getCdeptId());
            target.setCpersonId(source.getCpersonId());
            target.setCcusId(source.getCcusId());
            target.setCsupId(source.getCsupId());
            target.setCitemClass(source.getCitemClass());
            target.setCitemId(source.getCitemId());
            fillOpeningAmounts(target, subject, money(source.getMe()), money(source.getMeF()), qty(source.getNeS()), null, null, null);
            glAccassMapper.insert(target);
        }
        recalculateAncestorRows(effectiveCompanyId, effectiveYear, effectivePeriod);
        state.setStatus(STATUS_OPENED);
        state.setOpenedBy(operatorName);
        state.setOpenedAt(LocalDateTime.now());
        financeOpeningBalanceStateMapper.updateById(state);
    }

    protected List<SystemCompany> loadEnabledCompanies() {
        return systemCompanyMapper.selectList(Wrappers.<SystemCompany>lambdaQuery()
                .eq(SystemCompany::getStatus, 1)
                .orderByAsc(SystemCompany::getCompanyCode, SystemCompany::getCompanyId));
    }

    protected String resolveEffectiveCompanyId(Long currentUserId, String companyId, List<SystemCompany> companies) {
        String trimmed = trimToNull(companyId);
        if (trimmed != null) {
            requireCompanyId(trimmed);
            return trimmed;
        }
        User currentUser = currentUserId == null ? null : userMapper.selectById(currentUserId);
        if (currentUser != null && trimToNull(currentUser.getCompanyId()) != null) {
            return currentUser.getCompanyId();
        }
        if (companies.isEmpty()) {
            throw new IllegalStateException("当前没有可用公司");
        }
        return companies.get(0).getCompanyId();
    }

    protected String requireCompanyId(String companyId) {
        String effectiveCompanyId = trimToNull(companyId);
        if (effectiveCompanyId == null) {
            throw new IllegalArgumentException("公司不能为空");
        }
        Long count = systemCompanyMapper.selectCount(Wrappers.<SystemCompany>lambdaQuery()
                .eq(SystemCompany::getCompanyId, effectiveCompanyId)
                .eq(SystemCompany::getStatus, 1));
        if (count == null || count == 0) {
            throw new IllegalArgumentException("公司不存在或已停用");
        }
        return effectiveCompanyId;
    }

    protected List<SystemDepartment> loadEnabledDepartments() {
        return systemDepartmentMapper.selectList(Wrappers.<SystemDepartment>lambdaQuery()
                .eq(SystemDepartment::getStatus, 1)
                .orderByAsc(SystemDepartment::getSortOrder, SystemDepartment::getId));
    }

    protected List<User> loadEnabledUsers() {
        return userMapper.selectList(Wrappers.<User>lambdaQuery()
                .eq(User::getStatus, 1)
                .orderByAsc(User::getId));
    }

    protected List<FinanceCustomer> loadCustomers(String companyId) {
        return financeCustomerMapper.selectList(Wrappers.<FinanceCustomer>lambdaQuery()
                .eq(FinanceCustomer::getCompanyId, companyId)
                .orderByAsc(FinanceCustomer::getCCusCode));
    }

    protected List<FinanceVendor> loadVendors(String companyId) {
        return financeVendorMapper.selectList(Wrappers.<FinanceVendor>lambdaQuery()
                .eq(FinanceVendor::getCompanyId, companyId)
                .orderByAsc(FinanceVendor::getCVenCode));
    }

    protected List<FinanceProjectClass> loadProjectClasses(String companyId) {
        return financeProjectClassMapper.selectList(Wrappers.<FinanceProjectClass>lambdaQuery()
                .eq(FinanceProjectClass::getCompanyId, companyId)
                .eq(FinanceProjectClass::getStatus, 1)
                .orderByAsc(FinanceProjectClass::getSortOrder, FinanceProjectClass::getProjectClassCode));
    }

    protected List<FinanceProjectArchive> loadProjects(String companyId) {
        return financeProjectArchiveMapper.selectList(Wrappers.<FinanceProjectArchive>lambdaQuery()
                .eq(FinanceProjectArchive::getCompanyId, companyId)
                .eq(FinanceProjectArchive::getStatus, 1)
                .ne(FinanceProjectArchive::getBclose, 1)
                .orderByAsc(FinanceProjectArchive::getSortOrder, FinanceProjectArchive::getCitemcode));
    }

    protected List<FinanceAccountSubject> loadEnabledSubjects(String companyId) {
        return financeAccountSubjectMapper.selectList(Wrappers.<FinanceAccountSubject>lambdaQuery()
                .eq(FinanceAccountSubject::getCompanyId, companyId)
                .eq(FinanceAccountSubject::getStatus, 1)
                .ne(FinanceAccountSubject::getBclose, 1)
                .orderByAsc(FinanceAccountSubject::getSubjectLevel, FinanceAccountSubject::getSortOrder, FinanceAccountSubject::getSubjectCode));
    }

    protected FinanceAccountSubject requireSubject(String companyId, String subjectCode) {
        String effectiveCode = trimToNull(subjectCode);
        if (effectiveCode == null) {
            throw new IllegalArgumentException("科目不能为空");
        }
        FinanceAccountSubject subject = financeAccountSubjectMapper.selectOne(Wrappers.<FinanceAccountSubject>lambdaQuery()
                .eq(FinanceAccountSubject::getCompanyId, requireCompanyId(companyId))
                .eq(FinanceAccountSubject::getSubjectCode, effectiveCode)
                .eq(FinanceAccountSubject::getStatus, 1));
        if (subject == null) {
            throw new IllegalArgumentException("科目不存在: " + effectiveCode);
        }
        return subject;
    }

    protected void requireLeafSubject(FinanceAccountSubject subject) {
        if (!isLeaf(subject)) {
            throw new IllegalArgumentException("只有末级科目允许录入期初余额");
        }
    }

    protected void requireAssistSubject(FinanceAccountSubject subject) {
        if (!hasAssist(subject)) {
            throw new IllegalArgumentException("当前科目未启用辅助核算");
        }
    }

    protected boolean isLeaf(FinanceAccountSubject subject) {
        return subject != null && Objects.equals(subject.getLeafFlag(), 1);
    }

    protected boolean hasAssist(FinanceAccountSubject subject) {
        return flagOn(subject.getBdept()) || flagOn(subject.getBperson()) || flagOn(subject.getBcus()) || flagOn(subject.getBsup()) || flagOn(subject.getBitem());
    }

    protected boolean flagOn(Integer value) {
        return value != null && value == 1;
    }

    protected int normalizeYear(Integer iyear) {
        int value = iyear == null ? LocalDate.now().getYear() : iyear;
        if (value < 2000 || value > 2099) {
            throw new IllegalArgumentException("年度不合法");
        }
        return value;
    }

    protected int normalizePeriod(Integer iperiod) {
        int value = iperiod == null ? 1 : iperiod;
        if (value < 1 || value > 12) {
            throw new IllegalArgumentException("期间不合法");
        }
        return value;
    }

    protected int buildYearPeriod(int iyear, int iperiod) {
        return iyear * 100 + iperiod;
    }

    protected FinanceOpeningBalanceState findState(String companyId, int iyear, int iperiod) {
        return financeOpeningBalanceStateMapper.selectOne(Wrappers.<FinanceOpeningBalanceState>lambdaQuery()
                .eq(FinanceOpeningBalanceState::getCompanyId, companyId)
                .eq(FinanceOpeningBalanceState::getIyear, iyear)
                .eq(FinanceOpeningBalanceState::getIperiod, iperiod)
                .last("limit 1"));
    }

    protected void requireOpenedState(String companyId, int iyear, int iperiod) {
        FinanceOpeningBalanceState state = findState(companyId, iyear, iperiod);
        if (state == null || !Objects.equals(state.getStatus(), STATUS_OPENED)) {
            throw new IllegalStateException("当前公司本年度本期间尚未开账，不能录入期初余额");
        }
    }

    protected Map<String, GlAccsum> loadAccsumMap(String companyId, int iyear, int iperiod) {
        return glAccsumMapper.selectList(Wrappers.<GlAccsum>lambdaQuery()
                        .eq(GlAccsum::getCompanyId, companyId)
                        .eq(GlAccsum::getIyear, iyear)
                        .eq(GlAccsum::getIperiod, iperiod))
                .stream()
                .collect(Collectors.toMap(GlAccsum::getCcode, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    protected List<GlAccass> loadAssistRows(String companyId, int iyear, int iperiod, String subjectCode) {
        return glAccassMapper.selectList(Wrappers.<GlAccass>lambdaQuery()
                .eq(GlAccass::getCompanyId, companyId)
                .eq(GlAccass::getIyear, iyear)
                .eq(GlAccass::getIperiod, iperiod)
                .eq(GlAccass::getCcode, subjectCode)
                .orderByAsc(GlAccass::getId));
    }

    protected void saveSubjectOpeningSum(FinanceAccountSubject subject, String companyId, int iyear, int iperiod, BigDecimal mb, BigDecimal mbF, BigDecimal nbS) {
        GlAccsum current = glAccsumMapper.selectOne(Wrappers.<GlAccsum>lambdaQuery()
                .eq(GlAccsum::getCompanyId, companyId)
                .eq(GlAccsum::getIyear, iyear)
                .eq(GlAccsum::getIperiod, iperiod)
                .eq(GlAccsum::getCcode, subject.getSubjectCode())
                .last("limit 1"));
        if (current == null) {
            current = new GlAccsum();
            current.setCompanyId(companyId);
            current.setIyear(iyear);
            current.setIperiod(iperiod);
            current.setIyperiod(buildYearPeriod(iyear, iperiod));
            current.setCcode(subject.getSubjectCode());
            fillOpeningAmounts(current, subject, mb, mbF, nbS, null, null, null);
            glAccsumMapper.insert(current);
            return;
        }
        fillOpeningAmounts(current, subject, mb, mbF, nbS, current.getMd(), current.getMc(), current.getNdS());
        glAccsumMapper.updateById(current);
    }

    protected void recalculateAncestorRows(String companyId, int iyear, int iperiod) {
        List<FinanceAccountSubject> subjects = loadEnabledSubjects(companyId);
        Map<String, FinanceAccountSubject> subjectMap = subjects.stream()
                .collect(Collectors.toMap(FinanceAccountSubject::getSubjectCode, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<String, List<FinanceAccountSubject>> childrenMap = new HashMap<>();
        for (FinanceAccountSubject subject : subjects) {
            String parent = trimToNull(subject.getParentSubjectCode());
            if (parent != null) {
                childrenMap.computeIfAbsent(parent, key -> new ArrayList<>()).add(subject);
            }
        }
        Map<String, GlAccsum> sumMap = loadAccsumMap(companyId, iyear, iperiod);
        List<FinanceAccountSubject> ordered = new ArrayList<>(subjects);
        ordered.sort(Comparator.comparing(FinanceAccountSubject::getSubjectLevel, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(FinanceAccountSubject::getSubjectCode));
        for (FinanceAccountSubject subject : ordered) {
            if (isLeaf(subject)) {
                continue;
            }
            List<FinanceAccountSubject> children = childrenMap.getOrDefault(subject.getSubjectCode(), List.of());
            BigDecimal totalMb = ZERO;
            BigDecimal totalMbF = ZERO;
            BigDecimal totalNbS = ZERO_QTY;
            for (FinanceAccountSubject child : children) {
                GlAccsum childRow = sumMap.get(child.getSubjectCode());
                totalMb = totalMb.add(money(childRow == null ? null : childRow.getMb()));
                totalMbF = totalMbF.add(money(childRow == null ? null : childRow.getMbF()));
                totalNbS = totalNbS.add(qty(childRow == null ? null : childRow.getNbS()));
            }
            saveSubjectOpeningSum(subject, companyId, iyear, iperiod, totalMb, totalMbF, totalNbS);
            sumMap.put(subject.getSubjectCode(), glAccsumMapper.selectOne(Wrappers.<GlAccsum>lambdaQuery()
                    .eq(GlAccsum::getCompanyId, companyId)
                    .eq(GlAccsum::getIyear, iyear)
                    .eq(GlAccsum::getIperiod, iperiod)
                    .eq(GlAccsum::getCcode, subject.getSubjectCode())
                    .last("limit 1")));
        }
    }

    protected void validateAssistLine(FinanceAccountSubject subject, String companyId, OpeningAssistBalanceLineDTO line) {
        if (line == null) {
            throw new IllegalArgumentException("辅助核算明细不能为空");
        }
        validateAssistDimension(flagOn(subject.getBdept()), trimToNull(line.getCdeptId()), "部门");
        validateAssistDimension(flagOn(subject.getBperson()), trimToNull(line.getCpersonId()), "人员");
        validateAssistDimension(flagOn(subject.getBcus()), trimToNull(line.getCcusId()), "客户");
        validateAssistDimension(flagOn(subject.getBsup()), trimToNull(line.getCsupId()), "供应商");
        if (flagOn(subject.getBitem())) {
            String projectClass = trimToNull(line.getCitemClass());
            String projectId = trimToNull(line.getCitemId());
            String subjectCassItem = trimToNull(subject.getCassItem());
            if (subjectCassItem != null && projectClass != null && !Objects.equals(subjectCassItem, projectClass)) {
                throw new IllegalArgumentException("项目分类必须为科目挂载的项目分类【" + subjectCassItem + "】");
            }
            if (subjectCassItem != null && projectClass == null) {
                projectClass = subjectCassItem;
                line.setCitemClass(subjectCassItem);
            }
            String resolvedProjectClass = projectClass;
            if (resolvedProjectClass != null
                    && loadProjectClasses(companyId).stream().noneMatch(item -> Objects.equals(item.getProjectClassCode(), resolvedProjectClass))) {
                throw new IllegalArgumentException("项目分类不存在: " + resolvedProjectClass);
            }
            if (projectId != null) {
                FinanceProjectArchive project = loadProjects(companyId).stream()
                        .filter(item -> Objects.equals(item.getCitemcode(), projectId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("项目不存在: " + projectId));
                if (resolvedProjectClass == null) {
                    throw new IllegalArgumentException("录入项目时必须指定项目分类");
                }
                if (!Objects.equals(resolvedProjectClass, trimToNull(project.getCitemccode()))) {
                    throw new IllegalArgumentException("项目【" + projectId + "】不属于项目分类【" + resolvedProjectClass + "】");
                }
            }
        } else if (trimToNull(line.getCitemClass()) != null || trimToNull(line.getCitemId()) != null) {
            throw new IllegalArgumentException("当前科目未启用项目辅助核算");
        }
    }

    protected String validateAssistLineForReconcile(FinanceAccountSubject subject, GlAccass line, List<FinanceProjectArchive> projects) {
        if (flagOn(subject.getBitem())) {
            String subjectCassItem = trimToNull(subject.getCassItem());
            if (subjectCassItem != null && trimToNull(line.getCitemClass()) != null && !Objects.equals(subjectCassItem, trimToNull(line.getCitemClass()))) {
                return "科目【" + subject.getSubjectCode() + " " + subject.getSubjectName() + "】的辅助明细项目分类与挂载分类不一致";
            }
            if (trimToNull(line.getCitemId()) != null) {
                FinanceProjectArchive project = projects.stream()
                        .filter(item -> Objects.equals(item.getCitemcode(), line.getCitemId()))
                        .findFirst()
                        .orElse(null);
                if (project == null) {
                    return "科目【" + subject.getSubjectCode() + " " + subject.getSubjectName() + "】存在失效项目【" + line.getCitemId() + "】";
                }
                if (!Objects.equals(trimToNull(line.getCitemClass()), trimToNull(project.getCitemccode()))) {
                    return "科目【" + subject.getSubjectCode() + " " + subject.getSubjectName() + "】存在项目分类与项目错配";
                }
            }
        }
        return null;
    }

    protected OpeningBalanceRowVO toRowVO(FinanceAccountSubject subject, GlAccsum row) {
        OpeningBalanceRowVO vo = new OpeningBalanceRowVO();
        vo.setSubjectCode(subject.getSubjectCode());
        vo.setSubjectName(subject.getSubjectName());
        vo.setSubjectLevel(subject.getSubjectLevel());
        vo.setLeafFlag(subject.getLeafFlag());
        vo.setEditable(isLeaf(subject));
        vo.setAssistRequired(hasAssist(subject));
        vo.setBalanceDirection(subject.getBalanceDirection());
        vo.setBalanceDirectionLabel(isDebitDirection(subject.getBalanceDirection()) ? "借" : "贷");
        vo.setCexchName(trimToNull(subject.getCexchName()) == null ? "人民币" : subject.getCexchName());
        vo.setCurrencyCode(resolveCurrencyCode(subject.getCexchName()));
        vo.setBperson(subject.getBperson());
        vo.setBcus(subject.getBcus());
        vo.setBsup(subject.getBsup());
        vo.setBdept(subject.getBdept());
        vo.setBitem(subject.getBitem());
        vo.setCassItem(subject.getCassItem());
        vo.setMb(row == null ? ZERO : money(row.getMb()));
        return vo;
    }

    protected OpeningAssistBalanceLineVO toAssistLineVO(GlAccass entity) {
        OpeningAssistBalanceLineVO vo = new OpeningAssistBalanceLineVO();
        vo.setCdeptId(entity.getCdeptId());
        vo.setCpersonId(entity.getCpersonId());
        vo.setCcusId(entity.getCcusId());
        vo.setCsupId(entity.getCsupId());
        vo.setCitemClass(entity.getCitemClass());
        vo.setCitemId(entity.getCitemId());
        vo.setMb(money(entity.getMb()));
        vo.setMbF(money(entity.getMbF()));
        vo.setNbS(qty(entity.getNbS()));
        return vo;
    }

    protected FinanceVoucherOptionVO toCompanyOption(SystemCompany company) {
        FinanceVoucherOptionVO option = new FinanceVoucherOptionVO();
        option.setValue(company.getCompanyId());
        option.setCode(company.getCompanyCode());
        option.setName(company.getCompanyName());
        option.setLabel(company.getCompanyName());
        return option;
    }

    protected FinanceVoucherOptionVO toDepartmentOption(SystemDepartment department) {
        FinanceVoucherOptionVO option = new FinanceVoucherOptionVO();
        option.setValue(String.valueOf(department.getId()));
        option.setCode(department.getDeptCode());
        option.setName(department.getDeptName());
        option.setLabel(department.getDeptName());
        return option;
    }

    protected FinanceVoucherOptionVO toUserOption(User user) {
        FinanceVoucherOptionVO option = new FinanceVoucherOptionVO();
        option.setValue(String.valueOf(user.getId()));
        option.setCode(user.getUsername());
        option.setName(trimToNull(user.getName()) == null ? user.getUsername() : user.getName());
        option.setLabel(option.getName());
        return option;
    }

    protected FinanceVoucherOptionVO toCustomerOption(FinanceCustomer customer) {
        FinanceVoucherOptionVO option = new FinanceVoucherOptionVO();
        option.setValue(customer.getCCusCode());
        option.setCode(customer.getCCusCode());
        option.setName(customer.getCCusName());
        option.setLabel(customer.getCCusName());
        return option;
    }

    protected FinanceVoucherOptionVO toVendorOption(FinanceVendor vendor) {
        FinanceVoucherOptionVO option = new FinanceVoucherOptionVO();
        option.setValue(vendor.getCVenCode());
        option.setCode(vendor.getCVenCode());
        option.setName(vendor.getCVenName());
        option.setLabel(vendor.getCVenName());
        return option;
    }

    protected FinanceVoucherOptionVO toProjectClassOption(FinanceProjectClass projectClass) {
        FinanceVoucherOptionVO option = new FinanceVoucherOptionVO();
        option.setValue(projectClass.getProjectClassCode());
        option.setCode(projectClass.getProjectClassCode());
        option.setName(projectClass.getProjectClassName());
        option.setLabel(projectClass.getProjectClassName());
        return option;
    }

    protected FinanceVoucherOptionVO toProjectOption(FinanceProjectArchive project) {
        FinanceVoucherOptionVO option = new FinanceVoucherOptionVO();
        option.setValue(project.getCitemcode());
        option.setCode(project.getCitemcode());
        option.setName(project.getCitemname());
        option.setParentValue(project.getCitemccode());
        option.setLabel(project.getCitemname());
        return option;
    }

    protected void fillOpeningAmounts(GlAccsum row, FinanceAccountSubject subject, BigDecimal mb, BigDecimal mbF, BigDecimal nbS, BigDecimal md, BigDecimal mc, BigDecimal ndS) {
        row.setCbegindC(isDebitDirection(subject.getBalanceDirection()) ? "借" : "贷");
        row.setCbegindCEngl(isDebitDirection(subject.getBalanceDirection()) ? "DEBIT" : "CREDIT");
        row.setCenddC(row.getCbegindC());
        row.setCenddCEngl(row.getCbegindCEngl());
        row.setCexchName(trimToNull(subject.getCexchName()) == null ? "人民币" : subject.getCexchName());
        row.setCurrencyCode(resolveCurrencyCode(subject.getCexchName()));
        row.setMb(money(mb));
        row.setMbF(money(mbF));
        row.setNbS(qty(nbS));
        row.setMd(money(md));
        row.setMc(money(mc));
        row.setNdS(qty(ndS));
        row.setMdF(ZERO);
        row.setMcF(ZERO);
        row.setNcS(ZERO_QTY);
        row.setMe(row.getMb().add(row.getMd()).subtract(row.getMc()));
        row.setMeF(row.getMbF().add(money(row.getMdF())).subtract(money(row.getMcF())));
        row.setNeS(row.getNbS().add(row.getNdS()).subtract(qty(row.getNcS())));
    }

    protected void fillOpeningAmounts(GlAccass row, FinanceAccountSubject subject, BigDecimal mb, BigDecimal mbF, BigDecimal nbS, BigDecimal md, BigDecimal mc, BigDecimal ndS) {
        row.setCbegindC(isDebitDirection(subject.getBalanceDirection()) ? "借" : "贷");
        row.setCbegindCEngl(isDebitDirection(subject.getBalanceDirection()) ? "DEBIT" : "CREDIT");
        row.setCenddC(row.getCbegindC());
        row.setCenddCEngl(row.getCbegindCEngl());
        row.setCexchName(trimToNull(subject.getCexchName()) == null ? "人民币" : subject.getCexchName());
        row.setCurrencyCode(resolveCurrencyCode(subject.getCexchName()));
        row.setMb(money(mb));
        row.setMbF(money(mbF));
        row.setNbS(qty(nbS));
        row.setMd(money(md));
        row.setMc(money(mc));
        row.setNdS(qty(ndS));
        row.setMdF(ZERO);
        row.setMcF(ZERO);
        row.setNcS(ZERO_QTY);
        row.setMe(row.getMb().add(row.getMd()).subtract(row.getMc()));
        row.setMeF(row.getMbF().add(money(row.getMdF())).subtract(money(row.getMcF())));
        row.setNeS(row.getNbS().add(row.getNdS()).subtract(qty(row.getNcS())));
    }

    protected boolean isDebitDirection(String balanceDirection) {
        String normalized = trimToNull(balanceDirection);
        if (normalized == null) {
            return true;
        }
        return normalized.toUpperCase(Locale.ROOT).contains("DEBIT")
                || normalized.contains("借");
    }

    protected String resolveStatusLabel(String status) {
        if (Objects.equals(status, STATUS_OPENED)) {
            return "已开账";
        }
        if (Objects.equals(status, STATUS_OPENING)) {
            return "开账中";
        }
        if (Objects.equals(status, STATUS_CARRYING)) {
            return "结转中";
        }
        if (Objects.equals(status, STATUS_FAILED)) {
            return "失败";
        }
        return "未开账";
    }

    protected String resolveCurrencyCode(String cexchName) {
        String normalized = trimToNull(cexchName);
        if (normalized == null || normalized.contains("人民币")) {
            return "CNY";
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    protected BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    protected BigDecimal qty(BigDecimal value) {
        return value == null ? ZERO_QTY : value.setScale(6, RoundingMode.HALF_UP);
    }

    protected BigDecimal sanitizeAmount(BigDecimal value) {
        BigDecimal normalized = money(value);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("期初余额不能为负数");
        }
        return normalized;
    }

    protected BigDecimal sanitizeQty(BigDecimal value) {
        BigDecimal normalized = qty(value);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("期初数量不能为负数");
        }
        return normalized;
    }

    protected boolean isAllZero(BigDecimal mb, BigDecimal mbF, BigDecimal nbS) {
        return money(mb).compareTo(BigDecimal.ZERO) == 0
                && money(mbF).compareTo(BigDecimal.ZERO) == 0
                && qty(nbS).compareTo(BigDecimal.ZERO) == 0;
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateAssistDimension(boolean enabled, String value, String label) {
        if (enabled) {
            return;
        }
        if (value != null) {
            throw new IllegalArgumentException("当前科目未启用" + label + "辅助核算");
        }
    }
}
