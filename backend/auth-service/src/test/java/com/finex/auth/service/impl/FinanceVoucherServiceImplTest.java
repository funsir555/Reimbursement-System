package com.finex.auth.service.impl;

import com.finex.auth.dto.FinanceVoucherQueryDTO;
import com.finex.auth.dto.FinanceVoucherSaveDTO;
import com.finex.auth.dto.FinanceVoucherSummaryVO;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.entity.SystemCompany;
import com.finex.auth.entity.User;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceVoucherServiceImplTest {

    @Mock
    private GlAccvouchMapper glAccvouchMapper;

    @Mock
    private SystemCompanyMapper systemCompanyMapper;

    @Mock
    private SystemDepartmentMapper systemDepartmentMapper;

    @Mock
    private UserMapper userMapper;

    private FinanceVoucherServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FinanceVoucherServiceImpl(glAccvouchMapper, systemCompanyMapper, systemDepartmentMapper, userMapper);

        lenient().when(systemDepartmentMapper.selectList(any())).thenReturn(List.of());
        lenient().when(userMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void queryVouchersBuildsVoucherHeadSummary() {
        when(systemCompanyMapper.selectCount(any())).thenReturn(1L);
        when(glAccvouchMapper.selectList(any())).thenReturn(List.of(
                buildRow(1, "COMP-001", 3, "记", 8, 1, "2026-03-28", "办公费用", "5601", new BigDecimal("1280.00"), BigDecimal.ZERO),
                buildRow(2, "COMP-001", 3, "记", 8, 2, "2026-03-28", "支付办公费用", "1002", BigDecimal.ZERO, new BigDecimal("1280.00"))
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
        GlAccvouch row = buildRow(1, "COMP-001", 3, "记", 8, 1, "2026-03-28", "办公费用", "5601", new BigDecimal("1280.00"), BigDecimal.ZERO);
        row.setCcheck("checker");
        return row;
    }
}
