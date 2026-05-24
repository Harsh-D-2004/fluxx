package com.project.fluxx.FlagManagement.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.fluxx.FlagManagement.dao.FlagEventRepository;
import com.project.fluxx.FlagManagement.models.FlagEvent;
import com.project.fluxx.config.RedisConfig;

@Service
public class FlagEventService {

    private final FlagEventRepository flagEventRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public FlagEventService(FlagEventRepository flagEventRepository, RedisTemplate<String, Object> redisTemplate) {
        this.flagEventRepository = flagEventRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public void createFlagEvent(String projectId, String flagId, String eventType, String afterState) {
        FlagEvent saved = flagEventRepository.save(
            new FlagEvent(flagId, FlagEvent.EventType.valueOf(eventType), afterState)
        );

        Map<String, Object> eventData = new LinkedHashMap<>();
        eventData.put("projectId", projectId);
        eventData.put("flagId", saved.getFlagId());
        eventData.put("eventType", saved.getEventType());
        eventData.put("afterState", saved.getAfterState());
        eventData.put("createdAt", saved.getCreatedAt());

        redisTemplate.convertAndSend(RedisConfig.FLAG_EVENTS_CHANNEL, eventData);
    }

    public void deleteFlagEvent(String id) {
        flagEventRepository.deleteById(id);
    }

    public List<FlagEvent> getAllFlagEvents() {
        return flagEventRepository.findAll();
    }

    public List<FlagEvent> getFlagEventsByFlagId(String flagId) {
        return flagEventRepository.findByFlagId(flagId);
    }
}
