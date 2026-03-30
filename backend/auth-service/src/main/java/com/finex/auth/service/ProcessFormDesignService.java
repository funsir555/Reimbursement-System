package com.finex.auth.service;

import com.finex.auth.dto.ProcessFormDesignDetailVO;
import com.finex.auth.dto.ProcessFormDesignSaveDTO;
import com.finex.auth.dto.ProcessFormDesignSummaryVO;
import com.finex.auth.dto.ProcessFormOptionVO;

import java.util.List;
import java.util.Map;

public interface ProcessFormDesignService {

    List<ProcessFormDesignSummaryVO> listFormDesigns(String templateType);

    ProcessFormDesignDetailVO getFormDesignDetail(Long id);

    ProcessFormDesignDetailVO createFormDesign(ProcessFormDesignSaveDTO dto);

    ProcessFormDesignDetailVO updateFormDesign(Long id, ProcessFormDesignSaveDTO dto);

    Boolean deleteFormDesign(Long id);

    List<ProcessFormOptionVO> listFormDesignOptions(String templateType);

    Map<String, String> formDesignLabelMap(String templateType);

    String resolveFormDesignCode(String formCode, String templateType);
}
