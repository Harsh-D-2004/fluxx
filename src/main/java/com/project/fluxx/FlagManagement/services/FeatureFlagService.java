package com.project.fluxx.FlagManagement.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.fluxx.FlagManagement.dao.FeatureFlagRepository;
import com.project.fluxx.FlagManagement.models.FeatureFlag;
import com.project.fluxx.FlagManagement.models.FeatureFlag.FlagType;
import com.project.fluxx.FlagManagement.models.FlagEvent;

@Service
public class FeatureFlagService {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagService.class);

    private final FeatureFlagRepository featureFlagRepository;
    private final FlagEventService flagEventService;
    private final ObjectMapper objectMapper;

    public FeatureFlagService(FeatureFlagRepository featureFlagRepository, FlagEventService flagEventService, ObjectMapper objectMapper) {
        this.featureFlagRepository = featureFlagRepository;
        this.flagEventService = flagEventService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public FeatureFlag createFlag(String projectId, String name, FlagType flagType, boolean enabled) {
        log.info("Creating flag: projectId='{}', name='{}', type={}, enabled={}", projectId, name, flagType, enabled);
        long start = System.currentTimeMillis();

        FeatureFlag flag = new FeatureFlag();
        flag.setProjectId(projectId);
        flag.setName(name);
        flag.setFlagType(flagType);
        flag.setEnabled(enabled);

        FeatureFlag saved = featureFlagRepository.save(flag);
        flagEventService.createFlagEvent(saved.getProjectId(), saved.getId(), FlagEvent.EventType.FLAG_CREATED.toString(), toJson(saved));

        log.info("Flag created: id='{}', projectId='{}', name='{}' — event published in {}ms",
                saved.getId(), projectId, name, System.currentTimeMillis() - start);
        return saved;
    }

    public List<FeatureFlag> getFlagsByProject(String projectId) {
        return featureFlagRepository.findByProjectId(projectId);
    }

    @Transactional
    public FeatureFlag updateFlag(String id, boolean enabled) {
        log.info("Updating flag: id='{}', enabled={}", id, enabled);
        long start = System.currentTimeMillis();

        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flag not found: " + id));
        flag.setEnabled(enabled);
        FeatureFlag saved = featureFlagRepository.save(flag);
        flagEventService.createFlagEvent(saved.getProjectId(), saved.getId(), FlagEvent.EventType.FLAG_ENABLED.toString(), toJson(saved));

        log.info("Flag updated: id='{}', projectId='{}', enabled={} — event published in {}ms",
                id, saved.getProjectId(), enabled, System.currentTimeMillis() - start);
        return saved;
    }

    @Transactional
    public void deleteFlag(String id) {
        log.info("Deleting flag: id='{}'", id);
        long start = System.currentTimeMillis();

        FeatureFlag flag = featureFlagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flag not found: " + id));
        flagEventService.createFlagEvent(flag.getProjectId(), flag.getId(), FlagEvent.EventType.FLAG_ARCHIVED.toString(), toJson(flag));
        featureFlagRepository.deleteById(id);

        log.info("Flag deleted: id='{}', projectId='{}' — event published in {}ms",
                id, flag.getProjectId(), System.currentTimeMillis() - start);
    }

    public FeatureFlag getFlag(String id) {
        return featureFlagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flag not found: " + id));
    }

    public List<FeatureFlag> getAllFlags() {
        return featureFlagRepository.findAll();
    }

    private String toJson(FeatureFlag flag) {
        try {
            return objectMapper.writeValueAsString(flag);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize flag to JSON", e);
        }
    }
}
