package com.finex.auth.service.impl;

import com.finex.auth.dto.ExpenseVoucherGenerationMetaVO;
import com.finex.auth.dto.ExpenseVoucherPushBatchResultVO;
import com.finex.auth.dto.ExpenseVoucherPushDTO;
import com.finex.auth.service.impl.expensevoucher.ExpenseVoucherMappingDomainSupport;
import com.finex.auth.service.impl.expensevoucher.ExpenseVoucherMetaSupport;
import com.finex.auth.service.impl.expensevoucher.ExpenseVoucherPushDomainSupport;
import com.finex.auth.service.impl.expensevoucher.ExpenseVoucherRecordQuerySupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseVoucherGenerationServiceImplTest {

    @Mock
    private ExpenseVoucherMetaSupport expenseVoucherMetaSupport;

    @Mock
    private ExpenseVoucherMappingDomainSupport expenseVoucherMappingDomainSupport;

    @Mock
    private ExpenseVoucherPushDomainSupport expenseVoucherPushDomainSupport;

    @Mock
    private ExpenseVoucherRecordQuerySupport expenseVoucherRecordQuerySupport;

    @Test
    void getMetaDelegatesToMetaSupport() {
        ExpenseVoucherGenerationServiceImpl service = new ExpenseVoucherGenerationServiceImpl(
                expenseVoucherMetaSupport,
                expenseVoucherMappingDomainSupport,
                expenseVoucherPushDomainSupport,
                expenseVoucherRecordQuerySupport
        );
        ExpenseVoucherGenerationMetaVO expected = new ExpenseVoucherGenerationMetaVO();
        when(expenseVoucherMetaSupport.getMeta(1L)).thenReturn(expected);

        ExpenseVoucherGenerationMetaVO result = service.getMeta(1L);

        assertSame(expected, result);
        verify(expenseVoucherMetaSupport).getMeta(1L);
    }

    @Test
    void pushDocumentsDelegatesToPushSupport() {
        ExpenseVoucherGenerationServiceImpl service = new ExpenseVoucherGenerationServiceImpl(
                expenseVoucherMetaSupport,
                expenseVoucherMappingDomainSupport,
                expenseVoucherPushDomainSupport,
                expenseVoucherRecordQuerySupport
        );
        ExpenseVoucherPushDTO dto = new ExpenseVoucherPushDTO();
        dto.setDocumentCodes(List.of("DOC-001"));
        ExpenseVoucherPushBatchResultVO expected = new ExpenseVoucherPushBatchResultVO();
        expected.setLatestBatchNo("VG001");
        when(expenseVoucherPushDomainSupport.pushDocuments(dto, 1L, "tester")).thenReturn(expected);

        ExpenseVoucherPushBatchResultVO result = service.pushDocuments(dto, 1L, "tester");

        assertSame(expected, result);
        verify(expenseVoucherPushDomainSupport).pushDocuments(dto, 1L, "tester");
    }
}
