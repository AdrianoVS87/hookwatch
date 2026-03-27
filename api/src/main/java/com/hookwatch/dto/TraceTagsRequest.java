package com.hookwatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Trace tags update payload")
public class TraceTagsRequest {

    @NotNull
    @Schema(description = "Tag values to merge into the trace tag set", example = "[\"production\",\"reviewed\"]")
    private List<String> tags;
}
