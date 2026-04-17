package com.finex.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.finex.auth.dto.FinanceVoucherActionResultVO;
import com.finex.auth.dto.FinanceVoucherMetaVO;
import com.finex.auth.dto.FinanceVoucherQueryDTO;
import com.finex.auth.dto.FinanceVoucherSaveDTO;
import com.finex.auth.dto.FinanceVoucherSaveResultVO;
import com.finex.auth.dto.FinanceVoucherSummaryVO;
import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.FinanceCustomer;
import com.finex.auth.entity.FinanceProjectArchive;
import com.finex.auth.entity.FinanceProjectClass;
import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.SystemDepartment;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinanceCustomerMapper;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceVoucherServiceImplTest {

    @Mock
    private GlAccvouchMapper glAccvouchMapper;

    @Mock
    private FinanceAccountSubjectMapper financeAccountSubjectMapper;

    @Mock
    private FinanceCustomerMapper financeCustomerMapper;

    @Mock
    private FinanceVendorMapper financeVendorMapper;

    @Mock
    private FinanceProjectClassMapper financeProjectClassMapper;

    @Mock
    private FinanceProjectArchiveMapper financeProjectArchiveMapper;

    @Mock
    private SystemCompanyMapper systemCompanyMapper;

    @Mock
    private SystemDepartmentMapper systemDepartmentMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private FinanceAccountSetMapper financeAccountSetMapper;

    @Mock
    private UserService userService;

    private FinanceVoucherServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), GlAccvouch.class);

        service = new FinanceVoucherServiceImpl(
                glAccvouchMapper,
                financeAccountSubjectMapper,
                financeCustomerMapper,
                financeVendorMapper,
                financeProjectClassMapper,
                financeProjectArchiveMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                userMapper,
                financeAccountSetMapper,
                userService
        );

        lenient().when(systemDepartmentMapper.selectList(any())).thenReturn(List.of());
        lenient().when(userMapper.selectList(any())).thenReturn(List.of());
        lenient().when(financeProjectClassMapper.selectList(any())).thenReturn(List.of());
        lenient().when(financeProjectArchiveMapper.selectList(any())).thenReturn(List.of());
        lenient().when(financeCustomerMapper.selectList(any())).thenReturn(List.of());
        lenient().when(financeVendorMapper.selectList(any())).thenReturn(List.of());
        lenient().when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(buildSubject("5601", "绠＄悊璐圭敤"), buildSubject("1002", "閾惰瀛樻")));
        lenient().when(glAccvouchMapper.selectObjs(any())).thenReturn(List.of());
    }

    @Test
    void queryVouchersBuildsVoucherHeadSummary() {
        when(systemCompanyMapper.selectCount(any())).thenReturn(1L);
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(
                buildRow(1, "COMP-001", 3, "\u8bb0", 8, 1, "2026-03-28", "\u529e\u516c\u8d39", "5601", new BigDecimal("1280.00"), BigDecimal.ZERO),
                buildRow(2, "COMP-001", 3, "\u8bb0", 8, 2, "2026-03-28", "\u652f\u4ed8\u529e\u516c\u8d39", "1002", BigDecimal.ZERO, new BigDecimal("1280.00"))
        ));

        FinanceVoucherQueryDTO dto = new FinanceVoucherQueryDTO();
        dto.setCompanyId("COMP-001");
        dto.setPage(1);
        dto.setPageSize(20);

        FinanceVoucherSummaryVO summary = service.queryVouchers(dto).getItems().get(0);
        assertEquals("\u8bb0-0008", summary.getDisplayVoucherNo());
        assertEquals("1280.00", summary.getTotalDebit().toPlainString());
        assertEquals("1280.00", summary.getTotalCredit().toPlainString());
    }

    @Test
    void getMetaLoadsCustomerSupplierAndProjectOptionsFromArchives() {
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "alice", "\u8d22\u52a1\u5c0f\u738b", "COMP-001"));
        when(userService.getById(1L)).thenReturn(buildUser(1L, "alice", "\u8d22\u52a1\u5c0f\u738b", "COMP-001"));
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(buildCompany("COMP-001", "001", "Guangzhou Branch")));
        when(userMapper.selectList(any())).thenReturn(List.of(buildUser(2L, "bob", "\u5458\u5de5\u7532", "COMP-001")));
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(buildSubject("1001", "\u5e93\u5b58\u73b0\u91d1", 1, 1, 1, 1, 1, "7")));
        when(financeCustomerMapper.selectList(any())).thenReturn(List.of(buildCustomer("C00001", "\u534e\u5357\u5ba2\u6237", "COMP-001")));
        when(financeVendorMapper.selectList(any())).thenReturn(List.of(buildVendor("V00001", "Core Supplier", "COMP-001")));
        when(financeProjectClassMapper.selectList(any())).thenReturn(List.of(buildProjectClass("7", "Market Projects", "COMP-001")));
        when(financeProjectArchiveMapper.selectList(any())).thenReturn(List.of(buildProject("2002", "South Campaign", "7", "COMP-001")));
        when(glAccvouchMapper.selectObjs(any())).thenReturn(List.of());

        FinanceVoucherMetaVO meta = service.getMeta(1L, "alice", "COMP-001", "2026-04-09", "\u8bb0");

        assertEquals("C00001", meta.getCustomerOptions().get(0).getValue());
        assertEquals("C00001", meta.getCustomerOptions().get(0).getCode());
        assertEquals("\u534e\u5357\u5ba2\u6237", meta.getCustomerOptions().get(0).getName());
        assertEquals(1, meta.getAccountOptions().get(0).getBperson());
        assertEquals(1, meta.getAccountOptions().get(0).getBdept());
        assertEquals(1, meta.getAccountOptions().get(0).getBitem());
        assertEquals("7", meta.getAccountOptions().get(0).getCassItem());
        assertEquals("V00001", meta.getSupplierOptions().get(0).getValue());
        assertEquals("7", meta.getProjectClassOptions().get(0).getCode());
        assertEquals("2002", meta.getProjectOptions().get(0).getCode());
        assertEquals("7", meta.getProjectOptions().get(0).getParentValue());
    }

    @Test
    void saveVoucherAcceptsVariableLengthProjectCodes() {
        List<GlAccvouch> insertedRows = new ArrayList<>();
        doAnswer(invocation -> {
            insertedRows.add(invocation.getArgument(0));
            return 1;
        }).when(glAccvouchMapper).insert(any(GlAccvouch.class));

        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "alice", "Finance Tester", "COMP-001"));
        when(systemCompanyMapper.selectCount(any())).thenReturn(1L);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("5601", "Management Expense", 0, 0, 0, 0, 1, null),
                buildSubject("1002", "Bank Deposit")
        ));
        when(financeProjectClassMapper.selectList(any())).thenReturn(List.of(buildProjectClass("7", "Market Projects", "COMP-001")));
        when(financeProjectArchiveMapper.selectList(any())).thenReturn(List.of(buildProject("2002", "South Campaign", "7", "COMP-001")));
        when(glAccvouchMapper.selectObjs(any())).thenReturn(List.of());

        FinanceVoucherSaveDTO dto = new FinanceVoucherSaveDTO();
        dto.setCompanyId("COMP-001");
        dto.setIperiod(4);
        dto.setCsign("\u8bb0");
        dto.setDbillDate("2026-04-09");
        dto.setEntries(List.of(
                buildSaveEntry("Office Expense", "5601", "100.00", null),
                buildSaveEntry("Pay Office Expense", "1002", null, "100.00")
        ));
        dto.getEntries().get(0).setCitemClass("7");
        dto.getEntries().get(0).setCitemId("2002");

        service.saveVoucher(dto, 1L, "alice");

        assertEquals(2, insertedRows.size());
        assertEquals("7", insertedRows.get(0).getCitemClass());
        assertEquals("2002", insertedRows.get(0).getCitemId());
    }

    @Test
    void saveVoucherRejectsDisabledAuxiliaryDimensionForSubject() {
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "alice", "Finance Tester", "COMP-001"));
        when(systemCompanyMapper.selectCount(any())).thenReturn(1L);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("5601", "Management Expense"),
                buildSubject("1002", "Bank Deposit")
        ));
        when(systemDepartmentMapper.selectList(any())).thenReturn(List.of(buildDepartment(10L, "Finance Center")));

        FinanceVoucherSaveDTO dto = new FinanceVoucherSaveDTO();
        dto.setCompanyId("COMP-001");
        dto.setIperiod(4);
        dto.setCsign("\u8bb0");
        dto.setDbillDate("2026-04-09");
        dto.setEntries(List.of(
                buildSaveEntry("Office Expense", "5601", "100.00", null),
                buildSaveEntry("Pay Office Expense", "1002", null, "100.00")
        ));
        dto.getEntries().get(0).setCdeptId("10");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.saveVoucher(dto, 1L, "alice")
        );

        assertEquals("\u7b2c 1 \u884c\u5f53\u524d\u79d1\u76ee\u672a\u542f\u7528\u90e8\u95e8\u8f85\u52a9\u6838\u7b97", exception.getMessage());
    }

    @Test
    void saveVoucherRejectsProjectClassOutsideSubjectCassItemBinding() {
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "alice", "Finance Tester", "COMP-001"));
        when(systemCompanyMapper.selectCount(any())).thenReturn(1L);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("5601", "Management Expense", 0, 0, 0, 0, 1, "7"),
                buildSubject("1002", "Bank Deposit")
        ));
        when(financeProjectClassMapper.selectList(any())).thenReturn(List.of(
                buildProjectClass("7", "Market Projects", "COMP-001"),
                buildProjectClass("8", "Innovation Projects", "COMP-001")
        ));

        FinanceVoucherSaveDTO dto = new FinanceVoucherSaveDTO();
        dto.setCompanyId("COMP-001");
        dto.setIperiod(4);
        dto.setCsign("\u8bb0");
        dto.setDbillDate("2026-04-09");
        dto.setEntries(List.of(
                buildSaveEntry("Office Expense", "5601", "100.00", null),
                buildSaveEntry("Pay Office Expense", "1002", null, "100.00")
        ));
        dto.getEntries().get(0).setCitemClass("8");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.saveVoucher(dto, 1L, "alice")
        );

        assertEquals("\u7b2c 1 \u884c\u9879\u76ee\u5206\u7c7b\u5fc5\u987b\u4e3a\u79d1\u76ee\u6302\u8f7d\u7684\u9879\u76ee\u5206\u7c7b\u30107\u3011", exception.getMessage());
    }

    @Test
    void getMetaLoadsSharedEmployeesAndDepartmentsAcrossCompanies() {
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "alice", "Finance Tester", "COMP-001"));
        when(userService.getById(1L)).thenReturn(buildUser(1L, "alice", "Finance Tester", "COMP-001"));
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(buildCompany("COMP-001", "001", "Guangzhou Branch")));
        when(systemDepartmentMapper.selectList(any())).thenReturn(List.of(
                buildDepartment(10L, "Finance Center"),
                buildDepartment(11L, "Expense Admin", 10L)
        ));
        when(userMapper.selectList(any())).thenReturn(List.of(buildUser(2L, "bob", "Shared Employee", "COMP-OTHER")));
        when(glAccvouchMapper.selectObjs(any())).thenReturn(List.of());

        FinanceVoucherMetaVO meta = service.getMeta(1L, "alice", "COMP-001", "2026-04-09", "\u8bb0");

        assertEquals("10", meta.getDepartmentOptions().get(0).getValue());
        assertEquals("Finance Center", meta.getDepartmentOptions().get(0).getName());
        assertNull(meta.getDepartmentOptions().get(0).getParentValue());
        assertEquals("11", meta.getDepartmentOptions().get(1).getValue());
        assertEquals("10", meta.getDepartmentOptions().get(1).getParentValue());
        assertEquals("2", meta.getEmployeeOptions().get(0).getValue());
        assertEquals("Shared Employee", meta.getEmployeeOptions().get(0).getName());
    }

    @Test
    void saveVoucherWritesAccountNameSnapshot() {
        List<GlAccvouch> insertedRows = new ArrayList<>();
        doAnswer(invocation -> {
            insertedRows.add(invocation.getArgument(0));
            return 1;
        }).when(glAccvouchMapper).insert(any(GlAccvouch.class));

        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "alice", "\u8d22\u52a1\u5c0f\u738b", "COMP-001"));
        when(systemCompanyMapper.selectCount(any())).thenReturn(1L);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("5601", "\u7ba1\u7406\u8d39\u7528"),
                buildSubject("1002", "\u94f6\u884c\u5b58\u6b3e")
        ));
        when(glAccvouchMapper.selectObjs(any())).thenReturn(List.of());

        FinanceVoucherSaveDTO dto = new FinanceVoucherSaveDTO();
        dto.setCompanyId("COMP-001");
        dto.setIperiod(4);
        dto.setCsign("\u8bb0");
        dto.setDbillDate("2026-04-09");
        dto.setEntries(List.of(
                buildSaveEntry("\u529e\u516c\u8d39\u7528", "5601", "100.00", null),
                buildSaveEntry("\u652f\u4ed8\u529e\u516c\u8d39\u7528", "1002", null, "100.00")
        ));

        service.saveVoucher(dto, 1L, "alice");

        assertEquals(2, insertedRows.size());
        assertEquals("\u7ba1\u7406\u8d39\u7528", insertedRows.get(0).getCcodeName());
        assertEquals("\u94f6\u884c\u5b58\u6b3e", insertedRows.get(1).getCcodeName());
    }

    @Test
    void saveVoucherRejectsOverlongDigestBeforeWrite() {
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "alice", "\u8d22\u52a1\u5c0f\u738b", "COMP-001"));
        when(systemCompanyMapper.selectCount(any())).thenReturn(1L);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("5601", "\u7ba1\u7406\u8d39\u7528"),
                buildSubject("1002", "\u94f6\u884c\u5b58\u6b3e")
        ));

        FinanceVoucherSaveDTO dto = new FinanceVoucherSaveDTO();
        dto.setCompanyId("COMP-001");
        dto.setIperiod(4);
        dto.setCsign("\u8bb0");
        dto.setDbillDate("2026-04-09");
        dto.setEntries(List.of(
                buildSaveEntry("A".repeat(256), "5601", "100.00", null),
                buildSaveEntry("\u652f\u4ed8\u529e\u516c\u8d39\u7528", "1002", null, "100.00")
        ));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.saveVoucher(dto, 1L, "alice")
        );

        assertEquals("\u7b2c 1 \u884c\u6458\u8981\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 255 \u4e2a\u5b57\u7b26", exception.getMessage());
    }

    @Test
    void saveVoucherRejectsOversizedSubjectNameSnapshot() {
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "alice", "\u8d22\u52a1\u5c0f\u738b", "COMP-001"));
        when(systemCompanyMapper.selectCount(any())).thenReturn(1L);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("5601", "\u79d1".repeat(129)),
                buildSubject("1002", "\u94f6\u884c\u5b58\u6b3e")
        ));

        FinanceVoucherSaveDTO dto = new FinanceVoucherSaveDTO();
        dto.setCompanyId("COMP-001");
        dto.setIperiod(4);
        dto.setCsign("\u8bb0");
        dto.setDbillDate("2026-04-09");
        dto.setEntries(List.of(
                buildSaveEntry("\u529e\u516c\u8d39\u7528", "5601", "100.00", null),
                buildSaveEntry("\u652f\u4ed8\u529e\u516c\u8d39\u7528", "1002", null, "100.00")
        ));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.saveVoucher(dto, 1L, "alice")
        );

        assertEquals("\u79d1\u76ee\u30105601\u3011\u540d\u79f0\u957f\u5ea6\u8d85\u8fc7 128\uff0c\u8bf7\u5148\u7ef4\u62a4\u4f1a\u8ba1\u79d1\u76ee\u6863\u6848", exception.getMessage());
    }

    @Test
    void saveVoucherRejectsOverlongCurrencyNameBeforeWrite() {
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "alice", "\u8d22\u52a1\u5c0f\u738b", "COMP-001"));
        when(systemCompanyMapper.selectCount(any())).thenReturn(1L);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("5601", "\u7ba1\u7406\u8d39\u7528"),
                buildSubject("1002", "\u94f6\u884c\u5b58\u6b3e")
        ));

        FinanceVoucherSaveDTO dto = new FinanceVoucherSaveDTO();
        dto.setCompanyId("COMP-001");
        dto.setIperiod(4);
        dto.setCsign("\u8bb0");
        dto.setDbillDate("2026-04-09");
        dto.setEntries(List.of(
                buildSaveEntry("\u529e\u516c\u8d39\u7528", "5601", "100.00", null),
                buildSaveEntry("\u652f\u4ed8\u529e\u516c\u8d39\u7528", "1002", null, "100.00")
        ));
        dto.getEntries().get(0).setCexchName("C".repeat(33));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.saveVoucher(dto, 1L, "alice")
        );

        assertEquals("\u7b2c 1 \u884c\u5e01\u79cd\u540d\u79f0\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc7 32 \u4e2a\u5b57\u7b26", exception.getMessage());
    }

    @Test
    void updateVoucherPreservesChineseDigestWhenRebuildingEntries() {
        List<GlAccvouch> insertedRows = new ArrayList<>();
        doAnswer(invocation -> {
            insertedRows.add(invocation.getArgument(0));
            return 1;
        }).when(glAccvouchMapper).insert(any(GlAccvouch.class));

        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(
                buildRow(1, "COMP-001", 3, "\u8bb0", 8, 1, "2026-03-28", "\u539f\u59cb\u6458\u8981", "5601", new BigDecimal("1280.00"), BigDecimal.ZERO),
                buildRow(2, "COMP-001", 3, "\u8bb0", 8, 2, "2026-03-28", "\u539f\u59cb\u6458\u8981", "1002", BigDecimal.ZERO, new BigDecimal("1280.00"))
        ));
        when(systemCompanyMapper.selectCount(any())).thenReturn(1L);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("5601", "\u7ba1\u7406\u8d39\u7528"),
                buildSubject("1002", "\u94f6\u884c\u5b58\u6b3e")
        ));

        FinanceVoucherSaveDTO dto = new FinanceVoucherSaveDTO();
        dto.setCompanyId("COMP-001");
        dto.setIperiod(3);
        dto.setCsign("\u8bb0");
        dto.setInoId(8);
        dto.setDbillDate("2026-03-28");
        dto.setEntries(List.of(
                buildSaveEntry("\u529e\u516c\u8d39\u7528", "5601", "1280.00", null),
                buildSaveEntry("\u94f6\u884c\u4ed8\u6b3e", "1002", null, "1280.00")
        ));

        FinanceVoucherSaveResultVO result = service.updateVoucher("COMP-001", "COMP-001~3~\u8bb0~8", dto, 1L, "alice");

        assertEquals("COMP-001~3~\u8bb0~8", result.getVoucherNo());
        assertEquals("COMP-001", result.getCompanyId());
        assertEquals(3, result.getIperiod());
        assertEquals("\u8bb0", result.getCsign());
        assertEquals(8, result.getInoId());
        assertEquals(2, result.getEntryCount());
        assertEquals(2, insertedRows.size());
        assertEquals("\u529e\u516c\u8d39\u7528", insertedRows.get(0).getCdigest());
        assertEquals("\u94f6\u884c\u4ed8\u6b3e", insertedRows.get(1).getCdigest());
        assertEquals("5601", insertedRows.get(0).getCcode());
        assertEquals("1002", insertedRows.get(1).getCcode());
    }

    @Test
    void updateVoucherRejectsReviewedVoucher() {
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(buildReviewedRow()));

        FinanceVoucherSaveDTO dto = new FinanceVoucherSaveDTO();
        dto.setCompanyId("COMP-001");
        dto.setIperiod(3);
        dto.setCsign("\u8bb0");
        dto.setInoId(8);
        dto.setDbillDate("2026-03-28");
        dto.setEntries(List.of());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                service.updateVoucher("COMP-001", "COMP-001~3~\u8bb0~8", dto, 1L, "alice")
        );
        assertEquals("\u5f53\u524d\u51ed\u8bc1\u72b6\u6001\u4e0d\u5141\u8bb8\u4fee\u6539", exception.getMessage());
    }

    @Test
    void queryVouchersResolvesErrorStatusBeforeReviewed() {
        when(systemCompanyMapper.selectCount(any())).thenReturn(1L);
        GlAccvouch errorRow = buildRow(1, "COMP-001", 3, "\u8bb0", 8, 1, "2026-03-28", "\u5f85\u5ba1\u51ed\u8bc1", "5601", new BigDecimal("1280.00"), BigDecimal.ZERO);
        errorRow.setCcheck("\u5ba1\u6838\u4eba");
        errorRow.setIflag(1);
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(errorRow));

        FinanceVoucherQueryDTO dto = new FinanceVoucherQueryDTO();
        dto.setCompanyId("COMP-001");

        FinanceVoucherSummaryVO summary = service.queryVouchers(dto).getItems().get(0);

        assertEquals("ERROR", summary.getStatus());
        assertEquals("\u5df2\u6807\u8bb0\u9519\u8bef", summary.getStatusLabel());
        assertEquals("\u5ba1\u6838\u4eba", summary.getCheckerName());
    }

    @Test
    void reviewVoucherWritesCheckerAndReturnsNextReviewableVoucher() {
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "alice", "\u8d22\u52a1\u5c0f\u738b", "COMP-001"));

        List<GlAccvouch> currentRows = List.of(
                buildRow(1, "COMP-001", 3, "\u8bb0", 8, 1, "2026-03-28", "\u5f85\u5ba1\u6458\u8981", "5601", new BigDecimal("1280.00"), BigDecimal.ZERO),
                buildRow(2, "COMP-001", 3, "\u8bb0", 8, 2, "2026-03-28", "\u5f85\u5ba1\u6458\u8981", "1002", BigDecimal.ZERO, new BigDecimal("1280.00"))
        );
        List<GlAccvouch> nextRows = List.of(
                buildReviewedVoucherRow(9, 3, "\u8bb0", "2026-03-28"),
                buildErrorVoucherRow(10, 3, "\u8bb0", "2026-03-28"),
                buildRow(5, "COMP-001", 3, "\u8bb0", 11, 1, "2026-03-28", "\u4e0b\u4e00\u5f20\u51ed\u8bc1", "5601", new BigDecimal("256.00"), BigDecimal.ZERO)
        );
        List<GlAccvouch> refreshedRows = List.of(
                buildReviewedVoucherRow(8, 3, "\u8bb0", "2026-03-28"),
                buildReviewedVoucherRow(8, 3, "\u8bb0", "2026-03-28")
        );
        refreshedRows.forEach(row -> row.setCcheck("\u8d22\u52a1\u5c0f\u738b"));

        when(glAccvouchMapper.selectList(any())).thenReturn(currentRows, nextRows, refreshedRows);

        FinanceVoucherActionResultVO result = service.reviewVoucher("COMP-001", "COMP-001~3~\u8bb0~8", 1L, "alice");

        assertEquals("REVIEWED", result.getStatus());
        assertEquals("\u8d22\u52a1\u5c0f\u738b", result.getCheckerName());
        assertEquals("COMP-001~3~\u8bb0~11", result.getNextVoucherNo());
        assertFalse(Boolean.TRUE.equals(result.getLastVoucherOfMonth()));

        ArgumentCaptor<Wrapper<GlAccvouch>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(glAccvouchMapper).update(eq(null), wrapperCaptor.capture());
        assertWrapperContainsValues(wrapperCaptor.getValue(), "\u8d22\u52a1\u5c0f\u738b", 0);
    }

    @Test
    void reviewVoucherMarksLastVoucherWhenNoNextReviewableVoucher() {
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "alice", "\u8d22\u52a1\u5c0f\u738b", "COMP-001"));

        List<GlAccvouch> currentRows = List.of(
                buildRow(1, "COMP-001", 3, "\u8bb0", 8, 1, "2026-03-28", "\u5f85\u5ba1\u6458\u8981", "5601", new BigDecimal("1280.00"), BigDecimal.ZERO)
        );
        List<GlAccvouch> refreshedRows = List.of(buildReviewedVoucherRow(8, 3, "\u8bb0", "2026-03-28"));
        refreshedRows.get(0).setCcheck("\u8d22\u52a1\u5c0f\u738b");

        when(glAccvouchMapper.selectList(any())).thenReturn(currentRows, List.of(), refreshedRows);

        FinanceVoucherActionResultVO result = service.reviewVoucher("COMP-001", "COMP-001~3~\u8bb0~8", 1L, "alice");

        assertEquals("REVIEWED", result.getStatus());
        assertNull(result.getNextVoucherNo());
        assertTrue(Boolean.TRUE.equals(result.getLastVoucherOfMonth()));
    }

    @Test
    void unreviewVoucherClearsChecker() {
        List<GlAccvouch> currentRows = List.of(buildReviewedVoucherRow(8, 3, "\u8bb0", "2026-03-28"));
        List<GlAccvouch> refreshedRows = List.of(buildRow(1, "COMP-001", 3, "\u8bb0", 8, 1, "2026-03-28", "\u53cd\u5ba1\u6458\u8981", "5601", new BigDecimal("1280.00"), BigDecimal.ZERO));

        when(glAccvouchMapper.selectList(any())).thenReturn(currentRows, refreshedRows);

        FinanceVoucherActionResultVO result = service.unreviewVoucher("COMP-001", "COMP-001~3~\u8bb0~8");

        assertEquals("UNPOSTED", result.getStatus());
        assertNull(result.getCheckerName());

        ArgumentCaptor<Wrapper<GlAccvouch>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(glAccvouchMapper).update(eq(null), wrapperCaptor.capture());
        assertWrapperContainsValues(wrapperCaptor.getValue(), 0);
    }

    @Test
    void markVoucherErrorSetsErrorFlag() {
        List<GlAccvouch> currentRows = List.of(buildRow(1, "COMP-001", 3, "\u8bb0", 8, 1, "2026-03-28", "\u6807\u9519\u6458\u8981", "5601", new BigDecimal("1280.00"), BigDecimal.ZERO));
        List<GlAccvouch> refreshedRows = List.of(buildErrorVoucherRow(8, 3, "\u8bb0", "2026-03-28"));

        when(glAccvouchMapper.selectList(any())).thenReturn(currentRows, refreshedRows);

        FinanceVoucherActionResultVO result = service.markVoucherError("COMP-001", "COMP-001~3~\u8bb0~8");

        assertEquals("ERROR", result.getStatus());
        assertEquals("\u5df2\u6807\u8bb0\u9519\u8bef", result.getStatusLabel());

        ArgumentCaptor<Wrapper<GlAccvouch>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(glAccvouchMapper).update(eq(null), wrapperCaptor.capture());
        assertWrapperContainsValues(wrapperCaptor.getValue(), 1);
    }

    @Test
    void clearVoucherErrorRestoresReviewedStatus() {
        GlAccvouch currentRow = buildErrorVoucherRow(8, 3, "\u8bb0", "2026-03-28");
        currentRow.setCcheck("\u8d22\u52a1\u4e3b\u7ba1");
        GlAccvouch refreshedRow = buildReviewedVoucherRow(8, 3, "\u8bb0", "2026-03-28");
        refreshedRow.setCcheck("\u8d22\u52a1\u4e3b\u7ba1");

        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(currentRow), List.of(refreshedRow));

        FinanceVoucherActionResultVO result = service.clearVoucherError("COMP-001", "COMP-001~3~\u8bb0~8");

        assertEquals("REVIEWED", result.getStatus());
        assertEquals("\u8d22\u52a1\u4e3b\u7ba1", result.getCheckerName());

        ArgumentCaptor<Wrapper<GlAccvouch>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(glAccvouchMapper).update(eq(null), wrapperCaptor.capture());
        assertWrapperContainsValues(wrapperCaptor.getValue(), 0);
    }

    private com.finex.auth.dto.FinanceVoucherEntryDTO buildSaveEntry(String digest, String code, String debit, String credit) {
        com.finex.auth.dto.FinanceVoucherEntryDTO entry = new com.finex.auth.dto.FinanceVoucherEntryDTO();
        entry.setCdigest(digest);
        entry.setCcode(code);
        entry.setCexchName("CNY");
        entry.setNfrat(BigDecimal.ONE);
        entry.setMd(debit == null ? null : new BigDecimal(debit));
        entry.setMc(credit == null ? null : new BigDecimal(credit));
        return entry;
    }

    private FinanceAccountSubject buildSubject(String code, String name) {
        return buildSubject(code, name, 0, 0, 0, 0, 0, null);
    }

    private FinanceAccountSubject buildSubject(
            String code,
            String name,
            Integer bperson,
            Integer bcus,
            Integer bsup,
            Integer bdept,
            Integer bitem,
            String cassItem
    ) {
        FinanceAccountSubject subject = new FinanceAccountSubject();
        subject.setCompanyId("COMP-001");
        subject.setSubjectCode(code);
        subject.setSubjectName(name);
        subject.setStatus(1);
        subject.setBclose(0);
        subject.setBperson(bperson);
        subject.setBcus(bcus);
        subject.setBsup(bsup);
        subject.setBdept(bdept);
        subject.setBitem(bitem);
        subject.setCassItem(cassItem);
        return subject;
    }

    private FinanceCustomer buildCustomer(String code, String name, String companyId) {
        FinanceCustomer customer = new FinanceCustomer();
        customer.setCCusCode(code);
        customer.setCCusName(name);
        customer.setCompanyId(companyId);
        return customer;
    }

    private FinanceVendor buildVendor(String code, String name, String companyId) {
        FinanceVendor vendor = new FinanceVendor();
        vendor.setCVenCode(code);
        vendor.setCVenName(name);
        vendor.setCompanyId(companyId);
        return vendor;
    }

    private FinanceProjectClass buildProjectClass(String code, String name, String companyId) {
        FinanceProjectClass projectClass = new FinanceProjectClass();
        projectClass.setCompanyId(companyId);
        projectClass.setProjectClassCode(code);
        projectClass.setProjectClassName(name);
        projectClass.setStatus(1);
        projectClass.setSortOrder(1);
        return projectClass;
    }

    private FinanceProjectArchive buildProject(String code, String name, String classCode, String companyId) {
        FinanceProjectArchive project = new FinanceProjectArchive();
        project.setCompanyId(companyId);
        project.setCitemcode(code);
        project.setCitemname(name);
        project.setCitemccode(classCode);
        project.setStatus(1);
        project.setBclose(0);
        project.setSortOrder(1);
        return project;
    }

    private SystemCompany buildCompany(String companyId, String companyCode, String companyName) {
        SystemCompany company = new SystemCompany();
        company.setCompanyId(companyId);
        company.setCompanyCode(companyCode);
        company.setCompanyName(companyName);
        company.setStatus(1);
        return company;
    }

    private User buildUser(Long id, String username, String name, String companyId) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setName(name);
        user.setCompanyId(companyId);
        user.setStatus(1);
        return user;
    }

    private SystemDepartment buildDepartment(Long id, String deptName) {
        return buildDepartment(id, deptName, null);
    }

    private SystemDepartment buildDepartment(Long id, String deptName, Long parentId) {
        SystemDepartment department = new SystemDepartment();
        department.setId(id);
        department.setDeptName(deptName);
        department.setParentId(parentId);
        department.setStatus(1);
        return department;
    }

    private GlAccvouch buildRow(int id, String companyId, int period, String csign, int inoId, int inid, String billDate,
                                String digest, String code, BigDecimal debit, BigDecimal credit) {
        GlAccvouch row = new GlAccvouch();
        row.setId(id);
        row.setCompanyId(companyId);
        row.setIperiod(period);
        row.setCsign(csign);
        row.setInoId(inoId);
        row.setInid(inid);
        row.setDbillDate(LocalDateTime.parse(billDate + "T00:00:00"));
        row.setCdigest(digest);
        row.setCcode(code);
        row.setMd(debit);
        row.setMc(credit);
        row.setIdoc(1);
        row.setCbill("\u8d22\u52a1\u5236\u5355\u5458");
        row.setIbook(0);
        return row;
    }

    private GlAccvouch buildReviewedRow() {
        GlAccvouch row = buildRow(1, "COMP-001", 3, "\u8bb0", 8, 1, "2026-03-28", "\u529e\u516c\u8d39", "5601", new BigDecimal("1280.00"), BigDecimal.ZERO);
        row.setCcheck("checker");
        return row;
    }

    private GlAccvouch buildReviewedVoucherRow(int inoId, int period, String csign, String billDate) {
        GlAccvouch row = buildRow(inoId, "COMP-001", period, csign, inoId, 1, billDate, "\u5df2\u5ba1\u51ed\u8bc1", "5601", new BigDecimal("100.00"), BigDecimal.ZERO);
        row.setCcheck("\u5ba1\u6838\u4eba");
        return row;
    }

    private GlAccvouch buildErrorVoucherRow(int inoId, int period, String csign, String billDate) {
        GlAccvouch row = buildRow(inoId, "COMP-001", period, csign, inoId, 1, billDate, "\u9519\u8bef\u51ed\u8bc1", "5601", new BigDecimal("100.00"), BigDecimal.ZERO);
        row.setIflag(1);
        return row;
    }

    private void assertWrapperContainsValues(Wrapper<GlAccvouch> wrapper, Object... expectedValues) {
        AbstractWrapper<?, ?, ?> abstractWrapper = (AbstractWrapper<?, ?, ?>) wrapper;
        Collection<Object> paramValues = abstractWrapper.getParamNameValuePairs().values();
        for (Object expectedValue : expectedValues) {
            assertTrue(paramValues.contains(expectedValue), "wrapper should contain value: " + expectedValue);
        }
    }
}

