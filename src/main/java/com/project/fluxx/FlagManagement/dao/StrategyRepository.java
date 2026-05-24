package com.project.fluxx.FlagManagement.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.fluxx.FlagManagement.models.Strategy;

@Repository
public interface StrategyRepository extends JpaRepository<Strategy, String> {

    List<Strategy> findByStrategyType(Strategy.StrategyType strategyType);

    Optional<Strategy> findByFlagId(String flagId);
}
