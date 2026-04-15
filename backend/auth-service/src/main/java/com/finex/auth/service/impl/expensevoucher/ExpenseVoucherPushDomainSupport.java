// 涓氬姟鍩燂細鎶ラ攢鍑瘉鐢熸垚涓庢帹閫?
// 鏂囦欢瑙掕壊锛氶鍩熻鍒欐敮鎾戠被
// 涓婁笅娓稿叧绯伙細涓婃父閫氬父鏉ヨ嚜 鎶ラ攢鍗曞嚟璇佺敓鎴愭帴鍙ｅ拰璐㈠姟鎿嶄綔鍏ュ彛锛屼笅娓镐細缁х画鍗忚皟 鍑瘉鏄犲皠銆佹帹閫佽褰曞拰鎶ラ攢鍗曞嚟璇佺姸鎬併€?
// 椋庨櫓鎻愰啋锛氭敼鍧忓悗鏈€瀹规槗褰卞搷 閲嶅鐢熸垚鍑瘉銆佸嚟璇佸唴瀹归敊璇拰鎺ㄩ€佽褰曚笉涓€鑷淬€?

package com.finex.auth.service.impl.expensevoucher;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ExpenseVoucherPageVO;
import com.finex.auth.dto.ExpenseVoucherPushBatchResultVO;
import com.finex.auth.dto.ExpenseVoucherPushDTO;
import com.finex.auth.dto.ExpenseVoucherPushDocumentVO;
import com.finex.auth.dto.ExpenseVoucherPushResultVO;
import com.finex.auth.dto.FinanceVoucherEntryDTO;
import com.finex.auth.dto.FinanceVoucherSaveDTO;
import com.finex.auth.dto.FinanceVoucherSaveResultVO;
import com.finex.auth.entity.ExpVoucherPushBatch;
import com.finex.auth.entity.ExpVoucherPushDocument;
import com.finex.auth.entity.ExpVoucherPushEntry;
import com.finex.auth.entity.ExpVoucherSubjectMapping;
import com.finex.auth.entity.ExpVoucherTemplatePolicy;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ExpenseVoucherPushDomainSupport锛氶鍩熻鍒欐敮鎾戠被銆?
 * 鎵挎帴 鎶ラ攢鍗曞嚟璇佹帹閫佺殑鏍稿績涓氬姟瑙勫垯銆?
 * 鏀硅繖閲屾椂锛岃鐗瑰埆鍏虫敞 閲嶅鐢熸垚鍑瘉銆佸嚟璇佸唴瀹归敊璇拰鎺ㄩ€佽褰曚笉涓€鑷存槸鍚︿細琚竴璧峰甫鍧忋€?
 */
public class ExpenseVoucherPushDomainSupport extends AbstractExpenseVoucherGenerationSupport {

    /**
     * 鍒濆鍖栬繖涓被鎵€闇€鐨勪緷璧栫粍浠躲€?
     */
    public ExpenseVoucherPushDomainSupport(Dependencies dependencies) {
        super(dependencies);
    }

    /**
     * 鑾峰彇鎺ㄩ€佸崟鎹€?
     */
    public ExpenseVoucherPageVO<ExpenseVoucherPushDocumentVO> getPushDocuments(String companyId, String templateCode, String keyword, String pushStatus, String dateFrom, String dateTo, Integer page, Integer pageSize) {
        Map<String, String> companyMap = companyNameMap();
        Map<String, String> expenseTypeMap = expenseTypeNameMap();
        Map<String, ExpVoucherPushDocument> pushMap = listPushDocuments().stream()
                .collect(Collectors.toMap(ExpVoucherPushDocument::getDocumentCode, Function.identity(), (left, right) -> left));

        List<ExpenseVoucherPushDocumentVO> rows = new ArrayList<>();
        for (ProcessDocumentInstance document : listApprovedDocuments()) {
            String resolvedCompanyId = resolveDocumentCompanyId(document);
            if (!matchesCompany(resolvedCompanyId, companyId)) {
                continue;
            }
            if (hasText(templateCode) && !Objects.equals(trim(templateCode), trim(document.getTemplateCode()))) {
                continue;
            }
            if (!matchesKeyword(keyword, document.getDocumentCode(), document.getTemplateName(), document.getSubmitterName(), document.getDocumentTitle())) {
                continue;
            }
            if (!matchesDateRange(resolveBusinessDate(document), dateFrom, dateTo)) {
                continue;
            }
            ExpVoucherPushDocument pushDocument = pushMap.get(document.getDocumentCode());
            String resolvedPushStatus = pushDocument == null ? PUSH_STATUS_UNPUSHED : defaultText(pushDocument.getPushStatus(), PUSH_STATUS_UNPUSHED);
            if (hasText(pushStatus) && !Objects.equals(trim(pushStatus), resolvedPushStatus)) {
                continue;
            }
            List<ProcessDocumentExpenseDetail> details = listExpenseDetails(document.getDocumentCode());
            ExpenseVoucherPushDocumentVO row = new ExpenseVoucherPushDocumentVO();
            row.setCompanyId(resolvedCompanyId);
            row.setCompanyName(companyMap.getOrDefault(resolvedCompanyId, resolvedCompanyId));
            row.setDocumentCode(document.getDocumentCode());
            row.setTemplateCode(document.getTemplateCode());
            row.setTemplateName(document.getTemplateName());
            row.setSubmitterUserId(document.getSubmitterUserId());
            row.setSubmitterName(document.getSubmitterName());
            row.setTotalAmount(zero(document.getTotalAmount()));
            row.setFinishedAt(formatDateTime(document.getFinishedAt()));
            row.setExpenseSummary(buildExpenseSummary(details, expenseTypeMap));
            row.setPushStatus(resolvedPushStatus);
            row.setPushStatusLabel(resolvePushStatusLabel(resolvedPushStatus));
            row.setFailureReason(pushDocument == null ? null : trim(pushDocument.getErrorMessage()));
            row.setVoucherNo(pushDocument == null ? null : pushDocument.getVoucherNo());
            row.setCanPush(canPush(document, pushDocument, resolvedCompanyId, details));
            rows.add(row);
        }
        rows.sort(Comparator.comparing(ExpenseVoucherPushDocumentVO::getFinishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ExpenseVoucherPushDocumentVO::getDocumentCode, Comparator.nullsLast(Comparator.reverseOrder())));
        return buildPage(rows, page, pageSize);
    }

    /**
     * 鎺ㄩ€佸崟鎹€?
     */
    public ExpenseVoucherPushBatchResultVO pushDocuments(ExpenseVoucherPushDTO dto, Long currentUserId, String currentUsername) {
        LinkedHashSet<String> documentCodes = dto == null ? new LinkedHashSet<>() : dto.getDocumentCodes().stream()
                .map(this::trim)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (documentCodes.isEmpty()) {
            throw new IllegalArgumentException("鐠囩兘鈧瀚ㄩ棁鈧憰浣瑰腹闁胶娈戦崡鏇熷祦");
        }

        Map<String, ProcessDocumentInstance> documentMap = documentInstanceMapper.selectList(
                        Wrappers.<ProcessDocumentInstance>lambdaQuery().in(ProcessDocumentInstance::getDocumentCode, documentCodes)
                ).stream()
                .collect(Collectors.toMap(ProcessDocumentInstance::getDocumentCode, Function.identity(), (left, right) -> left));

        Map<String, CompanyBatchContext> batchMap = new LinkedHashMap<>();
        ExpenseVoucherPushBatchResultVO result = new ExpenseVoucherPushBatchResultVO();
        result.setSuccessCount(0);
        result.setFailureCount(0);

        for (String documentCode : documentCodes) {
            ProcessDocumentInstance document = documentMap.get(documentCode);
            if (document == null) {
                result.getResults().add(buildFailureResult(documentCode, null, null, null, "閸楁洘宓佹稉宥呯摠閸︻煉绱濋弮鐘崇《閹恒劑鈧礁鍤熺拠?"));
                result.setFailureCount(result.getFailureCount() + 1);
                continue;
            }

            String companyId = null;
            CompanyBatchContext batchContext = null;
            try {
                companyId = requireDocumentCompanyId(document);
                batchContext = batchMap.computeIfAbsent(companyId, key -> createBatchContext(key, currentUsername));
                ExpenseVoucherPushResultVO pushResult = pushOneDocument(document, batchContext, currentUserId, currentUsername);
                result.getResults().add(pushResult);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception ex) {
                String errorMessage = defaultText(ex.getMessage(), "閹恒劑鈧礁銇戠拹?");
                if (companyId != null) {
                    batchContext = batchContext == null ? batchMap.computeIfAbsent(companyId, key -> createBatchContext(key, currentUsername)) : batchContext;
                    saveFailedPushDocument(document, batchContext, errorMessage);
                }
                result.getResults().add(buildFailureResult(document.getDocumentCode(), companyId, document.getTemplateCode(), document.getTemplateName(), errorMessage));
                result.setFailureCount(result.getFailureCount() + 1);
            }
        }

        String latestBatchNo = null;
        for (CompanyBatchContext batchContext : batchMap.values()) {
            ExpVoucherPushBatch batch = batchContext.batch;
            batch.setDocumentCount(batchContext.documentCount);
            batch.setSuccessCount(batchContext.successCount);
            batch.setFailureCount(batchContext.failureCount);
            batch.setStatus(batchContext.failureCount > 0 ? (batchContext.successCount > 0 ? "PARTIAL" : PUSH_STATUS_FAILED) : PUSH_STATUS_SUCCESS);
            pushBatchMapper.updateById(batch);
            latestBatchNo = batch.getBatchNo();
        }
        result.setLatestBatchNo(latestBatchNo);
        return result;
    }
    /**
     * 鎺ㄩ€丱ne鍗曟嵁銆?
     */
    private ExpenseVoucherPushResultVO pushOneDocument(ProcessDocumentInstance document, CompanyBatchContext batchContext, Long currentUserId, String currentUsername) {
        if (!isVoucherEligibleDocumentStatus(trim(document.getStatus()))) {
            throw new IllegalStateException("\u5f53\u524d\u5355\u636e\u672a\u5904\u4e8e\u53ef\u751f\u6210\u51ed\u8bc1\u7684\u72b6\u6001");
        }

        String companyId = requireDocumentCompanyId(document);
        ExpVoucherTemplatePolicy templatePolicy = requireEnabledTemplatePolicy(companyId, document.getTemplateCode());
        List<ProcessDocumentExpenseDetail> details = listExpenseDetails(document.getDocumentCode());
        if (details.isEmpty()) {
            throw new IllegalStateException("褰撳墠鍗曟嵁娌℃湁鍙敓鎴愬嚟璇佺殑璐圭敤鏄庣粏");
        }

        LinkedHashMap<String, BigDecimal> debitAmounts = aggregateExpenseAmounts(details);
        Map<String, String> expenseTypeMap = expenseTypeNameMap();
        Map<String, ExpVoucherSubjectMapping> subjectMap = listEnabledSubjectMappings(companyId, document.getTemplateCode()).stream()
                .collect(Collectors.toMap(ExpVoucherSubjectMapping::getExpenseTypeCode, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        for (String expenseTypeCode : debitAmounts.keySet()) {
            if (!subjectMap.containsKey(expenseTypeCode)) {
                throw new IllegalStateException("鏈厤缃垂鐢ㄧ被鍨嬪搴旂殑浼氳绉戠洰: " + expenseTypeMap.getOrDefault(expenseTypeCode, expenseTypeCode));
            }
        }

        FinanceVoucherSaveDTO saveDTO = buildVoucherSaveDTO(document, companyId, templatePolicy, debitAmounts, subjectMap, expenseTypeMap, currentUsername);
        FinanceVoucherSaveResultVO saveResult = financeVoucherService.saveVoucher(saveDTO, currentUserId, currentUsername);
        ExpVoucherPushDocument pushDocument = saveSuccessPushDocument(document, batchContext, saveResult);
        rebuildPushEntries(pushDocument, saveDTO, subjectMap, templatePolicy, expenseTypeMap);
        batchContext.documentCount += 1;
        batchContext.successCount += 1;
        return buildSuccessResult(document, companyId, saveResult);
    }
    /**
     * 缁勮鍑瘉SaveDTO銆?
     */
    private FinanceVoucherSaveDTO buildVoucherSaveDTO(ProcessDocumentInstance document, String companyId, ExpVoucherTemplatePolicy templatePolicy, LinkedHashMap<String, BigDecimal> debitAmounts, Map<String, ExpVoucherSubjectMapping> subjectMap, Map<String, String> expenseTypeMap, String currentUsername) {
        LocalDate businessDate = resolveBusinessDate(document);
        FinanceVoucherSaveDTO dto = new FinanceVoucherSaveDTO();
        dto.setCompanyId(companyId);
        dto.setDbillDate(businessDate.format(DATE_FORMATTER));
        dto.setIperiod(businessDate.getMonthValue());
        dto.setCsign(defaultText(templatePolicy.getVoucherType(), DEFAULT_VOUCHER_TYPE));
        dto.setCbill(currentUsername);
        dto.setIdoc(0);
        dto.setCtext1("閹躲儵鏀㈤崙顓＄槈-" + document.getDocumentCode());
        dto.setCtext2(defaultText(document.getTemplateName(), document.getTemplateCode()));

        List<FinanceVoucherEntryDTO> entries = new ArrayList<>();
        BigDecimal totalAmount = ZERO;
        for (Map.Entry<String, BigDecimal> entry : debitAmounts.entrySet()) {
            String expenseTypeCode = entry.getKey();
            BigDecimal amount = zero(entry.getValue());
            ExpVoucherSubjectMapping mapping = subjectMap.get(expenseTypeCode);
            FinanceVoucherEntryDTO voucherEntry = new FinanceVoucherEntryDTO();
            voucherEntry.setCdigest(resolveSummary(templatePolicy.getSummaryRule(), document, expenseTypeMap.getOrDefault(expenseTypeCode, expenseTypeCode)));
            voucherEntry.setCcode(mapping.getDebitAccountCode());
            voucherEntry.setMd(amount);
            voucherEntry.setMc(ZERO);
            entries.add(voucherEntry);
            totalAmount = totalAmount.add(amount);
        }
        FinanceVoucherEntryDTO creditEntry = new FinanceVoucherEntryDTO();
        creditEntry.setCdigest(resolveSummary(templatePolicy.getSummaryRule(), document, "闁炬儼顢戠粔鎴犳窗"));
        creditEntry.setCcode(templatePolicy.getCreditAccountCode());
        creditEntry.setMd(ZERO);
        creditEntry.setMc(totalAmount);
        entries.add(creditEntry);
        dto.setEntries(entries);
        return dto;
    }

    /**
     * 淇濆瓨Success鎺ㄩ€佸崟鎹€?
     */
    private ExpVoucherPushDocument saveSuccessPushDocument(ProcessDocumentInstance document, CompanyBatchContext batchContext, FinanceVoucherSaveResultVO saveResult) {
        ExpVoucherPushDocument row = findPushDocument(batchContext.batch.getCompanyId(), document.getDocumentCode());
        if (row == null) {
            row = new ExpVoucherPushDocument();
            row.setCompanyId(batchContext.batch.getCompanyId());
            row.setDocumentCode(document.getDocumentCode());
        }
        row.setBatchId(batchContext.batch.getId());
        row.setBatchNo(batchContext.batch.getBatchNo());
        row.setTemplateCode(document.getTemplateCode());
        row.setTemplateName(document.getTemplateName());
        row.setSubmitterUserId(document.getSubmitterUserId());
        row.setSubmitterName(document.getSubmitterName());
        row.setTotalAmount(zero(document.getTotalAmount()));
        row.setPushStatus(PUSH_STATUS_SUCCESS);
        row.setVoucherNo(saveResult.getVoucherNo());
        row.setVoucherType(saveResult.getCsign());
        row.setVoucherNumber(saveResult.getInoId());
        row.setFiscalPeriod(saveResult.getIperiod());
        row.setBillDate(resolveBusinessDate(document));
        row.setErrorMessage(null);
        row.setPushedAt(LocalDateTime.now());
        if (row.getId() == null) {
            pushDocumentMapper.insert(row);
        } else {
            pushDocumentMapper.updateById(row);
        }
        return row;
    }

    /**
     * 淇濆瓨Failed鎺ㄩ€佸崟鎹€?
     */
    private void saveFailedPushDocument(ProcessDocumentInstance document, CompanyBatchContext batchContext, String errorMessage) {
        ExpVoucherPushDocument row = findPushDocument(batchContext.batch.getCompanyId(), document.getDocumentCode());
        if (row == null) {
            row = new ExpVoucherPushDocument();
            row.setCompanyId(batchContext.batch.getCompanyId());
            row.setDocumentCode(document.getDocumentCode());
        }
        row.setBatchId(batchContext.batch.getId());
        row.setBatchNo(batchContext.batch.getBatchNo());
        row.setTemplateCode(document.getTemplateCode());
        row.setTemplateName(document.getTemplateName());
        row.setSubmitterUserId(document.getSubmitterUserId());
        row.setSubmitterName(document.getSubmitterName());
        row.setTotalAmount(zero(document.getTotalAmount()));
        row.setPushStatus(PUSH_STATUS_FAILED);
        row.setVoucherNo(null);
        row.setVoucherType(null);
        row.setVoucherNumber(null);
        row.setFiscalPeriod(resolveBusinessDate(document).getMonthValue());
        row.setBillDate(resolveBusinessDate(document));
        row.setErrorMessage(trim(errorMessage));
        row.setPushedAt(LocalDateTime.now());
        if (row.getId() == null) {
            pushDocumentMapper.insert(row);
        } else {
            pushDocumentMapper.updateById(row);
        }
        pushEntryMapper.delete(Wrappers.<ExpVoucherPushEntry>lambdaQuery().eq(ExpVoucherPushEntry::getPushDocumentId, row.getId()));
        batchContext.documentCount += 1;
        batchContext.failureCount += 1;
    }

    private void rebuildPushEntries(ExpVoucherPushDocument pushDocument, FinanceVoucherSaveDTO saveDTO, Map<String, ExpVoucherSubjectMapping> subjectMap, ExpVoucherTemplatePolicy templatePolicy, Map<String, String> expenseTypeMap) {
        pushEntryMapper.delete(Wrappers.<ExpVoucherPushEntry>lambdaQuery().eq(ExpVoucherPushEntry::getPushDocumentId, pushDocument.getId()));
        Map<String, String> debitAccountExpenseTypeMap = new LinkedHashMap<>();
        for (ExpVoucherSubjectMapping mapping : subjectMap.values()) {
            debitAccountExpenseTypeMap.putIfAbsent(mapping.getDebitAccountCode(), mapping.getExpenseTypeCode());
        }
        for (int index = 0; index < saveDTO.getEntries().size(); index++) {
            FinanceVoucherEntryDTO source = saveDTO.getEntries().get(index);
            ExpVoucherPushEntry entry = new ExpVoucherPushEntry();
            entry.setCompanyId(pushDocument.getCompanyId());
            entry.setPushDocumentId(pushDocument.getId());
            entry.setEntryNo(index + 1);
            entry.setDigest(source.getCdigest());
            entry.setAccountCode(source.getCcode());
            entry.setAccountName(resolveAccountName(source.getCcode()));
            boolean isDebit = zero(source.getMd()).compareTo(ZERO) > 0;
            entry.setDirection(isDebit ? "DEBIT" : "CREDIT");
            entry.setAmount(isDebit ? zero(source.getMd()) : zero(source.getMc()));
            if (isDebit) {
                String expenseTypeCode = debitAccountExpenseTypeMap.get(source.getCcode());
                entry.setExpenseTypeCode(expenseTypeCode);
                entry.setExpenseTypeName(expenseTypeMap.getOrDefault(expenseTypeCode, expenseTypeCode));
            } else {
                entry.setExpenseTypeCode(null);
                entry.setExpenseTypeName(defaultText(templatePolicy.getTemplateName(), "\u6a21\u677f"));
            }
            pushEntryMapper.insert(entry);
        }
    }

    /**
     * 鍒涘缓Batch涓婁笅鏂囥€?
     */
    private CompanyBatchContext createBatchContext(String companyId, String currentUsername) {
        ExpVoucherPushBatch batch = new ExpVoucherPushBatch();
        batch.setCompanyId(companyId);
        batch.setBatchNo("VG" + LocalDateTime.now().format(BATCH_FORMATTER) + companyId.substring(Math.max(companyId.length() - 4, 0)));
        batch.setDocumentCount(0);
        batch.setSuccessCount(0);
        batch.setFailureCount(0);
        batch.setStatus("DRAFT");
        batch.setCreatedBy(currentUsername);
        pushBatchMapper.insert(batch);
        return new CompanyBatchContext(batch);
    }
}

