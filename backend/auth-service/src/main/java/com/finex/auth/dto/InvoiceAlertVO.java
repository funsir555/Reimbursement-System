package com.finex.auth.dto;

import lombok.Data;

/**
 * 发票提醒
 */
@Data
public class InvoiceAlertVO {

    private Long id;

    private String title;

    private String desc;

    private String time;
}
