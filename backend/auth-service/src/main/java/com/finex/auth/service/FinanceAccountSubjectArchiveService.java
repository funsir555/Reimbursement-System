package com.finex.auth.service;

import com.finex.auth.dto.FinanceAccountSubjectCloseDTO;
import com.finex.auth.dto.FinanceAccountSubjectDetailVO;
import com.finex.auth.dto.FinanceAccountSubjectMetaVO;
import com.finex.auth.dto.FinanceAccountSubjectSaveDTO;
import com.finex.auth.dto.FinanceAccountSubjectStatusDTO;
import com.finex.auth.dto.FinanceAccountSubjectSummaryVO;

import java.util.List;

public interface FinanceAccountSubjectArchiveService {

    FinanceAccountSubjectMetaVO getMeta();

    List<FinanceAccountSubjectSummaryVO> listSubjects(String companyId, String keyword, String subjectCategory, Integer status, Integer bclose);

    FinanceAccountSubjectDetailVO getSubjectDetail(String companyId, String subjectCode);

    FinanceAccountSubjectDetailVO createSubject(String companyId, FinanceAccountSubjectSaveDTO dto, String operatorName);

    FinanceAccountSubjectDetailVO updateSubject(String companyId, String subjectCode, FinanceAccountSubjectSaveDTO dto, String operatorName);

    Boolean updateStatus(String companyId, String subjectCode, FinanceAccountSubjectStatusDTO dto, String operatorName);

    Boolean updateCloseStatus(String companyId, String subjectCode, FinanceAccountSubjectCloseDTO dto, String operatorName);
}
