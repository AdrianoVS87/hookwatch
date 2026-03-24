package com.hookwatch.dto;

import com.hookwatch.domain.Score;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class ScoreDto {
    private UUID id;

    @NotBlank
    private String name;

    @NotNull
    private Score.DataType dataType;

    private Double numericValue;
    private String stringValue;
    private Boolean booleanValue;
    private String comment;

    @NotNull
    private Score.Source source;

    private UUID traceId;
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
