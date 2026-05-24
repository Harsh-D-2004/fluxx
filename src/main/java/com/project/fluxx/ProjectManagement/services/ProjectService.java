package com.project.fluxx.ProjectManagement.services;

import java.util.HexFormat;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.project.fluxx.ProjectManagement.dao.ProjectRepository;
import com.project.fluxx.ProjectManagement.models.Project;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    @Value("${api.key.secret}")
    private String apiKeySecret;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public String generateAPIkey(String projectId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiKeySecret.getBytes(), "HmacSHA256"));
            byte[] hash = mac.doFinal(projectId.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate API key", e);
        }
    }

    public Project createProject(String name) {
        Project project = new Project();
        project.setName(name);
        Project savedProject = projectRepository.save(project);
        return updateProjectComplete(savedProject);
    }

    public Project getProject(String id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found: " + id));
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public Project updateProject(String id, String name) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found: " + id));
        project.setName(name);
        return projectRepository.save(project);
    }

    private Project updateProjectComplete(Project project) {
        project.setApiKey(generateAPIkey(project.getId()));
        return projectRepository.save(project);
    }

    public void deleteProject(String id) {
        projectRepository.deleteById(id);
    }
}
