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

import com.project.fluxx.FlagManagement.models.FeatureFlag;
import com.project.fluxx.FlagManagement.services.FeatureFlagService;

@RestController
@RequestMapping("/api/feature-flags")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    public FeatureFlagController(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @PostMapping
    public ResponseEntity<FeatureFlag> createFeatureFlag(@RequestBody FeatureFlag featureFlag) {
        FeatureFlag created = featureFlagService.createFlag(
                featureFlag.getProjectId(), featureFlag.getName(), featureFlag.getFlagType(), featureFlag.isEnabled());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<FeatureFlag>> getAllFeatureFlags() {
        List<FeatureFlag> flags = featureFlagService.getAllFlags();
        return ResponseEntity.ok(flags);
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<FeatureFlag>> getFlagsByProject(@PathVariable String projectId) {
        List<FeatureFlag> flags = featureFlagService.getFlagsByProject(projectId);
        return ResponseEntity.ok(flags);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeatureFlag> getFeatureFlag(@PathVariable String id) {
        return ResponseEntity.ok(featureFlagService.getFlag(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FeatureFlag> updateFeatureFlag(
            @PathVariable String id,
            @RequestBody FeatureFlag featureFlag) {
        return ResponseEntity.ok(featureFlagService.updateFlag(id, featureFlag.isEnabled()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeatureFlag(@PathVariable String id) {
        featureFlagService.deleteFlag(id);
        return ResponseEntity.noContent().build();
    }
}