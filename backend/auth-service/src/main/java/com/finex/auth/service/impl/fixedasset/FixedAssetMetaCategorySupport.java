// 业务域：固定资产
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 固定资产卡片、变更处置、折旧期间等接口，下游会继续协调 资产卡片、折旧期间、凭证查询和分类元数据。
// 风险提醒：改坏后最容易影响 资产台账、折旧计提和资产凭证对应。

package com.finex.auth.service.impl.fixedasset;

import com.finex.auth.dto.FixedAssetCategorySaveDTO;
import com.finex.auth.dto.FixedAssetCategoryVO;
import com.finex.auth.dto.FixedAssetMetaVO;

import java.util.List;

/**
 * FixedAssetMetaCategorySupport：通用支撑类。
 * 封装 固定资产分类这块可复用的业务能力。
 * 改这里时，要特别关注 资产台账、折旧计提和资产凭证对应是否会被一起带坏。
 */
public class FixedAssetMetaCategorySupport {

    private final AbstractFixedAssetSupport support;

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FixedAssetMetaCategorySupport(AbstractFixedAssetSupport support) {
        this.support = support;
    }

    /**
     * 获取元数据。
     */
    public FixedAssetMetaVO getMeta(Long currentUserId, String currentUsername, String companyId, Integer fiscalYear, Integer fiscalPeriod) {
        return support.getMeta(currentUserId, currentUsername, companyId, fiscalYear, fiscalPeriod);
    }

    /**
     * 查询Categories列表。
     */
    public List<FixedAssetCategoryVO> listCategories(String companyId) {
        return support.listCategories(companyId);
    }

    /**
     * 创建分类。
     */
    public FixedAssetCategoryVO createCategory(FixedAssetCategorySaveDTO dto, String operatorName) {
        return support.createCategory(dto, operatorName);
    }

    /**
     * 更新分类。
     */
    public FixedAssetCategoryVO updateCategory(Long id, FixedAssetCategorySaveDTO dto, String operatorName) {
        return support.updateCategory(id, dto, operatorName);
    }
}
