package com.hookwatch.dto;

import java.util.List;

public record MemoryLineageDto(
        String traceId,
        int retrievalSpanCount,
        List<String> retrievalSpanNames,
        List<String> memoryReferences,
        String inferredOutcome
) {}
