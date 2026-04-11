package com.finex.auth.service.impl.financearchive;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.finex.auth.dto.FinanceProjectArchiveMetaVO;
import com.finex.auth.entity.FinanceProjectClass;
import com.finex.auth.mapper.FinanceProjectArchiveMapper;
import com.finex.auth.mapper.FinanceProjectClassMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;

public class FinanceProjectArchiveMetaSupport extends AbstractFinanceProjectArchiveSupport {

    public FinanceProjectArchiveMetaSupport(
            FinanceProjectClassMapper financeProjectClassMapper,
            FinanceProjectArchiveMapper financeProjectArchiveMapper,
            SystemCompanyMapper systemCompanyMapper,
            GlAccvouchMapper glAccvouchMapper
    ) {
        super(financeProjectClassMapper, financeProjectArchiveMapper, systemCompanyMapper, glAccvouchMapper);
    }

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