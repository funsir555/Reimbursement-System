// 业务域：固定资产
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 固定资产卡片、变更处置、折旧期间等接口，下游会继续协调 资产卡片、折旧期间、凭证查询和分类元数据。
// 风险提醒：改坏后最容易影响 资产台账、折旧计提和资产凭证对应。

package com.finex.auth.service.impl.fixedasset;

import com.finex.auth.dto.FixedAssetVoucherLinkVO;

/**
 * FixedAssetVoucherQuerySupport：通用支撑类。
 * 封装 固定资产凭证这块可复用的业务能力。
 * 改这里时，要特别关注 资产台账、折旧计提和资产凭证对应是否会被一起带坏。
 */
public class FixedAssetVoucherQuerySupport {

    private final AbstractFixedAssetSupport support;

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FixedAssetVoucherQuerySupport(AbstractFixedAssetSupport support) {
        this.support = support;
    }

    /**
     * 获取凭证Link。
     */
    public FixedAssetVoucherLinkVO getVoucherLink(String companyId, String businessType, Long businessId) {
        return support.getVoucherLink(companyId, businessType, businessId);
    }
}
