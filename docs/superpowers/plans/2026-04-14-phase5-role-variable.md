# Phase 5: RoleVariable & RoleDefaultVariable Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement CRUD for RoleVariable (Role vars/) and RoleDefaultVariable (Role defaults/), including backend entity/service/controller with tests, frontend types/API/components, and integration into the existing RoleDetail tabs.

**Architecture:** Both entities are simple key-value pairs scoped to a Role. They share the same structure (roleId, key, value, createdBy) and follow the established Task/Handler CRUD pattern. RoleVariable represents `vars/main.yml` and RoleDefaultVariable represents `defaults/main.yml` in Ansible Role structure.

**Tech Stack:** Spring Boot 3.x, Spring Data JPA, PostgreSQL, JUnit 5 + Mockito + Testcontainers, React 18 + TypeScript + Ant Design 5.x

---

### Task 1: RoleVariable Entity & Repository

**Files:**
- Create: `backend/src/main/java/com/ansible/role/entity/RoleVariable.java`
- Create: `backend/src/main/java/com/ansible/role/repository/RoleVariableRepository.java`

- [ ] **Step 1: Create RoleVariable entity**

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
    name = "role_variables",
    uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "variable_key"}))
@Getter
@Setter
@NoArgsConstructor
public class RoleVariable extends BaseEntity {

  @Column(name = "role_id", nullable = false)
  private Long roleId;

  @Column(name = "variable_key", nullable = false, length = 200)
  private String key;

  @Column(columnDefinition = "TEXT")
  private String value;
}
```

- [ ] **Step 2: Create RoleVariableRepository**

```java
package com.ansible.role.repository;

import com.ansible.role.entity.RoleVariable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleVariableRepository extends JpaRepository<RoleVariable, Long> {

  List<RoleVariable> findAllByRoleIdOrderByKeyAsc(Long roleId);

  boolean existsByRoleIdAndKey(Long roleId, String key);
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/ansible/role/entity/RoleVariable.java backend/src/main/java/com/ansible/role/repository/RoleVariableRepository.java
git commit -m "feat: add RoleVariable entity and repository"
```

---

### Task 2: RoleVariable DTOs

**Files:**
- Create: `backend/src/main/java/com/ansible/role/dto/CreateRoleVariableRequest.java`
- Create: `backend/src/main/java/com/ansible/role/dto/UpdateRoleVariableRequest.java`
- Create: `backend/src/main/java/com/ansible/role/dto/RoleVariableResponse.java`

- [ ] **Step 1: Create CreateRoleVariableRequest**

```java
package com.ansible.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoleVariableRequest {

  @NotBlank(message = "Variable key is required")
  @Size(max = 200, message = "Key must not exceed 200 characters")
  private String key;

  private String value;
}
```

- [ ] **Step 2: Create UpdateRoleVariableRequest**

```java
package com.ansible.role.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoleVariableRequest {

  @Size(max = 200, message = "Key must not exceed 200 characters")
  private String key;

  private String value;
}
```

- [ ] **Step 3: Create RoleVariableResponse**

```java
package com.ansible.role.dto;

import com.ansible.role.entity.RoleVariable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class RoleVariableResponse {

  private final Long id;
  private final Long roleId;
  private final String key;
  private final String value;
  private final Long createdBy;
  private final LocalDateTime createdAt;

  @JsonCreator
  public RoleVariableResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("roleId") Long roleId,
      @JsonProperty("key") String key,
      @JsonProperty("value") String value,
      @JsonProperty("createdBy") Long createdBy,
      @JsonProperty("createdAt") LocalDateTime createdAt) {
    this.id = id;
    this.roleId = roleId;
    this.key = key;
    this.value = value;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
  }

  public RoleVariableResponse(RoleVariable variable) {
    this.id = variable.getId();
    this.roleId = variable.getRoleId();
    this.key = variable.getKey();
    this.value = variable.getValue();
    this.createdBy = variable.getCreatedBy();
    this.createdAt = variable.getCreatedAt();
  }
}
```

- [ ] **Step 4: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/role/dto/CreateRoleVariableRequest.java backend/src/main/java/com/ansible/role/dto/UpdateRoleVariableRequest.java backend/src/main/java/com/ansible/role/dto/RoleVariableResponse.java
git commit -m "feat: add RoleVariable DTOs"
```

---

### Task 3: RoleVariableService with Unit Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/role/service/RoleVariableService.java`
- Create: `backend/src/test/java/com/ansible/role/service/RoleVariableServiceTest.java`

- [ ] **Step 1: Write the unit test**

```java
package com.ansible.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.role.dto.CreateRoleVariableRequest;
import com.ansible.role.dto.RoleVariableResponse;
import com.ansible.role.dto.UpdateRoleVariableRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.RoleVariable;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.RoleVariableRepository;
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
class RoleVariableServiceTest {

  @Mock private RoleVariableRepository roleVariableRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private ProjectAccessChecker accessChecker;
  @InjectMocks private RoleVariableService roleVariableService;

  private Role testRole;
  private RoleVariable testVariable;

  @BeforeEach
  void setUp() {
    testRole = new Role();
    ReflectionTestUtils.setField(testRole, "id", 1L);
    testRole.setProjectId(10L);
    testRole.setName("nginx");
    testRole.setCreatedBy(10L);

    testVariable = new RoleVariable();
    ReflectionTestUtils.setField(testVariable, "id", 1L);
    testVariable.setRoleId(1L);
    testVariable.setKey("http_port");
    testVariable.setValue("8080");
    testVariable.setCreatedBy(10L);
    ReflectionTestUtils.setField(testVariable, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(testVariable, "updatedAt", LocalDateTime.now());
  }

  @Test
  void createVariable_success() {
    CreateRoleVariableRequest request = new CreateRoleVariableRequest();
    request.setKey("http_port");
    request.setValue("8080");

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleVariableRepository.existsByRoleIdAndKey(1L, "http_port")).thenReturn(false);
    when(roleVariableRepository.save(any(RoleVariable.class))).thenReturn(testVariable);

    RoleVariableResponse response = roleVariableService.createVariable(1L, request, 10L);

    assertThat(response.getKey()).isEqualTo("http_port");
    assertThat(response.getValue()).isEqualTo("8080");
    verify(roleVariableRepository).save(any(RoleVariable.class));
  }

  @Test
  void createVariable_duplicateKey() {
    CreateRoleVariableRequest request = new CreateRoleVariableRequest();
    request.setKey("http_port");
    request.setValue("8080");

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleVariableRepository.existsByRoleIdAndKey(1L, "http_port")).thenReturn(true);

    assertThatThrownBy(() -> roleVariableService.createVariable(1L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Variable key already exists in this role");
  }

  @Test
  void createVariable_roleNotFound() {
    CreateRoleVariableRequest request = new CreateRoleVariableRequest();
    request.setKey("http_port");

    when(roleRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleVariableService.createVariable(99L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Role not found");
  }

  @Test
  void getVariablesByRole_success() {
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleVariableRepository.findAllByRoleIdOrderByKeyAsc(1L))
        .thenReturn(List.of(testVariable));

    List<RoleVariableResponse> variables = roleVariableService.getVariablesByRole(1L, 10L);

    assertThat(variables).hasSize(1);
    assertThat(variables.get(0).getKey()).isEqualTo("http_port");
  }

  @Test
  void updateVariable_success() {
    UpdateRoleVariableRequest request = new UpdateRoleVariableRequest();
    request.setValue("9090");

    when(roleVariableRepository.findById(1L)).thenReturn(Optional.of(testVariable));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleVariableRepository.save(any(RoleVariable.class))).thenReturn(testVariable);

    RoleVariableResponse response = roleVariableService.updateVariable(1L, request, 10L);

    assertThat(response).isNotNull();
    verify(roleVariableRepository).save(any(RoleVariable.class));
  }

  @Test
  void updateVariable_duplicateKey() {
    UpdateRoleVariableRequest request = new UpdateRoleVariableRequest();
    request.setKey("server_name");

    when(roleVariableRepository.findById(1L)).thenReturn(Optional.of(testVariable));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleVariableRepository.existsByRoleIdAndKey(1L, "server_name")).thenReturn(true);

    assertThatThrownBy(() -> roleVariableService.updateVariable(1L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Variable key already exists in this role");
  }

  @Test
  void updateVariable_keyNotChanged() {
    UpdateRoleVariableRequest request = new UpdateRoleVariableRequest();
    request.setKey("http_port");
    request.setValue("9090");

    when(roleVariableRepository.findById(1L)).thenReturn(Optional.of(testVariable));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleVariableRepository.save(any(RoleVariable.class))).thenReturn(testVariable);

    RoleVariableResponse response = roleVariableService.updateVariable(1L, request, 10L);

    assertThat(response).isNotNull();
    verify(roleVariableRepository).save(any(RoleVariable.class));
  }

  @Test
  void updateVariable_notFound() {
    UpdateRoleVariableRequest request = new UpdateRoleVariableRequest();
    request.setValue("9090");

    when(roleVariableRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleVariableService.updateVariable(99L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Role variable not found");
  }

  @Test
  void deleteVariable_success() {
    when(roleVariableRepository.findById(1L)).thenReturn(Optional.of(testVariable));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    roleVariableService.deleteVariable(1L, 10L);

    verify(roleVariableRepository).delete(testVariable);
  }

  @Test
  void deleteVariable_notFound() {
    when(roleVariableRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleVariableService.deleteVariable(99L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Role variable not found");
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -Dtest=RoleVariableServiceTest -q`
Expected: FAIL — `RoleVariableService` not found

- [ ] **Step 3: Implement RoleVariableService**

```java
package com.ansible.role.service;

import com.ansible.role.dto.CreateRoleVariableRequest;
import com.ansible.role.dto.RoleVariableResponse;
import com.ansible.role.dto.UpdateRoleVariableRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.RoleVariable;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.RoleVariableRepository;
import com.ansible.security.ProjectAccessChecker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RoleVariableService {

  private final RoleVariableRepository roleVariableRepository;
  private final RoleRepository roleRepository;
  private final ProjectAccessChecker accessChecker;

  @Transactional
  public RoleVariableResponse createVariable(
      Long roleId, CreateRoleVariableRequest request, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);

    if (roleVariableRepository.existsByRoleIdAndKey(roleId, request.getKey())) {
      throw new IllegalArgumentException("Variable key already exists in this role");
    }

    RoleVariable variable = new RoleVariable();
    variable.setRoleId(roleId);
    variable.setKey(request.getKey());
    variable.setValue(request.getValue());
    variable.setCreatedBy(currentUserId);
    RoleVariable saved = roleVariableRepository.save(variable);
    return new RoleVariableResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<RoleVariableResponse> getVariablesByRole(Long roleId, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    return roleVariableRepository.findAllByRoleIdOrderByKeyAsc(roleId).stream()
        .map(RoleVariableResponse::new)
        .toList();
  }

  @Transactional
  public RoleVariableResponse updateVariable(
      Long variableId, UpdateRoleVariableRequest request, Long currentUserId) {
    RoleVariable variable =
        roleVariableRepository
            .findById(variableId)
            .orElseThrow(() -> new IllegalArgumentException("Role variable not found"));
    Role role =
        roleRepository
            .findById(variable.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), variable.getCreatedBy(), currentUserId);

    if (StringUtils.hasText(request.getKey())
        && !request.getKey().equals(variable.getKey())) {
      if (roleVariableRepository.existsByRoleIdAndKey(variable.getRoleId(), request.getKey())) {
        throw new IllegalArgumentException("Variable key already exists in this role");
      }
      variable.setKey(request.getKey());
    }
    if (request.getValue() != null) {
      variable.setValue(request.getValue());
    }
    RoleVariable saved = roleVariableRepository.save(variable);
    return new RoleVariableResponse(saved);
  }

  @Transactional
  public void deleteVariable(Long variableId, Long currentUserId) {
    RoleVariable variable =
        roleVariableRepository
            .findById(variableId)
            .orElseThrow(() -> new IllegalArgumentException("Role variable not found"));
    Role role =
        roleRepository
            .findById(variable.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), variable.getCreatedBy(), currentUserId);
    roleVariableRepository.delete(variable);
  }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend && mvn test -Dtest=RoleVariableServiceTest -q`
Expected: ALL PASS

- [ ] **Step 5: Run code quality checks**

Run: `cd backend && mvn spotless:apply -q && mvn checkstyle:check pmd:check spotbugs:check -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/ansible/role/service/RoleVariableService.java backend/src/test/java/com/ansible/role/service/RoleVariableServiceTest.java
git commit -m "feat: add RoleVariableService with unit tests"
```

---

### Task 4: RoleVariableController with Integration Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/role/controller/RoleVariableController.java`
- Create: `backend/src/test/java/com/ansible/role/controller/RoleVariableControllerTest.java`

- [ ] **Step 1: Write the integration test**

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
import com.ansible.role.dto.CreateRoleVariableRequest;
import com.ansible.role.dto.RoleResponse;
import com.ansible.role.dto.RoleVariableResponse;
import com.ansible.role.dto.UpdateRoleVariableRequest;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.RoleVariableRepository;
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

class RoleVariableControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private RoleVariableRepository roleVariableRepository;

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
    roleVariableRepository.deleteAll();
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

  private Long createVariable(String key, String value) {
    CreateRoleVariableRequest req = new CreateRoleVariableRequest();
    req.setKey(key);
    req.setValue(value);
    ResponseEntity<Result<RoleVariableResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/vars",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().getId();
  }

  @Test
  void createVariable_success() {
    CreateRoleVariableRequest req = new CreateRoleVariableRequest();
    req.setKey("http_port");
    req.setValue("8080");

    ResponseEntity<Result<RoleVariableResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/vars",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getKey()).isEqualTo("http_port");
    assertThat(resp.getBody().getData().getValue()).isEqualTo("8080");
  }

  @Test
  void createVariable_duplicateKey() {
    createVariable("http_port", "8080");

    CreateRoleVariableRequest req = new CreateRoleVariableRequest();
    req.setKey("http_port");
    req.setValue("9090");

    ResponseEntity<Result<RoleVariableResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/vars",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void getVariables_success() {
    createVariable("http_port", "8080");
    createVariable("server_name", "localhost");

    ResponseEntity<Result<List<RoleVariableResponse>>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/vars",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData()).hasSize(2);
  }

  @Test
  void updateVariable_success() {
    Long varId = createVariable("http_port", "8080");

    UpdateRoleVariableRequest req = new UpdateRoleVariableRequest();
    req.setValue("9090");

    ResponseEntity<Result<RoleVariableResponse>> resp =
        restTemplate.exchange(
            "/api/role-vars/" + varId,
            HttpMethod.PUT,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getValue()).isEqualTo("9090");
  }

  @Test
  void deleteVariable_success() {
    Long varId = createVariable("http_port", "8080");

    ResponseEntity<Result<Void>> resp =
        restTemplate.exchange(
            "/api/role-vars/" + varId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Result<List<RoleVariableResponse>>> listResp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/vars",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});
    assertThat(listResp.getBody().getData()).isEmpty();
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn verify -Dtest=RoleVariableControllerTest -DfailIfNoTests=false -q`
Expected: FAIL — `RoleVariableController` not found

- [ ] **Step 3: Implement RoleVariableController**

```java
package com.ansible.role.controller;

import com.ansible.common.Result;
import com.ansible.role.dto.CreateRoleVariableRequest;
import com.ansible.role.dto.RoleVariableResponse;
import com.ansible.role.dto.UpdateRoleVariableRequest;
import com.ansible.role.service.RoleVariableService;
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
public class RoleVariableController {

  private final RoleVariableService roleVariableService;

  @PostMapping("/roles/{roleId}/vars")
  public Result<RoleVariableResponse> createVariable(
      @PathVariable Long roleId,
      @Valid @RequestBody CreateRoleVariableRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleVariableService.createVariable(roleId, request, currentUserId));
  }

  @GetMapping("/roles/{roleId}/vars")
  public Result<List<RoleVariableResponse>> getVariables(
      @PathVariable Long roleId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleVariableService.getVariablesByRole(roleId, currentUserId));
  }

  @PutMapping("/role-vars/{id}")
  public Result<RoleVariableResponse> updateVariable(
      @PathVariable Long id,
      @Valid @RequestBody UpdateRoleVariableRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleVariableService.updateVariable(id, request, currentUserId));
  }

  @DeleteMapping("/role-vars/{id}")
  public Result<Void> deleteVariable(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    roleVariableService.deleteVariable(id, currentUserId);
    return Result.success();
  }
}
```

- [ ] **Step 4: Run integration tests to verify they pass**

Run: `cd backend && mvn verify -Dtest=RoleVariableControllerTest -DfailIfNoTests=false -q`
Expected: ALL PASS

- [ ] **Step 5: Run full test suite and code quality checks**

Run: `cd backend && mvn spotless:apply -q && mvn verify -q`
Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/ansible/role/controller/RoleVariableController.java backend/src/test/java/com/ansible/role/controller/RoleVariableControllerTest.java
git commit -m "feat: add RoleVariableController with integration tests"
```

---

### Task 5: RoleDefaultVariable Entity, Repository & DTOs

**Files:**
- Create: `backend/src/main/java/com/ansible/role/entity/RoleDefaultVariable.java`
- Create: `backend/src/main/java/com/ansible/role/repository/RoleDefaultVariableRepository.java`
- Create: `backend/src/main/java/com/ansible/role/dto/CreateRoleDefaultVariableRequest.java`
- Create: `backend/src/main/java/com/ansible/role/dto/UpdateRoleDefaultVariableRequest.java`
- Create: `backend/src/main/java/com/ansible/role/dto/RoleDefaultVariableResponse.java`

- [ ] **Step 1: Create RoleDefaultVariable entity**

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
    name = "role_default_variables",
    uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "variable_key"}))
@Getter
@Setter
@NoArgsConstructor
public class RoleDefaultVariable extends BaseEntity {

  @Column(name = "role_id", nullable = false)
  private Long roleId;

  @Column(name = "variable_key", nullable = false, length = 200)
  private String key;

  @Column(columnDefinition = "TEXT")
  private String value;
}
```

- [ ] **Step 2: Create RoleDefaultVariableRepository**

```java
package com.ansible.role.repository;

import com.ansible.role.entity.RoleDefaultVariable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleDefaultVariableRepository extends JpaRepository<RoleDefaultVariable, Long> {

  List<RoleDefaultVariable> findAllByRoleIdOrderByKeyAsc(Long roleId);

  boolean existsByRoleIdAndKey(Long roleId, String key);
}
```

- [ ] **Step 3: Create CreateRoleDefaultVariableRequest**

```java
package com.ansible.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoleDefaultVariableRequest {

  @NotBlank(message = "Variable key is required")
  @Size(max = 200, message = "Key must not exceed 200 characters")
  private String key;

  private String value;
}
```

- [ ] **Step 4: Create UpdateRoleDefaultVariableRequest**

```java
package com.ansible.role.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoleDefaultVariableRequest {

  @Size(max = 200, message = "Key must not exceed 200 characters")
  private String key;

  private String value;
}
```

- [ ] **Step 5: Create RoleDefaultVariableResponse**

```java
package com.ansible.role.dto;

import com.ansible.role.entity.RoleDefaultVariable;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class RoleDefaultVariableResponse {

  private final Long id;
  private final Long roleId;
  private final String key;
  private final String value;
  private final Long createdBy;
  private final LocalDateTime createdAt;

  @JsonCreator
  public RoleDefaultVariableResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("roleId") Long roleId,
      @JsonProperty("key") String key,
      @JsonProperty("value") String value,
      @JsonProperty("createdBy") Long createdBy,
      @JsonProperty("createdAt") LocalDateTime createdAt) {
    this.id = id;
    this.roleId = roleId;
    this.key = key;
    this.value = value;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
  }

  public RoleDefaultVariableResponse(RoleDefaultVariable variable) {
    this.id = variable.getId();
    this.roleId = variable.getRoleId();
    this.key = variable.getKey();
    this.value = variable.getValue();
    this.createdBy = variable.getCreatedBy();
    this.createdAt = variable.getCreatedAt();
  }
}
```

- [ ] **Step 6: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/ansible/role/entity/RoleDefaultVariable.java backend/src/main/java/com/ansible/role/repository/RoleDefaultVariableRepository.java backend/src/main/java/com/ansible/role/dto/CreateRoleDefaultVariableRequest.java backend/src/main/java/com/ansible/role/dto/UpdateRoleDefaultVariableRequest.java backend/src/main/java/com/ansible/role/dto/RoleDefaultVariableResponse.java
git commit -m "feat: add RoleDefaultVariable entity, repository and DTOs"
```

---

### Task 6: RoleDefaultVariableService with Unit Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/role/service/RoleDefaultVariableService.java`
- Create: `backend/src/test/java/com/ansible/role/service/RoleDefaultVariableServiceTest.java`

- [ ] **Step 1: Write the unit test**

```java
package com.ansible.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.role.dto.CreateRoleDefaultVariableRequest;
import com.ansible.role.dto.RoleDefaultVariableResponse;
import com.ansible.role.dto.UpdateRoleDefaultVariableRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.RoleDefaultVariable;
import com.ansible.role.repository.RoleDefaultVariableRepository;
import com.ansible.role.repository.RoleRepository;
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
class RoleDefaultVariableServiceTest {

  @Mock private RoleDefaultVariableRepository roleDefaultVariableRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private ProjectAccessChecker accessChecker;
  @InjectMocks private RoleDefaultVariableService roleDefaultVariableService;

  private Role testRole;
  private RoleDefaultVariable testVariable;

  @BeforeEach
  void setUp() {
    testRole = new Role();
    ReflectionTestUtils.setField(testRole, "id", 1L);
    testRole.setProjectId(10L);
    testRole.setName("nginx");
    testRole.setCreatedBy(10L);

    testVariable = new RoleDefaultVariable();
    ReflectionTestUtils.setField(testVariable, "id", 1L);
    testVariable.setRoleId(1L);
    testVariable.setKey("http_port");
    testVariable.setValue("80");
    testVariable.setCreatedBy(10L);
    ReflectionTestUtils.setField(testVariable, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(testVariable, "updatedAt", LocalDateTime.now());
  }

  @Test
  void createDefault_success() {
    CreateRoleDefaultVariableRequest request = new CreateRoleDefaultVariableRequest();
    request.setKey("http_port");
    request.setValue("80");

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleDefaultVariableRepository.existsByRoleIdAndKey(1L, "http_port")).thenReturn(false);
    when(roleDefaultVariableRepository.save(any(RoleDefaultVariable.class)))
        .thenReturn(testVariable);

    RoleDefaultVariableResponse response =
        roleDefaultVariableService.createDefault(1L, request, 10L);

    assertThat(response.getKey()).isEqualTo("http_port");
    assertThat(response.getValue()).isEqualTo("80");
    verify(roleDefaultVariableRepository).save(any(RoleDefaultVariable.class));
  }

  @Test
  void createDefault_duplicateKey() {
    CreateRoleDefaultVariableRequest request = new CreateRoleDefaultVariableRequest();
    request.setKey("http_port");
    request.setValue("80");

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleDefaultVariableRepository.existsByRoleIdAndKey(1L, "http_port")).thenReturn(true);

    assertThatThrownBy(() -> roleDefaultVariableService.createDefault(1L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Default variable key already exists in this role");
  }

  @Test
  void createDefault_roleNotFound() {
    CreateRoleDefaultVariableRequest request = new CreateRoleDefaultVariableRequest();
    request.setKey("http_port");

    when(roleRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleDefaultVariableService.createDefault(99L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Role not found");
  }

  @Test
  void getDefaultsByRole_success() {
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleDefaultVariableRepository.findAllByRoleIdOrderByKeyAsc(1L))
        .thenReturn(List.of(testVariable));

    List<RoleDefaultVariableResponse> defaults =
        roleDefaultVariableService.getDefaultsByRole(1L, 10L);

    assertThat(defaults).hasSize(1);
    assertThat(defaults.get(0).getKey()).isEqualTo("http_port");
  }

  @Test
  void updateDefault_success() {
    UpdateRoleDefaultVariableRequest request = new UpdateRoleDefaultVariableRequest();
    request.setValue("8080");

    when(roleDefaultVariableRepository.findById(1L)).thenReturn(Optional.of(testVariable));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleDefaultVariableRepository.save(any(RoleDefaultVariable.class)))
        .thenReturn(testVariable);

    RoleDefaultVariableResponse response =
        roleDefaultVariableService.updateDefault(1L, request, 10L);

    assertThat(response).isNotNull();
    verify(roleDefaultVariableRepository).save(any(RoleDefaultVariable.class));
  }

  @Test
  void updateDefault_duplicateKey() {
    UpdateRoleDefaultVariableRequest request = new UpdateRoleDefaultVariableRequest();
    request.setKey("server_name");

    when(roleDefaultVariableRepository.findById(1L)).thenReturn(Optional.of(testVariable));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleDefaultVariableRepository.existsByRoleIdAndKey(1L, "server_name")).thenReturn(true);

    assertThatThrownBy(() -> roleDefaultVariableService.updateDefault(1L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Default variable key already exists in this role");
  }

  @Test
  void updateDefault_keyNotChanged() {
    UpdateRoleDefaultVariableRequest request = new UpdateRoleDefaultVariableRequest();
    request.setKey("http_port");
    request.setValue("8080");

    when(roleDefaultVariableRepository.findById(1L)).thenReturn(Optional.of(testVariable));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleDefaultVariableRepository.save(any(RoleDefaultVariable.class)))
        .thenReturn(testVariable);

    RoleDefaultVariableResponse response =
        roleDefaultVariableService.updateDefault(1L, request, 10L);

    assertThat(response).isNotNull();
    verify(roleDefaultVariableRepository).save(any(RoleDefaultVariable.class));
  }

  @Test
  void updateDefault_notFound() {
    UpdateRoleDefaultVariableRequest request = new UpdateRoleDefaultVariableRequest();
    request.setValue("8080");

    when(roleDefaultVariableRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleDefaultVariableService.updateDefault(99L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Role default variable not found");
  }

  @Test
  void deleteDefault_success() {
    when(roleDefaultVariableRepository.findById(1L)).thenReturn(Optional.of(testVariable));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    roleDefaultVariableService.deleteDefault(1L, 10L);

    verify(roleDefaultVariableRepository).delete(testVariable);
  }

  @Test
  void deleteDefault_notFound() {
    when(roleDefaultVariableRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleDefaultVariableService.deleteDefault(99L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Role default variable not found");
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -Dtest=RoleDefaultVariableServiceTest -q`
Expected: FAIL — `RoleDefaultVariableService` not found

- [ ] **Step 3: Implement RoleDefaultVariableService**

```java
package com.ansible.role.service;

import com.ansible.role.dto.CreateRoleDefaultVariableRequest;
import com.ansible.role.dto.RoleDefaultVariableResponse;
import com.ansible.role.dto.UpdateRoleDefaultVariableRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.RoleDefaultVariable;
import com.ansible.role.repository.RoleDefaultVariableRepository;
import com.ansible.role.repository.RoleRepository;
import com.ansible.security.ProjectAccessChecker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RoleDefaultVariableService {

  private final RoleDefaultVariableRepository roleDefaultVariableRepository;
  private final RoleRepository roleRepository;
  private final ProjectAccessChecker accessChecker;

  @Transactional
  public RoleDefaultVariableResponse createDefault(
      Long roleId, CreateRoleDefaultVariableRequest request, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);

    if (roleDefaultVariableRepository.existsByRoleIdAndKey(roleId, request.getKey())) {
      throw new IllegalArgumentException("Default variable key already exists in this role");
    }

    RoleDefaultVariable variable = new RoleDefaultVariable();
    variable.setRoleId(roleId);
    variable.setKey(request.getKey());
    variable.setValue(request.getValue());
    variable.setCreatedBy(currentUserId);
    RoleDefaultVariable saved = roleDefaultVariableRepository.save(variable);
    return new RoleDefaultVariableResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<RoleDefaultVariableResponse> getDefaultsByRole(Long roleId, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    return roleDefaultVariableRepository.findAllByRoleIdOrderByKeyAsc(roleId).stream()
        .map(RoleDefaultVariableResponse::new)
        .toList();
  }

  @Transactional
  public RoleDefaultVariableResponse updateDefault(
      Long variableId, UpdateRoleDefaultVariableRequest request, Long currentUserId) {
    RoleDefaultVariable variable =
        roleDefaultVariableRepository
            .findById(variableId)
            .orElseThrow(() -> new IllegalArgumentException("Role default variable not found"));
    Role role =
        roleRepository
            .findById(variable.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), variable.getCreatedBy(), currentUserId);

    if (StringUtils.hasText(request.getKey())
        && !request.getKey().equals(variable.getKey())) {
      if (roleDefaultVariableRepository.existsByRoleIdAndKey(
          variable.getRoleId(), request.getKey())) {
        throw new IllegalArgumentException("Default variable key already exists in this role");
      }
      variable.setKey(request.getKey());
    }
    if (request.getValue() != null) {
      variable.setValue(request.getValue());
    }
    RoleDefaultVariable saved = roleDefaultVariableRepository.save(variable);
    return new RoleDefaultVariableResponse(saved);
  }

  @Transactional
  public void deleteDefault(Long variableId, Long currentUserId) {
    RoleDefaultVariable variable =
        roleDefaultVariableRepository
            .findById(variableId)
            .orElseThrow(() -> new IllegalArgumentException("Role default variable not found"));
    Role role =
        roleRepository
            .findById(variable.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), variable.getCreatedBy(), currentUserId);
    roleDefaultVariableRepository.delete(variable);
  }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend && mvn test -Dtest=RoleDefaultVariableServiceTest -q`
Expected: ALL PASS

- [ ] **Step 5: Run code quality checks**

Run: `cd backend && mvn spotless:apply -q && mvn checkstyle:check pmd:check spotbugs:check -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/ansible/role/service/RoleDefaultVariableService.java backend/src/test/java/com/ansible/role/service/RoleDefaultVariableServiceTest.java
git commit -m "feat: add RoleDefaultVariableService with unit tests"
```

---

### Task 7: RoleDefaultVariableController with Integration Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/role/controller/RoleDefaultVariableController.java`
- Create: `backend/src/test/java/com/ansible/role/controller/RoleDefaultVariableControllerTest.java`

- [ ] **Step 1: Write the integration test**

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
import com.ansible.role.dto.CreateRoleDefaultVariableRequest;
import com.ansible.role.dto.RoleDefaultVariableResponse;
import com.ansible.role.dto.RoleResponse;
import com.ansible.role.dto.UpdateRoleDefaultVariableRequest;
import com.ansible.role.repository.RoleDefaultVariableRepository;
import com.ansible.role.repository.RoleRepository;
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

class RoleDefaultVariableControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private RoleDefaultVariableRepository roleDefaultVariableRepository;

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
    roleDefaultVariableRepository.deleteAll();
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

  private Long createDefault(String key, String value) {
    CreateRoleDefaultVariableRequest req = new CreateRoleDefaultVariableRequest();
    req.setKey(key);
    req.setValue(value);
    ResponseEntity<Result<RoleDefaultVariableResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/defaults",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().getId();
  }

  @Test
  void createDefault_success() {
    CreateRoleDefaultVariableRequest req = new CreateRoleDefaultVariableRequest();
    req.setKey("http_port");
    req.setValue("80");

    ResponseEntity<Result<RoleDefaultVariableResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/defaults",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getKey()).isEqualTo("http_port");
    assertThat(resp.getBody().getData().getValue()).isEqualTo("80");
  }

  @Test
  void createDefault_duplicateKey() {
    createDefault("http_port", "80");

    CreateRoleDefaultVariableRequest req = new CreateRoleDefaultVariableRequest();
    req.setKey("http_port");
    req.setValue("8080");

    ResponseEntity<Result<RoleDefaultVariableResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/defaults",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void getDefaults_success() {
    createDefault("http_port", "80");
    createDefault("server_name", "localhost");

    ResponseEntity<Result<List<RoleDefaultVariableResponse>>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/defaults",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData()).hasSize(2);
  }

  @Test
  void updateDefault_success() {
    Long varId = createDefault("http_port", "80");

    UpdateRoleDefaultVariableRequest req = new UpdateRoleDefaultVariableRequest();
    req.setValue("8080");

    ResponseEntity<Result<RoleDefaultVariableResponse>> resp =
        restTemplate.exchange(
            "/api/role-defaults/" + varId,
            HttpMethod.PUT,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getValue()).isEqualTo("8080");
  }

  @Test
  void deleteDefault_success() {
    Long varId = createDefault("http_port", "80");

    ResponseEntity<Result<Void>> resp =
        restTemplate.exchange(
            "/api/role-defaults/" + varId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Result<List<RoleDefaultVariableResponse>>> listResp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/defaults",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});
    assertThat(listResp.getBody().getData()).isEmpty();
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn verify -Dtest=RoleDefaultVariableControllerTest -DfailIfNoTests=false -q`
Expected: FAIL — `RoleDefaultVariableController` not found

- [ ] **Step 3: Implement RoleDefaultVariableController**

```java
package com.ansible.role.controller;

import com.ansible.common.Result;
import com.ansible.role.dto.CreateRoleDefaultVariableRequest;
import com.ansible.role.dto.RoleDefaultVariableResponse;
import com.ansible.role.dto.UpdateRoleDefaultVariableRequest;
import com.ansible.role.service.RoleDefaultVariableService;
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
public class RoleDefaultVariableController {

  private final RoleDefaultVariableService roleDefaultVariableService;

  @PostMapping("/roles/{roleId}/defaults")
  public Result<RoleDefaultVariableResponse> createDefault(
      @PathVariable Long roleId,
      @Valid @RequestBody CreateRoleDefaultVariableRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleDefaultVariableService.createDefault(roleId, request, currentUserId));
  }

  @GetMapping("/roles/{roleId}/defaults")
  public Result<List<RoleDefaultVariableResponse>> getDefaults(
      @PathVariable Long roleId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleDefaultVariableService.getDefaultsByRole(roleId, currentUserId));
  }

  @PutMapping("/role-defaults/{id}")
  public Result<RoleDefaultVariableResponse> updateDefault(
      @PathVariable Long id,
      @Valid @RequestBody UpdateRoleDefaultVariableRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleDefaultVariableService.updateDefault(id, request, currentUserId));
  }

  @DeleteMapping("/role-defaults/{id}")
  public Result<Void> deleteDefault(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    roleDefaultVariableService.deleteDefault(id, currentUserId);
    return Result.success();
  }
}
```

- [ ] **Step 4: Run integration tests to verify they pass**

Run: `cd backend && mvn verify -Dtest=RoleDefaultVariableControllerTest -DfailIfNoTests=false -q`
Expected: ALL PASS

- [ ] **Step 5: Run full backend test suite and code quality checks**

Run: `cd backend && mvn spotless:apply -q && mvn verify -q`
Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/ansible/role/controller/RoleDefaultVariableController.java backend/src/test/java/com/ansible/role/controller/RoleDefaultVariableControllerTest.java
git commit -m "feat: add RoleDefaultVariableController with integration tests"
```

---

### Task 8: Frontend Types and API Layer

**Files:**
- Create: `frontend/src/types/entity/RoleVariable.ts`
- Create: `frontend/src/api/roleVariable.ts`

- [ ] **Step 1: Create RoleVariable TypeScript types**

```typescript
export interface RoleVariable {
  id: number;
  roleId: number;
  key: string;
  value: string;
  createdBy: number;
  createdAt: string;
}

export interface CreateRoleVariableRequest {
  key: string;
  value?: string;
}

export interface UpdateRoleVariableRequest {
  key?: string;
  value?: string;
}

export interface RoleDefaultVariable {
  id: number;
  roleId: number;
  key: string;
  value: string;
  createdBy: number;
  createdAt: string;
}

export interface CreateRoleDefaultVariableRequest {
  key: string;
  value?: string;
}

export interface UpdateRoleDefaultVariableRequest {
  key?: string;
  value?: string;
}
```

- [ ] **Step 2: Create roleVariable API module**

```typescript
import request from './request';
import type {
  RoleVariable,
  CreateRoleVariableRequest,
  UpdateRoleVariableRequest,
  RoleDefaultVariable,
  CreateRoleDefaultVariableRequest,
  UpdateRoleDefaultVariableRequest,
} from '../types/entity/RoleVariable';

// Role Variable APIs
export async function createRoleVariable(
  roleId: number,
  data: CreateRoleVariableRequest
): Promise<RoleVariable> {
  const res = await request.post<RoleVariable>(`/roles/${roleId}/vars`, data);
  return res.data;
}

export async function getRoleVariables(roleId: number): Promise<RoleVariable[]> {
  const res = await request.get<RoleVariable[]>(`/roles/${roleId}/vars`);
  return res.data;
}

export async function updateRoleVariable(
  id: number,
  data: UpdateRoleVariableRequest
): Promise<RoleVariable> {
  const res = await request.put<RoleVariable>(`/role-vars/${id}`, data);
  return res.data;
}

export async function deleteRoleVariable(id: number): Promise<void> {
  await request.delete(`/role-vars/${id}`);
}

// Role Default Variable APIs
export async function createRoleDefault(
  roleId: number,
  data: CreateRoleDefaultVariableRequest
): Promise<RoleDefaultVariable> {
  const res = await request.post<RoleDefaultVariable>(
    `/roles/${roleId}/defaults`,
    data
  );
  return res.data;
}

export async function getRoleDefaults(roleId: number): Promise<RoleDefaultVariable[]> {
  const res = await request.get<RoleDefaultVariable[]>(`/roles/${roleId}/defaults`);
  return res.data;
}

export async function updateRoleDefault(
  id: number,
  data: UpdateRoleDefaultVariableRequest
): Promise<RoleDefaultVariable> {
  const res = await request.put<RoleDefaultVariable>(`/role-defaults/${id}`, data);
  return res.data;
}

export async function deleteRoleDefault(id: number): Promise<void> {
  await request.delete(`/role-defaults/${id}`);
}
```

- [ ] **Step 3: Verify frontend compiles**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add frontend/src/types/entity/RoleVariable.ts frontend/src/api/roleVariable.ts
git commit -m "feat: add RoleVariable and RoleDefaultVariable TypeScript types and API layer"
```

---

### Task 9: RoleVars Component

**Files:**
- Create: `frontend/src/pages/role/RoleVars.tsx`
- Modify: `frontend/src/pages/role/RoleDetail.tsx`

- [ ] **Step 1: Create RoleVars component**

```tsx
import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Space,
  Table,
  message,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type {
  RoleVariable,
  CreateRoleVariableRequest,
  UpdateRoleVariableRequest,
} from '../../types/entity/RoleVariable';
import {
  createRoleVariable,
  getRoleVariables,
  updateRoleVariable,
  deleteRoleVariable,
} from '../../api/roleVariable';

interface RoleVarsProps {
  roleId: number;
}

export default function RoleVars({ roleId }: RoleVarsProps) {
  const [variables, setVariables] = useState<RoleVariable[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingVar, setEditingVar] = useState<RoleVariable | null>(null);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const list = await getRoleVariables(roleId);
      setVariables(list);
    } finally {
      setLoading(false);
    }
  }, [roleId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleCreate = () => {
    setEditingVar(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (variable: RoleVariable) => {
    setEditingVar(variable);
    form.setFieldsValue({ key: variable.key, value: variable.value });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (editingVar) {
      const data: UpdateRoleVariableRequest = {
        key: values.key,
        value: values.value,
      };
      await updateRoleVariable(editingVar.id, data);
      message.success('变量已更新');
    } else {
      const data: CreateRoleVariableRequest = {
        key: values.key,
        value: values.value,
      };
      await createRoleVariable(roleId, data);
      message.success('变量已创建');
    }
    setModalOpen(false);
    fetchData();
  };

  const handleDelete = async (id: number) => {
    await deleteRoleVariable(id);
    message.success('变量已删除');
    fetchData();
  };

  const columns = [
    { title: 'Key', dataIndex: 'key', key: 'key' },
    { title: 'Value', dataIndex: 'value', key: 'value' },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: RoleVariable) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          添加变量
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={variables}
        rowKey="id"
        loading={loading}
        pagination={false}
      />
      <Modal
        title={editingVar ? '编辑变量' : '添加变量'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="key"
            label="Key"
            rules={[{ required: true, message: '请输入变量名' }]}
          >
            <Input placeholder="例如: http_port" />
          </Form.Item>
          <Form.Item name="value" label="Value">
            <Input.TextArea rows={3} placeholder="变量值" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
```

- [ ] **Step 2: Update RoleDetail.tsx to use RoleVars**

In `frontend/src/pages/role/RoleDetail.tsx`, add the import and replace the `ComingSoon` placeholder for `vars` tab:

Add import:
```typescript
import RoleVars from './RoleVars';
```

Replace tab item:
```typescript
{ key: 'vars', label: 'Vars', children: <RoleVars roleId={Number(roleId)} /> },
```

- [ ] **Step 3: Verify frontend compiles**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 4: Run frontend lint**

Run: `cd frontend && npm run lint`
Expected: No errors

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/role/RoleVars.tsx frontend/src/pages/role/RoleDetail.tsx
git commit -m "feat: add RoleVars tab component"
```

---

### Task 10: RoleDefaults Component

**Files:**
- Create: `frontend/src/pages/role/RoleDefaults.tsx`
- Modify: `frontend/src/pages/role/RoleDetail.tsx`

- [ ] **Step 1: Create RoleDefaults component**

```tsx
import { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Space,
  Table,
  message,
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type {
  RoleDefaultVariable,
  CreateRoleDefaultVariableRequest,
  UpdateRoleDefaultVariableRequest,
} from '../../types/entity/RoleVariable';
import {
  createRoleDefault,
  getRoleDefaults,
  updateRoleDefault,
  deleteRoleDefault,
} from '../../api/roleVariable';

interface RoleDefaultsProps {
  roleId: number;
}

export default function RoleDefaults({ roleId }: RoleDefaultsProps) {
  const [defaults, setDefaults] = useState<RoleDefaultVariable[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingVar, setEditingVar] = useState<RoleDefaultVariable | null>(null);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const list = await getRoleDefaults(roleId);
      setDefaults(list);
    } finally {
      setLoading(false);
    }
  }, [roleId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleCreate = () => {
    setEditingVar(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (variable: RoleDefaultVariable) => {
    setEditingVar(variable);
    form.setFieldsValue({ key: variable.key, value: variable.value });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (editingVar) {
      const data: UpdateRoleDefaultVariableRequest = {
        key: values.key,
        value: values.value,
      };
      await updateRoleDefault(editingVar.id, data);
      message.success('默认变量已更新');
    } else {
      const data: CreateRoleDefaultVariableRequest = {
        key: values.key,
        value: values.value,
      };
      await createRoleDefault(roleId, data);
      message.success('默认变量已创建');
    }
    setModalOpen(false);
    fetchData();
  };

  const handleDelete = async (id: number) => {
    await deleteRoleDefault(id);
    message.success('默认变量已删除');
    fetchData();
  };

  const columns = [
    { title: 'Key', dataIndex: 'key', key: 'key' },
    { title: 'Value', dataIndex: 'value', key: 'value' },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: RoleDefaultVariable) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          添加默认变量
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={defaults}
        rowKey="id"
        loading={loading}
        pagination={false}
      />
      <Modal
        title={editingVar ? '编辑默认变量' : '添加默认变量'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="key"
            label="Key"
            rules={[{ required: true, message: '请输入变量名' }]}
          >
            <Input placeholder="例如: http_port" />
          </Form.Item>
          <Form.Item name="value" label="Value">
            <Input.TextArea rows={3} placeholder="默认值" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
```

- [ ] **Step 2: Update RoleDetail.tsx to use RoleDefaults**

Add import:
```typescript
import RoleDefaults from './RoleDefaults';
```

Replace tab item:
```typescript
{ key: 'defaults', label: 'Defaults', children: <RoleDefaults roleId={Number(roleId)} /> },
```

- [ ] **Step 3: Remove ComingSoon component if no longer used**

If both `vars` and `defaults` tabs now use real components (and `templates`/`files` still use `ComingSoon`), keep the `ComingSoon` component. If all tabs have real components, remove it.

At this point, `templates` and `files` tabs still use `ComingSoon`, so keep it.

- [ ] **Step 4: Verify frontend compiles**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 5: Run frontend lint**

Run: `cd frontend && npm run lint`
Expected: No errors

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/role/RoleDefaults.tsx frontend/src/pages/role/RoleDetail.tsx
git commit -m "feat: add RoleDefaults tab component"
```

---

### Task 11: Update Progress & Final Verification

**Files:**
- Modify: `CLAUDE.md` (update progress section)

- [ ] **Step 1: Run full backend test suite**

Run: `cd backend && mvn spotless:apply -q && mvn verify -q`
Expected: BUILD SUCCESS, all tests pass (including existing tests)

- [ ] **Step 2: Run full frontend checks**

Run: `cd frontend && npm run lint && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 3: Update CLAUDE.md progress**

Change:
```
- Phase 4 ✅ Task、Handler CRUD
- 后续：RoleVariable、RoleDefaultVariable、Template、File、Variable、Environment、Tag、Playbook
```

To:
```
- Phase 4 ✅ Task、Handler CRUD
- Phase 5 ✅ RoleVariable、RoleDefaultVariable CRUD
- 后续：Template、File、Variable、Environment、Tag、Playbook
```

- [ ] **Step 4: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: update progress to Phase 5 - RoleVariable/RoleDefaultVariable complete"
```
