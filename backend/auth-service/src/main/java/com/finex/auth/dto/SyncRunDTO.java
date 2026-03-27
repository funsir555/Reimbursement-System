package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SyncRunDTO {

    private List<String> platformCodes = new ArrayList<>();

    private String triggerType;
}
