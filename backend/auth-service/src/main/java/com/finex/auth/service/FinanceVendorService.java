package com.finex.auth.service;

import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.FinanceVendorDetailVO;
import com.finex.auth.dto.FinanceVendorSaveDTO;
import com.finex.auth.dto.FinanceVendorSummaryVO;

import java.util.List;

public interface FinanceVendorService {

    List<FinanceVendorSummaryVO> listVendors(String companyId, String keyword, Boolean includeDisabled);

    FinanceVendorDetailVO getVendorDetail(String companyId, String vendorCode);

    FinanceVendorDetailVO createVendor(String companyId, FinanceVendorSaveDTO dto, String operatorName);

    FinanceVendorDetailVO createVendor(Long currentUserId, FinanceVendorSaveDTO dto, String operatorName);

    FinanceVendorDetailVO updateVendor(String companyId, String vendorCode, FinanceVendorSaveDTO dto, String operatorName);

    Boolean disableVendor(String companyId, String vendorCode, String operatorName);

    List<ExpenseCreateVendorOptionVO> listActiveVendorOptions(String companyId, String keyword);
}
