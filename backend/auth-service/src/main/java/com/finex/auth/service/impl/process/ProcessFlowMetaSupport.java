package com.finex.auth.service.impl.process;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessFlowMetaVO;
import com.finex.auth.dto.ProcessFormOptionVO;
import com.finex.auth.entity.ProcessFlow;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessFlowMapper;
import com.finex.auth.mapper.ProcessFlowNodeMapper;
import com.finex.auth.mapper.ProcessFlowRouteMapper;
import com.finex.auth.mapper.ProcessFlowSceneMapper;
import com.finex.auth.mapper.ProcessFlowVersionMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProcessFlowMetaSupport extends AbstractProcessFlowDesignSupport {

    public ProcessFlowMetaSupport(
            ProcessFlowMapper processFlowMapper,
            ProcessFlowVersionMapper processFlowVersionMapper,
            ProcessFlowNodeMapper processFlowNodeMapper,
            ProcessFlowRouteMapper processFlowRouteMapper,
            ProcessFlowSceneMapper processFlowSceneMapper,
            SystemCompanyMapper systemCompanyMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper,
            ProcessExpenseTypeMapper processExpenseTypeMapper,
            ProcessCustomArchiveDesignMapper processCustomArchiveDesignMapper,
            ProcessDocumentTemplateMapper processDocumentTemplateMapper,
            ObjectMapper objectMapper
    ) {
        super(
                processFlowMapper,
                processFlowVersionMapper,
                processFlowNodeMapper,
                processFlowRouteMapper,
                processFlowSceneMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                userMapper,
                processExpenseTypeMapper,
                processCustomArchiveDesignMapper,
                processDocumentTemplateMapper,
                objectMapper
        );
    }

    public ProcessFlowMetaVO getFlowMeta() {
        ProcessFlowMetaVO meta = new ProcessFlowMetaVO();
        meta.setNodeTypeOptions(List.of(
                option("审批节点", NODE_TYPE_APPROVAL),
                option("抄送节点", NODE_TYPE_CC),
                option("支付节点", NODE_TYPE_PAYMENT),
                option("流程分支", NODE_TYPE_BRANCH)
        ));
        meta.setSceneOptions(loadSceneOptions());
        meta.setApprovalApproverTypeOptions(List.of(
                option("指定主管", APPROVER_TYPE_MANAGER),
                option("指定成员", APPROVER_TYPE_DESIGNATED_MEMBER),
                option("手动选择", APPROVER_TYPE_MANUAL_SELECT)
        ));
        meta.setApprovalManagerRuleModeOptions(List.of(
                option("根据表单上的部门查找指定主管", MANAGER_RULE_MODE_FORM_DEPT_MANAGER)
        ));
        meta.setApprovalManagerDeptSourceOptions(List.of(
                option("承担部门", DEPT_SOURCE_UNDERTAKE),
                option("提单人部门", DEPT_SOURCE_SUBMITTER)
        ));
        meta.setApprovalManagerLevelOptions(buildLevelOptions("第 %s 级主管"));
        meta.setApprovalManagerLookupLevelOptions(buildLevelOptions("第 %s 级"));
        meta.setApprovalManualCandidateScopeOptions(List.of(
                option("全部有效用户", MANUAL_SCOPE_ALL_ACTIVE_USERS)
        ));
        meta.setCcReceiverTypeOptions(List.of(
                option("指定成员", "DESIGNATED_MEMBER"),
                option("提单人", "SUBMITTER"),
                option("部门主管", "DEPT_MANAGER")
        ));
        meta.setPaymentExecutorTypeOptions(List.of(
                option("指定成员", "DESIGNATED_MEMBER"),
                option("财务角色", "FINANCE_ROLE"),
                option("提单人", "SUBMITTER")
        ));
        meta.setMissingHandlerOptions(List.of(
                option("自动跳过", MISSING_HANDLER_AUTO_SKIP),
                option("作为异常流程处理", MISSING_HANDLER_EXCEPTION),
                option("自动转交", MISSING_HANDLER_AUTO_TRANSFER),
                option("提单时找不到审批人不允许提交", MISSING_HANDLER_BLOCK_SUBMIT)
        ));
        meta.setApprovalModeOptions(List.of(
                option("或签", APPROVAL_MODE_OR_SIGN),
                option("会签", APPROVAL_MODE_AND_SIGN)
        ));
        meta.setDefaultApprovalOpinions(new ArrayList<>(DEFAULT_OPINIONS));
        meta.setApprovalSpecialOptions(List.of(
                configOption("AUTO_PASS_IF_APPOVER_IS_SUBMITTER", "审批人与提单人重复时自动通过", ""),
                configOption("AUTO_PASS_IF_APPROVED_BEFORE", "审批人已在前面节点审批过时自动通过", ""),
                configOption("DIRECT_REACH_AFTER_RESUBMIT", "驳回后再提交允许直达本节点", ""),
                configOption("REJECT_TO_ANY_NODE", "本节点可以驳回至任意节点", ""),
                configOption("DIRECT_REACH_AFTER_ANY_REJECT", "驳回至任意节点后再提交允许直达本节点", ""),
                configOption("ALLOW_EDIT_PAY_ACCOUNT", "可授权提单人修改收款账户", ""),
                configOption("ALLOW_EDIT_FORM_MODULE", "本节点允许修改开了修改权限的表单模块", "")
        ));
        meta.setCcTimingOptions(List.of(
                option("进入节点时", "ON_ENTER"),
                option("通过后", "ON_APPROVED")
        ));
        meta.setCcSpecialOptions(List.of(
                configOption("SEND_ONCE", "同一对象仅发送一次", ""),
                configOption("INCLUDE_SUBMITTER", "包含提单人", "")
        ));
        meta.setPaymentActionOptions(List.of(
                option("生成付款任务", "GENERATE_PAYMENT"),
                option("确认已付款", "CONFIRM_PAYMENT")
        ));
        meta.setPaymentSpecialOptions(List.of(
                configOption("ALLOW_RETRY", "支付失败允许重试", ""),
                configOption("REQUIRE_RESULT_FEEDBACK", "要求回写支付结果", "")
        ));
        meta.setBranchOperatorOptions(List.of(
                option("等于", "EQ"),
                option("不等于", "NE"),
                option("属于", "IN"),
                option("不属于", "NOT_IN"),
                option("大于", "GT"),
                option("大于等于", "GE"),
                option("小于", "LT"),
                option("小于等于", "LE"),
                option("介于", "BETWEEN"),
                option("包含", "CONTAINS")
        ));
        meta.setBranchConditionFields(buildConditionFields());
        meta.setCompanyOptions(loadCompanyOptions());
        meta.setDepartmentOptions(loadDepartmentOptions());
        meta.setUserOptions(loadUserOptions());
        meta.setExpenseTypeOptions(loadExpenseTypeOptions());
        meta.setArchiveOptions(loadArchiveOptions());
        return meta;
    }

    public List<ProcessFormOptionVO> listPublishedFlowOptions() {
        return processFlowMapper.selectList(
                Wrappers.<ProcessFlow>lambdaQuery()
                        .eq(ProcessFlow::getStatus, FLOW_STATUS_ENABLED)
                        .isNotNull(ProcessFlow::getCurrentPublishedVersionId)
                        .orderByDesc(ProcessFlow::getUpdatedAt, ProcessFlow::getId)
        ).stream().map(item -> option(item.getFlowName(), item.getFlowCode())).toList();
    }

    public Map<String, String> publishedFlowLabelMap() {
        return processFlowMapper.selectList(
                Wrappers.<ProcessFlow>lambdaQuery()
                        .eq(ProcessFlow::getStatus, FLOW_STATUS_ENABLED)
                        .isNotNull(ProcessFlow::getCurrentPublishedVersionId)
        ).stream().collect(Collectors.toMap(
                ProcessFlow::getFlowCode,
                ProcessFlow::getFlowName,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }
}
