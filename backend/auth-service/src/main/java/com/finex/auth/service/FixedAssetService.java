// 业务域：固定资产
// 文件角色：service 接口
// 上下游关系：上游通常来自 固定资产卡片、变更处置、折旧期间等接口，下游会继续协调 资产卡片、折旧期间、凭证查询和分类元数据。
// 风险提醒：改坏后最容易影响 资产台账、折旧计提和资产凭证对应。

package com.finex.auth.service;

import com.finex.auth.dto.FixedAssetCardSaveDTO;
import com.finex.auth.dto.FixedAssetCardVO;
import com.finex.auth.dto.FixedAssetCategorySaveDTO;
import com.finex.auth.dto.FixedAssetCategoryVO;
import com.finex.auth.dto.FixedAssetChangeBillSaveDTO;
import com.finex.auth.dto.FixedAssetChangeBillVO;
import com.finex.auth.dto.FixedAssetDeprPreviewDTO;
import com.finex.auth.dto.FixedAssetDeprRunVO;
import com.finex.auth.dto.FixedAssetDisposalBillSaveDTO;
import com.finex.auth.dto.FixedAssetDisposalBillVO;
import com.finex.auth.dto.FixedAssetMetaVO;
import com.finex.auth.dto.FixedAssetOpeningImportDTO;
import com.finex.auth.dto.FixedAssetOpeningImportResultVO;
import com.finex.auth.dto.FixedAssetPeriodCloseDTO;
import com.finex.auth.dto.FixedAssetPeriodStatusVO;
import com.finex.auth.dto.FixedAssetTemplateVO;
import com.finex.auth.dto.FixedAssetVoucherLinkVO;

import java.util.List;

/**
 * FixedAssetService：service 接口。
 * 定义固定资产这块对外提供的业务入口能力。
 * 改这里时，要特别关注 资产台账、折旧计提和资产凭证对应是否会被一起带坏。
 */
public interface FixedAssetService {

    /**
     * 获取元数据。
     */
    FixedAssetMetaVO getMeta(Long currentUserId, String currentUsername, String companyId, Integer fiscalYear, Integer fiscalPeriod);

    /**
     * 查询Categories列表。
     */
    List<FixedAssetCategoryVO> listCategories(String companyId);

    /**
     * 创建分类。
     */
    FixedAssetCategoryVO createCategory(FixedAssetCategorySaveDTO dto, String operatorName);

    /**
     * 更新分类。
     */
    FixedAssetCategoryVO updateCategory(Long id, FixedAssetCategorySaveDTO dto, String operatorName);

    /**
     * 查询卡片列表。
     */
    List<FixedAssetCardVO> listCards(String companyId, String bookCode, String keyword, Long categoryId, String status);

    /**
     * 获取卡片。
     */
    FixedAssetCardVO getCard(Long id);

    /**
     * 创建卡片。
     */
    FixedAssetCardVO createCard(FixedAssetCardSaveDTO dto, String operatorName);

    /**
     * 更新卡片。
     */
    FixedAssetCardVO updateCard(Long id, FixedAssetCardSaveDTO dto, String operatorName);

    /**
     * 获取启用模板。
     */
    FixedAssetTemplateVO getOpeningTemplate(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod);

    FixedAssetOpeningImportResultVO importOpening(FixedAssetOpeningImportDTO dto, String operatorName);

    /**
     * 获取启用ImportResult。
     */
    FixedAssetOpeningImportResultVO getOpeningImportResult(Long batchId);

    /**
     * 查询变更Bills列表。
     */
    List<FixedAssetChangeBillVO> listChangeBills(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod);

    /**
     * 创建变更Bill。
     */
    FixedAssetChangeBillVO createChangeBill(FixedAssetChangeBillSaveDTO dto, String operatorName);

    FixedAssetChangeBillVO postChangeBill(Long id, String operatorName);

    FixedAssetDeprRunVO previewDepreciation(FixedAssetDeprPreviewDTO dto);

    /**
     * 查询折旧执行列表。
     */
    List<FixedAssetDeprRunVO> listDepreciationRuns(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod);

    /**
     * 创建折旧执行。
     */
    FixedAssetDeprRunVO createDepreciationRun(FixedAssetDeprPreviewDTO dto, String operatorName);

    FixedAssetDeprRunVO postDepreciationRun(Long id, String operatorName);

    /**
     * 查询处置Bills列表。
     */
    List<FixedAssetDisposalBillVO> listDisposalBills(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod);

    /**
     * 创建处置Bill。
     */
    FixedAssetDisposalBillVO createDisposalBill(FixedAssetDisposalBillSaveDTO dto, String operatorName);

    FixedAssetDisposalBillVO postDisposalBill(Long id, String operatorName);

    /**
     * 关闭期间。
     */
    FixedAssetPeriodStatusVO closePeriod(FixedAssetPeriodCloseDTO dto, String operatorName);

    /**
     * 获取期间Status。
     */
    FixedAssetPeriodStatusVO getPeriodStatus(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod);

    /**
     * 获取凭证Link。
     */
    FixedAssetVoucherLinkVO getVoucherLink(String companyId, String businessType, Long businessId);
}
