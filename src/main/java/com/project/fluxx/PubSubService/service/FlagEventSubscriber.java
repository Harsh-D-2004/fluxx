package com.project.fluxx.PubSubService.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FlagEventSubscriber {

    private static final Logger log = LoggerFactory.getLogger(FlagEventSubscriber.class);

    private final SseEmitterService sseEmitterService;

    public FlagEventSubscriber(SseEmitterService sseEmitterService) {
        this.sseEmitterService = sseEmitterService;
    }

    public void handleMessage(Map<String, Object> message) {
        String projectId = (String) message.get("projectId");
        String flagId = (String) message.get("flagId");
        String eventType = message.get("eventType") != null ? message.get("eventType").toString() : "UNKNOWN";
        log.info("Flag event received: projectId='{}', flagId='{}', eventType='{}'", projectId, flagId, eventType);

        if (projectId != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("flagId", flagId);
            response.put("eventType", message.get("eventType"));
            response.put("Message", "New update");
            sseEmitterService.broadcast(projectId, response);
        }
    }
}
