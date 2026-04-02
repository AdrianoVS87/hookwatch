package com.hookwatch.dto;

public record AutoEvalResponseDto(
        int evaluatedNow,
        int skippedAlreadyEvaluated,
        double averageScore
) {}
