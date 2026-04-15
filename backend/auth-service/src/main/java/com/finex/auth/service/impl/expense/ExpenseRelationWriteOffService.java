// 业务域：报销单录入、流转与查询
// 文件角色：业务支撑类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseDocumentPickerGroupVO;
import com.finex.auth.dto.ExpenseDocumentPickerItemVO;
import com.finex.auth.dto.ExpenseDocumentPickerVO;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentRelation;
import com.finex.auth.entity.ProcessDocumentWriteOff;
import com.finex.auth.entity.ProcessFormDesign;
import com.finex.auth.mapper.ProcessDocumentExpenseDetailMapper;
import com.finex.auth.mapper.ProcessDocumentInstanceMapper;
import com.finex.auth.mapper.ProcessDocumentRelationMapper;
import com.finex.auth.mapper.ProcessDocumentWriteOffMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ExpenseRelationWriteOffService：业务支撑类。
 * 封装 报销单关联写入Off这块可复用的业务能力。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
@Service
@RequiredArgsConstructor
public class ExpenseRelationWriteOffService {

    private static final String RELATED_DOCUMENT_COMPONENT_CODE = "related-document";
    private static final String WRITEOFF_DOCUMENT_COMPONENT_CODE = "writeoff-document";
    private static final String RELATION_TYPE_RELATED = "RELATED";
    private static final String RELATION_TYPE_WRITEOFF = "WRITEOFF";
    private static final String RELATION_STATUS_ACTIVE = "ACTIVE";
    private static final String RELATION_STATUS_VOID = "VOID";
    private static final String WRITEOFF_STATUS_PENDING = "PENDING_EFFECTIVE";
    private static final String WRITEOFF_STATUS_EFFECTIVE = "EFFECTIVE";
    private static final String WRITEOFF_STATUS_VOID = "VOID";
    private static final String WRITEOFF_SOURCE_LOAN = "LOAN";
    private static final String WRITEOFF_SOURCE_PREPAY_REPORT = "PREPAY_REPORT";
    private static final String DASHBOARD_WRITEOFF_SOURCE_FIELD_KEY = "dashboard-writeoff";
    private static final String ENTERPRISE_MODE_PREPAY_UNBILLED = "PREPAY_UNBILLED";
    private static final String DOCUMENT_STATUS_APPROVED = "APPROVED";
    private static final String DOCUMENT_STATUS_COMPLETED = "COMPLETED";
    private static final String DOCUMENT_STATUS_REJECTED = "REJECTED";
    private static final String DOCUMENT_STATUS_EXCEPTION = "EXCEPTION";
    private static final String DOCUMENT_STATUS_PENDING_PAYMENT = "PENDING_PAYMENT";
    private static final String DOCUMENT_STATUS_PAYING = "PAYING";
    private static final String DOCUMENT_STATUS_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";
    private static final String DOCUMENT_STATUS_PAYMENT_FINISHED = "PAYMENT_FINISHED";
    private static final String DOCUMENT_STATUS_PAYMENT_EXCEPTION = "PAYMENT_EXCEPTION";
    private static final String MESSAGE_RELATED_TEMPLATE_TYPE_NOT_ALLOWED = "\u5173\u8054\u5355\u636e\u7c7b\u578b\u4e0d\u5728\u5f53\u524d\u7ec4\u4ef6\u5141\u8bb8\u8303\u56f4\u5185";
    private static final String MESSAGE_WRITEOFF_TEMPLATE_TYPE_NOT_ALLOWED = "\u6838\u9500\u5355\u636e\u7c7b\u578b\u4e0d\u5728\u5f53\u524d\u7ec4\u4ef6\u5141\u8bb8\u8303\u56f4\u5185";

    private final ProcessDocumentInstanceMapper processDocumentInstanceMapper;
    private final ProcessDocumentExpenseDetailMapper processDocumentExpenseDetailMapper;
    private final ProcessDocumentRelationMapper processDocumentRelationMapper;
    private final ProcessDocumentWriteOffMapper processDocumentWriteOffMapper;
    private final ObjectMapper objectMapper;

    /**
     * 组装OutstandingAmount映射。
     */
    public Map<String, BigDecimal> buildOutstandingAmountMap(List<ProcessDocumentInstance> instances, String kind) {
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

/**
 * 获取单据Picker。
 */
public ExpenseDocumentPickerVO getDocumentPicker(
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
                        .in(ProcessDocumentInstance::getStatus, List.of(
                                DOCUMENT_STATUS_APPROVED,
                                DOCUMENT_STATUS_COMPLETED,
                                DOCUMENT_STATUS_PENDING_PAYMENT,
                                DOCUMENT_STATUS_PAYMENT_COMPLETED,
                                DOCUMENT_STATUS_PAYMENT_FINISHED
                        ))
                        .in(ProcessDocumentInstance::getTemplateType, normalizedTemplateTypes)
                        .ne(excludedDocumentCode != null, ProcessDocumentInstance::getDocumentCode, excludedDocumentCode)
                        .eq(!allowCrossView, ProcessDocumentInstance::getSubmitterUserId, userId)
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

/**
 * 获取首页看板写入OffSourceReportPicker。
 */
public ExpenseDocumentPickerVO getDashboardWriteOffSourceReportPicker(
            Long userId,
            String targetDocumentCode,
            String keyword,
            Integer page,
            Integer pageSize
    ) {
        ProcessDocumentInstance target = requireDocument(targetDocumentCode);
        requireSubmitter(target, userId);
        ensureDashboardWriteOffTargetSupported(target);

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

        Map<String, BigDecimal> sourceEffectiveAmountMap = loadEffectiveSourceWriteOffAmountMap(
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
            BigDecimal availableAmount = resolveReportSourceAvailableAmount(report, sourceEffectiveAmountMap);
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

/**
 * 处理报销单关联写入Off中的这一步。
 */
public boolean bindDashboardWriteOff(Long userId, String targetDocumentCode, String sourceReportDocumentCode) {
        ProcessDocumentInstance target = requireDocument(targetDocumentCode);
        ProcessDocumentInstance sourceReport = requireDocument(sourceReportDocumentCode);
        requireSubmitter(target, userId);
        requireSubmitter(sourceReport, userId);
        ensureDashboardWriteOffTargetSupported(target);
        ensureApprovedReportSource(sourceReport);
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

        Map<String, BigDecimal> prepayAmountMap = loadPrepayReportAmountMap(List.of(targetDocumentCode));
        String targetKind = resolveWriteOffSourceKind(target, prepayAmountMap);
        Map<String, BigDecimal> targetEffectiveAmountMap = loadEffectiveWriteOffAmountMap(List.of(targetDocumentCode));
        BigDecimal targetRemaining = resolveCurrentAvailableWriteOffAmount(target, targetKind, prepayAmountMap, targetEffectiveAmountMap);
        if (targetRemaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("鐩爣鍗曟嵁宸叉棤鍙牳閿€浣欓");
        }

        Map<String, BigDecimal> sourceEffectiveAmountMap = loadEffectiveSourceWriteOffAmountMap(List.of(sourceReportDocumentCode));
        BigDecimal sourceRemaining = resolveReportSourceAvailableAmount(sourceReport, sourceEffectiveAmountMap);
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

/**
 * 同步单据业务关联。
 */
public void syncDocumentBusinessRelations(
            String documentCode,
            ProcessFormDesign formDesign,
            Map<String, Object> formData
    ) {
        if (trimToNull(documentCode) == null || formDesign == null) {
            return;
        }
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
        Map<String, BigDecimal> prepayAmountMap = loadPrepayReportAmountMap(
                writeOffSelections.stream().map(WriteOffSelection::documentCode).distinct().toList()
        );
        Map<String, BigDecimal> effectiveAmountMap = loadEffectiveWriteOffAmountMap(
                writeOffSelections.stream().map(WriteOffSelection::documentCode).distinct().toList()
        );
        LocalDateTime now = LocalDateTime.now();

        for (RelatedDocumentSelection selection : relatedSelections) {
            ProcessDocumentInstance target = requireApprovedTargetDocument(targetDocumentMap, selection.documentCode(), "鍏宠仈鍗曟嵁");
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
            ProcessDocumentInstance target = requireApprovedTargetDocument(targetDocumentMap, selection.documentCode(), "鏍搁攢鍗曟嵁");
            String normalizedTemplateType = normalizeTemplateType(target.getTemplateType());
            if (!selection.allowedTemplateTypes().contains(normalizedTemplateType)) {
                throw new IllegalStateException(MESSAGE_WRITEOFF_TEMPLATE_TYPE_NOT_ALLOWED);
            }
            String writeOffSourceKind = resolveWriteOffSourceKind(target, prepayAmountMap);
            BigDecimal availableAmount = resolveCurrentAvailableWriteOffAmount(target, writeOffSourceKind, prepayAmountMap, effectiveAmountMap);
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

/**
 * 处理报销单关联写入Off中的这一步。
 */
public void finalizeEffectiveWriteOffs(String documentCode) {
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
        Map<String, BigDecimal> prepayAmountMap = loadPrepayReportAmountMap(
                pendingWriteOffs.stream().map(ProcessDocumentWriteOff::getTargetDocumentCode).distinct().toList()
        );
        Map<String, BigDecimal> effectiveAmountMap = loadEffectiveWriteOffAmountMap(
                pendingWriteOffs.stream().map(ProcessDocumentWriteOff::getTargetDocumentCode).distinct().toList()
        );
        LocalDateTime now = LocalDateTime.now();

        for (ProcessDocumentWriteOff writeOff : pendingWriteOffs) {
            ProcessDocumentInstance target = requireApprovedTargetDocument(targetDocumentMap, writeOff.getTargetDocumentCode(), "鏍搁攢鍗曟嵁");
            String sourceKind = resolveWriteOffSourceKind(target, prepayAmountMap);
            BigDecimal availableAmount = resolveCurrentAvailableWriteOffAmount(target, sourceKind, prepayAmountMap, effectiveAmountMap);
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

/**
 * 处理报销单关联写入Off中的这一步。
 */
public void voidActiveRelations(String documentCode) {
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

/**
 * 处理报销单关联写入Off中的这一步。
 */
public void voidPendingWriteOffs(String documentCode) {
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

/**
 * 组装RelatedGroup。
 */
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

/**
 * 组装写入OffGroup。
 */
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

        Map<String, BigDecimal> effectiveAmountMap = loadEffectiveWriteOffAmountMap(
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

        Map<String, BigDecimal> prepayAmountMap = loadPrepayReportAmountMap(
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

private ExpenseDocumentPickerGroupVO paginatePickerGroup(
            String templateType,
            List<ExpenseDocumentPickerItemVO> items,
            int page,
            int pageSize
    ) {
        ExpenseDocumentPickerGroupVO group = new ExpenseDocumentPickerGroupVO();
        group.setTemplateType(templateType);
        group.setTemplateTypeLabel(resolveTemplateTypeLabel(templateType, null));
        group.setPage(page);
        group.setPageSize(pageSize);
        group.setTotal(items.size());
        int fromIndex = Math.min(Math.max((page - 1) * pageSize, 0), items.size());
        int toIndex = Math.min(fromIndex + pageSize, items.size());
        group.setItems(new ArrayList<>(items.subList(fromIndex, toIndex)));
        return group;
    }

private ExpenseDocumentPickerItemVO toPickerItem(ProcessDocumentInstance instance) {
        ExpenseDocumentPickerItemVO item = new ExpenseDocumentPickerItemVO();
        item.setDocumentCode(instance.getDocumentCode());
        item.setDocumentTitle(instance.getDocumentTitle());
        item.setTemplateType(instance.getTemplateType());
        item.setTemplateTypeLabel(resolveTemplateTypeLabel(instance.getTemplateType(), null));
        item.setTemplateName(instance.getTemplateName());
        item.setStatus(instance.getStatus());
        item.setStatusLabel(resolveStatusLabel(instance.getStatus()));
        item.setTotalAmount(defaultDecimal(instance.getTotalAmount()));
        return item;
    }

/**
 * 加载PrepayReportAmount映射。
 */
public Map<String, BigDecimal> loadPrepayReportAmountMap(List<String> documentCodes) {
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
                        detail -> defaultDecimal(detail.getPendingWriteOffAmount()),
                        BigDecimal::add
                )
        ));
    }

/**
 * 加载Effective写入OffAmount映射。
 */
public Map<String, BigDecimal> loadEffectiveWriteOffAmountMap(List<String> targetDocumentCodes) {
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

/**
 * 加载EffectiveSource写入OffAmount映射。
 */
private Map<String, BigDecimal> loadEffectiveSourceWriteOffAmountMap(List<String> sourceDocumentCodes) {
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

private String normalizeDashboardOutstandingKind(String kind) {
        String normalizedKind = trimToNull(kind);
        if (Objects.equals(normalizedKind, WRITEOFF_SOURCE_LOAN) || Objects.equals(normalizedKind, WRITEOFF_SOURCE_PREPAY_REPORT)) {
            return normalizedKind;
        }
        throw new IllegalArgumentException("涓嶆敮鎸佺殑寰呭鐞嗗崟鎹被鍨?");
    }

/**
 * 解析OutstandingAmount。
 */
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

/**
 * 解析ReportSource可用Amount。
 */
private BigDecimal resolveReportSourceAvailableAmount(
            ProcessDocumentInstance sourceReport,
            Map<String, BigDecimal> sourceEffectiveAmountMap
    ) {
        BigDecimal availableAmount = defaultDecimal(sourceReport.getTotalAmount())
                .subtract(defaultDecimal(sourceEffectiveAmountMap.get(sourceReport.getDocumentCode())));
        return availableAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : availableAmount;
    }

private void ensureDashboardWriteOffTargetSupported(ProcessDocumentInstance target) {
        if (!isEffectiveApprovedStatus(target.getStatus())) {
            throw new IllegalStateException("浠呭凡閫氳繃鍗曟嵁鏀寔鏍搁攢");
        }
        Map<String, BigDecimal> prepayAmountMap = loadPrepayReportAmountMap(List.of(target.getDocumentCode()));
        resolveWriteOffSourceKind(target, prepayAmountMap);
    }

private void ensureApprovedReportSource(ProcessDocumentInstance sourceReport) {
        if (!isEffectiveApprovedStatus(sourceReport.getStatus())) {
            throw new IllegalStateException("浠呭凡閫氳繃鎶ラ攢鍗曞彲浣滀负鏍搁攢鏉ユ簮");
        }
        if (!Objects.equals(normalizeTemplateType(sourceReport.getTemplateType()), "report")) {
            throw new IllegalStateException("浠呮姤閿€鍗曞彲浣滀负鏍搁攢鏉ユ簮");
        }
    }

private String normalizeRelationType(String relationType) {
        return Objects.equals(trimToNull(relationType), RELATION_TYPE_WRITEOFF) ? RELATION_TYPE_WRITEOFF : RELATION_TYPE_RELATED;
    }

private List<String> normalizePickerTemplateTypes(String relationType, List<String> templateTypes) {
        if (Objects.equals(relationType, RELATION_TYPE_WRITEOFF)) {
            if (templateTypes == null || templateTypes.isEmpty()) {
                return List.of("report", "loan");
            }
            return templateTypes.stream()
                    .map(this::normalizeTemplateType)
                    .filter(item -> Objects.equals(item, "report") || Objects.equals(item, "loan"))
                    .distinct()
                    .toList();
        }
        if (templateTypes == null || templateTypes.isEmpty()) {
            return List.of("report", "application", "contract", "loan");
        }
        return templateTypes.stream()
                .map(this::normalizeTemplateType)
                .distinct()
                .toList();
    }

private String normalizeTemplateType(String templateType) {
        String value = trimToNull(templateType);
        if (Objects.equals(value, "application") || Objects.equals(value, "loan") || Objects.equals(value, "contract")) {
            return value;
        }
        return "report";
    }

private List<DocumentBusinessBinding> collectDocumentBusinessBindings(ProcessFormDesign formDesign) {
        Map<String, Object> schema = readSchema(formDesign.getSchemaJson());
        Object rawBlocks = schema.get("blocks");
        if (!(rawBlocks instanceof List<?> blocks) || blocks.isEmpty()) {
            return Collections.emptyList();
        }
        List<DocumentBusinessBinding> bindings = new ArrayList<>();
        for (Object rawBlock : blocks) {
            if (!(rawBlock instanceof Map<?, ?> blockMap)) {
                continue;
            }
            if (!Objects.equals(String.valueOf(blockMap.get("kind")), "BUSINESS_COMPONENT")) {
                continue;
            }
            Object rawFieldKey = blockMap.get("fieldKey");
            Object rawProps = blockMap.get("props");
            if (!(rawProps instanceof Map<?, ?> propsMap) || rawFieldKey == null) {
                continue;
            }
            String componentCode = asText(propsMap.get("componentCode"));
            String fieldKey = asText(rawFieldKey);
            if (fieldKey == null || componentCode == null) {
                continue;
            }
            if (!Objects.equals(componentCode, RELATED_DOCUMENT_COMPONENT_CODE)
                    && !Objects.equals(componentCode, WRITEOFF_DOCUMENT_COMPONENT_CODE)) {
                continue;
            }
            bindings.add(new DocumentBusinessBinding(fieldKey, componentCode, normalizeAllowedTemplateTypes(componentCode, propsMap.get("allowedTemplateTypes"))));
        }
        return bindings;
    }

private List<String> normalizeAllowedTemplateTypes(String componentCode, Object rawValue) {
        boolean writeOffComponent = Objects.equals(componentCode, WRITEOFF_DOCUMENT_COMPONENT_CODE);
        if (!(rawValue instanceof List<?> values) || values.isEmpty()) {
            return writeOffComponent ? List.of("report", "loan") : List.of("report", "application", "contract", "loan");
        }
        List<String> normalized = values.stream()
                .map(item -> normalizeTemplateType(item == null ? null : String.valueOf(item)))
                .filter(item -> !writeOffComponent || Objects.equals(item, "report") || Objects.equals(item, "loan"))
                .distinct()
                .toList();
        if (!normalized.isEmpty()) {
            return normalized;
        }
        return writeOffComponent ? List.of("report", "loan") : List.of("report", "application", "contract", "loan");
    }

private List<RelatedDocumentSelection> normalizeRelatedDocumentSelections(
            String documentCode,
            DocumentBusinessBinding binding,
            Map<String, Object> formData
    ) {
        Object rawValue = formData == null ? null : formData.get(binding.fieldKey());
        List<Map<String, Object>> records = normalizeDocumentRecords(rawValue);
        List<RelatedDocumentSelection> selections = new ArrayList<>();
        Set<String> seenCodes = new LinkedHashSet<>();
        int sortOrder = 1;
        for (Map<String, Object> record : records) {
            String targetDocumentCode = trimToNull(asText(record.get("documentCode")));
            if (targetDocumentCode == null || !seenCodes.add(targetDocumentCode)) {
                continue;
            }
            if (Objects.equals(targetDocumentCode, documentCode)) {
                throw new IllegalStateException("褰撳墠鍗曟嵁涓嶈兘鍏宠仈鑷繁");
            }
            selections.add(new RelatedDocumentSelection(binding.fieldKey(), targetDocumentCode, binding.allowedTemplateTypes(), sortOrder++));
        }
        return selections;
    }

private List<WriteOffSelection> normalizeWriteOffSelections(
            String documentCode,
            DocumentBusinessBinding binding,
            Map<String, Object> formData
    ) {
        Object rawValue = formData == null ? null : formData.get(binding.fieldKey());
        List<Map<String, Object>> records = normalizeDocumentRecords(rawValue);
        List<WriteOffSelection> selections = new ArrayList<>();
        Set<String> seenCodes = new LinkedHashSet<>();
        int sortOrder = 1;
        for (Map<String, Object> record : records) {
            String targetDocumentCode = trimToNull(asText(record.get("documentCode")));
            if (targetDocumentCode == null || !seenCodes.add(targetDocumentCode)) {
                continue;
            }
            if (Objects.equals(targetDocumentCode, documentCode)) {
                throw new IllegalStateException("褰撳墠鍗曟嵁涓嶈兘鏍搁攢鑷繁");
            }
            BigDecimal requestedAmount = toBigDecimal(record.get("writeOffAmount"));
            if (requestedAmount == null) {
                throw new IllegalStateException("鏍搁攢鍗曟嵁缂哄皯鏍搁攢閲戦");
            }
            selections.add(new WriteOffSelection(binding.fieldKey(), targetDocumentCode, binding.allowedTemplateTypes(), requestedAmount, sortOrder++));
        }
        return selections;
    }

private List<Map<String, Object>> normalizeDocumentRecords(Object rawValue) {
        if (rawValue == null) {
            return Collections.emptyList();
        }
        if (rawValue instanceof List<?> values) {
            List<Map<String, Object>> records = new ArrayList<>();
            for (Object value : values) {
                if (value instanceof Map<?, ?> map) {
                    records.add(toObjectMap(map));
                }
            }
            return records;
        }
        if (rawValue instanceof Map<?, ?> map) {
            return List.of(toObjectMap(map));
        }
        return Collections.emptyList();
    }

private Map<String, Object> toObjectMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), value);
            }
        });
        return result;
    }

private ProcessDocumentInstance requireApprovedTargetDocument(
            Map<String, ProcessDocumentInstance> targetDocumentMap,
            String documentCode,
            String actionName
    ) {
        ProcessDocumentInstance target = targetDocumentMap.get(documentCode);
        if (target == null || !isEffectiveApprovedStatus(target.getStatus())) {
            throw new IllegalStateException(actionName + "鐩爣涓嶅瓨鍦ㄦ垨鏈€氳繃瀹℃壒");
        }
        return target;
    }

/**
 * 解析写入OffSourceKind。
 */
public String resolveWriteOffSourceKind(
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

/**
 * 解析当前可用写入OffAmount。
 */
public BigDecimal resolveCurrentAvailableWriteOffAmount(
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

private void requireSubmitter(ProcessDocumentInstance instance, Long userId) {
        if (!Objects.equals(instance.getSubmitterUserId(), userId)) {
            throw new IllegalStateException("鍙湁鎻愬崟浜哄彲浠ユ墽琛屽綋鍓嶆搷浣?");
        }
    }

private ProcessDocumentInstance requireDocument(String documentCode) {
        String normalizedCode = trimToNull(documentCode);
        if (normalizedCode == null) {
            throw new IllegalArgumentException("Document code is required");
        }
        ProcessDocumentInstance instance = processDocumentInstanceMapper.selectOne(
                Wrappers.<ProcessDocumentInstance>lambdaQuery()
                        .eq(ProcessDocumentInstance::getDocumentCode, normalizedCode)
                        .last("limit 1")
        );
        if (instance == null) {
            throw new IllegalStateException("Document not found");
        }
        return instance;
    }

private Map<String, Object> readSchema(String schemaJson) {
        if (trimToNull(schemaJson) == null) {
            return defaultSchema();
        }
        try {
            return objectMapper.readValue(schemaJson, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse form schema", ex);
        }
    }

private Map<String, Object> defaultSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("layoutMode", "TWO_COLUMN");
        schema.put("blocks", Collections.emptyList());
        return schema;
    }

private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(String.valueOf(number));
        }
        try {
            String normalized = trimToNull(String.valueOf(value));
            return normalized == null ? null : new BigDecimal(normalized);
        } catch (Exception ex) {
            return null;
        }
    }

/**
 * 判断EffectiveApprovedStatus是否成立。
 */
private boolean isEffectiveApprovedStatus(String status) {
        String normalized = trimToNull(status);
        return DOCUMENT_STATUS_APPROVED.equals(normalized)
                || DOCUMENT_STATUS_COMPLETED.equals(normalized)
                || DOCUMENT_STATUS_PENDING_PAYMENT.equals(normalized)
                || DOCUMENT_STATUS_PAYMENT_COMPLETED.equals(normalized)
                || DOCUMENT_STATUS_PAYMENT_FINISHED.equals(normalized);
    }

private boolean matchesKeyword(String keyword, String... values) {
        if (keyword == null) {
            return true;
        }
        for (String value : values) {
            if (value != null && value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

/**
 * 解析模板类型Label。
 */
private String resolveTemplateTypeLabel(String templateType, String currentLabel) {
        if (trimToNull(currentLabel) != null) {
            return currentLabel;
        }
        return switch (trimToNull(templateType) == null ? "report" : templateType.trim()) {
            case "application" -> "\u7533\u8bf7\u5355";
            case "loan" -> "\u501f\u6b3e\u5355";
            case "contract" -> "\u5408\u540c\u5355";
            default -> "\u62a5\u9500\u5355";
        };
    }

/**
 * 解析StatusLabel。
 */
private String resolveStatusLabel(String status) {
        return switch (trimToNull(status) == null ? "" : status.trim()) {
            case DOCUMENT_STATUS_PENDING_PAYMENT -> "\u5f85\u652f\u4ed8";
            case DOCUMENT_STATUS_PAYING -> "\u652f\u4ed8\u4e2d";
            case DOCUMENT_STATUS_PAYMENT_COMPLETED -> "\u5df2\u652f\u4ed8";
            case DOCUMENT_STATUS_PAYMENT_FINISHED -> "\u5df2\u5b8c\u6210";
            case DOCUMENT_STATUS_PAYMENT_EXCEPTION -> "\u652f\u4ed8\u5f02\u5e38";
            case DOCUMENT_STATUS_APPROVED, DOCUMENT_STATUS_COMPLETED -> "\u5df2\u5b8c\u6210";
            case DOCUMENT_STATUS_REJECTED -> "\u5df2\u9a73\u56de";
            case "DRAFT" -> "\u8349\u7a3f";
            case DOCUMENT_STATUS_EXCEPTION -> "\u6d41\u7a0b\u5f02\u5e38";
            default -> "\u5ba1\u6279\u4e2d";
        };
    }

private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

private record DocumentBusinessBinding(
            String fieldKey,
            String componentCode,
            List<String> allowedTemplateTypes
    ) {
    }

private record RelatedDocumentSelection(
            String fieldKey,
            String documentCode,
            List<String> allowedTemplateTypes,
            int sortOrder
    ) {
    }

private record WriteOffSelection(
            String fieldKey,
            String documentCode,
            List<String> allowedTemplateTypes,
            BigDecimal requestedAmount,
            int sortOrder
    ) {
    }
}
