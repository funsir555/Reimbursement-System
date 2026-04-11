package com.finex.auth.service.impl.fixedasset;

import com.finex.auth.dto.FixedAssetCategorySaveDTO;
import com.finex.auth.dto.FixedAssetCategoryVO;
import com.finex.auth.dto.FixedAssetMetaVO;

import java.util.List;

public class FixedAssetMetaCategorySupport {

    private final AbstractFixedAssetSupport support;

    public FixedAssetMetaCategorySupport(AbstractFixedAssetSupport support) {
        this.support = support;
    }

    public FixedAssetMetaVO getMeta(Long currentUserId, String currentUsername, String companyId, Integer fiscalYear, Integer fiscalPeriod) {
        return support.getMeta(currentUserId, currentUsername, companyId, fiscalYear, fiscalPeriod);
    }

    public List<FixedAssetCategoryVO> listCategories(String companyId) {
        return support.listCategories(companyId);
    }

    public FixedAssetCategoryVO createCategory(FixedAssetCategorySaveDTO dto, String operatorName) {
        return support.createCategory(dto, operatorName);
    }

    public FixedAssetCategoryVO updateCategory(Long id, FixedAssetCategorySaveDTO dto, String operatorName) {
        return support.updateCategory(id, dto, operatorName);
    }
}
