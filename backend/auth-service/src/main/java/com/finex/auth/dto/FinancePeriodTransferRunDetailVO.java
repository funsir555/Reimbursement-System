package com.finex.auth.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class FinancePeriodTransferRunDetailVO {

    private FinancePeriodTransferRunVO run;

    private List<FinancePeriodTransferPreviewDetailVO> details = new ArrayList<>();
}
