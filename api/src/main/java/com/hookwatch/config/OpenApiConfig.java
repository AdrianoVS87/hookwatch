package com.hookwatch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hookWatchOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("HookWatch API")
                        .description("Observability API for AI agent traces, spans, scoring, and analytics")
                        .version("v1")
                        .contact(new Contact()
                                .name("HookWatch")
                                .url("https://github.com/AdrianoVS87/hookwatch")))
                .addTagsItem(new Tag().name("Tenants").description("Tenant lifecycle and API key bootstrap"))
                .addTagsItem(new Tag().name("Agents").description("Agent registration and metrics"))
                .addTagsItem(new Tag().name("Traces").description("Trace ingestion, listing, retrieval, and OTEL conversion"))
                .addTagsItem(new Tag().name("Spans").description("Live span events and streaming"))
                .addTagsItem(new Tag().name("Scores").description("Trace scoring and score aggregation"))
                .addTagsItem(new Tag().name("Analytics").description("Usage and cost analytics"))
                .addTagsItem(new Tag().name("System").description("System-level and operational endpoints"));
    }

    @Bean
    public GroupedOpenApi tenantsApiGroup() {
        return GroupedOpenApi.builder()
                .group("tenants")
                .pathsToMatch("/api/v1/tenants/**")
                .build();
    }

    @Bean
    public GroupedOpenApi agentsApiGroup() {
        return GroupedOpenApi.builder()
                .group("agents")
                .pathsToMatch("/api/v1/agents/**")
                .build();
    }

    @Bean
    public GroupedOpenApi tracesApiGroup() {
        return GroupedOpenApi.builder()
                .group("traces")
                .pathsToMatch("/api/v1/traces/**", "/api/v1/ingest/otel/**")
                .build();
    }

    @Bean
    public GroupedOpenApi spansApiGroup() {
        return GroupedOpenApi.builder()
                .group("spans")
                .pathsToMatch("/api/v1/traces/*/stream")
                .build();
    }

    @Bean
    public GroupedOpenApi scoresApiGroup() {
        return GroupedOpenApi.builder()
                .group("scores")
                .pathsToMatch("/api/v1/traces/*/scores/**", "/api/v1/agents/*/scores/**")
                .build();
    }

    @Bean
    public GroupedOpenApi analyticsApiGroup() {
        return GroupedOpenApi.builder()
                .group("analytics")
                .pathsToMatch("/api/v1/analytics/**")
                .build();
    }

    @Bean
    public GroupedOpenApi systemApiGroup() {
        return GroupedOpenApi.builder()
                .group("system")
                .pathsToMatch("/api/v1/health/**", "/api/webhooks/**")
                .build();
    }
}
