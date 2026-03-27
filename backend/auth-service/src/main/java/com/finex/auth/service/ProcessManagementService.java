package com.finex.auth.service;

import com.finex.auth.dto.ProcessCenterOverviewVO;
import com.finex.auth.dto.ProcessCustomArchiveDetailVO;
import com.finex.auth.dto.ProcessCustomArchiveMetaVO;
import com.finex.auth.dto.ProcessCustomArchiveResolveDTO;
import com.finex.auth.dto.ProcessCustomArchiveResolveResultVO;
import com.finex.auth.dto.ProcessCustomArchiveSaveDTO;
import com.finex.auth.dto.ProcessCustomArchiveSummaryVO;
import com.finex.auth.dto.ProcessExpenseTypeDetailVO;
import com.finex.auth.dto.ProcessExpenseTypeMetaVO;
import com.finex.auth.dto.ProcessExpenseTypeSaveDTO;
import com.finex.auth.dto.ProcessExpenseTypeTreeVO;
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

    List<ProcessCustomArchiveSummaryVO> listCustomArchives();

    ProcessCustomArchiveDetailVO getCustomArchiveDetail(Long id);

    ProcessCustomArchiveDetailVO createCustomArchive(ProcessCustomArchiveSaveDTO dto);

    ProcessCustomArchiveDetailVO updateCustomArchive(Long id, ProcessCustomArchiveSaveDTO dto);

    Boolean updateCustomArchiveStatus(Long id, Integer status);

    Boolean deleteCustomArchive(Long id);

    ProcessCustomArchiveMetaVO getCustomArchiveMeta();

    ProcessCustomArchiveResolveResultVO resolveCustomArchive(ProcessCustomArchiveResolveDTO dto);

    List<ProcessExpenseTypeTreeVO> listExpenseTypeTree();

    ProcessExpenseTypeMetaVO getExpenseTypeMeta();

    ProcessExpenseTypeDetailVO getExpenseTypeDetail(Long id);

    ProcessExpenseTypeDetailVO createExpenseType(ProcessExpenseTypeSaveDTO dto);

    ProcessExpenseTypeDetailVO updateExpenseType(Long id, ProcessExpenseTypeSaveDTO dto);

    Boolean updateExpenseTypeStatus(Long id, Integer status);

    Boolean deleteExpenseType(Long id);
}
