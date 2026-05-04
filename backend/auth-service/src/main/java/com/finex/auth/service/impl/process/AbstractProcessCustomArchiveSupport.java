package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessCustomArchiveDetailVO;
import com.finex.auth.dto.ProcessCustomArchiveItemDTO;
import com.finex.auth.dto.ProcessCustomArchiveResolveItemVO;
import com.finex.auth.dto.ProcessCustomArchiveRuleDTO;
import com.finex.auth.dto.ProcessCustomArchiveSaveDTO;
import com.finex.auth.entity.ProcessCustomArchiveDesign;
import com.finex.auth.entity.ProcessCustomArchiveItem;
import com.finex.auth.entity.ProcessCustomArchiveRule;
import com.finex.auth.mapper.CodeSequenceMapper;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessCustomArchiveItemMapper;
import com.finex.auth.mapper.ProcessCustomArchiveRuleMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessTemplateCategoryMapper;
import com.finex.auth.mapper.ProcessTemplateScopeMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.ProcessExpenseDetailDesignService;
import com.finex.auth.service.ProcessFlowDesignService;
import com.finex.auth.service.ProcessFormDesignService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

abstract class AbstractProcessCustomArchiveSupport extends AbstractProcessManagementSupport {

    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    protected AbstractProcessCustomArchiveSupport(
            ProcessTemplateCategoryMapper categoryMapper,
            ProcessDocumentTemplateMapper templateMapper,
            CodeSequenceMapper codeSequenceMapper,
            ProcessTemplateScopeMapper scopeMapper,
            ProcessCustomArchiveDesignMapper customArchiveDesignMapper,
            ProcessCustomArchiveItemMapper customArchiveItemMapper,
            ProcessCustomArchiveRuleMapper customArchiveRuleMapper,
            ProcessExpenseTypeMapper processExpenseTypeMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper,
            ProcessFormDesignService processFormDesignService,
            ProcessExpenseDetailDesignService processExpenseDetailDesignService,
            ProcessFlowDesignService processFlowDesignService,
            ObjectMapper objectMapper
    ) {
        super(
                categoryMapper,
                templateMapper,
                codeSequenceMapper,
                scopeMapper,
                customArchiveDesignMapper,
                customArchiveItemMapper,
                customArchiveRuleMapper,
                processExpenseTypeMapper,
                systemDepartmentMapper,
                userMapper,
                processFormDesignService,
                processExpenseDetailDesignService,
                processFlowDesignService,
                objectMapper
        );
    }

    protected ProcessCustomArchiveDesign requireCustomArchive(Long id) {
        ProcessCustomArchiveDesign archive = getCustomArchiveDesignMapper().selectById(id);
        if (archive == null) {
            throw new IllegalStateException("\u81ea\u5b9a\u4e49\u6863\u6848\u4e0d\u5b58\u5728");
        }
        return archive;
    }

    protected ProcessCustomArchiveDesign requireCustomArchive(String archiveCode) {
        ProcessCustomArchiveDesign archive = getCustomArchiveDesignMapper().selectOne(
                Wrappers.<ProcessCustomArchiveDesign>lambdaQuery()
                        .eq(ProcessCustomArchiveDesign::getArchiveCode, archiveCode)
                        .last("limit 1")
        );
        if (archive == null) {
            throw new IllegalStateException("\u81ea\u5b9a\u4e49\u6863\u6848\u4e0d\u5b58\u5728");
        }
        return archive;
    }

    protected ProcessCustomArchiveDetailVO buildCustomArchiveDetail(ProcessCustomArchiveDesign archive) {
        ProcessCustomArchiveDetailVO detail = new ProcessCustomArchiveDetailVO();
        detail.setId(archive.getId());
        detail.setArchiveCode(archive.getArchiveCode());
        detail.setArchiveName(archive.getArchiveName());
        detail.setArchiveType(archive.getArchiveType());
        detail.setArchiveTypeLabel(resolveArchiveTypeLabel(archive.getArchiveType()));
        detail.setArchiveDescription(archive.getArchiveDescription());
        detail.setStatus(archive.getStatus());

        List<ProcessCustomArchiveItem> items = getCustomArchiveItemMapper().selectList(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .eq(ProcessCustomArchiveItem::getArchiveId, archive.getId())
        );
        items = sortArchiveItems(items, archive.getArchiveType());

        Map<Long, List<ProcessCustomArchiveRule>> ruleMap = loadRuleMap(
                items.stream().map(ProcessCustomArchiveItem::getId).toList()
        );
        detail.setItems(items.stream().map(item -> toArchiveItemDto(item, ruleMap.getOrDefault(item.getId(), Collections.emptyList()))).toList());
        return detail;
    }

    protected ProcessCustomArchiveItemDTO toArchiveItemDto(ProcessCustomArchiveItem item, List<ProcessCustomArchiveRule> rules) {
        ProcessCustomArchiveItemDTO dto = new ProcessCustomArchiveItemDTO();
        dto.setId(item.getId());
        dto.setItemCode(item.getItemCode());
        dto.setItemName(item.getItemName());
        dto.setPriority(item.getPriority());
        dto.setStatus(item.getStatus());
        dto.setRules(rules.stream().map(this::toArchiveRuleDto).toList());
        return dto;
    }

    protected ProcessCustomArchiveRuleDTO toArchiveRuleDto(ProcessCustomArchiveRule rule) {
        ProcessCustomArchiveRuleDTO dto = new ProcessCustomArchiveRuleDTO();
        dto.setId(rule.getId());
        dto.setGroupNo(rule.getGroupNo());
        dto.setFieldKey(rule.getFieldKey());
        dto.setOperator(rule.getOperator());
        dto.setCompareValue(deserializeCompareValue(rule.getCompareValue()));
        return dto;
    }

    protected Map<Long, List<ProcessCustomArchiveRule>> loadRuleMap(List<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return getCustomArchiveRuleMapper().selectList(
                Wrappers.<ProcessCustomArchiveRule>lambdaQuery()
                        .in(ProcessCustomArchiveRule::getArchiveItemId, itemIds)
                        .orderByAsc(ProcessCustomArchiveRule::getGroupNo, ProcessCustomArchiveRule::getId)
        ).stream().collect(Collectors.groupingBy(
                ProcessCustomArchiveRule::getArchiveItemId,
                LinkedHashMap::new,
                Collectors.toList()
        ));
    }

    protected void applyCustomArchiveBase(ProcessCustomArchiveDesign archive, ProcessCustomArchiveSaveDTO dto) {
        archive.setArchiveName(trimToEmpty(dto.getArchiveName()));
        archive.setArchiveType(trimToEmpty(dto.getArchiveType()));
        archive.setArchiveDescription(trimToNull(dto.getArchiveDescription()));
        archive.setStatus(normalizeStatus(dto.getStatus()));
    }

    protected void validateCustomArchive(ProcessCustomArchiveSaveDTO dto) {
        if (!Set.of(ARCHIVE_TYPE_SELECT, ARCHIVE_TYPE_AUTO_RULE).contains(trimToEmpty(dto.getArchiveType()))) {
            throw new IllegalArgumentException("\u6863\u6848\u7c7b\u578b\u4e0d\u5408\u6cd5\uff0c\u53ea\u652f\u6301 SELECT \u6216 AUTO_RULE");
        }
        validatePmNameLength(dto.getArchiveName(), "\u6863\u6848\u540d\u79f0");
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("\u8bf7\u81f3\u5c11\u6dfb\u52a0\u4e00\u4e2a\u7ed3\u679c\u9879");
        }

        for (int index = 0; index < dto.getItems().size(); index++) {
            ProcessCustomArchiveItemDTO item = dto.getItems().get(index);
            if (trimToNull(item.getItemName()) == null) {
                throw new IllegalArgumentException("\u7ed3\u679c\u9879\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
            }
            validatePmNameLength(item.getItemName(), "\u7b2c " + (index + 1) + " \u4e2a\u7ed3\u679c\u9879\u540d\u79f0");
            if (ARCHIVE_TYPE_AUTO_RULE.equals(dto.getArchiveType())) {
                validateRules(item.getRules());
            }
        }
    }

    protected void validateRules(List<ProcessCustomArchiveRuleDTO> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("\u81ea\u52a8\u5212\u5206\u7c7b\u578b\u5fc5\u987b\u914d\u7f6e\u89c4\u5219");
        }
        for (ProcessCustomArchiveRuleDTO rule : rules) {
            if (rule.getGroupNo() == null || rule.getGroupNo() < 1) {
                throw new IllegalArgumentException("\u89c4\u5219\u7ec4\u5e8f\u53f7\u5fc5\u987b\u5927\u4e8e 0");
            }
            String fieldKey = trimToNull(rule.getFieldKey());
            validateFieldKeyLength(fieldKey, "\u89c4\u5219\u5b57\u6bb5");
            ProcessExpenseConditionFieldSupport.ConditionFieldDefinition definition = CUSTOM_ARCHIVE_RULE_FIELD_MAP.get(fieldKey);
            if (definition == null) {
                throw new IllegalArgumentException("\u4e0d\u652f\u6301\u7684\u89c4\u5219\u5b57\u6bb5: " + rule.getFieldKey());
            }
            if (!definition.operatorKeys().contains(rule.getOperator())) {
                throw new IllegalArgumentException("\u5b57\u6bb5 " + definition.label() + " \u4e0d\u652f\u6301\u64cd\u4f5c\u7b26 " + rule.getOperator());
            }
            if (rule.getCompareValue() == null) {
                throw new IllegalArgumentException("\u89c4\u5219\u6bd4\u8f83\u503c\u4e0d\u80fd\u4e3a\u7a7a");
            }
            if ("BETWEEN".equals(rule.getOperator())) {
                Object compareValue = rule.getCompareValue();
                if (!(compareValue instanceof List<?> valueList) || valueList.size() < 2) {
                    throw new IllegalArgumentException("BETWEEN \u64cd\u4f5c\u7b26\u9700\u8981\u4f20\u5165\u4e24\u4e2a\u6bd4\u8f83\u503c");
                }
            }
        }
    }

    protected void replaceCustomArchiveItems(Long archiveId, ProcessCustomArchiveSaveDTO dto) {
        deleteArchiveChildren(archiveId);

        for (int index = 0; index < dto.getItems().size(); index++) {
            ProcessCustomArchiveItemDTO itemDto = dto.getItems().get(index);
            ProcessCustomArchiveItem item = new ProcessCustomArchiveItem();
            item.setArchiveId(archiveId);
            item.setItemCode(resolveArchiveItemCode(itemDto));
            item.setItemName(trimToEmpty(itemDto.getItemName()));
            item.setPriority(itemDto.getPriority() == null ? index + 1 : itemDto.getPriority());
            item.setStatus(normalizeStatus(itemDto.getStatus()));
            getCustomArchiveItemMapper().insert(item);

            if (!ARCHIVE_TYPE_AUTO_RULE.equals(dto.getArchiveType())) {
                continue;
            }

            for (ProcessCustomArchiveRuleDTO ruleDto : itemDto.getRules()) {
                ProcessCustomArchiveRule rule = new ProcessCustomArchiveRule();
                rule.setArchiveItemId(item.getId());
                rule.setGroupNo(ruleDto.getGroupNo());
                rule.setFieldKey(trimToEmpty(ruleDto.getFieldKey()));
                rule.setOperator(trimToEmpty(ruleDto.getOperator()));
                rule.setCompareValue(serializeCompareValue(ruleDto.getCompareValue()));
                getCustomArchiveRuleMapper().insert(rule);
            }
        }
    }

    protected String resolveArchiveItemCode(ProcessCustomArchiveItemDTO itemDto) {
        String itemCode = trimToNull(itemDto.getItemCode());
        return itemCode != null ? itemCode : buildCustomArchiveItemCode();
    }

    protected void deleteArchiveChildren(Long archiveId) {
        List<ProcessCustomArchiveItem> items = getCustomArchiveItemMapper().selectList(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .eq(ProcessCustomArchiveItem::getArchiveId, archiveId)
        );
        if (!items.isEmpty()) {
            getCustomArchiveRuleMapper().delete(
                    Wrappers.<ProcessCustomArchiveRule>lambdaQuery()
                            .in(ProcessCustomArchiveRule::getArchiveItemId, items.stream().map(ProcessCustomArchiveItem::getId).toList())
            );
        }
        getCustomArchiveItemMapper().delete(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .eq(ProcessCustomArchiveItem::getArchiveId, archiveId)
        );
    }

    protected List<ProcessCustomArchiveItem> sortArchiveItems(List<ProcessCustomArchiveItem> items, String archiveType) {
        Comparator<ProcessCustomArchiveItem> comparator;
        if (ARCHIVE_TYPE_AUTO_RULE.equals(archiveType)) {
            comparator = Comparator
                    .comparing(ProcessCustomArchiveItem::getPriority, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(ProcessCustomArchiveItem::getId, Comparator.nullsLast(Long::compareTo));
        } else {
            comparator = Comparator.comparing(ProcessCustomArchiveItem::getId, Comparator.nullsLast(Long::compareTo));
        }
        return items.stream().sorted(comparator).toList();
    }

    protected String buildCustomArchiveCode() {
        String prefix = CUSTOM_ARCHIVE_CODE_PREFIX + LocalDate.now().format(CODE_DATE_FORMATTER);
        Long count = getCustomArchiveDesignMapper().selectCount(
                Wrappers.<ProcessCustomArchiveDesign>lambdaQuery()
                        .likeRight(ProcessCustomArchiveDesign::getArchiveCode, prefix)
        );
        long next = count == null ? 1L : count + 1L;
        return prefix + String.format("%04d", next);
    }

    protected String buildCustomArchiveItemCode() {
        String prefix = CUSTOM_ARCHIVE_ITEM_CODE_PREFIX + LocalDate.now().format(CODE_DATE_FORMATTER);
        Long count = getCustomArchiveItemMapper().selectCount(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .likeRight(ProcessCustomArchiveItem::getItemCode, prefix)
        );
        long next = count == null ? 1L : count + 1L;
        return prefix + String.format("%04d", next);
    }

    protected String serializeCompareValue(Object compareValue) {
        try {
            return getObjectMapper().writeValueAsString(compareValue);
        } catch (Exception ex) {
            throw new IllegalStateException("\u5e8f\u5217\u5316\u89c4\u5219\u6bd4\u8f83\u503c\u5931\u8d25", ex);
        }
    }

    protected Object deserializeCompareValue(String compareValue) {
        if (trimToNull(compareValue) == null) {
            return null;
        }
        try {
            return getObjectMapper().readValue(compareValue, Object.class);
        } catch (Exception ex) {
            throw new IllegalStateException("\u53cd\u5e8f\u5217\u5316\u89c4\u5219\u6bd4\u8f83\u503c\u5931\u8d25", ex);
        }
    }

    protected List<ProcessCustomArchiveResolveItemVO> resolveSelectArchive(Long archiveId) {
        return getCustomArchiveItemMapper().selectList(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .eq(ProcessCustomArchiveItem::getArchiveId, archiveId)
                        .eq(ProcessCustomArchiveItem::getStatus, 1)
                        .orderByAsc(ProcessCustomArchiveItem::getId)
        ).stream().map(this::toResolvedItem).toList();
    }

    protected List<ProcessCustomArchiveResolveItemVO> resolveAutoRuleArchive(Long archiveId, Map<String, Object> context) {
        List<ProcessCustomArchiveItem> items = getCustomArchiveItemMapper().selectList(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .eq(ProcessCustomArchiveItem::getArchiveId, archiveId)
                        .eq(ProcessCustomArchiveItem::getStatus, 1)
                        .orderByAsc(ProcessCustomArchiveItem::getPriority, ProcessCustomArchiveItem::getId)
        );
        Map<Long, List<ProcessCustomArchiveRule>> ruleMap = loadRuleMap(items.stream().map(ProcessCustomArchiveItem::getId).toList());

        List<ProcessCustomArchiveResolveItemVO> resolvedItems = new ArrayList<>();
        for (ProcessCustomArchiveItem item : items) {
            if (matchesItem(ruleMap.getOrDefault(item.getId(), Collections.emptyList()), context)) {
                resolvedItems.add(toResolvedItem(item));
            }
        }
        return resolvedItems;
    }

    protected boolean matchesItem(List<ProcessCustomArchiveRule> rules, Map<String, Object> context) {
        if (rules.isEmpty()) {
            return false;
        }
        Map<Integer, List<ProcessCustomArchiveRule>> groupedRules = rules.stream().collect(Collectors.groupingBy(
                ProcessCustomArchiveRule::getGroupNo,
                LinkedHashMap::new,
                Collectors.toList()
        ));
        for (List<ProcessCustomArchiveRule> groupRules : groupedRules.values()) {
            boolean allMatched = true;
            for (ProcessCustomArchiveRule rule : groupRules) {
                if (!matchesRule(rule, context)) {
                    allMatched = false;
                    break;
                }
            }
            if (allMatched) {
                return true;
            }
        }
        return false;
    }

    protected boolean matchesRule(ProcessCustomArchiveRule rule, Map<String, Object> context) {
        Object actualValue = context == null ? null : context.get(rule.getFieldKey());
        Object expectedValue = deserializeCompareValue(rule.getCompareValue());
        return switch (rule.getOperator()) {
            case "EQ" -> equalsComparable(actualValue, expectedValue);
            case "NE" -> !equalsComparable(actualValue, expectedValue);
            case "IN" -> collectionContains(expectedValue, actualValue);
            case "NOT_IN" -> !collectionContains(expectedValue, actualValue);
            case "GT" -> compareNumbers(actualValue, expectedValue) > 0;
            case "GE" -> compareNumbers(actualValue, expectedValue) >= 0;
            case "LT" -> compareNumbers(actualValue, expectedValue) < 0;
            case "LE" -> compareNumbers(actualValue, expectedValue) <= 0;
            case "BETWEEN" -> matchesBetween(actualValue, expectedValue);
            case "CONTAINS" -> normalizeComparable(actualValue).contains(normalizeComparable(expectedValue));
            default -> false;
        };
    }

    protected boolean equalsComparable(Object actualValue, Object expectedValue) {
        java.math.BigDecimal actualNumber = toBigDecimal(actualValue);
        java.math.BigDecimal expectedNumber = toBigDecimal(expectedValue);
        if (actualNumber != null && expectedNumber != null) {
            return actualNumber.compareTo(expectedNumber) == 0;
        }
        return Objects.equals(normalizeComparable(actualValue), normalizeComparable(expectedValue));
    }

    protected boolean collectionContains(Object collectionValue, Object actualValue) {
        if (collectionValue instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (equalsComparable(actualValue, item)) {
                    return true;
                }
            }
            return false;
        }
        return equalsComparable(actualValue, collectionValue);
    }

    protected int compareNumbers(Object actualValue, Object expectedValue) {
        java.math.BigDecimal actualNumber = toBigDecimal(actualValue);
        java.math.BigDecimal expectedNumber = toBigDecimal(expectedValue);
        if (actualNumber == null || expectedNumber == null) {
            return -1;
        }
        return actualNumber.compareTo(expectedNumber);
    }

    protected boolean matchesBetween(Object actualValue, Object expectedValue) {
        java.math.BigDecimal actualNumber = toBigDecimal(actualValue);
        if (actualNumber == null || !(expectedValue instanceof List<?> valueList) || valueList.size() < 2) {
            return false;
        }
        java.math.BigDecimal start = toBigDecimal(valueList.get(0));
        java.math.BigDecimal end = toBigDecimal(valueList.get(1));
        if (start == null || end == null) {
            return false;
        }
        return actualNumber.compareTo(start) >= 0 && actualNumber.compareTo(end) <= 0;
    }

    protected ProcessCustomArchiveResolveItemVO toResolvedItem(ProcessCustomArchiveItem item) {
        ProcessCustomArchiveResolveItemVO resolvedItem = new ProcessCustomArchiveResolveItemVO();
        resolvedItem.setItemCode(item.getItemCode());
        resolvedItem.setItemName(item.getItemName());
        resolvedItem.setPriority(item.getPriority());
        return resolvedItem;
    }

    protected String resolveArchiveTypeLabel(String archiveType) {
        if (ARCHIVE_TYPE_AUTO_RULE.equals(archiveType)) {
            return "\u81ea\u52a8\u5212\u5206";
        }
        return "\u63d0\u4f9b\u9009\u62e9";
    }
}
