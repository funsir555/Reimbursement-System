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

public interface FixedAssetService {

    FixedAssetMetaVO getMeta(Long currentUserId, String currentUsername, String companyId, Integer fiscalYear, Integer fiscalPeriod);

    List<FixedAssetCategoryVO> listCategories(String companyId);

    FixedAssetCategoryVO createCategory(FixedAssetCategorySaveDTO dto, String operatorName);

    FixedAssetCategoryVO updateCategory(Long id, FixedAssetCategorySaveDTO dto, String operatorName);

    List<FixedAssetCardVO> listCards(String companyId, String bookCode, String keyword, Long categoryId, String status);

    FixedAssetCardVO getCard(Long id);

    FixedAssetCardVO createCard(FixedAssetCardSaveDTO dto, String operatorName);

    FixedAssetCardVO updateCard(Long id, FixedAssetCardSaveDTO dto, String operatorName);

    FixedAssetTemplateVO getOpeningTemplate(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod);

    FixedAssetOpeningImportResultVO importOpening(FixedAssetOpeningImportDTO dto, String operatorName);

    FixedAssetOpeningImportResultVO getOpeningImportResult(Long batchId);

    List<FixedAssetChangeBillVO> listChangeBills(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod);

    FixedAssetChangeBillVO createChangeBill(FixedAssetChangeBillSaveDTO dto, String operatorName);

    FixedAssetChangeBillVO postChangeBill(Long id, String operatorName);

    FixedAssetDeprRunVO previewDepreciation(FixedAssetDeprPreviewDTO dto);

    List<FixedAssetDeprRunVO> listDepreciationRuns(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod);

    FixedAssetDeprRunVO createDepreciationRun(FixedAssetDeprPreviewDTO dto, String operatorName);

    FixedAssetDeprRunVO postDepreciationRun(Long id, String operatorName);

    List<FixedAssetDisposalBillVO> listDisposalBills(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod);

    FixedAssetDisposalBillVO createDisposalBill(FixedAssetDisposalBillSaveDTO dto, String operatorName);

    FixedAssetDisposalBillVO postDisposalBill(Long id, String operatorName);

    FixedAssetPeriodStatusVO closePeriod(FixedAssetPeriodCloseDTO dto, String operatorName);

    FixedAssetPeriodStatusVO getPeriodStatus(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod);

    FixedAssetVoucherLinkVO getVoucherLink(String companyId, String businessType, Long businessId);
}
