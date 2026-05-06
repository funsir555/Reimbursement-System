package com.finex.auth.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class OpeningAssistBalanceLineVO {

    private String cdeptId;

    private String cpersonId;

    private String ccusId;

    private String csupId;

    private String citemClass;

    private String citemId;

    private BigDecimal mb;

    private String actualBalanceDirection;

    private String actualBalanceDirectionLabel;

    private BigDecimal displayBalance;

    private BigDecimal mbF;

    private BigDecimal nbS;
}
