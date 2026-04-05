package com.finex.auth.dto;

import lombok.Data;

@Data
public class FixedAssetVoucherLinkVO {
    private Long id;
    private String companyId;
    private String businessType;
    private Long businessId;
    private String voucherNo;
    private Integer iperiod;
    private String csign;
    private Integer inoId;
    private String remark;
}
