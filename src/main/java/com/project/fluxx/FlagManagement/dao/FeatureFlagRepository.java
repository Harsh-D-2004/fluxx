package com.project.fluxx.FlagManagement.dao;

import com.project.fluxx.FlagManagement.models.FeatureFlag;
import com.project.fluxx.FlagManagement.models.FeatureFlag.FlagType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, String> {

    Optional<FeatureFlag> findByName(String name);

    List<FeatureFlag> findByFlagType(FlagType flagType);

    List<FeatureFlag> findByProjectId(String projectId);

}
