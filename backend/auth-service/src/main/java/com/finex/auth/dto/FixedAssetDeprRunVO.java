package com.finex.auth.dto;

import com.finex.auth.support.json.MoneyValue;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class FixedAssetDeprRunVO {
    private Long id;
    private String companyId;
    private String runNo;
    private String bookCode;
    private Integer fiscalYear;
    private Integer fiscalPeriod;
    private String status;
    private Integer assetCount;
    @MoneyValue
    private BigDecimal totalAmount;
    private String remark;
    private String postedAt;
    private FixedAssetVoucherLinkVO voucherLink;
    private List<FixedAssetDeprLineVO> lines = new ArrayList<>();
}
