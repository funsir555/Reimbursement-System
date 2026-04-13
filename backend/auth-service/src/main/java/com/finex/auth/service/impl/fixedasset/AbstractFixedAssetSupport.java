package com.finex.auth.service.impl.fixedasset;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.FixedAssetCardSaveDTO;
import com.finex.auth.dto.FixedAssetCardVO;
import com.finex.auth.dto.FixedAssetCategorySaveDTO;
import com.finex.auth.dto.FixedAssetCategoryVO;
import com.finex.auth.dto.FixedAssetChangeBillSaveDTO;
import com.finex.auth.dto.FixedAssetChangeBillVO;
import com.finex.auth.dto.FixedAssetChangeLineDTO;
import com.finex.auth.dto.FixedAssetChangeLineVO;
import com.finex.auth.dto.FixedAssetDeprLineVO;
import com.finex.auth.dto.FixedAssetDeprPreviewDTO;
import com.finex.auth.dto.FixedAssetDeprRunVO;
import com.finex.auth.dto.FixedAssetDeprWorkloadDTO;
import com.finex.auth.dto.FixedAssetDisposalBillSaveDTO;
import com.finex.auth.dto.FixedAssetDisposalBillVO;
import com.finex.auth.dto.FixedAssetDisposalLineDTO;
import com.finex.auth.dto.FixedAssetDisposalLineVO;
import com.finex.auth.dto.FixedAssetMetaVO;
import com.finex.auth.dto.FixedAssetOpeningImportDTO;
import com.finex.auth.dto.FixedAssetOpeningImportLineVO;
import com.finex.auth.dto.FixedAssetOpeningImportResultVO;
import com.finex.auth.dto.FixedAssetOpeningImportRowDTO;
import com.finex.auth.dto.FixedAssetOptionVO;
import com.finex.auth.dto.FixedAssetPeriodCloseDTO;
import com.finex.auth.dto.FixedAssetPeriodStatusVO;
import com.finex.auth.dto.FixedAssetTemplateVO;
import com.finex.auth.dto.FixedAssetVoucherLinkVO;
import com.finex.auth.entity.FaAssetAccountPolicy;
import com.finex.auth.entity.FaAssetCard;
import com.finex.auth.entity.FaAssetCategory;
import com.finex.auth.entity.FaAssetChangeBill;
import com.finex.auth.entity.FaAssetChangeLine;
import com.finex.auth.entity.FaAssetDeprLine;
import com.finex.auth.entity.FaAssetDeprRun;
import com.finex.auth.entity.FaAssetDisposalBill;
import com.finex.auth.entity.FaAssetDisposalLine;
import com.finex.auth.entity.FaAssetOpeningImport;
import com.finex.auth.entity.FaAssetOpeningImportLine;
import com.finex.auth.entity.FaAssetPeriodClose;
import com.finex.auth.entity.FaAssetVoucherLink;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.FaAssetAccountPolicyMapper;
import com.finex.auth.mapper.FaAssetCardMapper;
import com.finex.auth.mapper.FaAssetCategoryMapper;
import com.finex.auth.mapper.FaAssetChangeBillMapper;
import com.finex.auth.mapper.FaAssetChangeLineMapper;
import com.finex.auth.mapper.FaAssetDeprLineMapper;
import com.finex.auth.mapper.FaAssetDeprRunMapper;
import com.finex.auth.mapper.FaAssetDisposalBillMapper;
import com.finex.auth.mapper.FaAssetDisposalLineMapper;
import com.finex.auth.mapper.FaAssetOpeningImportLineMapper;
import com.finex.auth.mapper.FaAssetOpeningImportMapper;
import com.finex.auth.mapper.FaAssetPeriodCloseMapper;
import com.finex.auth.mapper.FaAssetVoucherLinkMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractFixedAssetSupport {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String BOOK_CODE_FINANCE = "FINANCE";
    private static final String SHARE_SCOPE_COMPANY = "COMPANY";
    private static final String SHARE_SCOPE_GROUP = "GROUP";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_POSTED = "POSTED";
    private static final String STATUS_VOID = "VOID";
    private static final String CARD_STATUS_IN_USE = "IN_USE";
    private static final String CARD_STATUS_IDLE = "IDLE";
    private static final String CARD_STATUS_DISPOSED = "DISPOSED";
    private static final String CARD_STATUS_DRAFT = "DRAFT";
    private static final String METHOD_STRAIGHT_LINE = "STRAIGHT_LINE";
    private static final String METHOD_WORKLOAD = "WORKLOAD";
    private static final String METHOD_DOUBLE_DECLINING = "DOUBLE_DECLINING";
    private static final String CHANGE_ADD = "ADD";
    private static final String CHANGE_TRANSFER_DEPT = "TRANSFER_DEPT";
    private static final String CHANGE_TRANSFER_KEEPER = "TRANSFER_KEEPER";
    private static final String CHANGE_VALUE_ADJUST = "VALUE_ADJUST";
    private static final String CHANGE_RESIDUAL_ADJUST = "RESIDUAL_ADJUST";
    private static final String CHANGE_LIFE_ADJUST = "LIFE_ADJUST";
    private static final String BUSINESS_CHANGE_BILL = "CHANGE_BILL";
    private static final String BUSINESS_DEPRECIATION_RUN = "DEPRECIATION_RUN";
    private static final String BUSINESS_DISPOSAL_BILL = "DISPOSAL_BILL";
    private static final String SOURCE_MANUAL = "MANUAL";
    private static final String SOURCE_OPENING = "OPENING";
    private static final String SOURCE_CHANGE_ADD = "CHANGE_ADD";
    private static final String VOUCHER_TYPE = "杞?";
    private static final int VOUCHER_SIGN_SEQUENCE = 4;
    private static final int FIXED_ASSET_CODE_MAX_LENGTH = 32;
    private static final int FIXED_ASSET_NAME_MAX_LENGTH = 64;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal ZERO_QTY = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

    private final FaAssetCategoryMapper faAssetCategoryMapper;
    private final FaAssetAccountPolicyMapper faAssetAccountPolicyMapper;
    private final FaAssetCardMapper faAssetCardMapper;
    private final FaAssetChangeBillMapper faAssetChangeBillMapper;
    private final FaAssetChangeLineMapper faAssetChangeLineMapper;
    private final FaAssetDeprRunMapper faAssetDeprRunMapper;
    private final FaAssetDeprLineMapper faAssetDeprLineMapper;
    private final FaAssetDisposalBillMapper faAssetDisposalBillMapper;
    private final FaAssetDisposalLineMapper faAssetDisposalLineMapper;
    private final FaAssetOpeningImportMapper faAssetOpeningImportMapper;
    private final FaAssetOpeningImportLineMapper faAssetOpeningImportLineMapper;
    private final FaAssetVoucherLinkMapper faAssetVoucherLinkMapper;
    private final FaAssetPeriodCloseMapper faAssetPeriodCloseMapper;
    private final GlAccvouchMapper glAccvouchMapper;
    private final SystemCompanyMapper systemCompanyMapper;
    private final SystemDepartmentMapper systemDepartmentMapper;
    private final UserMapper userMapper;

    private final ConcurrentHashMap<String, Object> voucherNoLocks = new ConcurrentHashMap<>();

    protected AbstractFixedAssetSupport(
            FaAssetCategoryMapper faAssetCategoryMapper,
            FaAssetAccountPolicyMapper faAssetAccountPolicyMapper,
            FaAssetCardMapper faAssetCardMapper,
            FaAssetChangeBillMapper faAssetChangeBillMapper,
            FaAssetChangeLineMapper faAssetChangeLineMapper,
            FaAssetDeprRunMapper faAssetDeprRunMapper,
            FaAssetDeprLineMapper faAssetDeprLineMapper,
            FaAssetDisposalBillMapper faAssetDisposalBillMapper,
            FaAssetDisposalLineMapper faAssetDisposalLineMapper,
            FaAssetOpeningImportMapper faAssetOpeningImportMapper,
            FaAssetOpeningImportLineMapper faAssetOpeningImportLineMapper,
            FaAssetVoucherLinkMapper faAssetVoucherLinkMapper,
            FaAssetPeriodCloseMapper faAssetPeriodCloseMapper,
            GlAccvouchMapper glAccvouchMapper,
            SystemCompanyMapper systemCompanyMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper
    ) {
        this.faAssetCategoryMapper = faAssetCategoryMapper;
        this.faAssetAccountPolicyMapper = faAssetAccountPolicyMapper;
        this.faAssetCardMapper = faAssetCardMapper;
        this.faAssetChangeBillMapper = faAssetChangeBillMapper;
        this.faAssetChangeLineMapper = faAssetChangeLineMapper;
        this.faAssetDeprRunMapper = faAssetDeprRunMapper;
        this.faAssetDeprLineMapper = faAssetDeprLineMapper;
        this.faAssetDisposalBillMapper = faAssetDisposalBillMapper;
        this.faAssetDisposalLineMapper = faAssetDisposalLineMapper;
        this.faAssetOpeningImportMapper = faAssetOpeningImportMapper;
        this.faAssetOpeningImportLineMapper = faAssetOpeningImportLineMapper;
        this.faAssetVoucherLinkMapper = faAssetVoucherLinkMapper;
        this.faAssetPeriodCloseMapper = faAssetPeriodCloseMapper;
        this.glAccvouchMapper = glAccvouchMapper;
        this.systemCompanyMapper = systemCompanyMapper;
        this.systemDepartmentMapper = systemDepartmentMapper;
        this.userMapper = userMapper;
    }
    public FixedAssetMetaVO getMeta(Long currentUserId, String currentUsername, String companyId, Integer fiscalYear, Integer fiscalPeriod) {
        User currentUser = currentUserId == null ? null : userMapper.selectById(currentUserId);
        List<SystemCompany> companies = loadEnabledCompanies();
        String effectiveCompanyId = resolveDefaultCompanyId(companyId, currentUser, companies);
        int effectiveYear = fiscalYear == null ? LocalDate.now().getYear() : fiscalYear;
        int effectivePeriod = normalizePeriod(fiscalPeriod == null ? LocalDate.now().getMonthValue() : fiscalPeriod);
        String bookCode = BOOK_CODE_FINANCE;

        FixedAssetMetaVO meta = new FixedAssetMetaVO();
        meta.setCompanyOptions(companies.stream().map(this::toCompanyOption).toList());
        meta.setDepartmentOptions(loadEnabledDepartments(effectiveCompanyId).stream().map(this::toDepartmentOption).toList());
        meta.setEmployeeOptions(loadEnabledUsers(effectiveCompanyId).stream().map(this::toUserOption).toList());
        meta.setCategoryOptions(listAccessibleCategories(effectiveCompanyId).stream().map(this::toCategoryOption).toList());
        meta.setDepreciationMethodOptions(List.of(
                option(METHOD_STRAIGHT_LINE, "骞冲潎骞撮檺娉?"),
                option(METHOD_WORKLOAD, "宸ヤ綔閲忔硶"),
                option(METHOD_DOUBLE_DECLINING, "鍙屽€嶄綑棰濋€掑噺娉?")
        ));
        meta.setCardStatusOptions(List.of(
                option(CARD_STATUS_DRAFT, "鑽夌"),
                option(CARD_STATUS_IN_USE, "鍦ㄧ敤"),
                option(CARD_STATUS_IDLE, "闂茬疆"),
                option(CARD_STATUS_DISPOSED, "宸插缃?")
        ));
        meta.setChangeTypeOptions(List.of(
                option(CHANGE_ADD, "璧勪骇澧炲姞"),
                option(CHANGE_TRANSFER_DEPT, "閮ㄩ棬璋冩暣"),
                option(CHANGE_TRANSFER_KEEPER, "淇濈浜鸿皟鏁?"),
                option(CHANGE_VALUE_ADJUST, "鍘熷€艰皟鏁?"),
                option(CHANGE_RESIDUAL_ADJUST, "娈嬪€艰皟鏁?"),
                option(CHANGE_LIFE_ADJUST, "浣跨敤骞撮檺璋冩暣")
        ));
        meta.setBookOptions(List.of(option(BOOK_CODE_FINANCE, "璐㈠姟璐?")));
        meta.setDefaultCompanyId(effectiveCompanyId);
        meta.setDefaultBookCode(bookCode);
        meta.setDefaultFiscalYear(effectiveYear);
        meta.setDefaultFiscalPeriod(effectivePeriod);
        meta.setPeriodStatus(getPeriodStatus(effectiveCompanyId, bookCode, effectiveYear, effectivePeriod).getStatus());
        meta.setCardCount(Math.toIntExact(countCards(effectiveCompanyId, bookCode)));
        meta.setPendingDepreciationCount(previewDepreciationCount(effectiveCompanyId, bookCode, effectiveYear, effectivePeriod));
        meta.setCurrentPeriodDepreciationAmount(currentPeriodDepreciationAmount(effectiveCompanyId, bookCode, effectiveYear, effectivePeriod));
        return meta;
    }
    public List<FixedAssetCategoryVO> listCategories(String companyId) {
        String effectiveCompanyId = requireCompanyId(companyId);
        List<FaAssetCategory> categories = listAccessibleCategories(effectiveCompanyId);
        Map<String, FaAssetAccountPolicy> policyMap = loadPolicyMapForCompanies(categories);
        return categories.stream()
                .map(category -> toCategoryVO(category, policyMap.get(policyKey(category.getCompanyId(), category.getId(), BOOK_CODE_FINANCE))))
                .toList();
    }
        public FixedAssetCategoryVO createCategory(FixedAssetCategorySaveDTO dto, String operatorName) {
        validateCategorySave(dto);
        if (findCategoryByCode(dto.getCompanyId(), dto.getCategoryCode()) != null) {
            throw new IllegalStateException("?????????");
        }

        FaAssetCategory category = new FaAssetCategory();
        fillCategory(category, dto, operatorName, true);
        faAssetCategoryMapper.insert(category);

        FaAssetAccountPolicy policy = new FaAssetAccountPolicy();
        fillPolicy(policy, category, dto);
        faAssetAccountPolicyMapper.insert(policy);
        return toCategoryVO(category, policy);
    }
        public FixedAssetCategoryVO updateCategory(Long id, FixedAssetCategorySaveDTO dto, String operatorName) {
        FaAssetCategory existing = requireCategory(id);
        validateCategorySave(dto);

        FaAssetCategory duplicate = findCategoryByCode(existing.getCompanyId(), dto.getCategoryCode());
        if (duplicate != null && !Objects.equals(duplicate.getId(), id)) {
            throw new IllegalStateException("?????????");
        }

        fillCategory(existing, dto, operatorName, false);
        faAssetCategoryMapper.updateById(existing);

        FaAssetAccountPolicy policy = findPolicy(existing.getCompanyId(), existing.getId(), defaultBookCode(dto.getBookCode()));
        if (policy == null) {
            policy = new FaAssetAccountPolicy();
            policy.setCompanyId(existing.getCompanyId());
            policy.setCategoryId(existing.getId());
            policy.setBookCode(defaultBookCode(dto.getBookCode()));
            fillPolicy(policy, existing, dto);
            faAssetAccountPolicyMapper.insert(policy);
        } else {
            fillPolicy(policy, existing, dto);
            faAssetAccountPolicyMapper.updateById(policy);
        }
        return toCategoryVO(existing, policy);
    }
    public List<FixedAssetCardVO> listCards(String companyId, String bookCode, String keyword, Long categoryId, String status) {
        String effectiveCompanyId = requireCompanyId(companyId);
        QueryWrapper<FaAssetCard> query = new QueryWrapper<>();
        query.eq("company_id", effectiveCompanyId)
                .eq("book_code", defaultBookCode(bookCode));
        if (categoryId != null) {
            query.eq("category_id", categoryId);
        }
        if (trimToNull(status) != null) {
            query.eq("status", status.trim());
        }
        String normalizedKeyword = trimToNull(keyword);
        if (normalizedKeyword != null) {
            query.and(wrapper -> wrapper.like("asset_code", normalizedKeyword).or().like("asset_name", normalizedKeyword));
        }
        query.orderByDesc("updated_at").orderByAsc("asset_code");
        return mapCards(faAssetCardMapper.selectList(query));
    }
    public FixedAssetCardVO getCard(Long id) {
        return mapCard(requireCard(id));
    }
        public FixedAssetCardVO createCard(FixedAssetCardSaveDTO dto, String operatorName) {
        validateCardSave(dto, null);
        if (findCardByCode(dto.getCompanyId(), dto.getAssetCode()) != null) {
            throw new IllegalStateException("???????");
        }
        FaAssetCategory category = requireAccessibleCategory(dto.getCompanyId(), dto.getCategoryId(), null);

        FaAssetCard card = new FaAssetCard();
        fillCard(card, dto, category, operatorName, true);
        faAssetCardMapper.insert(card);
        return mapCard(requireCard(card.getId()));
    }
        public FixedAssetCardVO updateCard(Long id, FixedAssetCardSaveDTO dto, String operatorName) {
        FaAssetCard existing = requireCard(id);
        validateCardSave(dto, existing);
        FaAssetCategory category = requireAccessibleCategory(existing.getCompanyId(), dto.getCategoryId(), null);

        fillCard(existing, dto, category, operatorName, false);
        existing.setCompanyId(existing.getCompanyId());
        existing.setAssetCode(existing.getAssetCode());
        existing.setSourceType(trimToNull(existing.getSourceType()) == null ? SOURCE_MANUAL : existing.getSourceType());
        faAssetCardMapper.updateById(existing);
        return mapCard(requireCard(id));
    }
    public FixedAssetTemplateVO getOpeningTemplate(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        String effectiveCompanyId = requireCompanyId(companyId);
        int year = fiscalYear == null ? LocalDate.now().getYear() : fiscalYear;
        int period = normalizePeriod(fiscalPeriod == null ? LocalDate.now().getMonthValue() : fiscalPeriod);
        String sample = String.join("\n",
                "assetCode,assetName,categoryCode,acquireDate,inServiceDate,originalAmount,accumDeprAmount,salvageAmount,usefulLifeMonths,depreciatedMonths,remainingMonths,useDeptId,keeperUserId,status,workTotal,workUsed,remark",
                "FA-OPEN-001,???????,IT_ASSET," + year + "-" + String.format("%02d", period) + "-01," + year + "-" + String.format("%02d", period) + "-01,12000,2000,600,36,6,30,,,IN_USE,,,??????-" + effectiveCompanyId
        );
        FixedAssetTemplateVO template = new FixedAssetTemplateVO();
        template.setFileName("??????????.csv");
        template.setContentType("text/csv");
        template.setTemplateContent(sample);
        return template;
    }
        public FixedAssetOpeningImportResultVO importOpening(FixedAssetOpeningImportDTO dto, String operatorName) {
        String companyId = requireCompanyId(dto.getCompanyId());
        String bookCode = defaultBookCode(dto.getBookCode());
        int year = dto.getFiscalYear() == null ? LocalDate.now().getYear() : dto.getFiscalYear();
        int period = normalizePeriod(dto.getFiscalPeriod() == null ? LocalDate.now().getMonthValue() : dto.getFiscalPeriod());
        ensurePeriodOpen(companyId, bookCode, year, period);

        FaAssetOpeningImport batch = new FaAssetOpeningImport();
        batch.setCompanyId(companyId);
        batch.setBatchNo(nextBusinessNo("OPEN", companyId, year, period));
        batch.setBookCode(bookCode);
        batch.setFiscalYear(year);
        batch.setFiscalPeriod(period);
        batch.setStatus(STATUS_DRAFT);
        batch.setTotalRows(dto.getRows().size());
        batch.setSuccessRows(0);
        batch.setFailedRows(0);
        batch.setCreatedBy(defaultOperator(operatorName));
        faAssetOpeningImportMapper.insert(batch);

        List<FixedAssetOpeningImportLineVO> lineResults = new ArrayList<>();
        List<FaAssetCard> cardsToInsert = new ArrayList<>();
        List<FaAssetOpeningImportLine> linesToInsert = new ArrayList<>();
        Set<String> duplicateCodes = new LinkedHashSet<>();

        for (FixedAssetOpeningImportRowDTO row : dto.getRows()) {
            String message = validateOpeningRow(companyId, row, duplicateCodes);
            FaAssetOpeningImportLine line = new FaAssetOpeningImportLine();
            line.setCompanyId(companyId);
            line.setBatchId(batch.getId());
            line.setRowNo(row.getRowNo());
            line.setAssetCode(trimToNull(row.getAssetCode()));
            line.setAssetName(trimToNull(row.getAssetName()));
            line.setCategoryCode(trimToNull(row.getCategoryCode()));
            line.setResultStatus(message == null ? "SUCCESS" : "FAILED");
            line.setErrorMessage(message);

            if (message == null) {
                FaAssetCategory category = requireAccessibleCategory(companyId, null, row.getCategoryCode());
                FaAssetCard card = buildCardFromOpeningRow(companyId, bookCode, row, category, operatorName);
                cardsToInsert.add(card);
            }

            linesToInsert.add(line);
            lineResults.add(toOpeningLineVO(line));
        }

        boolean hasFailure = linesToInsert.stream().anyMatch(item -> Objects.equals(item.getResultStatus(), "FAILED"));
        if (!hasFailure) {
            for (int index = 0; index < cardsToInsert.size(); index++) {
                FaAssetCard card = cardsToInsert.get(index);
                faAssetCardMapper.insert(card);
                linesToInsert.get(index).setImportedAssetId(card.getId());
                lineResults.get(index).setImportedAssetId(card.getId());
            }
        }

        int successRows = hasFailure ? 0 : cardsToInsert.size();
        int failedRows = hasFailure ? linesToInsert.size() - successRows : 0;
        batch.setSuccessRows(successRows);
        batch.setFailedRows(failedRows);
        batch.setStatus(hasFailure ? "FAILED" : "SUCCESS");
        faAssetOpeningImportMapper.updateById(batch);

        for (FaAssetOpeningImportLine line : linesToInsert) {
            faAssetOpeningImportLineMapper.insert(line);
        }

        return getOpeningImportResult(batch.getId());
    }
    public FixedAssetOpeningImportResultVO getOpeningImportResult(Long batchId) {
        FaAssetOpeningImport batch = faAssetOpeningImportMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalStateException("?????????");
        }
        List<FaAssetOpeningImportLine> lines = faAssetOpeningImportLineMapper.selectList(
                Wrappers.<FaAssetOpeningImportLine>lambdaQuery()
                        .eq(FaAssetOpeningImportLine::getBatchId, batchId)
                        .orderByAsc(FaAssetOpeningImportLine::getRowNo, FaAssetOpeningImportLine::getId)
        );
        FixedAssetOpeningImportResultVO result = new FixedAssetOpeningImportResultVO();
        result.setBatchId(batch.getId());
        result.setCompanyId(batch.getCompanyId());
        result.setBatchNo(batch.getBatchNo());
        result.setBookCode(batch.getBookCode());
        result.setFiscalYear(batch.getFiscalYear());
        result.setFiscalPeriod(batch.getFiscalPeriod());
        result.setStatus(batch.getStatus());
        result.setTotalRows(batch.getTotalRows());
        result.setSuccessRows(batch.getSuccessRows());
        result.setFailedRows(batch.getFailedRows());
        result.setLines(lines.stream().map(this::toOpeningLineVO).toList());
        return result;
    }
    public List<FixedAssetChangeBillVO> listChangeBills(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        String effectiveCompanyId = requireCompanyId(companyId);
        QueryWrapper<FaAssetChangeBill> query = new QueryWrapper<>();
        query.eq("company_id", effectiveCompanyId)
                .eq("book_code", defaultBookCode(bookCode));
        if (fiscalYear != null) {
            query.eq("fiscal_year", fiscalYear);
        }
        if (fiscalPeriod != null) {
            query.eq("fiscal_period", normalizePeriod(fiscalPeriod));
        }
        query.orderByDesc("created_at");
        return faAssetChangeBillMapper.selectList(query).stream().map(this::mapChangeBill).toList();
    }
    public FixedAssetChangeBillVO createChangeBill(FixedAssetChangeBillSaveDTO dto, String operatorName) {
        String companyId = requireCompanyId(dto.getCompanyId());
        String bookCode = defaultBookCode(dto.getBookCode());
        int year = dto.getFiscalYear() == null ? LocalDate.now().getYear() : dto.getFiscalYear();
        int period = normalizePeriod(dto.getFiscalPeriod() == null ? LocalDate.now().getMonthValue() : dto.getFiscalPeriod());
        ensurePeriodOpen(companyId, bookCode, year, period);
        validateChangeType(dto.getBillType());
        dto.getLines().forEach(this::validateChangeLineInput);

        FaAssetChangeBill bill = new FaAssetChangeBill();
        bill.setCompanyId(companyId);
        bill.setBillNo(nextBusinessNo(dto.getBillType(), companyId, year, period));
        bill.setBillType(dto.getBillType());
        bill.setBookCode(bookCode);
        bill.setFiscalYear(year);
        bill.setFiscalPeriod(period);
        bill.setBillDate(parseDateOrDefault(dto.getBillDate(), LocalDate.now()));
        bill.setStatus(STATUS_DRAFT);
        bill.setTotalAmount(sumChangeAmount(dto.getLines()));
        bill.setRemark(trimToNull(dto.getRemark()));
        bill.setCreatedBy(defaultOperator(operatorName));
        faAssetChangeBillMapper.insert(bill);

        for (FixedAssetChangeLineDTO item : dto.getLines()) {
            FaAssetChangeLine line = buildChangeLine(companyId, dto.getBillType(), bill.getId(), item);
            faAssetChangeLineMapper.insert(line);
        }
        return mapChangeBill(bill);
    }
        public FixedAssetChangeBillVO postChangeBill(Long id, String operatorName) {
        FaAssetChangeBill bill = requireChangeBill(id);
        if (!Objects.equals(bill.getStatus(), STATUS_DRAFT)) {
            throw new IllegalStateException("???????????????");
        }
        ensurePeriodOpen(bill.getCompanyId(), bill.getBookCode(), bill.getFiscalYear(), bill.getFiscalPeriod());
        List<FaAssetChangeLine> lines = listChangeLines(bill.getId());
        if (lines.isEmpty()) {
            throw new IllegalStateException("?????????");
        }

        VoucherAccumulator voucher = new VoucherAccumulator();
        for (FaAssetChangeLine line : lines) {
            applyChangeLine(bill, line, operatorName, voucher);
        }

        bill.setStatus(STATUS_POSTED);
        bill.setPostedBy(defaultOperator(operatorName));
        bill.setPostedAt(LocalDateTime.now());
        faAssetChangeBillMapper.updateById(bill);

        if (!voucher.isEmpty()) {
            createVoucher(
                    bill.getCompanyId(),
                    bill.getBookCode(),
                    bill.getFiscalPeriod(),
                    bill.getBillDate(),
                    "鍥哄畾璧勪骇鍙樺姩:" + bill.getBillNo(),
                    voucher,
                    BUSINESS_CHANGE_BILL,
                    bill.getId(),
                    operatorName
            );
        }
        return mapChangeBill(bill);
    }
    public FixedAssetDeprRunVO previewDepreciation(FixedAssetDeprPreviewDTO dto) {
        String companyId = requireCompanyId(dto.getCompanyId());
        String bookCode = defaultBookCode(dto.getBookCode());
        int year = dto.getFiscalYear() == null ? LocalDate.now().getYear() : dto.getFiscalYear();
        int period = normalizePeriod(dto.getFiscalPeriod() == null ? LocalDate.now().getMonthValue() : dto.getFiscalPeriod());
        ensurePeriodOpen(companyId, bookCode, year, period);

        List<FaAssetCard> candidates = eligibleCards(companyId, bookCode, dto.getAssetIds());
        Map<Long, BigDecimal> workloadMap = toWorkloadMap(dto.getWorkloads());
        Map<Long, String> categoryNames = categoryNameMap(candidates);
        List<FixedAssetDeprLineVO> lines = new ArrayList<>();
        BigDecimal total = ZERO;

        for (FaAssetCard card : candidates) {
            DepreciationResult depreciation = calculateDepreciation(card, year, period, workloadMap.get(card.getId()));
            if (depreciation == null || depreciation.amount().compareTo(ZERO) <= 0) {
                continue;
            }
            total = total.add(depreciation.amount());
            lines.add(toDeprLineVO(card, categoryNames.get(card.getCategoryId()), depreciation));
        }

        FixedAssetDeprRunVO preview = new FixedAssetDeprRunVO();
        preview.setCompanyId(companyId);
        preview.setBookCode(bookCode);
        preview.setFiscalYear(year);
        preview.setFiscalPeriod(period);
        preview.setStatus(STATUS_DRAFT);
        preview.setAssetCount(lines.size());
        preview.setTotalAmount(scale(total));
        preview.setRemark(trimToNull(dto.getRemark()));
        preview.setLines(lines);
        return preview;
    }
    public List<FixedAssetDeprRunVO> listDepreciationRuns(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        String effectiveCompanyId = requireCompanyId(companyId);
        QueryWrapper<FaAssetDeprRun> query = new QueryWrapper<>();
        query.eq("company_id", effectiveCompanyId)
                .eq("book_code", defaultBookCode(bookCode));
        if (fiscalYear != null) {
            query.eq("fiscal_year", fiscalYear);
        }
        if (fiscalPeriod != null) {
            query.eq("fiscal_period", normalizePeriod(fiscalPeriod));
        }
        query.orderByDesc("created_at");
        return faAssetDeprRunMapper.selectList(query).stream().map(this::mapDeprRun).toList();
    }
        public FixedAssetDeprRunVO createDepreciationRun(FixedAssetDeprPreviewDTO dto, String operatorName) {
        FixedAssetDeprRunVO preview = previewDepreciation(dto);
        if (preview.getLines().isEmpty()) {
            throw new IllegalStateException("??????????????");
        }
        if (findActiveDeprRun(preview.getCompanyId(), preview.getBookCode(), preview.getFiscalYear(), preview.getFiscalPeriod()) != null) {
            throw new IllegalStateException("???????????");
        }

        FaAssetDeprRun run = new FaAssetDeprRun();
        run.setCompanyId(preview.getCompanyId());
        run.setRunNo(nextBusinessNo("DEPR", preview.getCompanyId(), preview.getFiscalYear(), preview.getFiscalPeriod()));
        run.setBookCode(preview.getBookCode());
        run.setFiscalYear(preview.getFiscalYear());
        run.setFiscalPeriod(preview.getFiscalPeriod());
        run.setStatus(STATUS_DRAFT);
        run.setAssetCount(preview.getAssetCount());
        run.setTotalAmount(preview.getTotalAmount());
        run.setRemark(preview.getRemark());
        run.setCreatedBy(defaultOperator(operatorName));
        faAssetDeprRunMapper.insert(run);

        for (FixedAssetDeprLineVO lineVO : preview.getLines()) {
            FaAssetDeprLine line = new FaAssetDeprLine();
            line.setCompanyId(run.getCompanyId());
            line.setRunId(run.getId());
            line.setAssetId(lineVO.getAssetId());
            line.setAssetCode(lineVO.getAssetCode());
            line.setAssetName(lineVO.getAssetName());
            line.setCategoryId(lineVO.getCategoryId());
            line.setDepreciationMethod(lineVO.getDepreciationMethod());
            line.setWorkAmount(scaleQty(lineVO.getWorkAmount()));
            line.setDepreciationAmount(scale(lineVO.getDepreciationAmount()));
            line.setBeforeAccumAmount(scale(lineVO.getBeforeAccumAmount()));
            line.setAfterAccumAmount(scale(lineVO.getAfterAccumAmount()));
            line.setBeforeNetAmount(scale(lineVO.getBeforeNetAmount()));
            line.setAfterNetAmount(scale(lineVO.getAfterNetAmount()));
            faAssetDeprLineMapper.insert(line);
        }
        return mapDeprRun(run);
    }
        public FixedAssetDeprRunVO postDepreciationRun(Long id, String operatorName) {
        FaAssetDeprRun run = requireDeprRun(id);
        if (!Objects.equals(run.getStatus(), STATUS_DRAFT)) {
            throw new IllegalStateException("??????????????");
        }
        ensurePeriodOpen(run.getCompanyId(), run.getBookCode(), run.getFiscalYear(), run.getFiscalPeriod());

        List<FaAssetDeprLine> lines = listDeprLines(run.getId());
        if (lines.isEmpty()) {
            throw new IllegalStateException("????????");
        }

        VoucherAccumulator voucher = new VoucherAccumulator();
        for (FaAssetDeprLine line : lines) {
            FaAssetCard card = requireCard(line.getAssetId());
            if (hasDepreciated(card, run.getFiscalYear(), run.getFiscalPeriod())) {
                throw new IllegalStateException("?????????????" + card.getAssetCode());
            }
            FaAssetCategory category = requireCategory(card.getCategoryId());
            FaAssetAccountPolicy policy = requirePolicy(category.getCompanyId(), category.getId(), run.getBookCode());

            card.setAccumDeprAmount(scale(line.getAfterAccumAmount()));
            card.setNetAmount(scale(line.getAfterNetAmount()));
            card.setLastDeprYear(run.getFiscalYear());
            card.setLastDeprPeriod(run.getFiscalPeriod());
            card.setDepreciatedMonths((card.getDepreciatedMonths() == null ? 0 : card.getDepreciatedMonths()) + 1);
            if (card.getRemainingMonths() != null && card.getRemainingMonths() > 0) {
                card.setRemainingMonths(card.getRemainingMonths() - 1);
            }
            if (line.getWorkAmount() != null) {
                card.setWorkUsed(scaleQty(defaultQty(card.getWorkUsed()).add(scaleQty(line.getWorkAmount()))));
            }
            if (card.getNetAmount().compareTo(ZERO) <= 0) {
                card.setCanDepreciate(0);
            }
            card.setUpdatedBy(defaultOperator(operatorName));
            faAssetCardMapper.updateById(card);

            voucher.debit(policy.getDeprExpenseAccount(), line.getDepreciationAmount());
            voucher.credit(policy.getAccumDeprAccount(), line.getDepreciationAmount());
        }

        run.setStatus(STATUS_POSTED);
        run.setPostedBy(defaultOperator(operatorName));
        run.setPostedAt(LocalDateTime.now());
        faAssetDeprRunMapper.updateById(run);

        if (!voucher.isEmpty()) {
            createVoucher(
                    run.getCompanyId(),
                    run.getBookCode(),
                    run.getFiscalPeriod(),
                    LocalDate.of(run.getFiscalYear(), run.getFiscalPeriod(), 1),
                    "鍥哄畾璧勪骇鎶樻棫:" + run.getRunNo(),
                    voucher,
                    BUSINESS_DEPRECIATION_RUN,
                    run.getId(),
                    operatorName
            );
        }
        return mapDeprRun(run);
    }
    public List<FixedAssetDisposalBillVO> listDisposalBills(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        String effectiveCompanyId = requireCompanyId(companyId);
        QueryWrapper<FaAssetDisposalBill> query = new QueryWrapper<>();
        query.eq("company_id", effectiveCompanyId)
                .eq("book_code", defaultBookCode(bookCode));
        if (fiscalYear != null) {
            query.eq("fiscal_year", fiscalYear);
        }
        if (fiscalPeriod != null) {
            query.eq("fiscal_period", normalizePeriod(fiscalPeriod));
        }
        query.orderByDesc("created_at");
        return faAssetDisposalBillMapper.selectList(query).stream().map(this::mapDisposalBill).toList();
    }
    public FixedAssetDisposalBillVO createDisposalBill(FixedAssetDisposalBillSaveDTO dto, String operatorName) {
        String companyId = requireCompanyId(dto.getCompanyId());
        String bookCode = defaultBookCode(dto.getBookCode());
        int year = dto.getFiscalYear() == null ? LocalDate.now().getYear() : dto.getFiscalYear();
        int period = normalizePeriod(dto.getFiscalPeriod() == null ? LocalDate.now().getMonthValue() : dto.getFiscalPeriod());
        ensurePeriodOpen(companyId, bookCode, year, period);
        dto.getLines().forEach(this::validateDisposalLineInput);

        FaAssetDisposalBill bill = new FaAssetDisposalBill();
        bill.setCompanyId(companyId);
        bill.setBillNo(nextBusinessNo("DISP", companyId, year, period));
        bill.setBillType("DISPOSAL");
        bill.setBookCode(bookCode);
        bill.setFiscalYear(year);
        bill.setFiscalPeriod(period);
        bill.setBillDate(parseDateOrDefault(dto.getBillDate(), LocalDate.now()));
        bill.setStatus(STATUS_DRAFT);
        bill.setRemark(trimToNull(dto.getRemark()));
        bill.setCreatedBy(defaultOperator(operatorName));
        bill.setTotalOriginalAmount(ZERO);
        bill.setTotalAccumAmount(ZERO);
        bill.setTotalNetAmount(ZERO);
        faAssetDisposalBillMapper.insert(bill);

        BigDecimal totalOriginal = ZERO;
        BigDecimal totalAccum = ZERO;
        BigDecimal totalNet = ZERO;
        for (FixedAssetDisposalLineDTO item : dto.getLines()) {
            FaAssetCard card = requireExistingCard(companyId, item.getAssetId(), item.getAssetCode());
            if (Objects.equals(card.getStatus(), CARD_STATUS_DISPOSED)) {
                throw new IllegalStateException("??????" + card.getAssetCode());
            }
            FaAssetDisposalLine line = new FaAssetDisposalLine();
            line.setCompanyId(companyId);
            line.setBillId(bill.getId());
            line.setAssetId(card.getId());
            line.setAssetCode(card.getAssetCode());
            line.setAssetName(card.getAssetName());
            line.setCategoryId(card.getCategoryId());
            line.setOriginalAmount(scale(card.getOriginalAmount()));
            line.setAccumDeprAmount(scale(card.getAccumDeprAmount()));
            line.setNetAmount(scale(card.getNetAmount()));
            line.setRemark(trimToNull(item.getRemark()));
            faAssetDisposalLineMapper.insert(line);

            totalOriginal = totalOriginal.add(defaultAmount(line.getOriginalAmount()));
            totalAccum = totalAccum.add(defaultAmount(line.getAccumDeprAmount()));
            totalNet = totalNet.add(defaultAmount(line.getNetAmount()));
        }
        bill.setTotalOriginalAmount(scale(totalOriginal));
        bill.setTotalAccumAmount(scale(totalAccum));
        bill.setTotalNetAmount(scale(totalNet));
        faAssetDisposalBillMapper.updateById(bill);
        return mapDisposalBill(bill);
    }
        public FixedAssetDisposalBillVO postDisposalBill(Long id, String operatorName) {
        FaAssetDisposalBill bill = requireDisposalBill(id);
        if (!Objects.equals(bill.getStatus(), STATUS_DRAFT)) {
            throw new IllegalStateException("???????????????");
        }
        ensurePeriodOpen(bill.getCompanyId(), bill.getBookCode(), bill.getFiscalYear(), bill.getFiscalPeriod());

        List<FaAssetDisposalLine> lines = listDisposalLines(bill.getId());
        if (lines.isEmpty()) {
            throw new IllegalStateException("?????????");
        }

        VoucherAccumulator voucher = new VoucherAccumulator();
        for (FaAssetDisposalLine line : lines) {
            FaAssetCard card = requireCard(line.getAssetId());
            FaAssetCategory category = requireCategory(card.getCategoryId());
            FaAssetAccountPolicy policy = requirePolicy(category.getCompanyId(), category.getId(), bill.getBookCode());

            if (Objects.equals(card.getStatus(), CARD_STATUS_DISPOSED)) {
                throw new IllegalStateException("??????" + card.getAssetCode());
            }

            BigDecimal originalAmount = defaultAmount(line.getOriginalAmount());
            BigDecimal accumAmount = defaultAmount(line.getAccumDeprAmount());
            BigDecimal netAmount = defaultAmount(line.getNetAmount());

            voucher.debit(policy.getAccumDeprAccount(), accumAmount);
            voucher.debit(policy.getDisposalAccount(), netAmount);
            voucher.credit(policy.getAssetAccount(), originalAmount);
            if (netAmount.compareTo(ZERO) > 0) {
                voucher.debit(policy.getLossAccount(), netAmount);
                voucher.credit(policy.getDisposalAccount(), netAmount);
            }

            card.setStatus(CARD_STATUS_DISPOSED);
            card.setCanDepreciate(0);
            card.setRemainingMonths(0);
            card.setUpdatedBy(defaultOperator(operatorName));
            faAssetCardMapper.updateById(card);
        }

        bill.setStatus(STATUS_POSTED);
        bill.setPostedBy(defaultOperator(operatorName));
        bill.setPostedAt(LocalDateTime.now());
        faAssetDisposalBillMapper.updateById(bill);

        if (!voucher.isEmpty()) {
            createVoucher(
                    bill.getCompanyId(),
                    bill.getBookCode(),
                    bill.getFiscalPeriod(),
                    bill.getBillDate(),
                    "鍥哄畾璧勪骇澶勭疆:" + bill.getBillNo(),
                    voucher,
                    BUSINESS_DISPOSAL_BILL,
                    bill.getId(),
                    operatorName
            );
        }
        return mapDisposalBill(bill);
    }
        public FixedAssetPeriodStatusVO closePeriod(FixedAssetPeriodCloseDTO dto, String operatorName) {
        String companyId = requireCompanyId(dto.getCompanyId());
        String bookCode = defaultBookCode(dto.getBookCode());
        int year = dto.getFiscalYear() == null ? LocalDate.now().getYear() : dto.getFiscalYear();
        int period = normalizePeriod(dto.getFiscalPeriod() == null ? LocalDate.now().getMonthValue() : dto.getFiscalPeriod());

        FaAssetPeriodClose existing = findPeriodClose(companyId, bookCode, year, period);
        if (existing == null) {
            FaAssetPeriodClose close = new FaAssetPeriodClose();
            close.setCompanyId(companyId);
            close.setBookCode(bookCode);
            close.setFiscalYear(year);
            close.setFiscalPeriod(period);
            close.setStatus(STATUS_CLOSED);
            close.setClosedBy(defaultOperator(operatorName));
            close.setClosedAt(LocalDateTime.now());
            faAssetPeriodCloseMapper.insert(close);
        }
        return getPeriodStatus(companyId, bookCode, year, period);
    }
    public FixedAssetPeriodStatusVO getPeriodStatus(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        String effectiveCompanyId = requireCompanyId(companyId);
        String effectiveBookCode = defaultBookCode(bookCode);
        int year = fiscalYear == null ? LocalDate.now().getYear() : fiscalYear;
        int period = normalizePeriod(fiscalPeriod == null ? LocalDate.now().getMonthValue() : fiscalPeriod);

        FaAssetPeriodClose close = findPeriodClose(effectiveCompanyId, effectiveBookCode, year, period);
        FixedAssetPeriodStatusVO status = new FixedAssetPeriodStatusVO();
        status.setCompanyId(effectiveCompanyId);
        status.setBookCode(effectiveBookCode);
        status.setFiscalYear(year);
        status.setFiscalPeriod(period);
        status.setStatus(close == null ? STATUS_OPEN : STATUS_CLOSED);
        status.setClosedBy(close == null ? null : close.getClosedBy());
        status.setClosedAt(close == null ? null : formatDateTime(close.getClosedAt()));
        return status;
    }
    public FixedAssetVoucherLinkVO getVoucherLink(String companyId, String businessType, Long businessId) {
        String effectiveCompanyId = requireCompanyId(companyId);
        FaAssetVoucherLink link = faAssetVoucherLinkMapper.selectOne(
                Wrappers.<FaAssetVoucherLink>lambdaQuery()
                        .eq(FaAssetVoucherLink::getCompanyId, effectiveCompanyId)
                        .eq(FaAssetVoucherLink::getBusinessType, businessType)
                        .eq(FaAssetVoucherLink::getBusinessId, businessId)
                        .last("limit 1")
        );
        return link == null ? null : toVoucherLinkVO(link);
    }

    private void applyChangeLine(FaAssetChangeBill bill, FaAssetChangeLine line, String operatorName, VoucherAccumulator voucher) {
        String changeType = trimToNull(line.getChangeType());
        if (CHANGE_ADD.equals(changeType)) {
            FaAssetCategory category = requireAccessibleCategory(bill.getCompanyId(), line.getCategoryId(), line.getCategoryCode());
            if (findCardByCode(bill.getCompanyId(), line.getAssetCode()) != null) {
                throw new IllegalStateException("???????: " + line.getAssetCode());
            }
            BigDecimal amount = positiveAmount(line.getChangeAmount(), "changeAmount is required for ADD");
            BigDecimal salvage = defaultAmount(line.getNewSalvageAmount());
            if (salvage.compareTo(ZERO) == 0) {
                salvage = scale(amount.multiply(defaultRate(category.getResidualRate())));
            }

            FaAssetCard card = new FaAssetCard();
            card.setCompanyId(bill.getCompanyId());
            card.setAssetCode(line.getAssetCode());
            card.setAssetName(trimToNull(line.getAssetName()) == null ? line.getAssetCode() : line.getAssetName());
            card.setCategoryId(category.getId());
            card.setCategoryCode(category.getCategoryCode());
            card.setBookCode(bill.getBookCode());
            card.setUseCompanyId(trimToNull(line.getUseCompanyId()) == null ? bill.getCompanyId() : line.getUseCompanyId());
            card.setUseDeptId(line.getUseDeptId());
            card.setKeeperUserId(line.getKeeperUserId());
            card.setManagerUserId(line.getKeeperUserId());
            card.setSourceType(SOURCE_CHANGE_ADD);
            card.setAcquireDate(line.getInServiceDate() == null ? bill.getBillDate() : line.getInServiceDate());
            card.setInServiceDate(line.getInServiceDate() == null ? bill.getBillDate() : line.getInServiceDate());
            card.setOriginalAmount(scale(amount));
            card.setAccumDeprAmount(ZERO);
            card.setSalvageAmount(scale(salvage));
            card.setNetAmount(scale(amount));
            card.setUsefulLifeMonths(defaultUsefulLife(line.getNewUsefulLifeMonths(), category.getUsefulLifeMonths()));
            card.setDepreciatedMonths(0);
            card.setRemainingMonths(defaultUsefulLife(line.getNewRemainingMonths(), card.getUsefulLifeMonths()));
            card.setStatus(CARD_STATUS_IN_USE);
            card.setCanDepreciate(Objects.equals(category.getDepreciable(), 1) ? 1 : 0);
            card.setRemark(trimToNull(line.getRemark()));
            card.setCreatedBy(defaultOperator(operatorName));
            card.setUpdatedBy(defaultOperator(operatorName));
            faAssetCardMapper.insert(card);
            line.setAssetId(card.getId());
            faAssetChangeLineMapper.updateById(line);

            FaAssetAccountPolicy policy = requirePolicy(category.getCompanyId(), category.getId(), bill.getBookCode());
            voucher.debit(policy.getAssetAccount(), amount);
            voucher.credit(policy.getOffsetAccount(), amount);
            return;
        }

        FaAssetCard card = requireExistingCard(bill.getCompanyId(), line.getAssetId(), line.getAssetCode());
        if (Objects.equals(card.getStatus(), CARD_STATUS_DISPOSED)) {
            throw new IllegalStateException("?????????????" + card.getAssetCode());
        }

        if (CHANGE_TRANSFER_DEPT.equals(changeType)) {
            card.setUseDeptId(line.getUseDeptId());
        } else if (CHANGE_TRANSFER_KEEPER.equals(changeType)) {
            card.setKeeperUserId(line.getKeeperUserId());
        } else if (CHANGE_RESIDUAL_ADJUST.equals(changeType)) {
            card.setSalvageAmount(scale(defaultAmount(line.getNewSalvageAmount())));
        } else if (CHANGE_LIFE_ADJUST.equals(changeType)) {
            if (line.getNewUsefulLifeMonths() == null || line.getNewUsefulLifeMonths() <= 0) {
                throw new IllegalArgumentException("?????????0");
            }
            card.setUsefulLifeMonths(line.getNewUsefulLifeMonths());
            card.setRemainingMonths(line.getNewRemainingMonths() == null ? Math.max(0, line.getNewUsefulLifeMonths() - defaultInt(card.getDepreciatedMonths())) : line.getNewRemainingMonths());
        } else if (CHANGE_VALUE_ADJUST.equals(changeType)) {
            BigDecimal original = defaultAmount(card.getOriginalAmount());
            BigDecimal newValue = line.getNewValue() == null ? original.add(defaultAmount(line.getChangeAmount())) : scale(line.getNewValue());
            BigDecimal delta = newValue.subtract(original);
            if (newValue.compareTo(defaultAmount(card.getAccumDeprAmount())) < 0) {
                throw new IllegalArgumentException("?????????????");
            }
            card.setOriginalAmount(scale(newValue));
            card.setNetAmount(scale(newValue.subtract(defaultAmount(card.getAccumDeprAmount()))));

            if (delta.compareTo(ZERO) != 0) {
                FaAssetCategory category = requireCategory(card.getCategoryId());
                FaAssetAccountPolicy policy = requirePolicy(category.getCompanyId(), category.getId(), bill.getBookCode());
                if (delta.compareTo(ZERO) > 0) {
                    voucher.debit(policy.getAssetAccount(), delta);
                    voucher.credit(policy.getOffsetAccount(), delta);
                } else {
                    BigDecimal abs = delta.abs();
                    voucher.debit(policy.getOffsetAccount(), abs);
                    voucher.credit(policy.getAssetAccount(), abs);
                }
            }
        } else {
            throw new IllegalArgumentException("???????????" + changeType);
        }
        card.setUpdatedBy(defaultOperator(operatorName));
        faAssetCardMapper.updateById(card);
    }

    private void validateCategorySave(FixedAssetCategorySaveDTO dto) {
        validateFieldLength(dto.getCategoryCode(), FIXED_ASSET_CODE_MAX_LENGTH, "类别编码");
        validateFieldLength(dto.getCategoryName(), FIXED_ASSET_NAME_MAX_LENGTH, "类别名称");
        String shareScope = trimToNull(dto.getShareScope());
        if (!Objects.equals(shareScope, SHARE_SCOPE_COMPANY) && !Objects.equals(shareScope, SHARE_SCOPE_GROUP)) {
            throw new IllegalArgumentException("???????");
        }
        validateDepreciationMethod(dto.getDepreciationMethod());
        if (dto.getResidualRate().compareTo(BigDecimal.ZERO) < 0 || dto.getResidualRate().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("??????0?1??");
        }
    }

    private void fillCategory(FaAssetCategory category, FixedAssetCategorySaveDTO dto, String operatorName, boolean creating) {
        if (creating) {
            category.setCompanyId(dto.getCompanyId().trim());
            category.setCreatedBy(defaultOperator(operatorName));
        }
        category.setCategoryCode(dto.getCategoryCode().trim());
        category.setCategoryName(dto.getCategoryName().trim());
        category.setShareScope(dto.getShareScope().trim());
        category.setDepreciationMethod(dto.getDepreciationMethod().trim());
        category.setUsefulLifeMonths(dto.getUsefulLifeMonths());
        category.setResidualRate(dto.getResidualRate().setScale(4, RoundingMode.HALF_UP));
        category.setDepreciable(Boolean.FALSE.equals(dto.getDepreciable()) ? 0 : 1);
        category.setStatus(trimToNull(dto.getStatus()) == null ? STATUS_ACTIVE : dto.getStatus().trim());
        category.setRemark(trimToNull(dto.getRemark()));
        category.setUpdatedBy(defaultOperator(operatorName));
    }

    private void fillPolicy(FaAssetAccountPolicy policy, FaAssetCategory category, FixedAssetCategorySaveDTO dto) {
        policy.setCompanyId(category.getCompanyId());
        policy.setCategoryId(category.getId());
        policy.setBookCode(defaultBookCode(dto.getBookCode()));
        policy.setAssetAccount(dto.getAssetAccount().trim());
        policy.setAccumDeprAccount(dto.getAccumDeprAccount().trim());
        policy.setDeprExpenseAccount(dto.getDeprExpenseAccount().trim());
        policy.setDisposalAccount(dto.getDisposalAccount().trim());
        policy.setGainAccount(dto.getGainAccount().trim());
        policy.setLossAccount(dto.getLossAccount().trim());
        policy.setOffsetAccount(dto.getOffsetAccount().trim());
    }

    private void validateCardSave(FixedAssetCardSaveDTO dto, FaAssetCard existing) {
        validateFieldLength(dto.getAssetCode(), FIXED_ASSET_CODE_MAX_LENGTH, "资产编码");
        validateFieldLength(dto.getAssetName(), FIXED_ASSET_NAME_MAX_LENGTH, "资产名称");
        if (dto.getOriginalAmount().compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("??????0");
        }
        if (dto.getAccumDeprAmount().compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("?????????");
        }
        if (dto.getSalvageAmount().compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("????????");
        }
        if (dto.getAccumDeprAmount().compareTo(dto.getOriginalAmount()) > 0) {
            throw new IllegalArgumentException("??????????");
        }
        if (dto.getSalvageAmount().compareTo(dto.getOriginalAmount()) > 0) {
            throw new IllegalArgumentException("?????????");
        }
        if (dto.getDepreciatedMonths() > dto.getUsefulLifeMonths()) {
            throw new IllegalArgumentException("?????????????");
        }
        if (existing == null && trimToNull(dto.getCompanyId()) == null) {
            throw new IllegalArgumentException("??????");
        }
    }

    private void fillCard(FaAssetCard card, FixedAssetCardSaveDTO dto, FaAssetCategory category, String operatorName, boolean creating) {
        if (creating) {
            card.setCompanyId(dto.getCompanyId().trim());
            card.setAssetCode(dto.getAssetCode().trim());
            card.setCreatedBy(defaultOperator(operatorName));
            card.setSourceType(trimToNull(dto.getSourceType()) == null ? SOURCE_MANUAL : dto.getSourceType().trim());
        }
        card.setAssetName(dto.getAssetName().trim());
        card.setCategoryId(category.getId());
        card.setCategoryCode(category.getCategoryCode());
        card.setBookCode(defaultBookCode(dto.getBookCode()));
        card.setUseCompanyId(trimToNull(dto.getUseCompanyId()) == null ? card.getCompanyId() : dto.getUseCompanyId().trim());
        card.setUseDeptId(dto.getUseDeptId());
        card.setKeeperUserId(dto.getKeeperUserId());
        card.setManagerUserId(dto.getManagerUserId());
        card.setAcquireDate(parseNullableDate(dto.getAcquireDate()));
        card.setInServiceDate(parseDateOrThrow(dto.getInServiceDate()));
        card.setOriginalAmount(scale(dto.getOriginalAmount()));
        card.setAccumDeprAmount(scale(dto.getAccumDeprAmount()));
        card.setSalvageAmount(scale(dto.getSalvageAmount()));
        card.setNetAmount(scale(dto.getOriginalAmount().subtract(dto.getAccumDeprAmount())));
        card.setUsefulLifeMonths(dto.getUsefulLifeMonths());
        card.setDepreciatedMonths(dto.getDepreciatedMonths());
        card.setRemainingMonths(dto.getRemainingMonths());
        card.setWorkTotal(scaleQty(dto.getWorkTotal()));
        card.setWorkUsed(scaleQty(dto.getWorkUsed()));
        card.setStatus(trimToNull(dto.getStatus()) == null ? CARD_STATUS_IN_USE : dto.getStatus().trim());
        card.setCanDepreciate(Boolean.FALSE.equals(dto.getCanDepreciate()) ? 0 : 1);
        card.setRemark(trimToNull(dto.getRemark()));
        card.setUpdatedBy(defaultOperator(operatorName));
    }

    private String validateOpeningRow(String companyId, FixedAssetOpeningImportRowDTO row, Set<String> duplicateCodes) {
        String lengthMessage = validateOpeningRowTextLengths(row);
        if (lengthMessage != null) {
            return lengthMessage;
        }
        String assetCode = trimToNull(row.getAssetCode());
        if (assetCode == null) {
            return "assetCode is required";
        }
        if (!duplicateCodes.add(assetCode)) {
            return "assetCode is duplicated in the same batch";
        }
        if (findCardByCode(companyId, assetCode) != null) {
            return "???????";
        }
        if (trimToNull(row.getAssetName()) == null) {
            return "assetName is required";
        }
        if (trimToNull(row.getCategoryCode()) == null) {
            return "categoryCode is required";
        }
        try {
            requireAccessibleCategory(companyId, null, row.getCategoryCode());
        } catch (RuntimeException ex) {
            return ex.getMessage();
        }
        try {
            parseDateOrThrow(row.getInServiceDate());
        } catch (RuntimeException ex) {
            return "inServiceDate is invalid";
        }
        if (row.getOriginalAmount() == null || row.getOriginalAmount().compareTo(ZERO) <= 0) {
            return "??????0";
        }
        if (defaultAmount(row.getAccumDeprAmount()).compareTo(defaultAmount(row.getOriginalAmount())) > 0) {
            return "??????????";
        }
        if (defaultAmount(row.getSalvageAmount()).compareTo(defaultAmount(row.getOriginalAmount())) > 0) {
            return "?????????";
        }
        if (row.getUsefulLifeMonths() == null || row.getUsefulLifeMonths() <= 0) {
            return "usefulLifeMonths must be greater than 0";
        }
        return null;
    }

    private FaAssetCard buildCardFromOpeningRow(String companyId, String bookCode, FixedAssetOpeningImportRowDTO row, FaAssetCategory category, String operatorName) {
        FaAssetCard card = new FaAssetCard();
        card.setCompanyId(companyId);
        card.setAssetCode(row.getAssetCode().trim());
        card.setAssetName(row.getAssetName().trim());
        card.setCategoryId(category.getId());
        card.setCategoryCode(category.getCategoryCode());
        card.setBookCode(bookCode);
        card.setUseCompanyId(companyId);
        card.setUseDeptId(row.getUseDeptId());
        card.setKeeperUserId(row.getKeeperUserId());
        card.setManagerUserId(row.getKeeperUserId());
        card.setSourceType(SOURCE_OPENING);
        card.setAcquireDate(parseNullableDate(row.getAcquireDate()));
        card.setInServiceDate(parseDateOrThrow(row.getInServiceDate()));
        card.setOriginalAmount(scale(row.getOriginalAmount()));
        card.setAccumDeprAmount(scale(defaultAmount(row.getAccumDeprAmount())));
        card.setSalvageAmount(scale(defaultAmount(row.getSalvageAmount())));
        card.setNetAmount(scale(defaultAmount(row.getOriginalAmount()).subtract(defaultAmount(row.getAccumDeprAmount()))));
        card.setUsefulLifeMonths(row.getUsefulLifeMonths());
        card.setDepreciatedMonths(defaultInt(row.getDepreciatedMonths()));
        card.setRemainingMonths(row.getRemainingMonths() == null ? Math.max(0, row.getUsefulLifeMonths() - defaultInt(row.getDepreciatedMonths())) : row.getRemainingMonths());
        card.setWorkTotal(scaleQty(row.getWorkTotal()));
        card.setWorkUsed(scaleQty(row.getWorkUsed()));
        card.setStatus(trimToNull(row.getStatus()) == null ? CARD_STATUS_IN_USE : row.getStatus().trim());
        card.setCanDepreciate(Objects.equals(card.getStatus(), CARD_STATUS_DISPOSED) ? 0 : 1);
        card.setRemark(trimToNull(row.getRemark()));
        card.setCreatedBy(defaultOperator(operatorName));
        card.setUpdatedBy(defaultOperator(operatorName));
        return card;
    }

    private FaAssetChangeLine buildChangeLine(String companyId, String billType, Long billId, FixedAssetChangeLineDTO dto) {
        FaAssetChangeLine line = new FaAssetChangeLine();
        line.setCompanyId(companyId);
        line.setBillId(billId);
        line.setAssetId(dto.getAssetId());
        line.setAssetCode(dto.getAssetCode().trim());
        line.setAssetName(trimToNull(dto.getAssetName()));
        line.setChangeType(billType);
        line.setCategoryId(dto.getCategoryId());
        line.setCategoryCode(trimToNull(dto.getCategoryCode()));
        line.setUseCompanyId(trimToNull(dto.getUseCompanyId()));
        line.setUseDeptId(dto.getUseDeptId());
        line.setKeeperUserId(dto.getKeeperUserId());
        line.setInServiceDate(parseNullableDate(dto.getInServiceDate()));
        line.setChangeAmount(scale(dto.getChangeAmount()));
        line.setNewValue(scale(dto.getNewValue()));
        line.setNewSalvageAmount(scale(dto.getNewSalvageAmount()));
        line.setNewUsefulLifeMonths(dto.getNewUsefulLifeMonths());
        line.setNewRemainingMonths(dto.getNewRemainingMonths());
        line.setRemark(trimToNull(dto.getRemark()));

        if (!CHANGE_ADD.equals(billType)) {
            FaAssetCard card = requireExistingCard(companyId, dto.getAssetId(), dto.getAssetCode());
            line.setAssetId(card.getId());
            line.setAssetName(card.getAssetName());
            line.setOldValue(scale(card.getOriginalAmount()));
            line.setOldSalvageAmount(scale(card.getSalvageAmount()));
            line.setOldUsefulLifeMonths(card.getUsefulLifeMonths());
            line.setOldRemainingMonths(card.getRemainingMonths());
        }
        return line;
    }

    private void validateChangeType(String billType) {
        Set<String> supported = Set.of(CHANGE_ADD, CHANGE_TRANSFER_DEPT, CHANGE_TRANSFER_KEEPER, CHANGE_VALUE_ADJUST, CHANGE_RESIDUAL_ADJUST, CHANGE_LIFE_ADJUST);
        if (!supported.contains(trimToNull(billType))) {
            throw new IllegalArgumentException("???????");
        }
    }

    private void validateChangeLineInput(FixedAssetChangeLineDTO dto) {
        validateFieldLength(dto.getAssetCode(), FIXED_ASSET_CODE_MAX_LENGTH, "资产编码");
        validateFieldLength(dto.getAssetName(), FIXED_ASSET_NAME_MAX_LENGTH, "资产名称");
        validateFieldLength(dto.getCategoryCode(), FIXED_ASSET_CODE_MAX_LENGTH, "类别编码");
    }

    private void validateDisposalLineInput(FixedAssetDisposalLineDTO dto) {
        validateFieldLength(dto.getAssetCode(), FIXED_ASSET_CODE_MAX_LENGTH, "资产编码");
    }

    private String validateOpeningRowTextLengths(FixedAssetOpeningImportRowDTO row) {
        try {
            validateFieldLength(row.getAssetCode(), FIXED_ASSET_CODE_MAX_LENGTH, "资产编码");
            validateFieldLength(row.getAssetName(), FIXED_ASSET_NAME_MAX_LENGTH, "资产名称");
            validateFieldLength(row.getCategoryCode(), FIXED_ASSET_CODE_MAX_LENGTH, "类别编码");
        } catch (IllegalArgumentException ex) {
            return prefixRowMessage(row.getRowNo(), ex.getMessage());
        }
        return null;
    }

    private void validateFieldLength(String value, int maxLength, String fieldLabel) {
        String normalized = trimToNull(value);
        if (normalized != null && normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldLabel + "长度不能超过 " + maxLength + " 个字符");
        }
    }

    private String prefixRowMessage(Integer rowNo, String message) {
        if (rowNo == null) {
            return message;
        }
        return "第 " + rowNo + " 行" + message;
    }

    private void validateDepreciationMethod(String method) {
        Set<String> supported = Set.of(METHOD_STRAIGHT_LINE, METHOD_WORKLOAD, METHOD_DOUBLE_DECLINING);
        if (!supported.contains(trimToNull(method))) {
            throw new IllegalArgumentException("???????");
        }
    }

    private DepreciationResult calculateDepreciation(FaAssetCard card, int fiscalYear, int fiscalPeriod, BigDecimal workload) {
        if (!Objects.equals(card.getCanDepreciate(), 1) || !Objects.equals(card.getStatus(), CARD_STATUS_IN_USE) || card.getInServiceDate() == null) {
            return null;
        }
        LocalDate firstDayOfPeriod = LocalDate.of(fiscalYear, fiscalPeriod, 1);
        if (!card.getInServiceDate().isBefore(firstDayOfPeriod) || hasDepreciated(card, fiscalYear, fiscalPeriod)) {
            return null;
        }

        FaAssetCategory category = requireCategory(card.getCategoryId());
        BigDecimal original = defaultAmount(card.getOriginalAmount());
        BigDecimal accum = defaultAmount(card.getAccumDeprAmount());
        BigDecimal salvage = defaultAmount(card.getSalvageAmount());
        BigDecimal depreciableBase = original.subtract(salvage).max(ZERO);
        BigDecimal remainingDepreciable = depreciableBase.subtract(accum).max(ZERO);
        if (remainingDepreciable.compareTo(ZERO) <= 0) {
            return null;
        }

        BigDecimal amount;
        if (METHOD_WORKLOAD.equals(category.getDepreciationMethod())) {
            BigDecimal totalWork = defaultQty(card.getWorkTotal());
            if (totalWork.compareTo(ZERO_QTY) <= 0 || workload == null || workload.compareTo(ZERO_QTY) <= 0) {
                return null;
            }
            amount = remainingDepreciable.multiply(scaleQty(workload)).divide(totalWork, 2, RoundingMode.HALF_UP);
        } else if (METHOD_DOUBLE_DECLINING.equals(category.getDepreciationMethod())) {
            BigDecimal usefulLifeYears = BigDecimal.valueOf(Math.max(1, category.getUsefulLifeMonths())).divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
            BigDecimal monthlyRate = BigDecimal.valueOf(2).divide(usefulLifeYears, 6, RoundingMode.HALF_UP).divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
            BigDecimal doubleDeclining = defaultAmount(card.getNetAmount()).multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            int remainingMonths = Math.max(1, defaultInt(card.getRemainingMonths()));
            BigDecimal straightLineFloor = remainingDepreciable.divide(BigDecimal.valueOf(remainingMonths), 2, RoundingMode.HALF_UP);
            amount = doubleDeclining.max(straightLineFloor);
        } else {
            int remainingMonths = Math.max(1, defaultInt(card.getRemainingMonths()));
            amount = remainingDepreciable.divide(BigDecimal.valueOf(remainingMonths), 2, RoundingMode.HALF_UP);
        }

        amount = amount.min(remainingDepreciable).max(ZERO);
        if (amount.compareTo(ZERO) <= 0) {
            return null;
        }
        return new DepreciationResult(scale(amount), scaleQty(workload), scale(accum), scale(accum.add(amount)), scale(defaultAmount(card.getNetAmount())), scale(defaultAmount(card.getNetAmount()).subtract(amount)));
    }

    private boolean hasDepreciated(FaAssetCard card, int fiscalYear, int fiscalPeriod) {
        if (card.getLastDeprYear() == null || card.getLastDeprPeriod() == null) {
            return false;
        }
        int last = card.getLastDeprYear() * 100 + card.getLastDeprPeriod();
        int current = fiscalYear * 100 + fiscalPeriod;
        return last >= current;
    }

    private List<FaAssetCard> eligibleCards(String companyId, String bookCode, List<Long> assetIds) {
        QueryWrapper<FaAssetCard> query = new QueryWrapper<>();
        query.eq("company_id", companyId).eq("book_code", bookCode).eq("status", CARD_STATUS_IN_USE).eq("can_depreciate", 1);
        if (assetIds != null && !assetIds.isEmpty()) {
            query.in("id", assetIds);
        }
        query.orderByAsc("asset_code");
        return faAssetCardMapper.selectList(query);
    }

    private int previewDepreciationCount(String companyId, String bookCode, int fiscalYear, int fiscalPeriod) {
        int count = 0;
        for (FaAssetCard card : eligibleCards(companyId, bookCode, null)) {
            if (calculateDepreciation(card, fiscalYear, fiscalPeriod, null) != null) {
                count++;
            }
        }
        return count;
    }

    private BigDecimal currentPeriodDepreciationAmount(String companyId, String bookCode, int fiscalYear, int fiscalPeriod) {
        FaAssetDeprRun run = faAssetDeprRunMapper.selectOne(Wrappers.<FaAssetDeprRun>lambdaQuery()
                .eq(FaAssetDeprRun::getCompanyId, companyId)
                .eq(FaAssetDeprRun::getBookCode, bookCode)
                .eq(FaAssetDeprRun::getFiscalYear, fiscalYear)
                .eq(FaAssetDeprRun::getFiscalPeriod, fiscalPeriod)
                .eq(FaAssetDeprRun::getStatus, STATUS_POSTED)
                .orderByDesc(FaAssetDeprRun::getPostedAt, FaAssetDeprRun::getId)
                .last("limit 1"));
        return run == null ? ZERO : scale(run.getTotalAmount());
    }

    private long countCards(String companyId, String bookCode) {
        Long count = faAssetCardMapper.selectCount(Wrappers.<FaAssetCard>lambdaQuery().eq(FaAssetCard::getCompanyId, companyId).eq(FaAssetCard::getBookCode, bookCode));
        return count == null ? 0L : count;
    }

    private FaAssetDeprRun findActiveDeprRun(String companyId, String bookCode, int fiscalYear, int fiscalPeriod) {
        return faAssetDeprRunMapper.selectOne(Wrappers.<FaAssetDeprRun>lambdaQuery()
                .eq(FaAssetDeprRun::getCompanyId, companyId)
                .eq(FaAssetDeprRun::getBookCode, bookCode)
                .eq(FaAssetDeprRun::getFiscalYear, fiscalYear)
                .eq(FaAssetDeprRun::getFiscalPeriod, fiscalPeriod)
                .ne(FaAssetDeprRun::getStatus, STATUS_VOID)
                .last("limit 1"));
    }

    private FixedAssetCategoryVO toCategoryVO(FaAssetCategory category, FaAssetAccountPolicy policy) {
        FixedAssetCategoryVO vo = new FixedAssetCategoryVO();
        vo.setId(category.getId());
        vo.setCompanyId(category.getCompanyId());
        vo.setCategoryCode(category.getCategoryCode());
        vo.setCategoryName(category.getCategoryName());
        vo.setShareScope(category.getShareScope());
        vo.setDepreciationMethod(category.getDepreciationMethod());
        vo.setUsefulLifeMonths(category.getUsefulLifeMonths());
        vo.setResidualRate(category.getResidualRate());
        vo.setDepreciable(Objects.equals(category.getDepreciable(), 1));
        vo.setStatus(category.getStatus());
        vo.setRemark(category.getRemark());
        if (policy != null) {
            vo.setBookCode(policy.getBookCode());
            vo.setAssetAccount(policy.getAssetAccount());
            vo.setAccumDeprAccount(policy.getAccumDeprAccount());
            vo.setDeprExpenseAccount(policy.getDeprExpenseAccount());
            vo.setDisposalAccount(policy.getDisposalAccount());
            vo.setGainAccount(policy.getGainAccount());
            vo.setLossAccount(policy.getLossAccount());
            vo.setOffsetAccount(policy.getOffsetAccount());
        }
        return vo;
    }

    private List<FixedAssetCardVO> mapCards(List<FaAssetCard> cards) {
        if (cards.isEmpty()) {
            return List.of();
        }
        Map<Long, FaAssetCategory> categories = loadCategoryMap(cards.stream().map(FaAssetCard::getCategoryId).toList());
        Map<Long, SystemDepartment> departments = loadDepartmentMap(cards.stream().map(FaAssetCard::getUseDeptId).toList());
        Map<Long, User> users = loadUserMap(cards.stream().flatMap(card -> List.of(card.getKeeperUserId(), card.getManagerUserId()).stream()).filter(Objects::nonNull).toList());
        return cards.stream().sorted(Comparator.comparing(FaAssetCard::getAssetCode)).map(card -> toCardVO(card, categories, departments, users)).toList();
    }

    private FixedAssetCardVO mapCard(FaAssetCard card) {
        return mapCards(List.of(card)).get(0);
    }

    private FixedAssetCardVO toCardVO(FaAssetCard card, Map<Long, FaAssetCategory> categories, Map<Long, SystemDepartment> departments, Map<Long, User> users) {
        FixedAssetCardVO vo = new FixedAssetCardVO();
        FaAssetCategory category = categories.get(card.getCategoryId());
        vo.setId(card.getId());
        vo.setCompanyId(card.getCompanyId());
        vo.setAssetCode(card.getAssetCode());
        vo.setAssetName(card.getAssetName());
        vo.setCategoryId(card.getCategoryId());
        vo.setCategoryCode(card.getCategoryCode());
        vo.setCategoryName(category == null ? card.getCategoryCode() : category.getCategoryName());
        vo.setDepreciationMethod(category == null ? null : category.getDepreciationMethod());
        vo.setBookCode(card.getBookCode());
        vo.setUseCompanyId(card.getUseCompanyId());
        vo.setUseDeptId(card.getUseDeptId());
        vo.setUseDeptName(card.getUseDeptId() == null ? null : labelOfDepartment(departments.get(card.getUseDeptId())));
        vo.setKeeperUserId(card.getKeeperUserId());
        vo.setKeeperName(card.getKeeperUserId() == null ? null : labelOfUser(users.get(card.getKeeperUserId())));
        vo.setManagerUserId(card.getManagerUserId());
        vo.setManagerName(card.getManagerUserId() == null ? null : labelOfUser(users.get(card.getManagerUserId())));
        vo.setSourceType(card.getSourceType());
        vo.setAcquireDate(formatDate(card.getAcquireDate()));
        vo.setInServiceDate(formatDate(card.getInServiceDate()));
        vo.setOriginalAmount(scale(card.getOriginalAmount()));
        vo.setAccumDeprAmount(scale(card.getAccumDeprAmount()));
        vo.setSalvageAmount(scale(card.getSalvageAmount()));
        vo.setNetAmount(scale(card.getNetAmount()));
        vo.setUsefulLifeMonths(card.getUsefulLifeMonths());
        vo.setDepreciatedMonths(card.getDepreciatedMonths());
        vo.setRemainingMonths(card.getRemainingMonths());
        vo.setWorkTotal(scaleQty(card.getWorkTotal()));
        vo.setWorkUsed(scaleQty(card.getWorkUsed()));
        vo.setStatus(card.getStatus());
        vo.setCanDepreciate(Objects.equals(card.getCanDepreciate(), 1));
        vo.setLastDeprYear(card.getLastDeprYear());
        vo.setLastDeprPeriod(card.getLastDeprPeriod());
        vo.setRemark(card.getRemark());
        return vo;
    }

    private FixedAssetOptionVO toCompanyOption(SystemCompany company) {
        return option(company.getCompanyId(), company.getCompanyCode() + " - " + company.getCompanyName());
    }

    private FixedAssetOptionVO toDepartmentOption(SystemDepartment department) {
        return option(String.valueOf(department.getId()), department.getDeptCode() + " - " + department.getDeptName());
    }

    private FixedAssetOptionVO toUserOption(User user) {
        return option(String.valueOf(user.getId()), user.getUsername() + " - " + user.getName());
    }

    private FixedAssetOptionVO toCategoryOption(FaAssetCategory category) {
        return option(String.valueOf(category.getId()), category.getCategoryCode() + " - " + category.getCategoryName());
    }

    private FixedAssetOptionVO option(String value, String label) {
        FixedAssetOptionVO option = new FixedAssetOptionVO();
        option.setValue(value);
        option.setLabel(label);
        return option;
    }

    private FixedAssetChangeBillVO mapChangeBill(FaAssetChangeBill bill) {
        FixedAssetChangeBillVO vo = new FixedAssetChangeBillVO();
        vo.setId(bill.getId());
        vo.setCompanyId(bill.getCompanyId());
        vo.setBillNo(bill.getBillNo());
        vo.setBillType(bill.getBillType());
        vo.setBookCode(bill.getBookCode());
        vo.setFiscalYear(bill.getFiscalYear());
        vo.setFiscalPeriod(bill.getFiscalPeriod());
        vo.setBillDate(formatDate(bill.getBillDate()));
        vo.setStatus(bill.getStatus());
        vo.setTotalAmount(scale(bill.getTotalAmount()));
        vo.setRemark(bill.getRemark());
        vo.setPostedAt(formatDateTime(bill.getPostedAt()));
        vo.setVoucherLink(getVoucherLink(bill.getCompanyId(), BUSINESS_CHANGE_BILL, bill.getId()));
        vo.setLines(mapChangeLines(listChangeLines(bill.getId())));
        return vo;
    }

    private List<FixedAssetChangeLineVO> mapChangeLines(List<FaAssetChangeLine> lines) {
        if (lines.isEmpty()) {
            return List.of();
        }
        Map<Long, SystemDepartment> departments = loadDepartmentMap(lines.stream().map(FaAssetChangeLine::getUseDeptId).toList());
        Map<Long, User> users = loadUserMap(lines.stream().map(FaAssetChangeLine::getKeeperUserId).filter(Objects::nonNull).toList());
        return lines.stream().map(line -> {
            FixedAssetChangeLineVO vo = new FixedAssetChangeLineVO();
            vo.setId(line.getId());
            vo.setAssetId(line.getAssetId());
            vo.setAssetCode(line.getAssetCode());
            vo.setAssetName(line.getAssetName());
            vo.setChangeType(line.getChangeType());
            vo.setCategoryId(line.getCategoryId());
            vo.setCategoryCode(line.getCategoryCode());
            vo.setUseCompanyId(line.getUseCompanyId());
            vo.setUseDeptId(line.getUseDeptId());
            vo.setUseDeptName(line.getUseDeptId() == null ? null : labelOfDepartment(departments.get(line.getUseDeptId())));
            vo.setKeeperUserId(line.getKeeperUserId());
            vo.setKeeperName(line.getKeeperUserId() == null ? null : labelOfUser(users.get(line.getKeeperUserId())));
            vo.setInServiceDate(formatDate(line.getInServiceDate()));
            vo.setChangeAmount(scale(line.getChangeAmount()));
            vo.setOldValue(scale(line.getOldValue()));
            vo.setNewValue(scale(line.getNewValue()));
            vo.setOldSalvageAmount(scale(line.getOldSalvageAmount()));
            vo.setNewSalvageAmount(scale(line.getNewSalvageAmount()));
            vo.setOldUsefulLifeMonths(line.getOldUsefulLifeMonths());
            vo.setNewUsefulLifeMonths(line.getNewUsefulLifeMonths());
            vo.setOldRemainingMonths(line.getOldRemainingMonths());
            vo.setNewRemainingMonths(line.getNewRemainingMonths());
            vo.setRemark(line.getRemark());
            return vo;
        }).toList();
    }

    private FixedAssetDeprRunVO mapDeprRun(FaAssetDeprRun run) {
        FixedAssetDeprRunVO vo = new FixedAssetDeprRunVO();
        vo.setId(run.getId());
        vo.setCompanyId(run.getCompanyId());
        vo.setRunNo(run.getRunNo());
        vo.setBookCode(run.getBookCode());
        vo.setFiscalYear(run.getFiscalYear());
        vo.setFiscalPeriod(run.getFiscalPeriod());
        vo.setStatus(run.getStatus());
        vo.setAssetCount(run.getAssetCount());
        vo.setTotalAmount(scale(run.getTotalAmount()));
        vo.setRemark(run.getRemark());
        vo.setPostedAt(formatDateTime(run.getPostedAt()));
        vo.setVoucherLink(getVoucherLink(run.getCompanyId(), BUSINESS_DEPRECIATION_RUN, run.getId()));
        vo.setLines(mapDeprLines(listDeprLines(run.getId())));
        return vo;
    }

    private List<FixedAssetDeprLineVO> mapDeprLines(List<FaAssetDeprLine> lines) {
        if (lines.isEmpty()) {
            return List.of();
        }
        Map<Long, FaAssetCategory> categories = loadCategoryMap(lines.stream().map(FaAssetDeprLine::getCategoryId).toList());
        return lines.stream().map(line -> {
            FixedAssetDeprLineVO vo = new FixedAssetDeprLineVO();
            vo.setId(line.getId());
            vo.setAssetId(line.getAssetId());
            vo.setAssetCode(line.getAssetCode());
            vo.setAssetName(line.getAssetName());
            vo.setCategoryId(line.getCategoryId());
            vo.setCategoryName(categories.containsKey(line.getCategoryId()) ? categories.get(line.getCategoryId()).getCategoryName() : null);
            vo.setDepreciationMethod(line.getDepreciationMethod());
            vo.setWorkAmount(scaleQty(line.getWorkAmount()));
            vo.setDepreciationAmount(scale(line.getDepreciationAmount()));
            vo.setBeforeAccumAmount(scale(line.getBeforeAccumAmount()));
            vo.setAfterAccumAmount(scale(line.getAfterAccumAmount()));
            vo.setBeforeNetAmount(scale(line.getBeforeNetAmount()));
            vo.setAfterNetAmount(scale(line.getAfterNetAmount()));
            return vo;
        }).toList();
    }

    private FixedAssetDeprLineVO toDeprLineVO(FaAssetCard card, String categoryName, DepreciationResult depreciation) {
        FixedAssetDeprLineVO vo = new FixedAssetDeprLineVO();
        vo.setAssetId(card.getId());
        vo.setAssetCode(card.getAssetCode());
        vo.setAssetName(card.getAssetName());
        vo.setCategoryId(card.getCategoryId());
        vo.setCategoryName(categoryName);
        vo.setDepreciationMethod(requireCategory(card.getCategoryId()).getDepreciationMethod());
        vo.setWorkAmount(scaleQty(depreciation.workAmount()));
        vo.setDepreciationAmount(scale(depreciation.amount()));
        vo.setBeforeAccumAmount(scale(depreciation.beforeAccum()));
        vo.setAfterAccumAmount(scale(depreciation.afterAccum()));
        vo.setBeforeNetAmount(scale(depreciation.beforeNet()));
        vo.setAfterNetAmount(scale(depreciation.afterNet()));
        return vo;
    }

    private FixedAssetDisposalBillVO mapDisposalBill(FaAssetDisposalBill bill) {
        FixedAssetDisposalBillVO vo = new FixedAssetDisposalBillVO();
        vo.setId(bill.getId());
        vo.setCompanyId(bill.getCompanyId());
        vo.setBillNo(bill.getBillNo());
        vo.setBillType(bill.getBillType());
        vo.setBookCode(bill.getBookCode());
        vo.setFiscalYear(bill.getFiscalYear());
        vo.setFiscalPeriod(bill.getFiscalPeriod());
        vo.setBillDate(formatDate(bill.getBillDate()));
        vo.setStatus(bill.getStatus());
        vo.setTotalOriginalAmount(scale(bill.getTotalOriginalAmount()));
        vo.setTotalAccumAmount(scale(bill.getTotalAccumAmount()));
        vo.setTotalNetAmount(scale(bill.getTotalNetAmount()));
        vo.setRemark(bill.getRemark());
        vo.setPostedAt(formatDateTime(bill.getPostedAt()));
        vo.setVoucherLink(getVoucherLink(bill.getCompanyId(), BUSINESS_DISPOSAL_BILL, bill.getId()));
        vo.setLines(mapDisposalLines(listDisposalLines(bill.getId())));
        return vo;
    }

    private List<FixedAssetDisposalLineVO> mapDisposalLines(List<FaAssetDisposalLine> lines) {
        if (lines.isEmpty()) {
            return List.of();
        }
        Map<Long, FaAssetCategory> categories = loadCategoryMap(lines.stream().map(FaAssetDisposalLine::getCategoryId).toList());
        return lines.stream().map(line -> {
            FixedAssetDisposalLineVO vo = new FixedAssetDisposalLineVO();
            vo.setId(line.getId());
            vo.setAssetId(line.getAssetId());
            vo.setAssetCode(line.getAssetCode());
            vo.setAssetName(line.getAssetName());
            vo.setCategoryId(line.getCategoryId());
            vo.setCategoryName(categories.containsKey(line.getCategoryId()) ? categories.get(line.getCategoryId()).getCategoryName() : null);
            vo.setOriginalAmount(scale(line.getOriginalAmount()));
            vo.setAccumDeprAmount(scale(line.getAccumDeprAmount()));
            vo.setNetAmount(scale(line.getNetAmount()));
            vo.setRemark(line.getRemark());
            return vo;
        }).toList();
    }

    private FixedAssetOpeningImportLineVO toOpeningLineVO(FaAssetOpeningImportLine line) {
        FixedAssetOpeningImportLineVO vo = new FixedAssetOpeningImportLineVO();
        vo.setRowNo(line.getRowNo());
        vo.setAssetCode(line.getAssetCode());
        vo.setAssetName(line.getAssetName());
        vo.setCategoryCode(line.getCategoryCode());
        vo.setResultStatus(line.getResultStatus());
        vo.setErrorMessage(line.getErrorMessage());
        vo.setImportedAssetId(line.getImportedAssetId());
        return vo;
    }

    private FixedAssetVoucherLinkVO toVoucherLinkVO(FaAssetVoucherLink link) {
        FixedAssetVoucherLinkVO vo = new FixedAssetVoucherLinkVO();
        vo.setId(link.getId());
        vo.setCompanyId(link.getCompanyId());
        vo.setBusinessType(link.getBusinessType());
        vo.setBusinessId(link.getBusinessId());
        vo.setVoucherNo(link.getVoucherNo());
        vo.setIperiod(link.getIperiod());
        vo.setCsign(link.getCsign());
        vo.setInoId(link.getInoId());
        vo.setRemark(link.getRemark());
        return vo;
    }

    private List<FaAssetCategory> listAccessibleCategories(String companyId) {
        return faAssetCategoryMapper.selectList(Wrappers.<FaAssetCategory>lambdaQuery()
                .and(wrapper -> wrapper.eq(FaAssetCategory::getCompanyId, companyId).or().eq(FaAssetCategory::getShareScope, SHARE_SCOPE_GROUP))
                .orderByAsc(FaAssetCategory::getCategoryCode, FaAssetCategory::getId));
    }

    private Map<String, FaAssetAccountPolicy> loadPolicyMapForCompanies(List<FaAssetCategory> categories) {
        if (categories.isEmpty()) {
            return Map.of();
        }
        List<Long> categoryIds = categories.stream().map(FaAssetCategory::getId).toList();
        List<String> companyIds = categories.stream().map(FaAssetCategory::getCompanyId).distinct().toList();
        List<FaAssetAccountPolicy> policies = faAssetAccountPolicyMapper.selectList(Wrappers.<FaAssetAccountPolicy>lambdaQuery()
                .in(FaAssetAccountPolicy::getCategoryId, categoryIds)
                .in(FaAssetAccountPolicy::getCompanyId, companyIds)
                .eq(FaAssetAccountPolicy::getBookCode, BOOK_CODE_FINANCE));
        Map<String, FaAssetAccountPolicy> map = new HashMap<>();
        for (FaAssetAccountPolicy policy : policies) {
            map.put(policyKey(policy.getCompanyId(), policy.getCategoryId(), policy.getBookCode()), policy);
        }
        return map;
    }

    private Map<Long, FaAssetCategory> loadCategoryMap(Collection<Long> ids) {
        List<Long> filteredIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (filteredIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, FaAssetCategory> map = new HashMap<>();
        for (FaAssetCategory category : faAssetCategoryMapper.selectBatchIds(filteredIds)) {
            map.put(category.getId(), category);
        }
        return map;
    }

    private Map<Long, SystemDepartment> loadDepartmentMap(Collection<Long> ids) {
        List<Long> filteredIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (filteredIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, SystemDepartment> map = new HashMap<>();
        for (SystemDepartment department : systemDepartmentMapper.selectBatchIds(filteredIds)) {
            map.put(department.getId(), department);
        }
        return map;
    }

    private Map<Long, User> loadUserMap(Collection<Long> ids) {
        List<Long> filteredIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (filteredIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, User> map = new HashMap<>();
        for (User user : userMapper.selectBatchIds(filteredIds)) {
            map.put(user.getId(), user);
        }
        return map;
    }

    private List<SystemCompany> loadEnabledCompanies() {
        return systemCompanyMapper.selectList(Wrappers.<SystemCompany>lambdaQuery().eq(SystemCompany::getStatus, 1).orderByAsc(SystemCompany::getCompanyCode, SystemCompany::getCompanyId));
    }

    private List<SystemDepartment> loadEnabledDepartments(String companyId) {
        return systemDepartmentMapper.selectList(Wrappers.<SystemDepartment>lambdaQuery().eq(SystemDepartment::getCompanyId, companyId).eq(SystemDepartment::getStatus, 1).orderByAsc(SystemDepartment::getDeptCode, SystemDepartment::getId));
    }

    private List<User> loadEnabledUsers(String companyId) {
        QueryWrapper<User> query = new QueryWrapper<>();
        query.eq("status", 1);
        if (trimToNull(companyId) != null) {
            query.eq("company_id", companyId);
        }
        query.orderByAsc("username", "id");
        return userMapper.selectList(query);
    }

    private FaAssetCategory requireCategory(Long id) {
        FaAssetCategory category = faAssetCategoryMapper.selectById(id);
        if (category == null) {
            throw new IllegalStateException("???????");
        }
        return category;
    }

    private FaAssetCategory requireAccessibleCategory(String companyId, Long categoryId, String categoryCode) {
        FaAssetCategory category = categoryId != null ? faAssetCategoryMapper.selectById(categoryId) : findCategoryByCode(companyId, categoryCode);
        if (category == null) {
            throw new IllegalStateException("???????");
        }
        if (!Objects.equals(category.getCompanyId(), companyId) && !Objects.equals(category.getShareScope(), SHARE_SCOPE_GROUP)) {
            throw new IllegalStateException("?????????????");
        }
        return category;
    }

    private FaAssetCategory findCategoryByCode(String companyId, String categoryCode) {
        String normalizedCode = trimToNull(categoryCode);
        if (normalizedCode == null) {
            return null;
        }
        return faAssetCategoryMapper.selectOne(Wrappers.<FaAssetCategory>lambdaQuery()
                .and(wrapper -> wrapper.eq(FaAssetCategory::getCompanyId, companyId).or().eq(FaAssetCategory::getShareScope, SHARE_SCOPE_GROUP))
                .eq(FaAssetCategory::getCategoryCode, normalizedCode)
                .last("limit 1"));
    }

    private FaAssetAccountPolicy requirePolicy(String companyId, Long categoryId, String bookCode) {
        FaAssetAccountPolicy policy = findPolicy(companyId, categoryId, bookCode);
        if (policy == null) {
            throw new IllegalStateException("???????????");
        }
        return policy;
    }

    private FaAssetAccountPolicy findPolicy(String companyId, Long categoryId, String bookCode) {
        return faAssetAccountPolicyMapper.selectOne(Wrappers.<FaAssetAccountPolicy>lambdaQuery()
                .eq(FaAssetAccountPolicy::getCompanyId, companyId)
                .eq(FaAssetAccountPolicy::getCategoryId, categoryId)
                .eq(FaAssetAccountPolicy::getBookCode, defaultBookCode(bookCode))
                .last("limit 1"));
    }

    private FaAssetCard requireCard(Long id) {
        FaAssetCard card = faAssetCardMapper.selectById(id);
        if (card == null) {
            throw new IllegalStateException("???????");
        }
        return card;
    }

    private FaAssetCard requireExistingCard(String companyId, Long assetId, String assetCode) {
        FaAssetCard card = assetId == null ? findCardByCode(companyId, assetCode) : requireCard(assetId);
        if (card == null || !Objects.equals(card.getCompanyId(), companyId)) {
            throw new IllegalStateException("???????");
        }
        return card;
    }

    private FaAssetCard findCardByCode(String companyId, String assetCode) {
        String normalizedCode = trimToNull(assetCode);
        if (normalizedCode == null) {
            return null;
        }
        return faAssetCardMapper.selectOne(Wrappers.<FaAssetCard>lambdaQuery().eq(FaAssetCard::getCompanyId, companyId).eq(FaAssetCard::getAssetCode, normalizedCode).last("limit 1"));
    }

    private FaAssetChangeBill requireChangeBill(Long id) {
        FaAssetChangeBill bill = faAssetChangeBillMapper.selectById(id);
        if (bill == null) {
            throw new IllegalStateException("????????");
        }
        return bill;
    }

    private List<FaAssetChangeLine> listChangeLines(Long billId) {
        return faAssetChangeLineMapper.selectList(Wrappers.<FaAssetChangeLine>lambdaQuery().eq(FaAssetChangeLine::getBillId, billId).orderByAsc(FaAssetChangeLine::getId));
    }

    private FaAssetDeprRun requireDeprRun(Long id) {
        FaAssetDeprRun run = faAssetDeprRunMapper.selectById(id);
        if (run == null) {
            throw new IllegalStateException("???????");
        }
        return run;
    }

    private List<FaAssetDeprLine> listDeprLines(Long runId) {
        return faAssetDeprLineMapper.selectList(Wrappers.<FaAssetDeprLine>lambdaQuery().eq(FaAssetDeprLine::getRunId, runId).orderByAsc(FaAssetDeprLine::getId));
    }

    private FaAssetDisposalBill requireDisposalBill(Long id) {
        FaAssetDisposalBill bill = faAssetDisposalBillMapper.selectById(id);
        if (bill == null) {
            throw new IllegalStateException("????????");
        }
        return bill;
    }

    private List<FaAssetDisposalLine> listDisposalLines(Long billId) {
        return faAssetDisposalLineMapper.selectList(Wrappers.<FaAssetDisposalLine>lambdaQuery().eq(FaAssetDisposalLine::getBillId, billId).orderByAsc(FaAssetDisposalLine::getId));
    }

    private FaAssetPeriodClose findPeriodClose(String companyId, String bookCode, int fiscalYear, int fiscalPeriod) {
        return faAssetPeriodCloseMapper.selectOne(Wrappers.<FaAssetPeriodClose>lambdaQuery()
                .eq(FaAssetPeriodClose::getCompanyId, companyId)
                .eq(FaAssetPeriodClose::getBookCode, bookCode)
                .eq(FaAssetPeriodClose::getFiscalYear, fiscalYear)
                .eq(FaAssetPeriodClose::getFiscalPeriod, fiscalPeriod)
                .last("limit 1"));
    }

    private void ensurePeriodOpen(String companyId, String bookCode, int fiscalYear, int fiscalPeriod) {
        if (findPeriodClose(companyId, bookCode, fiscalYear, fiscalPeriod) != null) {
            throw new IllegalStateException("???????");
        }
    }

    private void createVoucher(String companyId, String bookCode, Integer period, LocalDate billDate, String summary, VoucherAccumulator voucher, String businessType, Long businessId, String operatorName) {
        if (voucher.isEmpty()) {
            return;
        }
        if (voucher.totalDebit().compareTo(voucher.totalCredit()) != 0) {
            throw new IllegalStateException("?????????????");
        }

        String lockKey = companyId + "#" + period + "#" + VOUCHER_TYPE;
        Object lock = voucherNoLocks.computeIfAbsent(lockKey, unused -> new Object());
        synchronized (lock) {
            int voucherNo = nextVoucherNo(companyId, period, VOUCHER_TYPE);
            int rowNo = 1;
            LocalDateTime billDateTime = billDate.atStartOfDay();
            for (Map.Entry<String, BigDecimal> entry : voucher.debits().entrySet()) {
                if (entry.getValue().compareTo(ZERO) > 0) {
                    glAccvouchMapper.insert(buildVoucherRow(companyId, period, voucherNo, rowNo++, billDateTime, summary, entry.getKey(), entry.getValue(), null, operatorName, businessType));
                }
            }
            for (Map.Entry<String, BigDecimal> entry : voucher.credits().entrySet()) {
                if (entry.getValue().compareTo(ZERO) > 0) {
                    glAccvouchMapper.insert(buildVoucherRow(companyId, period, voucherNo, rowNo++, billDateTime, summary, entry.getKey(), null, entry.getValue(), operatorName, businessType));
                }
            }
            FaAssetVoucherLink link = new FaAssetVoucherLink();
            link.setCompanyId(companyId);
            link.setBusinessType(businessType);
            link.setBusinessId(businessId);
            link.setVoucherNo(buildVoucherNo(companyId, period, VOUCHER_TYPE, voucherNo));
            link.setIperiod(period);
            link.setCsign(VOUCHER_TYPE);
            link.setInoId(voucherNo);
            link.setRemark(summary);
            faAssetVoucherLinkMapper.insert(link);
        }
    }

    private GlAccvouch buildVoucherRow(String companyId, Integer period, Integer voucherNo, Integer rowNo, LocalDateTime billDate, String summary, String accountCode, BigDecimal debit, BigDecimal credit, String operatorName, String businessType) {
        GlAccvouch row = new GlAccvouch();
        row.setCompanyId(companyId);
        row.setIperiod(period);
        row.setCsign(VOUCHER_TYPE);
        row.setIsignseq(VOUCHER_SIGN_SEQUENCE);
        row.setInoId(voucherNo);
        row.setInid(rowNo);
        row.setDbillDate(billDate);
        row.setIdoc(0);
        row.setCbill(defaultOperator(operatorName));
        row.setCcheck(null);
        row.setCbook(null);
        row.setIbook(0);
        row.setIflag(0);
        row.setCtext1(businessType);
        row.setCtext2(summary);
        row.setCdigest(summary);
        row.setCcode(accountCode);
        row.setCexchName("CNY");
        row.setNfrat(BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP));
        row.setMd(scale(debit));
        row.setMc(scale(credit));
        row.setMdF(scale(debit));
        row.setMcF(scale(credit));
        row.setNdS(null);
        row.setNcS(null);
        return row;
    }

    private int nextVoucherNo(String companyId, Integer period, String voucherType) {
        List<GlAccvouch> rows = glAccvouchMapper.selectList(Wrappers.<GlAccvouch>lambdaQuery()
                .eq(GlAccvouch::getCompanyId, companyId)
                .eq(GlAccvouch::getIperiod, period)
                .eq(GlAccvouch::getCsign, voucherType)
                .orderByDesc(GlAccvouch::getInoId)
                .last("limit 1"));
        if (rows.isEmpty() || rows.get(0).getInoId() == null) {
            return 1;
        }
        return rows.get(0).getInoId() + 1;
    }

    private String buildVoucherNo(String companyId, Integer period, String voucherType, Integer voucherNo) {
        return companyId + "~" + period + "~" + voucherType + "~" + voucherNo;
    }

    private String nextBusinessNo(String prefix, String companyId, int fiscalYear, int fiscalPeriod) {
        String normalizedPrefix = prefix.replaceAll("[^A-Z_]", "");
        String head = "FA-" + normalizedPrefix + "-" + fiscalYear + String.format("%02d", fiscalPeriod) + "-";
        long existing = defaultLong(faAssetChangeBillMapper.selectCount(new QueryWrapper<FaAssetChangeBill>().likeRight("bill_no", head)));
        long existingRuns = defaultLong(faAssetDeprRunMapper.selectCount(new QueryWrapper<FaAssetDeprRun>().likeRight("run_no", head)));
        long existingDisposals = defaultLong(faAssetDisposalBillMapper.selectCount(new QueryWrapper<FaAssetDisposalBill>().likeRight("bill_no", head)));
        long existingImports = defaultLong(faAssetOpeningImportMapper.selectCount(new QueryWrapper<FaAssetOpeningImport>().likeRight("batch_no", head)));
        long next = Math.max(Math.max(existing, existingRuns), Math.max(existingDisposals, existingImports)) + 1;
        return head + String.format("%04d", next);
    }

    private Map<Long, BigDecimal> toWorkloadMap(List<FixedAssetDeprWorkloadDTO> workloads) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (workloads == null) {
            return map;
        }
        for (FixedAssetDeprWorkloadDTO item : workloads) {
            if (item.getAssetId() != null && item.getWorkAmount() != null) {
                map.put(item.getAssetId(), scaleQty(item.getWorkAmount()));
            }
        }
        return map;
    }

    private Map<Long, String> categoryNameMap(List<FaAssetCard> cards) {
        Map<Long, String> map = new HashMap<>();
        for (FaAssetCategory category : loadCategoryMap(cards.stream().map(FaAssetCard::getCategoryId).toList()).values()) {
            map.put(category.getId(), category.getCategoryName());
        }
        return map;
    }

    private String resolveDefaultCompanyId(String companyId, User currentUser, List<SystemCompany> companies) {
        String candidate = trimToNull(companyId);
        if (candidate != null) {
            return candidate;
        }
        candidate = currentUser == null ? null : trimToNull(currentUser.getCompanyId());
        if (candidate != null) {
            return candidate;
        }
        if (companies.isEmpty()) {
            throw new IllegalStateException("?????????");
        }
        return companies.get(0).getCompanyId();
    }

    private String defaultBookCode(String bookCode) {
        String normalized = trimToNull(bookCode);
        return normalized == null ? BOOK_CODE_FINANCE : normalized;
    }

    private String requireCompanyId(String companyId) {
        String normalized = trimToNull(companyId);
        if (normalized == null) {
            throw new IllegalArgumentException("??????");
        }
        return normalized;
    }

    private Integer normalizePeriod(Integer fiscalPeriod) {
        if (fiscalPeriod == null || fiscalPeriod < 1 || fiscalPeriod > 12) {
            throw new IllegalArgumentException("???????1?12??");
        }
        return fiscalPeriod;
    }

    private String labelOfDepartment(SystemDepartment department) {
        return department == null ? null : department.getDeptName();
    }

    private String labelOfUser(User user) {
        if (user == null) {
            return null;
        }
        return trimToNull(user.getName()) == null ? user.getUsername() : user.getName();
    }

    private String policyKey(String companyId, Long categoryId, String bookCode) {
        return companyId + "#" + categoryId + "#" + bookCode;
    }

    private BigDecimal positiveAmount(BigDecimal value, String message) {
        BigDecimal normalized = defaultAmount(value);
        if (normalized.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private int defaultUsefulLife(Integer preferred, Integer fallback) {
        Integer candidate = preferred == null || preferred <= 0 ? fallback : preferred;
        if (candidate == null || candidate <= 0) {
            throw new IllegalArgumentException("????????");
        }
        return candidate;
    }

    private BigDecimal defaultRate(BigDecimal rate) {
        if (rate == null) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return rate.setScale(4, RoundingMode.HALF_UP);
    }

    private int defaultInt(Integer value) { return value == null ? 0 : value; }
    private long defaultLong(Long value) { return value == null ? 0L : value; }
    private BigDecimal defaultAmount(BigDecimal value) { return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal defaultQty(BigDecimal value) { return value == null ? ZERO_QTY : value.setScale(6, RoundingMode.HALF_UP); }
    private BigDecimal scale(BigDecimal value) { return value == null ? null : value.setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal scaleQty(BigDecimal value) { return value == null ? null : value.setScale(6, RoundingMode.HALF_UP); }

    private LocalDate parseDateOrThrow(String value) {
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("???????");
        }
    }

    private LocalDate parseDateOrDefault(String value, LocalDate fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? fallback : parseDateOrThrow(normalized);
    }

    private LocalDate parseNullableDate(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : parseDateOrThrow(normalized);
    }

    private String formatDate(LocalDate value) { return value == null ? null : value.format(DATE_FORMATTER); }
    private String formatDateTime(LocalDateTime value) { return value == null ? null : value.toString(); }
    private String defaultOperator(String operatorName) { return trimToNull(operatorName) == null ? "system" : operatorName.trim(); }

    private String trimToNull(String value) {
        if (value == null) { return null; }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal sumChangeAmount(List<FixedAssetChangeLineDTO> lines) {
        BigDecimal total = ZERO;
        for (FixedAssetChangeLineDTO line : lines) {
            total = total.add(defaultAmount(line.getChangeAmount()));
        }
        return scale(total);
    }

    private record DepreciationResult(BigDecimal amount, BigDecimal workAmount, BigDecimal beforeAccum, BigDecimal afterAccum, BigDecimal beforeNet, BigDecimal afterNet) {
    }

    private static final class VoucherAccumulator {
        private final Map<String, BigDecimal> debits = new LinkedHashMap<>();
        private final Map<String, BigDecimal> credits = new LinkedHashMap<>();

        void debit(String account, BigDecimal amount) { add(debits, account, amount); }
        void credit(String account, BigDecimal amount) { add(credits, account, amount); }
        Map<String, BigDecimal> debits() { return debits; }
        Map<String, BigDecimal> credits() { return credits; }
        BigDecimal totalDebit() { return debits.values().stream().filter(Objects::nonNull).reduce(ZERO, BigDecimal::add); }
        BigDecimal totalCredit() { return credits.values().stream().filter(Objects::nonNull).reduce(ZERO, BigDecimal::add); }
        boolean isEmpty() { return debits.isEmpty() && credits.isEmpty(); }

        private void add(Map<String, BigDecimal> bucket, String account, BigDecimal amount) {
            if (amount == null || amount.compareTo(ZERO) == 0) { return; }
            String normalizedAccount = account == null ? null : account.trim();
            if (normalizedAccount == null || normalizedAccount.isEmpty()) {
                throw new IllegalArgumentException("????????????");
            }
            bucket.merge(normalizedAccount, amount.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
        }
    }
}
