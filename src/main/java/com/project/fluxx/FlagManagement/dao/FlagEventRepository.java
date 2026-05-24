package com.project.fluxx.FlagManagement.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.fluxx.FlagManagement.models.FlagEvent;

@Repository
public interface FlagEventRepository extends JpaRepository<FlagEvent, String>  {

    List<FlagEvent> findByFlagId(String flagId);
    
}
