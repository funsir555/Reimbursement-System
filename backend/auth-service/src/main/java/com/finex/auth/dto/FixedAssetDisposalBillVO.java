package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class FixedAssetDisposalBillVO {
    private Long id;
    private String companyId;
    private String billNo;
    private String billType;
    private String bookCode;
    private Integer fiscalYear;
    private Integer fiscalPeriod;
    private String billDate;
    private String status;
    @MoneyValue
    private BigDecimal totalOriginalAmount;
    @MoneyValue
    private BigDecimal totalAccumAmount;
    @MoneyValue
    private BigDecimal totalNetAmount;
    private String remark;
    private String postedAt;
    private FixedAssetVoucherLinkVO voucherLink;
    private List<FixedAssetDisposalLineVO> lines = new ArrayList<>();
}
