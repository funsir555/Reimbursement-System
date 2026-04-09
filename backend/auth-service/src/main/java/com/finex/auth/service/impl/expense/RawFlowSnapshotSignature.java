package com.finex.auth.service.impl.expense;

record RawFlowSnapshotSignature(
        boolean hasApprovalNode,
        boolean hasBlankRootNode,
        boolean hasNullRootNode
) {
}
