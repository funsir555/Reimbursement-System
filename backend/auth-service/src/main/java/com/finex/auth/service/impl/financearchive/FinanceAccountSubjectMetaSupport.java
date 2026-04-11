package com.finex.auth.service.impl.financearchive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.dto.FinanceAccountSubjectMetaVO;
import com.finex.auth.mapper.FinanceAccountSubjectMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;

import java.util.List;

public class FinanceAccountSubjectMetaSupport extends AbstractFinanceAccountSubjectArchiveSupport {

    public FinanceAccountSubjectMetaSupport(
            FinanceAccountSubjectMapper financeAccountSubjectMapper,
            SystemCompanyMapper systemCompanyMapper,
            GlAccvouchMapper glAccvouchMapper,
            ObjectMapper objectMapper
    ) {
        super(financeAccountSubjectMapper, systemCompanyMapper, glAccvouchMapper, objectMapper);
    }

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
