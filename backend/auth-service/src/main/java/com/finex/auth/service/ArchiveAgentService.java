package com.finex.auth.service;

import com.finex.auth.dto.ArchiveAgentDetailVO;
import com.finex.auth.dto.ArchiveAgentMetaVO;
import com.finex.auth.dto.ArchiveAgentRunDTO;
import com.finex.auth.dto.ArchiveAgentRunDetailVO;
import com.finex.auth.dto.ArchiveAgentRunVO;
import com.finex.auth.dto.ArchiveAgentSaveDTO;
import com.finex.auth.dto.ArchiveAgentSummaryVO;

import java.util.List;

public interface ArchiveAgentService {

    List<ArchiveAgentSummaryVO> listAgents(Long ownerUserId, String keyword, String status);

    ArchiveAgentMetaVO getMeta();

    ArchiveAgentDetailVO getAgentDetail(Long ownerUserId, Long id);

    ArchiveAgentDetailVO createAgent(Long ownerUserId, String operatorName, ArchiveAgentSaveDTO dto);

    ArchiveAgentDetailVO updateAgent(Long ownerUserId, Long id, String operatorName, ArchiveAgentSaveDTO dto);

    ArchiveAgentDetailVO publishAgent(Long ownerUserId, Long id, String operatorName);

    ArchiveAgentDetailVO updateAgentStatus(Long ownerUserId, Long id, String status);

    ArchiveAgentRunVO runAgent(Long ownerUserId, Long id, ArchiveAgentRunDTO dto);

    List<ArchiveAgentRunVO> listRuns(Long ownerUserId, Long agentId);

    ArchiveAgentRunDetailVO getRunDetail(Long ownerUserId, Long runId);

    void runDueSchedules();
}
