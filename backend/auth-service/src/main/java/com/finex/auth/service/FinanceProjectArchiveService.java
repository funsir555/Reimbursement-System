// 业务域：财务档案
// 文件角色：service 接口
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

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

/**
 * FinanceProjectArchiveService：service 接口。
 * 定义财务项目档案这块对外提供的业务入口能力。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
public interface FinanceProjectArchiveService {

    /**
     * 获取元数据。
     */
    FinanceProjectArchiveMetaVO getMeta(String companyId);

    /**
     * 查询项目Classes列表。
     */
    List<FinanceProjectClassSummaryVO> listProjectClasses(String companyId, String keyword, Integer status);

    /**
     * 创建项目Class。
     */
    FinanceProjectClassSummaryVO createProjectClass(String companyId, FinanceProjectClassSaveDTO dto, String operatorName);

    /**
     * 更新项目Class。
     */
    FinanceProjectClassSummaryVO updateProjectClass(String companyId, String projectClassCode, FinanceProjectClassSaveDTO dto, String operatorName);

    /**
     * 更新项目ClassStatus。
     */
    Boolean updateProjectClassStatus(String companyId, String projectClassCode, FinanceProjectStatusDTO dto, String operatorName);

    /**
     * 查询项目列表。
     */
    List<FinanceProjectSummaryVO> listProjects(String companyId, String keyword, String projectClassCode, Integer status, Integer bclose);

    /**
     * 获取项目明细。
     */
    FinanceProjectDetailVO getProjectDetail(String companyId, String projectCode);

    /**
     * 创建项目。
     */
    FinanceProjectDetailVO createProject(String companyId, FinanceProjectSaveDTO dto, String operatorName);

    /**
     * 更新项目。
     */
    FinanceProjectDetailVO updateProject(String companyId, String projectCode, FinanceProjectSaveDTO dto, String operatorName);

    /**
     * 更新项目Status。
     */
    Boolean updateProjectStatus(String companyId, String projectCode, FinanceProjectStatusDTO dto, String operatorName);

    /**
     * 更新项目CloseStatus。
     */
    Boolean updateProjectCloseStatus(String companyId, String projectCode, FinanceProjectCloseDTO dto, String operatorName);
}
