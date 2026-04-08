package com.finex.auth.service;

import com.finex.auth.dto.FinanceProjectArchiveMetaVO;
import com.finex.auth.dto.FinanceProjectClassSaveDTO;
import com.finex.auth.dto.FinanceProjectClassSummaryVO;
import com.finex.auth.dto.FinanceProjectCloseDTO;
import com.finex.auth.dto.FinanceProjectDetailVO;
import com.finex.auth.dto.FinanceProjectSaveDTO;
import com.finex.auth.dto.FinanceProjectStatusDTO;
import com.finex.auth.dto.FinanceProjectSummaryVO;

import java.util.List;

public interface FinanceProjectArchiveService {

    FinanceProjectArchiveMetaVO getMeta(String companyId);

    List<FinanceProjectClassSummaryVO> listProjectClasses(String companyId, String keyword, Integer status);

    FinanceProjectClassSummaryVO createProjectClass(String companyId, FinanceProjectClassSaveDTO dto, String operatorName);

    FinanceProjectClassSummaryVO updateProjectClass(String companyId, String projectClassCode, FinanceProjectClassSaveDTO dto, String operatorName);

    Boolean updateProjectClassStatus(String companyId, String projectClassCode, FinanceProjectStatusDTO dto, String operatorName);

    List<FinanceProjectSummaryVO> listProjects(String companyId, String keyword, String projectClassCode, Integer status, Integer bclose);

    FinanceProjectDetailVO getProjectDetail(String companyId, String projectCode);

    FinanceProjectDetailVO createProject(String companyId, FinanceProjectSaveDTO dto, String operatorName);

    FinanceProjectDetailVO updateProject(String companyId, String projectCode, FinanceProjectSaveDTO dto, String operatorName);

    Boolean updateProjectStatus(String companyId, String projectCode, FinanceProjectStatusDTO dto, String operatorName);

    Boolean updateProjectCloseStatus(String companyId, String projectCode, FinanceProjectCloseDTO dto, String operatorName);
}
