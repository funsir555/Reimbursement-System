// 业务域：固定资产
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 固定资产卡片、变更处置、折旧期间等接口，下游会继续协调 资产卡片、折旧期间、凭证查询和分类元数据。
// 风险提醒：改坏后最容易影响 资产台账、折旧计提和资产凭证对应。

package com.finex.auth.service.impl.fixedasset;

import com.finex.auth.dto.FixedAssetCardSaveDTO;
import com.finex.auth.dto.FixedAssetCardVO;
import com.finex.auth.dto.FixedAssetOpeningImportDTO;
import com.finex.auth.dto.FixedAssetOpeningImportResultVO;
import com.finex.auth.dto.FixedAssetTemplateVO;

import java.util.List;

/**
 * FixedAssetCardOpeningSupport：通用支撑类。
 * 封装 固定资产卡片启用这块可复用的业务能力。
 * 改这里时，要特别关注 资产台账、折旧计提和资产凭证对应是否会被一起带坏。
 */
public class FixedAssetCardOpeningSupport {

    private final AbstractFixedAssetSupport support;

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FixedAssetCardOpeningSupport(AbstractFixedAssetSupport support) {
        this.support = support;
    }

    /**
     * 查询卡片列表。
     */
    public List<FixedAssetCardVO> listCards(String companyId, String bookCode, String keyword, Long categoryId, String status) {
        return support.listCards(companyId, bookCode, keyword, categoryId, status);
    }

    /**
     * 获取卡片。
     */
    public FixedAssetCardVO getCard(Long id) {
        return support.getCard(id);
    }

    /**
     * 创建卡片。
     */
    public FixedAssetCardVO createCard(FixedAssetCardSaveDTO dto, String operatorName) {
        return support.createCard(dto, operatorName);
    }

    /**
     * 更新卡片。
     */
    public FixedAssetCardVO updateCard(Long id, FixedAssetCardSaveDTO dto, String operatorName) {
        return support.updateCard(id, dto, operatorName);
    }

    /**
     * 获取启用模板。
     */
    public FixedAssetTemplateVO getOpeningTemplate(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return support.getOpeningTemplate(companyId, bookCode, fiscalYear, fiscalPeriod);
    }

    /**
     * 处理固定资产卡片启用中的这一步。
     */
    public FixedAssetOpeningImportResultVO importOpening(FixedAssetOpeningImportDTO dto, String operatorName) {
        return support.importOpening(dto, operatorName);
    }

    /**
     * 获取启用ImportResult。
     */
    public FixedAssetOpeningImportResultVO getOpeningImportResult(Long batchId) {
        return support.getOpeningImportResult(batchId);
    }
}
