package com.finex.auth.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class OpeningBalanceCarryForwardPreviewVO {

    private List<OpeningBalanceRowVO> rows = new ArrayList<>();

    private List<OpeningBalanceAssistDraftLineDTO> assistLines = new ArrayList<>();
}
