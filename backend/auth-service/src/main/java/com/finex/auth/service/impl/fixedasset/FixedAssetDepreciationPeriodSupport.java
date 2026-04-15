// 业务域：固定资产
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 固定资产卡片、变更处置、折旧期间等接口，下游会继续协调 资产卡片、折旧期间、凭证查询和分类元数据。
// 风险提醒：改坏后最容易影响 资产台账、折旧计提和资产凭证对应。

package com.finex.auth.service.impl.fixedasset;

import com.finex.auth.dto.FixedAssetDeprPreviewDTO;
import com.finex.auth.dto.FixedAssetDeprRunVO;
import com.finex.auth.dto.FixedAssetPeriodCloseDTO;
import com.finex.auth.dto.FixedAssetPeriodStatusVO;

import java.util.List;

/**
 * FixedAssetDepreciationPeriodSupport：通用支撑类。
 * 封装 固定资产折旧期间这块可复用的业务能力。
 * 改这里时，要特别关注 资产台账、折旧计提和资产凭证对应是否会被一起带坏。
 */
public class FixedAssetDepreciationPeriodSupport {

    private final AbstractFixedAssetSupport support;

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FixedAssetDepreciationPeriodSupport(AbstractFixedAssetSupport support) {
        this.support = support;
    }

    /**
     * 处理固定资产折旧期间中的这一步。
     */
    public FixedAssetDeprRunVO previewDepreciation(FixedAssetDeprPreviewDTO dto) {
        return support.previewDepreciation(dto);
    }

    /**
     * 查询折旧执行列表。
     */
    public List<FixedAssetDeprRunVO> listDepreciationRuns(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return support.listDepreciationRuns(companyId, bookCode, fiscalYear, fiscalPeriod);
    }

    /**
     * 创建折旧执行。
     */
    public FixedAssetDeprRunVO createDepreciationRun(FixedAssetDeprPreviewDTO dto, String operatorName) {
        return support.createDepreciationRun(dto, operatorName);
    }

    /**
     * 处理固定资产折旧期间中的这一步。
     */
    public FixedAssetDeprRunVO postDepreciationRun(Long id, String operatorName) {
        return support.postDepreciationRun(id, operatorName);
    }

    /**
     * 关闭期间。
     */
    public FixedAssetPeriodStatusVO closePeriod(FixedAssetPeriodCloseDTO dto, String operatorName) {
        return support.closePeriod(dto, operatorName);
    }

    /**
     * 获取期间Status。
     */
    public FixedAssetPeriodStatusVO getPeriodStatus(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return support.getPeriodStatus(companyId, bookCode, fiscalYear, fiscalPeriod);
    }
}
