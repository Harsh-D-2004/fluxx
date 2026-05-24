package com.project.fluxx.PubSubService.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseEmitterService {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterService.class);

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter addEmitter(String projectId) {
        SseEmitter emitter = new SseEmitter(0L);

        emitter.onCompletion(() -> removeEmitter(projectId, emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            removeEmitter(projectId, emitter);
        });
        emitter.onError(e -> {
            emitter.completeWithError(e);
            removeEmitter(projectId, emitter);
        });

        emitters.computeIfAbsent(projectId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("SSE client connected for project '{}' — active connections: {}", projectId,
                emitters.get(projectId).size());
        return emitter;
    }

    private void removeEmitter(String projectId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(projectId);
        if (list != null) {
            list.remove(emitter);
            log.info("SSE client disconnected for project '{}' — remaining connections: {}", projectId, list.size());
        }
    }

    public void broadcast(String projectId, Map<String, Object> event) {
        List<SseEmitter> list = emitters.get(projectId);
        if (list == null || list.isEmpty()) {
            log.debug("No active SSE connections for project '{}'", projectId);
            return;
        }

        String eventType = event.get("eventType") != null ? event.get("eventType").toString() : "UNKNOWN";
        int total = list.size();
        long start = System.currentTimeMillis();

        List<SseEmitter> dead = new ArrayList<>();
        int successCount = 0;
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("flag-event")
                        .data(event));
                successCount++;
            } catch (IOException e) {
                dead.add(emitter);
            }
        }

        list.removeAll(dead);
        long elapsed = System.currentTimeMillis() - start;
        log.info("SSE broadcast complete: projectId='{}', eventType='{}', sent={}/{} clients, elapsed={}ms",
                projectId, eventType, successCount, total, elapsed);

        if (!dead.isEmpty()) {
            log.debug("Removed {} disconnected SSE emitter(s) for project '{}'", dead.size(), projectId);
        }
    }
}
