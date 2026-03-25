package com.finex.auth.service;

import com.finex.auth.dto.ProcessCenterOverviewVO;
import com.finex.auth.dto.ProcessTemplateFormOptionsVO;
import com.finex.auth.dto.ProcessTemplateSaveDTO;
import com.finex.auth.dto.ProcessTemplateSaveResultVO;
import com.finex.auth.dto.ProcessTemplateTypeVO;

import java.util.List;

public interface ProcessManagementService {

    ProcessCenterOverviewVO getOverview();

    List<ProcessTemplateTypeVO> getTemplateTypes();

    ProcessTemplateFormOptionsVO getFormOptions(String templateType);

    ProcessTemplateSaveResultVO saveTemplate(ProcessTemplateSaveDTO dto, String operatorName);
}
