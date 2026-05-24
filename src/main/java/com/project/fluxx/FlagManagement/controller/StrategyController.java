package com.project.fluxx.FlagManagement.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.fluxx.FlagManagement.models.Strategy;
import com.project.fluxx.FlagManagement.models.Strategy.StrategyType;
import com.project.fluxx.FlagManagement.models.StrategyParam;
import com.project.fluxx.FlagManagement.services.StrategyService;

@RestController
@RequestMapping("/api/strategies")
public class StrategyController {

    private final StrategyService strategyService;

    public StrategyController(StrategyService strategyService) {
        this.strategyService = strategyService;
    }

    record StrategyRequest(String flagId, StrategyType strategyType, List<StrategyParam> params) {}

    @PostMapping
    public ResponseEntity<Strategy> createStrategy(@RequestBody StrategyRequest request) {
        Strategy created = strategyService.createStrategy(request.flagId(), request.strategyType(), request.params());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<Strategy>> getAllStrategies() {
        List<Strategy> strategies = strategyService.getAllStrategies();
        return ResponseEntity.ok(strategies);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Strategy> getStrategy(@PathVariable String id) {
        return ResponseEntity.ok(strategyService.getStrategy(id));
    }

    @GetMapping("/flag/{flagId}")
    public ResponseEntity<Strategy> getStrategyByFlag(@PathVariable String flagId) {
        return ResponseEntity.ok(strategyService.getStrategyByFlag(flagId));
    }

    @GetMapping("/type/{strategyType}")
    public ResponseEntity<List<Strategy>> getStrategiesByType(@PathVariable StrategyType strategyType) {
        List<Strategy> strategies = strategyService.getStrategiesByType(strategyType);
        return ResponseEntity.ok(strategies);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Strategy> updateStrategy(
            @PathVariable String id,
            @RequestBody StrategyRequest request) {
        return ResponseEntity.ok(strategyService.updateStrategy(id, request.strategyType(), request.params()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStrategy(@PathVariable String id) {
        strategyService.deleteStrategy(id);
        return ResponseEntity.noContent().build();
    }
}
