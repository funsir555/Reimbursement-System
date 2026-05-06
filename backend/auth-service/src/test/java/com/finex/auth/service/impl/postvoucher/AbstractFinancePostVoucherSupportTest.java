package com.finex.auth.service.impl.postvoucher;

import com.finex.auth.entity.FinanceAccountSubject;
import com.finex.auth.entity.GlAccass;
import com.finex.auth.entity.GlAccsum;
import com.finex.auth.entity.GlAccvouch;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.FinanceAccountSetMapper;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.FinanceOpeningBalanceStateMapper;
import com.finex.auth.mapper.FinancePeriodCloseMapper;
import com.finex.auth.mapper.FinancePostVoucherStateMapper;
import com.finex.auth.mapper.GlAccassMapper;
import com.finex.auth.mapper.GlAccsumMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.UserMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractFinancePostVoucherSupportTest {

    @Mock
    private FinanceAccountSetMapper financeAccountSetMapper;
    @Mock
    private FinanceAccountSubjectMapper financeAccountSubjectMapper;
    @Mock
    private FinanceOpeningBalanceStateMapper financeOpeningBalanceStateMapper;
    @Mock
    private FinancePostVoucherStateMapper financePostVoucherStateMapper;
    @Mock
    private FinancePeriodCloseMapper financePeriodCloseMapper;
    @Mock
    private AsyncTaskRecordMapper asyncTaskRecordMapper;
    @Mock
    private GlAccvouchMapper glAccvouchMapper;
    @Mock
    private GlAccsumMapper glAccsumMapper;
    @Mock
    private GlAccassMapper glAccassMapper;
    @Mock
    private SystemCompanyMapper systemCompanyMapper;
    @Mock
    private UserMapper userMapper;

    @Test
    void loadOrCreateAccsumRowCarriesNegativeEndingBalanceAsOppositeBeginDirection() {
        TestSupport support = new TestSupport();
        FinanceAccountSubject subject = creditSubject("4103");
        GlAccsum previous = new GlAccsum();
        previous.setMe(new BigDecimal("-25.00"));
        previous.setMeF(BigDecimal.ZERO.setScale(2));
        previous.setNeS(BigDecimal.ZERO.setScale(6));
        ArgumentCaptor<GlAccsum> insertCaptor = ArgumentCaptor.forClass(GlAccsum.class);

        when(glAccsumMapper.selectOne(any())).thenReturn(null, previous);
        doAnswer(invocation -> 1).when(glAccsumMapper).insert(any(GlAccsum.class));

        support.loadOrCreateAccsumRow(voucherRow("4103"), subject);

        verify(glAccsumMapper).insert(insertCaptor.capture());
        assertEquals("-25.00", insertCaptor.getValue().getMb().toPlainString());
        assertEquals("借", insertCaptor.getValue().getCbegindC());
        assertEquals("DEBIT", insertCaptor.getValue().getCbegindCEngl());
        assertEquals("借", insertCaptor.getValue().getCenddC());
        assertEquals("DEBIT", insertCaptor.getValue().getCenddCEngl());
    }

    @Test
    void loadOrCreateAccassRowCarriesNegativeEndingBalanceAsOppositeBeginDirection() {
        TestSupport support = new TestSupport();
        FinanceAccountSubject subject = creditSubject("4103");
        GlAccass previous = new GlAccass();
        previous.setMe(new BigDecimal("-12.00"));
        previous.setMeF(BigDecimal.ZERO.setScale(2));
        previous.setNeS(BigDecimal.ZERO.setScale(6));
        ArgumentCaptor<GlAccass> insertCaptor = ArgumentCaptor.forClass(GlAccass.class);

        when(glAccassMapper.selectOne(any())).thenReturn(null, previous);
        doAnswer(invocation -> 1).when(glAccassMapper).insert(any(GlAccass.class));

        support.loadOrCreateAccassRow(voucherRow("4103"), subject);

        verify(glAccassMapper).insert(insertCaptor.capture());
        assertEquals("-12.00", insertCaptor.getValue().getMb().toPlainString());
        assertEquals("借", insertCaptor.getValue().getCbegindC());
        assertEquals("DEBIT", insertCaptor.getValue().getCbegindCEngl());
        assertEquals("借", insertCaptor.getValue().getCenddC());
        assertEquals("DEBIT", insertCaptor.getValue().getCenddCEngl());
    }

    private FinanceAccountSubject creditSubject(String subjectCode) {
        FinanceAccountSubject subject = new FinanceAccountSubject();
        subject.setCompanyId("COMP-001");
        subject.setSubjectCode(subjectCode);
        subject.setSubjectName("本年利润");
        subject.setBalanceDirection("CREDIT");
        subject.setStatus(1);
        subject.setBclose(0);
        return subject;
    }

    private GlAccvouch voucherRow(String subjectCode) {
        GlAccvouch row = new GlAccvouch();
        row.setCompanyId("COMP-001");
        row.setIyear(2026);
        row.setIyperiod(202605);
        row.setIperiod(5);
        row.setCcode(subjectCode);
        row.setCurrencyCode("CNY");
        row.setCexchName("人民币");
        return row;
    }

    private final class TestSupport extends AbstractFinancePostVoucherSupport {

        private TestSupport() {
            super(
                    financeAccountSetMapper,
                    financeAccountSubjectMapper,
                    financeOpeningBalanceStateMapper,
                    financePostVoucherStateMapper,
                    financePeriodCloseMapper,
                    asyncTaskRecordMapper,
                    glAccvouchMapper,
                    glAccsumMapper,
                    glAccassMapper,
                    systemCompanyMapper,
                    userMapper
            );
        }
    }
}
