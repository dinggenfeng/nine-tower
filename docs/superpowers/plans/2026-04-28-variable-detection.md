# Variable Detection Feature — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scan Tasks/Handlers/Templates for `{{ variable }}` references, display deduplicated results with auto-inferred scope, let users adjust scope and batch-save variables.

**Architecture:** New `VariableDetectionService` scans all project Roles' content via regex. New `VariableDetectionController` exposes `GET /detect-variables` and `POST /variables/batch`. Frontend adds a "变量探测" button in `VariableManager` that triggers scan, shows results in a table below the toolbar, and supports batch save with validation.

**Tech Stack:** Java 21 + Spring Boot 3.x (backend), React 18 + TypeScript + Ant Design 5 (frontend)

---

### Task 1: Backend DTOs

**Files:**
- Create: `backend/src/main/java/com/ansible/variable/dto/VariableOccurrence.java`
- Create: `backend/src/main/java/com/ansible/variable/dto/DetectedVariableResponse.java`
- Create: `backend/src/main/java/com/ansible/variable/dto/BatchVariableSaveRequest.java`

- [ ] **Step 1: Create VariableOccurrence record**

```java
package com.ansible.variable.dto;

public record VariableOccurrence(
    Long roleId,
    String roleName,
    String type,       // "TASK", "HANDLER", "TEMPLATE"
    Long entityId,
    String entityName,
    String field       // "args", "whenCondition", "loop", "content", "name"
) {}
```

- [ ] **Step 2: Create DetectedVariableResponse record**

```java
package com.ansible.variable.dto;

import java.util.List;

public record DetectedVariableResponse(
    String key,
    List<VariableOccurrence> occurrences,
    String suggestedScope  // "ROLE" or "PROJECT"
) {}
```

- [ ] **Step 3: Create BatchVariableSaveRequest record**

```java
package com.ansible.variable.dto;

public record BatchVariableSaveRequest(
    String key,
    String saveAs,       // "VARIABLE" or "ROLE_VARIABLE"
    String scope,        // "PROJECT", "HOSTGROUP", "ENVIRONMENT" — only when saveAs=VARIABLE
    Long roleId,         // only when saveAs=ROLE_VARIABLE
    String value
) {}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/ansible/variable/dto/VariableOccurrence.java backend/src/main/java/com/ansible/variable/dto/DetectedVariableResponse.java backend/src/main/java/com/ansible/variable/dto/BatchVariableSaveRequest.java
git commit -m "feat: add DTOs for variable detection"
```

---

### Task 2: VariableDetectionService — scan logic

**Files:**
- Create: `backend/src/main/java/com/ansible/variable/service/VariableDetectionService.java`

- [ ] **Step 1: Create the service class with field-scanning and project-scanning methods**

```java
package com.ansible.variable.service;

import com.ansible.role.entity.*;
import com.ansible.role.repository.*;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.variable.dto.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VariableDetectionService {

  private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*\\}\\}");

  private final RoleRepository roleRepository;
  private final TaskRepository taskRepository;
  private final HandlerRepository handlerRepository;
  private final TemplateRepository templateRepository;
  private final ProjectAccessChecker accessChecker;

  public List<DetectedVariableResponse> detectVariables(Long projectId, Long userId) {
    accessChecker.checkMembership(projectId, userId);
    List<Role> roles = roleRepository.findAllByProjectId(projectId);
    if (roles.isEmpty()) return Collections.emptyList();

    // Map: key -> list of occurrences across all roles
    Map<String, List<VariableOccurrence>> allOccurrences = new LinkedHashMap<>();
    Set<Long> rolesWithKey = new HashSet<>();

    for (Role role : roles) {
      Map<String, List<VariableOccurrence>> roleOccurrences = scanRole(role);
      for (var entry : roleOccurrences.entrySet()) {
        allOccurrences.computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
            .addAll(entry.getValue());
        rolesWithKey.add(role.getId());
      }
    }

    return allOccurrences.entrySet().stream()
        .map(entry -> {
          String key = entry.getKey();
          List<VariableOccurrence> occurrences = entry.getValue();
          // Suggested scope: ROLE if only in one role, PROJECT otherwise
          Set<Long> distinctRoles = occurrences.stream()
              .map(VariableOccurrence::roleId).collect(Collectors.toSet());
          String suggestedScope = distinctRoles.size() == 1 ? "ROLE" : "PROJECT";
          return new DetectedVariableResponse(key, occurrences, suggestedScope);
        })
        .sorted(Comparator.comparing(DetectedVariableResponse::key))
        .collect(Collectors.toList());
  }

  private Map<String, List<VariableOccurrence>> scanRole(Role role) {
    Map<String, List<VariableOccurrence>> map = new LinkedHashMap<>();
    Long roleId = role.getId();
    String roleName = role.getName();

    // Scan tasks
    for (Task task : taskRepository.findAllByRoleIdOrderByTaskOrderAsc(roleId)) {
      extractVars(task.getName(), roleId, roleName, "TASK", task.getId(), task.getName(), "name", map);
      extractVars(task.getArgs(), roleId, roleName, "TASK", task.getId(), task.getName(), "args", map);
      extractVars(task.getWhenCondition(), roleId, roleName, "TASK", task.getId(), task.getName(), "whenCondition", map);
      extractVars(task.getLoop(), roleId, roleName, "TASK", task.getId(), task.getName(), "loop", map);
    }

    // Scan handlers
    for (Handler handler : handlerRepository.findAllByRoleId(roleId)) {
      extractVars(handler.getName(), roleId, roleName, "HANDLER", handler.getId(), handler.getName(), "name", map);
      extractVars(handler.getArgs(), roleId, roleName, "HANDLER", handler.getId(), handler.getName(), "args", map);
      extractVars(handler.getWhenCondition(), roleId, roleName, "HANDLER", handler.getId(), handler.getName(), "whenCondition", map);
    }

    // Scan templates
    for (Template template : templateRepository.findAllByRoleIdOrderByParentDirAscNameAsc(roleId)) {
      if (Boolean.TRUE.equals(template.getIsDirectory())) continue;
      extractVars(template.getContent(), roleId, roleName, "TEMPLATE", template.getId(), template.getName(), "content", map);
    }

    return map;
  }

  private void extractVars(String text, Long roleId, String roleName,
      String type, Long entityId, String entityName, String field,
      Map<String, List<VariableOccurrence>> map) {
    if (text == null || text.isBlank()) return;
    Matcher matcher = VAR_PATTERN.matcher(text);
    while (matcher.find()) {
      String varName = matcher.group(1);
      if (isBuiltin(varName)) continue;
      map.computeIfAbsent(varName, k -> new ArrayList<>())
          .add(new VariableOccurrence(roleId, roleName, type, entityId, entityName, field));
    }
  }

  private boolean isBuiltin(String varName) {
    return varName.equals("item") || varName.startsWith("item.")
        || varName.startsWith("ansible_");
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/ansible/variable/service/VariableDetectionService.java
git commit -m "feat: add VariableDetectionService for scanning {{ var }} references"
```

---

### Task 3: VariableDetectionService unit test

**Files:**
- Create: `backend/src/test/java/com/ansible/variable/service/VariableDetectionServiceTest.java`

- [ ] **Step 1: Write the unit test**

```java
package com.ansible.variable.service;

import com.ansible.role.entity.*;
import com.ansible.role.repository.*;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.variable.dto.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VariableDetectionServiceTest {

  @Mock private RoleRepository roleRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private HandlerRepository handlerRepository;
  @Mock private TemplateRepository templateRepository;
  @Mock private ProjectAccessChecker accessChecker;
  @InjectMocks private VariableDetectionService service;

  private Role role1, role2;
  private Task task1;
  private Handler handler1;
  private Template template1;

  @BeforeEach
  void setUp() {
    role1 = new Role(); role1.setProjectId(1L); role1.setName("web");
    ReflectionTestUtils.setField(role1, "id", 1L);
    role2 = new Role(); role2.setProjectId(1L); role2.setName("api");
    ReflectionTestUtils.setField(role2, "id", 2L);

    task1 = new Task(); task1.setRoleId(1L); task1.setName("启动 {{ app_port }}");
    task1.setModule("shell"); task1.setTaskOrder(1);
    task1.setArgs("{\"cmd\":\"start --port {{ app_port }}\"}");
    task1.setWhenCondition("ansible_os_family == '{{ target_os }}'");
    ReflectionTestUtils.setField(task1, "id", 10L);

    handler1 = new Handler(); handler1.setRoleId(2L); handler1.setName("重启服务");
    handler1.setModule("systemd");
    handler1.setArgs("{\"name\":\"{{ app_port }}\"}");
    ReflectionTestUtils.setField(handler1, "id", 20L);

    template1 = new Template(); template1.setRoleId(2L); template1.setName("nginx.conf");
    template1.setContent("server { listen {{ app_port }}; }");
    template1.setIsDirectory(false);
    ReflectionTestUtils.setField(template1, "id", 30L);
  }

  @Test
  void detectVariables_extractsVarsAndInfersScope() {
    when(roleRepository.findAllByProjectId(1L)).thenReturn(List.of(role1, role2));
    when(taskRepository.findAllByRoleIdOrderByTaskOrderAsc(1L)).thenReturn(List.of(task1));
    when(handlerRepository.findAllByRoleId(1L)).thenReturn(List.of());
    when(templateRepository.findAllByRoleIdOrderByParentDirAscNameAsc(1L)).thenReturn(List.of());
    when(taskRepository.findAllByRoleIdOrderByTaskOrderAsc(2L)).thenReturn(List.of());
    when(handlerRepository.findAllByRoleId(2L)).thenReturn(List.of(handler1));
    when(templateRepository.findAllByRoleIdOrderByParentDirAscNameAsc(2L)).thenReturn(List.of(template1));

    List<DetectedVariableResponse> result = service.detectVariables(1L, 1L);

    assertThat(result).hasSize(2);

    // app_port appears in both roles -> PROJECT scope
    DetectedVariableResponse appPort = result.stream()
        .filter(r -> r.key().equals("app_port")).findFirst().orElseThrow();
    assertThat(appPort.suggestedScope()).isEqualTo("PROJECT");
    assertThat(appPort.occurrences()).hasSize(3); // task.args + task.name + handler.args + template
    // Actually: task.name(1), task.args(1), handler.args(1), template.content(1) = 4

    // target_os appears in one role -> ROLE scope
    DetectedVariableResponse targetOs = result.stream()
        .filter(r -> r.key().equals("target_os")).findFirst().orElseThrow();
    assertThat(targetOs.suggestedScope()).isEqualTo("ROLE");
    assertThat(targetOs.occurrences()).hasSize(1);
  }

  @Test
  void detectVariables_excludesBuiltinVars() {
    task1.setLoop("{{ item.name }}");
    when(roleRepository.findAllByProjectId(1L)).thenReturn(List.of(role1));
    when(taskRepository.findAllByRoleIdOrderByTaskOrderAsc(1L)).thenReturn(List.of(task1));
    when(handlerRepository.findAllByRoleId(1L)).thenReturn(List.of());
    when(templateRepository.findAllByRoleIdOrderByParentDirAscNameAsc(1L)).thenReturn(List.of());

    List<DetectedVariableResponse> result = service.detectVariables(1L, 1L);

    assertThat(result.stream().anyMatch(r -> r.key().startsWith("item"))).isFalse();
  }

  @Test
  void detectVariables_emptyWhenNoRoles() {
    when(roleRepository.findAllByProjectId(1L)).thenReturn(List.of());

    List<DetectedVariableResponse> result = service.detectVariables(1L, 1L);

    assertThat(result).isEmpty();
  }
}
```

- [ ] **Step 2: Run test to verify it passes**

```bash
cd backend && mvn test -Dtest=VariableDetectionServiceTest -v
```
Expected: All 3 tests PASS

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/ansible/variable/service/VariableDetectionServiceTest.java
git commit -m "test: add VariableDetectionService unit tests"
```

---

### Task 4: VariableDetectionController — scan + batch save endpoints

**Files:**
- Create: `backend/src/main/java/com/ansible/variable/controller/VariableDetectionController.java`

- [ ] **Step 1: Create the controller**

```java
package com.ansible.variable.controller;

import com.ansible.common.Result;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.RoleVariable;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.RoleVariableRepository;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.variable.dto.*;
import com.ansible.variable.entity.*;
import com.ansible.variable.repository.VariableRepository;
import com.ansible.variable.service.VariableDetectionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VariableDetectionController {

  private final VariableDetectionService detectionService;
  private final VariableRepository variableRepository;
  private final RoleVariableRepository roleVariableRepository;
  private final RoleRepository roleRepository;
  private final ProjectAccessChecker accessChecker;

  @GetMapping("/projects/{projectId}/detect-variables")
  public Result<List<DetectedVariableResponse>> detectVariables(
      @PathVariable Long projectId,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    return Result.success(detectionService.detectVariables(projectId, userId));
  }

  @PostMapping("/projects/{projectId}/variables/batch")
  public Result<List<Map<String, Object>>> batchSave(
      @PathVariable Long projectId,
      @Valid @RequestBody List<BatchVariableSaveRequest> requests,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    accessChecker.checkMembership(projectId, userId);

    List<Map<String, Object>> results = new ArrayList<>();
    for (int i = 0; i < requests.size(); i++) {
      BatchVariableSaveRequest req = requests.get(i);
      try {
        if ("ROLE_VARIABLE".equals(req.saveAs())) {
          if (req.roleId() == null) {
            results.add(Map.of("index", i, "success", false, "error", "roleId is required for ROLE_VARIABLE"));
            continue;
          }
          Role role = roleRepository.findById(req.roleId())
              .orElseThrow(() -> new IllegalArgumentException("Role not found: " + req.roleId()));
          if (!role.getProjectId().equals(projectId)) {
            results.add(Map.of("index", i, "success", false, "error", "Role does not belong to this project"));
            continue;
          }
          if (roleVariableRepository.existsByRoleIdAndKey(req.roleId(), req.key())) {
            results.add(Map.of("index", i, "success", false, "error", "Variable '" + req.key() + "' already exists in this Role"));
            continue;
          }
          RoleVariable rv = new RoleVariable();
          rv.setRoleId(req.roleId());
          rv.setKey(req.key());
          rv.setValue(req.value() != null ? req.value() : "");
          rv.setCreatedBy(userId);
          roleVariableRepository.save(rv);
          results.add(Map.of("index", i, "success", true, "key", req.key()));
        } else {
          VariableScope scope = VariableScope.valueOf(req.scope());
          if (variableRepository.existsByScopeAndScopeIdAndKey(scope, projectId, req.key())) {
            results.add(Map.of("index", i, "success", false, "error", "Variable '" + req.key() + "' already exists at " + scope + " level"));
            continue;
          }
          Variable v = new Variable();
          v.setScope(scope);
          v.setScopeId(projectId);
          v.setKey(req.key());
          v.setValue(req.value() != null ? req.value() : "");
          v.setCreatedBy(userId);
          variableRepository.save(v);
          results.add(Map.of("index", i, "success", true, "key", req.key()));
        }
      } catch (Exception e) {
        results.add(Map.of("index", i, "success", false, "error", e.getMessage()));
      }
    }
    return Result.success(results);
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/ansible/variable/controller/VariableDetectionController.java
git commit -m "feat: add VariableDetectionController with scan and batch save endpoints"
```

---

### Task 5: VariableDetectionController integration test

**Files:**
- Create: `backend/src/test/java/com/ansible/variable/controller/VariableDetectionControllerTest.java`

- [ ] **Step 1: Write the integration test**

```java
package com.ansible.variable.controller;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.role.dto.*;
import com.ansible.role.entity.*;
import com.ansible.variable.dto.*;
import com.ansible.role.repository.*;
import com.ansible.user.repository.UserRepository;
import com.ansible.project.repository.*;
import com.ansible.variable.repository.VariableRepository;
import com.ansible.project.entity.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class VariableDetectionControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private TaskRepository taskRepository;
  @Autowired private HandlerRepository handlerRepository;
  @Autowired private TemplateRepository templateRepository;
  @Autowired private RoleVariableRepository roleVariableRepository;
  @Autowired private VariableRepository variableRepository;

  private String token;
  private Long projectId;
  private Long roleId;

  @BeforeEach
  void setUp() {
    // Register user and get token (same pattern as other integration tests)
    var registerReq = Map.of("username", "testuser_" + System.currentTimeMillis(),
        "password", "password123", "displayName", "Test User");
    var authRes = restTemplate.postForEntity("/api/auth/register", registerReq, Map.class);
    token = (String) ((Map) authRes.getBody().get("data")).get("token");

    // Create project
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    var projectReq = Map.of("name", "Test Project " + System.currentTimeMillis(), "description", "test");
    var projectRes = restTemplate.exchange("/api/projects", HttpMethod.POST,
        new HttpEntity<>(projectReq, headers),
        new ParameterizedTypeReference<Result<Map<String, Object>>>() {});
    projectId = ((Number) projectRes.getBody().getData().get("id")).longValue();

    // Create role
    var roleReq = Map.of("name", "Test Role");
    var roleRes = restTemplate.exchange("/api/projects/" + projectId + "/roles", HttpMethod.POST,
        new HttpEntity<>(roleReq, headers),
        new ParameterizedTypeReference<Result<Map<String, Object>>>() {});
    roleId = ((Number) roleRes.getBody().getData().get("id")).longValue();

    // Create a task with variable reference
    var taskReq = Map.of("name", "Start {{ app_port }}",
        "module", "shell",
        "args", "{\"cmd\":\"start --port {{ app_port }}\"}");
    restTemplate.exchange("/api/roles/" + roleId + "/tasks", HttpMethod.POST,
        new HttpEntity<>(taskReq, headers),
        new ParameterizedTypeReference<Result<Map<String, Object>>>() {});
  }

  @AfterEach
  void tearDown() {
    roleVariableRepository.deleteAll();
    variableRepository.deleteAll();
    templateRepository.deleteAll();
    handlerRepository.deleteAll();
    taskRepository.deleteAll();
    roleRepository.deleteAll();
    projectMemberRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void detectVariables_returnsDetectedVars() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    var response = restTemplate.exchange("/api/projects/" + projectId + "/detect-variables",
        HttpMethod.GET, new HttpEntity<>(headers),
        new ParameterizedTypeReference<Result<List<DetectedVariableResponse>>>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<DetectedVariableResponse> vars = response.getBody().getData();
    assertThat(vars).isNotEmpty();
    DetectedVariableResponse appPort = vars.stream()
        .filter(v -> v.key().equals("app_port")).findFirst().orElseThrow();
    assertThat(appPort.suggestedScope()).isEqualTo("ROLE");
  }

  @Test
  void batchSave_savesRoleVariable() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    List<BatchVariableSaveRequest> requests = List.of(
        new BatchVariableSaveRequest("my_var", "ROLE_VARIABLE", null, roleId, "test_value")
    );
    var response = restTemplate.exchange("/api/projects/" + projectId + "/variables/batch",
        HttpMethod.POST, new HttpEntity<>(requests, headers),
        new ParameterizedTypeReference<Result<List<Map<String, Object>>>>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Map<String, Object>> results = response.getBody().getData();
    assertThat(results).hasSize(1);
    assertThat(results.get(0).get("success")).isEqualTo(true);

    // Verify it was saved
    List<RoleVariable> saved = roleVariableRepository.findAllByRoleIdOrderByKeyAsc(roleId);
    assertThat(saved).hasSize(1);
    assertThat(saved.get(0).getKey()).isEqualTo("my_var");
  }

  @Test
  void batchSave_rejectsDuplicateRoleVariable() {
    // Pre-create a variable
    RoleVariable existing = new RoleVariable();
    existing.setRoleId(roleId); existing.setKey("my_var"); existing.setValue("old");
    existing.setCreatedBy(1L);
    roleVariableRepository.save(existing);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    List<BatchVariableSaveRequest> requests = List.of(
        new BatchVariableSaveRequest("my_var", "ROLE_VARIABLE", null, roleId, "new")
    );
    var response = restTemplate.exchange("/api/projects/" + projectId + "/variables/batch",
        HttpMethod.POST, new HttpEntity<>(requests, headers),
        new ParameterizedTypeReference<Result<List<Map<String, Object>>>>() {});

    List<Map<String, Object>> results = response.getBody().getData();
    assertThat(results.get(0).get("success")).isEqualTo(false);
  }
}
```

- [ ] **Step 2: Run integration test to verify**

```bash
cd backend && mvn test -Dtest=VariableDetectionControllerTest -v
```
Expected: All 3 tests PASS

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/ansible/variable/controller/VariableDetectionControllerTest.java
git commit -m "test: add VariableDetectionController integration tests"
```

---

### Task 6: Backend code quality checks

- [ ] **Step 1: Run Spotless formatting**

```bash
cd backend && mvn spotless:apply
```

- [ ] **Step 2: Run Checkstyle**

```bash
cd backend && mvn checkstyle:check
```
Expected: PASS (no violations)

- [ ] **Step 3: Run PMD**

```bash
cd backend && mvn pmd:check
```
Expected: PASS

- [ ] **Step 4: Run SpotBugs**

```bash
cd backend && mvn spotbugs:check
```
Expected: PASS

- [ ] **Step 5: Commit any formatting fixes**

```bash
git add -A && git diff --cached --quiet || git commit -m "style: apply spotless formatting for variable detection"
```

---

### Task 7: Frontend types

**Files:**
- Modify: `frontend/src/types/entity/Variable.ts`

- [ ] **Step 1: Add new interfaces to Variable.ts**

Append to `frontend/src/types/entity/Variable.ts`:

```typescript
export interface VariableOccurrence {
  roleId: number;
  roleName: string;
  type: "TASK" | "HANDLER" | "TEMPLATE";
  entityId: number;
  entityName: string;
  field: string;
}

export interface DetectedVariable {
  key: string;
  occurrences: VariableOccurrence[];
  suggestedScope: "ROLE" | "PROJECT";
}

export interface DetectedVariableRow extends DetectedVariable {
  /** The user-selected scope target */
  scopeType: "project" | "role";
  /** Role ID when scopeType is "role" */
  targetRoleId?: number;
  /** Value filled by user */
  userValue: string;
  /** Unique key for table row */
  rowKey: string;
}

export interface BatchVariableSaveItem {
  key: string;
  saveAs: "VARIABLE" | "ROLE_VARIABLE";
  scope?: string;
  roleId?: number;
  value?: string;
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/types/entity/Variable.ts
git commit -m "feat: add frontend types for variable detection"
```

---

### Task 8: Frontend API client

**Files:**
- Modify: `frontend/src/api/variable.ts`

- [ ] **Step 1: Add API functions**

Append to `frontend/src/api/variable.ts`:

```typescript
import type { DetectedVariable, BatchVariableSaveItem } from "../types/entity/Variable";

export async function detectVariables(projectId: number): Promise<DetectedVariable[]> {
  const res = await request.get<DetectedVariable[]>(`/projects/${projectId}/detect-variables`);
  return res.data;
}

export async function batchSaveVariables(
  projectId: number,
  items: BatchVariableSaveItem[]
): Promise<Array<{ index: number; success: boolean; key?: string; error?: string }>> {
  const res = await request.post<
    Array<{ index: number; success: boolean; key?: string; error?: string }>
  >(`/projects/${projectId}/variables/batch`, items);
  return res.data;
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/api/variable.ts
git commit -m "feat: add detectVariables and batchSaveVariables API functions"
```

---

### Task 9: VariableManager — button and result table

**Files:**
- Modify: `frontend/src/pages/variable/VariableManager.tsx`

- [ ] **Step 1: Add imports**

After line 18 (`PlusOutlined`), add `ScanOutlined`:
```typescript
import {
  PlusOutlined,
  TableOutlined,
  ApartmentOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  ScanOutlined,
  CopyOutlined,
  DeleteOutlined,
} from "@ant-design/icons";
```

Add import for the API functions (modify line 28):
```typescript
import { listVariables, createVariable, updateVariable, deleteVariable, detectVariables, batchSaveVariables } from "../../api/variable";
```

Add import for new types (modify line 26):
```typescript
import type { Variable, VariableScope, CreateVariableRequest, DetectedVariable, DetectedVariableRow, BatchVariableSaveItem } from "../../types/entity/Variable";
```

Add import for `Tooltip` and `Tag` from antd (modify line 3):
```typescript
import {
  Button, Table, Modal, Form, Input, Select, message, Popconfirm,
  Space, Empty, Segmented, Tree, Card, Typography, Tooltip, Tag,
} from "antd";
```

- [ ] **Step 2: Add state variables**

After line 72 (`treeLoading` state), add:
```typescript
// Variable detection state
const [detecting, setDetecting] = useState(false);
const [detectedVars, setDetectedVars] = useState<DetectedVariableRow[]>([]);
const [savingVars, setSavingVars] = useState(false);
```

After the `scopeLabels` constant (line 40-43), add:
```typescript
interface CopyModalState {
  open: boolean;
  sourceVar: DetectedVariableRow | null;
  targetType: "project" | "role";
  targetRoleId?: number;
}
```

And after the `savingVars` state, add:
```typescript
const [copyModal, setCopyModal] = useState<CopyModalState>({
  open: false, sourceVar: null, targetType: "role",
});
```

- [ ] **Step 3: Add handler functions**

After `handleDelete` (line ~381), add:

```typescript
const handleDetect = useCallback(async () => {
  if (!pid) return;
  setDetecting(true);
  try {
    const vars = await detectVariables(pid);
    const rows: DetectedVariableRow[] = vars.map((v) => {
      const scopeType = v.suggestedScope === "PROJECT" ? "project" : "role";
      const firstOccurrence = v.occurrences[0];
      return {
        ...v,
        scopeType,
        targetRoleId: scopeType === "role" ? firstOccurrence.roleId : undefined,
        userValue: "",
        rowKey: v.key + "-" + (scopeType === "role" ? firstOccurrence.roleId : "proj"),
      };
    });
    setDetectedVars(rows);
    message.success(`检测到 ${rows.length} 个未注册变量`);
  } catch {
    message.error("变量探测失败");
    setDetectedVars([]);
  } finally {
    setDetecting(false);
  }
}, [pid]);

const updateRowScope = useCallback((rowKey: string, scopeType: "project" | "role", targetRoleId?: number) => {
  setDetectedVars((prev) =>
    prev.map((r) => (r.rowKey === rowKey ? { ...r, scopeType, targetRoleId } : r))
  );
}, []);

const updateRowValue = useCallback((rowKey: string, value: string) => {
  setDetectedVars((prev) =>
    prev.map((r) => (r.rowKey === rowKey ? { ...r, userValue: value } : r))
  );
}, []);

const deleteRow = useCallback((rowKey: string) => {
  setDetectedVars((prev) => prev.filter((r) => r.rowKey !== rowKey));
}, []);

const handleCopy = useCallback((record: DetectedVariableRow) => {
  setCopyModal({ open: true, sourceVar: record, targetType: record.scopeType === "project" ? "role" : "role" });
}, []);

const confirmCopy = useCallback(() => {
  if (!copyModal.sourceVar) return;
  const src = copyModal.sourceVar;
  const newRowKey = src.key + "-" + copyModal.targetType + "-" + (copyModal.targetRoleId ?? "proj") + "-" + Date.now();
  const newRow: DetectedVariableRow = {
    ...src,
    scopeType: copyModal.targetType,
    targetRoleId: copyModal.targetType === "role" ? copyModal.targetRoleId : undefined,
    rowKey: newRowKey,
  };
  setDetectedVars((prev) => [...prev, newRow]);
  setCopyModal({ open: false, sourceVar: null, targetType: "role" });
}, [copyModal]);

const handleBatchSave = useCallback(async () => {
  if (!pid) return;
  // Frontend validation: check duplicates within batch
  const seen = new Map<string, string>(); // "scopeType:targetId:key" -> rowKey
  const errors: string[] = [];
  for (const row of detectedVars) {
    const id = row.scopeType === "project"
      ? `project:${pid}:${row.key}`
      : `role:${row.targetRoleId}:${row.key}`;
    if (seen.has(id)) {
      errors.push(`变量 "${row.key}" 重复`);
    }
    seen.set(id, row.rowKey);
  }
  if (errors.length > 0) {
    message.error(errors.join(", "));
    return;
  }

  setSavingVars(true);
  try {
    const items: BatchVariableSaveItem[] = detectedVars.map((row) => ({
      key: row.key,
      saveAs: row.scopeType === "project" ? "VARIABLE" : "ROLE_VARIABLE",
      scope: row.scopeType === "project" ? "PROJECT" : undefined,
      roleId: row.scopeType === "role" ? row.targetRoleId : undefined,
      value: row.userValue || undefined,
    }));

    const results = await batchSaveVariables(pid, items);
    const failed = results.filter((r) => !r.success);
    const succeeded = results.filter((r) => r.success);

    if (succeeded.length > 0) {
      message.success(`成功保存 ${succeeded.length} 个变量`);
      setDetectedVars((prev) =>
        prev.filter((_, i) => results[i]?.success)
      );
      fetchVariables();
    }
    if (failed.length > 0) {
      const msgs = failed.map((f) => f.error).join("; ");
      message.error(`保存失败: ${msgs}`);
    }
  } catch {
    message.error("批量保存失败");
  } finally {
    setSavingVars(false);
  }
}, [pid, detectedVars, fetchVariables]);
```

- [ ] **Step 4: Add "变量探测" button next to "新建变量" button**

After line 493 (`<Button type="primary" ...>新建变量</Button>`), add:
```tsx
<Button
  icon={<ScanOutlined />}
  onClick={handleDetect}
  loading={detecting}
  style={{ borderStyle: "dashed" }}
>
  变量探测
</Button>
```

- [ ] **Step 5: Add result table after the control bar and before the Table/Tree View**

After line 494 (`</div>` closing the control bar), add:
```tsx
{detectedVars.length > 0 && (
  <Card
    size="small"
    title={
      <Space>
        <span>探测结果</span>
        <Tag color="blue">{detectedVars.length} 个变量</Tag>
      </Space>
    }
    extra={
      <Space>
        <Button onClick={() => setDetectedVars([])}>取消</Button>
        <Button type="primary" onClick={handleBatchSave} loading={savingVars}>
          批量保存 ({detectedVars.length}条)
        </Button>
      </Space>
    }
    style={{ marginBottom: 16 }}
  >
    <Table
      rowKey="rowKey"
      dataSource={detectedVars}
      pagination={false}
      size="small"
      columns={[
        {
          title: "变量名",
          dataIndex: "key",
          width: 180,
          render: (key: string) => <code style={{ background: "#f5f5f5", padding: "2px 6px", borderRadius: 4 }}>{key}</code>,
        },
        {
          title: "来源",
          dataIndex: "occurrences",
          width: 300,
          render: (occurrences: DetectedVariableRow["occurrences"]) => (
            <div>
              {occurrences.map((o, i) => (
                <div key={i} style={{ fontSize: 12, color: "#666" }}>
                  {o.roleName} · {o.type === "TASK" ? "任务" : o.type === "HANDLER" ? "Handler" : "模板"} · {o.entityName}
                </div>
              ))}
            </div>
          ),
        },
        {
          title: "作用域",
          width: 220,
          render: (_: unknown, record: DetectedVariableRow) => (
            <Space>
              <Select
                value={record.scopeType}
                onChange={(val) => updateRowScope(record.rowKey, val, val === "role" ? record.occurrences[0]?.roleId : undefined)}
                style={{ width: 100 }}
                size="small"
                options={[
                  { label: "项目级", value: "project" },
                  { label: "Role 级", value: "role" },
                ]}
              />
              {record.scopeType === "role" && (
                <Select
                  value={record.targetRoleId}
                  onChange={(val) => updateRowScope(record.rowKey, "role", val)}
                  style={{ width: 100 }}
                  size="small"
                  options={Array.from(new Map(
                    record.occurrences.map((o) => [o.roleId, o.roleName])
                  )).map(([id, name]) => ({ label: name, value: id }))}
                />
              )}
            </Space>
          ),
        },
        {
          title: "值（可选）",
          dataIndex: "userValue",
          width: 160,
          render: (_: unknown, record: DetectedVariableRow) => (
            <Input
              size="small"
              placeholder="输入变量值"
              value={record.userValue}
              onChange={(e) => updateRowValue(record.rowKey, e.target.value)}
            />
          ),
        },
        {
          title: "操作",
          width: 100,
          render: (_: unknown, record: DetectedVariableRow) => (
            <Space>
              <Tooltip title="复制">
                <Button type="text" size="small" icon={<CopyOutlined />} onClick={() => handleCopy(record)} />
              </Tooltip>
              <Tooltip title="删除">
                <Button type="text" size="small" danger icon={<DeleteOutlined />} onClick={() => deleteRow(record.rowKey)} />
              </Tooltip>
            </Space>
          ),
        },
      ]}
    />
  </Card>
)}
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/variable/VariableManager.tsx
git commit -m "feat: add variable detection button, result table, and batch save to VariableManager"
```

---

### Task 10: Copy modal

**Files:**
- Modify: `frontend/src/pages/variable/VariableManager.tsx`

- [ ] **Step 1: Add the copy modal JSX**

After the existing `<Modal ...>` for create/edit (before closing `</div>` of the component, after line 561), add:

```tsx
<Modal
  title="复制变量"
  open={copyModal.open}
  onOk={confirmCopy}
  onCancel={() => setCopyModal({ open: false, sourceVar: null, targetType: "role" })}
  width={400}
>
  {copyModal.sourceVar && (
    <div>
      <p style={{ marginBottom: 12 }}>
        将 <code>{copyModal.sourceVar.key}</code> 复制为：
      </p>
      <Radio.Group
        value={copyModal.targetType}
        onChange={(e) => setCopyModal({ ...copyModal, targetType: e.target.value })}
      >
        <Space direction="vertical">
          <Radio value="role">
            Role 级
            {copyModal.targetType === "role" && (
              <Select
                value={copyModal.targetRoleId}
                onChange={(val) => setCopyModal({ ...copyModal, targetRoleId: val })}
                style={{ width: 140, marginLeft: 8 }}
                size="small"
                options={Array.from(new Map(
                  copyModal.sourceVar.occurrences.map((o) => [o.roleId, o.roleName])
                )).map(([id, name]) => ({ label: name, value: id }))}
              />
            )}
          </Radio>
          {copyModal.sourceVar.scopeType === "role" && (
            <Radio value="project">项目级</Radio>
          )}
        </Space>
      </Radio.Group>
    </div>
  )}
</Modal>
```

Add `Radio` to the antd import:
```typescript
import {
  Button, Table, Modal, Form, Input, Select, message, Popconfirm,
  Space, Empty, Segmented, Tree, Card, Typography, Tooltip, Tag, Radio,
} from "antd";
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/pages/variable/VariableManager.tsx
git commit -m "feat: add copy modal for variable detection results"
```

---

### Task 11: Frontend lint and format

- [ ] **Step 1: Run ESLint**

```bash
cd frontend && npm run lint
```
Expected: PASS (no errors)

- [ ] **Step 2: Run Prettier**

```bash
cd frontend && npm run format
```

- [ ] **Step 3: Run TypeScript check**

```bash
cd frontend && npx tsc --noEmit
```
Expected: No type errors

- [ ] **Step 4: Commit any formatting fixes**

```bash
git add -A && git diff --cached --quiet || git commit -m "style: apply frontend formatting for variable detection"
```

---

### Task 12: End-to-end verification

- [ ] **Step 1: Start backend**

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
Wait until "Started" appears in logs.

- [ ] **Step 2: Start frontend**

```bash
cd frontend && npm run dev
```

- [ ] **Step 3: Manual smoke test**
  - Open browser to `http://localhost:5173`
  - Log in, navigate to a project with Roles/Tasks/Templates
  - Go to 变量管理 page
  - Click【变量探测】button
  - Verify detected variables appear in table
  - Try changing scope from project to role level, verify role selector appears
  - Try copying a variable row
  - Try deleting a row
  - Click【批量保存】and verify variables appear in the regular table
  - Verify duplicate prevention: try saving same variable twice

- [ ] **Step 4: Stop servers**

```bash
# Ctrl+C on both dev servers
```

- [ ] **Step 5: Final commit (if any fixes)**

```bash
git add -A && git diff --cached --quiet || git commit -m "fix: end-to-end adjustments for variable detection"
```
