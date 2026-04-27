package com.finex.auth.service.impl.openingbalance;

import com.finex.auth.dto.OpeningBalanceAssistSaveDTO;
import com.finex.auth.dto.OpeningBalanceSaveDTO;
import com.finex.auth.dto.OpeningAssistBalanceLineVO;
import com.finex.auth.dto.OpeningBalanceRowVO;
import java.util.List;

public class OpeningBalanceMutationSupport {

    private final SharedOpeningBalanceSupport support;

    public OpeningBalanceMutationSupport(SharedOpeningBalanceSupport support) {
        this.support = support;
    }

    public List<OpeningBalanceRowVO> saveRows(OpeningBalanceSaveDTO dto, String operatorName) {
        return support.saveSimpleRows(dto.getCompanyId(), dto.getIyear(), dto.getIperiod(), dto.getRows());
    }

    public List<OpeningAssistBalanceLineVO> saveAssistBalances(String subjectCode, OpeningBalanceAssistSaveDTO dto, String operatorName) {
        return support.saveAssistLines(subjectCode, dto.getCompanyId(), dto.getIyear(), dto.getIperiod(), dto.getLines());
    }
}
