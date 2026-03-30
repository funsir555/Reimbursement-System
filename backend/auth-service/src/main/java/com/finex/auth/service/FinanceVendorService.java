package com.finex.auth.service;

import com.finex.auth.dto.ExpenseCreateVendorOptionVO;
import com.finex.auth.dto.FinanceVendorDetailVO;
import com.finex.auth.dto.FinanceVendorSaveDTO;
import com.finex.auth.dto.FinanceVendorSummaryVO;

import java.util.List;

public interface FinanceVendorService {

    List<FinanceVendorSummaryVO> listVendors(String keyword, Boolean includeDisabled);

    FinanceVendorDetailVO getVendorDetail(String vendorCode);

    FinanceVendorDetailVO createVendor(FinanceVendorSaveDTO dto, String operatorName);

    FinanceVendorDetailVO updateVendor(String vendorCode, FinanceVendorSaveDTO dto, String operatorName);

    Boolean disableVendor(String vendorCode, String operatorName);

    List<ExpenseCreateVendorOptionVO> listActiveVendorOptions(String keyword);
}
