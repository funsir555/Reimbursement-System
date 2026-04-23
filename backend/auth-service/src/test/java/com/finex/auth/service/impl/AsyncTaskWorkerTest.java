package com.finex.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpensePaymentOrderVO;
import com.finex.auth.mapper.AsyncTaskRecordMapper;
import com.finex.auth.mapper.DownloadRecordMapper;
import com.finex.auth.service.ExpenseDocumentService;
import com.finex.auth.service.NotificationService;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class AsyncTaskWorkerTest {

    @Mock private AsyncTaskRecordMapper asyncTaskRecordMapper;
    @Mock private DownloadRecordMapper downloadRecordMapper;
    @Mock private NotificationService notificationService;
    @Mock private ExpenseDocumentService expenseDocumentService;
    @Mock private DownloadStorageService downloadStorageService;

    @Test
    void buildPaymentPendingWorkbookUsesNewExportColumns() throws Exception {
        AsyncTaskWorker worker = new AsyncTaskWorker(
                asyncTaskRecordMapper,
                downloadRecordMapper,
                notificationService,
                expenseDocumentService,
                new ObjectMapper(),
                downloadStorageService
        );

        ExpensePaymentOrderVO item = new ExpensePaymentOrderVO();
        item.setDocumentCode("DOC202604220001");
        item.setDocumentTitle("差旅报销");
        item.setTemplateTypeLabel("报销单");
        item.setSubmitterName("刘礼聪");
        item.setPaymentCompanyName("上进青年");
        item.setPayeeOrCounterpartyName("上海供应商");
        item.setPayeeAccountNo("6222000012345678");
        item.setPayeeBankName("招商银行上海分行");
        item.setPayeeBankProvince("上海");
        item.setPayeeBankCity("上海市");
        item.setBankPushSummary("差旅付款摘要");
        item.setActualPaymentAmount(new BigDecimal("88.66"));
        item.setPaymentStatusLabel("待支付");

        Method method = AsyncTaskWorker.class.getDeclaredMethod(
                "buildPaymentPendingWorkbook",
                String.class,
                List.class
        );
        method.setAccessible(true);
        byte[] workbookBytes = (byte[]) method.invoke(worker, "支付单", List.of(item));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(workbookBytes))) {
            Row headerRow = workbook.getSheetAt(0).getRow(0);
            String[] headers = new String[13];
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headerRow.getCell(i).getStringCellValue();
            }
            assertArrayEquals(new String[] {
                    "单据编号",
                    "单据名称",
                    "单据类型",
                    "提单人",
                    "付款公司",
                    "收款单位",
                    "收款账号",
                    "开户行",
                    "开户省",
                    "开户市",
                    "银行推送摘要",
                    "实际支付金额",
                    "单据状态"
            }, headers);

            DataFormatter formatter = new DataFormatter();
            Row dataRow = workbook.getSheetAt(0).getRow(1);
            assertEquals("DOC202604220001", formatter.formatCellValue(dataRow.getCell(0)));
            assertEquals("上海供应商", formatter.formatCellValue(dataRow.getCell(5)));
            assertEquals("6222000012345678", formatter.formatCellValue(dataRow.getCell(6)));
            assertEquals("招商银行上海分行", formatter.formatCellValue(dataRow.getCell(7)));
            assertEquals("上海", formatter.formatCellValue(dataRow.getCell(8)));
            assertEquals("上海市", formatter.formatCellValue(dataRow.getCell(9)));
            assertEquals("差旅付款摘要", formatter.formatCellValue(dataRow.getCell(10)));
            assertEquals("88.66", formatter.formatCellValue(dataRow.getCell(11)));
            assertEquals("待支付", formatter.formatCellValue(dataRow.getCell(12)));
        }
    }
}
