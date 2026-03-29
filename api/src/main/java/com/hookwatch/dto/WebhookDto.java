package com.hookwatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Webhook registration payload")
public class WebhookDto {

    @NotBlank
    @Schema(description = "Webhook display name", example = "slack-alerts")
    private String name;

    @NotBlank
    @Schema(description = "Target callback URL", example = "https://example.com/hookwatch/events")
    private String targetUrl;
}
