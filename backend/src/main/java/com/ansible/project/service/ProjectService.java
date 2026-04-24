package com.ansible.project.service;

import com.ansible.common.enums.ProjectRole;
import com.ansible.project.dto.CreateProjectRequest;
import com.ansible.project.dto.ProjectResponse;
import com.ansible.project.dto.UpdateProjectRequest;
import com.ansible.project.entity.Project;
import com.ansible.project.entity.ProjectMember;
import com.ansible.project.repository.ProjectMemberRepository;
import com.ansible.project.repository.ProjectRepository;
import com.ansible.security.ProjectAccessChecker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProjectService {

  private final ProjectRepository projectRepository;
  private final ProjectMemberRepository projectMemberRepository;
  private final ProjectAccessChecker accessChecker;
  private final ProjectCleanupService cleanupService;

  @Transactional
  public ProjectResponse createProject(CreateProjectRequest request, Long currentUserId) {
    Project project = new Project();
    project.setName(request.getName());
    project.setDescription(request.getDescription());
    project.setCreatedBy(currentUserId);
    Project saved = projectRepository.save(project);

    ProjectMember member = new ProjectMember();
    member.setProjectId(saved.getId());
    member.setUserId(currentUserId);
    member.setRole(ProjectRole.PROJECT_ADMIN);
    projectMemberRepository.save(member);

    return new ProjectResponse(saved, ProjectRole.PROJECT_ADMIN);
  }

  @Transactional(readOnly = true)
  public List<ProjectResponse> getMyProjects(Long currentUserId) {
    List<Project> projects = projectRepository.findAllByMemberUserId(currentUserId);
    return projects.stream()
        .map(
            p -> {
              ProjectRole role =
                  projectMemberRepository
                      .findByProjectIdAndUserId(p.getId(), currentUserId)
                      .map(ProjectMember::getRole)
                      .orElse(null);
              return new ProjectResponse(p, role);
            })
        .toList();
  }

  @Transactional(readOnly = true)
  public ProjectResponse getProject(Long projectId, Long currentUserId) {
    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));
    ProjectMember member = accessChecker.checkMembership(projectId, currentUserId);
    return new ProjectResponse(project, member.getRole());
  }

  @Transactional
  public ProjectResponse updateProject(
      Long projectId, UpdateProjectRequest request, Long currentUserId) {
    accessChecker.checkAdmin(projectId, currentUserId);
    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));
    if (StringUtils.hasText(request.getName())) {
      project.setName(request.getName());
    }
    if (request.getDescription() != null) {
      project.setDescription(request.getDescription());
    }
    Project saved = projectRepository.save(project);
    ProjectMember member = accessChecker.checkMembership(projectId, currentUserId);
    return new ProjectResponse(saved, member.getRole());
  }

  @Transactional
  public void deleteProject(Long projectId, Long currentUserId) {
    accessChecker.checkAdmin(projectId, currentUserId);
    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found"));
    cleanupService.cleanupProject(projectId);
    projectMemberRepository.deleteByProjectId(projectId);
    projectRepository.delete(project);
  }
}
