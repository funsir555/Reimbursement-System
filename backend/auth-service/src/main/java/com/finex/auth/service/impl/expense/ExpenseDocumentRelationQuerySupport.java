package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.ExpenseDocumentPickerGroupVO;
import com.finex.auth.dto.ExpenseDocumentPickerItemVO;
import com.finex.auth.dto.ExpenseDocumentPickerVO;
import com.finex.auth.dto.ExpenseDocumentRelationBindingVO;
import com.finex.auth.dto.ExpenseDocumentWriteOffBindingVO;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentRelation;
import com.finex.auth.entity.ProcessDocumentWriteOff;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

class ExpenseDocumentRelationQuerySupport extends AbstractExpenseRelationWriteOffSupport {

    private final ExpenseWriteOffAmountSupport amountSupport;

    ExpenseDocumentRelationQuerySupport(
            ExpenseRelationWriteOffSupportContext context,
            ExpenseWriteOffAmountSupport amountSupport
    ) {
        super(context);
        this.amountSupport = amountSupport;
    }

    List<ExpenseDocumentRelationBindingVO> loadRelatedDocumentBindings(String documentCode) {
        String normalizedDocumentCode = trimToNull(documentCode);
        if (normalizedDocumentCode == null) {
            return Collections.emptyList();
        }
        List<ProcessDocumentRelation> outboundRelations = processDocumentRelationMapper.selectList(
                Wrappers.<ProcessDocumentRelation>lambdaQuery()
                        .eq(ProcessDocumentRelation::getSourceDocumentCode, normalizedDocumentCode)
                        .eq(ProcessDocumentRelation::getStatus, RELATION_STATUS_ACTIVE)
                        .orderByAsc(ProcessDocumentRelation::getSortOrder, ProcessDocumentRelation::getId)
        );
        List<ProcessDocumentRelation> inboundRelations = processDocumentRelationMapper.selectList(
                Wrappers.<ProcessDocumentRelation>lambdaQuery()
                        .eq(ProcessDocumentRelation::getTargetDocumentCode, normalizedDocumentCode)
                        .eq(ProcessDocumentRelation::getStatus, RELATION_STATUS_ACTIVE)
                        .orderByAsc(ProcessDocumentRelation::getSortOrder, ProcessDocumentRelation::getId)
        );
        if (outboundRelations.isEmpty() && inboundRelations.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> documentCodes = new LinkedHashSet<>();
        outboundRelations.stream()
                .map(ProcessDocumentRelation::getTargetDocumentCode)
                .filter(Objects::nonNull)
                .forEach(documentCodes::add);
        inboundRelations.stream()
                .map(ProcessDocumentRelation::getSourceDocumentCode)
                .filter(Objects::nonNull)
                .forEach(documentCodes::add);
        Map<String, ProcessDocumentInstance> documentMap = loadDocumentMap(documentCodes);

        List<ExpenseDocumentRelationBindingVO> bindings = new ArrayList<>();
        for (ProcessDocumentRelation relation : outboundRelations) {
            bindings.add(toRelationBinding(
                    relation,
                    BINDING_DIRECTION_OUTBOUND,
                    documentMap.get(relation.getTargetDocumentCode()),
                    relation.getTargetTemplateType()
            ));
        }
        for (ProcessDocumentRelation relation : inboundRelations) {
            ProcessDocumentInstance source = documentMap.get(relation.getSourceDocumentCode());
            bindings.add(toRelationBinding(
                    relation,
                    BINDING_DIRECTION_INBOUND,
                    source,
                    source == null ? null : source.getTemplateType()
            ));
        }
        return bindings;
    }

    List<ExpenseDocumentWriteOffBindingVO> loadWriteOffDocumentBindings(String documentCode) {
        String normalizedDocumentCode = trimToNull(documentCode);
        if (normalizedDocumentCode == null) {
            return Collections.emptyList();
        }
        List<ProcessDocumentWriteOff> outboundWriteOffs = processDocumentWriteOffMapper.selectList(
                Wrappers.<ProcessDocumentWriteOff>lambdaQuery()
                        .eq(ProcessDocumentWriteOff::getSourceDocumentCode, normalizedDocumentCode)
                        .in(ProcessDocumentWriteOff::getStatus, List.of(WRITEOFF_STATUS_PENDING, WRITEOFF_STATUS_EFFECTIVE, WRITEOFF_STATUS_VOID))
                        .orderByAsc(ProcessDocumentWriteOff::getSortOrder, ProcessDocumentWriteOff::getId)
        );
        List<ProcessDocumentWriteOff> inboundWriteOffs = processDocumentWriteOffMapper.selectList(
                Wrappers.<ProcessDocumentWriteOff>lambdaQuery()
                        .eq(ProcessDocumentWriteOff::getTargetDocumentCode, normalizedDocumentCode)
                        .in(ProcessDocumentWriteOff::getStatus, List.of(WRITEOFF_STATUS_PENDING, WRITEOFF_STATUS_EFFECTIVE, WRITEOFF_STATUS_VOID))
                        .orderByAsc(ProcessDocumentWriteOff::getSortOrder, ProcessDocumentWriteOff::getId)
        );
        if (outboundWriteOffs.isEmpty() && inboundWriteOffs.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> documentCodes = new LinkedHashSet<>();
        outboundWriteOffs.stream()
                .map(ProcessDocumentWriteOff::getTargetDocumentCode)
                .filter(Objects::nonNull)
                .forEach(documentCodes::add);
        inboundWriteOffs.stream()
                .map(ProcessDocumentWriteOff::getSourceDocumentCode)
                .filter(Objects::nonNull)
                .forEach(documentCodes::add);
        Map<String, ProcessDocumentInstance> documentMap = loadDocumentMap(documentCodes);

        List<ExpenseDocumentWriteOffBindingVO> bindings = new ArrayList<>();
        for (ProcessDocumentWriteOff writeOff : outboundWriteOffs) {
            bindings.add(toWriteOffBinding(
                    writeOff,
                    BINDING_DIRECTION_OUTBOUND,
                    documentMap.get(writeOff.getTargetDocumentCode()),
                    writeOff.getTargetTemplateType()
            ));
        }
        for (ProcessDocumentWriteOff writeOff : inboundWriteOffs) {
            ProcessDocumentInstance source = documentMap.get(writeOff.getSourceDocumentCode());
            bindings.add(toWriteOffBinding(
                    writeOff,
                    BINDING_DIRECTION_INBOUND,
                    source,
                    source == null ? null : source.getTemplateType()
            ));
        }
        return bindings;
    }

    ExpenseDocumentPickerVO getDocumentPicker(
            Long userId,
            String relationType,
            List<String> templateTypes,
            String keyword,
            Integer page,
            Integer pageSize,
            String excludeDocumentCode,
            boolean allowCrossView
    ) {
        String normalizedRelationType = normalizeRelationType(relationType);
        List<String> normalizedTemplateTypes = normalizePickerTemplateTypes(normalizedRelationType, templateTypes);
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        String excludedDocumentCode = trimToNull(excludeDocumentCode);
        String normalizedKeyword = trimToNull(keyword);

        List<ProcessDocumentInstance> visibleApprovedDocuments = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .eq(ProcessDocumentInstance::getSubmitterUserId, userId)
                        .in(ProcessDocumentInstance::getStatus, RELATION_PICKER_ALLOWED_STATUSES)
                        .in(ProcessDocumentInstance::getTemplateType, normalizedTemplateTypes)
                        .ne(excludedDocumentCode != null, ProcessDocumentInstance::getDocumentCode, excludedDocumentCode)
                        .orderByDesc(ProcessDocumentInstance::getFinishedAt, ProcessDocumentInstance::getUpdatedAt, ProcessDocumentInstance::getId)
        ).stream()
                .filter(item -> Objects.equals(item.getSubmitterUserId(), userId))
                .filter(item -> isRelationSelectableStatus(item.getStatus()))
                .filter(item -> normalizedTemplateTypes.contains(normalizeTemplateType(item.getTemplateType())))
                .filter(item -> excludedDocumentCode == null || !Objects.equals(item.getDocumentCode(), excludedDocumentCode))
                .filter(item -> matchesKeyword(
                        normalizedKeyword,
                        item.getDocumentCode(),
                        item.getDocumentTitle(),
                        item.getTemplateName(),
                        item.getDocumentReason()
                ))
                .toList();

        ExpenseDocumentPickerVO result = new ExpenseDocumentPickerVO();
        result.setRelationType(normalizedRelationType);
        if (visibleApprovedDocuments.isEmpty()) {
            return result;
        }

        if (Objects.equals(normalizedRelationType, RELATION_TYPE_RELATED)) {
            for (String templateType : normalizedTemplateTypes) {
                result.getGroups().add(buildRelatedGroup(templateType, visibleApprovedDocuments, safePage, safePageSize));
            }
            return result;
        }

        for (String templateType : normalizedTemplateTypes) {
            ExpenseDocumentPickerGroupVO group = buildWriteOffGroup(templateType, visibleApprovedDocuments, safePage, safePageSize);
            if (group.getTotal() > 0) {
                result.getGroups().add(group);
            }
        }
        return result;
    }

    ExpenseDocumentPickerVO getDashboardWriteOffSourceReportPicker(
            Long userId,
            String targetDocumentCode,
            String keyword,
            Integer page,
            Integer pageSize
    ) {
        ProcessDocumentInstance target = requireDocument(targetDocumentCode);
        requireSubmitter(target, userId);
        amountSupport.ensureDashboardWriteOffTargetSupported(target);

        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        String normalizedKeyword = trimToNull(keyword);

        List<ProcessDocumentInstance> sourceReports = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .eq(ProcessDocumentInstance::getSubmitterUserId, userId)
                        .in(ProcessDocumentInstance::getStatus, List.of(
                                DOCUMENT_STATUS_APPROVED,
                                DOCUMENT_STATUS_COMPLETED,
                                DOCUMENT_STATUS_PENDING_PAYMENT,
                                DOCUMENT_STATUS_PAYMENT_COMPLETED,
                                DOCUMENT_STATUS_PAYMENT_FINISHED
                        ))
                        .eq(ProcessDocumentInstance::getTemplateType, "report")
                        .ne(ProcessDocumentInstance::getDocumentCode, targetDocumentCode)
                        .orderByDesc(ProcessDocumentInstance::getFinishedAt, ProcessDocumentInstance::getUpdatedAt, ProcessDocumentInstance::getId)
        ).stream()
                .filter(item -> matchesKeyword(
                        normalizedKeyword,
                        item.getDocumentCode(),
                        item.getDocumentTitle(),
                        item.getTemplateName(),
                        item.getDocumentReason()
                ))
                .toList();

        ExpenseDocumentPickerVO result = new ExpenseDocumentPickerVO();
        result.setRelationType(RELATION_TYPE_WRITEOFF);
        if (sourceReports.isEmpty()) {
            return result;
        }

        Map<String, BigDecimal> sourceEffectiveAmountMap = amountSupport.loadEffectiveSourceWriteOffAmountMap(
                sourceReports.stream().map(ProcessDocumentInstance::getDocumentCode).toList()
        );
        Set<String> boundSourceCodes = processDocumentWriteOffMapper.selectList(
                Wrappers.<ProcessDocumentWriteOff>lambdaQuery()
                        .eq(ProcessDocumentWriteOff::getTargetDocumentCode, targetDocumentCode)
                        .eq(ProcessDocumentWriteOff::getStatus, WRITEOFF_STATUS_EFFECTIVE)
        ).stream()
                .map(ProcessDocumentWriteOff::getSourceDocumentCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<ExpenseDocumentPickerItemVO> items = new ArrayList<>();
        for (ProcessDocumentInstance report : sourceReports) {
            if (boundSourceCodes.contains(report.getDocumentCode())) {
                continue;
            }
            BigDecimal availableAmount = amountSupport.resolveReportSourceAvailableAmount(report, sourceEffectiveAmountMap);
            if (availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            ExpenseDocumentPickerItemVO item = toPickerItem(report);
            item.setAvailableWriteOffAmount(availableAmount);
            items.add(item);
        }
        if (items.isEmpty()) {
            return result;
        }

        result.getGroups().add(paginatePickerGroup("report", items, safePage, safePageSize));
        return result;
    }

    private ExpenseDocumentPickerGroupVO buildRelatedGroup(
            String templateType,
            List<ProcessDocumentInstance> documents,
            int page,
            int pageSize
    ) {
        List<ExpenseDocumentPickerItemVO> items = documents.stream()
                .filter(item -> Objects.equals(trimToNull(item.getTemplateType()), templateType))
                .map(this::toPickerItem)
                .toList();
        return paginatePickerGroup(templateType, items, page, pageSize);
    }

    private ExpenseDocumentPickerGroupVO buildWriteOffGroup(
            String templateType,
            List<ProcessDocumentInstance> documents,
            int page,
            int pageSize
    ) {
        List<ProcessDocumentInstance> typedDocuments = documents.stream()
                .filter(item -> Objects.equals(trimToNull(item.getTemplateType()), templateType))
                .toList();
        if (typedDocuments.isEmpty()) {
            return paginatePickerGroup(templateType, Collections.emptyList(), page, pageSize);
        }

        Map<String, BigDecimal> effectiveAmountMap = amountSupport.loadEffectiveWriteOffAmountMap(
                typedDocuments.stream().map(ProcessDocumentInstance::getDocumentCode).toList()
        );
        List<ExpenseDocumentPickerItemVO> items = new ArrayList<>();
        if (Objects.equals(templateType, "loan")) {
            for (ProcessDocumentInstance instance : typedDocuments) {
                BigDecimal totalAmount = defaultDecimal(instance.getTotalAmount());
                BigDecimal effectiveAmount = defaultDecimal(effectiveAmountMap.get(instance.getDocumentCode()));
                BigDecimal availableAmount = totalAmount.subtract(effectiveAmount);
                if (availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                ExpenseDocumentPickerItemVO item = toPickerItem(instance);
                item.setAvailableWriteOffAmount(availableAmount);
                item.setWriteOffSourceKind(WRITEOFF_SOURCE_LOAN);
                items.add(item);
            }
            return paginatePickerGroup(templateType, items, page, pageSize);
        }

        Map<String, BigDecimal> prepayAmountMap = amountSupport.loadPrepayReportAmountMap(
                typedDocuments.stream().map(ProcessDocumentInstance::getDocumentCode).toList()
        );
        for (ProcessDocumentInstance instance : typedDocuments) {
            BigDecimal prepayAmount = defaultDecimal(prepayAmountMap.get(instance.getDocumentCode()));
            if (prepayAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal effectiveAmount = defaultDecimal(effectiveAmountMap.get(instance.getDocumentCode()));
            BigDecimal availableAmount = prepayAmount.subtract(effectiveAmount);
            if (availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            ExpenseDocumentPickerItemVO item = toPickerItem(instance);
            item.setAvailableWriteOffAmount(availableAmount);
            item.setWriteOffSourceKind(WRITEOFF_SOURCE_PREPAY_REPORT);
            items.add(item);
        }
        return paginatePickerGroup(templateType, items, page, pageSize);
    }
}
