package com.hookwatch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hookwatch.domain.Tenant;
import com.hookwatch.dto.TenantDto;
import com.hookwatch.filter.ApiKeyFilter;
import com.hookwatch.service.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TenantController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApiKeyFilter.class))
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TenantService tenantService;

    @Test
    void createTenant_returnsCreated() throws Exception {
        TenantDto dto = new TenantDto();
        dto.setName("acme");

        Tenant tenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name("acme")
                .apiKey(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .build();

        when(tenantService.create(any())).thenReturn(tenant);

        mockMvc.perform(post("/api/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("acme"))
                .andExpect(jsonPath("$.apiKey").isNotEmpty());
    }

    @Test
    void createTenant_withBlankName_returnsBadRequest() throws Exception {
        TenantDto dto = new TenantDto();
        dto.setName("");

        mockMvc.perform(post("/api/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}
