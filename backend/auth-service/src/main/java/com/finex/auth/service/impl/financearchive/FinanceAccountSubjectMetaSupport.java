// 业务域：财务档案
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 供应商、客户、项目、科目等档案页面接口，下游会继续协调 档案主数据、下拉选项和与凭证、报销单的基础对应。
// 风险提醒：改坏后最容易影响 基础档案错配、下游选项错误和历史单据对应失效。

package com.finex.auth.service.impl.financearchive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSubjectMetaVO;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;

import java.util.List;

/**
 * FinanceAccountSubjectMetaSupport：通用支撑类。
 * 封装 财务账户科目这块可复用的业务能力。
 * 改这里时，要特别关注 基础档案错配、下游选项错误和历史单据对应失效是否会被一起带坏。
 */
public class FinanceAccountSubjectMetaSupport extends AbstractFinanceAccountSubjectArchiveSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FinanceAccountSubjectMetaSupport(
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            SystemCompanyMapper systemCompanyMapper,
            GlAccvouchMapper glAccvouchMapper,
            ObjectMapper objectMapper
    ) {
        super(financeAccountSubjectMapper, systemCompanyMapper, glAccvouchMapper, objectMapper);
    }

    /**
     * 获取元数据。
     */
    public FinanceAccountSubjectMetaVO getMeta() {
        FinanceAccountSubjectMetaVO meta = new FinanceAccountSubjectMetaVO();
        meta.setSubjectCategoryOptions(List.of(
                option(CATEGORY_ASSET, "资产"),
                option(CATEGORY_LIABILITY, "负债"),
                option(CATEGORY_EQUITY, "权益"),
                option(CATEGORY_COST, "成本"),
                option(CATEGORY_PROFIT, "损益")
        ));
        meta.setStatusOptions(List.of(option("1", "启用"), option("0", "停用")));
        meta.setCloseStatusOptions(List.of(option("0", "未封存"), option("1", "已封存")));
        meta.setYesNoOptions(List.of(option("1", "是"), option("0", "否")));
        return meta;
    }
}
