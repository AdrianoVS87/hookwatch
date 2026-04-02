package com.hookwatch.dto;

import com.hookwatch.domain.FailureFingerprint;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * DTO for fingerprint list and trend responses.
 */
public final class FingerprintDto {

    private FingerprintDto() {}

    @Schema(description = "A grouped failure fingerprint entry")
    public record FingerprintSummary(
            @Schema(description = "Fingerprint ID") String id,
            @Schema(description = "Agent ID") String agentId,
            @Schema(description = "SHA-256 hash") String hash,
            @Schema(description = "Error message excerpt") String errorMessage,
            @Schema(description = "Span type that triggered the failure") String spanType,
            @Schema(description = "Model involved") String model,
            @Schema(description = "First seen timestamp") String firstSeenAt,
            @Schema(description = "Last seen timestamp") String lastSeenAt,
            @Schema(description = "Total occurrences") int occurrenceCount
    ) {
        /**
         * Converts a domain entity to a summary DTO.
         */
        public static FingerprintSummary fromEntity(FailureFingerprint fp) {
            return new FingerprintSummary(
                    fp.getId().toString(),
                    fp.getAgentId().toString(),
                    fp.getHash(),
                    fp.getErrorMessage(),
                    fp.getSpanType(),
                    fp.getModel(),
                    fp.getFirstSeenAt().toString(),
                    fp.getLastSeenAt().toString(),
                    fp.getOccurrenceCount()
            );
        }
    }

    @Schema(description = "Daily occurrence count for a fingerprint trend")
    public record TrendPoint(
            @Schema(description = "Date (ISO-8601)", example = "2026-03-28") String date,
            @Schema(description = "Occurrence count on that day") int count
    ) {}

    @Schema(description = "Trend response for a single fingerprint")
    public record TrendResponse(
            @Schema(description = "Fingerprint ID") String fingerprintId,
            @Schema(description = "Daily timeseries") List<TrendPoint> trend
    ) {}
}
