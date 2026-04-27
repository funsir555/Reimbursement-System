package com.finex.auth.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class OpeningBalanceRowVO {

    private String subjectCode;

    private String subjectName;

    private Integer subjectLevel;

    private Integer leafFlag;

    private Boolean editable;

    private Boolean assistRequired;

    private String balanceDirection;

    private String balanceDirectionLabel;

    private String cexchName;

    private String currencyCode;

    private Integer bperson;

    private Integer bcus;

    private Integer bsup;

    private Integer bdept;

    private Integer bitem;

    private String cassItem;

    private BigDecimal mb;
}
