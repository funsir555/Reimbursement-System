package com.finex.auth.service.impl.openingbalance;

import com.finex.auth.dto.OpeningAssistBalanceLineVO;
import com.finex.auth.dto.OpeningBalanceRowVO;
import java.util.List;

public class OpeningBalanceQuerySupport {

    private final SharedOpeningBalanceSupport support;

    public OpeningBalanceQuerySupport(SharedOpeningBalanceSupport support) {
        this.support = support;
    }

    public List<OpeningBalanceRowVO> listRows(String companyId, Integer iyear, Integer iperiod) {
        return support.buildRows(companyId, iyear, iperiod);
    }

    public List<OpeningAssistBalanceLineVO> getAssistBalances(String companyId, Integer iyear, Integer iperiod, String subjectCode) {
        return support.buildAssistLines(companyId, iyear, iperiod, subjectCode);
    }
}
