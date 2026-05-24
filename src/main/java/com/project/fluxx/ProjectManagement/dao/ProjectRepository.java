package com.project.fluxx.ProjectManagement.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.fluxx.ProjectManagement.models.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {

    Optional<Project> findByName(String name);

    Optional<Project> findByApiKey(String apiKey);
}
