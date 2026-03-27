package com.hookwatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Payload used to register an agent")
public class AgentDto {
    @NotNull
    @Schema(description = "Owning tenant identifier", example = "8bb6a6f8-e5af-4f8e-a8ba-45f4029659c8")
    private UUID tenantId;

    @NotBlank
    @Schema(description = "Human-friendly agent name", example = "OpenClaw Assistant")
    private String name;

    @Schema(description = "Optional agent description", example = "Production support bot")
    private String description;
}
