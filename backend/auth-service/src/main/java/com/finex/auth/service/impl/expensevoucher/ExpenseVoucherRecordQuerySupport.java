package com.finex.auth.service.impl.expensevoucher;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ExpenseVoucherGeneratedRecordDetailVO;
import com.finex.auth.dto.ExpenseVoucherGeneratedRecordVO;
import com.finex.auth.dto.ExpenseVoucherPageVO;
import com.finex.auth.entity.ExpVoucherPushDocument;
import com.finex.auth.entity.ExpVoucherPushEntry;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExpenseVoucherRecordQuerySupport extends AbstractExpenseVoucherGenerationSupport {

    public ExpenseVoucherRecordQuerySupport(Dependencies dependencies) {
        super(dependencies);
    }

    public ExpenseVoucherPageVO<ExpenseVoucherGeneratedRecordVO> getGeneratedVouchers(String companyId, String templateCode, String documentCode, String voucherNo, String pushStatus, String dateFrom, String dateTo, Integer page, Integer pageSize) {
        Map<String, String> companyMap = companyNameMap();
        List<ExpenseVoucherGeneratedRecordVO> rows = listPushDocuments().stream()
                .filter(item -> matchesCompany(item.getCompanyId(), companyId))
                .filter(item -> !hasText(templateCode) || Objects.equals(trim(templateCode), trim(item.getTemplateCode())))
                .filter(item -> !hasText(documentCode) || containsIgnoreCase(item.getDocumentCode(), trim(documentCode)))
                .filter(item -> !hasText(voucherNo) || containsIgnoreCase(item.getVoucherNo(), trim(voucherNo)))
                .filter(item -> !hasText(pushStatus) || Objects.equals(trim(pushStatus), trim(item.getPushStatus())))
                .filter(item -> matchesDateRange(item.getPushedAt() == null ? null : item.getPushedAt().toLocalDate(), dateFrom, dateTo))
                .sorted(Comparator.comparing(ExpVoucherPushDocument::getPushedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ExpVoucherPushDocument::getId, Comparator.reverseOrder()))
                .map(item -> toGeneratedRecordVO(item, companyMap))
                .toList();
        return buildPage(rows, page, pageSize);
    }

    public ExpenseVoucherGeneratedRecordDetailVO getGeneratedVoucherDetail(Long id) {
        ExpVoucherPushDocument pushDocument = pushDocumentMapper.selectById(id);
        if (pushDocument == null) {
            throw new IllegalArgumentException("鎺ㄩ€佽褰曚笉瀛樺湪");
        }
        ExpenseVoucherGeneratedRecordDetailVO detail = new ExpenseVoucherGeneratedRecordDetailVO();
        detail.setRecord(toGeneratedRecordVO(pushDocument, companyNameMap()));
        detail.setEntries(pushEntryMapper.selectList(
                        Wrappers.<ExpVoucherPushEntry>lambdaQuery()
                                .eq(ExpVoucherPushEntry::getPushDocumentId, pushDocument.getId())
                                .orderByAsc(ExpVoucherPushEntry::getEntryNo, ExpVoucherPushEntry::getId)
                ).stream().map(this::toEntrySnapshotVO).toList());
        if (hasText(pushDocument.getVoucherNo())) {
            try {
                detail.setVoucherDetail(financeVoucherService.getDetail(pushDocument.getCompanyId(), pushDocument.getVoucherNo()));
            } catch (Exception ignored) {
                detail.setVoucherDetail(null);
            }
        }
        return detail;
    }
}
