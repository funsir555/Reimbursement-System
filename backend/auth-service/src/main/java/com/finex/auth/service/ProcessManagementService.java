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
import com.finex.auth.dto.ProcessExpenseDetailDesignDetailVO;
import com.finex.auth.dto.ProcessExpenseDetailDesignSaveDTO;
import com.finex.auth.dto.ProcessExpenseDetailDesignSummaryVO;
import com.finex.auth.dto.ProcessFormDesignDetailVO;
import com.finex.auth.dto.ProcessFormDesignSaveDTO;
import com.finex.auth.dto.ProcessFormDesignSummaryVO;
import com.finex.auth.dto.ProcessFlowDetailVO;
import com.finex.auth.dto.ProcessFlowMetaVO;
import com.finex.auth.dto.ProcessFlowResolveApproversDTO;
import com.finex.auth.dto.ProcessFlowResolveApproversVO;
import com.finex.auth.dto.ProcessFlowSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneVO;
import com.finex.auth.dto.ProcessFlowSummaryVO;
import com.finex.auth.dto.ProcessTemplateDetailVO;
import com.finex.auth.dto.ProcessTemplateFormOptionsVO;
import com.finex.auth.dto.ProcessTemplateSaveDTO;
import com.finex.auth.dto.ProcessTemplateSaveResultVO;
import com.finex.auth.dto.ProcessTemplateTypeVO;

import java.util.List;

public interface ProcessManagementService {

    ProcessCenterOverviewVO getOverview();

    List<ProcessTemplateTypeVO> getTemplateTypes();

    ProcessTemplateFormOptionsVO getFormOptions(String templateType);

    ProcessTemplateDetailVO getTemplateDetail(Long id);

    ProcessTemplateSaveResultVO saveTemplate(ProcessTemplateSaveDTO dto, String operatorName);

    ProcessTemplateSaveResultVO updateTemplate(Long id, ProcessTemplateSaveDTO dto, String operatorName);

    Boolean deleteTemplate(Long id);

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

    List<ProcessExpenseDetailDesignSummaryVO> listExpenseDetailDesigns();

    ProcessExpenseDetailDesignDetailVO getExpenseDetailDesignDetail(Long id);

    ProcessExpenseDetailDesignDetailVO createExpenseDetailDesign(ProcessExpenseDetailDesignSaveDTO dto);

    ProcessExpenseDetailDesignDetailVO updateExpenseDetailDesign(Long id, ProcessExpenseDetailDesignSaveDTO dto);

    Boolean deleteExpenseDetailDesign(Long id);

    List<ProcessFormDesignSummaryVO> listFormDesigns(String templateType);

    ProcessFormDesignDetailVO getFormDesignDetail(Long id);

    ProcessFormDesignDetailVO createFormDesign(ProcessFormDesignSaveDTO dto);

    ProcessFormDesignDetailVO updateFormDesign(Long id, ProcessFormDesignSaveDTO dto);

    Boolean deleteFormDesign(Long id);

    List<ProcessFlowSummaryVO> listFlows();

    ProcessFlowMetaVO getFlowMeta();

    ProcessFlowDetailVO getFlowDetail(Long id);

    ProcessFlowDetailVO createFlow(ProcessFlowSaveDTO dto);

    ProcessFlowDetailVO updateFlow(Long id, ProcessFlowSaveDTO dto);

    ProcessFlowDetailVO publishFlow(Long id);

    Boolean updateFlowStatus(Long id, String status);

    ProcessFlowSceneVO createFlowScene(ProcessFlowSceneSaveDTO dto);

    ProcessFlowResolveApproversVO resolveFlowApprovers(ProcessFlowResolveApproversDTO dto);
}
