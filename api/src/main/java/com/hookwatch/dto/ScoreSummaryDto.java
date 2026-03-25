package com.hookwatch.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ScoreSummaryDto {
    private List<NumericAverage> numericAverages;
    private Map<String, Map<String, Long>> categoricalDistributions;

    @Data
    @AllArgsConstructor
    public static class NumericAverage {
        private String name;
        private double average;
    }
}
