package com.finex.auth.service.impl.financesystem;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.entity.FaAssetCard;
import com.finex.auth.entity.FaAssetCategory;
import com.finex.auth.entity.FaAssetChangeBill;
import com.finex.auth.entity.FaAssetChangeLine;
import com.finex.auth.entity.FaAssetDeprLine;
import com.finex.auth.entity.FaAssetDeprRun;
import com.finex.auth.entity.FaAssetDisposalBill;
import com.finex.auth.entity.FaAssetDisposalLine;
import com.finex.auth.entity.FaAssetOpeningImport;
import com.finex.auth.entity.FaAssetOpeningImportLine;
import com.finex.auth.entity.FaAssetPeriodClose;
import com.finex.auth.entity.FaAssetVoucherLink;
import com.finex.auth.entity.FinanceOpeningBalanceState;
import com.finex.auth.entity.FinancePeriodClose;
import com.finex.auth.entity.FinancePeriodCloseLog;
import com.finex.auth.entity.FinancePostVoucherState;
import com.finex.auth.entity.GlAccass;
import com.finex.auth.entity.GlAccsum;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.mapper.FaAssetCardMapper;
import com.finex.auth.mapper.FaAssetCategoryMapper;
import com.finex.auth.mapper.FaAssetChangeBillMapper;
import com.finex.auth.mapper.FaAssetChangeLineMapper;
import com.finex.auth.mapper.FaAssetDeprLineMapper;
import com.finex.auth.mapper.FaAssetDeprRunMapper;
import com.finex.auth.mapper.FaAssetDisposalBillMapper;
import com.finex.auth.mapper.FaAssetDisposalLineMapper;
import com.finex.auth.mapper.FaAssetOpeningImportLineMapper;
import com.finex.auth.mapper.FaAssetOpeningImportMapper;
import com.finex.auth.mapper.FaAssetPeriodCloseMapper;
import com.finex.auth.mapper.FaAssetVoucherLinkMapper;
import com.finex.auth.mapper.FinanceOpeningBalanceStateMapper;
import com.finex.auth.mapper.FinancePeriodCloseLogMapper;
import com.finex.auth.mapper.FinancePeriodCloseMapper;
import com.finex.auth.mapper.FinancePostVoucherStateMapper;
import com.finex.auth.mapper.GlAccassMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.support.FinanceModuleEnableSupport;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class FinanceModuleDataSupport {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Map<String, List<TableBinding>> backupBindingsByModule;
    private final Map<String, List<TableBinding>> clearBindingsByModule;

    public FinanceModuleDataSupport(
            GlAccvouchMapper glAccvouchMapper,
            GlAccsumMapper glAccsumMapper,
            GlAccassMapper glAccassMapper,
            FinanceOpeningBalanceStateMapper financeOpeningBalanceStateMapper,
            FinancePostVoucherStateMapper financePostVoucherStateMapper,
            FinancePeriodCloseMapper financePeriodCloseMapper,
            FinancePeriodCloseLogMapper financePeriodCloseLogMapper,
            FaAssetCategoryMapper faAssetCategoryMapper,
            FaAssetCardMapper faAssetCardMapper,
            FaAssetChangeBillMapper faAssetChangeBillMapper,
            FaAssetChangeLineMapper faAssetChangeLineMapper,
            FaAssetDeprRunMapper faAssetDeprRunMapper,
            FaAssetDeprLineMapper faAssetDeprLineMapper,
            FaAssetDisposalBillMapper faAssetDisposalBillMapper,
            FaAssetDisposalLineMapper faAssetDisposalLineMapper,
            FaAssetOpeningImportMapper faAssetOpeningImportMapper,
            FaAssetOpeningImportLineMapper faAssetOpeningImportLineMapper,
            FaAssetPeriodCloseMapper faAssetPeriodCloseMapper,
            FaAssetVoucherLinkMapper faAssetVoucherLinkMapper
    ) {
        List<TableBinding> generalLedgerBindings = List.of(
                binding("gl_accvouch", GlAccvouch.class, glAccvouchMapper),
                binding("gl_accsum", GlAccsum.class, glAccsumMapper),
                binding("gl_accass", GlAccass.class, glAccassMapper),
                binding("gl_opening_balance_state", FinanceOpeningBalanceState.class, financeOpeningBalanceStateMapper),
                binding("gl_post_state", FinancePostVoucherState.class, financePostVoucherStateMapper),
                binding("gl_period_close", FinancePeriodClose.class, financePeriodCloseMapper),
                binding("gl_period_close_log", FinancePeriodCloseLog.class, financePeriodCloseLogMapper)
        );
        List<TableBinding> fixedAssetBindings = List.of(
                binding("fa_asset_category", FaAssetCategory.class, faAssetCategoryMapper),
                binding("fa_asset_card", FaAssetCard.class, faAssetCardMapper),
                binding("fa_asset_change_bill", FaAssetChangeBill.class, faAssetChangeBillMapper),
                binding("fa_asset_change_line", FaAssetChangeLine.class, faAssetChangeLineMapper),
                binding("fa_asset_depr_run", FaAssetDeprRun.class, faAssetDeprRunMapper),
                binding("fa_asset_depr_line", FaAssetDeprLine.class, faAssetDeprLineMapper),
                binding("fa_asset_disposal_bill", FaAssetDisposalBill.class, faAssetDisposalBillMapper),
                binding("fa_asset_disposal_line", FaAssetDisposalLine.class, faAssetDisposalLineMapper),
                binding("fa_asset_opening_import", FaAssetOpeningImport.class, faAssetOpeningImportMapper),
                binding("fa_asset_opening_import_line", FaAssetOpeningImportLine.class, faAssetOpeningImportLineMapper),
                binding("fa_asset_period_close", FaAssetPeriodClose.class, faAssetPeriodCloseMapper),
                binding("fa_asset_voucher_link", FaAssetVoucherLink.class, faAssetVoucherLinkMapper)
        );

        Map<String, List<TableBinding>> backupBindings = new LinkedHashMap<>();
        backupBindings.put(FinanceModuleEnableSupport.GENERAL_LEDGER, generalLedgerBindings);
        backupBindings.put(FinanceModuleEnableSupport.FIXED_ASSETS, fixedAssetBindings);
        this.backupBindingsByModule = Collections.unmodifiableMap(backupBindings);

        Map<String, List<TableBinding>> clearBindings = new LinkedHashMap<>();
        clearBindings.put(FinanceModuleEnableSupport.FIXED_ASSETS, List.of(
                binding("fa_asset_voucher_link", FaAssetVoucherLink.class, faAssetVoucherLinkMapper),
                binding("fa_asset_depr_line", FaAssetDeprLine.class, faAssetDeprLineMapper),
                binding("fa_asset_depr_run", FaAssetDeprRun.class, faAssetDeprRunMapper),
                binding("fa_asset_disposal_line", FaAssetDisposalLine.class, faAssetDisposalLineMapper),
                binding("fa_asset_disposal_bill", FaAssetDisposalBill.class, faAssetDisposalBillMapper),
                binding("fa_asset_change_line", FaAssetChangeLine.class, faAssetChangeLineMapper),
                binding("fa_asset_change_bill", FaAssetChangeBill.class, faAssetChangeBillMapper),
                binding("fa_asset_opening_import_line", FaAssetOpeningImportLine.class, faAssetOpeningImportLineMapper),
                binding("fa_asset_opening_import", FaAssetOpeningImport.class, faAssetOpeningImportMapper),
                binding("fa_asset_period_close", FaAssetPeriodClose.class, faAssetPeriodCloseMapper),
                binding("fa_asset_card", FaAssetCard.class, faAssetCardMapper),
                binding("fa_asset_category", FaAssetCategory.class, faAssetCategoryMapper)
        ));
        this.clearBindingsByModule = Collections.unmodifiableMap(clearBindings);
    }

    public boolean hasModuleData(String companyId, String moduleCode) {
        return bindingsForBackup(moduleCode).stream().anyMatch(item -> item.hasRows(companyId));
    }

    public List<TableBinding> bindingsForBackup(String moduleCode) {
        return backupBindingsByModule.getOrDefault(moduleCode, List.of());
    }

    public List<TableBinding> bindingsForClear(String moduleCode) {
        return clearBindingsByModule.getOrDefault(moduleCode, List.of());
    }

    public String buildBackupSql(String companyId, String moduleCode) {
        List<TableBinding> bindings = bindingsForBackup(moduleCode);
        if (bindings.isEmpty()) {
            throw new IllegalStateException("当前模块尚未开放数据备份");
        }
        StringBuilder builder = new StringBuilder(2048);
        builder.append("USE finex_db;\n\n");
        builder.append("SET NAMES utf8mb4;\n\n");
        builder.append("-- 公司主体: ").append(companyId).append('\n');
        builder.append("-- 模块编码: ").append(moduleCode).append("\n\n");
        builder.append("-- 恢复前清理当前公司该模块旧数据\n");
        for (TableBinding binding : reversed(bindingsForClearOrBackupCleanup(moduleCode))) {
            builder.append("DELETE FROM ").append(binding.tableName).append(" WHERE company_id = '")
                    .append(escapeSqlText(companyId)).append("';\n");
        }
        builder.append('\n');
        for (TableBinding binding : bindings) {
            appendTableBackup(builder, binding, companyId);
        }
        return builder.toString();
    }

    public void clearModuleData(String companyId, String moduleCode) {
        List<TableBinding> bindings = bindingsForClear(moduleCode);
        if (bindings.isEmpty()) {
            throw new IllegalStateException("当前模块尚未开放数据清理");
        }
        for (TableBinding binding : bindings) {
            binding.deleteByCompany(companyId);
        }
    }

    public String resolveClearBlockedMessage(String moduleCode) {
        if (Objects.equals(moduleCode, FinanceModuleEnableSupport.GENERAL_LEDGER)) {
            return "总账不支持清除数据";
        }
        if (Objects.equals(moduleCode, FinanceModuleEnableSupport.FIXED_ASSETS)) {
            return "请先备份再清理";
        }
        return "当前模块尚未开放数据清理";
    }

    private List<TableBinding> bindingsForClearOrBackupCleanup(String moduleCode) {
        List<TableBinding> clearBindings = bindingsForClear(moduleCode);
        if (!clearBindings.isEmpty()) {
            return clearBindings;
        }
        return bindingsForBackup(moduleCode);
    }

    private void appendTableBackup(StringBuilder builder, TableBinding binding, String companyId) {
        List<?> rows = binding.selectByCompany(companyId);
        builder.append("-- 表: ").append(binding.tableName).append('\n');
        if (rows.isEmpty()) {
            builder.append("-- 无数据\n\n");
            return;
        }
        for (Object row : rows) {
            builder.append("INSERT INTO ").append(binding.tableName)
                    .append(" (").append(String.join(", ", binding.columnNames)).append(") VALUES (")
                    .append(binding.toInsertValues(row)).append(");\n");
        }
        builder.append('\n');
    }

    private List<TableBinding> reversed(List<TableBinding> bindings) {
        List<TableBinding> reversed = new ArrayList<>(bindings);
        Collections.reverse(reversed);
        return reversed;
    }

    @SuppressWarnings("rawtypes")
    private TableBinding binding(String tableName, Class<?> entityClass, BaseMapper mapper) {
        return new TableBinding(tableName, entityClass, mapper, resolveColumns(entityClass));
    }

    private List<ColumnBinding> resolveColumns(Class<?> entityClass) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = entityClass;
        while (current != null && current != Object.class) {
            fields.addAll(List.of(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields.stream()
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .filter(field -> !field.isSynthetic())
                .filter(field -> shouldPersist(field))
                .map(field -> new ColumnBinding(resolveColumnName(field), field))
                .collect(Collectors.toList());
    }

    private boolean shouldPersist(Field field) {
        TableField tableField = field.getAnnotation(TableField.class);
        return tableField == null || tableField.exist();
    }

    private String resolveColumnName(Field field) {
        TableId tableId = field.getAnnotation(TableId.class);
        if (tableId != null && tableId.value() != null && !tableId.value().isBlank()) {
            return tableId.value();
        }
        TableField tableField = field.getAnnotation(TableField.class);
        if (tableField != null && tableField.value() != null && !tableField.value().isBlank()) {
            return tableField.value();
        }
        return camelToSnake(field.getName());
    }

    private String camelToSnake(String value) {
        StringBuilder builder = new StringBuilder(value.length() + 4);
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isUpperCase(current)) {
                if (index > 0) {
                    builder.append('_');
                }
                builder.append(Character.toLowerCase(current));
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    private String escapeSqlText(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("'", "''")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private String formatSqlValue(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String text) {
            return "'" + escapeSqlText(text) + "'";
        }
        if (value instanceof BigDecimal number) {
            return number.toPlainString();
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof LocalDateTime dateTime) {
            return "'" + DATE_TIME_FORMATTER.format(dateTime) + "'";
        }
        if (value instanceof LocalDate date) {
            return "'" + DATE_FORMATTER.format(date) + "'";
        }
        if (value instanceof Enum<?> enumValue) {
            return "'" + escapeSqlText(enumValue.name()) + "'";
        }
        return "'" + escapeSqlText(String.valueOf(value)) + "'";
    }

    public final class TableBinding {

        private final String tableName;
        private final Class<?> entityClass;
        @SuppressWarnings("rawtypes")
        private final BaseMapper mapper;
        private final List<ColumnBinding> columns;
        private final List<String> columnNames;

        @SuppressWarnings("rawtypes")
        private TableBinding(String tableName, Class<?> entityClass, BaseMapper mapper, List<ColumnBinding> columns) {
            this.tableName = tableName;
            this.entityClass = entityClass;
            this.mapper = mapper;
            this.columns = columns;
            this.columnNames = columns.stream().map(item -> item.columnName).toList();
        }

        public boolean hasRows(String companyId) {
            Number result = mapper.selectCount(Wrappers.query().eq("company_id", companyId));
            return result != null && result.longValue() > 0L;
        }

        public List<?> selectByCompany(String companyId) {
            String keyColumn = resolveKeyColumn();
            var query = Wrappers.query().eq("company_id", companyId);
            if (keyColumn != null) {
                query.orderByAsc(keyColumn);
            }
            return mapper.selectList(query);
        }

        public void deleteByCompany(String companyId) {
            mapper.delete(Wrappers.query().eq("company_id", companyId));
        }

        private String resolveKeyColumn() {
            return columns.stream()
                    .filter(item -> item.field.isAnnotationPresent(TableId.class))
                    .map(item -> item.columnName)
                    .findFirst()
                    .orElse(null);
        }

        private String toInsertValues(Object row) {
            return columns.stream()
                    .map(item -> formatSqlValue(item.readValue(row)))
                    .collect(Collectors.joining(", "));
        }
    }

    private static final class ColumnBinding {

        private final String columnName;
        private final Field field;

        private ColumnBinding(String columnName, Field field) {
            this.columnName = columnName;
            this.field = field;
            this.field.setAccessible(true);
        }

        private Object readValue(Object row) {
            try {
                return field.get(row);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("读取模块备份字段失败: " + field.getName(), ex);
            }
        }
    }
}
