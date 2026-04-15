package com.finex.auth.service;

import com.finex.auth.dto.FinanceDepartmentArchiveMetaVO;
import com.finex.auth.dto.FinanceDepartmentQueryDTO;
import com.finex.auth.dto.FinanceDepartmentVO;

import java.util.List;

public interface FinanceDepartmentArchiveService {

    FinanceDepartmentArchiveMetaVO getMeta();

    List<FinanceDepartmentVO> queryDepartments(FinanceDepartmentQueryDTO query);
}
