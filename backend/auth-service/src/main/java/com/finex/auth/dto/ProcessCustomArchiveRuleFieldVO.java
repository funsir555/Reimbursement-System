package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProcessCustomArchiveRuleFieldVO {

    private String key;

    private String label;

    private String valueType;

    private List<String> operatorKeys = new ArrayList<>();
}
