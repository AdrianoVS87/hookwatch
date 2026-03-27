package com.hookwatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@Schema(description = "Aggregate score summary for an agent")
public class ScoreSummaryDto {
    @Schema(description = "Average values for numeric metrics")
    private List<NumericAverage> numericAverages;

    @Schema(description = "Categorical distributions by metric and value")
    private Map<String, Map<String, Long>> categoricalDistributions;

    @Data
    @AllArgsConstructor
    @Schema(description = "Average value for one numeric score name")
    public static class NumericAverage {
        @Schema(description = "Score metric name", example = "faithfulness")
        private String name;

        @Schema(description = "Average value", example = "0.91")
        private double average;
    }
}
