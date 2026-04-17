// 业务域：财务档案
// 文件角色：service 接口
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

package com.finex.auth.service;

import com.finex.auth.dto.FinanceAccountSubjectCloseDTO;
import com.finex.auth.dto.FinanceAccountSubjectDerivedDefaultsVO;
import com.finex.auth.dto.FinanceAccountSubjectDetailVO;
import com.finex.auth.dto.FinanceAccountSubjectMetaVO;
import com.finex.auth.dto.FinanceAccountSubjectSaveDTO;
import com.finex.auth.dto.FinanceAccountSubjectStatusDTO;
import com.finex.auth.dto.FinanceAccountSubjectSummaryVO;

import java.util.List;

/**
 * FinanceAccountSubjectArchiveService：service 接口。
 * 定义财务账户科目档案这块对外提供的业务入口能力。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
public interface FinanceAccountSubjectArchiveService {

    /**
     * 获取元数据。
     */
    FinanceAccountSubjectMetaVO getMeta();

    /**
     * 查询科目列表。
     */
    List<FinanceAccountSubjectSummaryVO> listSubjects(String companyId, String keyword, String subjectCategory, Integer status, Integer bclose);

    /**
     * 获取科目明细。
     */
    FinanceAccountSubjectDetailVO getSubjectDetail(String companyId, String subjectCode);

    FinanceAccountSubjectDerivedDefaultsVO getDerivedDefaults(String companyId, String subjectCode);

    /**
     * 创建科目。
     */
    FinanceAccountSubjectDetailVO createSubject(String companyId, FinanceAccountSubjectSaveDTO dto, String operatorName);

    /**
     * 更新科目。
     */
    FinanceAccountSubjectDetailVO updateSubject(String companyId, String subjectCode, FinanceAccountSubjectSaveDTO dto, String operatorName);

    /**
     * 更新Status。
     */
    Boolean updateStatus(String companyId, String subjectCode, FinanceAccountSubjectStatusDTO dto, String operatorName);

    /**
     * 更新CloseStatus。
     */
    Boolean updateCloseStatus(String companyId, String subjectCode, FinanceAccountSubjectCloseDTO dto, String operatorName);
}
