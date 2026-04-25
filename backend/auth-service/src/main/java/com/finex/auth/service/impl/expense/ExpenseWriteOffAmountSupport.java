package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentWriteOff;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

class ExpenseWriteOffAmountSupport extends AbstractExpenseRelationWriteOffSupport {

    ExpenseWriteOffAmountSupport(ExpenseRelationWriteOffSupportContext context) {
        super(context);
    }

    Map<String, BigDecimal> buildOutstandingAmountMap(List<ProcessDocumentInstance> instances, String kind) {
        if (instances == null || instances.isEmpty()) {
            return Collections.emptyMap();
        }
        String normalizedKind = normalizeDashboardOutstandingKind(kind);
        List<String> documentCodes = instances.stream().map(ProcessDocumentInstance::getDocumentCode).toList();
        Map<String, BigDecimal> effectiveAmountMap = loadEffectiveWriteOffAmountMap(documentCodes);
        Map<String, BigDecimal> prepayAmountMap = Objects.equals(normalizedKind, WRITEOFF_SOURCE_PREPAY_REPORT)
                ? loadPrepayReportAmountMap(documentCodes)
                : Collections.emptyMap();

        Map<String, BigDecimal> outstandingAmountMap = new LinkedHashMap<>();
        for (ProcessDocumentInstance instance : instances) {
            BigDecimal outstandingAmount = resolveOutstandingAmount(instance, normalizedKind, prepayAmountMap, effectiveAmountMap);
            if (outstandingAmount.compareTo(BigDecimal.ZERO) > 0) {
                outstandingAmountMap.put(instance.getDocumentCode(), outstandingAmount);
            }
        }
        return outstandingAmountMap;
    }

    Map<String, BigDecimal> loadPrepayReportAmountMap(List<String> documentCodes) {
        if (documentCodes == null || documentCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return processDocumentExpenseDetailMapper.selectList(
                Wrappers.<ProcessDocumentExpenseDetail>lambdaQuery()
                        .in(ProcessDocumentExpenseDetail::getDocumentCode, documentCodes)
                        .eq(ProcessDocumentExpenseDetail::getBusinessSceneMode, ENTERPRISE_MODE_PREPAY_UNBILLED)
        ).stream().collect(Collectors.groupingBy(
                ProcessDocumentExpenseDetail::getDocumentCode,
                LinkedHashMap::new,
                Collectors.reducing(
                        BigDecimal.ZERO,
                        detail -> defaultDecimal(ExpenseAmountResolver.resolvePrepayWriteOffAmount(
                                null,
                                detail.getActualPaymentAmount()
                        )),
                        BigDecimal::add
                )
        ));
    }

    Map<String, BigDecimal> loadEffectiveWriteOffAmountMap(List<String> targetDocumentCodes) {
        if (targetDocumentCodes == null || targetDocumentCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return processDocumentWriteOffMapper.selectList(
                Wrappers.<ProcessDocumentWriteOff>lambdaQuery()
                        .in(ProcessDocumentWriteOff::getTargetDocumentCode, targetDocumentCodes)
                        .eq(ProcessDocumentWriteOff::getStatus, WRITEOFF_STATUS_EFFECTIVE)
        ).stream().collect(Collectors.groupingBy(
                ProcessDocumentWriteOff::getTargetDocumentCode,
                LinkedHashMap::new,
                Collectors.reducing(
                        BigDecimal.ZERO,
                        item -> defaultDecimal(item.getEffectiveAmount()),
                        BigDecimal::add
                )
        ));
    }

    String resolveWriteOffSourceKind(
            ProcessDocumentInstance target,
            Map<String, BigDecimal> prepayAmountMap
    ) {
        String templateType = normalizeTemplateType(target.getTemplateType());
        if (Objects.equals(templateType, "loan")) {
            return WRITEOFF_SOURCE_LOAN;
        }
        if (Objects.equals(templateType, "report")
                && defaultDecimal(prepayAmountMap.get(target.getDocumentCode())).compareTo(BigDecimal.ZERO) > 0) {
            return WRITEOFF_SOURCE_PREPAY_REPORT;
        }
        throw new IllegalStateException("褰撳墠鍗曟嵁涓嶆敮鎸佷綔涓烘牳閿€鐩爣");
    }

    BigDecimal resolveCurrentAvailableWriteOffAmount(
            ProcessDocumentInstance target,
            String writeOffSourceKind,
            Map<String, BigDecimal> prepayAmountMap,
            Map<String, BigDecimal> effectiveAmountMap
    ) {
        BigDecimal baseAmount = Objects.equals(writeOffSourceKind, WRITEOFF_SOURCE_LOAN)
                ? defaultDecimal(target.getTotalAmount())
                : defaultDecimal(prepayAmountMap.get(target.getDocumentCode()));
        BigDecimal effectiveAmount = defaultDecimal(effectiveAmountMap.get(target.getDocumentCode()));
        BigDecimal availableAmount = baseAmount.subtract(effectiveAmount);
        return availableAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : availableAmount;
    }

    Map<String, BigDecimal> loadEffectiveSourceWriteOffAmountMap(List<String> sourceDocumentCodes) {
        if (sourceDocumentCodes == null || sourceDocumentCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return processDocumentWriteOffMapper.selectList(
                Wrappers.<ProcessDocumentWriteOff>lambdaQuery()
                        .in(ProcessDocumentWriteOff::getSourceDocumentCode, sourceDocumentCodes)
                        .eq(ProcessDocumentWriteOff::getStatus, WRITEOFF_STATUS_EFFECTIVE)
        ).stream().collect(Collectors.groupingBy(
                ProcessDocumentWriteOff::getSourceDocumentCode,
                LinkedHashMap::new,
                Collectors.reducing(
                        BigDecimal.ZERO,
                        item -> defaultDecimal(item.getEffectiveAmount()),
                        BigDecimal::add
                )
        ));
    }

    BigDecimal resolveReportSourceAvailableAmount(
            ProcessDocumentInstance sourceReport,
            Map<String, BigDecimal> sourceEffectiveAmountMap
    ) {
        BigDecimal availableAmount = defaultDecimal(sourceReport.getTotalAmount())
                .subtract(defaultDecimal(sourceEffectiveAmountMap.get(sourceReport.getDocumentCode())));
        return availableAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : availableAmount;
    }

    void ensureDashboardWriteOffTargetSupported(ProcessDocumentInstance target) {
        if (!isEffectiveApprovedStatus(target.getStatus())) {
            throw new IllegalStateException("浠呭凡閫氳繃鍗曟嵁鏀寔鏍搁攢");
        }
        Map<String, BigDecimal> prepayAmountMap = loadPrepayReportAmountMap(List.of(target.getDocumentCode()));
        resolveWriteOffSourceKind(target, prepayAmountMap);
    }

    void ensureApprovedReportSource(ProcessDocumentInstance sourceReport) {
        if (!isEffectiveApprovedStatus(sourceReport.getStatus())) {
            throw new IllegalStateException("浠呭凡閫氳繃鎶ラ攢鍗曞彲浣滀负鏍搁攢鏉ユ簮");
        }
        if (!Objects.equals(normalizeTemplateType(sourceReport.getTemplateType()), "report")) {
            throw new IllegalStateException("浠呮姤閿€鍗曞彲浣滀负鏍搁攢鏉ユ簮");
        }
    }

    private BigDecimal resolveOutstandingAmount(
            ProcessDocumentInstance instance,
            String kind,
            Map<String, BigDecimal> prepayAmountMap,
            Map<String, BigDecimal> effectiveAmountMap
    ) {
        BigDecimal baseAmount = Objects.equals(kind, WRITEOFF_SOURCE_LOAN)
                ? defaultDecimal(instance.getTotalAmount())
                : defaultDecimal(prepayAmountMap.get(instance.getDocumentCode()));
        BigDecimal effectiveAmount = defaultDecimal(effectiveAmountMap.get(instance.getDocumentCode()));
        BigDecimal outstandingAmount = baseAmount.subtract(effectiveAmount);
        return outstandingAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : outstandingAmount;
    }
}
