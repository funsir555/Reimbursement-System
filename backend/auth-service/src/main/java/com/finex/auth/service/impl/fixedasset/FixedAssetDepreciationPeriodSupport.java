package com.finex.auth.service.impl.fixedasset;

import com.finex.auth.dto.FixedAssetDeprPreviewDTO;
import com.finex.auth.dto.FixedAssetDeprRunVO;
import com.finex.auth.dto.FixedAssetPeriodCloseDTO;
import com.finex.auth.dto.FixedAssetPeriodStatusVO;

import java.util.List;

public class FixedAssetDepreciationPeriodSupport {

    private final AbstractFixedAssetSupport support;

    public FixedAssetDepreciationPeriodSupport(AbstractFixedAssetSupport support) {
        this.support = support;
    }

    public FixedAssetDeprRunVO previewDepreciation(FixedAssetDeprPreviewDTO dto) {
        return support.previewDepreciation(dto);
    }

    public List<FixedAssetDeprRunVO> listDepreciationRuns(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return support.listDepreciationRuns(companyId, bookCode, fiscalYear, fiscalPeriod);
    }

    public FixedAssetDeprRunVO createDepreciationRun(FixedAssetDeprPreviewDTO dto, String operatorName) {
        return support.createDepreciationRun(dto, operatorName);
    }

    public FixedAssetDeprRunVO postDepreciationRun(Long id, String operatorName) {
        return support.postDepreciationRun(id, operatorName);
    }

    public FixedAssetPeriodStatusVO closePeriod(FixedAssetPeriodCloseDTO dto, String operatorName) {
        return support.closePeriod(dto, operatorName);
    }

    public FixedAssetPeriodStatusVO getPeriodStatus(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return support.getPeriodStatus(companyId, bookCode, fiscalYear, fiscalPeriod);
    }
}
