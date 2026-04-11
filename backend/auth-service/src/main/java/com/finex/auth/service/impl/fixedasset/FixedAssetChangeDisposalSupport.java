package com.finex.auth.service.impl.fixedasset;

import com.finex.auth.dto.FixedAssetChangeBillSaveDTO;
import com.finex.auth.dto.FixedAssetChangeBillVO;
import com.finex.auth.dto.FixedAssetDisposalBillSaveDTO;
import com.finex.auth.dto.FixedAssetDisposalBillVO;

import java.util.List;

public class FixedAssetChangeDisposalSupport {

    private final AbstractFixedAssetSupport support;

    public FixedAssetChangeDisposalSupport(AbstractFixedAssetSupport support) {
        this.support = support;
    }

    public List<FixedAssetChangeBillVO> listChangeBills(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return support.listChangeBills(companyId, bookCode, fiscalYear, fiscalPeriod);
    }

    public FixedAssetChangeBillVO createChangeBill(FixedAssetChangeBillSaveDTO dto, String operatorName) {
        return support.createChangeBill(dto, operatorName);
    }

    public FixedAssetChangeBillVO postChangeBill(Long id, String operatorName) {
        return support.postChangeBill(id, operatorName);
    }

    public List<FixedAssetDisposalBillVO> listDisposalBills(String companyId, String bookCode, Integer fiscalYear, Integer fiscalPeriod) {
        return support.listDisposalBills(companyId, bookCode, fiscalYear, fiscalPeriod);
    }

    public FixedAssetDisposalBillVO createDisposalBill(FixedAssetDisposalBillSaveDTO dto, String operatorName) {
        return support.createDisposalBill(dto, operatorName);
    }

    public FixedAssetDisposalBillVO postDisposalBill(Long id, String operatorName) {
        return support.postDisposalBill(id, operatorName);
    }
}
