package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentWriteOff;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

class ExpenseDashboardWriteOffSupport extends AbstractExpenseRelationWriteOffSupport {

    private final ExpenseWriteOffAmountSupport amountSupport;

    ExpenseDashboardWriteOffSupport(
            ExpenseRelationWriteOffSupportContext context,
            ExpenseWriteOffAmountSupport amountSupport
    ) {
        super(context);
        this.amountSupport = amountSupport;
    }

    boolean bindDashboardWriteOff(Long userId, String targetDocumentCode, String sourceReportDocumentCode) {
        ProcessDocumentInstance target = requireDocument(targetDocumentCode);
        ProcessDocumentInstance sourceReport = requireDocument(sourceReportDocumentCode);
        requireSubmitter(target, userId);
        requireSubmitter(sourceReport, userId);
        amountSupport.ensureDashboardWriteOffTargetSupported(target);
        amountSupport.ensureApprovedReportSource(sourceReport);
        if (Objects.equals(target.getDocumentCode(), sourceReport.getDocumentCode())) {
            throw new IllegalStateException("鏍搁攢鏉ユ簮鎶ラ攢鍗曚笉鑳戒笌鐩爣鍗曟嵁鐩稿悓");
        }

        long duplicateCount = processDocumentWriteOffMapper.selectCount(
                Wrappers.<ProcessDocumentWriteOff>lambdaQuery()
                        .eq(ProcessDocumentWriteOff::getSourceDocumentCode, sourceReportDocumentCode)
                        .eq(ProcessDocumentWriteOff::getTargetDocumentCode, targetDocumentCode)
                        .eq(ProcessDocumentWriteOff::getStatus, WRITEOFF_STATUS_EFFECTIVE)
        );
        if (duplicateCount > 0) {
            throw new IllegalStateException("璇ユ姤閿€鍗曚笌鐩爣鍗曟嵁宸插瓨鍦ㄦ湁鏁堟牳閿€鍏崇郴");
        }

        Map<String, BigDecimal> prepayAmountMap = amountSupport.loadPrepayReportAmountMap(List.of(targetDocumentCode));
        String targetKind = amountSupport.resolveWriteOffSourceKind(target, prepayAmountMap);
        Map<String, BigDecimal> targetEffectiveAmountMap = amountSupport.loadEffectiveWriteOffAmountMap(List.of(targetDocumentCode));
        BigDecimal targetRemaining = amountSupport.resolveCurrentAvailableWriteOffAmount(target, targetKind, prepayAmountMap, targetEffectiveAmountMap);
        if (targetRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("鐩爣鍗曟嵁宸叉棤鍙牳閿€浣欓");
        }

        Map<String, BigDecimal> sourceEffectiveAmountMap = amountSupport.loadEffectiveSourceWriteOffAmountMap(List.of(sourceReportDocumentCode));
        BigDecimal sourceRemaining = amountSupport.resolveReportSourceAvailableAmount(sourceReport, sourceEffectiveAmountMap);
        if (sourceRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("鏉ユ簮鎶ラ攢鍗曞凡鏃犲彲鐢ㄦ牳閿€浣欓");
        }

        BigDecimal effectiveAmount = targetRemaining.min(sourceRemaining);
        if (effectiveAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("鏈鏍搁攢閲戦蹇呴』澶т簬 0");
        }

        LocalDateTime now = LocalDateTime.now();
        ProcessDocumentWriteOff writeOff = new ProcessDocumentWriteOff();
        writeOff.setSourceDocumentCode(sourceReportDocumentCode);
        writeOff.setSourceFieldKey(DASHBOARD_WRITEOFF_SOURCE_FIELD_KEY);
        writeOff.setTargetDocumentCode(targetDocumentCode);
        writeOff.setTargetTemplateType(target.getTemplateType());
        writeOff.setWriteoffSourceKind(targetKind);
        writeOff.setRequestedAmount(effectiveAmount);
        writeOff.setEffectiveAmount(effectiveAmount);
        writeOff.setAvailableSnapshotAmount(targetRemaining);
        writeOff.setRemainingSnapshotAmount(targetRemaining.subtract(effectiveAmount));
        writeOff.setSortOrder(1);
        writeOff.setStatus(WRITEOFF_STATUS_EFFECTIVE);
        writeOff.setEffectiveAt(now);
        writeOff.setCreatedAt(now);
        writeOff.setUpdatedAt(now);
        processDocumentWriteOffMapper.insert(writeOff);
        return true;
    }

    void finalizeEffectiveWriteOffs(String documentCode) {
        List<ProcessDocumentWriteOff> pendingWriteOffs = processDocumentWriteOffMapper.selectList(
                Wrappers.<ProcessDocumentWriteOff>lambdaQuery()
                        .eq(ProcessDocumentWriteOff::getSourceDocumentCode, documentCode)
                        .eq(ProcessDocumentWriteOff::getStatus, WRITEOFF_STATUS_PENDING)
                        .orderByAsc(ProcessDocumentWriteOff::getSortOrder, ProcessDocumentWriteOff::getId)
        );
        if (pendingWriteOffs.isEmpty()) {
            return;
        }

        Map<String, ProcessDocumentInstance> targetDocumentMap = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .in(ProcessDocumentInstance::getDocumentCode, pendingWriteOffs.stream().map(ProcessDocumentWriteOff::getTargetDocumentCode).toList())
        ).stream().collect(Collectors.toMap(
                ProcessDocumentInstance::getDocumentCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        Map<String, BigDecimal> prepayAmountMap = amountSupport.loadPrepayReportAmountMap(
                pendingWriteOffs.stream().map(ProcessDocumentWriteOff::getTargetDocumentCode).distinct().toList()
        );
        Map<String, BigDecimal> effectiveAmountMap = amountSupport.loadEffectiveWriteOffAmountMap(
                pendingWriteOffs.stream().map(ProcessDocumentWriteOff::getTargetDocumentCode).distinct().toList()
        );
        LocalDateTime now = LocalDateTime.now();

        for (ProcessDocumentWriteOff writeOff : pendingWriteOffs) {
            ProcessDocumentInstance target = requireApprovedTargetDocument(targetDocumentMap, writeOff.getTargetDocumentCode(), "鏍搁攢鍗曟嵁");
            String sourceKind = amountSupport.resolveWriteOffSourceKind(target, prepayAmountMap);
            BigDecimal availableAmount = amountSupport.resolveCurrentAvailableWriteOffAmount(target, sourceKind, prepayAmountMap, effectiveAmountMap);
            BigDecimal requestedAmount = defaultDecimal(writeOff.getRequestedAmount());
            if (requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("鏍搁攢閲戦蹇呴』澶т簬 0");
            }
            if (requestedAmount.compareTo(availableAmount) > 0) {
                throw new IllegalStateException("鏍搁攢鍗曟嵁 " + writeOff.getTargetDocumentCode() + " 鐨勫彲鏍搁攢浣欓涓嶈冻锛岃鍒锋柊鍚庨噸璇?");
            }
            writeOff.setWriteoffSourceKind(sourceKind);
            writeOff.setEffectiveAmount(requestedAmount);
            writeOff.setAvailableSnapshotAmount(availableAmount);
            writeOff.setRemainingSnapshotAmount(availableAmount.subtract(requestedAmount));
            writeOff.setStatus(WRITEOFF_STATUS_EFFECTIVE);
            writeOff.setEffectiveAt(now);
            writeOff.setUpdatedAt(now);
            processDocumentWriteOffMapper.updateById(writeOff);
            effectiveAmountMap.put(
                    writeOff.getTargetDocumentCode(),
                    defaultDecimal(effectiveAmountMap.get(writeOff.getTargetDocumentCode())).add(requestedAmount)
            );
        }
    }
}
