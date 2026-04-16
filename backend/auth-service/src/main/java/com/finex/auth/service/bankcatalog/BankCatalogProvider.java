package com.finex.auth.service.bankcatalog;

import com.finex.auth.dto.FinanceBankBranchVO;
import com.finex.auth.dto.FinanceBankOptionVO;

import java.util.List;

public interface BankCatalogProvider {

    BankCatalogProviderType getType();

    List<FinanceBankOptionVO> listBanks(String keyword, String businessScope);

    List<String> listProvinces(String bankCode, String businessScope);

    List<String> listCities(String bankCode, String province, String businessScope);

    List<FinanceBankBranchVO> listBankBranches(String bankCode, String province, String city, String keyword, String businessScope);

    FinanceBankBranchVO lookupBranchByCnaps(String cnapsCode);
}
