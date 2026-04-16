# Phase 2: Project CRUD + Member Management — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement full Project CRUD (create, list my projects, get, update, delete) and Project Member management (add member, list members, remove member, update role), with `ProjectAccessChecker` enforcing membership and admin-level permissions on all project-scoped endpoints.

**Architecture:** Two new entities (`Project`, `ProjectMember`) in a `project` package following the same entity→repository→service→controller layering as the existing `user` package. `ProjectAccessChecker` is a Spring `@Component` in the `security` package that checks project membership and admin role, called by service methods. Project creator is automatically added as `PROJECT_ADMIN`. All project-scoped APIs require JWT + project membership. Edit/delete operations require `PROJECT_ADMIN`. Unit tests mock repositories with Mockito; integration tests use Testcontainers via `AbstractIntegrationTest`.

**Tech Stack:** Java 21, Spring Boot 3.3.5, Spring Data JPA, PostgreSQL 16, Testcontainers 1.19, JUnit 5, Mockito, Lombok

---

## File Map

### Backend — New Files

| File | Responsibility |
|------|---------------|
| `backend/src/main/java/com/ansible/project/entity/Project.java` | Project JPA entity (extends BaseEntity) |
| `backend/src/main/java/com/ansible/project/entity/ProjectMember.java` | ProjectMember JPA entity (composite: projectId + userId, role enum) |
| `backend/src/main/java/com/ansible/project/repository/ProjectRepository.java` | Spring Data repository for Project |
| `backend/src/main/java/com/ansible/project/repository/ProjectMemberRepository.java` | Spring Data repository for ProjectMember |
| `backend/src/main/java/com/ansible/project/dto/CreateProjectRequest.java` | DTO: name, description |
| `backend/src/main/java/com/ansible/project/dto/UpdateProjectRequest.java` | DTO: name, description |
| `backend/src/main/java/com/ansible/project/dto/ProjectResponse.java` | DTO: project info + role of current user |
| `backend/src/main/java/com/ansible/project/dto/AddMemberRequest.java` | DTO: userId, role |
| `backend/src/main/java/com/ansible/project/dto/UpdateMemberRoleRequest.java` | DTO: role |
| `backend/src/main/java/com/ansible/project/dto/ProjectMemberResponse.java` | DTO: userId, username, email, role |
| `backend/src/main/java/com/ansible/project/service/ProjectService.java` | Project CRUD business logic |
| `backend/src/main/java/com/ansible/project/service/ProjectMemberService.java` | Member management business logic |
| `backend/src/main/java/com/ansible/project/controller/ProjectController.java` | REST endpoints for project CRUD + member management |
| `backend/src/main/java/com/ansible/security/ProjectAccessChecker.java` | Checks project membership + admin role |
| `backend/src/test/java/com/ansible/project/service/ProjectServiceTest.java` | ProjectService unit tests |
| `backend/src/test/java/com/ansible/project/service/ProjectMemberServiceTest.java` | ProjectMemberService unit tests |
| `backend/src/test/java/com/ansible/project/controller/ProjectControllerTest.java` | Project CRUD integration tests |
| `backend/src/test/java/com/ansible/project/controller/ProjectMemberControllerTest.java` | Member management integration tests |

---

## Task 1: Create Project and ProjectMember entities

**Files:**
- Create: `backend/src/main/java/com/ansible/project/entity/Project.java`
- Create: `backend/src/main/java/com/ansible/project/entity/ProjectMember.java`
- Create: `backend/src/main/java/com/ansible/project/repository/ProjectRepository.java`
- Create: `backend/src/main/java/com/ansible/project/repository/ProjectMemberRepository.java`

- [ ] **Step 1: Create directory structure**

```bash
mkdir -p backend/src/main/java/com/ansible/project/entity
mkdir -p backend/src/main/java/com/ansible/project/repository
mkdir -p backend/src/main/java/com/ansible/project/dto
mkdir -p backend/src/main/java/com/ansible/project/service
mkdir -p backend/src/main/java/com/ansible/project/controller
mkdir -p backend/src/test/java/com/ansible/project/service
mkdir -p backend/src/test/java/com/ansible/project/controller
```

- [ ] **Step 2: Create `Project.java`**

```java
package com.ansible.project.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
public class Project extends BaseEntity {

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 500)
  private String description;
}
```

- [ ] **Step 3: Create `ProjectMember.java`**

```java
package com.ansible.project.entity;

import com.ansible.common.enums.ProjectRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "project_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"projectId", "userId"}))
@Getter
@Setter
@NoArgsConstructor
public class ProjectMember {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long projectId;

  @Column(nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ProjectRole role;

  @Column(nullable = false, updatable = false)
  private LocalDateTime joinedAt;

  @PrePersist
  void onCreate() {
    joinedAt = LocalDateTime.now();
  }
}
```

- [ ] **Step 4: Create `ProjectRepository.java`**

```java
package com.ansible.project.repository;

import com.ansible.project.entity.Project;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

  @Query(
      "SELECT p FROM Project p WHERE p.id IN "
          + "(SELECT pm.projectId FROM ProjectMember pm WHERE pm.userId = :userId)")
  List<Project> findAllByMemberUserId(@Param("userId") Long userId);
}
```

- [ ] **Step 5: Create `ProjectMemberRepository.java`**

```java
package com.ansible.project.repository;

import com.ansible.project.entity.ProjectMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

  List<ProjectMember> findAllByProjectId(Long projectId);

  Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

  boolean existsByProjectIdAndUserId(Long projectId, Long userId);

  void deleteByProjectId(Long projectId);
}
```

- [ ] **Step 6: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/ansible/project/
git commit -m "feat: add Project and ProjectMember entities with repositories"
```

---

## Task 2: Create ProjectAccessChecker

**Files:**
- Create: `backend/src/main/java/com/ansible/security/ProjectAccessChecker.java`

- [ ] **Step 1: Create `ProjectAccessChecker.java`**

```java
package com.ansible.security;

import com.ansible.common.enums.ProjectRole;
import com.ansible.project.entity.ProjectMember;
import com.ansible.project.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectAccessChecker {

  private final ProjectMemberRepository projectMemberRepository;

  public ProjectMember checkMembership(Long projectId, Long userId) {
    return projectMemberRepository
        .findByProjectIdAndUserId(projectId, userId)
        .orElseThrow(() -> new SecurityException("Not a member of this project"));
  }

  public void checkAdmin(Long projectId, Long userId) {
    ProjectMember member = checkMembership(projectId, userId);
    if (member.getRole() != ProjectRole.PROJECT_ADMIN) {
      throw new SecurityException("Only project admins can perform this action");
    }
  }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/ansible/security/ProjectAccessChecker.java
git commit -m "feat: add ProjectAccessChecker for membership and admin verification"
```

---

## Task 3: Create Project DTOs

**Files:**
- Create: `backend/src/main/java/com/ansible/project/dto/CreateProjectRequest.java`
- Create: `backend/src/main/java/com/ansible/project/dto/UpdateProjectRequest.java`
- Create: `backend/src/main/java/com/ansible/project/dto/ProjectResponse.java`

- [ ] **Step 1: Create `CreateProjectRequest.java`**

```java
package com.ansible.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProjectRequest {

  @NotBlank(message = "Project name is required")
  @Size(max = 100, message = "Project name must not exceed 100 characters")
  private String name;

  @Size(max = 500, message = "Description must not exceed 500 characters")
  private String description;
}
```

- [ ] **Step 2: Create `UpdateProjectRequest.java`**

```java
package com.ansible.project.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProjectRequest {

  @Size(max = 100, message = "Project name must not exceed 100 characters")
  private String name;

  @Size(max = 500, message = "Description must not exceed 500 characters")
  private String description;
}
```

- [ ] **Step 3: Create `ProjectResponse.java`**

```java
package com.ansible.project.dto;

import com.ansible.common.enums.ProjectRole;
import com.ansible.project.entity.Project;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class ProjectResponse {

  private final Long id;
  private final String name;
  private final String description;
  private final Long createdBy;
  private final LocalDateTime createdAt;
  private final ProjectRole myRole;

  @JsonCreator
  public ProjectResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("name") String name,
      @JsonProperty("description") String description,
      @JsonProperty("createdBy") Long createdBy,
      @JsonProperty("createdAt") LocalDateTime createdAt,
      @JsonProperty("myRole") ProjectRole myRole) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
    this.myRole = myRole;
  }

  public ProjectResponse(Project project, ProjectRole myRole) {
    this.id = project.getId();
    this.name = project.getName();
    this.description = project.getDescription();
    this.createdBy = project.getCreatedBy();
    this.createdAt = project.getCreatedAt();
    this.myRole = myRole;
  }
}
```

- [ ] **Step 4: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/project/dto/
git commit -m "feat: add Project DTOs (CreateProjectRequest, UpdateProjectRequest, ProjectResponse)"
```

---

## Task 4: Implement ProjectService with unit tests

**Files:**
- Create: `backend/src/main/java/com/ansible/project/service/ProjectService.java`
- Create: `backend/src/test/java/com/ansible/project/service/ProjectServiceTest.java`

- [ ] **Step 1: Write `ProjectServiceTest.java`**

```java
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
    testProject.setId(1L);
    testProject.setName("Test Project");
    testProject.setDescription("A test project");
    testProject.setCreatedBy(10L);
    testProject.setCreatedAt(LocalDateTime.now());
    testProject.setUpdatedAt(LocalDateTime.now());

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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend && mvn test -pl . -Dtest="com.ansible.project.service.ProjectServiceTest" -q`
Expected: COMPILATION FAILURE (ProjectService does not exist yet)

- [ ] **Step 3: Create `ProjectService.java`**

```java
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
    projectMemberRepository.deleteByProjectId(projectId);
    projectRepository.delete(project);
  }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend && mvn test -pl . -Dtest="com.ansible.project.service.ProjectServiceTest" -q`
Expected: Tests run: 6, Failures: 0, Errors: 0

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/project/service/ProjectService.java
git add backend/src/test/java/com/ansible/project/service/ProjectServiceTest.java
git commit -m "feat: add ProjectService with unit tests — create/list/get/update/delete"
```

---

## Task 5: Create Member DTOs and implement ProjectMemberService with unit tests

**Files:**
- Create: `backend/src/main/java/com/ansible/project/dto/AddMemberRequest.java`
- Create: `backend/src/main/java/com/ansible/project/dto/UpdateMemberRoleRequest.java`
- Create: `backend/src/main/java/com/ansible/project/dto/ProjectMemberResponse.java`
- Create: `backend/src/main/java/com/ansible/project/service/ProjectMemberService.java`
- Create: `backend/src/test/java/com/ansible/project/service/ProjectMemberServiceTest.java`

- [ ] **Step 1: Create `AddMemberRequest.java`**

```java
package com.ansible.project.dto;

import com.ansible.common.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddMemberRequest {

  @NotNull(message = "User ID is required")
  private Long userId;

  @NotNull(message = "Role is required")
  private ProjectRole role;
}
```

- [ ] **Step 2: Create `UpdateMemberRoleRequest.java`**

```java
package com.ansible.project.dto;

import com.ansible.common.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMemberRoleRequest {

  @NotNull(message = "Role is required")
  private ProjectRole role;
}
```

- [ ] **Step 3: Create `ProjectMemberResponse.java`**

```java
package com.ansible.project.dto;

import com.ansible.common.enums.ProjectRole;
import com.ansible.project.entity.ProjectMember;
import com.ansible.user.entity.User;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class ProjectMemberResponse {

  private final Long userId;
  private final String username;
  private final String email;
  private final ProjectRole role;
  private final LocalDateTime joinedAt;

  @JsonCreator
  public ProjectMemberResponse(
      @JsonProperty("userId") Long userId,
      @JsonProperty("username") String username,
      @JsonProperty("email") String email,
      @JsonProperty("role") ProjectRole role,
      @JsonProperty("joinedAt") LocalDateTime joinedAt) {
    this.userId = userId;
    this.username = username;
    this.email = email;
    this.role = role;
    this.joinedAt = joinedAt;
  }

  public ProjectMemberResponse(ProjectMember member, User user) {
    this.userId = user.getId();
    this.username = user.getUsername();
    this.email = user.getEmail();
    this.role = member.getRole();
    this.joinedAt = member.getJoinedAt();
  }
}
```

- [ ] **Step 4: Write `ProjectMemberServiceTest.java`**

```java
package com.ansible.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.common.enums.ProjectRole;
import com.ansible.project.dto.AddMemberRequest;
import com.ansible.project.dto.ProjectMemberResponse;
import com.ansible.project.dto.UpdateMemberRoleRequest;
import com.ansible.project.entity.ProjectMember;
import com.ansible.project.repository.ProjectMemberRepository;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.user.entity.User;
import com.ansible.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

  @Mock private ProjectMemberRepository projectMemberRepository;
  @Mock private UserRepository userRepository;
  @Mock private ProjectAccessChecker accessChecker;

  @InjectMocks private ProjectMemberService projectMemberService;

  private User testUser;
  private ProjectMember testMember;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(20L);
    testUser.setUsername("bob");
    testUser.setEmail("bob@example.com");
    testUser.setPassword("encoded");
    testUser.setCreatedAt(LocalDateTime.now());
    testUser.setUpdatedAt(LocalDateTime.now());

    testMember = new ProjectMember();
    testMember.setId(1L);
    testMember.setProjectId(1L);
    testMember.setUserId(20L);
    testMember.setRole(ProjectRole.PROJECT_MEMBER);
    testMember.setJoinedAt(LocalDateTime.now());
  }

  @Test
  void listMembers_success() {
    when(projectMemberRepository.findAllByProjectId(1L)).thenReturn(List.of(testMember));
    when(userRepository.findById(20L)).thenReturn(Optional.of(testUser));

    List<ProjectMemberResponse> members = projectMemberService.listMembers(1L, 10L);

    assertThat(members).hasSize(1);
    assertThat(members.get(0).getUsername()).isEqualTo("bob");
    assertThat(members.get(0).getRole()).isEqualTo(ProjectRole.PROJECT_MEMBER);
    verify(accessChecker).checkMembership(1L, 10L);
  }

  @Test
  void addMember_success() {
    AddMemberRequest request = new AddMemberRequest();
    request.setUserId(20L);
    request.setRole(ProjectRole.PROJECT_MEMBER);

    when(userRepository.findById(20L)).thenReturn(Optional.of(testUser));
    when(projectMemberRepository.existsByProjectIdAndUserId(1L, 20L)).thenReturn(false);
    when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(testMember);

    ProjectMemberResponse response = projectMemberService.addMember(1L, request, 10L);

    assertThat(response.getUsername()).isEqualTo("bob");
    verify(accessChecker).checkAdmin(1L, 10L);
  }

  @Test
  void addMember_fails_when_already_member() {
    AddMemberRequest request = new AddMemberRequest();
    request.setUserId(20L);
    request.setRole(ProjectRole.PROJECT_MEMBER);

    when(userRepository.findById(20L)).thenReturn(Optional.of(testUser));
    when(projectMemberRepository.existsByProjectIdAndUserId(1L, 20L)).thenReturn(true);

    assertThatThrownBy(() -> projectMemberService.addMember(1L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("User is already a member");
  }

  @Test
  void addMember_fails_when_user_not_found() {
    AddMemberRequest request = new AddMemberRequest();
    request.setUserId(99L);
    request.setRole(ProjectRole.PROJECT_MEMBER);

    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> projectMemberService.addMember(1L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("User not found");
  }

  @Test
  void removeMember_success() {
    when(projectMemberRepository.findByProjectIdAndUserId(1L, 20L))
        .thenReturn(Optional.of(testMember));

    projectMemberService.removeMember(1L, 20L, 10L);

    verify(accessChecker).checkAdmin(1L, 10L);
    verify(projectMemberRepository).delete(testMember);
  }

  @Test
  void removeMember_fails_when_not_member() {
    when(projectMemberRepository.findByProjectIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> projectMemberService.removeMember(1L, 99L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Member not found");
  }

  @Test
  void updateMemberRole_success() {
    UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
    request.setRole(ProjectRole.PROJECT_ADMIN);

    when(projectMemberRepository.findByProjectIdAndUserId(1L, 20L))
        .thenReturn(Optional.of(testMember));
    when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(testMember);
    when(userRepository.findById(20L)).thenReturn(Optional.of(testUser));

    ProjectMemberResponse response =
        projectMemberService.updateMemberRole(1L, 20L, request, 10L);

    verify(accessChecker).checkAdmin(1L, 10L);
    assertThat(testMember.getRole()).isEqualTo(ProjectRole.PROJECT_ADMIN);
  }
}
```

- [ ] **Step 5: Run tests to verify they fail**

Run: `cd backend && mvn test -pl . -Dtest="com.ansible.project.service.ProjectMemberServiceTest" -q`
Expected: COMPILATION FAILURE (ProjectMemberService does not exist yet)

- [ ] **Step 6: Create `ProjectMemberService.java`**

```java
package com.ansible.project.service;

import com.ansible.project.dto.AddMemberRequest;
import com.ansible.project.dto.ProjectMemberResponse;
import com.ansible.project.dto.UpdateMemberRoleRequest;
import com.ansible.project.entity.ProjectMember;
import com.ansible.project.repository.ProjectMemberRepository;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.user.entity.User;
import com.ansible.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

  private final ProjectMemberRepository projectMemberRepository;
  private final UserRepository userRepository;
  private final ProjectAccessChecker accessChecker;

  @Transactional(readOnly = true)
  public List<ProjectMemberResponse> listMembers(Long projectId, Long currentUserId) {
    accessChecker.checkMembership(projectId, currentUserId);
    List<ProjectMember> members = projectMemberRepository.findAllByProjectId(projectId);
    return members.stream()
        .map(
            member -> {
              User user =
                  userRepository
                      .findById(member.getUserId())
                      .orElseThrow(() -> new IllegalArgumentException("User not found"));
              return new ProjectMemberResponse(member, user);
            })
        .toList();
  }

  @Transactional
  public ProjectMemberResponse addMember(
      Long projectId, AddMemberRequest request, Long currentUserId) {
    accessChecker.checkAdmin(projectId, currentUserId);
    User user =
        userRepository
            .findById(request.getUserId())
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    if (projectMemberRepository.existsByProjectIdAndUserId(projectId, request.getUserId())) {
      throw new IllegalArgumentException("User is already a member of this project");
    }
    ProjectMember member = new ProjectMember();
    member.setProjectId(projectId);
    member.setUserId(request.getUserId());
    member.setRole(request.getRole());
    ProjectMember saved = projectMemberRepository.save(member);
    return new ProjectMemberResponse(saved, user);
  }

  @Transactional
  public void removeMember(Long projectId, Long userId, Long currentUserId) {
    accessChecker.checkAdmin(projectId, currentUserId);
    ProjectMember member =
        projectMemberRepository
            .findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found in this project"));
    projectMemberRepository.delete(member);
  }

  @Transactional
  public ProjectMemberResponse updateMemberRole(
      Long projectId, Long userId, UpdateMemberRoleRequest request, Long currentUserId) {
    accessChecker.checkAdmin(projectId, currentUserId);
    ProjectMember member =
        projectMemberRepository
            .findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found in this project"));
    member.setRole(request.getRole());
    ProjectMember saved = projectMemberRepository.save(member);
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    return new ProjectMemberResponse(saved, user);
  }
}
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `cd backend && mvn test -pl . -Dtest="com.ansible.project.service.ProjectMemberServiceTest" -q`
Expected: Tests run: 7, Failures: 0, Errors: 0

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/ansible/project/dto/AddMemberRequest.java
git add backend/src/main/java/com/ansible/project/dto/UpdateMemberRoleRequest.java
git add backend/src/main/java/com/ansible/project/dto/ProjectMemberResponse.java
git add backend/src/main/java/com/ansible/project/service/ProjectMemberService.java
git add backend/src/test/java/com/ansible/project/service/ProjectMemberServiceTest.java
git commit -m "feat: add ProjectMemberService with unit tests — add/list/remove/update-role"
```

---

## Task 6: Implement ProjectController with integration tests

**Files:**
- Create: `backend/src/main/java/com/ansible/project/controller/ProjectController.java`
- Create: `backend/src/test/java/com/ansible/project/controller/ProjectControllerTest.java`

- [ ] **Step 1: Write `ProjectControllerTest.java`**

```java
package com.ansible.project.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.project.dto.CreateProjectRequest;
import com.ansible.project.dto.ProjectResponse;
import com.ansible.project.dto.UpdateProjectRequest;
import com.ansible.project.repository.ProjectMemberRepository;
import com.ansible.project.repository.ProjectRepository;
import com.ansible.user.dto.RegisterRequest;
import com.ansible.user.dto.TokenResponse;
import com.ansible.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ProjectControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;

  private String token;

  @BeforeEach
  void setUp() {
    RegisterRequest reg = new RegisterRequest();
    reg.setUsername("alice");
    reg.setPassword("password123");
    reg.setEmail("alice@example.com");
    ResponseEntity<Result<TokenResponse>> response =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(reg),
            new ParameterizedTypeReference<>() {});
    token = response.getBody().getData().getToken();
  }

  @AfterEach
  void tearDown() {
    projectMemberRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private Long createProject(String name) {
    CreateProjectRequest req = new CreateProjectRequest();
    req.setName(name);
    req.setDescription("desc");
    ResponseEntity<Result<ProjectResponse>> resp =
        restTemplate.exchange(
            "/api/projects",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().getId();
  }

  @Test
  void createProject_success() {
    CreateProjectRequest req = new CreateProjectRequest();
    req.setName("My Project");
    req.setDescription("A project");

    ResponseEntity<Result<ProjectResponse>> response =
        restTemplate.exchange(
            "/api/projects",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("My Project");
    assertThat(response.getBody().getData().getMyRole().name()).isEqualTo("PROJECT_ADMIN");
  }

  @Test
  void getMyProjects_returns_list() {
    createProject("Project A");
    createProject("Project B");

    ResponseEntity<Result<List<ProjectResponse>>> response =
        restTemplate.exchange(
            "/api/projects",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData()).hasSize(2);
  }

  @Test
  void getProject_success() {
    Long projectId = createProject("Project X");

    ResponseEntity<Result<ProjectResponse>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("Project X");
  }

  @Test
  void updateProject_success() {
    Long projectId = createProject("Old Name");

    UpdateProjectRequest req = new UpdateProjectRequest();
    req.setName("New Name");

    ResponseEntity<Result<ProjectResponse>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId,
            HttpMethod.PUT,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("New Name");
  }

  @Test
  void deleteProject_success() {
    Long projectId = createProject("To Delete");

    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(projectRepository.findById(projectId)).isEmpty();
  }

  @Test
  void getProject_forbidden_for_non_member() {
    Long projectId = createProject("Private Project");

    // Register second user
    RegisterRequest reg2 = new RegisterRequest();
    reg2.setUsername("bob");
    reg2.setPassword("password123");
    reg2.setEmail("bob@example.com");
    ResponseEntity<Result<TokenResponse>> reg2Resp =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(reg2),
            new ParameterizedTypeReference<>() {});
    String bobToken = reg2Resp.getBody().getData().getToken();

    HttpHeaders bobHeaders = new HttpHeaders();
    bobHeaders.setBearerAuth(bobToken);

    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId,
            HttpMethod.GET,
            new HttpEntity<>(bobHeaders),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend && mvn test -pl . -Dtest="com.ansible.project.controller.ProjectControllerTest" -q`
Expected: COMPILATION FAILURE (ProjectController does not exist yet)

- [ ] **Step 3: Create `ProjectController.java`**

```java
package com.ansible.project.controller;

import com.ansible.common.Result;
import com.ansible.project.dto.AddMemberRequest;
import com.ansible.project.dto.CreateProjectRequest;
import com.ansible.project.dto.ProjectMemberResponse;
import com.ansible.project.dto.ProjectResponse;
import com.ansible.project.dto.UpdateMemberRoleRequest;
import com.ansible.project.dto.UpdateProjectRequest;
import com.ansible.project.service.ProjectMemberService;
import com.ansible.project.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

  private final ProjectService projectService;
  private final ProjectMemberService projectMemberService;

  @PostMapping
  public Result<ProjectResponse> createProject(
      @Valid @RequestBody CreateProjectRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(projectService.createProject(request, currentUserId));
  }

  @GetMapping
  public Result<List<ProjectResponse>> getMyProjects(
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(projectService.getMyProjects(currentUserId));
  }

  @GetMapping("/{id}")
  public Result<ProjectResponse> getProject(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(projectService.getProject(id, currentUserId));
  }

  @PutMapping("/{id}")
  public Result<ProjectResponse> updateProject(
      @PathVariable Long id,
      @Valid @RequestBody UpdateProjectRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(projectService.updateProject(id, request, currentUserId));
  }

  @DeleteMapping("/{id}")
  public Result<Void> deleteProject(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    projectService.deleteProject(id, currentUserId);
    return Result.success();
  }

  @GetMapping("/{id}/members")
  public Result<List<ProjectMemberResponse>> listMembers(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(projectMemberService.listMembers(id, currentUserId));
  }

  @PostMapping("/{id}/members")
  public Result<ProjectMemberResponse> addMember(
      @PathVariable Long id,
      @Valid @RequestBody AddMemberRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(projectMemberService.addMember(id, request, currentUserId));
  }

  @DeleteMapping("/{id}/members/{userId}")
  public Result<Void> removeMember(
      @PathVariable Long id,
      @PathVariable Long userId,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    projectMemberService.removeMember(id, userId, currentUserId);
    return Result.success();
  }

  @PutMapping("/{id}/members/{userId}")
  public Result<ProjectMemberResponse> updateMemberRole(
      @PathVariable Long id,
      @PathVariable Long userId,
      @Valid @RequestBody UpdateMemberRoleRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(
        projectMemberService.updateMemberRole(id, userId, request, currentUserId));
  }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend && mvn test -pl . -Dtest="com.ansible.project.controller.ProjectControllerTest" -q`
Expected: Tests run: 6, Failures: 0, Errors: 0

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/project/controller/ProjectController.java
git add backend/src/test/java/com/ansible/project/controller/ProjectControllerTest.java
git commit -m "feat: add ProjectController with integration tests — project CRUD endpoints"
```

---

## Task 7: Add ProjectMember integration tests

**Files:**
- Create: `backend/src/test/java/com/ansible/project/controller/ProjectMemberControllerTest.java`

- [ ] **Step 1: Write `ProjectMemberControllerTest.java`**

```java
package com.ansible.project.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.common.enums.ProjectRole;
import com.ansible.project.dto.AddMemberRequest;
import com.ansible.project.dto.CreateProjectRequest;
import com.ansible.project.dto.ProjectMemberResponse;
import com.ansible.project.dto.ProjectResponse;
import com.ansible.project.dto.UpdateMemberRoleRequest;
import com.ansible.project.repository.ProjectMemberRepository;
import com.ansible.project.repository.ProjectRepository;
import com.ansible.user.dto.RegisterRequest;
import com.ansible.user.dto.TokenResponse;
import com.ansible.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ProjectMemberControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;

  private String aliceToken;
  private String bobToken;
  private Long aliceId;
  private Long bobId;
  private Long projectId;

  @BeforeEach
  void setUp() {
    // Register alice (project admin)
    RegisterRequest aliceReg = new RegisterRequest();
    aliceReg.setUsername("alice");
    aliceReg.setPassword("password123");
    aliceReg.setEmail("alice@example.com");
    ResponseEntity<Result<TokenResponse>> aliceResp =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(aliceReg),
            new ParameterizedTypeReference<>() {});
    aliceToken = aliceResp.getBody().getData().getToken();
    aliceId = aliceResp.getBody().getData().getUser().getId();

    // Register bob
    RegisterRequest bobReg = new RegisterRequest();
    bobReg.setUsername("bob");
    bobReg.setPassword("password123");
    bobReg.setEmail("bob@example.com");
    ResponseEntity<Result<TokenResponse>> bobResp =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(bobReg),
            new ParameterizedTypeReference<>() {});
    bobToken = bobResp.getBody().getData().getToken();
    bobId = bobResp.getBody().getData().getUser().getId();

    // Alice creates a project
    CreateProjectRequest projReq = new CreateProjectRequest();
    projReq.setName("Team Project");
    projReq.setDescription("desc");
    HttpHeaders aliceHeaders = new HttpHeaders();
    aliceHeaders.setBearerAuth(aliceToken);
    ResponseEntity<Result<ProjectResponse>> projResp =
        restTemplate.exchange(
            "/api/projects",
            HttpMethod.POST,
            new HttpEntity<>(projReq, aliceHeaders),
            new ParameterizedTypeReference<>() {});
    projectId = projResp.getBody().getData().getId();
  }

  @AfterEach
  void tearDown() {
    projectMemberRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  private HttpHeaders aliceHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(aliceToken);
    return headers;
  }

  private HttpHeaders bobHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(bobToken);
    return headers;
  }

  @Test
  void addMember_success() {
    AddMemberRequest req = new AddMemberRequest();
    req.setUserId(bobId);
    req.setRole(ProjectRole.PROJECT_MEMBER);

    ResponseEntity<Result<ProjectMemberResponse>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members",
            HttpMethod.POST,
            new HttpEntity<>(req, aliceHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getUsername()).isEqualTo("bob");
    assertThat(response.getBody().getData().getRole()).isEqualTo(ProjectRole.PROJECT_MEMBER);
  }

  @Test
  void addMember_forbidden_for_non_admin() {
    // First add bob as member
    AddMemberRequest addBob = new AddMemberRequest();
    addBob.setUserId(bobId);
    addBob.setRole(ProjectRole.PROJECT_MEMBER);
    restTemplate.exchange(
        "/api/projects/" + projectId + "/members",
        HttpMethod.POST,
        new HttpEntity<>(addBob, aliceHeaders()),
        new ParameterizedTypeReference<Result<ProjectMemberResponse>>() {});

    // Register charlie
    RegisterRequest charlieReg = new RegisterRequest();
    charlieReg.setUsername("charlie");
    charlieReg.setPassword("password123");
    charlieReg.setEmail("charlie@example.com");
    ResponseEntity<Result<TokenResponse>> charlieResp =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(charlieReg),
            new ParameterizedTypeReference<>() {});
    Long charlieId = charlieResp.getBody().getData().getUser().getId();

    // Bob (non-admin) tries to add charlie
    AddMemberRequest addCharlie = new AddMemberRequest();
    addCharlie.setUserId(charlieId);
    addCharlie.setRole(ProjectRole.PROJECT_MEMBER);

    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members",
            HttpMethod.POST,
            new HttpEntity<>(addCharlie, bobHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void listMembers_success() {
    ResponseEntity<Result<List<ProjectMemberResponse>>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members",
            HttpMethod.GET,
            new HttpEntity<>(aliceHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData()).hasSize(1);
    assertThat(response.getBody().getData().get(0).getUsername()).isEqualTo("alice");
  }

  @Test
  void removeMember_success() {
    // Add bob first
    AddMemberRequest addBob = new AddMemberRequest();
    addBob.setUserId(bobId);
    addBob.setRole(ProjectRole.PROJECT_MEMBER);
    restTemplate.exchange(
        "/api/projects/" + projectId + "/members",
        HttpMethod.POST,
        new HttpEntity<>(addBob, aliceHeaders()),
        new ParameterizedTypeReference<Result<ProjectMemberResponse>>() {});

    // Remove bob
    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members/" + bobId,
            HttpMethod.DELETE,
            new HttpEntity<>(aliceHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void updateMemberRole_success() {
    // Add bob first
    AddMemberRequest addBob = new AddMemberRequest();
    addBob.setUserId(bobId);
    addBob.setRole(ProjectRole.PROJECT_MEMBER);
    restTemplate.exchange(
        "/api/projects/" + projectId + "/members",
        HttpMethod.POST,
        new HttpEntity<>(addBob, aliceHeaders()),
        new ParameterizedTypeReference<Result<ProjectMemberResponse>>() {});

    // Update role
    UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
    req.setRole(ProjectRole.PROJECT_ADMIN);

    ResponseEntity<Result<ProjectMemberResponse>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members/" + bobId,
            HttpMethod.PUT,
            new HttpEntity<>(req, aliceHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getRole()).isEqualTo(ProjectRole.PROJECT_ADMIN);
  }
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `cd backend && mvn test -pl . -Dtest="com.ansible.project.controller.ProjectMemberControllerTest" -q`
Expected: Tests run: 5, Failures: 0, Errors: 0

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/ansible/project/controller/ProjectMemberControllerTest.java
git commit -m "feat: add ProjectMember integration tests — add/list/remove/update-role"
```

---

## Task 8: Run code quality scans and fix violations

**Files:**
- Modify: any files that have violations

- [ ] **Step 1: Run Spotless to auto-format**

Run: `cd backend && mvn spotless:apply -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run Checkstyle**

Run: `cd backend && mvn checkstyle:check -q`
Expected: BUILD SUCCESS (fix any violations if not)

- [ ] **Step 3: Run PMD**

Run: `cd backend && mvn pmd:check -q`
Expected: BUILD SUCCESS (fix any violations if not)

- [ ] **Step 4: Run SpotBugs**

Run: `cd backend && mvn compile spotbugs:check -q`
Expected: BUILD SUCCESS (fix any violations if not)

- [ ] **Step 5: Run all tests**

Run: `cd backend && mvn test -q`
Expected: All tests pass (auth + project tests)

- [ ] **Step 6: Commit if any fixes were needed**

```bash
git add -A
git commit -m "fix: resolve code quality scan violations for project module"
```

---

## Task 9: Add frontend API layer and types for Project

**Files:**
- Create: `frontend/src/types/entity/Project.ts`
- Create: `frontend/src/api/project.ts`
- Create: `frontend/src/stores/projectStore.ts`

- [ ] **Step 1: Create `frontend/src/types/entity/Project.ts`**

```typescript
export interface Project {
  id: number;
  name: string;
  description: string;
  createdBy: number;
  createdAt: string;
  myRole: 'PROJECT_ADMIN' | 'PROJECT_MEMBER';
}

export interface ProjectMember {
  userId: number;
  username: string;
  email: string;
  role: 'PROJECT_ADMIN' | 'PROJECT_MEMBER';
  joinedAt: string;
}

export interface CreateProjectRequest {
  name: string;
  description?: string;
}

export interface UpdateProjectRequest {
  name?: string;
  description?: string;
}

export interface AddMemberRequest {
  userId: number;
  role: 'PROJECT_ADMIN' | 'PROJECT_MEMBER';
}

export interface UpdateMemberRoleRequest {
  role: 'PROJECT_ADMIN' | 'PROJECT_MEMBER';
}
```

- [ ] **Step 2: Create `frontend/src/api/project.ts`**

```typescript
import request from './request';
import type {
  Project,
  ProjectMember,
  CreateProjectRequest,
  UpdateProjectRequest,
  AddMemberRequest,
  UpdateMemberRoleRequest,
} from '../types/entity/Project';

export function createProject(data: CreateProjectRequest) {
  return request.post<Project>('/api/projects', data);
}

export function getMyProjects() {
  return request.get<Project[]>('/api/projects');
}

export function getProject(id: number) {
  return request.get<Project>(`/api/projects/${id}`);
}

export function updateProject(id: number, data: UpdateProjectRequest) {
  return request.put<Project>(`/api/projects/${id}`, data);
}

export function deleteProject(id: number) {
  return request.delete<void>(`/api/projects/${id}`);
}

export function getMembers(projectId: number) {
  return request.get<ProjectMember[]>(`/api/projects/${projectId}/members`);
}

export function addMember(projectId: number, data: AddMemberRequest) {
  return request.post<ProjectMember>(`/api/projects/${projectId}/members`, data);
}

export function removeMember(projectId: number, userId: number) {
  return request.delete<void>(`/api/projects/${projectId}/members/${userId}`);
}

export function updateMemberRole(
  projectId: number,
  userId: number,
  data: UpdateMemberRoleRequest
) {
  return request.put<ProjectMember>(
    `/api/projects/${projectId}/members/${userId}`,
    data
  );
}
```

- [ ] **Step 3: Create `frontend/src/stores/projectStore.ts`**

```typescript
import { create } from 'zustand';
import type { Project } from '../types/entity/Project';

interface ProjectState {
  currentProject: Project | null;
  setCurrentProject: (project: Project | null) => void;
}

export const useProjectStore = create<ProjectState>((set) => ({
  currentProject: null,
  setCurrentProject: (project) => set({ currentProject: project }),
}));
```

- [ ] **Step 4: Verify TypeScript compilation**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 5: Commit**

```bash
git add frontend/src/types/entity/Project.ts
git add frontend/src/api/project.ts
git add frontend/src/stores/projectStore.ts
git commit -m "feat: add frontend Project types, API layer, and project store"
```

---

## Task 10: Add ProjectList page

**Files:**
- Create: `frontend/src/pages/project/ProjectList.tsx`
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: Create `frontend/src/pages/project/ProjectList.tsx`**

```tsx
import { useEffect, useState } from 'react';
import { Button, Card, Empty, List, Modal, Form, Input, message, Tag } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SettingOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import type { Project, CreateProjectRequest } from '../../types/entity/Project';
import { getMyProjects, createProject, deleteProject } from '../../api/project';

const { TextArea } = Input;

export default function ProjectList() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(false);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [form] = Form.useForm<CreateProjectRequest>();
  const navigate = useNavigate();

  const fetchProjects = async () => {
    setLoading(true);
    try {
      const res = await getMyProjects();
      setProjects(res);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProjects();
  }, []);

  const handleCreate = async () => {
    const values = await form.validateFields();
    await createProject(values);
    message.success('Project created');
    setCreateModalOpen(false);
    form.resetFields();
    fetchProjects();
  };

  const handleDelete = (id: number) => {
    Modal.confirm({
      title: 'Delete project?',
      content: 'This action cannot be undone.',
      onOk: async () => {
        await deleteProject(id);
        message.success('Project deleted');
        fetchProjects();
      },
    });
  };

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>My Projects</h2>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => setCreateModalOpen(true)}
        >
          New Project
        </Button>
      </div>

      <List
        grid={{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 3, xl: 4 }}
        loading={loading}
        dataSource={projects}
        locale={{ emptyText: <Empty description="No projects yet" /> }}
        renderItem={(project) => (
          <List.Item>
            <Card
              hoverable
              onClick={() => navigate(`/projects/${project.id}/roles`)}
              actions={[
                project.myRole === 'PROJECT_ADMIN' && (
                  <SettingOutlined
                    key="settings"
                    onClick={(e) => {
                      e.stopPropagation();
                      navigate(`/projects/${project.id}/settings`);
                    }}
                  />
                ),
                project.myRole === 'PROJECT_ADMIN' && (
                  <DeleteOutlined
                    key="delete"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDelete(project.id);
                    }}
                  />
                ),
              ].filter(Boolean)}
            >
              <Card.Meta
                title={
                  <span>
                    {project.name}{' '}
                    <Tag color={project.myRole === 'PROJECT_ADMIN' ? 'blue' : 'default'}>
                      {project.myRole === 'PROJECT_ADMIN' ? 'Admin' : 'Member'}
                    </Tag>
                  </span>
                }
                description={project.description || 'No description'}
              />
            </Card>
          </List.Item>
        )}
      />

      <Modal
        title="Create Project"
        open={createModalOpen}
        onOk={handleCreate}
        onCancel={() => {
          setCreateModalOpen(false);
          form.resetFields();
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="Project Name"
            rules={[{ required: true, message: 'Please enter a project name' }]}
          >
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
```

- [ ] **Step 2: Update `App.tsx` to add `/projects` route**

Add the import and route for ProjectList. The `/projects` route should be the default authenticated landing page. The route structure should be:

```tsx
// Add import:
import ProjectList from './pages/project/ProjectList';

// In routes, add under MainLayout:
<Route index element={<Navigate to="/projects" replace />} />
<Route path="projects" element={<ProjectList />} />
```

Read current `App.tsx` before modifying to ensure exact match.

- [ ] **Step 3: Verify TypeScript compilation**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/project/ProjectList.tsx
git add frontend/src/App.tsx
git commit -m "feat: add ProjectList page with create/delete functionality"
```

---

## Task 11: Add ProjectSettings and MemberManagement pages

**Files:**
- Create: `frontend/src/pages/project/ProjectSettings.tsx`
- Create: `frontend/src/pages/project/MemberManagement.tsx`
- Create: `frontend/src/components/Layout/ProjectLayout.tsx`
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: Create `frontend/src/components/Layout/ProjectLayout.tsx`**

```tsx
import { useEffect } from 'react';
import { Layout, Menu } from 'antd';
import {
  TeamOutlined,
  AppstoreOutlined,
  SettingOutlined,
  DatabaseOutlined,
  TagsOutlined,
  CloudOutlined,
  CodeOutlined,
  PlayCircleOutlined,
} from '@ant-design/icons';
import { Outlet, useNavigate, useParams, useLocation } from 'react-router-dom';
import { useProjectStore } from '../../stores/projectStore';
import { getProject } from '../../api/project';

const { Sider, Content } = Layout;

export default function ProjectLayout() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const { currentProject, setCurrentProject } = useProjectStore();

  useEffect(() => {
    if (id) {
      getProject(Number(id)).then(setCurrentProject);
    }
    return () => setCurrentProject(null);
  }, [id, setCurrentProject]);

  const menuItems = [
    { key: 'roles', icon: <CodeOutlined />, label: 'Roles' },
    { key: 'host-groups', icon: <DatabaseOutlined />, label: 'Host Groups' },
    { key: 'variables', icon: <AppstoreOutlined />, label: 'Variables' },
    { key: 'environments', icon: <CloudOutlined />, label: 'Environments' },
    { key: 'tags', icon: <TagsOutlined />, label: 'Tags' },
    { key: 'playbooks', icon: <PlayCircleOutlined />, label: 'Playbooks' },
    { key: 'members', icon: <TeamOutlined />, label: 'Members' },
    { key: 'settings', icon: <SettingOutlined />, label: 'Settings' },
  ];

  const currentKey = location.pathname.split('/').pop() || 'roles';

  return (
    <Layout style={{ minHeight: '100%' }}>
      <Sider width={200} style={{ background: '#fff' }}>
        <div style={{ padding: '16px', fontWeight: 'bold', fontSize: 16 }}>
          {currentProject?.name || 'Loading...'}
        </div>
        <Menu
          mode="inline"
          selectedKeys={[currentKey]}
          items={menuItems}
          onClick={({ key }) => navigate(`/projects/${id}/${key}`)}
        />
      </Sider>
      <Content style={{ padding: 24, background: '#fff', margin: 0 }}>
        <Outlet />
      </Content>
    </Layout>
  );
}
```

- [ ] **Step 2: Create `frontend/src/pages/project/ProjectSettings.tsx`**

```tsx
import { useEffect, useState } from 'react';
import { Form, Input, Button, message, Card } from 'antd';
import { useParams, useNavigate } from 'react-router-dom';
import type { UpdateProjectRequest } from '../../types/entity/Project';
import { getProject, updateProject, deleteProject } from '../../api/project';
import { useProjectStore } from '../../stores/projectStore';

const { TextArea } = Input;

export default function ProjectSettings() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [form] = Form.useForm<UpdateProjectRequest>();
  const [loading, setLoading] = useState(false);
  const { currentProject, setCurrentProject } = useProjectStore();

  useEffect(() => {
    if (currentProject) {
      form.setFieldsValue({
        name: currentProject.name,
        description: currentProject.description,
      });
    }
  }, [currentProject, form]);

  const handleUpdate = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      const updated = await updateProject(Number(id), values);
      setCurrentProject(updated);
      message.success('Project updated');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = () => {
    deleteProject(Number(id)).then(() => {
      message.success('Project deleted');
      navigate('/projects');
    });
  };

  if (currentProject?.myRole !== 'PROJECT_ADMIN') {
    return <div>Only project admins can access settings.</div>;
  }

  return (
    <div>
      <h2>Project Settings</h2>
      <Card style={{ maxWidth: 600 }}>
        <Form form={form} layout="vertical" onFinish={handleUpdate}>
          <Form.Item
            name="name"
            label="Project Name"
            rules={[{ required: true, message: 'Project name is required' }]}
          >
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <TextArea rows={3} maxLength={500} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading}>
              Save
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Card style={{ maxWidth: 600, marginTop: 24 }} title="Danger Zone">
        <Button danger onClick={handleDelete}>
          Delete Project
        </Button>
      </Card>
    </div>
  );
}
```

- [ ] **Step 3: Create `frontend/src/pages/project/MemberManagement.tsx`**

```tsx
import { useEffect, useState } from 'react';
import {
  Table,
  Button,
  Modal,
  Form,
  InputNumber,
  Select,
  message,
  Tag,
  Popconfirm,
} from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useParams } from 'react-router-dom';
import type {
  ProjectMember,
  AddMemberRequest,
} from '../../types/entity/Project';
import {
  getMembers,
  addMember,
  removeMember,
  updateMemberRole,
} from '../../api/project';
import { useProjectStore } from '../../stores/projectStore';

export default function MemberManagement() {
  const { id } = useParams<{ id: string }>();
  const projectId = Number(id);
  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [loading, setLoading] = useState(false);
  const [addModalOpen, setAddModalOpen] = useState(false);
  const [form] = Form.useForm<AddMemberRequest>();
  const { currentProject } = useProjectStore();
  const isAdmin = currentProject?.myRole === 'PROJECT_ADMIN';

  const fetchMembers = async () => {
    setLoading(true);
    try {
      setMembers(await getMembers(projectId));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMembers();
  }, [projectId]);

  const handleAdd = async () => {
    const values = await form.validateFields();
    await addMember(projectId, values);
    message.success('Member added');
    setAddModalOpen(false);
    form.resetFields();
    fetchMembers();
  };

  const handleRemove = async (userId: number) => {
    await removeMember(projectId, userId);
    message.success('Member removed');
    fetchMembers();
  };

  const handleRoleChange = async (
    userId: number,
    role: 'PROJECT_ADMIN' | 'PROJECT_MEMBER'
  ) => {
    await updateMemberRole(projectId, userId, { role });
    message.success('Role updated');
    fetchMembers();
  };

  const columns = [
    { title: 'Username', dataIndex: 'username', key: 'username' },
    { title: 'Email', dataIndex: 'email', key: 'email' },
    {
      title: 'Role',
      dataIndex: 'role',
      key: 'role',
      render: (role: string, record: ProjectMember) =>
        isAdmin ? (
          <Select
            value={role}
            onChange={(value) => handleRoleChange(record.userId, value)}
            options={[
              { value: 'PROJECT_ADMIN', label: 'Admin' },
              { value: 'PROJECT_MEMBER', label: 'Member' },
            ]}
            style={{ width: 120 }}
          />
        ) : (
          <Tag color={role === 'PROJECT_ADMIN' ? 'blue' : 'default'}>
            {role === 'PROJECT_ADMIN' ? 'Admin' : 'Member'}
          </Tag>
        ),
    },
    {
      title: 'Joined',
      dataIndex: 'joinedAt',
      key: 'joinedAt',
      render: (date: string) => new Date(date).toLocaleDateString(),
    },
    ...(isAdmin
      ? [
          {
            title: 'Action',
            key: 'action',
            render: (_: unknown, record: ProjectMember) => (
              <Popconfirm
                title="Remove this member?"
                onConfirm={() => handleRemove(record.userId)}
              >
                <Button type="link" danger>
                  Remove
                </Button>
              </Popconfirm>
            ),
          },
        ]
      : []),
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>Members</h2>
        {isAdmin && (
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setAddModalOpen(true)}
          >
            Add Member
          </Button>
        )}
      </div>

      <Table
        columns={columns}
        dataSource={members}
        rowKey="userId"
        loading={loading}
        pagination={false}
      />

      <Modal
        title="Add Member"
        open={addModalOpen}
        onOk={handleAdd}
        onCancel={() => {
          setAddModalOpen(false);
          form.resetFields();
        }}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="userId"
            label="User ID"
            rules={[{ required: true, message: 'Please enter the user ID' }]}
          >
            <InputNumber style={{ width: '100%' }} min={1} />
          </Form.Item>
          <Form.Item
            name="role"
            label="Role"
            rules={[{ required: true, message: 'Please select a role' }]}
          >
            <Select
              options={[
                { value: 'PROJECT_ADMIN', label: 'Admin' },
                { value: 'PROJECT_MEMBER', label: 'Member' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
```

- [ ] **Step 4: Update `App.tsx` to add project sub-routes**

Read current `App.tsx`, then add imports and routes:

```tsx
// Add imports:
import ProjectLayout from './components/Layout/ProjectLayout';
import ProjectSettings from './pages/project/ProjectSettings';
import MemberManagement from './pages/project/MemberManagement';

// Add nested routes under MainLayout:
<Route path="projects/:id" element={<ProjectLayout />}>
  <Route path="settings" element={<ProjectSettings />} />
  <Route path="members" element={<MemberManagement />} />
</Route>
```

- [ ] **Step 5: Verify TypeScript compilation**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 6: Run ESLint**

Run: `cd frontend && npx eslint src/ --ext .ts,.tsx`
Expected: No errors (fix any if found)

- [ ] **Step 7: Commit**

```bash
git add frontend/src/components/Layout/ProjectLayout.tsx
git add frontend/src/pages/project/ProjectSettings.tsx
git add frontend/src/pages/project/MemberManagement.tsx
git add frontend/src/App.tsx
git commit -m "feat: add ProjectSettings, MemberManagement pages and ProjectLayout"
```
