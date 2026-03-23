package com.hookwatch.service;

import com.hookwatch.domain.Agent;
import com.hookwatch.dto.AgentDto;
import com.hookwatch.dto.AgentMetricsDto;
import com.hookwatch.repository.AgentRepository;
import com.hookwatch.repository.TraceRepository;
import com.hookwatch.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentRepository agentRepository;
    private final TraceRepository traceRepository;

    public Agent create(AgentDto dto) {
        Agent agent = Agent.builder()
                .tenantId(dto.getTenantId())
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
        return agentRepository.save(agent);
    }

    /**
     * Returns all agents belonging to the authenticated tenant.
     * Tenant ID is sourced from TenantContext (set by ApiKeyFilter).
     */
    public List<Agent> listForCurrentTenant() {
        UUID tenantId = TenantContext.get();
        return agentRepository.findByTenantId(tenantId);
    }

    /**
     * Finds an agent by ID, enforcing it belongs to the authenticated tenant.
     */
    public Optional<Agent> findById(UUID id) {
        UUID tenantId = TenantContext.get();
        if (tenantId != null) {
            return agentRepository.findByIdAndTenantId(id, tenantId);
        }
        return agentRepository.findById(id);
    }

    public AgentMetricsDto getMetrics(UUID agentId) {
        long total = traceRepository.countByAgentId(agentId);
        double avgTokens = Optional.ofNullable(traceRepository.avgTokensByAgentId(agentId)).orElse(0.0);
        double avgCost = Optional.ofNullable(traceRepository.avgCostByAgentId(agentId)).orElse(0.0);
        long completed = traceRepository.countCompletedByAgentId(agentId);
        double successRate = total > 0 ? (double) completed / total * 100.0 : 0.0;

        // p95 latency placeholder — requires percentile query or metrics backend
        long p95LatencyMs = 0L;

        return new AgentMetricsDto(total, avgTokens, avgCost, successRate, p95LatencyMs);
    }
}
