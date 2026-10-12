package com.finex.auth.agent.desensitization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finex.auth.agent.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntityTypeConfigRepositoryTest {

    private static final String TENANT_ID = "default";
    private static final String COMPANY_ID = "COMPANY_TEST";

    private EntityTypeConfigMapper mapper;
    private EntityTypeConfigRepository repository;

    @BeforeEach
    void setUp() {
        mapper = mock(EntityTypeConfigMapper.class);
        repository = new EntityTypeConfigRepository(mapper, new ObjectMapper());
        TenantContext.setTenantId(TENANT_ID);
        TenantContext.setCompanyId(COMPANY_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void readsAreScopedToCurrentCompany() {
        when(mapper.findAll(TENANT_ID, COMPANY_ID)).thenReturn(List.of());
        when(mapper.findAllEnabled(TENANT_ID, COMPANY_ID)).thenReturn(List.of());

        repository.findAll();
        repository.findAllEnabled();

        verify(mapper).findAll(TENANT_ID, COMPANY_ID);
        verify(mapper).findAllEnabled(TENANT_ID, COMPANY_ID);
    }

    @Test
    void mutationsAreScopedToCurrentCompany() {
        DesensitizationDTO.SaveEntityTypeRequest request = new DesensitizationDTO.SaveEntityTypeRequest();
        request.setCode("PHONE");
        request.setName("手机号");
        request.setPlaceholderPrefix("PHONE");
        request.setRegexPatterns(List.of("1[3-9]\\d{9}"));
        request.setExamples(List.of("13812345678"));
        request.setEnabled(true);

        EntityTypeConfigEntity existing = new EntityTypeConfigEntity();
        existing.setId(1L);
        existing.setTenantId(TENANT_ID);
        existing.setCompanyId(COMPANY_ID);
        existing.setCode("PHONE");
        when(mapper.findByCode("PHONE", TENANT_ID, COMPANY_ID)).thenReturn(existing);

        repository.save(request);
        repository.updateEnabled("PHONE", false);
        repository.deleteByCode("PHONE");

        verify(mapper).findByCode("PHONE", TENANT_ID, COMPANY_ID);
        verify(mapper).updateEnabled("PHONE", TENANT_ID, COMPANY_ID, false);
        verify(mapper).deleteByCode("PHONE", TENANT_ID, COMPANY_ID);
    }
}
