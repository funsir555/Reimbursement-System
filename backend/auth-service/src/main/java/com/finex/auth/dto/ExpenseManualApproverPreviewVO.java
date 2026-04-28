package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExpenseManualApproverPreviewVO {

    private List<ExpenseApprovalTimelineItemVO> approvalTimeline = new ArrayList<>();

    private List<ExpenseManualApproverPreviewNodeVO> manualNodes = new ArrayList<>();
}
