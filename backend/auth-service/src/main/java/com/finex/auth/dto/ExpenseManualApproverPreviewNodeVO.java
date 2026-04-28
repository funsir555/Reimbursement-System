package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExpenseManualApproverPreviewNodeVO {

    private String nodeKey;

    private String nodeName;

    private String nodeType;

    private boolean required = true;

    private List<ProcessFormOptionVO> candidateOptions = new ArrayList<>();

    private List<Long> selectedUserIds = new ArrayList<>();
}
