package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentRelation;
import com.finex.auth.entity.ProcessDocumentWriteOff;
import com.finex.auth.entity.ProcessFormDesign;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

class ExpenseRelationBindingMutationSupport extends AbstractExpenseRelationWriteOffSupport {

    private final ExpenseWriteOffAmountSupport amountSupport;

    ExpenseRelationBindingMutationSupport(
            ExpenseRelationWriteOffSupportContext context,
            ExpenseWriteOffAmountSupport amountSupport
    ) {
        super(context);
        this.amountSupport = amountSupport;
    }

    void syncDocumentBusinessRelations(
            String documentCode,
            ProcessFormDesign formDesign,
            Map<String, Object> formData
    ) {
        if (trimToNull(documentCode) == null || formDesign == null) {
            return;
        }
        ProcessDocumentInstance source = requireDocument(documentCode);
        Long sourceSubmitterUserId = source.getSubmitterUserId();
        voidActiveRelations(documentCode);
        voidPendingWriteOffs(documentCode);

        List<DocumentBusinessBinding> bindings = collectDocumentBusinessBindings(formDesign);
        if (bindings.isEmpty()) {
            return;
        }

        List<RelatedDocumentSelection> relatedSelections = new ArrayList<>();
        List<WriteOffSelection> writeOffSelections = new ArrayList<>();
        for (DocumentBusinessBinding binding : bindings) {
            if (Objects.equals(binding.componentCode(), RELATED_DOCUMENT_COMPONENT_CODE)) {
                relatedSelections.addAll(normalizeRelatedDocumentSelections(documentCode, binding, formData));
            } else if (Objects.equals(binding.componentCode(), WRITEOFF_DOCUMENT_COMPONENT_CODE)) {
                writeOffSelections.addAll(normalizeWriteOffSelections(documentCode, binding, formData));
            }
        }

        Set<String> targetDocumentCodes = new LinkedHashSet<>();
        relatedSelections.forEach(item -> targetDocumentCodes.add(item.documentCode()));
        writeOffSelections.forEach(item -> targetDocumentCodes.add(item.documentCode()));
        if (targetDocumentCodes.isEmpty()) {
            return;
        }

        Map<String, ProcessDocumentInstance> targetDocumentMap = processDocumentInstanceMapper.selectList(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .in(ProcessDocumentInstance::getDocumentCode, targetDocumentCodes)
        ).stream().collect(Collectors.toMap(
                ProcessDocumentInstance::getDocumentCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        Map<String, BigDecimal> prepayAmountMap = amountSupport.loadPrepayReportAmountMap(
                writeOffSelections.stream().map(WriteOffSelection::documentCode).distinct().toList()
        );
        Map<String, BigDecimal> effectiveAmountMap = amountSupport.loadEffectiveWriteOffAmountMap(
                writeOffSelections.stream().map(WriteOffSelection::documentCode).distinct().toList()
        );
        LocalDateTime now = LocalDateTime.now();

        for (RelatedDocumentSelection selection : relatedSelections) {
            ProcessDocumentInstance target = requireRelationSelectableTargetDocument(
                    targetDocumentMap,
                    selection.documentCode(),
                    sourceSubmitterUserId,
                    MESSAGE_RELATED_DOCUMENT_SCOPE_RESTRICTED
            );
            String normalizedTemplateType = normalizeTemplateType(target.getTemplateType());
            if (!selection.allowedTemplateTypes().contains(normalizedTemplateType)) {
                throw new IllegalStateException(MESSAGE_RELATED_TEMPLATE_TYPE_NOT_ALLOWED);
            }
            ProcessDocumentRelation relation = new ProcessDocumentRelation();
            relation.setSourceDocumentCode(documentCode);
            relation.setSourceFieldKey(selection.fieldKey());
            relation.setTargetDocumentCode(selection.documentCode());
            relation.setTargetTemplateType(normalizedTemplateType);
            relation.setSortOrder(selection.sortOrder());
            relation.setStatus(RELATION_STATUS_ACTIVE);
            relation.setCreatedAt(now);
            relation.setUpdatedAt(now);
            processDocumentRelationMapper.insert(relation);
        }

        for (WriteOffSelection selection : writeOffSelections) {
            ProcessDocumentInstance target = requireRelationSelectableTargetDocument(
                    targetDocumentMap,
                    selection.documentCode(),
                    sourceSubmitterUserId,
                    MESSAGE_WRITEOFF_DOCUMENT_SCOPE_RESTRICTED
            );
            String normalizedTemplateType = normalizeTemplateType(target.getTemplateType());
            if (!selection.allowedTemplateTypes().contains(normalizedTemplateType)) {
                throw new IllegalStateException(MESSAGE_WRITEOFF_TEMPLATE_TYPE_NOT_ALLOWED);
            }
            String writeOffSourceKind = amountSupport.resolveWriteOffSourceKind(target, prepayAmountMap);
            BigDecimal availableAmount = amountSupport.resolveCurrentAvailableWriteOffAmount(target, writeOffSourceKind, prepayAmountMap, effectiveAmountMap);
            if (selection.requestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("鏍搁攢閲戦蹇呴』澶т簬 0");
            }
            if (availableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("褰撳墠鏍搁攢鍗曟嵁宸叉棤鍙牳閿€浣欓");
            }
            if (selection.requestedAmount().compareTo(availableAmount) > 0) {
                throw new IllegalStateException("鏍搁攢閲戦涓嶈兘瓒呰繃褰撳墠鍙牳閿€浣欓");
            }
            ProcessDocumentWriteOff writeOff = new ProcessDocumentWriteOff();
            writeOff.setSourceDocumentCode(documentCode);
            writeOff.setSourceFieldKey(selection.fieldKey());
            writeOff.setTargetDocumentCode(selection.documentCode());
            writeOff.setTargetTemplateType(normalizedTemplateType);
            writeOff.setWriteoffSourceKind(writeOffSourceKind);
            writeOff.setRequestedAmount(selection.requestedAmount());
            writeOff.setEffectiveAmount(null);
            writeOff.setAvailableSnapshotAmount(availableAmount);
            writeOff.setRemainingSnapshotAmount(availableAmount.subtract(selection.requestedAmount()));
            writeOff.setSortOrder(selection.sortOrder());
            writeOff.setStatus(WRITEOFF_STATUS_PENDING);
            writeOff.setEffectiveAt(null);
            writeOff.setCreatedAt(now);
            writeOff.setUpdatedAt(now);
            processDocumentWriteOffMapper.insert(writeOff);
        }
    }

    void voidActiveRelations(String documentCode) {
        List<ProcessDocumentRelation> relations = processDocumentRelationMapper.selectList(
                Wrappers.<ProcessDocumentRelation>lambdaQuery()
                        .eq(ProcessDocumentRelation::getSourceDocumentCode, documentCode)
                        .eq(ProcessDocumentRelation::getStatus, RELATION_STATUS_ACTIVE)
        );
        if (relations.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (ProcessDocumentRelation relation : relations) {
            relation.setStatus(RELATION_STATUS_VOID);
            relation.setUpdatedAt(now);
            processDocumentRelationMapper.updateById(relation);
        }
    }

    void voidPendingWriteOffs(String documentCode) {
        List<ProcessDocumentWriteOff> writeOffs = processDocumentWriteOffMapper.selectList(
                Wrappers.<ProcessDocumentWriteOff>lambdaQuery()
                        .eq(ProcessDocumentWriteOff::getSourceDocumentCode, documentCode)
                        .eq(ProcessDocumentWriteOff::getStatus, WRITEOFF_STATUS_PENDING)
        );
        if (writeOffs.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (ProcessDocumentWriteOff writeOff : writeOffs) {
            writeOff.setStatus(WRITEOFF_STATUS_VOID);
            writeOff.setUpdatedAt(now);
            processDocumentWriteOffMapper.updateById(writeOff);
        }
    }
}
