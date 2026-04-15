// 业务域：固定资产
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 固定资产卡片、变更处置、折旧期间等接口，下游会继续协调 资产卡片、折旧期间、凭证查询和分类元数据。
// 风险提醒：改坏后最容易影响 资产台账、折旧计提和资产凭证对应。

package com.finex.auth.service.impl.fixedasset;

import com.finex.auth.mapper.FaAssetAccountPolicyMapper;
import com.finex.auth.mapper.FaAssetCardMapper;
import com.finex.auth.mapper.FaAssetCategoryMapper;
import com.finex.auth.mapper.FaAssetChangeBillMapper;
import com.finex.auth.mapper.FaAssetChangeLineMapper;
import com.finex.auth.mapper.FaAssetDeprLineMapper;
import com.finex.auth.mapper.FaAssetDeprRunMapper;
import com.finex.auth.mapper.FaAssetDisposalBillMapper;
import com.finex.auth.mapper.FaAssetDisposalLineMapper;
import com.finex.auth.mapper.FaAssetOpeningImportLineMapper;
import com.finex.auth.mapper.FaAssetOpeningImportMapper;
import com.finex.auth.mapper.FaAssetPeriodCloseMapper;
import com.finex.auth.mapper.FaAssetVoucherLinkMapper;
import com.finex.auth.mapper.GlAccvouchMapper;
import com.finex.auth.mapper.SystemCompanyMapper;
import com.finex.auth.mapper.SystemDepartmentMapper;
import com.finex.auth.mapper.UserMapper;

/**
 * SharedFixedAssetSupport：通用支撑类。
 * 封装 共享固定资产这块可复用的业务能力。
 * 改这里时，要特别关注 资产台账、折旧计提和资产凭证对应是否会被一起带坏。
 */
public final class SharedFixedAssetSupport extends AbstractFixedAssetSupport {

    /**
     * 初始化这个类所需的依赖组件。
     */
    public SharedFixedAssetSupport(
            FaAssetCategoryMapper faAssetCategoryMapper,
            FaAssetAccountPolicyMapper faAssetAccountPolicyMapper,
            FaAssetCardMapper faAssetCardMapper,
            FaAssetChangeBillMapper faAssetChangeBillMapper,
            FaAssetChangeLineMapper faAssetChangeLineMapper,
            FaAssetDeprRunMapper faAssetDeprRunMapper,
            FaAssetDeprLineMapper faAssetDeprLineMapper,
            FaAssetDisposalBillMapper faAssetDisposalBillMapper,
            FaAssetDisposalLineMapper faAssetDisposalLineMapper,
            FaAssetOpeningImportMapper faAssetOpeningImportMapper,
            FaAssetOpeningImportLineMapper faAssetOpeningImportLineMapper,
            FaAssetVoucherLinkMapper faAssetVoucherLinkMapper,
            FaAssetPeriodCloseMapper faAssetPeriodCloseMapper,
            GlAccvouchMapper glAccvouchMapper,
            SystemCompanyMapper systemCompanyMapper,
            SystemDepartmentMapper systemDepartmentMapper,
            UserMapper userMapper
    ) {
        super(
                faAssetCategoryMapper,
                faAssetAccountPolicyMapper,
                faAssetCardMapper,
                faAssetChangeBillMapper,
                faAssetChangeLineMapper,
                faAssetDeprRunMapper,
                faAssetDeprLineMapper,
                faAssetDisposalBillMapper,
                faAssetDisposalLineMapper,
                faAssetOpeningImportMapper,
                faAssetOpeningImportLineMapper,
                faAssetVoucherLinkMapper,
                faAssetPeriodCloseMapper,
                glAccvouchMapper,
                systemCompanyMapper,
                systemDepartmentMapper,
                userMapper
        );
    }
}
