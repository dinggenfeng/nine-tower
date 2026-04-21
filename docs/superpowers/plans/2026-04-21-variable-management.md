# Variable Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Variable CRUD with four scopes (PROJECT, HOSTGROUP, ENVIRONMENT) for managing Ansible variables at different priority levels.

**Architecture:** Single Variable entity with scope enum + scopeId to support four levels. API filters by scope + optional scopeId. Depends on Environment module being complete (for ENVIRONMENT scope). Follows existing pattern.

**Tech Stack:** Spring Boot 3.x, Spring Data JPA, Jakarta Validation, Mockito + Testcontainers (PostgreSQL 16), React 18 + Ant Design 5 + TypeScript

**Dependencies:** Environment module must be complete before this module.

---

## File Change Summary

| File | Action |
|------|--------|
| `backend/.../variable/entity/Variable.java` | Create |
| `backend/.../variable/entity/VariableScope.java` | Create (enum) |
| `backend/.../variable/repository/VariableRepository.java` | Create |
| `backend/.../variable/dto/CreateVariableRequest.java` | Create |
| `backend/.../variable/dto/UpdateVariableRequest.java` | Create |
| `backend/.../variable/dto/VariableResponse.java` | Create |
| `backend/.../variable/service/VariableService.java` | Create |
| `backend/.../variable/controller/VariableController.java` | Create |
| `backend/.../variable/service/VariableServiceTest.java` | Create |
| `backend/.../variable/controller/VariableControllerTest.java` | Create |
| `frontend/src/types/entity/Variable.ts` | Create |
| `frontend/src/api/variable.ts` | Create |
| `frontend/src/pages/variable/VariableManager.tsx` | Create |

---

## Backend Tasks

### Task 1: Variable Entity with Scope Enum and Repository

**Files:**
- Create: `backend/src/main/java/com/ansible/variable/entity/VariableScope.java`
- Create: `backend/src/main/java/com/ansible/variable/entity/Variable.java`
- Create: `backend/src/main/java/com/ansible/variable/repository/VariableRepository.java`

- [ ] **Step 1: Create VariableScope enum**

```java
package com.ansible.variable.entity;

public enum VariableScope {
    PROJECT,
    HOSTGROUP,
    ENVIRONMENT
}
```

- [ ] **Step 2: Create Variable entity**

```java
package com.ansible.variable.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "variables", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"scope", "scopeId", "key"})
})
@Getter
@Setter
@NoArgsConstructor
public class Variable extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VariableScope scope;

    @Column(nullable = false)
    private Long scopeId;

    @Column(nullable = false, length = 100)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String value;
}
```

- [ ] **Step 3: Create VariableRepository**

```java
package com.ansible.variable.repository;

import com.ansible.variable.entity.Variable;
import com.ansible.variable.entity.VariableScope;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariableRepository extends JpaRepository<Variable, Long> {

    List<Variable> findByScopeAndScopeIdOrderByIdAsc(VariableScope scope, Long scopeId);

    List<Variable> findByScopeOrderByIdAsc(VariableScope scope);

    boolean existsByScopeAndScopeIdAndKey(VariableScope scope, Long scopeId, String key);

    boolean existsByScopeAndScopeIdAndKeyAndIdNot(VariableScope scope, Long scopeId, String key, Long id);
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/ansible/variable/
git commit -m "feat(variable): add Variable entity, scope enum, and repository"
```

---

### Task 2: Variable DTOs

**Files:**
- Create: `backend/src/main/java/com/ansible/variable/dto/CreateVariableRequest.java`
- Create: `backend/src/main/java/com/ansible/variable/dto/UpdateVariableRequest.java`
- Create: `backend/src/main/java/com/ansible/variable/dto/VariableResponse.java`

- [ ] **Step 1: Create DTOs**

```java
// CreateVariableRequest.java
package com.ansible.variable.dto;

import com.ansible.variable.entity.VariableScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateVariableRequest(
    @NotNull VariableScope scope,
    @NotNull Long scopeId,
    @NotBlank @Size(max = 100) String key,
    String value
) {}

// UpdateVariableRequest.java
package com.ansible.variable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateVariableRequest(
    @NotBlank @Size(max = 100) String key,
    String value
) {}

// VariableResponse.java
package com.ansible.variable.dto;

import com.ansible.variable.entity.VariableScope;
import java.time.LocalDateTime;

public record VariableResponse(
    Long id,
    VariableScope scope,
    Long scopeId,
    String key,
    String value,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/ansible/variable/dto/
git commit -m "feat(variable): add Variable DTOs"
```

---

### Task 3: VariableService with Unit Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/variable/service/VariableService.java`
- Create: `backend/src/test/java/com/ansible/variable/service/VariableServiceTest.java`

- [ ] **Step 1: Write VariableServiceTest**

```java
package com.ansible.variable.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ansible.variable.dto.CreateVariableRequest;
import com.ansible.variable.dto.VariableResponse;
import com.ansible.variable.dto.UpdateVariableRequest;
import com.ansible.variable.entity.Variable;
import com.ansible.variable.entity.VariableScope;
import com.ansible.variable.repository.VariableRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VariableServiceTest {

    @Mock
    private VariableRepository variableRepository;

    @InjectMocks
    private VariableService variableService;

    private Variable createVar(Long id, VariableScope scope, Long scopeId, String key, String value) {
        Variable v = new Variable();
        v.setId(id);
        v.setScope(scope);
        v.setScopeId(scopeId);
        v.setKey(key);
        v.setValue(value);
        return v;
    }

    @Test
    void createVariable_projectScope_success() {
        when(variableRepository.existsByScopeAndScopeIdAndKey(VariableScope.PROJECT, 1L, "APP_PORT"))
            .thenReturn(false);
        when(variableRepository.save(any(Variable.class))).thenAnswer(inv -> {
            Variable v = inv.getArgument(0);
            v.setId(10L);
            return v;
        });

        VariableResponse response = variableService.createVariable(
            1L, new CreateVariableRequest(VariableScope.PROJECT, 1L, "APP_PORT", "8080"), 100L);

        assertThat(response.key()).isEqualTo("APP_PORT");
        assertThat(response.scope()).isEqualTo(VariableScope.PROJECT);
    }

    @Test
    void createVariable_duplicateKey_throws() {
        when(variableRepository.existsByScopeAndScopeIdAndKey(VariableScope.PROJECT, 1L, "APP_PORT"))
            .thenReturn(true);

        assertThatThrownBy(() -> variableService.createVariable(
            1L, new CreateVariableRequest(VariableScope.PROJECT, 1L, "APP_PORT", "8080"), 100L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void listVariables_byScopeAndScopeId() {
        when(variableRepository.findByScopeAndScopeIdOrderByIdAsc(VariableScope.PROJECT, 1L))
            .thenReturn(List.of(
                createVar(1L, VariableScope.PROJECT, 1L, "APP_PORT", "8080"),
                createVar(2L, VariableScope.PROJECT, 1L, "APP_HOST", "localhost")));

        List<VariableResponse> list = variableService.listVariables(VariableScope.PROJECT, 1L);

        assertThat(list).hasSize(2);
    }

    @Test
    void listVariables_byScopeOnly() {
        when(variableRepository.findByScopeOrderByIdAsc(VariableScope.PROJECT))
            .thenReturn(List.of(createVar(1L, VariableScope.PROJECT, 1L, "APP_PORT", "8080")));

        List<VariableResponse> list = variableService.listVariables(VariableScope.PROJECT, null);

        assertThat(list).hasSize(1);
    }

    @Test
    void updateVariable_success() {
        Variable v = createVar(1L, VariableScope.PROJECT, 1L, "APP_PORT", "8080");
        when(variableRepository.findById(1L)).thenReturn(Optional.of(v));
        when(variableRepository.existsByScopeAndScopeIdAndKeyAndIdNot(VariableScope.PROJECT, 1L, "PORT", 1L))
            .thenReturn(false);
        when(variableRepository.save(any(Variable.class))).thenReturn(v);

        VariableResponse response = variableService.updateVariable(1L, new UpdateVariableRequest("PORT", "9090"), 100L);

        assertThat(response.key()).isEqualTo("PORT");
    }

    @Test
    void deleteVariable_success() {
        Variable v = createVar(1L, VariableScope.PROJECT, 1L, "APP_PORT", "8080");
        when(variableRepository.findById(1L)).thenReturn(Optional.of(v));

        variableService.deleteVariable(1L, 100L);

        verify(variableRepository).delete(v);
    }
}
```

- [ ] **Step 2: Run test to verify failures**

Run: `cd backend && mvn test -Dtest=VariableServiceTest -pl .`
Expected: FAIL

- [ ] **Step 3: Write VariableService**

```java
package com.ansible.variable.service;

import com.ansible.variable.dto.CreateVariableRequest;
import com.ansible.variable.dto.UpdateVariableRequest;
import com.ansible.variable.dto.VariableResponse;
import com.ansible.variable.entity.Variable;
import com.ansible.variable.entity.VariableScope;
import com.ansible.variable.repository.VariableRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VariableService {

    private final VariableRepository variableRepository;

    public VariableService(VariableRepository variableRepository) {
        this.variableRepository = variableRepository;
    }

    public VariableResponse createVariable(Long projectId, CreateVariableRequest request, Long userId) {
        if (variableRepository.existsByScopeAndScopeIdAndKey(request.scope(), request.scopeId(), request.key())) {
            throw new IllegalArgumentException("Variable '" + request.key() + "' already exists in this scope");
        }
        Variable v = new Variable();
        v.setScope(request.scope());
        v.setScopeId(request.scopeId());
        v.setKey(request.key());
        v.setValue(request.value());
        v.setCreatedBy(userId);
        return toResponse(variableRepository.save(v));
    }

    @Transactional(readOnly = true)
    public List<VariableResponse> listVariables(VariableScope scope, Long scopeId) {
        if (scopeId != null) {
            return variableRepository.findByScopeAndScopeIdOrderByIdAsc(scope, scopeId).stream()
                .map(this::toResponse).toList();
        }
        return variableRepository.findByScopeOrderByIdAsc(scope).stream()
            .map(this::toResponse).toList();
    }

    public VariableResponse updateVariable(Long varId, UpdateVariableRequest request, Long userId) {
        Variable v = variableRepository.findById(varId)
            .orElseThrow(() -> new IllegalArgumentException("Variable not found"));
        if (!v.getKey().equals(request.key())) {
            if (variableRepository.existsByScopeAndScopeIdAndKeyAndIdNot(v.getScope(), v.getScopeId(), request.key(), varId)) {
                throw new IllegalArgumentException("Variable '" + request.key() + "' already exists in this scope");
            }
        }
        v.setKey(request.key());
        v.setValue(request.value());
        return toResponse(variableRepository.save(v));
    }

    public void deleteVariable(Long varId, Long userId) {
        Variable v = variableRepository.findById(varId)
            .orElseThrow(() -> new IllegalArgumentException("Variable not found"));
        variableRepository.delete(v);
    }

    private VariableResponse toResponse(Variable v) {
        return new VariableResponse(v.getId(), v.getScope(), v.getScopeId(),
            v.getKey(), v.getValue(), v.getCreatedAt(), v.getUpdatedAt());
    }
}
```

- [ ] **Step 4: Run tests**

Run: `cd backend && mvn test -Dtest=VariableServiceTest -pl .`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/variable/service/ backend/src/test/java/com/ansible/variable/
git commit -m "feat(variable): add VariableService with unit tests"
```

---

### Task 4: VariableController with Integration Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/variable/controller/VariableController.java`
- Create: `backend/src/test/java/com/ansible/variable/controller/VariableControllerTest.java`

- [ ] **Step 1: Write VariableControllerTest**

```java
package com.ansible.variable.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.common.AbstractIntegrationTest;
import com.ansible.variable.dto.CreateVariableRequest;
import com.ansible.variable.entity.VariableScope;
import com.ansible.variable.repository.VariableRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

class VariableControllerTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private VariableRepository variableRepository;

    private String authHeaders;
    private Long projectId;

    @BeforeEach
    void setUp() {
        var auth = registerAndLogin("varuser", "varuser@test.com", "pass123");
        authHeaders = auth.token();
        projectId = createProject(authHeaders, "Var Test Project");
    }

    @AfterEach
    void tearDown() {
        variableRepository.deleteAll();
        cleanupProjectsAndUsers();
    }

    @Test
    void createVariable_projectScope_success() {
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/projects/" + projectId + "/variables",
            HttpMethod.POST,
            withAuth(authHeaders, new CreateVariableRequest(VariableScope.PROJECT, projectId, "APP_PORT", "8080")),
            String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("APP_PORT", "PROJECT");
    }

    @Test
    void createVariable_duplicateKey_returns400() {
        restTemplate.exchange("/api/projects/" + projectId + "/variables",
            HttpMethod.POST,
            withAuth(authHeaders, new CreateVariableRequest(VariableScope.PROJECT, projectId, "APP_PORT", "8080")),
            String.class);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/projects/" + projectId + "/variables",
            HttpMethod.POST,
            withAuth(authHeaders, new CreateVariableRequest(VariableScope.PROJECT, projectId, "APP_PORT", "9090")),
            String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void listVariables_byProjectScope() {
        restTemplate.exchange("/api/projects/" + projectId + "/variables",
            HttpMethod.POST,
            withAuth(authHeaders, new CreateVariableRequest(VariableScope.PROJECT, projectId, "PORT", "8080")),
            String.class);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/projects/" + projectId + "/variables?scope=PROJECT",
            HttpMethod.GET, withAuth(authHeaders, null), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("PORT");
    }

    @Test
    void deleteVariable_success() {
        var createResp = restTemplate.exchange("/api/projects/" + projectId + "/variables",
            HttpMethod.POST,
            withAuth(authHeaders, new CreateVariableRequest(VariableScope.PROJECT, projectId, "PORT", "8080")),
            String.class);
        Long varId = extractId(createResp);

        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/variables/" + varId,
            HttpMethod.DELETE, withAuth(authHeaders, null), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(variableRepository.findById(varId)).isEmpty();
    }
}
```

- [ ] **Step 2: Write VariableController**

```java
package com.ansible.variable.controller;

import com.ansible.variable.dto.CreateVariableRequest;
import com.ansible.variable.dto.UpdateVariableRequest;
import com.ansible.variable.dto.VariableResponse;
import com.ansible.variable.entity.VariableScope;
import com.ansible.variable.service.VariableService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class VariableController {

    private final VariableService variableService;

    public VariableController(VariableService variableService) {
        this.variableService = variableService;
    }

    @PostMapping("/projects/{projectId}/variables")
    public ResponseEntity<VariableResponse> createVariable(
        @PathVariable Long projectId,
        @Valid @RequestBody CreateVariableRequest request,
        @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(variableService.createVariable(projectId, request, userId));
    }

    @GetMapping("/projects/{projectId}/variables")
    public ResponseEntity<List<VariableResponse>> listVariables(
        @PathVariable Long projectId,
        @RequestParam VariableScope scope,
        @RequestParam(required = false) Long scopeId) {
        return ResponseEntity.ok(variableService.listVariables(scope, scopeId));
    }

    @GetMapping("/variables/{varId}")
    public ResponseEntity<VariableResponse> getVariable(@PathVariable Long varId) {
        return ResponseEntity.ok(variableService.getVariable(varId));
    }

    @PutMapping("/variables/{varId}")
    public ResponseEntity<VariableResponse> updateVariable(
        @PathVariable Long varId,
        @Valid @RequestBody UpdateVariableRequest request,
        @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(variableService.updateVariable(varId, request, userId));
    }

    @DeleteMapping("/variables/{varId}")
    public ResponseEntity<Void> deleteVariable(
        @PathVariable Long varId,
        @AuthenticationPrincipal Long userId) {
        variableService.deleteVariable(varId, userId);
        return ResponseEntity.ok().build();
    }
}
```

Add `getVariable` to VariableService (simple findById → toResponse).

- [ ] **Step 3: Run integration tests**

Run: `cd backend && mvn verify -Dtest=VariableControllerTest -pl .`
Expected: PASS

- [ ] **Step 4: Run quality checks**

Run: `cd backend && mvn spotless:apply checkstyle:check pmd:check spotbugs:check -pl .`
Expected: All pass

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/variable/controller/ backend/src/test/java/com/ansible/variable/controller/
git commit -m "feat(variable): add VariableController with integration tests"
```

---

## Frontend Tasks

### Task 5: Variable Types and API Layer

**Files:**
- Create: `frontend/src/types/entity/Variable.ts`
- Create: `frontend/src/api/variable.ts`

- [ ] **Step 1: Create Variable types**

```typescript
// frontend/src/types/entity/Variable.ts

export type VariableScope = 'PROJECT' | 'HOSTGROUP' | 'ENVIRONMENT';

export interface Variable {
  id: number;
  scope: VariableScope;
  scopeId: number;
  key: string;
  value: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateVariableRequest {
  scope: VariableScope;
  scopeId: number;
  key: string;
  value?: string;
}

export interface UpdateVariableRequest {
  key: string;
  value?: string;
}
```

- [ ] **Step 2: Create Variable API**

```typescript
// frontend/src/api/variable.ts

import request from './request';
import type { Variable, CreateVariableRequest, UpdateVariableRequest, VariableScope } from '../types/entity/Variable';

export const variableApi = {
  list: (projectId: number, scope: VariableScope, scopeId?: number) =>
    request.get<Variable[]>(`/api/projects/${projectId}/variables`, {
      params: { scope, ...(scopeId != null ? { scopeId } : {}) },
    }),

  get: (varId: number) =>
    request.get<Variable>(`/api/variables/${varId}`),

  create: (projectId: number, data: CreateVariableRequest) =>
    request.post<Variable>(`/api/projects/${projectId}/variables`, data),

  update: (varId: number, data: UpdateVariableRequest) =>
    request.put<Variable>(`/api/variables/${varId}`, data),

  delete: (varId: number) =>
    request.delete(`/api/variables/${varId}`),
};
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/types/entity/Variable.ts frontend/src/api/variable.ts
git commit -m "feat(variable): add Variable types and API layer"
```

---

### Task 6: VariableManager Page

**Files:**
- Create: `frontend/src/pages/variable/VariableManager.tsx`

- [ ] **Step 1: Create VariableManager component**

```tsx
// frontend/src/pages/variable/VariableManager.tsx

import { useEffect, useState } from 'react';
import { Button, Table, Modal, Form, Input, Select, message, Popconfirm, Space } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { variableApi } from '../../api/variable';
import type { Variable, VariableScope } from '../../types/entity/Variable';
import { useProjectStore } from '../../stores/projectStore';
import { environmentApi } from '../../api/environment';
import { hostGroupApi } from '../../api/hostGroup';
import type { Environment } from '../../types/entity/Environment';
import type { HostGroup } from '../../types/entity/HostGroup';

const scopeLabels: Record<VariableScope, string> = {
  PROJECT: '项目级',
  HOSTGROUP: '主机组级',
  ENVIRONMENT: '环境级',
};

export default function VariableManager() {
  const { currentProject } = useProjectStore();
  const [variables, setVariables] = useState<Variable[]>([]);
  const [loading, setLoading] = useState(false);
  const [scope, setScope] = useState<VariableScope>('PROJECT');
  const [scopeId, setScopeId] = useState<number | undefined>();
  const [modalOpen, setModalOpen] = useState(false);
  const [editingVar, setEditingVar] = useState<Variable | null>(null);
  const [hostGroups, setHostGroups] = useState<HostGroup[]>([]);
  const [environments, setEnvironments] = useState<Environment[]>([]);
  const [form] = Form.useForm();

  useEffect(() => {
    if (!currentProject) return;
    hostGroupApi.list(currentProject.id).then(res => setHostGroups(res.data));
    environmentApi.list(currentProject.id).then(res => setEnvironments(res.data));
  }, [currentProject]);

  const fetchVariables = async () => {
    if (!currentProject) return;
    setLoading(true);
    try {
      const res = await variableApi.list(currentProject.id, scope, scopeId);
      setVariables(res.data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchVariables(); }, [currentProject, scope, scopeId]);

  const handleScopeChange = (newScope: VariableScope) => {
    setScope(newScope);
    setScopeId(undefined);
  };

  const handleCreate = () => {
    setEditingVar(null);
    form.resetFields();
    form.setFieldsValue({ scope, scopeId });
    setModalOpen(true);
  };

  const handleEdit = (v: Variable) => {
    setEditingVar(v);
    form.setFieldsValue({ key: v.key, value: v.value });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (editingVar) {
      await variableApi.update(editingVar.id, { key: values.key, value: values.value });
      message.success('更新成功');
    } else {
      await variableApi.create(currentProject!.id, {
        scope: values.scope,
        scopeId: values.scopeId,
        key: values.key,
        value: values.value,
      });
      message.success('创建成功');
    }
    setModalOpen(false);
    fetchVariables();
  };

  const handleDelete = async (varId: number) => {
    await variableApi.delete(varId);
    message.success('删除成功');
    fetchVariables();
  };

  const scopeOptions = (): { label: string; value: number }[] => {
    if (scope === 'HOSTGROUP') return hostGroups.map(h => ({ label: h.name, value: h.id }));
    if (scope === 'ENVIRONMENT') return environments.map(e => ({ label: e.name, value: e.id }));
    return [];
  };

  const columns = [
    { title: 'Key', dataIndex: 'key', key: 'key' },
    { title: 'Value', dataIndex: 'value', key: 'value' },
    {
      title: '操作', key: 'action',
      render: (_: unknown, record: Variable) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', gap: 12, alignItems: 'center' }}>
        <Select value={scope} onChange={handleScopeChange} style={{ width: 140 }}
          options={Object.entries(scopeLabels).map(([k, v]) => ({ label: v, value: k }))} />
        {scope !== 'PROJECT' && (
          <Select value={scopeId} onChange={setScopeId} style={{ width: 200 }}
            placeholder={`选择${scopeLabels[scope]}`}
            options={scopeOptions()} />
        )}
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>新建变量</Button>
      </div>
      <Table rowKey="id" columns={columns} dataSource={variables} loading={loading} />

      <Modal title={editingVar ? '编辑变量' : '新建变量'} open={modalOpen} onOk={handleSubmit} onCancel={() => setModalOpen(false)}>
        <Form form={form} layout="vertical">
          {!editingVar && (
            <>
              <Form.Item name="scope" label="作用域" rules={[{ required: true }]}>
                <Select options={Object.entries(scopeLabels).map(([k, v]) => ({ label: v, value: k }))} />
              </Form.Item>
              {form.getFieldValue('scope') !== 'PROJECT' && (
                <Form.Item name="scopeId" label="关联对象" rules={[{ required: true }]}>
                  <Select options={scopeOptions()} />
                </Form.Item>
              )}
            </>
          )}
          <Form.Item name="key" label="Key" rules={[{ required: true }]}>
            <Input maxLength={100} placeholder="变量名，如 APP_PORT" />
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

- [ ] **Step 2: Wire into project routes**

Add route and menu item for "变量管理" pointing to `/projects/:id/variables`.

- [ ] **Step 3: Run lint and build**

Run: `cd frontend && npm run lint && npm run build`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/variable/ frontend/src/components/Layout/
git commit -m "feat(variable): add VariableManager page and route"
```
