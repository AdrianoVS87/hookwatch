package com.hookwatch.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Maintains active SSE connections per trace and broadcasts span events.
 * Uses CopyOnWriteArrayList so iteration is safe during concurrent send/remove.
 */
@Service
@Slf4j
public class TraceEventPublisher {

    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /** Registers a new SSE client for the given trace. */
    public SseEmitter subscribe(UUID traceId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.computeIfAbsent(traceId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> {
            List<SseEmitter> list = emitters.get(traceId);
            if (list != null) list.remove(emitter);
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        return emitter;
    }

    /** Broadcasts a span event to all clients watching this trace. */
    public void publish(UUID traceId, Object payload) {
        List<SseEmitter> list = emitters.get(traceId);
        if (list == null || list.isEmpty()) return;

        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("span").data(payload));
            } catch (IOException e) {
                log.debug("SSE send failed for trace {}, removing emitter", traceId);
                dead.add(emitter);
            }
        }
        list.removeAll(dead);
    }
}
