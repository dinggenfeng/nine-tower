# Template Management Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Template CRUD backend for managing Ansible Role .j2 template files with directory structure support.

**Architecture:** Template entity belongs to Role (via roleId FK), stores template content as TEXT. parentDir string field represents directory paths (e.g. "nginx/conf.d"). Unique constraint on (roleId, parentDir, name). Follows existing RoleVariable pattern: entity → repository → DTOs → service → controller → tests.

**Tech Stack:** Spring Boot 3.x, Spring Data JPA, Jakarta Validation, Mockito + Testcontainers (PostgreSQL 16)

---

### Task 1: Template Entity

**Files:**
- Create: `backend/src/main/java/com/ansible/role/entity/Template.java`

- [ ] **Step 1: Create Template entity**

```java
package com.ansible.role.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "templates",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"role_id", "parent_dir", "name"}))
@Getter
@Setter
@NoArgsConstructor
public class Template extends BaseEntity {

  @Column(name = "role_id", nullable = false)
  private Long roleId;

  @Column(name = "parent_dir", length = 500)
  private String parentDir;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(name = "target_path", length = 500)
  private String targetPath;

  @Column(columnDefinition = "TEXT")
  private String content;
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/ansible/role/entity/Template.java
git commit -m "feat: add Template entity"
```

---

### Task 2: Template Repository

**Files:**
- Create: `backend/src/main/java/com/ansible/role/repository/TemplateRepository.java`

- [ ] **Step 1: Create TemplateRepository**

```java
package com.ansible.role.repository;

import com.ansible.role.entity.Template;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<Template, Long> {

  List<Template> findAllByRoleIdOrderByParentDirAscNameAsc(Long roleId);

  boolean existsByRoleIdAndParentDirAndName(Long roleId, String parentDir, String name);
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/ansible/role/repository/TemplateRepository.java
git commit -m "feat: add TemplateRepository"
```

---

### Task 3: Template DTOs

**Files:**
- Create: `backend/src/main/java/com/ansible/role/dto/CreateTemplateRequest.java`
- Create: `backend/src/main/java/com/ansible/role/dto/UpdateTemplateRequest.java`
- Create: `backend/src/main/java/com/ansible/role/dto/TemplateResponse.java`

- [ ] **Step 1: Create CreateTemplateRequest**

```java
package com.ansible.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTemplateRequest {

  @NotBlank(message = "Template name is required")
  @Size(max = 200, message = "Name must not exceed 200 characters")
  private String name;

  @Size(max = 500, message = "Parent directory must not exceed 500 characters")
  private String parentDir;

  @Size(max = 500, message = "Target path must not exceed 500 characters")
  private String targetPath;

  private String content;
}
```

- [ ] **Step 2: Create UpdateTemplateRequest**

```java
package com.ansible.role.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTemplateRequest {

  @Size(max = 200, message = "Name must not exceed 200 characters")
  private String name;

  @Size(max = 500, message = "Parent directory must not exceed 500 characters")
  private String parentDir;

  @Size(max = 500, message = "Target path must not exceed 500 characters")
  private String targetPath;

  private String content;
}
```

- [ ] **Step 3: Create TemplateResponse**

```java
package com.ansible.role.dto;

import com.ansible.role.entity.Template;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class TemplateResponse {

  private final Long id;
  private final Long roleId;
  private final String parentDir;
  private final String name;
  private final String targetPath;
  private final String content;
  private final Long createdBy;
  private final LocalDateTime createdAt;

  @JsonCreator
  public TemplateResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("roleId") Long roleId,
      @JsonProperty("parentDir") String parentDir,
      @JsonProperty("name") String name,
      @JsonProperty("targetPath") String targetPath,
      @JsonProperty("content") String content,
      @JsonProperty("createdBy") Long createdBy,
      @JsonProperty("createdAt") LocalDateTime createdAt) {
    this.id = id;
    this.roleId = roleId;
    this.parentDir = parentDir;
    this.name = name;
    this.targetPath = targetPath;
    this.content = content;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
  }

  public TemplateResponse(Template template) {
    this.id = template.getId();
    this.roleId = template.getRoleId();
    this.parentDir = template.getParentDir();
    this.name = template.getName();
    this.targetPath = template.getTargetPath();
    this.content = template.getContent();
    this.createdBy = template.getCreatedBy();
    this.createdAt = template.getCreatedAt();
  }
}
```

- [ ] **Step 4: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/role/dto/CreateTemplateRequest.java backend/src/main/java/com/ansible/role/dto/UpdateTemplateRequest.java backend/src/main/java/com/ansible/role/dto/TemplateResponse.java
git commit -m "feat: add Template DTOs"
```

---

### Task 4: TemplateService + Unit Tests (TDD)

**Files:**
- Create: `backend/src/test/java/com/ansible/role/service/TemplateServiceTest.java`
- Create: `backend/src/main/java/com/ansible/role/service/TemplateService.java`

- [ ] **Step 1: Write all unit tests first**

```java
package com.ansible.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.role.dto.CreateTemplateRequest;
import com.ansible.role.dto.TemplateResponse;
import com.ansible.role.dto.UpdateTemplateRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.Template;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.TemplateRepository;
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
class TemplateServiceTest {

  @Mock private TemplateRepository templateRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private ProjectAccessChecker accessChecker;
  @InjectMocks private TemplateService templateService;

  private Role testRole;
  private Template testTemplate;

  @BeforeEach
  void setUp() {
    testRole = new Role();
    ReflectionTestUtils.setField(testRole, "id", 1L);
    testRole.setProjectId(10L);
    testRole.setName("nginx");
    testRole.setCreatedBy(10L);

    testTemplate = new Template();
    ReflectionTestUtils.setField(testTemplate, "id", 1L);
    testTemplate.setRoleId(1L);
    testTemplate.setParentDir(null);
    testTemplate.setName("nginx.conf.j2");
    testTemplate.setTargetPath("/etc/nginx/nginx.conf");
    testTemplate.setContent("server { listen {{ http_port }}; }");
    testTemplate.setCreatedBy(10L);
    ReflectionTestUtils.setField(testTemplate, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(testTemplate, "updatedAt", LocalDateTime.now());
  }

  @Test
  void createTemplate_success() {
    CreateTemplateRequest request = new CreateTemplateRequest();
    request.setName("nginx.conf.j2");
    request.setTargetPath("/etc/nginx/nginx.conf");
    request.setContent("server { listen {{ http_port }}; }");

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(templateRepository.existsByRoleIdAndParentDirAndName(1L, null, "nginx.conf.j2"))
        .thenReturn(false);
    when(templateRepository.save(any(Template.class))).thenReturn(testTemplate);

    TemplateResponse response = templateService.createTemplate(1L, request, 10L);

    assertThat(response.getName()).isEqualTo("nginx.conf.j2");
    assertThat(response.getTargetPath()).isEqualTo("/etc/nginx/nginx.conf");
    assertThat(response.getContent()).isEqualTo("server { listen {{ http_port }}; }");
    verify(templateRepository).save(any(Template.class));
  }

  @Test
  void createTemplate_duplicateName() {
    CreateTemplateRequest request = new CreateTemplateRequest();
    request.setName("nginx.conf.j2");

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(templateRepository.existsByRoleIdAndParentDirAndName(1L, null, "nginx.conf.j2"))
        .thenReturn(true);

    assertThatThrownBy(() -> templateService.createTemplate(1L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Template with this name already exists in this directory");
  }

  @Test
  void createTemplate_roleNotFound() {
    CreateTemplateRequest request = new CreateTemplateRequest();
    request.setName("nginx.conf.j2");

    when(roleRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> templateService.createTemplate(99L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Role not found");
  }

  @Test
  void createTemplate_withParentDir() {
    CreateTemplateRequest request = new CreateTemplateRequest();
    request.setName("vhost.conf.j2");
    request.setParentDir("nginx/conf.d");
    request.setTargetPath("/etc/nginx/conf.d/vhost.conf");
    request.setContent("server { server_name {{ domain }}; }");

    Template savedTemplate = new Template();
    ReflectionTestUtils.setField(savedTemplate, "id", 2L);
    savedTemplate.setRoleId(1L);
    savedTemplate.setParentDir("nginx/conf.d");
    savedTemplate.setName("vhost.conf.j2");
    savedTemplate.setTargetPath("/etc/nginx/conf.d/vhost.conf");
    savedTemplate.setContent("server { server_name {{ domain }}; }");
    savedTemplate.setCreatedBy(10L);
    ReflectionTestUtils.setField(savedTemplate, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(savedTemplate, "updatedAt", LocalDateTime.now());

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(templateRepository.existsByRoleIdAndParentDirAndName(1L, "nginx/conf.d", "vhost.conf.j2"))
        .thenReturn(false);
    when(templateRepository.save(any(Template.class))).thenReturn(savedTemplate);

    TemplateResponse response = templateService.createTemplate(1L, request, 10L);

    assertThat(response.getParentDir()).isEqualTo("nginx/conf.d");
    assertThat(response.getName()).isEqualTo("vhost.conf.j2");
    verify(templateRepository).save(any(Template.class));
  }

  @Test
  void getTemplatesByRole_success() {
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(templateRepository.findAllByRoleIdOrderByParentDirAscNameAsc(1L))
        .thenReturn(List.of(testTemplate));

    List<TemplateResponse> templates = templateService.getTemplatesByRole(1L, 10L);

    assertThat(templates).hasSize(1);
    assertThat(templates.get(0).getName()).isEqualTo("nginx.conf.j2");
  }

  @Test
  void getTemplate_success() {
    when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    TemplateResponse response = templateService.getTemplate(1L, 10L);

    assertThat(response.getName()).isEqualTo("nginx.conf.j2");
    assertThat(response.getContent()).isEqualTo("server { listen {{ http_port }}; }");
  }

  @Test
  void getTemplate_notFound() {
    when(templateRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> templateService.getTemplate(99L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Template not found");
  }

  @Test
  void updateTemplate_success() {
    UpdateTemplateRequest request = new UpdateTemplateRequest();
    request.setContent("server { listen {{ new_port }}; }");

    when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(templateRepository.save(any(Template.class))).thenReturn(testTemplate);

    TemplateResponse response = templateService.updateTemplate(1L, request, 10L);

    assertThat(response).isNotNull();
    verify(templateRepository).save(any(Template.class));
  }

  @Test
  void updateTemplate_notFound() {
    UpdateTemplateRequest request = new UpdateTemplateRequest();
    request.setContent("new content");

    when(templateRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> templateService.updateTemplate(99L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Template not found");
  }

  @Test
  void updateTemplate_duplicateName() {
    UpdateTemplateRequest request = new UpdateTemplateRequest();
    request.setName("other.conf.j2");

    when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(templateRepository.existsByRoleIdAndParentDirAndName(1L, null, "other.conf.j2"))
        .thenReturn(true);

    assertThatThrownBy(() -> templateService.updateTemplate(1L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Template with this name already exists in this directory");
  }

  @Test
  void updateTemplate_nameNotChanged() {
    UpdateTemplateRequest request = new UpdateTemplateRequest();
    request.setName("nginx.conf.j2");

    when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(templateRepository.save(any(Template.class))).thenReturn(testTemplate);

    TemplateResponse response = templateService.updateTemplate(1L, request, 10L);

    assertThat(response).isNotNull();
    verify(templateRepository).save(any(Template.class));
  }

  @Test
  void deleteTemplate_success() {
    when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    templateService.deleteTemplate(1L, 10L);

    verify(templateRepository).delete(testTemplate);
  }

  @Test
  void deleteTemplate_notFound() {
    when(templateRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> templateService.deleteTemplate(99L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Template not found");
  }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend && mvn test -Dtest=TemplateServiceTest -q`
Expected: COMPILATION FAILURE (TemplateService does not exist yet)

- [ ] **Step 3: Implement TemplateService**

```java
package com.ansible.role.service;

import com.ansible.role.dto.CreateTemplateRequest;
import com.ansible.role.dto.TemplateResponse;
import com.ansible.role.dto.UpdateTemplateRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.Template;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.TemplateRepository;
import com.ansible.security.ProjectAccessChecker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TemplateService {

  private final TemplateRepository templateRepository;
  private final RoleRepository roleRepository;
  private final ProjectAccessChecker accessChecker;

  @Transactional
  public TemplateResponse createTemplate(
      Long roleId, CreateTemplateRequest request, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);

    if (templateRepository.existsByRoleIdAndParentDirAndName(
        roleId, request.getParentDir(), request.getName())) {
      throw new IllegalArgumentException(
          "Template with this name already exists in this directory");
    }

    Template template = new Template();
    template.setRoleId(roleId);
    template.setParentDir(request.getParentDir());
    template.setName(request.getName());
    template.setTargetPath(request.getTargetPath());
    template.setContent(request.getContent());
    template.setCreatedBy(currentUserId);
    Template saved = templateRepository.save(template);
    return new TemplateResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<TemplateResponse> getTemplatesByRole(Long roleId, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    return templateRepository.findAllByRoleIdOrderByParentDirAscNameAsc(roleId).stream()
        .map(TemplateResponse::new)
        .toList();
  }

  @Transactional(readOnly = true)
  public TemplateResponse getTemplate(Long templateId, Long currentUserId) {
    Template template =
        templateRepository
            .findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found"));
    Role role =
        roleRepository
            .findById(template.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    return new TemplateResponse(template);
  }

  @Transactional
  public TemplateResponse updateTemplate(
      Long templateId, UpdateTemplateRequest request, Long currentUserId) {
    Template template =
        templateRepository
            .findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found"));
    Role role =
        roleRepository
            .findById(template.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), template.getCreatedBy(), currentUserId);

    String newParentDir = request.getParentDir();
    String newName = request.getName();
    boolean parentDirChanged =
        newParentDir != null && !java.util.Objects.equals(newParentDir, template.getParentDir());
    boolean nameChanged =
        StringUtils.hasText(newName) && !newName.equals(template.getName());

    if (parentDirChanged || nameChanged) {
      String checkDir = parentDirChanged ? newParentDir : template.getParentDir();
      String checkName = nameChanged ? newName : template.getName();
      if (templateRepository.existsByRoleIdAndParentDirAndName(
          template.getRoleId(), checkDir, checkName)) {
        throw new IllegalArgumentException(
            "Template with this name already exists in this directory");
      }
      if (parentDirChanged) {
        template.setParentDir(newParentDir);
      }
      if (nameChanged) {
        template.setName(newName);
      }
    }

    if (request.getTargetPath() != null) {
      template.setTargetPath(request.getTargetPath());
    }
    if (request.getContent() != null) {
      template.setContent(request.getContent());
    }
    Template saved = templateRepository.save(template);
    return new TemplateResponse(saved);
  }

  @Transactional
  public void deleteTemplate(Long templateId, Long currentUserId) {
    Template template =
        templateRepository
            .findById(templateId)
            .orElseThrow(() -> new IllegalArgumentException("Template not found"));
    Role role =
        roleRepository
            .findById(template.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), template.getCreatedBy(), currentUserId);
    templateRepository.delete(template);
  }
}
```

- [ ] **Step 4: Run unit tests**

Run: `cd backend && mvn test -Dtest=TemplateServiceTest -q`
Expected: All 13 tests PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/role/service/TemplateService.java backend/src/test/java/com/ansible/role/service/TemplateServiceTest.java
git commit -m "feat: add TemplateService with unit tests"
```

---

### Task 5: TemplateController + Integration Tests (TDD)

**Files:**
- Create: `backend/src/test/java/com/ansible/role/controller/TemplateControllerTest.java`
- Create: `backend/src/main/java/com/ansible/role/controller/TemplateController.java`

- [ ] **Step 1: Write integration tests first**

```java
package com.ansible.role.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.project.dto.CreateProjectRequest;
import com.ansible.project.dto.ProjectResponse;
import com.ansible.project.repository.ProjectMemberRepository;
import com.ansible.project.repository.ProjectRepository;
import com.ansible.role.dto.CreateRoleRequest;
import com.ansible.role.dto.CreateTemplateRequest;
import com.ansible.role.dto.RoleResponse;
import com.ansible.role.dto.TemplateResponse;
import com.ansible.role.dto.UpdateTemplateRequest;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.TemplateRepository;
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

class TemplateControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private TemplateRepository templateRepository;

  private String token;
  private Long roleId;

  @BeforeEach
  void setUp() {
    RegisterRequest reg = new RegisterRequest();
    reg.setUsername("alice");
    reg.setPassword("password123");
    reg.setEmail("alice@example.com");
    ResponseEntity<Result<TokenResponse>> regResp =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(reg),
            new ParameterizedTypeReference<>() {});
    token = regResp.getBody().getData().getToken();

    CreateProjectRequest projReq = new CreateProjectRequest();
    projReq.setName("Test Project");
    ResponseEntity<Result<ProjectResponse>> projResp =
        restTemplate.exchange(
            "/api/projects",
            HttpMethod.POST,
            new HttpEntity<>(projReq, authHeaders()),
            new ParameterizedTypeReference<>() {});
    Long projectId = projResp.getBody().getData().getId();

    CreateRoleRequest roleReq = new CreateRoleRequest();
    roleReq.setName("nginx");
    ResponseEntity<Result<RoleResponse>> roleResp =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/roles",
            HttpMethod.POST,
            new HttpEntity<>(roleReq, authHeaders()),
            new ParameterizedTypeReference<>() {});
    roleId = roleResp.getBody().getData().getId();
  }

  @AfterEach
  void tearDown() {
    templateRepository.deleteAll();
    roleRepository.deleteAll();
    projectMemberRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private Long createTemplate(String name, String parentDir, String targetPath, String content) {
    CreateTemplateRequest req = new CreateTemplateRequest();
    req.setName(name);
    req.setParentDir(parentDir);
    req.setTargetPath(targetPath);
    req.setContent(content);
    ResponseEntity<Result<TemplateResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/templates",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().getId();
  }

  @Test
  void createTemplate_success() {
    CreateTemplateRequest req = new CreateTemplateRequest();
    req.setName("nginx.conf.j2");
    req.setTargetPath("/etc/nginx/nginx.conf");
    req.setContent("server { listen {{ http_port }}; }");

    ResponseEntity<Result<TemplateResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/templates",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getName()).isEqualTo("nginx.conf.j2");
    assertThat(resp.getBody().getData().getTargetPath()).isEqualTo("/etc/nginx/nginx.conf");
    assertThat(resp.getBody().getData().getContent())
        .isEqualTo("server { listen {{ http_port }}; }");
  }

  @Test
  void createTemplate_withParentDir() {
    CreateTemplateRequest req = new CreateTemplateRequest();
    req.setName("vhost.conf.j2");
    req.setParentDir("nginx/conf.d");
    req.setTargetPath("/etc/nginx/conf.d/vhost.conf");
    req.setContent("server { server_name {{ domain }}; }");

    ResponseEntity<Result<TemplateResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/templates",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getParentDir()).isEqualTo("nginx/conf.d");
    assertThat(resp.getBody().getData().getName()).isEqualTo("vhost.conf.j2");
  }

  @Test
  void createTemplate_duplicateName() {
    createTemplate("nginx.conf.j2", null, "/etc/nginx/nginx.conf", "content");

    CreateTemplateRequest req = new CreateTemplateRequest();
    req.setName("nginx.conf.j2");
    req.setContent("other content");

    ResponseEntity<Result<TemplateResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/templates",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void getTemplates_success() {
    createTemplate("nginx.conf.j2", null, "/etc/nginx/nginx.conf", "content1");
    createTemplate("vhost.conf.j2", "conf.d", "/etc/nginx/conf.d/vhost.conf", "content2");

    ResponseEntity<Result<List<TemplateResponse>>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/templates",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData()).hasSize(2);
  }

  @Test
  void getTemplate_success() {
    Long templateId =
        createTemplate("nginx.conf.j2", null, "/etc/nginx/nginx.conf", "template content");

    ResponseEntity<Result<TemplateResponse>> resp =
        restTemplate.exchange(
            "/api/templates/" + templateId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getName()).isEqualTo("nginx.conf.j2");
    assertThat(resp.getBody().getData().getContent()).isEqualTo("template content");
  }

  @Test
  void updateTemplate_success() {
    Long templateId =
        createTemplate("nginx.conf.j2", null, "/etc/nginx/nginx.conf", "old content");

    UpdateTemplateRequest req = new UpdateTemplateRequest();
    req.setContent("new content");

    ResponseEntity<Result<TemplateResponse>> resp =
        restTemplate.exchange(
            "/api/templates/" + templateId,
            HttpMethod.PUT,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getContent()).isEqualTo("new content");
  }

  @Test
  void deleteTemplate_success() {
    Long templateId =
        createTemplate("nginx.conf.j2", null, "/etc/nginx/nginx.conf", "content");

    ResponseEntity<Result<Void>> resp =
        restTemplate.exchange(
            "/api/templates/" + templateId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Result<List<TemplateResponse>>> listResp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/templates",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});
    assertThat(listResp.getBody().getData()).isEmpty();
  }
}
```

- [ ] **Step 2: Run integration tests to verify they fail**

Run: `cd backend && mvn test -Dtest=TemplateControllerTest -q`
Expected: COMPILATION FAILURE (TemplateController does not exist yet)

- [ ] **Step 3: Implement TemplateController**

```java
package com.ansible.role.controller;

import com.ansible.common.Result;
import com.ansible.role.dto.CreateTemplateRequest;
import com.ansible.role.dto.TemplateResponse;
import com.ansible.role.dto.UpdateTemplateRequest;
import com.ansible.role.service.TemplateService;
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
@RequestMapping("/api")
@RequiredArgsConstructor
public class TemplateController {

  private final TemplateService templateService;

  @PostMapping("/roles/{roleId}/templates")
  public Result<TemplateResponse> createTemplate(
      @PathVariable Long roleId,
      @Valid @RequestBody CreateTemplateRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(templateService.createTemplate(roleId, request, currentUserId));
  }

  @GetMapping("/roles/{roleId}/templates")
  public Result<List<TemplateResponse>> getTemplates(
      @PathVariable Long roleId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(templateService.getTemplatesByRole(roleId, currentUserId));
  }

  @GetMapping("/templates/{id}")
  public Result<TemplateResponse> getTemplate(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(templateService.getTemplate(id, currentUserId));
  }

  @PutMapping("/templates/{id}")
  public Result<TemplateResponse> updateTemplate(
      @PathVariable Long id,
      @Valid @RequestBody UpdateTemplateRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(templateService.updateTemplate(id, request, currentUserId));
  }

  @DeleteMapping("/templates/{id}")
  public Result<Void> deleteTemplate(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    templateService.deleteTemplate(id, currentUserId);
    return Result.success();
  }
}
```

- [ ] **Step 4: Run integration tests**

Run: `cd backend && mvn test -Dtest=TemplateControllerTest -q`
Expected: All 7 tests PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/role/controller/TemplateController.java backend/src/test/java/com/ansible/role/controller/TemplateControllerTest.java
git commit -m "feat: add TemplateController with integration tests"
```

---

### Task 6: Code Quality Checks

- [ ] **Step 1: Run Spotless formatting**

Run: `cd backend && mvn spotless:apply -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run all quality checks**

Run: `cd backend && mvn checkstyle:check pmd:check spotbugs:check -q`
Expected: BUILD SUCCESS for all three

- [ ] **Step 3: Run full test suite**

Run: `cd backend && mvn verify -q`
Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 4: Commit any formatting changes (if any)**

```bash
git add -A backend/src/
git commit -m "style: apply formatting to Template module"
```
