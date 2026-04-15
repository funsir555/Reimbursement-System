// 业务域：报销单录入、流转与查询
// 文件角色：service 接口
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service;

import com.finex.auth.dto.ExpenseAttachmentVO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * ExpenseAttachmentService：service 接口。
 * 定义报销单附件这块对外提供的业务入口能力。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
public interface ExpenseAttachmentService {

    /**
     * 上传附件。
     */
    ExpenseAttachmentVO uploadAttachment(MultipartFile file);

    /**
     * 加载附件。
     */
    StoredExpenseAttachment loadAttachment(String attachmentId);

    /**
     * 保存Generated附件。
     */
    ExpenseAttachmentVO saveGeneratedAttachment(String fileName, String contentType, byte[] content);

    record StoredExpenseAttachment(Resource resource, String fileName, String contentType, long fileSize) {
    }
}
