package com.finex.auth.support.systemsettings;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.entity.SystemSyncConnector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WecomSyncAdapter implements OrganizationSyncAdapter {

    private static final String PLATFORM_CODE = "WECOM";
    private static final String API_BASE_URL = "https://qyapi.weixin.qq.com/cgi-bin";

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String getPlatformCode() {
        return PLATFORM_CODE;
    }

    @Override
    public ExternalSyncPayload pull(SystemSyncConnector connector) {
        Map<String, String> config = readConnectorConfig(connector.getConfigJson());
        String corpId = requireConfigValue(config, "corpId", "企业 ID");
        String appSecret = requireConfigValue(config, "appSecret", "通讯录 Secret");

        String accessToken = fetchAccessToken(corpId, appSecret);
        List<ExternalDepartmentData> departments = fetchDepartments(accessToken);
        List<ExternalEmployeeData> employees = fetchEmployees(accessToken, departments);
        return new ExternalSyncPayload(departments, employees);
    }

    private String fetchAccessToken(String corpId, String appSecret) {
        URI uri = UriComponentsBuilder.fromHttpUrl(API_BASE_URL + "/gettoken")
                .queryParam("corpid", corpId)
                .queryParam("corpsecret", appSecret)
                .build(true)
                .toUri();
        JsonNode body = requestJson(uri, "获取 access_token");
        String accessToken = trimToNull(body.path("access_token").asText(null));
        if (accessToken == null) {
            throw new IllegalArgumentException("企微获取 access_token 失败：返回结果缺少 access_token");
        }
        return accessToken;
    }

    private List<ExternalDepartmentData> fetchDepartments(String accessToken) {
        URI uri = UriComponentsBuilder.fromHttpUrl(API_BASE_URL + "/department/list")
                .queryParam("access_token", accessToken)
                .build(true)
                .toUri();
        JsonNode body = requestJson(uri, "拉取部门列表");
        JsonNode departmentsNode = body.path("department");
        List<ExternalDepartmentData> departments = new ArrayList<>();
        if (!departmentsNode.isArray()) {
            return departments;
        }

        for (JsonNode departmentNode : departmentsNode) {
            JsonNode idNode = departmentNode.get("id");
            if (idNode == null || idNode.isNull()) {
                continue;
            }
            String deptCode = trimToNull(idNode.asText());
            if (deptCode == null) {
                continue;
            }

            JsonNode parentIdNode = departmentNode.get("parentid");
            String parentDeptCode = null;
            if (parentIdNode != null && !parentIdNode.isNull() && parentIdNode.asLong() > 0) {
                parentDeptCode = trimToNull(parentIdNode.asText());
            }

            departments.add(new ExternalDepartmentData(
                    deptCode,
                    trimToNull(departmentNode.path("name").asText(null)),
                    parentDeptCode,
                    deptCode,
                    1
            ));
        }
        return departments;
    }

    private List<ExternalEmployeeData> fetchEmployees(String accessToken, List<ExternalDepartmentData> departments) {
        Map<String, ExternalEmployeeData> employeeByExternalId = new LinkedHashMap<>();
        for (ExternalDepartmentData department : departments) {
            URI uri = UriComponentsBuilder.fromHttpUrl(API_BASE_URL + "/user/list")
                    .queryParam("access_token", accessToken)
                    .queryParam("department_id", department.getDeptCode())
                    .queryParam("fetch_child", 0)
                    .build(true)
                    .toUri();
            JsonNode body = requestJson(uri, "拉取部门[" + department.getDeptName() + "]员工列表");
            JsonNode usersNode = body.path("userlist");
            if (!usersNode.isArray()) {
                continue;
            }

            for (JsonNode userNode : usersNode) {
                String externalId = trimToNull(userNode.path("userid").asText(null));
                if (externalId == null) {
                    continue;
                }

                String primaryDeptCode = resolvePrimaryDeptCode(userNode.path("department"), department.getDeptCode());
                ExternalEmployeeData employee = new ExternalEmployeeData(
                        externalId,
                        trimToNull(userNode.path("name").asText(null)),
                        trimToNull(userNode.path("mobile").asText(null)),
                        trimToNull(userNode.path("email").asText(null)),
                        primaryDeptCode,
                        trimToNull(userNode.path("position").asText(null)),
                        null,
                        externalId,
                        userNode.path("enable").asInt(1) == 1 ? 1 : 0
                );
                employeeByExternalId.putIfAbsent(externalId, employee);
            }
        }
        return new ArrayList<>(employeeByExternalId.values());
    }

    private String resolvePrimaryDeptCode(JsonNode departmentArrayNode, String fallbackDeptCode) {
        if (departmentArrayNode != null && departmentArrayNode.isArray()) {
            for (JsonNode departmentNode : departmentArrayNode) {
                String deptCode = trimToNull(departmentNode.asText());
                if (deptCode != null) {
                    return deptCode;
                }
            }
        }
        return fallbackDeptCode;
    }

    private JsonNode requestJson(URI uri, String stage) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .GET()
                .build();
        String responseBody;
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new IllegalArgumentException("企微" + stage + "失败：HTTP " + response.statusCode());
            }
            responseBody = response.body();
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("企微" + stage + "失败：" + ex.getMessage(), ex);
        }

        try {
            JsonNode body = objectMapper.readTree(responseBody);
            validateWecomResponse(body, stage);
            return body;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("企微" + stage + "失败：响应解析异常", ex);
        }
    }

    private void validateWecomResponse(JsonNode body, String stage) {
        int errCode = body.path("errcode").asInt(0);
        if (errCode == 0) {
            return;
        }
        String errMsg = trimToNull(body.path("errmsg").asText(null));
        throw new IllegalArgumentException(
                "企微" + stage + "失败：errcode=" + errCode + (errMsg == null ? "" : ", errmsg=" + errMsg)
        );
    }

    private Map<String, String> readConnectorConfig(String configJson) {
        if (StrUtil.isBlank(configJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(configJson, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException("企微同步配置解析失败", ex);
        }
    }

    private String requireConfigValue(Map<String, String> config, String key, String fieldName) {
        String value = trimToNull(config.get(key));
        if (value == null) {
            throw new IllegalArgumentException("企微同步缺少" + fieldName);
        }
        return value;
    }

    private String trimToNull(String value) {
        return StrUtil.isBlank(value) ? null : value.trim();
    }
}
