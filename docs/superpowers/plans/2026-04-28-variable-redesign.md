# Variable Management Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Unify the two separate variable systems (general scoped variables + role variables) into a single table, single API, single frontend page.

**Architecture:** Merge `variables`, `role_variables`, and `role_default_variables` tables into one `variables` table with a 5-value scope enum (`PROJECT`, `HOSTGROUP`, `ENVIRONMENT`, `ROLE_VARS`, `ROLE_DEFAULTS`). Remove all role-specific variable code. Update consumers (PlaybookService, ProjectCleanupService). Simplify frontend to one unified variable page.

**Tech Stack:** Spring Boot 3.x / Java 21, React 18 / TypeScript / Ant Design 5, Vitest, Mockito

---

## File Structure

### Backend — Modified

| File | Change |
|------|--------|
| `variable/entity/VariableScope.java` | Add `ROLE_VARS`, `ROLE_DEFAULTS` enum values |
| `variable/entity/Variable.java` | Expand key length to 200 |
| `variable/repository/VariableRepository.java` | Add `findByScopeAndScopeIdIn` query |
| `variable/service/VariableService.java` | Handle ROLE_VARS/ROLE_DEFAULTS in resolveProjectId, add RoleRepository dependency |
| `variable/dto/CreateVariableRequest.java` | Expand key size to 200 |
| `variable/dto/UpdateVariableRequest.java` | Expand key size to 200 |
| `variable/dto/BatchVariableSaveRequest.java` | Replace `saveAs`/`scope`/`roleId` with unified `scope`/`scopeId` |
| `variable/controller/VariableDetectionController.java` | Simplify batch save to use unified Variable |
| `variable/controller/VariableController.java` | Make `scope` param optional |
| `playbook/service/PlaybookService.java` | Use unified VariableRepository for role vars/defaults |
| `project/service/ProjectCleanupService.java` | Use unified VariableRepository for role cleanup |

### Backend — Deleted

| File | Reason |
|------|--------|
| `role/entity/RoleVariable.java` | Merged into Variable |
| `role/entity/RoleDefaultVariable.java` | Merged into Variable |
| `role/repository/RoleVariableRepository.java` | No longer needed |
| `role/repository/RoleDefaultVariableRepository.java` | No longer needed |
| `role/service/RoleVariableService.java` | Merged into VariableService |
| `role/service/RoleDefaultVariableService.java` | Merged into VariableService |
| `role/controller/RoleVariableController.java` | API removed |
| `role/controller/RoleDefaultVariableController.java` | API removed |
| `role/dto/CreateRoleVariableRequest.java` | No longer needed |
| `role/dto/UpdateRoleVariableRequest.java` | No longer needed |
| `role/dto/RoleVariableResponse.java` | No longer needed |
| `role/dto/CreateRoleDefaultVariableRequest.java` | No longer needed |
| `role/dto/UpdateRoleDefaultVariableRequest.java` | No longer needed |
| `role/dto/RoleDefaultVariableResponse.java` | No longer needed |

### Backend Tests — Modified

| File | Change |
|------|--------|
| `variable/service/VariableServiceTest.java` | Add tests for ROLE_VARS/ROLE_DEFAULTS scope |
| `variable/controller/VariableControllerTest.java` | Add tests for role scope variables |
| `variable/service/VariableDetectionServiceTest.java` | Minimal changes (detection logic unchanged) |
| `variable/controller/VariableDetectionControllerTest.java` | Update batch save to use new DTO |

### Backend Tests — Deleted

| File | Reason |
|------|--------|
| `role/service/RoleVariableServiceTest.java` | Service removed |
| `role/service/RoleDefaultVariableServiceTest.java` | Service removed |
| `role/controller/RoleVariableControllerTest.java` | Controller removed |
| `role/controller/RoleDefaultVariableControllerTest.java` | Controller removed |

### Frontend — Modified

| File | Change |
|------|--------|
| `types/entity/Variable.ts` | Expand VariableScope to 5 values, update BatchVariableSaveItem |
| `api/variable.ts` | No API path changes needed (endpoints stay the same) |
| `utils/variablePriority.ts` | Adjust priority ranks to 4-3-2-1-0 |
| `pages/variable/VariableManager.tsx` | Major rewrite: remove role variable API calls, use unified scope |
| `pages/role/RoleDetail.tsx` | Remove Vars and Defaults tabs |

### Frontend — Deleted

| File | Reason |
|------|--------|
| `types/entity/RoleVariable.ts` | Merged into Variable.ts |
| `api/roleVariable.ts` | API removed |
| `pages/role/RoleVars.tsx` | Replaced by unified VariableManager |
| `pages/role/RoleDefaults.tsx` | Replaced by unified VariableManager |

### Frontend Tests — Modified

| File | Change |
|------|--------|
| `api/variable.test.ts` | Update to use expanded scope |
| `utils/__tests__/variablePriority.test.ts` | Update priority ranks |

### Frontend Tests — Deleted

| File | Reason |
|------|--------|
| `pages/role/__tests__/RoleVars.test.tsx` | Component removed |
| `pages/role/__tests__/RoleDefaults.test.tsx` | Component removed |

---

## Task 1: Backend — Expand VariableScope enum and Variable entity

**Files:**
- Modify: `backend/src/main/java/com/ansible/variable/entity/VariableScope.java`
- Modify: `backend/src/main/java/com/ansible/variable/entity/Variable.java`
- Modify: `backend/src/main/java/com/ansible/variable/dto/CreateVariableRequest.java`
- Modify: `backend/src/main/java/com/ansible/variable/dto/UpdateVariableRequest.java`

- [ ] **Step 1: Update VariableScope enum**

Replace `backend/src/main/java/com/ansible/variable/entity/VariableScope.java` with:

```java
package com.ansible.variable.entity;

public enum VariableScope {
    PROJECT,
    HOSTGROUP,
    ENVIRONMENT,
    ROLE_VARS,
    ROLE_DEFAULTS
}
```

- [ ] **Step 2: Update Variable entity key length**

In `backend/src/main/java/com/ansible/variable/entity/Variable.java`, change line 30 from:

```java
    @Column(nullable = false, length = 100)
```

to:

```java
    @Column(nullable = false, length = 200)
```

- [ ] **Step 3: Update CreateVariableRequest key size**

In `backend/src/main/java/com/ansible/variable/dto/CreateVariableRequest.java`, change line 11 from:

```java
      @NotBlank @Size(max = 100) String key,
```

to:

```java
      @NotBlank @Size(max = 200) String key,
```

- [ ] **Step 4: Update UpdateVariableRequest key size**

In `backend/src/main/java/com/ansible/variable/dto/UpdateVariableRequest.java`, change line 5 from:

```java
public record UpdateVariableRequest(@NotBlank @Size(max = 100) String key, String value) { }
```

to:

```java
public record UpdateVariableRequest(@NotBlank @Size(max = 200) String key, String value) { }
```

- [ ] **Step 5: Compile to verify no breakage**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```
feat(variable): expand VariableScope to 5 values and increase key length to 200
```

---

## Task 2: Backend — Update VariableRepository, VariableService, and VariableDetectionService

**Files:**
- Modify: `backend/src/main/java/com/ansible/variable/repository/VariableRepository.java`
- Modify: `backend/src/main/java/com/ansible/variable/service/VariableService.java`
- Modify: `backend/src/main/java/com/ansible/variable/service/VariableDetectionService.java`

- [ ] **Step 1: Update VariableDetectionService suggestedScope**

In `backend/src/main/java/com/ansible/variable/service/VariableDetectionService.java`, line 64, change:

```java
            String suggestedScope = distinctRoles.size() == 1 ? "ROLE" : "PROJECT";
```

to:

```java
            String suggestedScope = distinctRoles.size() == 1 ? "ROLE_VARS" : "PROJECT";
```

This aligns the backend suggestion with the new 5-value scope enum.

- [ ] **Step 2: Write failing tests for ROLE_VARS/ROLE_DEFAULTS in VariableServiceTest**

In `backend/src/test/java/com/ansible/variable/service/VariableServiceTest.java`, add these test methods:

```java
import com.ansible.role.entity.Role;
import com.ansible.role.repository.RoleRepository;

// Add to the test class fields:
@Mock private RoleRepository roleRepository;

// Update @InjectMocks to include the new dependency if needed - VariableService already has RoleRepository now

@Test
void createVariable_roleVarsScope_success() {
    when(accessChecker.checkMembership(anyLong(), anyLong())).thenReturn(true);
    when(variableRepository.existsByScopeAndScopeIdAndKey(
        VariableScope.ROLE_VARS, 1L, "app_port")).thenReturn(false);
    when(variableRepository.save(any(Variable.class))).thenAnswer(inv -> {
        Variable v = inv.getArgument(0);
        v.setId(10L);
        return v;
    });

    CreateVariableRequest req = new CreateVariableRequest(VariableScope.ROLE_VARS, 1L, "app_port", "8080");
    VariableResponse resp = variableService.createVariable(1L, req, 1L);

    assertThat(resp.key()).isEqualTo("app_port");
    verify(variableRepository).save(argThat(v ->
        v.getScope() == VariableScope.ROLE_VARS
        && v.getScopeId().equals(1L)
        && v.getKey().equals("app_port")));
}

@Test
void getVariable_roleVarsScope_resolvesProjectId() {
    Variable v = new Variable();
    v.setId(5L);
    v.setScope(VariableScope.ROLE_VARS);
    v.setScopeId(10L);
    v.setKey("db_port");
    v.setValue("3306");
    v.setCreatedBy(1L);

    Role role = new Role();
    role.setId(10L);
    role.setProjectId(1L);

    when(variableRepository.findById(5L)).thenReturn(Optional.of(v));
    when(roleRepository.findById(10L)).thenReturn(Optional.of(role));
    when(accessChecker.checkMembership(1L, 1L)).thenReturn(true);

    VariableResponse resp = variableService.getVariable(5L, 1L);
    assertThat(resp.scope()).isEqualTo(VariableScope.ROLE_VARS);
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `cd backend && mvn test -Dtest=VariableServiceTest -pl . -q 2>&1 | tail -20`
Expected: Compilation error or test failure (VariableService doesn't handle ROLE_VARS yet)

- [ ] **Step 4: Update VariableRepository**

Replace `backend/src/main/java/com/ansible/variable/repository/VariableRepository.java` with:

```java
package com.ansible.variable.repository;

import com.ansible.variable.entity.Variable;
import com.ansible.variable.entity.VariableScope;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VariableRepository extends JpaRepository<Variable, Long> {

    List<Variable> findByScopeAndScopeIdOrderByIdAsc(VariableScope scope, Long scopeId);

    List<Variable> findByScopeAndScopeIdInOrderByIdAsc(VariableScope scope, List<Long> scopeIds);

    List<Variable> findByScopeOrderByIdAsc(VariableScope scope);

    boolean existsByScopeAndScopeIdAndKey(VariableScope scope, Long scopeId, String key);

    boolean existsByScopeAndScopeIdAndKeyAndIdNot(
        VariableScope scope, Long scopeId, String key, Long id);

    void deleteByScopeAndScopeId(VariableScope scope, Long scopeId);
}
```

- [ ] **Step 5: Update VariableService to handle ROLE_VARS/ROLE_DEFAULTS**

Replace `backend/src/main/java/com/ansible/variable/service/VariableService.java` with:

```java
package com.ansible.variable.service;

import com.ansible.environment.repository.EnvironmentRepository;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.role.repository.RoleRepository;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.variable.dto.CreateVariableRequest;
import com.ansible.variable.dto.UpdateVariableRequest;
import com.ansible.variable.dto.VariableResponse;
import com.ansible.variable.entity.Variable;
import com.ansible.variable.entity.VariableScope;
import com.ansible.variable.repository.VariableRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VariableService {

    private final VariableRepository variableRepository;
    private final ProjectAccessChecker accessChecker;
    private final HostGroupRepository hostGroupRepository;
    private final EnvironmentRepository environmentRepository;
    private final RoleRepository roleRepository;

    private Long resolveProjectId(Variable v) {
        return switch (v.getScope()) {
            case PROJECT -> v.getScopeId();
            case HOSTGROUP -> hostGroupRepository
                .findById(v.getScopeId())
                .orElseThrow(() -> new IllegalArgumentException("Host group not found"))
                .getProjectId();
            case ENVIRONMENT -> environmentRepository
                .findById(v.getScopeId())
                .orElseThrow(() -> new IllegalArgumentException("Environment not found"))
                .getProjectId();
            case ROLE_VARS, ROLE_DEFAULTS -> roleRepository
                .findById(v.getScopeId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found"))
                .getProjectId();
        };
    }

    @Transactional
    public VariableResponse createVariable(
        Long projectId, CreateVariableRequest request, Long userId) {
        accessChecker.checkMembership(projectId, userId);
        if (variableRepository.existsByScopeAndScopeIdAndKey(
            request.scope(), request.scopeId(), request.key())) {
            throw new IllegalArgumentException(
                "Variable '" + request.key() + "' already exists in this scope");
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
    public List<VariableResponse> listVariables(
        Long projectId, VariableScope scope, Long scopeId, Long userId) {
        accessChecker.checkMembership(projectId, userId);
        if (scopeId != null) {
            return variableRepository.findByScopeAndScopeIdOrderByIdAsc(scope, scopeId).stream()
                .map(this::toResponse)
                .toList();
        }
        return variableRepository.findByScopeOrderByIdAsc(scope).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public VariableResponse getVariable(Long varId, Long userId) {
        Variable v =
            variableRepository
                .findById(varId)
                .orElseThrow(() -> new IllegalArgumentException("Variable not found"));
        accessChecker.checkMembership(resolveProjectId(v), userId);
        return toResponse(v);
    }

    @Transactional
    public VariableResponse updateVariable(
        Long varId, UpdateVariableRequest request, Long userId) {
        Variable v =
            variableRepository
                .findById(varId)
                .orElseThrow(() -> new IllegalArgumentException("Variable not found"));
        accessChecker.checkOwnerOrAdmin(resolveProjectId(v), v.getCreatedBy(), userId);
        if (!v.getKey().equals(request.key())
            && variableRepository.existsByScopeAndScopeIdAndKeyAndIdNot(
                v.getScope(), v.getScopeId(), request.key(), varId)) {
            throw new IllegalArgumentException(
                "Variable '" + request.key() + "' already exists in this scope");
        }
        v.setKey(request.key());
        v.setValue(request.value());
        return toResponse(variableRepository.save(v));
    }

    @Transactional
    public void deleteVariable(Long varId, Long userId) {
        Variable v =
            variableRepository
                .findById(varId)
                .orElseThrow(() -> new IllegalArgumentException("Variable not found"));
        accessChecker.checkOwnerOrAdmin(resolveProjectId(v), v.getCreatedBy(), userId);
        variableRepository.delete(v);
    }

    private VariableResponse toResponse(Variable v) {
        return new VariableResponse(
            v.getId(),
            v.getScope(),
            v.getScopeId(),
            v.getKey(),
            v.getValue(),
            v.getCreatedAt(),
            v.getUpdatedAt());
    }
}
```

Key changes: Added `RoleRepository` dependency, expanded `resolveProjectId` switch to handle `ROLE_VARS` and `ROLE_DEFAULTS`.

- [ ] **Step 6: Update VariableServiceTest to add RoleRepository mock**

In the test file, add `@Mock private RoleRepository roleRepository;` to the test class fields (if not already present). Ensure the test class has all mocks needed for the new dependency.

- [ ] **Step 7: Run VariableServiceTest**

Run: `cd backend && mvn test -Dtest=VariableServiceTest -q`
Expected: All tests PASS

- [ ] **Step 8: Update VariableDetectionServiceTest**

In `backend/src/test/java/com/ansible/variable/service/VariableDetectionServiceTest.java`, update the test that checks `suggestedScope` — change assertions expecting `"ROLE"` to expect `"ROLE_VARS"`.

Run: `cd backend && mvn test -Dtest=VariableDetectionServiceTest -q`
Expected: All tests PASS

- [ ] **Step 9: Commit**

```
feat(variable): add ROLE_VARS and ROLE_DEFAULTS scope support to VariableService
```

---

## Task 3: Backend — Update BatchVariableSaveRequest and VariableDetectionController

**Files:**
- Modify: `backend/src/main/java/com/ansible/variable/dto/BatchVariableSaveRequest.java`
- Modify: `backend/src/main/java/com/ansible/variable/controller/VariableDetectionController.java`

- [ ] **Step 1: Update BatchVariableSaveRequest DTO**

Replace `backend/src/main/java/com/ansible/variable/dto/BatchVariableSaveRequest.java` with:

```java
package com.ansible.variable.dto;

import com.ansible.variable.entity.VariableScope;

public record BatchVariableSaveRequest(
    String key,
    VariableScope scope,
    Long scopeId,
    String value
) {}
```

- [ ] **Step 2: Update VariableDetectionController to use unified Variable**

Replace `backend/src/main/java/com/ansible/variable/controller/VariableDetectionController.java` with:

```java
package com.ansible.variable.controller;

import com.ansible.common.Result;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.variable.dto.BatchVariableSaveRequest;
import com.ansible.variable.dto.DetectedVariableResponse;
import com.ansible.variable.entity.Variable;
import com.ansible.variable.repository.VariableRepository;
import com.ansible.variable.service.VariableDetectionService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VariableDetectionController {

    private final VariableDetectionService detectionService;
    private final VariableRepository variableRepository;
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
            results.add(saveOneItem(projectId, userId, i, requests.get(i)));
        }
        return Result.success(results);
    }

    private Map<String, Object> saveOneItem(
        Long projectId, Long userId, int index, BatchVariableSaveRequest req) {
        try {
            Long scopeId = resolveScopeId(projectId, req);
            if (variableRepository.existsByScopeAndScopeIdAndKey(req.scope(), scopeId, req.key())) {
                return errorResult(index,
                    "Variable '" + req.key() + "' already exists in " + req.scope() + " scope");
            }
            Variable v = new Variable();
            v.setScope(req.scope());
            v.setScopeId(scopeId);
            v.setKey(req.key());
            v.setValue(req.value() != null ? req.value() : "");
            v.setCreatedBy(userId);
            variableRepository.save(v);
            return Map.of("index", index, "success", true, "key", req.key());
        } catch (IllegalArgumentException e) {
            return Map.of("index", index, "success", false, "error", e.getMessage());
        }
    }

    private Long resolveScopeId(Long projectId, BatchVariableSaveRequest req) {
        if (req.scopeId() != null) {
            return req.scopeId();
        }
        if (req.scope() == com.ansible.variable.entity.VariableScope.PROJECT) {
            return projectId;
        }
        throw new IllegalArgumentException("scopeId is required for scope: " + req.scope());
    }

    private static Map<String, Object> errorResult(int index, String message) {
        return Map.of("index", index, "success", false, "error", message);
    }
}
```

Key changes: Removed `RoleVariableRepository`, `RoleRepository`, `RoleVariable` imports. Simplified batch save to always create `Variable` entities with the specified `scope`.

- [ ] **Step 3: Compile to verify**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Update and run VariableDetectionControllerTest**

Update the integration test to use the new DTO format. In `backend/src/test/java/com/ansible/variable/controller/VariableDetectionControllerTest.java`:

- In `batchSave_savesRoleVariable`: Change the request body to use `"scope": "ROLE_VARS"` and `"scopeId": roleId` instead of `"saveAs": "ROLE_VARIABLE"` and `"roleId": roleId`.
- In `batchSave_rejectsDuplicateRoleVariable`: Same change.

The test should now save a `Variable` with `scope=ROLE_VARS` and `scopeId=roleId` into the `variables` table instead of `role_variables`.

Run: `cd backend && mvn test -Dtest=VariableDetectionControllerTest -q`
Expected: All tests PASS

- [ ] **Step 5: Commit**

```
refactor(variable): simplify batch save to use unified Variable entity
```

---

## Task 4: Backend — Update VariableController to make scope optional

**Files:**
- Modify: `backend/src/main/java/com/ansible/variable/controller/VariableController.java`
- Modify: `backend/src/main/java/com/ansible/variable/service/VariableService.java`

- [ ] **Step 1: Make scope query parameter optional in VariableController**

In `backend/src/main/java/com/ansible/variable/controller/VariableController.java`, change the `listVariables` method (line 40-48):

From:
```java
    @GetMapping("/projects/{projectId}/variables")
    public Result<List<VariableResponse>> listVariables(
        @PathVariable Long projectId,
        @RequestParam VariableScope scope,
        @RequestParam(required = false) Long scopeId,
        @AuthenticationPrincipal UserDetails userDetails) {
```

To:
```java
    @GetMapping("/projects/{projectId}/variables")
    public Result<List<VariableResponse>> listVariables(
        @PathVariable Long projectId,
        @RequestParam(required = false) VariableScope scope,
        @RequestParam(required = false) Long scopeId,
        @AuthenticationPrincipal UserDetails userDetails) {
```

Also update the call to pass nullable scope:
```java
        return Result.success(variableService.listVariables(projectId, scope, scopeId, currentUserId));
```

- [ ] **Step 2: Update VariableService.listVariables to handle null scope**

In `backend/src/main/java/com/ansible/variable/service/VariableService.java`, update the `listVariables` method:

From:
```java
    @Transactional(readOnly = true)
    public List<VariableResponse> listVariables(
        Long projectId, VariableScope scope, Long scopeId, Long userId) {
        accessChecker.checkMembership(projectId, userId);
        if (scopeId != null) {
            return variableRepository.findByScopeAndScopeIdOrderByIdAsc(scope, scopeId).stream()
                .map(this::toResponse)
                .toList();
        }
        return variableRepository.findByScopeOrderByIdAsc(scope).stream()
            .map(this::toResponse)
            .toList();
    }
```

To:
```java
    @Transactional(readOnly = true)
    public List<VariableResponse> listVariables(
        Long projectId, VariableScope scope, Long scopeId, Long userId) {
        accessChecker.checkMembership(projectId, userId);
        if (scope == null) {
            return variableRepository.findAll().stream()
                .filter(v -> belongsToProject(v, projectId))
                .map(this::toResponse)
                .toList();
        }
        if (scopeId != null) {
            return variableRepository.findByScopeAndScopeIdOrderByIdAsc(scope, scopeId).stream()
                .map(this::toResponse)
                .toList();
        }
        return variableRepository.findByScopeOrderByIdAsc(scope).stream()
            .map(this::toResponse)
            .toList();
    }

    private boolean belongsToProject(Variable v, Long projectId) {
        return switch (v.getScope()) {
            case PROJECT -> v.getScopeId().equals(projectId);
            case HOSTGROUP -> hostGroupRepository.findById(v.getScopeId())
                .map(hg -> hg.getProjectId().equals(projectId))
                .orElse(false);
            case ENVIRONMENT -> environmentRepository.findById(v.getScopeId())
                .map(env -> env.getProjectId().equals(projectId))
                .orElse(false);
            case ROLE_VARS, ROLE_DEFAULTS -> roleRepository.findById(v.getScopeId())
                .map(role -> role.getProjectId().equals(projectId))
                .orElse(false);
        };
    }
```

Note: The `findAll().stream().filter()` approach is simple and correct for the internal-team single-tenant use case. Performance is acceptable because variable counts are small.

- [ ] **Step 3: Run tests**

Run: `cd backend && mvn test -Dtest=VariableServiceTest -q`
Expected: All tests PASS

Run: `cd backend && mvn test -Dtest=VariableControllerTest -q`
Expected: All tests PASS

- [ ] **Step 4: Commit**

```
feat(variable): make scope query parameter optional for listing all project variables
```

---

## Task 5: Backend — Update PlaybookService

**Files:**
- Modify: `backend/src/main/java/com/ansible/playbook/service/PlaybookService.java`

- [ ] **Step 1: Update imports**

In `backend/src/main/java/com/ansible/playbook/service/PlaybookService.java`, remove these imports (lines 23-26):

```java
import com.ansible.role.entity.RoleDefaultVariable;
import com.ansible.role.entity.RoleVariable;
import com.ansible.role.repository.RoleDefaultVariableRepository;
import com.ansible.role.repository.RoleVariableRepository;
```

No new imports needed — `VariableRepository` and `VariableScope` are already imported.

- [ ] **Step 2: Remove RoleVariableRepository and RoleDefaultVariableRepository fields**

Remove these field declarations (lines 54-55):

```java
  private final RoleVariableRepository roleVariableRepository;
  private final RoleDefaultVariableRepository roleDefaultVariableRepository;
```

- [ ] **Step 3: Update addRoleDefaults method**

Replace the `addRoleDefaults` method (lines 352-359) with:

```java
  private void addRoleDefaults(Map<String, String> merged, List<PlaybookRole> playbookRoles) {
    for (PlaybookRole pr : playbookRoles) {
      for (Variable v : variableRepository.findByScopeAndScopeIdOrderByIdAsc(
          VariableScope.ROLE_DEFAULTS, pr.getRoleId())) {
        merged.put(v.getKey(), v.getValue());
      }
    }
  }
```

- [ ] **Step 4: Update addRoleVars method**

Replace the `addRoleVars` method (lines 361-367) with:

```java
  private void addRoleVars(Map<String, String> merged, List<PlaybookRole> playbookRoles) {
    for (PlaybookRole pr : playbookRoles) {
      for (Variable v : variableRepository.findByScopeAndScopeIdOrderByIdAsc(
          VariableScope.ROLE_VARS, pr.getRoleId())) {
        merged.put(v.getKey(), v.getValue());
      }
    }
  }
```

- [ ] **Step 5: Compile to verify**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Run PlaybookService tests**

Run: `cd backend && mvn test -Dtest=PlaybookServiceTest -q`
Expected: All tests PASS

- [ ] **Step 7: Commit**

```
refactor(playbook): use unified Variable for role vars and defaults in YAML generation
```

---

## Task 6: Backend — Update ProjectCleanupService

**Files:**
- Modify: `backend/src/main/java/com/ansible/project/service/ProjectCleanupService.java`

- [ ] **Step 1: Update imports**

Remove these imports:
```java
import com.ansible.role.repository.RoleDefaultVariableRepository;
import com.ansible.role.repository.RoleVariableRepository;
```

- [ ] **Step 2: Remove old repository fields**

Remove these field declarations:
```java
  private final RoleVariableRepository roleVariableRepository;
  private final RoleDefaultVariableRepository roleDefaultVariableRepository;
```

- [ ] **Step 3: Update cleanupRoleResources**

In `cleanupRoleResources` (lines 108-118), replace:
```java
    roleVariableRepository.deleteByRoleId(roleId);
    roleDefaultVariableRepository.deleteByRoleId(roleId);
```

with:
```java
    variableRepository.deleteByScopeAndScopeId(VariableScope.ROLE_VARS, roleId);
    variableRepository.deleteByScopeAndScopeId(VariableScope.ROLE_DEFAULTS, roleId);
```

- [ ] **Step 4: Compile to verify**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```
refactor(cleanup): use unified Variable for role variable cleanup
```

---

## Task 7: Backend — Delete old role variable code

**Files:**
- Delete: `backend/src/main/java/com/ansible/role/entity/RoleVariable.java`
- Delete: `backend/src/main/java/com/ansible/role/entity/RoleDefaultVariable.java`
- Delete: `backend/src/main/java/com/ansible/role/repository/RoleVariableRepository.java`
- Delete: `backend/src/main/java/com/ansible/role/repository/RoleDefaultVariableRepository.java`
- Delete: `backend/src/main/java/com/ansible/role/service/RoleVariableService.java`
- Delete: `backend/src/main/java/com/ansible/role/service/RoleDefaultVariableService.java`
- Delete: `backend/src/main/java/com/ansible/role/controller/RoleVariableController.java`
- Delete: `backend/src/main/java/com/ansible/role/controller/RoleDefaultVariableController.java`
- Delete: `backend/src/main/java/com/ansible/role/dto/CreateRoleVariableRequest.java`
- Delete: `backend/src/main/java/com/ansible/role/dto/UpdateRoleVariableRequest.java`
- Delete: `backend/src/main/java/com/ansible/role/dto/RoleVariableResponse.java`
- Delete: `backend/src/main/java/com/ansible/role/dto/CreateRoleDefaultVariableRequest.java`
- Delete: `backend/src/main/java/com/ansible/role/dto/UpdateRoleDefaultVariableRequest.java`
- Delete: `backend/src/main/java/com/ansible/role/dto/RoleDefaultVariableResponse.java`
- Delete: `backend/src/test/java/com/ansible/role/service/RoleVariableServiceTest.java`
- Delete: `backend/src/test/java/com/ansible/role/service/RoleDefaultVariableServiceTest.java`
- Delete: `backend/src/test/java/com/ansible/role/controller/RoleVariableControllerTest.java`
- Delete: `backend/src/test/java/com/ansible/role/controller/RoleDefaultVariableControllerTest.java`

- [ ] **Step 1: Delete all 14 source files and 4 test files**

```bash
cd backend
rm -f src/main/java/com/ansible/role/entity/RoleVariable.java
rm -f src/main/java/com/ansible/role/entity/RoleDefaultVariable.java
rm -f src/main/java/com/ansible/role/repository/RoleVariableRepository.java
rm -f src/main/java/com/ansible/role/repository/RoleDefaultVariableRepository.java
rm -f src/main/java/com/ansible/role/service/RoleVariableService.java
rm -f src/main/java/com/ansible/role/service/RoleDefaultVariableService.java
rm -f src/main/java/com/ansible/role/controller/RoleVariableController.java
rm -f src/main/java/com/ansible/role/controller/RoleDefaultVariableController.java
rm -f src/main/java/com/ansible/role/dto/CreateRoleVariableRequest.java
rm -f src/main/java/com/ansible/role/dto/UpdateRoleVariableRequest.java
rm -f src/main/java/com/ansible/role/dto/RoleVariableResponse.java
rm -f src/main/java/com/ansible/role/dto/CreateRoleDefaultVariableRequest.java
rm -f src/main/java/com/ansible/role/dto/UpdateRoleDefaultVariableRequest.java
rm -f src/main/java/com/ansible/role/dto/RoleDefaultVariableResponse.java
rm -f src/test/java/com/ansible/role/service/RoleVariableServiceTest.java
rm -f src/test/java/com/ansible/role/service/RoleDefaultVariableServiceTest.java
rm -f src/test/java/com/ansible/role/controller/RoleVariableControllerTest.java
rm -f src/test/java/com/ansible/role/controller/RoleDefaultVariableControllerTest.java
```

- [ ] **Step 2: Check for any remaining references to deleted classes**

Run: `cd backend && grep -r "RoleVariable\|RoleDefaultVariable\|RoleVariableRepository\|RoleDefaultVariableRepository\|RoleVariableService\|RoleDefaultVariableService\|RoleVariableController\|RoleDefaultVariableController" src/main/java/ src/test/java/ --include="*.java" -l`

Expected: No files found. If any files reference deleted classes, update them to use the unified Variable.

- [ ] **Step 3: Compile and run all tests**

Run: `cd backend && mvn test -q`
Expected: BUILD SUCCESS, all tests pass

- [ ] **Step 4: Commit**

```
refactor: remove separate role variable entities, services, controllers, and DTOs
```

---

## Task 8: Backend — Code quality checks

**Files:** All backend files modified in Tasks 1-7.

- [ ] **Step 1: Run formatting**

Run: `cd backend && mvn spotless:apply -q`

- [ ] **Step 2: Run checkstyle**

Run: `cd backend && mvn checkstyle:check -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Run PMD**

Run: `cd backend && mvn pmd:check -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Run SpotBugs**

Run: `cd backend && mvn spotbugs:check -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Run full test suite**

Run: `cd backend && mvn verify -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit any formatting changes**

```
style: apply formatting for variable redesign
```

---

## Task 9: Frontend — Update types and API layer

**Files:**
- Modify: `frontend/src/types/entity/Variable.ts`
- Modify: `frontend/src/api/variable.ts`
- Modify: `frontend/src/utils/variablePriority.ts`
- Delete: `frontend/src/types/entity/RoleVariable.ts`
- Delete: `frontend/src/api/roleVariable.ts`

- [ ] **Step 1: Update Variable.ts types**

Replace `frontend/src/types/entity/Variable.ts` with:

```typescript
export type VariableScope =
  | "PROJECT"
  | "HOSTGROUP"
  | "ENVIRONMENT"
  | "ROLE_VARS"
  | "ROLE_DEFAULTS";

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
  suggestedScope: "ROLE_VARS" | "PROJECT";
}

export interface DetectedVariableRow extends DetectedVariable {
  scope: VariableScope;
  scopeId?: number;
  userValue: string;
  rowKey: string;
}

export interface BatchVariableSaveItem {
  key: string;
  scope: VariableScope;
  scopeId?: number;
  value?: string;
}
```

Key changes:
- `VariableScope` expanded to 5 values
- `DetectedVariable.suggestedScope` changed from `"ROLE" | "PROJECT"` to `"ROLE_VARS" | "PROJECT"`
- `DetectedVariableRow` uses `scope: VariableScope` and `scopeId` instead of `scopeType` and `targetRoleId`
- `BatchVariableSaveItem` uses `scope: VariableScope` and `scopeId` instead of `saveAs`/`scope`/`roleId`

- [ ] **Step 2: Update variable.ts API**

Replace `frontend/src/api/variable.ts` with:

```typescript
import request from "./request";
import type {
  Variable,
  CreateVariableRequest,
  UpdateVariableRequest,
  VariableScope,
  DetectedVariable,
  BatchVariableSaveItem,
} from "../types/entity/Variable";

export async function createVariable(
  projectId: number,
  data: CreateVariableRequest
): Promise<Variable> {
  const res = await request.post<Variable>(`/projects/${projectId}/variables`, data);
  return res.data;
}

export async function listVariables(
  projectId: number,
  scope?: VariableScope,
  scopeId?: number
): Promise<Variable[]> {
  const params: Record<string, string | number> = {};
  if (scope != null) {
    params.scope = scope;
  }
  if (scopeId != null) {
    params.scopeId = scopeId;
  }
  const res = await request.get<Variable[]>(`/projects/${projectId}/variables`, { params });
  return res.data;
}

export async function getVariable(varId: number): Promise<Variable> {
  const res = await request.get<Variable>(`/variables/${varId}`);
  return res.data;
}

export async function updateVariable(
  varId: number,
  data: UpdateVariableRequest
): Promise<Variable> {
  const res = await request.put<Variable>(`/variables/${varId}`, data);
  return res.data;
}

export async function deleteVariable(varId: number): Promise<void> {
  await request.delete(`/variables/${varId}`);
}

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

Key change: `listVariables` now has `scope` as optional parameter.

- [ ] **Step 3: Update variablePriority.ts**

In `frontend/src/utils/variablePriority.ts`, update the priority ranks:

```typescript
const PRIORITY_RANK: Record<VariableScopeKind, number> = {
  ENVIRONMENT: 4,
  HOSTGROUP: 3,
  PROJECT: 2,
  ROLE_VARS: 1,
  ROLE_DEFAULTS: 0,
};
```

- [ ] **Step 4: Delete RoleVariable.ts and roleVariable.ts**

```bash
rm -f frontend/src/types/entity/RoleVariable.ts
rm -f frontend/src/api/roleVariable.ts
```

- [ ] **Step 5: Update variable.test.ts**

Update `frontend/src/api/variable.test.ts` to use the expanded `VariableScope` type. The existing tests should still work since the API paths haven't changed. Just ensure the `VariableScope` import includes the new values.

- [ ] **Step 6: Update variablePriority.test.ts**

Update priority tests in `frontend/src/utils/__tests__/variablePriority.test.ts` to match the new rank values (4-3-2-1-0 instead of 5-4-3-2-1).

- [ ] **Step 7: Run frontend tests**

Run: `cd frontend && npm run test -- --run`
Expected: All tests pass

- [ ] **Step 8: Run lint and format**

Run: `cd frontend && npm run format && npm run lint`
Expected: No errors

- [ ] **Step 9: Commit**

```
feat(frontend): unify variable types and API to use 5-value scope enum
```

---

## Task 10: Frontend — Rewrite VariableManager.tsx

**Files:**
- Modify: `frontend/src/pages/variable/VariableManager.tsx`

This is the largest task. The VariableManager needs to be rewritten to use the unified variable API for all 5 scopes, removing all references to `roleVariable.ts` API calls.

- [ ] **Step 1: Rewrite VariableManager.tsx**

Key changes to apply:

1. **Imports**: Remove all imports from `roleVariable.ts`. Import only from `variable.ts`. Remove `RoleVariable`, `RoleDefaultVariable` type imports. Import `VariableScope` with 5 values.

2. **Scope selector**: Update the scope Segmented/S Radio to include all 5 values:
   - `PROJECT` → "项目级"
   - `HOSTGROUP` → "主机组级"
   - `ENVIRONMENT` → "环境级"
   - `ROLE_VARS` → "Role Vars"
   - `ROLE_DEFAULTS` → "Role Defaults"

3. **ScopeId selector**: When scope is `HOSTGROUP`, show host group dropdown. When `ENVIRONMENT`, show environment dropdown. When `ROLE_VARS` or `ROLE_DEFAULTS`, show role dropdown. When `PROJECT`, hide selector.

4. **Variable CRUD**: All operations use the unified `createVariable`, `updateVariable`, `deleteVariable` from `variable.ts`.

5. **Tree view**: Instead of calling `getRoleVariables(roleId)` and `getRoleDefaults(roleId)` separately, use `listVariables(projectId, scope)` for each of the 5 scopes.

6. **Variable detection**:
   - `DetectedVariableRow` uses `scope: VariableScope` and `scopeId` instead of `scopeType` and `targetRoleId`
   - Scope selector in detection results: dropdown with PROJECT / ROLE_VARS / ROLE_DEFAULTS
   - When ROLE_VARS or ROLE_DEFAULTS selected, show role dropdown for scopeId
   - Batch save items use `{ key, scope, scopeId, value }` format

7. **Create/Edit Modal**:
   - Scope dropdown with 5 options
   - Dynamic scopeId selector based on scope
   - For ROLE_VARS/ROLE_DEFAULTS: show role dropdown (fetch roles via `getRoles(projectId)`)

8. **Priority info card**: Update to show 5-level priority:
   ```
   Environment (4) > HostGroup (3) > Project (2) > Role Vars (1) > Role Defaults (0)
   ```

- [ ] **Step 2: Run frontend tests**

Run: `cd frontend && npm run test -- --run`
Expected: All tests pass

- [ ] **Step 3: Run lint and format**

Run: `cd frontend && npm run format && npm run lint`
Expected: No errors

- [ ] **Step 4: Commit**

```
refactor(frontend): rewrite VariableManager to use unified variable API
```

---

## Task 11: Frontend — Update RoleDetail and remove old components

**Files:**
- Modify: `frontend/src/pages/role/RoleDetail.tsx`
- Delete: `frontend/src/pages/role/RoleVars.tsx`
- Delete: `frontend/src/pages/role/RoleDefaults.tsx`
- Delete: `frontend/src/pages/role/__tests__/RoleVars.test.tsx`
- Delete: `frontend/src/pages/role/__tests__/RoleDefaults.test.tsx`

- [ ] **Step 1: Remove Vars and Defaults tabs from RoleDetail.tsx**

In `frontend/src/pages/role/RoleDetail.tsx`:

Remove imports:
```typescript
import RoleVars from "./RoleVars";
import RoleDefaults from "./RoleDefaults";
```

Remove from `tabItems` array (lines 38-39):
```typescript
{ key: "vars", label: "Vars", children: <RoleVars roleId={Number(roleId)} /> },
{ key: "defaults", label: "Defaults", children: <RoleDefaults roleId={Number(roleId)} /> },
```

- [ ] **Step 2: Delete component and test files**

```bash
rm -f frontend/src/pages/role/RoleVars.tsx
rm -f frontend/src/pages/role/RoleDefaults.tsx
rm -f frontend/src/pages/role/__tests__/RoleVars.test.tsx
rm -f frontend/src/pages/role/__tests__/RoleDefaults.test.tsx
```

- [ ] **Step 3: Check for any remaining references to deleted modules**

Run: `cd frontend && grep -r "RoleVars\|RoleDefaults\|roleVariable" src/ --include="*.ts" --include="*.tsx" -l`
Expected: No files found

- [ ] **Step 4: Run frontend tests**

Run: `cd frontend && npm run test -- --run`
Expected: All tests pass

- [ ] **Step 5: Run lint and format**

Run: `cd frontend && npm run format && npm run lint`
Expected: No errors

- [ ] **Step 6: Commit**

```
refactor(frontend): remove RoleVars and RoleDefaults tabs from RoleDetail
```

---

## Task 12: End-to-end verification

**Files:** None (verification only)

- [ ] **Step 1: Full backend test suite**

Run: `cd backend && mvn verify -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Full frontend test suite**

Run: `cd frontend && npm run test -- --run`
Expected: All tests pass

- [ ] **Step 3: Frontend build**

Run: `cd frontend && npm run build`
Expected: Build succeeds with no errors

- [ ] **Step 4: Backend compile**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Manual smoke test**

Start the dev servers and verify:
1. Navigate to `/projects/:id/variables` — page loads
2. Create a variable with each scope type (PROJECT, HOSTGROUP, ENVIRONMENT, ROLE_VARS, ROLE_DEFAULTS)
3. Verify variables appear in table view
4. Switch to tree view — verify all scopes display correctly
5. Navigate to Role detail — verify Vars and Defaults tabs are gone
6. Run variable detection — verify results display
7. Batch save detected variables — verify they save correctly
8. Generate a playbook YAML — verify variable merge is correct
