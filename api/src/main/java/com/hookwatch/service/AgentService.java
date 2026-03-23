package com.hookwatch.service;

import com.hookwatch.domain.Agent;
import com.hookwatch.dto.AgentDto;
import com.hookwatch.dto.AgentMetricsDto;
import com.hookwatch.repository.AgentRepository;
import com.hookwatch.repository.TraceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public List<Agent> findAll() {
        return agentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Agent> findById(UUID id) {
        return agentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public AgentMetricsDto getMetrics(UUID agentId) {
        long total = traceRepository.countByAgentId(agentId);
        double avgTokens = Optional.ofNullable(traceRepository.avgTokensByAgentId(agentId)).orElse(0.0);
        double avgCost = Optional.ofNullable(traceRepository.avgCostByAgentId(agentId)).orElse(0.0);
        long completed = traceRepository.countCompletedByAgentId(agentId);
        double successRate = total > 0 ? (double) completed / total * 100.0 : 0.0;

        // p95 latency requires a percentile query not available in H2; returns 0 until PostgreSQL native query is added
        long p95LatencyMs = 0L;

        return new AgentMetricsDto(total, avgTokens, avgCost, successRate, p95LatencyMs);
    }
}
