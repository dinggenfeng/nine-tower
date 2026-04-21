# Environment Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Environment CRUD with EnvConfig key-value pairs for managing deployment environments (dev/staging/production etc.).

**Architecture:** Environment entity belongs to Project. EnvConfig belongs to Environment (via environmentId FK). Follows existing pattern: entity → repository → DTOs → service → controller → tests.

**Tech Stack:** Spring Boot 3.x, Spring Data JPA, Jakarta Validation, Mockito + Testcontainers (PostgreSQL 16), React 18 + Ant Design 5 + TypeScript

---

## File Change Summary

| File | Action |
|------|--------|
| `backend/.../environment/entity/Environment.java` | Create |
| `backend/.../environment/entity/EnvConfig.java` | Create |
| `backend/.../environment/repository/EnvironmentRepository.java` | Create |
| `backend/.../environment/repository/EnvConfigRepository.java` | Create |
| `backend/.../environment/dto/CreateEnvironmentRequest.java` | Create |
| `backend/.../environment/dto/UpdateEnvironmentRequest.java` | Create |
| `backend/.../environment/dto/EnvironmentResponse.java` | Create |
| `backend/.../environment/dto/EnvConfigRequest.java` | Create |
| `backend/.../environment/dto/EnvConfigResponse.java` | Create |
| `backend/.../environment/service/EnvironmentService.java` | Create |
| `backend/.../environment/controller/EnvironmentController.java` | Create |
| `backend/.../environment/service/EnvironmentServiceTest.java` | Create |
| `backend/.../environment/controller/EnvironmentControllerTest.java` | Create |
| `frontend/src/types/entity/Environment.ts` | Create |
| `frontend/src/api/environment.ts` | Create |
| `frontend/src/pages/environment/EnvironmentManager.tsx` | Create |

---

## Backend Tasks

### Task 1: Environment and EnvConfig Entities

**Files:**
- Create: `backend/src/main/java/com/ansible/environment/entity/Environment.java`
- Create: `backend/src/main/java/com/ansible/environment/entity/EnvConfig.java`

- [ ] **Step 1: Create Environment entity**

```java
package com.ansible.environment.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "environments", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"projectId", "name"})
})
@Getter
@Setter
@NoArgsConstructor
public class Environment extends BaseEntity {

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;
}
```

- [ ] **Step 2: Create EnvConfig entity**

```java
package com.ansible.environment.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "env_configs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"environmentId", "configKey"})
})
@Getter
@Setter
@NoArgsConstructor
public class EnvConfig extends BaseEntity {

    @Column(nullable = false)
    private Long environmentId;

    @Column(nullable = false, length = 100)
    private String configKey;

    @Column(columnDefinition = "TEXT")
    private String configValue;
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/ansible/environment/
git commit -m "feat(environment): add Environment and EnvConfig entities"
```

---

### Task 2: Environment and EnvConfig Repositories

**Files:**
- Create: `backend/src/main/java/com/ansible/environment/repository/EnvironmentRepository.java`
- Create: `backend/src/main/java/com/ansible/environment/repository/EnvConfigRepository.java`

- [ ] **Step 1: Create EnvironmentRepository**

```java
package com.ansible.environment.repository;

import com.ansible.environment.entity.Environment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    List<Environment> findByProjectIdOrderByIdAsc(Long projectId);

    boolean existsByProjectIdAndName(Long projectId, String name);

    boolean existsByProjectIdAndNameAndIdNot(Long projectId, String name, Long id);
}
```

- [ ] **Step 2: Create EnvConfigRepository**

```java
package com.ansible.environment.repository;

import com.ansible.environment.entity.EnvConfig;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnvConfigRepository extends JpaRepository<EnvConfig, Long> {

    List<EnvConfig> findByEnvironmentIdOrderByConfigKeyAsc(Long environmentId);

    boolean existsByEnvironmentIdAndConfigKey(Long environmentId, String configKey);

    boolean existsByEnvironmentIdAndConfigKeyAndIdNot(Long environmentId, String configKey, Long id);

    void deleteByEnvironmentId(Long environmentId);
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/ansible/environment/repository/
git commit -m "feat(environment): add EnvironmentRepository and EnvConfigRepository"
```

---

### Task 3: Environment DTOs

**Files:**
- Create: `backend/src/main/java/com/ansible/environment/dto/CreateEnvironmentRequest.java`
- Create: `backend/src/main/java/com/ansible/environment/dto/UpdateEnvironmentRequest.java`
- Create: `backend/src/main/java/com/ansible/environment/dto/EnvironmentResponse.java`
- Create: `backend/src/main/java/com/ansible/environment/dto/EnvConfigRequest.java`
- Create: `backend/src/main/java/com/ansible/environment/dto/EnvConfigResponse.java`

- [ ] **Step 1: Create request/response DTOs**

```java
// CreateEnvironmentRequest.java
package com.ansible.environment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateEnvironmentRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description
) {}

// UpdateEnvironmentRequest.java
package com.ansible.environment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEnvironmentRequest(
    @NotBlank @Size(max = 100) String name,
    @Size(max = 500) String description
) {}

// EnvironmentResponse.java
package com.ansible.environment.dto;

import java.time.LocalDateTime;
import java.util.List;

public record EnvironmentResponse(
    Long id,
    Long projectId,
    String name,
    String description,
    List<EnvConfigResponse> configs,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

// EnvConfigRequest.java
package com.ansible.environment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnvConfigRequest(
    @NotBlank @Size(max = 100) String configKey,
    String configValue
) {}

// EnvConfigResponse.java
package com.ansible.environment.dto;

public record EnvConfigResponse(
    Long id,
    Long environmentId,
    String configKey,
    String configValue
) {}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/ansible/environment/dto/
git commit -m "feat(environment): add Environment and EnvConfig DTOs"
```

---

### Task 4: EnvironmentService with Unit Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/environment/service/EnvironmentService.java`
- Create: `backend/src/test/java/com/ansible/environment/service/EnvironmentServiceTest.java`

- [ ] **Step 1: Write EnvironmentServiceTest**

```java
package com.ansible.environment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ansible.environment.dto.*;
import com.ansible.environment.entity.EnvConfig;
import com.ansible.environment.entity.Environment;
import com.ansible.environment.repository.EnvConfigRepository;
import com.ansible.environment.repository.EnvironmentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnvironmentServiceTest {

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private EnvConfigRepository envConfigRepository;

    @InjectMocks
    private EnvironmentService environmentService;

    private Environment createEnv(Long id, Long projectId, String name) {
        Environment env = new Environment();
        env.setId(id);
        env.setProjectId(projectId);
        env.setName(name);
        return env;
    }

    @Test
    void createEnvironment_success() {
        when(environmentRepository.existsByProjectIdAndName(1L, "dev")).thenReturn(false);
        when(environmentRepository.save(any(Environment.class))).thenAnswer(inv -> {
            Environment e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        var response = environmentService.createEnvironment(1L, new CreateEnvironmentRequest("dev", "Development"), 100L);

        assertThat(response.name()).isEqualTo("dev");
    }

    @Test
    void createEnvironment_duplicateName_throws() {
        when(environmentRepository.existsByProjectIdAndName(1L, "dev")).thenReturn(true);

        assertThatThrownBy(() -> environmentService.createEnvironment(1L, new CreateEnvironmentRequest("dev", null), 100L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void listEnvironments_success() {
        when(environmentRepository.findByProjectIdOrderByIdAsc(1L))
            .thenReturn(List.of(createEnv(1L, 1L, "dev"), createEnv(2L, 1L, "prod")));
        when(envConfigRepository.findByEnvironmentIdOrderByConfigKeyAsc(anyLong())).thenReturn(List.of());

        var list = environmentService.listEnvironments(1L);

        assertThat(list).hasSize(2);
    }

    @Test
    void getEnvironment_success() {
        Environment env = createEnv(1L, 1L, "dev");
        env.setDescription("Dev env");
        when(environmentRepository.findById(1L)).thenReturn(Optional.of(env));
        when(envConfigRepository.findByEnvironmentIdOrderByConfigKeyAsc(1L)).thenReturn(List.of());

        var response = environmentService.getEnvironment(1L);

        assertThat(response.name()).isEqualTo("dev");
        assertThat(response.description()).isEqualTo("Dev env");
    }

    @Test
    void updateEnvironment_success() {
        Environment env = createEnv(1L, 1L, "dev");
        when(environmentRepository.findById(1L)).thenReturn(Optional.of(env));
        when(environmentRepository.existsByProjectIdAndNameAndIdNot(1L, "staging", 1L)).thenReturn(false);
        when(environmentRepository.save(any(Environment.class))).thenReturn(env);

        var response = environmentService.updateEnvironment(1L, new UpdateEnvironmentRequest("staging", "Staging"), 100L);

        assertThat(response.name()).isEqualTo("staging");
    }

    @Test
    void deleteEnvironment_success() {
        Environment env = createEnv(1L, 1L, "dev");
        when(environmentRepository.findById(1L)).thenReturn(Optional.of(env));

        environmentService.deleteEnvironment(1L, 100L);

        verify(envConfigRepository).deleteByEnvironmentId(1L);
        verify(environmentRepository).delete(env);
    }

    @Test
    void addConfig_success() {
        Environment env = createEnv(1L, 1L, "dev");
        when(environmentRepository.findById(1L)).thenReturn(Optional.of(env));
        when(envConfigRepository.existsByEnvironmentIdAndConfigKey(1L, "DB_HOST")).thenReturn(false);
        when(envConfigRepository.save(any(EnvConfig.class))).thenAnswer(inv -> {
            EnvConfig c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        var response = environmentService.addConfig(1L, new EnvConfigRequest("DB_HOST", "localhost"), 100L);

        assertThat(response.configKey()).isEqualTo("DB_HOST");
    }

    @Test
    void addConfig_duplicateKey_throws() {
        Environment env = createEnv(1L, 1L, "dev");
        when(environmentRepository.findById(1L)).thenReturn(Optional.of(env));
        when(envConfigRepository.existsByEnvironmentIdAndConfigKey(1L, "DB_HOST")).thenReturn(true);

        assertThatThrownBy(() -> environmentService.addConfig(1L, new EnvConfigRequest("DB_HOST", "localhost"), 100L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void removeConfig_success() {
        EnvConfig config = new EnvConfig();
        config.setId(10L);
        config.setEnvironmentId(1L);
        when(envConfigRepository.findById(10L)).thenReturn(Optional.of(config));

        environmentService.removeConfig(10L);

        verify(envConfigRepository).delete(config);
    }
}
```

- [ ] **Step 2: Run test to verify failures**

Run: `cd backend && mvn test -Dtest=EnvironmentServiceTest -pl .`
Expected: FAIL

- [ ] **Step 3: Write EnvironmentService**

```java
package com.ansible.environment.service;

import com.ansible.environment.dto.*;
import com.ansible.environment.entity.EnvConfig;
import com.ansible.environment.entity.Environment;
import com.ansible.environment.repository.EnvConfigRepository;
import com.ansible.environment.repository.EnvironmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final EnvConfigRepository envConfigRepository;

    public EnvironmentService(EnvironmentRepository environmentRepository,
                              EnvConfigRepository envConfigRepository) {
        this.environmentRepository = environmentRepository;
        this.envConfigRepository = envConfigRepository;
    }

    public EnvironmentResponse createEnvironment(Long projectId, CreateEnvironmentRequest request, Long userId) {
        if (environmentRepository.existsByProjectIdAndName(projectId, request.name())) {
            throw new IllegalArgumentException("Environment with name '" + request.name() + "' already exists");
        }
        Environment env = new Environment();
        env.setProjectId(projectId);
        env.setName(request.name());
        env.setDescription(request.description());
        env.setCreatedBy(userId);
        return toResponse(environmentRepository.save(env), List.of());
    }

    @Transactional(readOnly = true)
    public List<EnvironmentResponse> listEnvironments(Long projectId) {
        return environmentRepository.findByProjectIdOrderByIdAsc(projectId).stream()
            .map(env -> toResponse(env, envConfigRepository.findByEnvironmentIdOrderByConfigKeyAsc(env.getId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public EnvironmentResponse getEnvironment(Long envId) {
        Environment env = environmentRepository.findById(envId)
            .orElseThrow(() -> new IllegalArgumentException("Environment not found"));
        return toResponse(env, envConfigRepository.findByEnvironmentIdOrderByConfigKeyAsc(envId));
    }

    public EnvironmentResponse updateEnvironment(Long envId, UpdateEnvironmentRequest request, Long userId) {
        Environment env = environmentRepository.findById(envId)
            .orElseThrow(() -> new IllegalArgumentException("Environment not found"));
        if (environmentRepository.existsByProjectIdAndNameAndIdNot(env.getProjectId(), request.name(), envId)) {
            throw new IllegalArgumentException("Environment with name '" + request.name() + "' already exists");
        }
        env.setName(request.name());
        env.setDescription(request.description());
        return toResponse(environmentRepository.save(env),
            envConfigRepository.findByEnvironmentIdOrderByConfigKeyAsc(envId));
    }

    public void deleteEnvironment(Long envId, Long userId) {
        Environment env = environmentRepository.findById(envId)
            .orElseThrow(() -> new IllegalArgumentException("Environment not found"));
        envConfigRepository.deleteByEnvironmentId(envId);
        environmentRepository.delete(env);
    }

    public EnvConfigResponse addConfig(Long envId, EnvConfigRequest request, Long userId) {
        environmentRepository.findById(envId)
            .orElseThrow(() -> new IllegalArgumentException("Environment not found"));
        if (envConfigRepository.existsByEnvironmentIdAndConfigKey(envId, request.configKey())) {
            throw new IllegalArgumentException("Config key '" + request.configKey() + "' already exists");
        }
        EnvConfig config = new EnvConfig();
        config.setEnvironmentId(envId);
        config.setConfigKey(request.configKey());
        config.setConfigValue(request.configValue());
        config.setCreatedBy(userId);
        config = envConfigRepository.save(config);
        return new EnvConfigResponse(config.getId(), config.getEnvironmentId(),
            config.getConfigKey(), config.getConfigValue());
    }

    public void removeConfig(Long configId) {
        EnvConfig config = envConfigRepository.findById(configId)
            .orElseThrow(() -> new IllegalArgumentException("Config not found"));
        envConfigRepository.delete(config);
    }

    private EnvironmentResponse toResponse(Environment env, List<EnvConfig> configs) {
        return new EnvironmentResponse(
            env.getId(), env.getProjectId(), env.getName(), env.getDescription(),
            configs.stream().map(c -> new EnvConfigResponse(c.getId(), c.getEnvironmentId(),
                c.getConfigKey(), c.getConfigValue())).toList(),
            env.getCreatedAt(), env.getUpdatedAt());
    }
}
```

- [ ] **Step 4: Run tests**

Run: `cd backend && mvn test -Dtest=EnvironmentServiceTest -pl .`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/environment/service/ backend/src/test/java/com/ansible/environment/
git commit -m "feat(environment): add EnvironmentService with unit tests"
```

---

### Task 5: EnvironmentController with Integration Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/environment/controller/EnvironmentController.java`
- Create: `backend/src/test/java/com/ansible/environment/controller/EnvironmentControllerTest.java`

- [ ] **Step 1: Write EnvironmentControllerTest** (integration test)

```java
package com.ansible.environment.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.common.AbstractIntegrationTest;
import com.ansible.environment.dto.*;
import com.ansible.environment.repository.EnvConfigRepository;
import com.ansible.environment.repository.EnvironmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

class EnvironmentControllerTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private EnvConfigRepository envConfigRepository;

    private String authHeaders;
    private Long projectId;

    @BeforeEach
    void setUp() {
        var auth = registerAndLogin("envuser", "envuser@test.com", "pass123");
        authHeaders = auth.token();
        projectId = createProject(authHeaders, "Env Test Project");
    }

    @AfterEach
    void tearDown() {
        envConfigRepository.deleteAll();
        environmentRepository.deleteAll();
        cleanupProjectsAndUsers();
    }

    @Test
    void createEnvironment_success() {
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/projects/" + projectId + "/environments",
            HttpMethod.POST,
            withAuth(authHeaders, new CreateEnvironmentRequest("dev", "Development")),
            String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("dev");
    }

    @Test
    void createEnvironment_duplicateName_returns400() {
        restTemplate.exchange("/api/projects/" + projectId + "/environments",
            HttpMethod.POST, withAuth(authHeaders, new CreateEnvironmentRequest("dev", null)), String.class);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/projects/" + projectId + "/environments",
            HttpMethod.POST,
            withAuth(authHeaders, new CreateEnvironmentRequest("dev", null)),
            String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void listEnvironments_success() {
        restTemplate.exchange("/api/projects/" + projectId + "/environments",
            HttpMethod.POST, withAuth(authHeaders, new CreateEnvironmentRequest("dev", null)), String.class);
        restTemplate.exchange("/api/projects/" + projectId + "/environments",
            HttpMethod.POST, withAuth(authHeaders, new CreateEnvironmentRequest("prod", null)), String.class);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/projects/" + projectId + "/environments",
            HttpMethod.GET, withAuth(authHeaders, null), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("dev", "prod");
    }

    @Test
    void addConfig_success() {
        var createResp = restTemplate.exchange("/api/projects/" + projectId + "/environments",
            HttpMethod.POST, withAuth(authHeaders, new CreateEnvironmentRequest("dev", null)), String.class);
        Long envId = extractId(createResp);

        ResponseEntity<String> configResp = restTemplate.exchange(
            "/api/environments/" + envId + "/configs",
            HttpMethod.POST,
            withAuth(authHeaders, new EnvConfigRequest("DB_HOST", "localhost")),
            String.class);

        assertThat(configResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(configResp.getBody()).contains("DB_HOST");
    }

    @Test
    void deleteEnvironment_cascadesConfigs() {
        var createResp = restTemplate.exchange("/api/projects/" + projectId + "/environments",
            HttpMethod.POST, withAuth(authHeaders, new CreateEnvironmentRequest("dev", null)), String.class);
        Long envId = extractId(createResp);

        restTemplate.exchange("/api/environments/" + envId + "/configs",
            HttpMethod.POST, withAuth(authHeaders, new EnvConfigRequest("KEY", "val")), String.class);

        restTemplate.exchange("/api/environments/" + envId,
            HttpMethod.DELETE, withAuth(authHeaders, null), Void.class);

        assertThat(environmentRepository.findById(envId)).isEmpty();
        assertThat(envConfigRepository.findByEnvironmentIdOrderByConfigKeyAsc(envId)).isEmpty();
    }
}
```

- [ ] **Step 2: Write EnvironmentController**

```java
package com.ansible.environment.controller;

import com.ansible.environment.dto.*;
import com.ansible.environment.service.EnvironmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    public EnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @PostMapping("/projects/{projectId}/environments")
    public ResponseEntity<EnvironmentResponse> createEnvironment(
        @PathVariable Long projectId,
        @Valid @RequestBody CreateEnvironmentRequest request,
        @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(environmentService.createEnvironment(projectId, request, userId));
    }

    @GetMapping("/projects/{projectId}/environments")
    public ResponseEntity<List<EnvironmentResponse>> listEnvironments(@PathVariable Long projectId) {
        return ResponseEntity.ok(environmentService.listEnvironments(projectId));
    }

    @GetMapping("/environments/{envId}")
    public ResponseEntity<EnvironmentResponse> getEnvironment(@PathVariable Long envId) {
        return ResponseEntity.ok(environmentService.getEnvironment(envId));
    }

    @PutMapping("/environments/{envId}")
    public ResponseEntity<EnvironmentResponse> updateEnvironment(
        @PathVariable Long envId,
        @Valid @RequestBody UpdateEnvironmentRequest request,
        @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(environmentService.updateEnvironment(envId, request, userId));
    }

    @DeleteMapping("/environments/{envId}")
    public ResponseEntity<Void> deleteEnvironment(
        @PathVariable Long envId,
        @AuthenticationPrincipal Long userId) {
        environmentService.deleteEnvironment(envId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/environments/{envId}/configs")
    public ResponseEntity<EnvConfigResponse> addConfig(
        @PathVariable Long envId,
        @Valid @RequestBody EnvConfigRequest request,
        @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(environmentService.addConfig(envId, request, userId));
    }

    @DeleteMapping("/env-configs/{configId}")
    public ResponseEntity<Void> removeConfig(@PathVariable Long configId) {
        environmentService.removeConfig(configId);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 3: Run integration tests**

Run: `cd backend && mvn verify -Dtest=EnvironmentControllerTest -pl .`
Expected: PASS

- [ ] **Step 4: Run quality checks**

Run: `cd backend && mvn spotless:apply checkstyle:check pmd:check spotbugs:check -pl .`
Expected: All pass

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/environment/controller/ backend/src/test/java/com/ansible/environment/controller/
git commit -m "feat(environment): add EnvironmentController with integration tests"
```

---

## Frontend Tasks

### Task 6: Environment Types and API Layer

**Files:**
- Create: `frontend/src/types/entity/Environment.ts`
- Create: `frontend/src/api/environment.ts`

- [ ] **Step 1: Create Environment types**

```typescript
// frontend/src/types/entity/Environment.ts

export interface EnvConfig {
  id: number;
  environmentId: number;
  configKey: string;
  configValue: string;
}

export interface Environment {
  id: number;
  projectId: number;
  name: string;
  description: string;
  configs: EnvConfig[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateEnvironmentRequest {
  name: string;
  description?: string;
}

export interface UpdateEnvironmentRequest {
  name: string;
  description?: string;
}

export interface EnvConfigRequest {
  configKey: string;
  configValue: string;
}
```

- [ ] **Step 2: Create Environment API**

```typescript
// frontend/src/api/environment.ts

import request from './request';
import type {
  Environment,
  CreateEnvironmentRequest,
  UpdateEnvironmentRequest,
  EnvConfigRequest,
  EnvConfigResponse as EnvConfig,
} from '../types/entity/Environment';

export const environmentApi = {
  list: (projectId: number) =>
    request.get<Environment[]>(`/api/projects/${projectId}/environments`),

  get: (envId: number) =>
    request.get<Environment>(`/api/environments/${envId}`),

  create: (projectId: number, data: CreateEnvironmentRequest) =>
    request.post<Environment>(`/api/projects/${projectId}/environments`, data),

  update: (envId: number, data: UpdateEnvironmentRequest) =>
    request.put<Environment>(`/api/environments/${envId}`, data),

  delete: (envId: number) =>
    request.delete(`/api/environments/${envId}`),

  addConfig: (envId: number, data: EnvConfigRequest) =>
    request.post<EnvConfig>(`/api/environments/${envId}/configs`, data),

  removeConfig: (configId: number) =>
    request.delete(`/api/env-configs/${configId}`),
};
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/types/entity/Environment.ts frontend/src/api/environment.ts
git commit -m "feat(environment): add Environment types and API layer"
```

---

### Task 7: EnvironmentManager Page

**Files:**
- Create: `frontend/src/pages/environment/EnvironmentManager.tsx`

- [ ] **Step 1: Create EnvironmentManager component**

```tsx
// frontend/src/pages/environment/EnvironmentManager.tsx

import { useEffect, useState } from 'react';
import { Button, Table, Modal, Form, Input, message, Popconfirm, Space, Card, Typography } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { environmentApi } from '../../api/environment';
import type { Environment, EnvConfig } from '../../types/entity/Environment';
import { useProjectStore } from '../../stores/projectStore';

const { Text } = Typography;

export default function EnvironmentManager() {
  const { currentProject } = useProjectStore();
  const [environments, setEnvironments] = useState<Environment[]>([]);
  const [loading, setLoading] = useState(false);
  const [envModalOpen, setEnvModalOpen] = useState(false);
  const [editingEnv, setEditingEnv] = useState<Environment | null>(null);
  const [configModalOpen, setConfigModalOpen] = useState(false);
  const [activeEnvId, setActiveEnvId] = useState<number | null>(null);
  const [envForm] = Form.useForm();
  const [configForm] = Form.useForm();

  const fetchEnvironments = async () => {
    if (!currentProject) return;
    setLoading(true);
    try {
      const res = await environmentApi.list(currentProject.id);
      setEnvironments(res.data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchEnvironments(); }, [currentProject]);

  const handleCreateEnv = () => {
    setEditingEnv(null);
    envForm.resetFields();
    setEnvModalOpen(true);
  };

  const handleEditEnv = (env: Environment) => {
    setEditingEnv(env);
    envForm.setFieldsValue({ name: env.name, description: env.description });
    setEnvModalOpen(true);
  };

  const handleDeleteEnv = async (envId: number) => {
    await environmentApi.delete(envId);
    message.success('删除成功');
    fetchEnvironments();
  };

  const handleEnvSubmit = async () => {
    const values = await envForm.validateFields();
    if (editingEnv) {
      await environmentApi.update(editingEnv.id, values);
      message.success('更新成功');
    } else {
      await environmentApi.create(currentProject!.id, values);
      message.success('创建成功');
    }
    setEnvModalOpen(false);
    fetchEnvironments();
  };

  const handleAddConfig = (envId: number) => {
    setActiveEnvId(envId);
    configForm.resetFields();
    setConfigModalOpen(true);
  };

  const handleConfigSubmit = async () => {
    const values = await configForm.validateFields();
    await environmentApi.addConfig(activeEnvId!, values);
    message.success('配置项已添加');
    setConfigModalOpen(false);
    fetchEnvironments();
  };

  const handleRemoveConfig = async (configId: number) => {
    await environmentApi.removeConfig(configId);
    message.success('配置项已删除');
    fetchEnvironments();
  };

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreateEnv}>
          新建环境
        </Button>
      </div>

      {environments.map(env => (
        <Card
          key={env.id}
          title={env.name}
          extra={
            <Space>
              <Button type="link" size="small" onClick={() => handleEditEnv(env)}>编辑</Button>
              <Button type="link" size="small" onClick={() => handleAddConfig(env.id)}>添加配置</Button>
              <Popconfirm title="确定删除？" onConfirm={() => handleDeleteEnv(env.id)}>
                <Button type="link" size="small" danger>删除</Button>
              </Popconfirm>
            </Space>
          }
          style={{ marginBottom: 16 }}
        >
          {env.description && <Text type="secondary">{env.description}</Text>}
          <Table
            size="small"
            pagination={false}
            rowKey="id"
            columns={[
              { title: 'Key', dataIndex: 'configKey', key: 'key' },
              { title: 'Value', dataIndex: 'configValue', key: 'value' },
              {
                title: '操作', key: 'action', width: 80,
                render: (_: unknown, record: EnvConfig) => (
                  <Popconfirm title="确定删除？" onConfirm={() => handleRemoveConfig(record.id)}>
                    <Button type="link" size="small" danger icon={<DeleteOutlined />} />
                  </Popconfirm>
                ),
              },
            ]}
            dataSource={env.configs}
          />
        </Card>
      ))}

      <Modal
        title={editingEnv ? '编辑环境' : '新建环境'}
        open={envModalOpen}
        onOk={handleEnvSubmit}
        onCancel={() => setEnvModalOpen(false)}
      >
        <Form form={envForm} layout="vertical">
          <Form.Item name="name" label="环境名称" rules={[{ required: true, message: '请输入环境名称' }]}>
            <Input maxLength={100} placeholder="例如: dev, staging, production" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea maxLength={500} placeholder="环境描述（可选）" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="添加配置项"
        open={configModalOpen}
        onOk={handleConfigSubmit}
        onCancel={() => setConfigModalOpen(false)}
      >
        <Form form={configForm} layout="vertical">
          <Form.Item name="configKey" label="Key" rules={[{ required: true }]}>
            <Input maxLength={100} placeholder="例如: DB_HOST" />
          </Form.Item>
          <Form.Item name="configValue" label="Value" rules={[{ required: true }]}>
            <Input.TextArea placeholder="配置值" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
```

- [ ] **Step 2: Wire into project routes**

Add route and menu item for "环境管理" pointing to `/projects/:id/environments`.

- [ ] **Step 3: Run lint and build**

Run: `cd frontend && npm run lint && npm run build`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/environment/ frontend/src/components/Layout/
git commit -m "feat(environment): add EnvironmentManager page and route"
```
