package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.ProcessCustomArchiveDesign;
import com.finex.auth.entity.ProcessCustomArchiveItem;
import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.entity.ProcessDocumentExpenseDetail;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessTemplateScope;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;

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

final class ExpenseSummaryLookupSupport extends AbstractExpenseSummarySupport {

    ExpenseSummaryLookupSupport(ExpenseSummarySupportContext context) {
        super(context);
    }

    Map<String, List<ProcessDocumentExpenseDetail>> loadExpenseDetailMap(List<String> documentCodes) {
        if (documentCodes == null || documentCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return processDocumentExpenseDetailMapper.selectList(
                Wrappers.<ProcessDocumentExpenseDetail>lambdaQuery()
                        .in(ProcessDocumentExpenseDetail::getDocumentCode, documentCodes)
                        .orderByAsc(ProcessDocumentExpenseDetail::getSortOrder, ProcessDocumentExpenseDetail::getId)
        ).stream().collect(Collectors.groupingBy(
                ProcessDocumentExpenseDetail::getDocumentCode,
                LinkedHashMap::new,
                Collectors.toList()
        ));
    }

    ExpenseSummaryLifecycleData loadLifecycleData(List<String> documentCodes) {
        if (documentCodes == null || documentCodes.isEmpty()) {
            return ExpenseSummaryLifecycleData.empty();
        }
        List<ProcessDocumentActionLog> lifecycleLogs = processDocumentActionLogMapper.selectList(
                Wrappers.<ProcessDocumentActionLog>lambdaQuery()
                        .in(ProcessDocumentActionLog::getDocumentCode, documentCodes)
                        .in(ProcessDocumentActionLog::getActionType, List.of(LOG_SUBMIT, LOG_RESUBMIT, LOG_RECALL))
                        .orderByDesc(ProcessDocumentActionLog::getCreatedAt, ProcessDocumentActionLog::getId)
        );
        Map<String, LocalDateTime> latestSubmitAtMap = new LinkedHashMap<>();
        Set<String> formalProcessHistoryDocumentCodes = new LinkedHashSet<>();
        for (ProcessDocumentActionLog log : lifecycleLogs) {
            String documentCode = trimToNull(log.getDocumentCode());
            if (documentCode == null) {
                continue;
            }
            formalProcessHistoryDocumentCodes.add(documentCode);
            if ((Objects.equals(log.getActionType(), LOG_SUBMIT) || Objects.equals(log.getActionType(), LOG_RESUBMIT))
                    && log.getCreatedAt() != null) {
                latestSubmitAtMap.putIfAbsent(documentCode, log.getCreatedAt());
            }
        }
        return new ExpenseSummaryLifecycleData(latestSubmitAtMap, formalProcessHistoryDocumentCodes);
    }

    Map<String, ProcessDocumentTemplate> loadTemplateMap(List<String> templateCodes) {
        if (templateCodes == null || templateCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return templateMapper.selectList(
                Wrappers.<ProcessDocumentTemplate>lambdaQuery()
                        .in(ProcessDocumentTemplate::getTemplateCode, templateCodes)
        ).stream().collect(Collectors.toMap(
                ProcessDocumentTemplate::getTemplateCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    Map<String, String> loadTagArchiveCodeByTemplateCode(Map<String, ProcessDocumentTemplate> templateMap) {
        if (templateMap == null || templateMap.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> templateIds = templateMap.values().stream()
                .map(ProcessDocumentTemplate::getId)
                .filter(Objects::nonNull)
                .toList();
        if (templateIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> templateCodeById = templateMap.values().stream()
                .filter(item -> item.getId() != null && trimToNull(item.getTemplateCode()) != null)
                .collect(Collectors.toMap(
                        ProcessDocumentTemplate::getId,
                        ProcessDocumentTemplate::getTemplateCode,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        return processTemplateScopeMapper.selectList(
                Wrappers.<ProcessTemplateScope>lambdaQuery()
                        .in(ProcessTemplateScope::getTemplateId, templateIds)
                        .eq(ProcessTemplateScope::getOptionType, TEMPLATE_SCOPE_TYPE_TAG_ARCHIVE)
                        .orderByAsc(ProcessTemplateScope::getSortOrder, ProcessTemplateScope::getId)
        ).stream()
                .filter(item -> trimToNull(templateCodeById.get(item.getTemplateId())) != null)
                .collect(Collectors.toMap(
                        item -> templateCodeById.get(item.getTemplateId()),
                        ProcessTemplateScope::getOptionCode,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    Map<Long, User> loadUserMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .in(User::getId, userIds)
        ).stream().collect(Collectors.toMap(
                User::getId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    Map<String, SystemCompany> loadCompanyMap(Set<String> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return systemCompanyMapper.selectList(
                Wrappers.<SystemCompany>lambdaQuery()
                        .in(SystemCompany::getCompanyId, companyIds)
        ).stream().collect(Collectors.toMap(
                SystemCompany::getCompanyId,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    Map<String, FinanceVendor> loadVendorMap(Set<String> vendorCodes) {
        if (vendorCodes == null || vendorCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return financeVendorMapper.selectList(
                Wrappers.<FinanceVendor>lambdaQuery()
                        .in(FinanceVendor::getCVenCode, vendorCodes)
        ).stream().collect(Collectors.toMap(
                FinanceVendor::getCVenCode,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    Map<String, String> loadDepartmentNameMap(Set<String> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> departmentIdValues = departmentIds.stream().map(this::toLong).filter(Objects::nonNull).toList();
        if (departmentIdValues.isEmpty()) {
            return Collections.emptyMap();
        }
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .in(SystemDepartment::getId, departmentIdValues)
        ).stream().collect(Collectors.toMap(
                item -> String.valueOf(item.getId()),
                SystemDepartment::getDeptName,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    Map<String, Map<String, String>> loadArchiveItemLabelMap(Set<String> archiveCodes) {
        if (archiveCodes == null || archiveCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ProcessCustomArchiveDesign> archives = customArchiveDesignMapper.selectList(
                Wrappers.<ProcessCustomArchiveDesign>lambdaQuery()
                        .in(ProcessCustomArchiveDesign::getArchiveCode, archiveCodes)
        );
        if (archives.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> archiveCodeById = archives.stream().collect(Collectors.toMap(
                ProcessCustomArchiveDesign::getId,
                ProcessCustomArchiveDesign::getArchiveCode,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        List<ProcessCustomArchiveItem> items = customArchiveItemMapper.selectList(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .in(ProcessCustomArchiveItem::getArchiveId, archiveCodeById.keySet())
                        .eq(ProcessCustomArchiveItem::getStatus, 1)
                        .orderByAsc(ProcessCustomArchiveItem::getPriority, ProcessCustomArchiveItem::getId)
        );
        Map<String, Map<String, String>> labelMap = new LinkedHashMap<>();
        for (ProcessCustomArchiveItem item : items) {
            String archiveCode = archiveCodeById.get(item.getArchiveId());
            if (archiveCode == null) {
                continue;
            }
            labelMap.computeIfAbsent(archiveCode, ignored -> new LinkedHashMap<>())
                    .put(trimToNull(item.getItemCode()) == null ? item.getItemName() : item.getItemCode(), item.getItemName());
        }
        return labelMap;
    }

    void addSubmitterDepartmentIds(Map<Long, User> userMap, Set<String> departmentIds) {
        if (userMap == null || userMap.isEmpty() || departmentIds == null) {
            return;
        }
        userMap.values().stream()
                .map(User::getDeptId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .forEach(departmentIds::add);
    }

    ExpenseSummaryAssembler.SummaryMetadata buildMetadata(
            Long submitterUserId,
            ExpenseSummaryAssembler.SummaryDraft draft,
            Map<Long, User> userMap,
            Map<String, SystemCompany> companyMap,
            Map<String, FinanceVendor> vendorMap,
            Map<String, String> departmentNameMap,
            Map<String, Map<String, String>> archiveItemLabelMap
    ) {
        User submitter = submitterUserId == null ? null : userMap.get(submitterUserId);
        return new ExpenseSummaryAssembler.SummaryMetadata(
                submitter == null || submitter.getDeptId() == null ? null : departmentNameMap.get(String.valueOf(submitter.getDeptId())),
                draft == null ? null : draft.getPaymentCompanyId(),
                draft == null ? null : resolvePaymentCompanyName(draft.getPaymentCompanyId(), companyMap),
                draft == null ? null : resolvePartyName(draft.getPayeeValue(), userMap, vendorMap),
                draft == null ? null : resolveVendorName(draft.getCounterpartyValue(), vendorMap),
                draft == null ? null : draft.getPaymentDate(),
                draft == null ? Collections.emptyList() : resolveDepartmentNames(draft.getUndertakeDepartmentIds(), departmentNameMap),
                draft == null ? Collections.emptyList() : resolveArchiveItemNames(draft.getTagArchiveCode(), draft.getTagValues(), archiveItemLabelMap)
        );
    }

    private String resolvePaymentCompanyName(String companyId, Map<String, SystemCompany> companyMap) {
        String normalized = trimToNull(companyId);
        if (normalized == null) {
            return null;
        }
        SystemCompany company = companyMap.get(normalized);
        return company == null ? normalized : firstNonBlank(company.getCompanyName(), company.getCompanyCode(), normalized);
    }

    private String resolvePartyName(String value, Map<Long, User> userMap, Map<String, FinanceVendor> vendorMap) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith(PERSONAL_PAYEE_VALUE_PREFIX)) {
            return trimToNull(normalized.substring(PERSONAL_PAYEE_VALUE_PREFIX.length()));
        }
        if (normalized.startsWith("USER:")) {
            Long userId = toLong(normalized.substring("USER:".length()));
            User user = userId == null ? null : userMap.get(userId);
            return user == null ? normalized : firstNonBlank(user.getName(), user.getUsername(), normalized);
        }
        return resolveVendorName(normalized, vendorMap);
    }

    private String resolveVendorName(String value, Map<String, FinanceVendor> vendorMap) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith("VENDOR:")) {
            normalized = trimToNull(normalized.substring("VENDOR:".length()));
        }
        if (normalized == null) {
            return null;
        }
        FinanceVendor vendor = vendorMap.get(normalized);
        return vendor == null ? normalized : firstNonBlank(vendor.getCVenName(), vendor.getCVenAbbName(), normalized);
    }

    private List<String> resolveDepartmentNames(List<String> departmentIds, Map<String, String> departmentNameMap) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String departmentId : departmentIds) {
            String normalized = trimToNull(departmentId);
            if (normalized == null) {
                continue;
            }
            names.add(defaultText(trimToNull(departmentNameMap.get(normalized)), normalized));
        }
        return new ArrayList<>(names);
    }

    private List<String> resolveArchiveItemNames(
            String archiveCode,
            List<String> values,
            Map<String, Map<String, String>> archiveItemLabelMap
    ) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, String> labelMap = trimToNull(archiveCode) == null
                ? Collections.emptyMap()
                : archiveItemLabelMap.getOrDefault(archiveCode, Collections.emptyMap());
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized == null) {
                continue;
            }
            names.add(defaultText(trimToNull(labelMap.get(normalized)), normalized));
        }
        return new ArrayList<>(names);
    }
}
