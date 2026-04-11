package com.finex.auth.service.impl.fixedasset;

import com.finex.auth.dto.FixedAssetCardSaveDTO;
import com.finex.auth.dto.FixedAssetCardVO;
import com.finex.auth.dto.FixedAssetOpeningImportDTO;
import com.finex.auth.dto.FixedAssetOpeningImportResultVO;
import com.finex.auth.dto.FixedAssetTemplateVO;

import java.util.List;

public class FixedAssetCardOpeningSupport {

    private final AbstractFixedAssetSupport support;

    public FixedAssetCardOpeningSupport(AbstractFixedAssetSupport support) {
        this.support = support;
    }

    public List<FixedAssetCardVO> listCards(String companyId, String bookCode, String keyword, Long categoryId, String status) {
        return support.listCards(companyId, bookCode, keyword, categoryId, status);
    }

    public FixedAssetCardVO getCard(Long id) {
        return support.getCard(id);
    }

    public FixedAssetCardVO createCard(FixedAssetCardSaveDTO dto, String operatorName) {
        return support.createCard(dto, operatorName);
    }

    public FixedAssetCardVO updateCard(Long id, FixedAssetCardSaveDTO dto, String operatorName) {
        return support.updateCard(id, dto, operatorName);
    }

    public FixedAssetTemplateVO getOpeningTemplate(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return support.getOpeningTemplate(companyId, bookCode, fiscalYear, fiscalPeriod);
    }

    public FixedAssetOpeningImportResultVO importOpening(FixedAssetOpeningImportDTO dto, String operatorName) {
        return support.importOpening(dto, operatorName);
    }

    public FixedAssetOpeningImportResultVO getOpeningImportResult(Long batchId) {
        return support.getOpeningImportResult(batchId);
    }
}
