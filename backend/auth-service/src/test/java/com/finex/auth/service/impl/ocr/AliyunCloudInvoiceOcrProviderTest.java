package com.finex.auth.service.impl.ocr;

import com.aliyun.tea.TeaException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseAttachmentOcrResultVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AliyunCloudInvoiceOcrProviderTest {

    private final AliyunCloudInvoiceOcrProvider provider = new AliyunCloudInvoiceOcrProvider(new ObjectMapper());

    @Test
    void parseResponseReadsAliyunDataPayload() {
        OcrProviderRuntimeConfig config = new OcrProviderRuntimeConfig(
                "ALIYUN",
                "阿里云",
                "ak",
                "sk",
                "ocr-api.cn-hangzhou.aliyuncs.com",
                5000,
                15000
        );

        String payload = """
                {
                  "Data": {
                    "title": "增值税电子普通发票",
                    "data": {
                      "invoiceCode": "123456789012",
                      "invoiceNumber": "87654321",
                      "invoiceDate": "2026年04月19日",
                      "sellerName": "上海测试商户",
                      "totalAmount": "188.50",
                      "invoiceTax": "10.68"
                    }
                  }
                }
                """;

        ExpenseAttachmentOcrResultVO result = provider.parseResponse(payload, "req-001", config);

        assertEquals("SUCCESS", result.getStatus());
        assertEquals("ALIYUN", result.getProviderCode());
        assertEquals("阿里云", result.getProviderName());
        assertEquals("req-001", result.getRequestId());
        assertEquals("123456789012", result.getInvoiceCode());
        assertEquals("87654321", result.getInvoiceNumber());
        assertEquals("2026-04-19", result.getInvoiceDate());
        assertEquals("增值税电子普通发票", result.getInvoiceType());
        assertEquals("上海测试商户", result.getSellerName());
        assertEquals(new BigDecimal("188.50"), result.getTotalAmount());
        assertEquals(new BigDecimal("10.68"), result.getTaxAmount());
        assertEquals("识别成功", result.getMessage());
    }

    @Test
    void parseResponseFailsWhenAliyunPayloadHasNoInvoiceFields() {
        OcrProviderRuntimeConfig config = new OcrProviderRuntimeConfig(
                "ALIYUN",
                "阿里云",
                "ak",
                "sk",
                "ocr-api.cn-hangzhou.aliyuncs.com",
                5000,
                15000
        );

        String payload = """
                {
                  "Data": {
                    "title": "",
                    "data": {}
                  }
                }
                """;

        InvoiceOcrException error = assertThrows(
                InvoiceOcrException.class,
                () -> provider.parseResponse(payload, "req-002", config)
        );

        assertEquals("PARSE_FAILED", error.getStatus());
        assertEquals("阿里云 OCR 未识别到发票关键信息", error.getMessage());
    }

    @Test
    void mapTeaExceptionReturnsFriendlyMessageForSignatureMismatch() throws Exception {
        TeaException teaException = new TeaException(Map.of(
                "message",
                "Specified signature does not match our calculation. request id: FEA34A4C-6847-5DD3-8709-BEEF92552CDA"
        ));
        Method method = AliyunCloudInvoiceOcrProvider.class.getDeclaredMethod("mapTeaException", TeaException.class);
        method.setAccessible(true);

        InvoiceOcrException error = (InvoiceOcrException) method.invoke(provider, teaException);

        assertEquals("PROVIDER_ERROR", error.getStatus());
        assertEquals(
                "阿里云签名校验失败，请确认 AccessKey ID 与 Secret 来自同一组密钥（requestId: FEA34A4C-6847-5DD3-8709-BEEF92552CDA）",
                error.getMessage()
        );
    }
}
