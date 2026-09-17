package com.finex.auth.agent.desensitization;

import com.finex.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 脱敏管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth/agent/desensitization")
@RequiredArgsConstructor
public class DesensitizationController {

    private final DesensitizationConfig config;
    private final DesensitizationService desensitizationService;
    private final MappingTableRepository mappingTableRepository;
    private final EntityTypeConfigRepository entityTypeConfigRepository;

    /**
     * 获取所有实体类型配置
     */
    @GetMapping("/entity-types")
    public Result<List<DesensitizationDTO.EntityTypeDTO>> getEntityTypes() {
        List<DesensitizationDTO.EntityTypeDTO> entityTypes = entityTypeConfigRepository.findAll().stream()
                .map(entityTypeConfigRepository::toDto)
                .collect(Collectors.toList());

        return Result.success(entityTypes);
    }

    /**
     * 保存实体类型配置
     */
    @PostMapping("/entity-types")
    public Result<String> saveEntityType(@RequestBody DesensitizationDTO.SaveEntityTypeRequest request) {
        try {
            validateEntityTypeRequest(request);
            log.info("保存实体类型配置: {}", request);
            entityTypeConfigRepository.save(request);
            return Result.success("保存成功");
        } catch (Exception e) {
            log.error("保存实体类型配置失败", e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    /**
     * 删除实体类型配置
     */
    @DeleteMapping("/entity-types/{code}")
    public Result<String> deleteEntityType(@PathVariable String code) {
        try {
            log.info("删除实体类型配置: {}", code);
            entityTypeConfigRepository.deleteByCode(code);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除实体类型配置失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 更新实体类型启用状态
     */
    @PutMapping("/entity-types/{code}/enabled")
    public Result<String> updateEntityTypeEnabled(@PathVariable String code, @RequestParam Boolean enabled) {
        try {
            log.info("更新实体类型 {} 启用状态: {}", code, enabled);
            entityTypeConfigRepository.updateEnabled(code, Boolean.TRUE.equals(enabled));
            return Result.success("更新成功");
        } catch (Exception e) {
            log.error("更新实体类型启用状态失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 脱敏测试
     */
    @PostMapping("/test")
    public Result<DesensitizationDTO.TestResponse> test(@RequestBody DesensitizationDTO.TestRequest request) {
        try {
            log.info("执行脱敏测试，策略: {}", request.getStrategy());

            String originalText = request.getText();

            DesensitizedData result = desensitizationService.desensitize(
                    UUID.randomUUID().toString(),
                    originalText,
                    request.getStrategy()
            );

            String aiResponse = "已为您处理：" + result.getMaskedText();
            String restoredResponse = desensitizationService.restore(result.getMappingId(), aiResponse);

            DesensitizationDTO.TestResponse response = new DesensitizationDTO.TestResponse();
            response.setOriginalText(originalText);
            response.setMaskedText(result.getMaskedText());
            response.setAiResponse(aiResponse);
            response.setRestoredText(restoredResponse);
            response.setMappingId(result.getMappingId());
            response.setStrategy(result.getStrategy());
            response.setMappings(result.getEntityMappings());
            response.setSuccess(result.isSuccess());
            response.setErrorMessage(result.getErrorMessage());

            return Result.success(response);

        } catch (Exception e) {
            log.error("脱敏测试失败", e);
            return Result.error("测试失败: " + e.getMessage());
        }
    }

    /**
     * 查询映射表列表
     */
    @GetMapping("/mappings")
    public Result<Map<String, Object>> getMappings(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String sessionId) {
        try {
            int safePage = Math.max(page == null ? 1 : page, 1);
            int safePageSize = Math.max(Math.min(pageSize == null ? 20 : pageSize, 100), 1);

            List<DesensitizationDTO.MappingTableDTO> items = mappingTableRepository
                    .findPage(sessionId, safePage, safePageSize)
                    .stream()
                    .map(this::toMappingDto)
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("items", items);
            result.put("total", mappingTableRepository.count(sessionId));
            result.put("page", safePage);
            result.put("pageSize", safePageSize);

            return Result.success(result);

        } catch (Exception e) {
            log.error("查询映射表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 新增映射表
     */
    @PostMapping("/mappings")
    public Result<DesensitizationDTO.MappingTableDTO> saveMapping(@RequestBody DesensitizationDTO.SaveMappingRequest request) {
        try {
            validateMappingRequest(request);

            Instant now = Instant.now();
            int ttlHours = request.getTtlHours() == null ? config.getMapping().getTtlHours() : request.getTtlHours();

            MappingTable mappingTable = new MappingTable();
            mappingTable.setId(UUID.randomUUID().toString());
            mappingTable.setSessionId(request.getSessionId().trim());
            mappingTable.setCreatedAt(now);
            mappingTable.setExpiresAt(now.plus(Math.max(ttlHours, 1), ChronoUnit.HOURS));

            request.getMappings().forEach((placeholder, original) -> {
                String normalizedPlaceholder = normalizePlaceholder(placeholder);
                if (!normalizedPlaceholder.isBlank() && original != null && !original.isBlank()) {
                    mappingTable.addMapping(normalizedPlaceholder, original.trim());
                }
            });

            if (mappingTable.getPlaceholderToOriginal().isEmpty()) {
                return Result.badRequest("映射关系不能为空");
            }

            mappingTableRepository.save(mappingTable);
            return Result.success(toMappingDto(mappingTable));
        } catch (IllegalArgumentException e) {
            return Result.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("保存映射表失败", e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    /**
     * 获取映射表统计
     */
    @GetMapping("/mappings/stats")
    public Result<Map<String, Object>> getMappingStats() {
        try {
            Instant now = Instant.now();

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", mappingTableRepository.count());
            stats.put("activeCount", mappingTableRepository.countActive(now));
            stats.put("expiredCount", mappingTableRepository.countExpired(now));

            return Result.success(stats);

        } catch (Exception e) {
            log.error("获取映射表统计失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 手动清理过期映射
     */
    @PostMapping("/mappings/cleanup")
    public Result<String> cleanupMappings() {
        try {
            desensitizationService.cleanupExpiredMappings();
            return Result.success("清理完成");
        } catch (Exception e) {
            log.error("清理过期映射失败", e);
            return Result.error("清理失败: " + e.getMessage());
        }
    }

    private void validateEntityTypeRequest(DesensitizationDTO.SaveEntityTypeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (isBlank(request.getCode())) {
            throw new IllegalArgumentException("实体类型编码不能为空");
        }
        if (isBlank(request.getName())) {
            throw new IllegalArgumentException("实体类型名称不能为空");
        }
        if (isBlank(request.getPlaceholderPrefix())) {
            throw new IllegalArgumentException("占位符前缀不能为空");
        }
    }

    private void validateMappingRequest(DesensitizationDTO.SaveMappingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (isBlank(request.getSessionId())) {
            throw new IllegalArgumentException("会话ID不能为空");
        }
        if (request.getMappings() == null || request.getMappings().isEmpty()) {
            throw new IllegalArgumentException("映射关系不能为空");
        }
    }

    private DesensitizationDTO.MappingTableDTO toMappingDto(MappingTable table) {
        DesensitizationDTO.MappingTableDTO dto = new DesensitizationDTO.MappingTableDTO();
        dto.setId(table.getId());
        dto.setSessionId(table.getSessionId());
        dto.setMappings(table.getPlaceholderToOriginal());
        dto.setCreatedAt(table.getCreatedAt());
        dto.setExpiresAt(table.getExpiresAt());
        dto.setExpired(table.isExpired());
        return dto;
    }

    private String normalizePlaceholder(String placeholder) {
        if (placeholder == null) {
            return "";
        }
        return placeholder.trim().replaceAll("^\\[", "").replaceAll("]$", "");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
