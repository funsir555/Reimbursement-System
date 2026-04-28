package com.finex.auth.service.impl.postvoucher;

import com.finex.auth.dto.FinancePostVoucherTaskStatusVO;
import com.finex.auth.entity.AsyncTaskRecord;
import com.finex.auth.entity.GlAccvouch;
import java.util.List;

public class PostVoucherMutationSupport {

    private final SharedPostVoucherSupport support;

    public PostVoucherMutationSupport(SharedPostVoucherSupport support) {
        this.support = support;
    }

    public FinancePostVoucherTaskStatusVO getTaskStatus(String taskNo) {
        return support.buildTaskStatus(taskNo);
    }

    public void markPostingStarted(PostVoucherRunContext context, AsyncTaskRecord task) {
        support.markPostingStarted(context, task);
    }

    public void postVoucherGroup(AbstractFinancePostVoucherSupport.VoucherKey voucherKey, List<GlAccvouch> rows, String operatorName) {
        support.postVoucherGroup(voucherKey, rows, operatorName);
    }

    public void markPostingSuccess(PostVoucherRunContext context, String operatorName, String taskNo) {
        support.markPostingSuccess(context, operatorName, taskNo);
    }

    public void markPostingFailed(PostVoucherRunContext context, String taskNo, String errorMessage) {
        support.markPostingFailed(context, taskNo, errorMessage);
    }
}
