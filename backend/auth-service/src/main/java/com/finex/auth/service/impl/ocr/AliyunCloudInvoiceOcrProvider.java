package com.finex.auth.service.impl.ocr;

import cn.hutool.core.util.StrUtil;
import com.aliyun.ocr_api20210707.Client;
import com.aliyun.ocr_api20210707.models.RecognizeInvoiceRequest;
import com.aliyun.ocr_api20210707.models.RecognizeInvoiceResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teautil.models.RuntimeOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ExpenseAttachmentOcrResultVO;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AliyunCloudInvoiceOcrProvider implements CloudInvoiceOcrProvider {

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})\\D?(\\d{1,2})\\D?(\\d{1,2})");
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("request\\s*id\\s*[:=]\\s*([A-Za-z0-9-]+)", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;

    public AliyunCloudInvoiceOcrProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String getProviderCode() {
        return "ALIYUN";
    }

    @Override
    public ExpenseAttachmentOcrResultVO recognize(InvoiceOcrRequest request, OcrProviderRuntimeConfig config) {
        try {
            com.aliyun.teaopenapi.models.Config sdkConfig = new com.aliyun.teaopenapi.models.Config();
            sdkConfig.accessKeyId = config.accessKeyId();
            sdkConfig.accessKeySecret = config.accessKeySecret();
            sdkConfig.endpoint = config.endpoint();

            Client client = new Client(sdkConfig);
            RecognizeInvoiceRequest sdkRequest = new RecognizeInvoiceRequest();
            sdkRequest.setBody(new ByteArrayInputStream(request.fileBytes()));
            if (request.pageNo() != null) {
                sdkRequest.setPageNo(request.pageNo());
            }

            RuntimeOptions runtime = new RuntimeOptions();
            runtime.connectTimeout = config.connectTimeoutMs();
            runtime.readTimeout = config.readTimeoutMs();

            RecognizeInvoiceResponse response = client.recognizeInvoiceWithOptions(sdkRequest, runtime);
            if (response.getBody() == null) {
                throw new InvoiceOcrException("PROVIDER_ERROR", "阿里云 OCR 未返回响应体");
            }
            if (StrUtil.isNotBlank(response.getBody().getCode())
                    && !"200".equals(response.getBody().getCode())) {
                throw new InvoiceOcrException(
                        "PROVIDER_ERROR",
                        StrUtil.blankToDefault(response.getBody().getMessage(), "阿里云 OCR 调用失败")
                );
            }
            return parseResponse(
                    response.getBody().getData(),
                    response.getBody().getRequestId(),
                    config
            );
        } catch (InvoiceOcrException ex) {
            throw ex;
        } catch (TeaException ex) {
            throw mapTeaException(ex);
        } catch (Exception ex) {
            throw new InvoiceOcrException("PROVIDER_ERROR", "阿里云 OCR 调用失败，请检查配置或稍后重试", ex);
        }
    }

    ExpenseAttachmentOcrResultVO parseResponse(String data, String requestId, OcrProviderRuntimeConfig config) {
        if (StrUtil.isBlank(data)) {
            throw new InvoiceOcrException("PARSE_FAILED", "阿里云 OCR 未返回可解析结果");
        }
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode dataNode = firstPresentNode(root, "Data", "data");
            JsonNode payload = firstPresentNode(dataNode, "data", "Data");
            String invoiceCode = firstNonBlank(payload, "invoiceCode", "invoiceCodes");
            String invoiceNumber = firstNonBlank(payload, "invoiceNumber", "invoiceNo");
            String invoiceDate = normalizeDate(firstNonBlank(payload, "invoiceDate"));
            String invoiceType = firstNonBlank(payload, "invoiceType", "title");
            if (StrUtil.isBlank(invoiceType)) {
                invoiceType = firstNonBlank(dataNode, "title");
            }
            String sellerName = firstNonBlank(payload, "sellerName");
            BigDecimal totalAmount = toDecimal(firstNonBlank(payload, "totalAmount"));
            BigDecimal taxAmount = toDecimal(firstNonBlank(payload, "invoiceTax", "taxAmount"));

            boolean hasAnyField = StrUtil.isNotBlank(invoiceCode)
                    || StrUtil.isNotBlank(invoiceNumber)
                    || StrUtil.isNotBlank(invoiceDate)
                    || StrUtil.isNotBlank(invoiceType)
                    || StrUtil.isNotBlank(sellerName)
                    || totalAmount != null
                    || taxAmount != null;
            if (!hasAnyField) {
                throw new InvoiceOcrException("PARSE_FAILED", "阿里云 OCR 未识别到发票关键信息");
            }

            ExpenseAttachmentOcrResultVO result = new ExpenseAttachmentOcrResultVO();
            result.setStatus("SUCCESS");
            result.setProviderCode(config.providerCode());
            result.setProviderName(config.providerName());
            result.setRequestId(requestId);
            result.setRecognizedAt(LocalDateTime.now());
            result.setInvoiceCode(invoiceCode);
            result.setInvoiceNumber(invoiceNumber);
            result.setInvoiceDate(invoiceDate);
            result.setInvoiceType(invoiceType);
            result.setSellerName(sellerName);
            result.setTotalAmount(totalAmount);
            result.setTaxAmount(taxAmount);
            result.setMessage("识别成功");
            return result;
        } catch (InvoiceOcrException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvoiceOcrException("PARSE_FAILED", "阿里云 OCR 结果解析失败", ex);
        }
    }

    private JsonNode firstPresentNode(JsonNode node, String... fieldNames) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return objectMapper.nullNode();
        }
        for (String fieldName : fieldNames) {
            JsonNode field = node.path(fieldName);
            if (!field.isMissingNode() && !field.isNull()) {
                return field;
            }
        }
        return node;
    }

    private String firstNonBlank(JsonNode node, String... fieldNames) {
        if (node == null || fieldNames == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode field = node.path(fieldName);
            if (!field.isMissingNode() && !field.isNull()) {
                String value = field.asText();
                if (StrUtil.isNotBlank(value)) {
                    return value.trim();
                }
            }
        }
        return null;
    }

    private BigDecimal toDecimal(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private String normalizeDate(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        Matcher matcher = DATE_PATTERN.matcher(value.trim());
        if (!matcher.find()) {
            return value.trim();
        }
        return String.format(
                "%s-%02d-%02d",
                matcher.group(1),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3))
        );
    }

    private InvoiceOcrException mapTeaException(TeaException ex) {
        String message = StrUtil.blankToDefault(ex.getMessage(), "阿里云 OCR 调用失败");
        String lowered = message.toLowerCase(Locale.ROOT);
        String requestId = extractRequestId(message);
        if (lowered.contains("specified signature does not match our calculation")
                || lowered.contains("signature does not match")
                || (lowered.contains("signature") && lowered.contains("does not match"))) {
            return new InvoiceOcrException(
                    "PROVIDER_ERROR",
                    appendRequestId("阿里云签名校验失败，请确认 AccessKey ID 与 Secret 来自同一组密钥", requestId),
                    ex
            );
        }
        if (lowered.contains("timeout") || lowered.contains("timed out")) {
            return new InvoiceOcrException("TIMEOUT", appendRequestId("阿里云 OCR 请求超时", requestId), ex);
        }
        return new InvoiceOcrException("PROVIDER_ERROR", appendRequestId(message, requestId), ex);
    }

    private String extractRequestId(String message) {
        if (StrUtil.isBlank(message)) {
            return null;
        }
        Matcher matcher = REQUEST_ID_PATTERN.matcher(message);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String appendRequestId(String message, String requestId) {
        if (StrUtil.isBlank(requestId)) {
            return message;
        }
        return message + "（requestId: " + requestId + "）";
    }
}
