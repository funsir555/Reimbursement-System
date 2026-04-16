package com.finex.auth.service.impl;

import com.finex.auth.dto.FinanceBankBranchVO;
import com.finex.auth.dto.FinanceBankOptionVO;
import com.finex.auth.service.bankcatalog.BankCatalogProvider;
import com.finex.auth.service.bankcatalog.BankCatalogProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceBankCatalogServiceImplTest {

    @Mock
    private BankCatalogProvider localProvider;
    @Mock
    private BankCatalogProvider wechatProvider;

    private FinanceBankCatalogServiceImpl service;

    @BeforeEach
    void setUp() {
        when(localProvider.getType()).thenReturn(BankCatalogProviderType.LOCAL);
        when(wechatProvider.getType()).thenReturn(BankCatalogProviderType.WECHAT_PAY);
        service = new FinanceBankCatalogServiceImpl(List.of(wechatProvider, localProvider));
    }

    @Test
    void listBanksUsesLocalProviderAsCurrentBaseline() {
        FinanceBankOptionVO option = new FinanceBankOptionVO();
        option.setBankCode("ICBC");
        option.setBusinessScope("BOTH");
        when(localProvider.listBanks("工行", "PRIVATE")).thenReturn(List.of(option));

        List<FinanceBankOptionVO> result = service.listBanks("工行", "PRIVATE");

        assertEquals(1, result.size());
        assertEquals("ICBC", result.get(0).getBankCode());
        verify(localProvider).listBanks("工行", "PRIVATE");
    }

    @Test
    void listBankBranchesDelegatesWithoutChangingResponseShape() {
        FinanceBankBranchVO branch = new FinanceBankBranchVO();
        branch.setBranchCode("ICBC-SH-PD");
        when(localProvider.listBankBranches("ICBC", "上海市", "上海市", "浦东", "PUBLIC"))
                .thenReturn(List.of(branch));

        List<FinanceBankBranchVO> result = service.listBankBranches("ICBC", "上海市", "上海市", "浦东", "PUBLIC");

        assertEquals("ICBC-SH-PD", result.get(0).getBranchCode());
        verify(localProvider).listBankBranches("ICBC", "上海市", "上海市", "浦东", "PUBLIC");
    }
}
