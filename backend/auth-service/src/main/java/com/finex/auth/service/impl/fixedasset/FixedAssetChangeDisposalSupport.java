// 业务域：固定资产
// 文件角色：通用支撑类
// 上下游关系：上游通常来自 固定资产卡片、变更处置、折旧期间等接口，下游会继续协调 资产卡片、折旧期间、凭证查询和分类元数据。
// 风险提醒：改坏后最容易影响 资产台账、折旧计提和资产凭证对应。

package com.finex.auth.service.impl.fixedasset;

import com.finex.auth.dto.FixedAssetChangeBillSaveDTO;
import com.finex.auth.dto.FixedAssetChangeBillVO;
import com.finex.auth.dto.FixedAssetDisposalBillSaveDTO;
import com.finex.auth.dto.FixedAssetDisposalBillVO;

import java.util.List;

/**
 * FixedAssetChangeDisposalSupport：通用支撑类。
 * 封装 固定资产变更处置这块可复用的业务能力。
 * 改这里时，要特别关注 资产台账、折旧计提和资产凭证对应是否会被一起带坏。
 */
public class FixedAssetChangeDisposalSupport {

    private final AbstractFixedAssetSupport support;

    /**
     * 初始化这个类所需的依赖组件。
     */
    public FixedAssetChangeDisposalSupport(AbstractFixedAssetSupport support) {
        this.support = support;
    }

    /**
     * 查询变更Bills列表。
     */
    public List<FixedAssetChangeBillVO> listChangeBills(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return support.listChangeBills(companyId, bookCode, fiscalYear, fiscalPeriod);
    }

    /**
     * 创建变更Bill。
     */
    public FixedAssetChangeBillVO createChangeBill(FixedAssetChangeBillSaveDTO dto, String operatorName) {
        return support.createChangeBill(dto, operatorName);
    }

    /**
     * 处理固定资产变更处置中的这一步。
     */
    public FixedAssetChangeBillVO postChangeBill(Long id, String operatorName) {
        return support.postChangeBill(id, operatorName);
    }

    /**
     * 查询处置Bills列表。
     */
    public List<FixedAssetDisposalBillVO> listDisposalBills(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return support.listDisposalBills(companyId, bookCode, fiscalYear, fiscalPeriod);
    }

    /**
     * 创建处置Bill。
     */
    public FixedAssetDisposalBillVO createDisposalBill(FixedAssetDisposalBillSaveDTO dto, String operatorName) {
        return support.createDisposalBill(dto, operatorName);
    }

    /**
     * 处理固定资产变更处置中的这一步。
     */
    public FixedAssetDisposalBillVO postDisposalBill(Long id, String operatorName) {
        return support.postDisposalBill(id, operatorName);
    }
}
