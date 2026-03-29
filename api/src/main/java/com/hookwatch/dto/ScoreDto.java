package com.hookwatch.dto;

import com.hookwatch.domain.Score;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Schema(description = "Trace score payload")
public class ScoreDto {
    @Schema(description = "Score id", example = "f89c0af6-1231-4ca6-96bf-0051d4f47d8e")
    private UUID id;

    @NotBlank
    @Schema(description = "Metric name", example = "faithfulness")
    private String name;

    @NotNull
    @Schema(description = "Type of score value", example = "NUMERIC")
    private Score.DataType dataType;

    @Schema(description = "Numeric score value", example = "0.94")
    private Double numericValue;

    @Schema(description = "String score value", example = "GOOD")
    private String stringValue;

    @Schema(description = "Boolean score value", example = "true")
    private Boolean booleanValue;

    @Schema(description = "Optional evaluator comment", example = "Model stayed grounded in source context")
    private String comment;

    @NotNull
    @Schema(description = "Score source", example = "HUMAN")
    private Score.Source source;

    @Schema(description = "Associated trace id", example = "e3b7f4fd-2930-47f2-808e-7f331f546f8d")
    private UUID traceId;

    @Schema(description = "Creation timestamp", example = "2026-03-27T09:10:00Z")
    private OffsetDateTime createdAt;

    public static ScoreDto fromEntity(Score score) {
        ScoreDto dto = new ScoreDto();
        dto.setId(score.getId());
        dto.setName(score.getName());
        dto.setDataType(score.getDataType());
        dto.setNumericValue(score.getNumericValue());
        dto.setStringValue(score.getStringValue());
        dto.setBooleanValue(score.getBooleanValue());
        dto.setComment(score.getComment());
        dto.setSource(score.getSource());
        dto.setTraceId(score.getTrace().getId());
        dto.setCreatedAt(score.getCreatedAt());
        return dto;
    }
}
