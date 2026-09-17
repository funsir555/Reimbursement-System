package com.finex.auth.agent.desensitization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DesensitizationServiceTest {

    @Test
    void regexStrategyPreservesBusinessContextForBroadPersonRule() {
        DesensitizationConfig config = new DesensitizationConfig();
        config.setEntityTypes(List.of(
                entityType("PERSON", "PERSON", List.of("[一-龥]{2,4}")),
                entityType("DOCUMENT_ID", "DOC", List.of("EXP\\d{8,12}")),
                entityType("AMOUNT", "AMOUNT", List.of("\\d+(\\.\\d{1,2})?元")),
                entityType("PHONE", "PHONE", List.of("1[3-9]\\d{9}"))
        ));
        config.setStrategies(List.of());
        DesensitizationConfig.MappingConfig mappingConfig = new DesensitizationConfig.MappingConfig();
        mappingConfig.setTtlHours(24);
        config.setMapping(mappingConfig);

        MappingTableRepository mappingRepository = mock(MappingTableRepository.class);
        EntityTypeConfigRepository entityRepository = mock(EntityTypeConfigRepository.class);
        DesensitizationService service = new DesensitizationService(
                config,
                mappingRepository,
                entityRepository,
                new ObjectMapper()
        );

        String text = "张三提交了报销单EXP20240101001，金额1200元，手机号13812345678";
        DesensitizedData result = service.desensitize("session-test", text, "REGEX");

        assertTrue(result.isSuccess());
        assertEquals(
                "[PERSON_001]提交了报销单[DOC_001]，金额[AMOUNT_001]，手机号[PHONE_001]",
                result.getMaskedText()
        );
        assertEquals("张三", result.getEntityMappings().get("PERSON_001"));
        assertEquals("EXP20240101001", result.getEntityMappings().get("DOC_001"));
        assertEquals("1200元", result.getEntityMappings().get("AMOUNT_001"));
        assertEquals("13812345678", result.getEntityMappings().get("PHONE_001"));
    }

    @Test
    void requestedStrategyIsKeptForCompatibilityButStillUsesRegex() {
        DesensitizationConfig config = new DesensitizationConfig();
        config.setEntityTypes(List.of(
                entityType("PERSON", "PERSON", List.of("[一-龥]{2,4}")),
                entityType("DOCUMENT_ID", "DOC", List.of("EXP\\d{8,12}")),
                entityType("AMOUNT", "AMOUNT", List.of("\\d+(\\.\\d{1,2})?元")),
                entityType("PHONE", "PHONE", List.of("1[3-9]\\d{9}"))
        ));
        DesensitizationConfig.MappingConfig mappingConfig = new DesensitizationConfig.MappingConfig();
        mappingConfig.setTtlHours(24);
        config.setMapping(mappingConfig);

        MappingTableRepository mappingRepository = mock(MappingTableRepository.class);
        EntityTypeConfigRepository entityRepository = mock(EntityTypeConfigRepository.class);
        DesensitizationService service = new DesensitizationService(
                config,
                mappingRepository,
                entityRepository,
                new ObjectMapper()
        );

        String text = "张三提交了报销单EXP20240101001，金额1200元，手机号13812345678";
        DesensitizedData result = service.desensitize("session-test", text, "UNKNOWN");

        assertTrue(result.isSuccess());
        assertEquals("REGEX", result.getStrategy());
        assertEquals(
                "[PERSON_001]提交了报销单[DOC_001]，金额[AMOUNT_001]，手机号[PHONE_001]",
                result.getMaskedText()
        );
        assertEquals("张三", result.getEntityMappings().get("PERSON_001"));
    }

    @Test
    void restoreReturnsMappedTextWithoutPostValidationBlocking() {
        DesensitizationConfig config = new DesensitizationConfig();
        DesensitizationConfig.PostValidation postValidation = new DesensitizationConfig.PostValidation();
        postValidation.setEnabled(true);
        DesensitizationConfig.LeakagePattern phonePattern = new DesensitizationConfig.LeakagePattern();
        phonePattern.setType("PHONE");
        phonePattern.setPattern("1[3-9]\\d{9}");
        phonePattern.setSeverity("HIGH");
        postValidation.setLeakagePatterns(List.of(phonePattern));
        config.setPostValidation(postValidation);

        MappingTable mappingTable = new MappingTable();
        mappingTable.setId("mapping-test");
        mappingTable.setSessionId("session-test");
        mappingTable.setCreatedAt(Instant.now());
        mappingTable.setExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS));
        mappingTable.addMapping("PERSON_001", "张三");

        MappingTableRepository mappingRepository = mock(MappingTableRepository.class);
        when(mappingRepository.findById("mapping-test")).thenReturn(mappingTable);
        EntityTypeConfigRepository entityRepository = mock(EntityTypeConfigRepository.class);
        DesensitizationService service = new DesensitizationService(
                config,
                mappingRepository,
                entityRepository,
                new ObjectMapper()
        );

        String restored = service.restore("mapping-test", "[PERSON_001]确认，备用电话13899998888");

        assertEquals("张三确认，备用电话13899998888", restored);
    }

    @Test
    void structuredJsonMasksConfiguredFieldsAndPreservesEvidenceReferences() {
        DesensitizationConfig config = new DesensitizationConfig();
        config.setEntityTypes(List.of());
        DesensitizationConfig.MappingConfig mappingConfig = new DesensitizationConfig.MappingConfig();
        mappingConfig.setTtlHours(24);
        config.setMapping(mappingConfig);

        MappingTableRepository mappingRepository = mock(MappingTableRepository.class);
        EntityTypeConfigRepository entityRepository = mock(EntityTypeConfigRepository.class);
        DesensitizationService service = new DesensitizationService(
                config,
                mappingRepository,
                entityRepository,
                new ObjectMapper()
        );
        String json = "{\"question\":\"查询远智的报销单\",\"context\":\"请汇总审批状态\","
                + "\"variables\":{\"normalizedKeyword\":\"远智\"},\"records\":[{"
                + "\"documentId\":64,\"documentCode\":\"DOC202604240002\","
                + "\"title\":\"朱敏2026年4月报销\",\"amount\":2173.91,"
                + "\"statusLabel\":\"待审批\",\"evidenceRef\":\"EXPENSE_DOCUMENT:64\"}]}";

        DesensitizedData result = service.desensitizeJsonFields("session-json", json, Map.of(
                "normalizedKeyword", "KEYWORD",
                "documentId", "RECORD",
                "documentCode", "DOC",
                "title", "DETAIL",
                "amount", "AMOUNT"
        ), Set.of("KEYWORD"));

        assertTrue(result.isSuccess());
        assertFalse(result.getMaskedText().contains("远智"));
        assertFalse(result.getMaskedText().contains("DOC202604240002"));
        assertFalse(result.getMaskedText().contains("朱敏"));
        assertFalse(result.getMaskedText().contains("2173.91"));
        assertTrue(result.getMaskedText().contains("EXPENSE_DOCUMENT:64"));
        assertTrue(result.getMaskedText().contains("待审批"));
        assertTrue(result.getMaskedText().contains("查询[KEYWORD_001]的报销单"));
        assertTrue(result.getMaskedText().contains("请汇总审批状态"));
        assertEquals("远智", result.getEntityMappings().get("KEYWORD_001"));
        assertEquals("DOC202604240002", result.getEntityMappings().get("DOC_001"));
        assertEquals("2173.91", result.getEntityMappings().get("AMOUNT_001"));
    }

    private DesensitizationConfig.EntityType entityType(String code, String prefix, List<String> patterns) {
        DesensitizationConfig.EntityType entityType = new DesensitizationConfig.EntityType();
        entityType.setCode(code);
        entityType.setPlaceholderPrefix(prefix);
        entityType.setRegexPatterns(patterns);
        entityType.setEnabled(true);
        return entityType;
    }
}
