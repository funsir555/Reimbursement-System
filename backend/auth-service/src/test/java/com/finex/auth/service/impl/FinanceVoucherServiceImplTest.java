package com.finex.auth.service.impl;

import com.finex.auth.dto.FinanceVoucherMetaVO;
import com.finex.auth.dto.FinanceVoucherQueryDTO;
import com.finex.auth.dto.FinanceVoucherSaveDTO;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
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
        lenient().when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(buildSubject("5601", "管理费用"), buildSubject("1002", "银行存款")));
        lenient().when(glAccvouchMapper.selectObjs(any())).thenReturn(List.of());
    }

    @Test
    void queryVouchersBuildsVoucherHeadSummary() {
        when(systemCompanyMapper.selectCount(any())).thenReturn(1L);
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(
                buildRow(1, "COMP-001", 3, "记", 8, 1, "2026-03-28", "办公费", "5601", new BigDecimal("1280.00"), BigDecimal.ZERO),
                buildRow(2, "COMP-001", 3, "记", 8, 2, "2026-03-28", "支付办公费", "1002", BigDecimal.ZERO, new BigDecimal("1280.00"))
        ));

        FinanceVoucherQueryDTO dto = new FinanceVoucherQueryDTO();
        dto.setCompanyId("COMP-001");
        dto.setPage(1);
        dto.setPageSize(20);

        FinanceVoucherSummaryVO summary = service.queryVouchers(dto).getItems().get(0);
        assertEquals("记-0008", summary.getDisplayVoucherNo());
        assertEquals("1280.00", summary.getTotalDebit().toPlainString());
        assertEquals("1280.00", summary.getTotalCredit().toPlainString());
    }

    @Test
    void getMetaLoadsCustomerSupplierAndProjectOptionsFromArchives() {
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "alice", "财务小王", "COMP-001"));
        when(userService.getById(1L)).thenReturn(buildUser(1L, "alice", "财务小王", "COMP-001"));
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(buildCompany("COMP-001", "001", "广州分公司")));
        when(userMapper.selectList(any())).thenReturn(List.of(buildUser(2L, "bob", "员工甲", "COMP-001")));
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(buildSubject("1001", "库存现金")));
        when(financeCustomerMapper.selectList(any())).thenReturn(List.of(buildCustomer("C00001", "华南客户", "COMP-001")));
        when(financeVendorMapper.selectList(any())).thenReturn(List.of(buildVendor("V00001", "核心供应商", "COMP-001")));
        when(financeProjectClassMapper.selectList(any())).thenReturn(List.of(buildProjectClass("7", "Market Projects", "COMP-001")));
        when(financeProjectArchiveMapper.selectList(any())).thenReturn(List.of(buildProject("2002", "South Campaign", "7", "COMP-001")));
        when(glAccvouchMapper.selectObjs(any())).thenReturn(List.of());

        FinanceVoucherMetaVO meta = service.getMeta(1L, "alice", "COMP-001", "2026-04-09", "记");

        assertEquals("C00001", meta.getCustomerOptions().get(0).getValue());
        assertEquals("C00001", meta.getCustomerOptions().get(0).getCode());
        assertEquals("华南客户", meta.getCustomerOptions().get(0).getName());
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
                buildSubject("5601", "Management Expense"),
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
    void getMetaLoadsSharedEmployeesAndDepartmentsAcrossCompanies() {
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "alice", "财务小王", "COMP-001"));
        when(userService.getById(1L)).thenReturn(buildUser(1L, "alice", "财务小王", "COMP-001"));
        when(systemCompanyMapper.selectList(any())).thenReturn(List.of(buildCompany("COMP-001", "001", "广州分公司")));
        when(systemDepartmentMapper.selectList(any())).thenReturn(List.of(buildDepartment(10L, "财务部")));
        when(userMapper.selectList(any())).thenReturn(List.of(buildUser(2L, "bob", "共享员工", "COMP-OTHER")));
        when(glAccvouchMapper.selectObjs(any())).thenReturn(List.of());

        FinanceVoucherMetaVO meta = service.getMeta(1L, "alice", "COMP-001", "2026-04-09", "记");

        assertEquals("10", meta.getDepartmentOptions().get(0).getValue());
        assertEquals("财务部", meta.getDepartmentOptions().get(0).getName());
        assertEquals("2", meta.getEmployeeOptions().get(0).getValue());
        assertEquals("共享员工", meta.getEmployeeOptions().get(0).getName());
    }

    @Test
    void saveVoucherWritesAccountNameSnapshot() {
        List<GlAccvouch> insertedRows = new ArrayList<>();
        doAnswer(invocation -> {
            insertedRows.add(invocation.getArgument(0));
            return 1;
        }).when(glAccvouchMapper).insert(any(GlAccvouch.class));

        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "alice", "财务小王", "COMP-001"));
        when(systemCompanyMapper.selectCount(any())).thenReturn(1L);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("5601", "管理费用"),
                buildSubject("1002", "银行存款")
        ));
        when(glAccvouchMapper.selectObjs(any())).thenReturn(List.of());

        FinanceVoucherSaveDTO dto = new FinanceVoucherSaveDTO();
        dto.setCompanyId("COMP-001");
        dto.setIperiod(4);
        dto.setCsign("记");
        dto.setDbillDate("2026-04-09");
        dto.setEntries(List.of(
                buildSaveEntry("办公费", "5601", "100.00", null),
                buildSaveEntry("支付办公费", "1002", null, "100.00")
        ));

        service.saveVoucher(dto, 1L, "alice");

        assertEquals(2, insertedRows.size());
        assertEquals("管理费用", insertedRows.get(0).getCcodeName());
        assertEquals("银行存款", insertedRows.get(1).getCcodeName());
    }

    @Test
    void saveVoucherRejectsOverlongDigestBeforeWrite() {
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "alice", "财务小王", "COMP-001"));
        when(systemCompanyMapper.selectCount(any())).thenReturn(1L);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("5601", "管理费用"),
                buildSubject("1002", "银行存款")
        ));

        FinanceVoucherSaveDTO dto = new FinanceVoucherSaveDTO();
        dto.setCompanyId("COMP-001");
        dto.setIperiod(4);
        dto.setCsign("记");
        dto.setDbillDate("2026-04-09");
        dto.setEntries(List.of(
                buildSaveEntry("A".repeat(256), "5601", "100.00", null),
                buildSaveEntry("支付办公费用", "1002", null, "100.00")
        ));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.saveVoucher(dto, 1L, "alice")
        );

        assertEquals("第 1 行摘要长度不能超过 255 个字符", exception.getMessage());
    }

    @Test
    void saveVoucherRejectsOversizedSubjectNameSnapshot() {
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "alice", "财务小王", "COMP-001"));
        when(systemCompanyMapper.selectCount(any())).thenReturn(1L);
        when(financeAccountSubjectMapper.selectList(any())).thenReturn(List.of(
                buildSubject("5601", "科".repeat(129)),
                buildSubject("1002", "银行存款")
        ));

        FinanceVoucherSaveDTO dto = new FinanceVoucherSaveDTO();
        dto.setCompanyId("COMP-001");
        dto.setIperiod(4);
        dto.setCsign("记");
        dto.setDbillDate("2026-04-09");
        dto.setEntries(List.of(
                buildSaveEntry("办公费用", "5601", "100.00", null),
                buildSaveEntry("支付办公费用", "1002", null, "100.00")
        ));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.saveVoucher(dto, 1L, "alice")
        );

        assertEquals("科目【5601】名称长度超过 128，请先维护会计科目档案", exception.getMessage());
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
    void updateVoucherRejectsReviewedVoucher() {
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(buildReviewedRow()));

        FinanceVoucherSaveDTO dto = new FinanceVoucherSaveDTO();
        dto.setCompanyId("COMP-001");
        dto.setIperiod(3);
        dto.setCsign("记");
        dto.setInoId(8);
        dto.setDbillDate("2026-03-28");
        dto.setEntries(List.of());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                service.updateVoucher("COMP-001", "COMP-001~3~记~8", dto, 1L, "alice")
        );
        assertEquals("当前凭证状态不允许修改", exception.getMessage());
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
        FinanceAccountSubject subject = new FinanceAccountSubject();
        subject.setCompanyId("COMP-001");
        subject.setSubjectCode(code);
        subject.setSubjectName(name);
        subject.setStatus(1);
        subject.setBclose(0);
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
        SystemDepartment department = new SystemDepartment();
        department.setId(id);
        department.setDeptName(deptName);
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
        row.setCbill("财务制单员");
        row.setIbook(0);
        return row;
    }

    private GlAccvouch buildReviewedRow() {
        GlAccvouch row = buildRow(1, "COMP-001", 3, "记", 8, 1, "2026-03-28", "办公费", "5601", new BigDecimal("1280.00"), BigDecimal.ZERO);
        row.setCcheck("checker");
        return row;
    }
}
