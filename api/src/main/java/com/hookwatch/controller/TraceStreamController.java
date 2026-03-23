package com.hookwatch.controller;

import com.hookwatch.service.TraceEventPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * Provides a Server-Sent Events stream for live span updates on a given trace.
 * Clients connect once and receive events as new spans are added.
 */
@RestController
@RequestMapping("/api/v1/traces")
@RequiredArgsConstructor
@Tag(name = "Trace Stream")
public class TraceStreamController {

    private final TraceEventPublisher publisher;

    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to live span events for a trace")
    public SseEmitter stream(@PathVariable UUID id) {
        return publisher.subscribe(id);
    }
}
