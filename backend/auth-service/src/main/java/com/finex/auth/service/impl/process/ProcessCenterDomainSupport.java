// 业务域：流程模板与流程配置
// 文件角色：领域规则支撑类
// 上下游关系：上游通常来自 流程管理页面对应的 Controller，下游会继续协调 流程模板、报销类型、自定义档案和发布状态。
// 风险提醒：改坏后最容易影响 审批路由、模板发布和后续单据流转。

package com.finex.auth.service.impl.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessCenterOverviewVO;
import com.finex.auth.dto.ProcessTemplateFormOptionsVO;
import com.finex.auth.dto.ProcessTemplateTypeVO;
import com.finex.auth.mapper.CodeSequenceMapper;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessCustomArchiveItemMapper;
import com.finex.auth.mapper.ProcessCustomArchiveRuleMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessTemplateCategoryMapper;
import com.finex.auth.mapper.ProcessTemplateScopeMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.ProcessExpenseDetailDesignService;
import com.finex.auth.service.ProcessFlowDesignService;
import com.finex.auth.service.ProcessFormDesignService;

import java.util.List;

/**
 * ProcessCenterDomainSupport：领域规则支撑类。
 * 承接 流程中心的核心业务规则。
 * 改这里时，要特别关注 审批路由、模板发布和后续单据流转是否会被一起带坏。
 */
public final class ProcessCenterDomainSupport extends AbstractProcessManagementSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public ProcessCenterDomainSupport(
            ProcessTemplateCategoryMapper categoryMapper,
            ProcessDocumentTemplateMapper templateMapper,
            CodeSequenceMapper codeSequenceMapper,
            ProcessTemplateScopeMapper scopeMapper,
            ProcessCustomArchiveDesignMapper customArchiveDesignMapper,
            ProcessCustomArchiveItemMapper customArchiveItemMapper,
            ProcessCustomArchiveRuleMapper customArchiveRuleMapper,
            ProcessExpenseTypeMapper processExpenseTypeMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper,
            ProcessFormDesignService processFormDesignService,
            ProcessExpenseDetailDesignService processExpenseDetailDesignService,
            ProcessFlowDesignService processFlowDesignService,
            ObjectMapper objectMapper
    ) {
        super(categoryMapper, templateMapper, codeSequenceMapper, scopeMapper, customArchiveDesignMapper, customArchiveItemMapper, customArchiveRuleMapper, processExpenseTypeMapper, systemDepartmentMapper, userMapper, processFormDesignService, processExpenseDetailDesignService, processFlowDesignService, objectMapper);
    }

    /**
     * 获取Overview。
     */
    public ProcessCenterOverviewVO getOverview() {
        return super.getOverview();
    }

    /**
     * 获取模板类型。
     */
    public List<ProcessTemplateTypeVO> getTemplateTypes() {
        return super.getTemplateTypes();
    }

    /**
     * 获取表单选项。
     */
    public ProcessTemplateFormOptionsVO getFormOptions(String templateType) {
        return super.getFormOptions(templateType);
    }
}
