package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessCustomArchiveDetailVO;
import com.finex.auth.dto.ProcessCustomArchiveMetaVO;
import com.finex.auth.dto.ProcessCustomArchiveOperatorVO;
import com.finex.auth.dto.ProcessCustomArchiveResolveDTO;
import com.finex.auth.dto.ProcessCustomArchiveResolveResultVO;
import com.finex.auth.dto.ProcessCustomArchiveRuleFieldVO;
import com.finex.auth.dto.ProcessCustomArchiveSaveDTO;
import com.finex.auth.dto.ProcessCustomArchiveSummaryVO;
import com.finex.auth.entity.ProcessCustomArchiveDesign;
import com.finex.auth.entity.ProcessCustomArchiveItem;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ProcessCustomArchiveLifecycleSupport extends AbstractProcessCustomArchiveSupport {

    public ProcessCustomArchiveLifecycleSupport(
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
        super(categoryMapper, templateMapper, codeSequenceMapper, scopeMapper, customArchiveDesignMapper, customArchiveItemMapper, customArchiveRuleMapper, processExpenseTypeMapper, systemDepartmentMapper, userMapper, processFormDesignService, processExpenseDetailDesignService, processFlowDesignService, objectMapper);
    }

    public List<ProcessCustomArchiveSummaryVO> listCustomArchives() {
        List<ProcessCustomArchiveDesign> archives = getCustomArchiveDesignMapper().selectList(
                Wrappers.<ProcessCustomArchiveDesign>lambdaQuery()
                        .orderByDesc(ProcessCustomArchiveDesign::getCreatedAt, ProcessCustomArchiveDesign::getId)
        );
        if (archives.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> archiveIds = archives.stream().map(ProcessCustomArchiveDesign::getId).toList();
        Map<Long, Long> itemCountMap = getCustomArchiveItemMapper().selectList(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .in(ProcessCustomArchiveItem::getArchiveId, archiveIds)
        ).stream().collect(Collectors.groupingBy(ProcessCustomArchiveItem::getArchiveId, Collectors.counting()));

        return archives.stream().map(archive -> {
            ProcessCustomArchiveSummaryVO summary = new ProcessCustomArchiveSummaryVO();
            summary.setId(archive.getId());
            summary.setArchiveCode(archive.getArchiveCode());
            summary.setArchiveName(archive.getArchiveName());
            summary.setArchiveType(archive.getArchiveType());
            summary.setArchiveTypeLabel(resolveArchiveTypeLabel(archive.getArchiveType()));
            summary.setArchiveDescription(archive.getArchiveDescription());
            summary.setStatus(archive.getStatus());
            summary.setItemCount(itemCountMap.getOrDefault(archive.getId(), 0L).intValue());
            summary.setUpdatedAt(formatDateTime(archive.getUpdatedAt()));
            return summary;
        }).toList();
    }

    public ProcessCustomArchiveDetailVO getCustomArchiveDetail(Long id) {
        return buildCustomArchiveDetail(requireCustomArchive(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessCustomArchiveDetailVO createCustomArchive(ProcessCustomArchiveSaveDTO dto) {
        validateCustomArchive(dto);

        ProcessCustomArchiveDesign archive = new ProcessCustomArchiveDesign();
        applyCustomArchiveBase(archive, dto);
        archive.setArchiveCode(buildCustomArchiveCode());
        getCustomArchiveDesignMapper().insert(archive);

        replaceCustomArchiveItems(archive.getId(), dto);
        return buildCustomArchiveDetail(requireCustomArchive(archive.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public ProcessCustomArchiveDetailVO updateCustomArchive(Long id, ProcessCustomArchiveSaveDTO dto) {
        ProcessCustomArchiveDesign archive = requireCustomArchive(id);
        validateCustomArchive(dto);

        applyCustomArchiveBase(archive, dto);
        getCustomArchiveDesignMapper().updateById(archive);
        replaceCustomArchiveItems(id, dto);
        return buildCustomArchiveDetail(requireCustomArchive(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCustomArchiveStatus(Long id, Integer status) {
        ProcessCustomArchiveDesign archive = requireCustomArchive(id);
        archive.setStatus(normalizeStatus(status));
        getCustomArchiveDesignMapper().updateById(archive);
        return Boolean.TRUE;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCustomArchive(Long id) {
        ProcessCustomArchiveDesign archive = requireCustomArchive(id);
        List<ProcessCustomArchiveItem> items = getCustomArchiveItemMapper().selectList(
                Wrappers.<ProcessCustomArchiveItem>lambdaQuery()
                        .eq(ProcessCustomArchiveItem::getArchiveId, id)
        );
        List<String> itemCodes = items.stream()
                .map(ProcessCustomArchiveItem::getItemCode)
                .filter(Objects::nonNull)
                .toList();
        if (!itemCodes.isEmpty()) {
            Long referencedCount = getScopeMapper().selectCount(
                    Wrappers.<com.finex.auth.entity.ProcessTemplateScope>lambdaQuery()
                            .in(com.finex.auth.entity.ProcessTemplateScope::getOptionType, List.of("TAG_OPTION", "INSTALLMENT_OPTION"))
                            .in(com.finex.auth.entity.ProcessTemplateScope::getOptionCode, itemCodes)
            );
            if (referencedCount != null && referencedCount > 0) {
                throw new IllegalStateException("当前档案结果项已被模板引用，不能删除档案");
            }
        }

        Long archiveReferencedCount = getScopeMapper().selectCount(
                Wrappers.<com.finex.auth.entity.ProcessTemplateScope>lambdaQuery()
                        .in(com.finex.auth.entity.ProcessTemplateScope::getOptionType, List.of(SCOPE_TYPE_TAG_ARCHIVE, SCOPE_TYPE_INSTALLMENT_ARCHIVE))
                        .eq(com.finex.auth.entity.ProcessTemplateScope::getOptionCode, archive.getArchiveCode())
        );
        if (archiveReferencedCount != null && archiveReferencedCount > 0) {
            throw new IllegalStateException("当前档案已被模板作为标签或分期付款来源引用，不能删除");
        }

        deleteArchiveChildren(id);
        getCustomArchiveDesignMapper().deleteById(archive.getId());
        return Boolean.TRUE;
    }

    public ProcessCustomArchiveMetaVO getCustomArchiveMeta() {
        ProcessCustomArchiveMetaVO meta = new ProcessCustomArchiveMetaVO();
        meta.setArchiveTypeOptions(List.of(
                option("提供选择", ARCHIVE_TYPE_SELECT),
                option("自动划分", ARCHIVE_TYPE_AUTO_RULE)
        ));
        meta.setOperatorOptions(OPERATOR_KEYS.stream().map(key -> {
            ProcessCustomArchiveOperatorVO operator = new ProcessCustomArchiveOperatorVO();
            operator.setKey(key);
            operator.setLabel(OPERATOR_LABELS.getOrDefault(key, key));
            return operator;
        }).toList());
        meta.setRuleFields(RULE_FIELD_DEFINITIONS.stream().map(definition -> {
            ProcessCustomArchiveRuleFieldVO field = new ProcessCustomArchiveRuleFieldVO();
            field.setKey(definition.key());
            field.setLabel(definition.label());
            field.setValueType(definition.valueType());
            field.setOperatorKeys(new ArrayList<>(definition.operatorKeys()));
            return field;
        }).toList());
        meta.setDepartmentOptions(loadDepartmentOptions());
        meta.setTagArchiveCode(DEFAULT_TAG_ARCHIVE_CODE);
        meta.setInstallmentArchiveCode(DEFAULT_INSTALLMENT_ARCHIVE_CODE);
        return meta;
    }

    public ProcessCustomArchiveResolveResultVO resolveCustomArchive(ProcessCustomArchiveResolveDTO dto) {
        ProcessCustomArchiveDesign archive = requireCustomArchive(trimToEmpty(dto.getArchiveCode()));

        ProcessCustomArchiveResolveResultVO result = new ProcessCustomArchiveResolveResultVO();
        result.setArchiveCode(archive.getArchiveCode());
        result.setArchiveType(archive.getArchiveType());

        if (ARCHIVE_TYPE_SELECT.equals(archive.getArchiveType())) {
            result.setItems(resolveSelectArchive(archive.getId()));
            return result;
        }

        result.setItems(resolveAutoRuleArchive(archive.getId(), dto.getContext()));
        return result;
    }
}
