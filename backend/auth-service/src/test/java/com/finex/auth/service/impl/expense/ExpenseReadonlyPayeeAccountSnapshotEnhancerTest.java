package com.finex.auth.service.impl.expense;

import com.finex.auth.entity.FinanceVendor;
import com.finex.auth.entity.UserBankAccount;
import com.finex.auth.mapper.FinanceVendorMapper;
import com.finex.auth.mapper.UserBankAccountMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseReadonlyPayeeAccountSnapshotEnhancerTest {

    @Mock
    private FinanceVendorMapper financeVendorMapper;
    @Mock
    private UserBankAccountMapper userBankAccountMapper;

    private ExpenseReadonlyPayeeAccountSnapshotEnhancer enhancer;

    @BeforeEach
    void setUp() {
        enhancer = new ExpenseReadonlyPayeeAccountSnapshotEnhancer(financeVendorMapper, userBankAccountMapper);
    }

    @Test
    void enhanceFormDataPopulatesRealUserAccountFields() {
        UserBankAccount account = new UserBankAccount();
        account.setId(8L);
        account.setAccountName("张三");
        account.setAccountNo("6222333344448888");
        account.setBankName("招商银行");
        account.setBranchName("招商银行上海分行");
        when(userBankAccountMapper.selectById(8L)).thenReturn(account);

        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("payeeAccount", new LinkedHashMap<>(Map.of(
                "value", "USER_ACCOUNT:8",
                "sourceType", "USER",
                "accountNoMasked", "6222 **** 8888"
        )));

        Map<String, Object> result = enhancer.enhanceFormData(schemaWithBlock("payeeAccount", "payee-account"), formData, null);

        assertSame(formData, result);
        Map<?, ?> snapshot = (Map<?, ?>) result.get("payeeAccount");
        assertEquals("张三", snapshot.get("ownerName"));
        assertEquals("张三", snapshot.get("accountName"));
        assertEquals("6222333344448888", snapshot.get("accountNo"));
        assertEquals("招商银行上海分行", snapshot.get("bankName"));
    }

    @Test
    void enhanceFormDataUsesPaymentCompanyToPopulateRealVendorAccountFields() {
        FinanceVendor vendor = new FinanceVendor();
        vendor.setCompanyId("COMP-001");
        vendor.setCVenCode("VEN-001");
        vendor.setCVenName("上海供应商");
        vendor.setReceiptAccountName("上海供应商收款户");
        vendor.setCVenAccount("6222000011112222");
        vendor.setReceiptBranchName("中国银行上海徐汇支行");
        vendor.setCVenBank("中国银行");
        when(financeVendorMapper.selectOne(any())).thenReturn(vendor);

        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("paymentCompany", "COMP-001");
        formData.put("payeeAccount", new LinkedHashMap<>(Map.of(
                "value", "VENDOR:VEN-001",
                "sourceType", "VENDOR",
                "accountNoMasked", "6222 **** 2222"
        )));

        Map<String, Object> result = enhancer.enhanceFormData(
                schemaWithBlocks(
                        businessBlock("paymentCompany", "payment-company"),
                        businessBlock("payeeAccount", "payee-account")
                ),
                formData,
                null
        );

        Map<?, ?> snapshot = (Map<?, ?>) result.get("payeeAccount");
        assertEquals("上海供应商收款户", snapshot.get("ownerName"));
        assertEquals("上海供应商收款户", snapshot.get("accountName"));
        assertEquals("6222000011112222", snapshot.get("accountNo"));
        assertEquals("中国银行上海徐汇支行", snapshot.get("bankName"));
    }

    @Test
    void enhanceFormDataKeepsOriginalSnapshotWhenVendorCompanyContextIsMissing() {
        Map<String, Object> originalSnapshot = new LinkedHashMap<>(Map.of(
                "value", "VENDOR:VEN-001",
                "sourceType", "VENDOR",
                "accountNoMasked", "6222 **** 2222"
        ));
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("payeeAccount", originalSnapshot);

        Map<String, Object> result = enhancer.enhanceFormData(schemaWithBlock("payeeAccount", "payee-account"), formData, null);

        assertSame(formData, result);
        assertSame(originalSnapshot, result.get("payeeAccount"));
        assertEquals("6222 **** 2222", ((Map<?, ?>) result.get("payeeAccount")).get("accountNoMasked"));
    }

    private Map<String, Object> schemaWithBlock(String fieldKey, String componentCode) {
        return schemaWithBlocks(businessBlock(fieldKey, componentCode));
    }

    private Map<String, Object> schemaWithBlocks(Map<String, Object>... blocks) {
        return new LinkedHashMap<>(Map.of("blocks", List.of(blocks)));
    }

    private Map<String, Object> businessBlock(String fieldKey, String componentCode) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("blockId", fieldKey);
        block.put("fieldKey", fieldKey);
        block.put("kind", "BUSINESS_COMPONENT");
        block.put("props", new LinkedHashMap<>(Map.of("componentCode", componentCode)));
        return block;
    }
}
