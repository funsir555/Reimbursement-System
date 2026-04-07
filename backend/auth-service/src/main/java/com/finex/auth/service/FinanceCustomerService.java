package com.finex.auth.service;

import com.finex.auth.dto.FinanceCustomerDetailVO;
import com.finex.auth.dto.FinanceCustomerSaveDTO;
import com.finex.auth.dto.FinanceCustomerSummaryVO;

import java.util.List;

public interface FinanceCustomerService {

    List<FinanceCustomerSummaryVO> listCustomers(String companyId, String keyword, Boolean includeDisabled);

    FinanceCustomerDetailVO getCustomerDetail(String companyId, String customerCode);

    FinanceCustomerDetailVO createCustomer(String companyId, FinanceCustomerSaveDTO dto, String operatorName);

    FinanceCustomerDetailVO updateCustomer(String companyId, String customerCode, FinanceCustomerSaveDTO dto, String operatorName);

    Boolean disableCustomer(String companyId, String customerCode, String operatorName);
}
