package com.finex.auth.service;

import com.finex.auth.dto.ProcessFlowDetailVO;
import com.finex.auth.dto.ProcessFlowMetaVO;
import com.finex.auth.dto.ProcessFlowResolveApproversDTO;
import com.finex.auth.dto.ProcessFlowResolveApproversVO;
import com.finex.auth.dto.ProcessFlowSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneSaveDTO;
import com.finex.auth.dto.ProcessFlowSceneVO;
import com.finex.auth.dto.ProcessFlowSummaryVO;
import com.finex.auth.dto.ProcessFormOptionVO;

import java.util.List;
import java.util.Map;

public interface ProcessFlowDesignService {

    List<ProcessFlowSummaryVO> listFlows();

    ProcessFlowMetaVO getFlowMeta();

    ProcessFlowDetailVO getFlowDetail(Long id);

    ProcessFlowDetailVO createFlow(ProcessFlowSaveDTO dto);

    ProcessFlowDetailVO updateFlow(Long id, ProcessFlowSaveDTO dto);

    ProcessFlowDetailVO publishFlow(Long id);

    Boolean updateFlowStatus(Long id, String status);

    ProcessFlowSceneVO createFlowScene(ProcessFlowSceneSaveDTO dto);

    ProcessFlowResolveApproversVO resolveApprovers(ProcessFlowResolveApproversDTO dto);

    List<ProcessFormOptionVO> listPublishedFlowOptions();

    Map<String, String> publishedFlowLabelMap();
}
