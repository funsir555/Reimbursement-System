// 业务域：报销凭证生成与推送
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 报销单凭证生成接口和财务操作入口，下游会继续协调 凭证映射、推送记录和报销单凭证状态。
// 风险提醒：改坏后最容易影响 重复生成凭证、凭证内容错误和推送记录不一致。

package com.finex.auth.service.impl.expensevoucher;

import com.finex.auth.dto.ExpenseVoucherGenerationMetaVO;
import com.finex.auth.entity.ExpVoucherPushDocument;
import com.finex.auth.entity.ProcessDocumentInstance;
import com.finex.auth.entity.ProcessDocumentTemplate;
import com.finex.auth.entity.ProcessExpenseType;
import com.finex.auth.entity.SystemCompany;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ExpenseVoucherMetaSupport：通用支撑类。
 * 封装 报销单凭证这块可复用的业务能力。
 * 改这里时，要特别关注 重复生成凭证、凭证内容错误和推送记录不一致是否会被一起带坏。
 */
public class ExpenseVoucherMetaSupport extends AbstractExpenseVoucherGenerationSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public ExpenseVoucherMetaSupport(Dependencies dependencies) {
        super(dependencies);
    }

    /**
     * 获取元数据。
     */
    public ExpenseVoucherGenerationMetaVO getMeta(Long currentUserId) {
        List<SystemCompany> companies = listCompanies();
        List<ProcessDocumentTemplate> templates = listTemplates();
        List<ProcessExpenseType> expenseTypes = listExpenseTypes();
        List<ProcessDocumentInstance> approvedDocuments = listApprovedDocuments();
        Map<String, ExpVoucherPushDocument> pushMap = listPushDocuments().stream()
                .collect(Collectors.toMap(ExpVoucherPushDocument::getDocumentCode, Function.identity(), (left, right) -> left));

        ExpenseVoucherGenerationMetaVO meta = new ExpenseVoucherGenerationMetaVO();
        meta.setCompanyOptions(companies.stream().map(this::toCompanyOption).toList());
        meta.setTemplateOptions(templates.stream().map(this::toTemplateOption).toList());
        meta.setExpenseTypeOptions(expenseTypes.stream().map(this::toExpenseTypeOption).toList());
        meta.setAccountOptions(loadAccountOptions());
        meta.setVoucherTypeOptions(toOptions(VOUCHER_TYPE_SEEDS));
        meta.setPushStatusOptions(List.of(
                option(PUSH_STATUS_UNPUSHED, "寰呮帹閫?"),
                option(PUSH_STATUS_SUCCESS, "鎺ㄩ€佹垚鍔?"),
                option(PUSH_STATUS_FAILED, "鎺ㄩ€佸け璐?")
        ));
        meta.setDefaultCompanyId(resolveDefaultCompanyId(currentUserId, companies));
        meta.setLatestBatchNo(resolveLatestBatchNo());

        int pendingCount = 0;
        BigDecimal pendingAmount = ZERO;
        for (ProcessDocumentInstance document : approvedDocuments) {
            ExpVoucherPushDocument pushDocument = pushMap.get(document.getDocumentCode());
            if (pushDocument == null || !PUSH_STATUS_SUCCESS.equals(pushDocument.getPushStatus())) {
                pendingCount += 1;
                pendingAmount = pendingAmount.add(zero(document.getTotalAmount()));
            }
        }
        meta.setPendingPushCount(pendingCount);
        meta.setPendingPushAmount(pendingAmount);
        meta.setPushedVoucherCount((int) pushMap.values().stream().filter(item -> PUSH_STATUS_SUCCESS.equals(item.getPushStatus())).count());
        meta.setPushFailureCount((int) pushMap.values().stream().filter(item -> PUSH_STATUS_FAILED.equals(item.getPushStatus())).count());
        return meta;
    }
}
