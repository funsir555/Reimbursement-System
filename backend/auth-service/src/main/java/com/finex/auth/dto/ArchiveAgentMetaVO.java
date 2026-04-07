package com.finex.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ArchiveAgentMetaVO {

    private List<Map<String, Object>> modelProviders = new ArrayList<>();

    private List<Map<String, Object>> tools = new ArrayList<>();

    private List<Map<String, Object>> nodeTypes = new ArrayList<>();

    private List<Map<String, Object>> triggerTypes = new ArrayList<>();

    private List<Map<String, Object>> iconOptions = new ArrayList<>();

    private List<Map<String, Object>> themeOptions = new ArrayList<>();

    private String defaultSystemPrompt;
}
