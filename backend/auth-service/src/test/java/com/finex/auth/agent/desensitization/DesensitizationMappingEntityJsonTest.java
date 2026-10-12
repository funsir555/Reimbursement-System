package com.finex.auth.agent.desensitization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DesensitizationMappingEntityJsonTest {

    @Test
    void mappingJsonRoundTripsWithInjectedMapper() {
        ObjectMapper configuredMapper = new ObjectMapper();
        MappingTable table = new MappingTable();
        table.setId("MAP-1");
        table.setSessionId("SESSION-1");
        table.addMapping("[NAME_1]", "张三");

        DesensitizationMappingEntity entity = DesensitizationMappingEntity.fromMappingTable(
                table, "tenant-a", configuredMapper);

        assertThat(entity.getMappings()).contains("张三", "[NAME_1]");
        assertThat(entity.toMappingTable(configuredMapper).getOriginal("[NAME_1]")).isEqualTo("张三");
    }
}
