package com.finex.auth.service;

import com.finex.auth.dto.ProcessExpenseDetailDesignDetailVO;
import com.finex.auth.dto.ProcessExpenseDetailDesignSaveDTO;
import com.finex.auth.dto.ProcessExpenseDetailDesignSummaryVO;

import java.util.List;
import java.util.Map;

public interface ProcessExpenseDetailDesignService {

    List<ProcessExpenseDetailDesignSummaryVO> listExpenseDetailDesigns();

    ProcessExpenseDetailDesignDetailVO getExpenseDetailDesignDetail(Long id);

    ProcessExpenseDetailDesignDetailVO createExpenseDetailDesign(ProcessExpenseDetailDesignSaveDTO dto);

    ProcessExpenseDetailDesignDetailVO updateExpenseDetailDesign(Long id, ProcessExpenseDetailDesignSaveDTO dto);

    Boolean deleteExpenseDetailDesign(Long id);

    Map<String, String> detailDesignLabelMap();

    String resolveExpenseDetailDesignCode(String detailCode);

    String resolveExpenseDetailType(String detailCode);
}
