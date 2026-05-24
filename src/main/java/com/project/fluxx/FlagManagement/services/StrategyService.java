package com.project.fluxx.FlagManagement.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.fluxx.FlagManagement.dao.FeatureFlagRepository;
import com.project.fluxx.FlagManagement.dao.StrategyRepository;
import com.project.fluxx.FlagManagement.models.FlagEvent;
import com.project.fluxx.FlagManagement.models.Strategy;
import com.project.fluxx.FlagManagement.models.Strategy.StrategyType;
import com.project.fluxx.FlagManagement.models.StrategyParam;

@Service
public class StrategyService {

    private static final Logger log = LoggerFactory.getLogger(StrategyService.class);

    private final StrategyRepository strategyRepository;
    private final FeatureFlagRepository featureFlagRepository;
    private final FlagEventService flagEventService;
    private final ObjectMapper objectMapper;

    public StrategyService(StrategyRepository strategyRepository, FeatureFlagRepository featureFlagRepository,
            FlagEventService flagEventService, ObjectMapper objectMapper) {
        this.strategyRepository = strategyRepository;
        this.featureFlagRepository = featureFlagRepository;
        this.flagEventService = flagEventService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Strategy createStrategy(String flagId, StrategyType strategyType, List<StrategyParam> params) {
        log.info("Creating strategy: flagId='{}', type={}", flagId, strategyType);
        long start = System.currentTimeMillis();

        String projectId = featureFlagRepository.findById(flagId)
                .orElseThrow(() -> new RuntimeException("Flag not found: " + flagId))
                .getProjectId();

        Strategy strategy = new Strategy();
        strategy.setFlagId(flagId);
        strategy.setStrategyType(strategyType);
        if (params != null) strategy.setParameters(params);

        Strategy saved = strategyRepository.save(strategy);
        flagEventService.createFlagEvent(projectId, flagId, FlagEvent.EventType.STRATEGY_ADDED.toString(), toJson(saved));

        log.info("Strategy created: id='{}', flagId='{}', projectId='{}' — event published in {}ms",
                saved.getId(), flagId, projectId, System.currentTimeMillis() - start);
        return saved;
    }

    public Strategy getStrategy(String id) {
        return strategyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Strategy not found: " + id));
    }

    public Strategy getStrategyByFlag(String flagId) {
        return strategyRepository.findByFlagId(flagId)
                .orElseThrow(() -> new RuntimeException("No strategy found for flag: " + flagId));
    }

    public List<Strategy> getAllStrategies() {
        return strategyRepository.findAll();
    }

    public List<Strategy> getStrategiesByType(StrategyType strategyType) {
        return strategyRepository.findByStrategyType(strategyType);
    }

    @Transactional
    public Strategy updateStrategy(String id, StrategyType strategyType, List<StrategyParam> params) {
        log.info("Updating strategy: id='{}', type={}", id, strategyType);
        long start = System.currentTimeMillis();

        Strategy strategy = strategyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Strategy not found: " + id));
        strategy.setStrategyType(strategyType);
        if (params != null) {
            List<StrategyParam> existing = strategy.getParameters();
            if (existing != null) existing.clear();
            else strategy.setParameters(new java.util.ArrayList<>());
            strategy.getParameters().addAll(params);
        }
        Strategy saved = strategyRepository.save(strategy);
        String projectId = featureFlagRepository.findById(saved.getFlagId())
                .orElseThrow(() -> new RuntimeException("Flag not found: " + saved.getFlagId()))
                .getProjectId();
        flagEventService.createFlagEvent(projectId, saved.getFlagId(), FlagEvent.EventType.STRATEGY_UPDATED.toString(), toJson(saved));

        log.info("Strategy updated: id='{}', flagId='{}', projectId='{}' — event published in {}ms",
                id, saved.getFlagId(), projectId, System.currentTimeMillis() - start);
        return saved;
    }

    @Transactional
    public void deleteStrategy(String id) {
        log.info("Deleting strategy: id='{}'", id);
        long start = System.currentTimeMillis();

        Strategy strategy = strategyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Strategy not found: " + id));
        String projectId = featureFlagRepository.findById(strategy.getFlagId())
                .orElseThrow(() -> new RuntimeException("Flag not found: " + strategy.getFlagId()))
                .getProjectId();
        strategyRepository.deleteById(id);
        flagEventService.createFlagEvent(projectId, strategy.getFlagId(), FlagEvent.EventType.STRATEGY_REMOVED.toString(), toJson(strategy));

        log.info("Strategy deleted: id='{}', flagId='{}', projectId='{}' — event published in {}ms",
                id, strategy.getFlagId(), projectId, System.currentTimeMillis() - start);
    }

    private String toJson(Strategy strategy) {
        try {
            return objectMapper.writeValueAsString(strategy);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize strategy to JSON", e);
        }
    }
}
