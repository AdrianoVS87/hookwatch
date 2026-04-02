package com.hookwatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * OTel compliance validation report for a single trace or aggregate summary.
 */
public final class ComplianceReportDto {

    private ComplianceReportDto() {}

    @Schema(description = "Compliance gap detail")
    public record Gap(
            @Schema(description = "Field or attribute that failed validation", example = "resource.service.name")
            String field,
            @Schema(description = "Expected value or format", example = "non-empty string")
            String expected,
            @Schema(description = "Actual value found", example = "null")
            String actual,
            @Schema(description = "Severity level", example = "ERROR")
            Severity severity
    ) {}

    public enum Severity { ERROR, WARNING, INFO }

    @Schema(description = "Full compliance report for a single trace")
    public record TraceReport(
            @Schema(description = "Trace ID") String traceId,
            @Schema(description = "Total checks performed") int totalChecks,
            @Schema(description = "Checks passed") int passed,
            @Schema(description = "Checks failed") int failed,
            @Schema(description = "Detailed gap list") List<Gap> gaps
    ) {}

    @Schema(description = "Aggregate compliance summary for an agent")
    public record AgentSummary(
            @Schema(description = "Agent ID") String agentId,
            @Schema(description = "Total traces checked") int totalTraces,
            @Schema(description = "Fully compliant traces") int compliantTraces,
            @Schema(description = "Compliance rate (0.0-1.0)") double complianceRate,
            @Schema(description = "Most common gaps across traces") List<GapFrequency> topGaps
    ) {}

    @Schema(description = "Gap frequency entry for aggregate view")
    public record GapFrequency(
            @Schema(description = "Field name") String field,
            @Schema(description = "Number of traces with this gap") int count,
            @Schema(description = "Severity") Severity severity
    ) {}
}
