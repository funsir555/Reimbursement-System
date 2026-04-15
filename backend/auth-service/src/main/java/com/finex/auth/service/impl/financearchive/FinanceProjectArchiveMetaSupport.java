// 业务域：财务档案
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

package com.finex.auth.service.impl.financearchive;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.FinanceProjectArchiveMetaVO;
import com.finex.auth.entity.FinanceProjectClass;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;

/**
 * FinanceProjectArchiveMetaSupport：通用支撑类。
 * 封装 财务项目档案这块可复用的业务能力。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
public class FinanceProjectArchiveMetaSupport extends AbstractFinanceProjectArchiveSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FinanceProjectArchiveMetaSupport(
            FinanceProjectClassMapper financeProjectClassMapper,
            FinanceProjectArchiveMapper financeProjectArchiveMapper,
            SystemCompanyMapper systemCompanyMapper,
            GlAccvouchMapper glAccvouchMapper
    ) {
        super(financeProjectClassMapper, financeProjectArchiveMapper, systemCompanyMapper, glAccvouchMapper);
    }

    /**
     * 获取元数据。
     */
    public FinanceProjectArchiveMetaVO getMeta(String companyId) {
        String normalizedCompanyId = requireCompanyId(companyId);
        requireEnabledCompany(normalizedCompanyId);

        FinanceProjectArchiveMetaVO meta = createMetaContainer();
        meta.setStatusOptions(buildStatusOptions());
        meta.setCloseStatusOptions(buildCloseStatusOptions());
        meta.setProjectClassOptions(
                financeProjectClassMapper.selectList(
                                Wrappers.<FinanceProjectClass>lambdaQuery()
                                        .eq(FinanceProjectClass::getCompanyId, normalizedCompanyId)
                                        .eq(FinanceProjectClass::getStatus, 1)
                                        .orderByAsc(FinanceProjectClass::getSortOrder, FinanceProjectClass::getProjectClassCode, FinanceProjectClass::getId)
                        ).stream()
                        .map(item -> option(item.getProjectClassCode(), item.getProjectClassCode() + " / " + item.getProjectClassName()))
                        .toList()
        );
        return meta;
    }
}
