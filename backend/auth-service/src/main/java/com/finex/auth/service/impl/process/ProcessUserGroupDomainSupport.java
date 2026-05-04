package com.finex.auth.service.impl.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.ProcessUserGroupDetailVO;
import com.finex.auth.dto.ProcessUserGroupMetaVO;
import com.finex.auth.dto.ProcessUserGroupSaveDTO;
import com.finex.auth.dto.ProcessUserGroupTreeVO;
import com.finex.auth.entity.ProcessUserGroup;
import com.finex.auth.mapper.CodeSequenceMapper;
import com.finex.auth.mapper.ProcessCustomArchiveDesignMapper;
import com.finex.auth.mapper.ProcessCustomArchiveItemMapper;
import com.finex.auth.mapper.ProcessCustomArchiveRuleMapper;
import com.finex.auth.mapper.ProcessDocumentTemplateMapper;
import com.finex.auth.mapper.ProcessExpenseTypeMapper;
import com.finex.auth.mapper.ProcessTemplateCategoryMapper;
import com.finex.auth.mapper.ProcessTemplateScopeMapper;
import com.finex.auth.mapper.ProcessUserGroupMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;
import com.finex.auth.service.ProcessExpenseDetailDesignService;
import com.finex.auth.service.ProcessFlowDesignService;
import com.finex.auth.service.ProcessFormDesignService;

import java.util.List;
import java.util.Objects;

public class ProcessUserGroupDomainSupport extends AbstractProcessUserGroupSupport {

    public ProcessUserGroupDomainSupport(
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
            ObjectMapper objectMapper,
            ProcessUserGroupMapper processUserGroupMapper,
            SystemCompanyMapper systemCompanyMapper,
            ProcessUserGroupResolverSupport resolverSupport
    ) {
        super(
                categoryMapper,
                templateMapper,
                codeSequenceMapper,
                scopeMapper,
                customArchiveDesignMapper,
                customArchiveItemMapper,
                customArchiveRuleMapper,
                processExpenseTypeMapper,
                systemDepartmentMapper,
                userMapper,
                processFormDesignService,
                processExpenseDetailDesignService,
                processFlowDesignService,
                objectMapper,
                processUserGroupMapper,
                systemCompanyMapper,
                resolverSupport
        );
    }

    public List<ProcessUserGroupTreeVO> listUserGroupTree() {
        return buildTree(loadAllUserGroups());
    }

    public ProcessUserGroupMetaVO getUserGroupMeta() {
        return buildMeta();
    }

    public ProcessUserGroupDetailVO getUserGroupDetail(Long id) {
        return buildDetail(requireUserGroup(id));
    }

    public ProcessUserGroupDetailVO createUserGroup(ProcessUserGroupSaveDTO dto) {
        validateUserGroupSave(dto, null);
        ProcessUserGroup group = new ProcessUserGroup();
        applyUserGroup(group, dto, true);
        getProcessUserGroupMapper().insert(group);
        return buildDetail(requireUserGroup(group.getId()));
    }

    public ProcessUserGroupDetailVO updateUserGroup(Long id, ProcessUserGroupSaveDTO dto) {
        ProcessUserGroup existing = requireUserGroup(id);
        validateUserGroupSave(dto, existing);
        applyUserGroup(existing, dto, false);
        getProcessUserGroupMapper().updateById(existing);
        return buildDetail(requireUserGroup(existing.getId()));
    }

    public Boolean deleteUserGroup(Long id) {
        ProcessUserGroup existing = requireUserGroup(id);
        if (hasChildren(id)) {
            throw new IllegalStateException("\u5f53\u524d\u7528\u6237\u7ec4\u5b58\u5728\u4e0b\u7ea7\uff0c\u4e0d\u80fd\u5220\u9664");
        }
        if (Objects.equals(existing.getCodeLevel(), 2) && isReferencedByFlow(id)) {
            throw new IllegalStateException("\u5f53\u524d 2 \u7ea7\u7528\u6237\u7ec4\u5df2\u88ab\u5ba1\u6279\u6d41\u5f15\u7528\uff0c\u4e0d\u80fd\u5220\u9664");
        }
        return getProcessUserGroupMapper().deleteById(id) > 0;
    }
}
