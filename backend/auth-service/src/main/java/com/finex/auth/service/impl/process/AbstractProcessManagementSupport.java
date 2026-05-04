// 娑撴艾濮熼崺鐕傜窗濞翠胶鈻煎Ο鈩冩緲娑撳孩绁︾粙瀣帳缂?// 閺傚洣娆㈢憴鎺曞閿涙岸鈧氨鏁ら弨顖涙嫼缁?// 娑撳﹣绗呭〒绋垮彠缁紮绱版稉濠冪埗闁艾鐖堕弶銉ㄥ殰 濞翠胶鈻肩粻锛勬倞妞ょ敻娼扮€电懓绨查惃?Controller閿涘奔绗呭〒闀愮窗缂佈呯敾閸楀繗鐨?濞翠胶鈻煎Ο鈩冩緲閵嗕焦濮ら柨鈧猾璇茬€烽妴浣藉殰鐎规矮绠熷锝嗩攳閸滃苯褰傜敮鍐Ц閹降鈧?// 妞嬪酣娅撻幓鎰板晪閿涙碍鏁奸崸蹇撴倵閺堚偓鐎硅妲楄ぐ鍗炴惙 鐎光剝澹掔捄顖滄暠閵嗕焦膩閺夊灝褰傜敮鍐ㄦ嫲閸氬海鐢婚崡鏇熷祦濞翠浇娴嗛妴?
package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessCenterNavItemVO;
import com.finex.auth.dto.ProcessCenterOverviewVO;
import com.finex.auth.dto.ProcessCustomArchiveDetailVO;
import com.finex.auth.dto.ProcessCustomArchiveItemDTO;
import com.finex.auth.dto.ProcessCustomArchiveMetaVO;
import com.finex.auth.dto.ProcessCustomArchiveResolveDTO;
import com.finex.auth.dto.ProcessCustomArchiveResolveItemVO;
import com.finex.auth.dto.ProcessCustomArchiveResolveResultVO;
import com.finex.auth.dto.ProcessCustomArchiveRuleDTO;
import com.finex.auth.dto.ProcessCustomArchiveRuleFieldVO;
import com.finex.auth.dto.ProcessCustomArchiveSaveDTO;
import com.finex.auth.dto.ProcessCustomArchiveSummaryVO;
import com.finex.auth.dto.ProcessExpenseTypeConfigOptionVO;
import com.finex.auth.dto.ProcessExpenseTypeDetailVO;
import com.finex.auth.dto.ProcessExpenseTypeMetaVO;
import com.finex.auth.dto.ProcessExpenseTypeSaveDTO;
import com.finex.auth.dto.ProcessExpenseTypeTreeVO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.dto.ProcessTemplateDetailVO;
import com.finex.auth.dto.ProcessTemplateFormOptionsVO;
import com.finex.auth.dto.ProcessTemplateSaveDTO;
import com.finex.auth.dto.ProcessTemplateSaveResultVO;
import com.finex.auth.dto.ProcessTemplateTypeVO;
import com.finex.auth.entity.ProcessCustomArchiveDesign;
import com.finex.auth.entity.ProcessCustomArchiveItem;
import com.finex.auth.entity.ProcessCustomArchiveRule;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseType;
import com.finex.auth.entity.ProcessTemplateScope;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.interceptor.TemplateSaveTraceInterceptor;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
abstract class AbstractProcessManagementSupport {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter CODE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    protected static final String HIGHLIGHT_SEPARATOR = "|";
    private static final String TEMPLATE_CODE_PREFIX = "FX";
    private static final String TEMPLATE_CODE_SEQUENCE_KEY = "DOCUMENT_TEMPLATE";
    private static final int TEMPLATE_CODE_RETRY_LIMIT = 3;
    private static final int PM_NAME_MAX_LENGTH = 64;
    private static final int PM_FIELD_KEY_MAX_LENGTH = 64;

    protected static final String DEFAULT_NUMBERING_RULE_CODE = "FX_DATE_4SEQ";
    protected static final String DEFAULT_TEMPLATE_COLOR = "blue";
    protected static final String TEMPLATE_STATUS_ENABLED = "ENABLED";
    protected static final String TEMPLATE_STATUS_DRAFT = "DRAFT";
    protected static final String TEMPLATE_STATUS_DELETED = "DELETED";
    protected static final String TEMPLATE_COPY_SUFFIX = " \u002d \u526f\u672c";
    protected static final String SCOPE_TYPE_DEPARTMENT = "SCOPE_DEPARTMENT";
    protected static final String SCOPE_TYPE_EXPENSE_TYPE = "SCOPE_EXPENSE_TYPE";
    protected static final String SCOPE_TYPE_AMOUNT_MIN = "SCOPE_AMOUNT_MIN";
    protected static final String SCOPE_TYPE_AMOUNT_MAX = "SCOPE_AMOUNT_MAX";
    protected static final String SCOPE_TYPE_TAG_ARCHIVE = "TAG_ARCHIVE";
    protected static final String SCOPE_TYPE_INSTALLMENT_ARCHIVE = "INSTALLMENT_ARCHIVE";

    protected static final String ARCHIVE_TYPE_SELECT = "SELECT";
    protected static final String ARCHIVE_TYPE_AUTO_RULE = "AUTO_RULE";
    protected static final String CUSTOM_ARCHIVE_CODE_PREFIX = "CA";
    protected static final String CUSTOM_ARCHIVE_ITEM_CODE_PREFIX = "CI";
    protected static final String DEFAULT_TAG_ARCHIVE_CODE = "PROCESS_TAG_OPTIONS";
    protected static final String DEFAULT_INSTALLMENT_ARCHIVE_CODE = "PROCESS_INSTALLMENT_OPTIONS";

    private static final String FIELD_VALUE_TYPE_TEXT = "text";
    private static final String FIELD_VALUE_TYPE_NUMBER = "number";
    private static final String FIELD_VALUE_TYPE_DEPARTMENT = "department";

    protected static final String EXPENSE_TYPE_INVOICE_FREE = "FREE";
    protected static final String EXPENSE_TYPE_INVOICE_REQUIRED = "NOT_FREE";
    protected static final String EXPENSE_TYPE_TAX_DEFAULT = "DEFAULT";
    protected static final String EXPENSE_TYPE_TAX_SPECIAL_NO_DEDUCT_NEED_OUT = "SPECIAL_NO_DEDUCT_NEED_OUT";
    protected static final String EXPENSE_TYPE_TAX_SPECIAL_NO_DEDUCT_NEED_OUT_OTHERS_NONE = "SPECIAL_NO_DEDUCT_NEED_OUT_OTHERS_NONE";
    protected static final String EXPENSE_TYPE_TAX_ALL_NO_DEDUCT_NO_OUT = "ALL_NO_DEDUCT_NO_OUT";
    protected static final String EXPENSE_TYPE_TAX_HAS_DEDUCT_NO_DEDUCT_NEED_OUT = "HAS_DEDUCT_NO_DEDUCT_NEED_OUT";
    protected static final String EXPENSE_TYPE_TAX_SEPARATE = "SEPARATE";
    protected static final String EXPENSE_TYPE_TAX_NOT_SEPARATE = "NOT_SEPARATE";

    protected static final Set<String> EXPENSE_TYPE_INVOICE_MODES = Set.of(
            EXPENSE_TYPE_INVOICE_FREE,
            EXPENSE_TYPE_INVOICE_REQUIRED
    );
    protected static final Set<String> EXPENSE_TYPE_TAX_MODES = Set.of(
            EXPENSE_TYPE_TAX_DEFAULT,
            EXPENSE_TYPE_TAX_SPECIAL_NO_DEDUCT_NEED_OUT,
            EXPENSE_TYPE_TAX_SPECIAL_NO_DEDUCT_NEED_OUT_OTHERS_NONE,
            EXPENSE_TYPE_TAX_ALL_NO_DEDUCT_NO_OUT,
            EXPENSE_TYPE_TAX_HAS_DEDUCT_NO_DEDUCT_NEED_OUT
    );
    protected static final Set<String> EXPENSE_TYPE_SEPARATION_MODES = Set.of(
            EXPENSE_TYPE_TAX_SEPARATE,
            EXPENSE_TYPE_TAX_NOT_SEPARATE
    );

    protected static final List<String> OPERATOR_KEYS = List.of(
            "EQ", "NE", "IN", "NOT_IN", "GT", "GE", "LT", "LE", "BETWEEN", "CONTAINS"
    );

    protected static final Map<String, String> OPERATOR_LABELS = Map.ofEntries(
            Map.entry("EQ", "\u7b49\u4e8e"),
            Map.entry("NE", "\u4e0d\u7b49\u4e8e"),
            Map.entry("IN", "\u5c5e\u4e8e"),
            Map.entry("NOT_IN", "\u4e0d\u5c5e\u4e8e"),
            Map.entry("GT", "\u5927\u4e8e"),
            Map.entry("GE", "\u5927\u4e8e\u7b49\u4e8e"),
            Map.entry("LT", "\u5c0f\u4e8e"),
            Map.entry("LE", "\u5c0f\u4e8e\u7b49\u4e8e"),
            Map.entry("BETWEEN", "\u4ecb\u4e8e"),
            Map.entry("CONTAINS", "\u5305\u542b")
    );

    protected static final List<RuleFieldDefinition> RULE_FIELD_DEFINITIONS = List.of(
            new RuleFieldDefinition("submitterDeptId", "\u63d0\u5355\u4eba\u90e8\u95e8", FIELD_VALUE_TYPE_DEPARTMENT, List.of("EQ", "NE", "IN", "NOT_IN")),
            new RuleFieldDefinition("submitterPosition", "\u63d0\u5355\u4eba\u5c97\u4f4d", FIELD_VALUE_TYPE_TEXT, List.of("EQ", "NE", "IN", "NOT_IN", "CONTAINS")),
            new RuleFieldDefinition("laborRelationBelong", "\u52b3\u52a8\u5173\u7cfb\u5f52\u5c5e", FIELD_VALUE_TYPE_TEXT, List.of("EQ", "NE", "IN", "NOT_IN", "CONTAINS")),
            new RuleFieldDefinition("documentType", "\u5355\u636e\u7c7b\u578b", FIELD_VALUE_TYPE_TEXT, List.of("EQ", "NE", "IN", "NOT_IN")),
            new RuleFieldDefinition("amount", "\u91d1\u989d", FIELD_VALUE_TYPE_NUMBER, List.of("EQ", "NE", "GT", "GE", "LT", "LE", "BETWEEN"))
    );

    protected static final Map<String, RuleFieldDefinition> RULE_FIELD_MAP = RULE_FIELD_DEFINITIONS.stream()
            .collect(Collectors.toMap(RuleFieldDefinition::key, Function.identity()));

    private final ProcessTemplateCategoryMapper categoryMapper;
    private final ProcessDocumentTemplateMapper templateMapper;
    private final CodeSequenceMapper codeSequenceMapper;
    private final ProcessTemplateScopeMapper scopeMapper;
    private final ProcessCustomArchiveDesignMapper customArchiveDesignMapper;
    private final ProcessCustomArchiveItemMapper customArchiveItemMapper;
    private final ProcessCustomArchiveRuleMapper customArchiveRuleMapper;
    private final ProcessExpenseTypeMapper processExpenseTypeMapper;
    private final SystemDepartmentMapper systemDepartmentMapper;
    private final UserMapper userMapper;
    private final ProcessFormDesignService processFormDesignService;
    private final ProcessExpenseDetailDesignService processExpenseDetailDesignService;
    private final ProcessFlowDesignService processFlowDesignService;
    private final ObjectMapper objectMapper;
    private ProcessCenterMetaSupport processCenterMetaSupport;
    private ProcessTemplateLifecycleSupport processTemplateLifecycleSupport;
    private ProcessCustomArchiveLifecycleSupport processCustomArchiveLifecycleSupport;
    private ProcessExpenseTypeLifecycleSupport processExpenseTypeLifecycleSupport;
    /**
     * 闁告梻濮惧ù鍢峞partment闂侇偄顦甸妴宥夊Υ?
     */
    protected List<ProcessFormOptionVO> loadDepartmentOptions() {
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .select(SystemDepartment::getId, SystemDepartment::getDeptCode, SystemDepartment::getDeptName, SystemDepartment::getParentId)
                        .eq(SystemDepartment::getStatus, 1)
                        .orderByAsc(SystemDepartment::getSortOrder, SystemDepartment::getId)
        ).stream().map(this::toDepartmentOption).toList();
    }

    protected ProcessFormOptionVO toDepartmentOption(SystemDepartment department) {
        ProcessFormOptionVO option = new ProcessFormOptionVO();
        option.setValue(String.valueOf(department.getId()));
        option.setCode(department.getDeptCode());
        option.setName(department.getDeptName());
        option.setParentValue(department.getParentId() == null ? null : String.valueOf(department.getParentId()));
        option.setLabel(formatDepartmentLabel(department.getDeptCode(), department.getDeptName(), option.getValue()));
        return option;
    }

    /**
     * 闁告梻濮惧ù鍥偨閵婏箑鐓曢梺顐㈩樀閵嗗秹濡?
     */
    protected List<ProcessFormOptionVO> loadUserOptions() {
        return userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getStatus, 1)
                        .orderByAsc(User::getId)
        ).stream().map(user -> {
            String label = trimToNull(user.getName()) != null ? user.getName() : normalize(user.getUsername(), "\u672a\u547d\u540d\u7528\u6237");
            if (trimToNull(user.getUsername()) != null && !Objects.equals(label, user.getUsername())) {
                label = label + " (" + user.getUsername() + ")";
            }
            return option(label, String.valueOf(user.getId()));
        }).toList();
    }

    /**
     * 闁告梻濮惧ù鍢╝lidDepartmentIdSet闁?
     */
    protected Set<String> loadValidDepartmentIdSet() {
        return systemDepartmentMapper.selectList(
                Wrappers.<SystemDepartment>lambdaQuery()
                        .eq(SystemDepartment::getStatus, 1)
                        .select(SystemDepartment::getId)
        ).stream().map(item -> String.valueOf(item.getId())).collect(Collectors.toSet());
    }

    /**
     * 闁告梻濮惧ù鍢╝lid闁活潿鍔嶉崺姹璬Set闁?
     */
    protected Set<String> loadValidUserIdSet() {
        return userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getStatus, 1)
                        .select(User::getId)
        ).stream().map(item -> String.valueOf(item.getId())).collect(Collectors.toSet());
    }

    /**
     * 闁告梻濮惧ù鍢╝lid闁硅翰鍎甸弨銏ゅ础閺囩姾顫﹂柛銊ヮ儑缁鳖亪鎯嶆稉绯磘闁?
     */
    protected Set<String> loadValidExpenseTypeCodeSet() {
        return processExpenseTypeMapper.selectList(
                Wrappers.<ProcessExpenseType>lambdaQuery()
                        .eq(ProcessExpenseType::getStatus, 1)
                        .orderByAsc(ProcessExpenseType::getExpenseCode, ProcessExpenseType::getId)
        ).stream()
                .map(ProcessExpenseType::getExpenseCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 闁哄稄绻濋悰姗瞖lectableIds闁?
     */
    protected void validateSelectableIds(List<String> selectedIds, Set<String> validIds, String fieldName) {
        for (String selectedId : selectedIds) {
            if (!validIds.contains(selectedId)) {
                throw new IllegalArgumentException(fieldName + " \u9009\u62e9\u9879\u4e0d\u5b58\u5728: " + selectedId);
            }
        }
    }

    protected List<String> normalizeIdList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String normalizedValue = trimToNull(value);
            if (normalizedValue != null) {
                result.add(normalizedValue);
            }
        }
        return new ArrayList<>(result);
    }

    protected String serializeStringList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(normalizeIdList(values));
        } catch (Exception ex) {
            throw new IllegalStateException("\u5e8f\u5217\u5316\u8303\u56f4\u6570\u636e\u5931\u8d25", ex);
        }
    }

    protected List<String> deserializeStringList(String json) {
        if (trimToNull(json) == null) {
            return Collections.emptyList();
        }
        try {
            List<String> values = objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
            return normalizeIdList(values);
        } catch (Exception ex) {
            throw new IllegalStateException("\u53cd\u5e8f\u5217\u5316\u8303\u56f4\u6570\u636e\u5931\u8d25", ex);
        }
    }

    protected Integer normalizeStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        return status == 1 ? 1 : 0;
    }

    protected String normalizeComparable(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    protected BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(String.valueOf(number));
        }
        String normalizedValue = trimToNull(String.valueOf(value));
        if (normalizedValue == null) {
            return null;
        }
        try {
            return new BigDecimal(normalizedValue);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    protected String normalize(String value, String defaultValue) {
        String normalizedValue = trimToNull(value);
        return normalizedValue == null ? defaultValue : normalizedValue;
    }

    protected String resolveTemplateStatusLabel(String status) {
        if (TEMPLATE_STATUS_ENABLED.equals(status)) {
            return "\u5df2\u542f\u7528";
        }
        if (TEMPLATE_STATUS_DRAFT.equals(status)) {
            return "\u8349\u7a3f";
        }
        if (TEMPLATE_STATUS_DELETED.equals(status)) {
            return "\u5df2\u5220\u9664";
        }
        return "\u8349\u7a3f";
    }
    protected void validatePmNameLength(String value, String label) {
        String normalized = trimToNull(value);
        if (normalized != null && normalized.length() > PM_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(label + "\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26");
        }
    }

    protected void validateFieldKeyLength(String value, String label) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(label + "\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (normalized.length() > PM_FIELD_KEY_MAX_LENGTH) {
            throw new IllegalArgumentException(label + "\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 64 \u4e2a\u5b57\u7b26");
        }
    }

    protected String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    protected String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProcessCenterMetaSupport processCenterMetaSupport() {
        if (processCenterMetaSupport == null) {
            processCenterMetaSupport = new ProcessCenterMetaSupport(
                    categoryMapper,
                    templateMapper,
                    customArchiveDesignMapper,
                    processExpenseTypeMapper,
                    systemDepartmentMapper,
                    processFormDesignService,
                    processExpenseDetailDesignService,
                    processFlowDesignService
            );
        }
        return processCenterMetaSupport;
    }
    private ProcessTemplateLifecycleSupport processTemplateLifecycleSupport() {
        if (processTemplateLifecycleSupport == null) {
            processTemplateLifecycleSupport = new ProcessTemplateLifecycleSupport(
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
        return processTemplateLifecycleSupport;
    }

    private ProcessCustomArchiveLifecycleSupport processCustomArchiveLifecycleSupport() {
        if (processCustomArchiveLifecycleSupport == null) {
            processCustomArchiveLifecycleSupport = new ProcessCustomArchiveLifecycleSupport(
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
        return processCustomArchiveLifecycleSupport;
    }

    private ProcessExpenseTypeLifecycleSupport processExpenseTypeLifecycleSupport() {
        if (processExpenseTypeLifecycleSupport == null) {
            processExpenseTypeLifecycleSupport = new ProcessExpenseTypeLifecycleSupport(
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
        return processExpenseTypeLifecycleSupport;
    }

    protected ProcessCenterMetaSupport getProcessCenterMetaSupport() {
        return processCenterMetaSupport();
    }
    protected ProcessTemplateLifecycleSupport getProcessTemplateLifecycleSupport() {
        return processTemplateLifecycleSupport();
    }

    protected ProcessCustomArchiveLifecycleSupport getProcessCustomArchiveLifecycleSupport() {
        return processCustomArchiveLifecycleSupport();
    }

    protected ProcessExpenseTypeLifecycleSupport getProcessExpenseTypeLifecycleSupport() {
        return processExpenseTypeLifecycleSupport();
    }

    protected ProcessTemplateScopeMapper getScopeMapper() {
        return scopeMapper;
    }

    protected ProcessDocumentTemplateMapper getTemplateMapper() {
        return templateMapper;
    }

    protected CodeSequenceMapper getCodeSequenceMapper() {
        return codeSequenceMapper;
    }

    protected ProcessCustomArchiveDesignMapper getCustomArchiveDesignMapper() {
        return customArchiveDesignMapper;
    }

    protected ProcessCustomArchiveItemMapper getCustomArchiveItemMapper() {
        return customArchiveItemMapper;
    }

    protected ProcessCustomArchiveRuleMapper getCustomArchiveRuleMapper() {
        return customArchiveRuleMapper;
    }

    protected ProcessExpenseTypeMapper getProcessExpenseTypeMapper() {
        return processExpenseTypeMapper;
    }

    protected SystemDepartmentMapper getSystemDepartmentMapper() {
        return systemDepartmentMapper;
    }

    protected ProcessFormDesignService getProcessFormDesignService() {
        return processFormDesignService;
    }

    protected ProcessExpenseDetailDesignService getProcessExpenseDetailDesignService() {
        return processExpenseDetailDesignService;
    }

    protected ProcessFlowDesignService getProcessFlowDesignService() {
        return processFlowDesignService;
    }

    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    protected String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME_FORMATTER.format(dateTime);
    }

    protected String currentTemplateSaveTraceId() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return "no-trace-id";
        }
        HttpServletRequest request = attributes.getRequest();
        Object traceId = request.getAttribute(TemplateSaveTraceInterceptor.TRACE_ATTRIBUTE);
        if (traceId instanceof String value && !value.isBlank()) {
            return value;
        }
        String traceHeader = request.getHeader(TemplateSaveTraceInterceptor.TRACE_HEADER);
        if (traceHeader != null && !traceHeader.isBlank()) {
            return traceHeader.trim();
        }
        return "no-trace-id";
    }

    protected void logTemplateSaveStage(String traceId, String action, String stage, long startedAt) {
        log.info(
                "[TemplateSaveTrace][{}][service] {} {} costMs={}",
                traceId,
                action,
                stage,
                elapsedMillis(startedAt)
        );
    }

    protected <T> T traceTemplateSaveValueStep(String traceId, String action, String stage, Supplier<T> supplier) {
        log.info("[TemplateSaveTrace][{}][service] {} {} start", traceId, action, stage);
        long startedAt = System.nanoTime();
        try {
            T result = supplier.get();
            log.info(
                    "[TemplateSaveTrace][{}][service] {} {} costMs={} result={}",
                    traceId,
                    action,
                    stage,
                    elapsedMillis(startedAt),
                    summarizeTemplateSaveTraceValue(result)
            );
            return result;
        } catch (RuntimeException ex) {
            log.error(
                    "[TemplateSaveTrace][{}][service] {} {} failed after {}ms: {}",
                    traceId,
                    action,
                    stage,
                    elapsedMillis(startedAt),
                    ex.getMessage(),
                    ex
            );
            throw ex;
        }
    }

    private String summarizeTemplateSaveTraceValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof CharSequence text) {
            String normalized = text.toString().replace('\n', ' ').replace('\r', ' ');
            if (normalized.length() > 120) {
                return normalized.substring(0, 120) + "...";
            }
            return normalized;
        }
        if (value instanceof Collection<?> collection) {
            return "Collection(size=" + collection.size() + ")";
        }
        return String.valueOf(value);
    }

    protected long elapsedMillis(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }

    private ProcessCenterNavItemVO navItem(String key, String label, String tip) {
        ProcessCenterNavItemVO item = new ProcessCenterNavItemVO();
        item.setKey(key);
        item.setLabel(label);
        item.setTip(tip);
        return item;
    }

    private ProcessTemplateTypeVO templateType(String code, String name, String subtitle, String description, String accent) {
        ProcessTemplateTypeVO type = new ProcessTemplateTypeVO();
        type.setCode(code);
        type.setName(name);
        type.setSubtitle(subtitle);
        type.setDescription(description);
        type.setAccent(accent);
        return type;
    }

    protected ProcessFormOptionVO option(String label, String value) {
        ProcessFormOptionVO option = new ProcessFormOptionVO();
        option.setLabel(label);
        option.setValue(value);
        return option;
    }

    protected String formatDepartmentLabel(String code, String name, String fallback) {
        String normalizedCode = trimToNull(code);
        String normalizedName = trimToNull(name);
        if (normalizedCode != null && normalizedName != null) {
            return normalizedCode + "  " + normalizedName;
        }
        return normalizedName != null ? normalizedName : normalizedCode != null ? normalizedCode : fallback;
    }

    protected record RuleFieldDefinition(
            String key,
            String label,
            String valueType,
            List<String> operatorKeys
    ) {
    }
}
