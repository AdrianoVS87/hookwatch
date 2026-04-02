package com.hookwatch.service;

import com.hookwatch.domain.Score;
import com.hookwatch.domain.Span;
import com.hookwatch.domain.Trace;
import com.hookwatch.dto.AutoEvalResponseDto;
import com.hookwatch.dto.ScoreDto;
import com.hookwatch.dto.ScoreSummaryDto;
import com.hookwatch.repository.AgentRepository;
import com.hookwatch.repository.ScoreRepository;
import com.hookwatch.repository.TraceRepository;
import com.hookwatch.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScoreService {

    private final ScoreRepository scoreRepository;
    private final TraceRepository traceRepository;
    private final AgentRepository agentRepository;

    @Transactional
    public ScoreDto create(UUID traceId, ScoreDto dto) {
        UUID tenantId = TenantContext.get();
        Trace trace = resolveTraceForTenant(traceId, tenantId);

        Score score = new Score();
        score.setTrace(trace);
        score.setName(dto.getName());
        score.setDataType(dto.getDataType());
        score.setNumericValue(dto.getNumericValue());
        score.setStringValue(dto.getStringValue());
        score.setBooleanValue(dto.getBooleanValue());
        score.setComment(dto.getComment());
        score.setSource(dto.getSource());

        Score saved = scoreRepository.save(score);
        return ScoreDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ScoreDto> listByTraceId(UUID traceId) {
        UUID tenantId = TenantContext.get();
        resolveTraceForTenant(traceId, tenantId);

        return scoreRepository.findByTraceId(traceId).stream()
                .map(ScoreDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ScoreSummaryDto summarizeByAgent(UUID agentId) {
        UUID tenantId = TenantContext.get();
        if (tenantId != null && agentRepository.findByIdAndTenantId(agentId, tenantId).isEmpty()) {
            if (agentRepository.existsById(agentId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access denied: agent does not belong to your tenant");
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found");
        }

        List<ScoreSummaryDto.NumericAverage> numericAverages = scoreRepository
                .avgNumericScoresByAgentId(agentId).stream()
                .map(row -> new ScoreSummaryDto.NumericAverage((String) row[0], (Double) row[1]))
                .collect(Collectors.toList());

        Map<String, Map<String, Long>> categoricalDist = new LinkedHashMap<>();
        for (Object[] row : scoreRepository.categoricalDistributionByAgentId(agentId)) {
            String name = (String) row[0];
            String value = (String) row[1];
            long count = (Long) row[2];
            categoricalDist.computeIfAbsent(name, k -> new LinkedHashMap<>()).put(value, count);
        }

        return new ScoreSummaryDto(numericAverages, categoricalDist);
    }

    @Transactional
    public AutoEvalResponseDto autoEvaluateRecentTraces(UUID agentId, int limit) {
        UUID tenantId = TenantContext.get();
        if (tenantId != null && agentRepository.findByIdAndTenantId(agentId, tenantId).isEmpty()) {
            if (agentRepository.existsById(agentId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access denied: agent does not belong to your tenant");
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found");
        }

        int cappedLimit = Math.max(1, Math.min(limit, 200));
        List<Trace> traces = traceRepository.findByAgentId(agentId, org.springframework.data.domain.PageRequest.of(0, cappedLimit)).getContent();

        int evaluated = 0;
        int skipped = 0;
        double scoreSum = 0.0;

        for (Trace trace : traces) {
            if (scoreRepository.existsByTraceIdAndName(trace.getId(), "auto_quality_v1")) {
                skipped++;
                continue;
            }

            double scoreValue = heuristicQualityScore(trace);
            Score score = new Score();
            score.setTrace(trace);
            score.setName("auto_quality_v1");
            score.setDataType(Score.DataType.NUMERIC);
            score.setNumericValue(scoreValue);
            score.setComment("Auto-generated heuristic quality score");
            score.setSource(Score.Source.LLM_JUDGE);
            scoreRepository.save(score);

            evaluated++;
            scoreSum += scoreValue;
        }

        double avg = evaluated == 0 ? 0.0 : scoreSum / evaluated;
        return new AutoEvalResponseDto(evaluated, skipped, avg);
    }

    private double heuristicQualityScore(Trace trace) {
        double score = 1.0;

        if (trace.getStatus() == Trace.Status.FAILED) score -= 0.7;
        if (trace.getTotalCost() != null && trace.getTotalCost().compareTo(java.math.BigDecimal.valueOf(0.05)) > 0) score -= 0.1;
        if (trace.getTotalTokens() != null && trace.getTotalTokens() > 20000) score -= 0.1;

        long failedSpans = trace.getSpans() == null ? 0 : trace.getSpans().stream()
                .filter(s -> s.getStatus() == Span.Status.FAILED)
                .count();
        score -= Math.min(0.2, failedSpans * 0.05);

        if (score < 0) score = 0;
        if (score > 1) score = 1;
        return score;
    }

    private Trace resolveTraceForTenant(UUID traceId, UUID tenantId) {
        if (tenantId != null) {
            Optional<Trace> trace = traceRepository.findByIdAndTenantId(traceId, tenantId);
            if (trace.isEmpty()) {
                if (traceRepository.existsById(traceId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Access denied: trace does not belong to your tenant");
                }
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trace not found");
            }
            return trace.get();
        }
        return traceRepository.findById(traceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trace not found"));
    }
}
