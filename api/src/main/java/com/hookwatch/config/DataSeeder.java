package com.hookwatch.config;

import com.hookwatch.domain.*;
import com.hookwatch.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final AgentRepository agentRepository;
    private final TraceRepository traceRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (tenantRepository.findByApiKey("demo-key-hookwatch").isPresent()) {
            log.info("Demo data already seeded, skipping.");
            return;
        }

        Tenant tenant = tenantRepository.save(
            Tenant.builder().name("Demo Tenant").apiKey("demo-key-hookwatch").build()
        );

        Agent assistant = agentRepository.save(
            Agent.builder()
                .tenantId(tenant.getId())
                .name("OpenClaw Assistant")
                .description("General-purpose assistant for developer workflows")
                .build()
        );

        Agent reviewer = agentRepository.save(
            Agent.builder()
                .tenantId(tenant.getId())
                .name("Code Review Bot")
                .description("Automated code review and quality analysis")
                .build()
        );

        record TraceConfig(UUID agentId, Trace.Status status, int tokens, BigDecimal cost) {}

        List<TraceConfig> configs = List.of(
            new TraceConfig(assistant.getId(), Trace.Status.COMPLETED, 1823, new BigDecimal("0.027345")),
            new TraceConfig(assistant.getId(), Trace.Status.COMPLETED, 3241, new BigDecimal("0.048615")),
            new TraceConfig(assistant.getId(), Trace.Status.FAILED,     512, new BigDecimal("0.007680")),
            new TraceConfig(assistant.getId(), Trace.Status.COMPLETED, 2109, new BigDecimal("0.031635")),
            new TraceConfig(assistant.getId(), Trace.Status.RUNNING,    890, null),
            new TraceConfig(reviewer.getId(),  Trace.Status.COMPLETED, 4502, new BigDecimal("0.067530")),
            new TraceConfig(reviewer.getId(),  Trace.Status.COMPLETED, 1200, new BigDecimal("0.018000")),
            new TraceConfig(reviewer.getId(),  Trace.Status.FAILED,    2800, new BigDecimal("0.042000")),
            new TraceConfig(reviewer.getId(),  Trace.Status.COMPLETED, 3700, new BigDecimal("0.055500")),
            new TraceConfig(reviewer.getId(),  Trace.Status.COMPLETED,  990, new BigDecimal("0.014850"))
        );

        for (TraceConfig cfg : configs) {
            Trace trace = new Trace();
            trace.setAgentId(cfg.agentId());
            trace.setStatus(cfg.status());
            trace.setTotalTokens(cfg.tokens());
            trace.setTotalCost(cfg.cost());
            trace.setMetadata(Map.of("source", "demo"));

            Trace saved = traceRepository.save(trace);

            if (cfg.status() != Trace.Status.RUNNING) {
                saved.setCompletedAt(saved.getStartedAt().plusMillis(500 + (long)(Math.random() * 4500)));
            }

            Span toolSpan = new Span();
            toolSpan.setTraceId(saved.getId());
            toolSpan.setName("web_search");
            toolSpan.setType(Span.Type.TOOL_CALL);
            toolSpan.setStatus(Span.Status.COMPLETED);
            toolSpan.setSortOrder(0);
            toolSpan.setInput("{\"query\": \"Spring Boot best practices\"}");
            toolSpan.setOutput("{\"results\": [\"tip1\", \"tip2\"]}");

            Span llmSpan = new Span();
            llmSpan.setTraceId(saved.getId());
            llmSpan.setName("claude-sonnet-completion");
            llmSpan.setType(Span.Type.LLM_CALL);
            llmSpan.setStatus(cfg.status() == Trace.Status.FAILED ? Span.Status.FAILED : Span.Status.COMPLETED);
            llmSpan.setModel("claude-sonnet-4-6");
            llmSpan.setInputTokens(cfg.tokens() / 3);
            llmSpan.setOutputTokens(cfg.tokens() - cfg.tokens() / 3);
            llmSpan.setCost(cfg.cost() != null ? cfg.cost().divide(BigDecimal.valueOf(2)) : null);
            llmSpan.setSortOrder(1);
            llmSpan.setInput("You are a helpful assistant.");
            llmSpan.setOutput(cfg.status() == Trace.Status.FAILED ? null : "Here is my analysis of the code...");
            llmSpan.setError(cfg.status() == Trace.Status.FAILED ? "Rate limit exceeded after 3 retries" : null);

            // Mutable list required — Hibernate may call clear() during merge operations
            List<Span> spans = new ArrayList<>();
            spans.add(toolSpan);
            spans.add(llmSpan);
            saved.setSpans(spans);
            traceRepository.save(saved);
        }

        log.info("Demo data seeded: 1 tenant, 2 agents, 10 traces with spans.");
    }
}
