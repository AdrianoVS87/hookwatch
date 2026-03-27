package com.hookwatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Annotation creation payload")
public class AnnotationCreateRequest {

    @NotBlank
    @Size(max = 5000)
    @Schema(description = "Annotation text", example = "Investigate retry policy for this failure.")
    private String text;

    @NotBlank
    @Size(max = 120)
    @Schema(description = "Author identifier", example = "adriano")
    private String author;
}
