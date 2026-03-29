package com.hookwatch.dto;

import com.hookwatch.domain.Annotation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
@Schema(description = "Trace annotation")
public class AnnotationDto {
    UUID id;
    UUID traceId;
    String text;
    String author;
    Instant createdAt;

    public static AnnotationDto fromEntity(Annotation annotation) {
        return AnnotationDto.builder()
                .id(annotation.getId())
                .traceId(annotation.getTrace().getId())
                .text(annotation.getText())
                .author(annotation.getAuthor())
                .createdAt(annotation.getCreatedAt())
                .build();
    }
}
