package com.hookwatch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WebhookDto {

    @NotBlank
    private String name;

    @NotBlank
    private String targetUrl;
}
