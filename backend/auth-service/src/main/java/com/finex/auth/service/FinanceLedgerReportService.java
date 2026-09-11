package com.finex.auth.service;

import com.finex.auth.dto.FinanceBalanceSheetRowVO;
import com.finex.auth.dto.FinanceDetailLedgerRowVO;
import com.finex.auth.dto.FinanceGeneralLedgerSectionVO;
import com.finex.auth.dto.FinanceLedgerReportMetaVO;
import com.finex.auth.dto.FinanceLedgerReportPageVO;
import com.finex.auth.dto.FinanceLedgerReportQueryDTO;
import com.finex.auth.dto.FinanceSequenceLedgerRowVO;

public interface FinanceLedgerReportService {

    FinanceLedgerReportMetaVO getMeta(Long currentUserId, String companyId, Integer iyear, Integer iperiod);

    FinanceLedgerReportPageVO<FinanceBalanceSheetRowVO> queryBalanceSheet(Long currentUserId, FinanceLedgerReportQueryDTO dto);

    FinanceLedgerReportPageVO<FinanceGeneralLedgerSectionVO> queryGeneralLedger(Long currentUserId, FinanceLedgerReportQueryDTO dto);

    FinanceLedgerReportPageVO<FinanceDetailLedgerRowVO> queryDetailLedger(Long currentUserId, FinanceLedgerReportQueryDTO dto);

    FinanceLedgerReportPageVO<FinanceSequenceLedgerRowVO> querySequenceLedger(Long currentUserId, FinanceLedgerReportQueryDTO dto);

    byte[] exportBalanceSheet(Long currentUserId, FinanceLedgerReportQueryDTO dto);

    byte[] exportGeneralLedger(Long currentUserId, FinanceLedgerReportQueryDTO dto);

    byte[] exportDetailLedger(Long currentUserId, FinanceLedgerReportQueryDTO dto);

    byte[] exportSequenceLedger(Long currentUserId, FinanceLedgerReportQueryDTO dto);
}
