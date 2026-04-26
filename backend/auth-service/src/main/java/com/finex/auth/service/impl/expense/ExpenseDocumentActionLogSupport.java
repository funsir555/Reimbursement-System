// 业务域：报销单录入、流转与查询
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 报销单页面、审批页面、付款页面对应的 Controller，下游会继续协调 报销单、流程节点、附件、付款与核销等数据。
// 风险提醒：改坏后最容易影响 单据状态、审批链、金额结果和重复提交。

package com.finex.auth.service.impl.expense;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.entity.ProcessDocumentActionLog;
import com.finex.auth.mapper.ProcessDocumentActionLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ExpenseDocumentActionLogSupport：通用支撑类。
 * 封装 报销单单据ActionLog这块可复用的业务能力。
 * 改这里时，要特别关注 单据状态、审批链、金额结果和重复提交是否会被一起带坏。
 */
@Service
@RequiredArgsConstructor
class ExpenseDocumentActionLogSupport {

    private static final int PM_NAME_MAX_LENGTH = 64;

    private final ProcessDocumentActionLogMapper processDocumentActionLogMapper;
    private final ObjectMapper objectMapper;

    /**
     * 初始化这个类所需的依赖组件。
     */
    void appendLog(
            String documentCode,
            String nodeKey,
            String nodeName,
            String actionType,
            Long operatorUserId,
            String operatorName,
            String actionComment,
            Map<String, Object> payload
    ) {
        validatePmNameLength(nodeName, "节点名称");
        validatePmNameLength(operatorName, "操作人姓名");
        ProcessDocumentActionLog log = new ProcessDocumentActionLog();
        log.setDocumentCode(documentCode);
        log.setNodeKey(nodeKey);
        log.setNodeName(nodeName);
        log.setActionType(actionType);
        log.setActorUserId(operatorUserId);
        log.setActorName(operatorName);
        log.setActionComment(trimToNull(actionComment));
        log.setPayloadJson(payload == null || payload.isEmpty() ? null : writeJson(payload));
        log.setCreatedAt(LocalDateTime.now());
        processDocumentActionLogMapper.insert(log);
    }

    List<ProcessDocumentActionLog> loadActionLogs(String documentCode) {
        return processDocumentActionLogMapper.selectList(
                Wrappers.<ProcessDocumentActionLog>lambdaQuery()
                        .eq(ProcessDocumentActionLog::getDocumentCode, documentCode)
                        .orderByAsc(ProcessDocumentActionLog::getCreatedAt, ProcessDocumentActionLog::getId)
        );
    }

    private void validatePmNameLength(String value, String fieldName) {
        if (value != null && value.length() > PM_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(fieldName + "长度不能超过" + PM_NAME_MAX_LENGTH + "个字符");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("数据序列化失败", ex);
        }
    }
}
