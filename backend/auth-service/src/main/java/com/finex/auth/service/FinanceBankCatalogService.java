package com.finex.auth.service;

import com.finex.auth.dto.FinanceBankBranchVO;
import com.finex.auth.dto.FinanceBankOptionVO;

import java.util.List;

public interface FinanceBankCatalogService {

    List<FinanceBankOptionVO> listBanks(String keyword);

    List<FinanceBankBranchVO> listBankBranches(String bankCode, String province, String city, String keyword);

    FinanceBankBranchVO lookupBranchByCnaps(String cnapsCode);
}
