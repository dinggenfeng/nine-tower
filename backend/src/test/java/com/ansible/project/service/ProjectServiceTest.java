package com.ansible.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.common.enums.ProjectRole;
import com.ansible.project.dto.CreateProjectRequest;
import com.ansible.project.dto.ProjectResponse;
import com.ansible.project.dto.UpdateProjectRequest;
import com.ansible.project.entity.Project;
import com.ansible.project.entity.ProjectMember;
import com.ansible.project.repository.ProjectMemberRepository;
import com.ansible.project.repository.ProjectRepository;
import com.ansible.security.ProjectAccessChecker;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

  @Mock private ProjectRepository projectRepository;
  @Mock private ProjectMemberRepository projectMemberRepository;
  @Mock private ProjectAccessChecker accessChecker;

  @InjectMocks private ProjectService projectService;

  private Project testProject;
  private ProjectMember adminMember;

  @BeforeEach
  void setUp() {
    testProject = new Project();
    ReflectionTestUtils.setField(testProject, "id", 1L);
    testProject.setName("Test Project");
    testProject.setDescription("A test project");
    testProject.setCreatedBy(10L);
    ReflectionTestUtils.setField(testProject, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(testProject, "updatedAt", LocalDateTime.now());

    adminMember = new ProjectMember();
    adminMember.setId(1L);
    adminMember.setProjectId(1L);
    adminMember.setUserId(10L);
    adminMember.setRole(ProjectRole.PROJECT_ADMIN);
  }

  @Test
  void createProject_success() {
    CreateProjectRequest request = new CreateProjectRequest();
    request.setName("New Project");
    request.setDescription("Desc");

    when(projectRepository.save(any(Project.class))).thenReturn(testProject);

    ProjectResponse response = projectService.createProject(request, 10L);

    assertThat(response.getName()).isEqualTo("Test Project");
    assertThat(response.getMyRole()).isEqualTo(ProjectRole.PROJECT_ADMIN);
    verify(projectMemberRepository).save(any(ProjectMember.class));
  }

  @Test
  void getMyProjects_returns_user_projects() {
    when(projectRepository.findAllByMemberUserId(10L)).thenReturn(List.of(testProject));
    when(projectMemberRepository.findByProjectIdAndUserId(1L, 10L))
        .thenReturn(Optional.of(adminMember));

    List<ProjectResponse> projects = projectService.getMyProjects(10L);

    assertThat(projects).hasSize(1);
    assertThat(projects.get(0).getName()).isEqualTo("Test Project");
    assertThat(projects.get(0).getMyRole()).isEqualTo(ProjectRole.PROJECT_ADMIN);
  }

  @Test
  void getProject_success() {
    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(accessChecker.checkMembership(1L, 10L)).thenReturn(adminMember);

    ProjectResponse response = projectService.getProject(1L, 10L);

    assertThat(response.getName()).isEqualTo("Test Project");
  }

  @Test
  void getProject_notFound_throws() {
    when(projectRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> projectService.getProject(99L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Project not found");
  }

  @Test
  void updateProject_success() {
    UpdateProjectRequest request = new UpdateProjectRequest();
    request.setName("Updated Name");

    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
    when(projectRepository.save(any(Project.class))).thenReturn(testProject);
    when(accessChecker.checkMembership(1L, 10L)).thenReturn(adminMember);

    projectService.updateProject(1L, request, 10L);

    verify(accessChecker).checkAdmin(1L, 10L);
    verify(projectRepository).save(testProject);
  }

  @Test
  void deleteProject_success() {
    when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

    projectService.deleteProject(1L, 10L);

    verify(accessChecker).checkAdmin(1L, 10L);
    verify(projectMemberRepository).deleteByProjectId(1L);
    verify(projectRepository).delete(testProject);
  }
}
