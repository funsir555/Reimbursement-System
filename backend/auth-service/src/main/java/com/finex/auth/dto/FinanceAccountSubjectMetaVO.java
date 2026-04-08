package com.finex.auth.dto;

import lombok.Data;

import java.util.List;

@Data
public class FinanceAccountSubjectMetaVO {

    private List<FinanceAccountSubjectOptionVO> subjectCategoryOptions;

    private List<FinanceAccountSubjectOptionVO> statusOptions;

    private List<FinanceAccountSubjectOptionVO> closeStatusOptions;

    private List<FinanceAccountSubjectOptionVO> yesNoOptions;
}
