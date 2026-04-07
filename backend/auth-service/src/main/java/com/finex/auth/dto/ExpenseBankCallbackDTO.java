package com.finex.auth.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ExpenseBankCallbackDTO {

    private String pushRequestNo;

    private Long taskId;

    private String documentCode;

    private String callbackSecret;

    private String bankOrderNo;

    private String bankFlowNo;

    private String resultCode;

    private String resultMessage;

    private Boolean success;

    private String paidAt;

    private Map<String, Object> rawPayload = new LinkedHashMap<>();
}
