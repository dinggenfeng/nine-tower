# Code Review Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all 38 issues (8 Critical, 14 High, 16 Medium) found in the comprehensive code review, organized in P0-P3 priority batches.

**Architecture:** Priority-batch execution with parallel streams within each batch. Backend fixes target Service/Repository layers. Frontend fixes target API layer, components, and routing. TDD for all new logic.

**Tech Stack:** Spring Boot 3 / Java 21 (backend), React 18 / TypeScript / Ant Design 5 (frontend), Mockito/Vitest (testing)

---

## P0 — 立即修复

### Task 1: C1-C5 级联删除 — 添加缺失的 Repository 方法

**Files:**
- Modify: `backend/src/main/java/com/ansible/tag/repository/TaskTagRepository.java`
- Modify: `backend/src/main/java/com/ansible/playbook/repository/PlaybookTagRepository.java`
- Modify: `backend/src/main/java/com/ansible/role/repository/TaskRepository.java`
- Modify: `backend/src/main/java/com/ansible/role/repository/HandlerRepository.java`
- Modify: `backend/src/main/java/com/ansible/role/repository/TemplateRepository.java`
- Modify: `backend/src/main/java/com/ansible/role/repository/RoleVariableRepository.java`
- Modify: `backend/src/main/java/com/ansible/role/repository/RoleDefaultVariableRepository.java`
- Modify: `backend/src/main/java/com/ansible/variable/repository/VariableRepository.java`
- Modify: `backend/src/main/java/com/ansible/playbook/repository/PlaybookRoleRepository.java`
- Modify: `backend/src/main/java/com/ansible/playbook/repository/PlaybookHostGroupRepository.java`
- Modify: `backend/src/main/java/com/ansible/playbook/repository/PlaybookEnvironmentRepository.java`

- [ ] **Step 1: Add missing delete methods to repositories**

`TaskTagRepository.java` — add:
```java
void deleteByTagId(Long tagId);
```

`PlaybookTagRepository.java` — add:
```java
void deleteByTagId(Long tagId);
```

`TaskRepository.java` — add:
```java
void deleteByRoleId(Long roleId);
```

`HandlerRepository.java` — add:
```java
void deleteByRoleId(Long roleId);
```

`TemplateRepository.java` — add:
```java
void deleteByRoleId(Long roleId);
```

`RoleVariableRepository.java` — add:
```java
void deleteByRoleId(Long roleId);
```

`RoleDefaultVariableRepository.java` — add:
```java
void deleteByRoleId(Long roleId);
```

`VariableRepository.java` — add:
```java
void deleteByScopeAndScopeId(VariableScope scope, Long scopeId);
```

`PlaybookRoleRepository.java` — add:
```java
void deleteByRoleId(Long roleId);
```

`PlaybookHostGroupRepository.java` — add:
```java
void deleteByHostGroupId(Long hostGroupId);
```

`PlaybookEnvironmentRepository.java` — add:
```java
void deleteByEnvironmentId(Long environmentId);
```

- [ ] **Step 2: Add bulk delete method for task_tags by roleId**

`TaskTagRepository.java` — add:
```java
@Modifying
@Query("DELETE FROM TaskTag tt WHERE tt.taskId IN (SELECT t.id FROM Task t WHERE t.roleId = :roleId)")
void deleteByTaskRoleId(@Param("roleId") Long roleId);
```

Also add the required imports:
```java
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
```

- [ ] **Step 3: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat: add cascade delete repository methods for C1-C5"
```

---

### Task 2: C1-C5 级联删除 — 创建 ProjectCleanupService + 更新各 Service delete 方法

**Files:**
- Create: `backend/src/main/java/com/ansible/project/service/ProjectCleanupService.java`
- Modify: `backend/src/main/java/com/ansible/project/service/ProjectService.java:88-97`
- Modify: `backend/src/main/java/com/ansible/role/service/RoleService.java:69-77`
- Modify: `backend/src/main/java/com/ansible/host/service/HostGroupService.java:76-84`
- Modify: `backend/src/main/java/com/ansible/environment/service/EnvironmentService.java:86-95`
- Modify: `backend/src/main/java/com/ansible/tag/service/TagService.java:57-64`
- Test: `backend/src/test/java/com/ansible/project/service/ProjectCleanupServiceTest.java`
- Test: `backend/src/test/java/com/ansible/project/service/ProjectServiceTest.java` (update existing)

- [ ] **Step 1: Write failing test for ProjectCleanupService**

Create `backend/src/test/java/com/ansible/project/service/ProjectCleanupServiceTest.java`:

```java
package com.ansible.project.service;

import static org.mockito.Mockito.*;

import com.ansible.environment.repository.EnvConfigRepository;
import com.ansible.environment.repository.EnvironmentRepository;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.host.repository.HostRepository;
import com.ansible.playbook.repository.*;
import com.ansible.role.repository.*;
import com.ansible.tag.repository.TaskTagRepository;
import com.ansible.tag.repository.TagRepository;
import com.ansible.variable.repository.VariableRepository;
import com.ansible.variable.entity.VariableScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectCleanupServiceTest {

  @Mock private RoleRepository roleRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private TaskTagRepository taskTagRepository;
  @Mock private HandlerRepository handlerRepository;
  @Mock private TemplateRepository templateRepository;
  @Mock private RoleFileRepository roleFileRepository;
  @Mock private RoleVariableRepository roleVariableRepository;
  @Mock private RoleDefaultVariableRepository roleDefaultVariableRepository;
  @Mock private HostGroupRepository hostGroupRepository;
  @Mock private HostRepository hostRepository;
  @Mock private VariableRepository variableRepository;
  @Mock private TagRepository tagRepository;
  @Mock private PlaybookRepository playbookRepository;
  @Mock private PlaybookRoleRepository playbookRoleRepository;
  @Mock private PlaybookHostGroupRepository playbookHostGroupRepository;
  @Mock private PlaybookTagRepository playbookTagRepository;
  @Mock private PlaybookEnvironmentRepository playbookEnvironmentRepository;
  @Mock private EnvironmentRepository environmentRepository;
  @Mock private EnvConfigRepository envConfigRepository;

  @InjectMocks private ProjectCleanupService cleanupService;

  @Test
  void cleanupProject_deletesAllChildResources() {
    Long projectId = 1L;

    cleanupService.cleanupProject(projectId);

    verify(playbookTagRepository).deleteByPlaybookId(1L);
    verify(playbookRoleRepository).deleteByPlaybookId(1L);
    verify(playbookHostGroupRepository).deleteByPlaybookId(1L);
    verify(playbookEnvironmentRepository).deleteByPlaybookId(1L);
    verify(playbookRepository).deleteByProjectId(projectId);
    verify(taskTagRepository).deleteByTaskRoleId(1L);
    verify(taskRepository).deleteByRoleId(1L);
    verify(handlerRepository).deleteByRoleId(1L);
    verify(templateRepository).deleteByRoleId(1L);
    verify(roleFileRepository).deleteByRoleId(1L);
    verify(roleVariableRepository).deleteByRoleId(1L);
    verify(roleDefaultVariableRepository).deleteByRoleId(1L);
    verify(roleRepository).deleteByProjectId(projectId);
    verify(hostRepository).deleteAllByHostGroupId(1L);
    verify(hostGroupRepository).deleteByProjectId(projectId);
    verify(variableRepository).deleteByScopeAndScopeId(VariableScope.PROJECT, projectId);
    verify(envConfigRepository).deleteByEnvironmentId(1L);
    verify(environmentRepository).deleteByProjectId(projectId);
    verify(taskTagRepository).deleteByTagId(1L);
    verify(playbookTagRepository).deleteByTagId(1L);
    verify(tagRepository).deleteByProjectId(projectId);
  }
}
```

Note: The test above verifies interaction with mocked repositories. The exact verify calls will need to match the implementation — the cleanup method needs to first find all child IDs (playbooks, roles, hostgroups, environments, tags) then delete their sub-resources, then delete the parents. Let me refine this in the implementation step.

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -Dtest=ProjectCleanupServiceTest -pl . -q`
Expected: FAIL (class not found)

- [ ] **Step 3: Create ProjectCleanupService**

Create `backend/src/main/java/com/ansible/project/service/ProjectCleanupService.java`:

```java
package com.ansible.project.service;

import com.ansible.environment.repository.EnvConfigRepository;
import com.ansible.environment.repository.EnvironmentRepository;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.host.repository.HostRepository;
import com.ansible.playbook.repository.*;
import com.ansible.role.repository.*;
import com.ansible.tag.repository.TaskTagRepository;
import com.ansible.tag.repository.TagRepository;
import com.ansible.variable.entity.VariableScope;
import com.ansible.variable.repository.VariableRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectCleanupService {

  private final RoleRepository roleRepository;
  private final TaskRepository taskRepository;
  private final TaskTagRepository taskTagRepository;
  private final HandlerRepository handlerRepository;
  private final TemplateRepository templateRepository;
  private final RoleFileRepository roleFileRepository;
  private final RoleVariableRepository roleVariableRepository;
  private final RoleDefaultVariableRepository roleDefaultVariableRepository;
  private final HostGroupRepository hostGroupRepository;
  private final HostRepository hostRepository;
  private final VariableRepository variableRepository;
  private final TagRepository tagRepository;
  private final PlaybookRepository playbookRepository;
  private final PlaybookRoleRepository playbookRoleRepository;
  private final PlaybookHostGroupRepository playbookHostGroupRepository;
  private final PlaybookTagRepository playbookTagRepository;
  private final PlaybookEnvironmentRepository playbookEnvironmentRepository;
  private final EnvironmentRepository environmentRepository;
  private final EnvConfigRepository envConfigRepository;

  @Transactional
  public void cleanupProject(Long projectId) {
    List<Long> playbookIds =
        playbookRepository.findByProjectIdOrderByIdAsc(projectId).stream()
            .map(p -> p.getId())
            .toList();
    for (Long pbId : playbookIds) {
      playbookRoleRepository.deleteByPlaybookId(pbId);
      playbookHostGroupRepository.deleteByPlaybookId(pbId);
      playbookTagRepository.deleteByPlaybookId(pbId);
      playbookEnvironmentRepository.deleteByPlaybookId(pbId);
    }
    playbookRepository.deleteByProjectId(projectId);

    List<Long> roleIds =
        roleRepository.findAllByProjectId(projectId).stream()
            .map(r -> r.getId())
            .toList();
    for (Long roleId : roleIds) {
      cleanupRoleResources(roleId);
    }
    roleRepository.deleteByProjectId(projectId);

    List<Long> hostGroupIds =
        hostGroupRepository.findAllByProjectId(projectId).stream()
            .map(hg -> hg.getId())
            .toList();
    for (Long hgId : hostGroupIds) {
      hostRepository.deleteAllByHostGroupId(hgId);
      variableRepository.deleteByScopeAndScopeId(VariableScope.HOSTGROUP, hgId);
    }
    hostGroupRepository.deleteByProjectId(projectId);

    List<Long> envIds =
        environmentRepository.findByProjectIdOrderByIdAsc(projectId).stream()
            .map(e -> e.getId())
            .toList();
    for (Long envId : envIds) {
      envConfigRepository.deleteByEnvironmentId(envId);
      variableRepository.deleteByScopeAndScopeId(VariableScope.ENVIRONMENT, envId);
    }
    environmentRepository.deleteByProjectId(projectId);

    List<Long> tagIds =
        tagRepository.findByProjectIdOrderByIdAsc(projectId).stream()
            .map(t -> t.getId())
            .toList();
    for (Long tagId : tagIds) {
      taskTagRepository.deleteByTagId(tagId);
      playbookTagRepository.deleteByTagId(tagId);
    }
    tagRepository.deleteByProjectId(projectId);

    variableRepository.deleteByScopeAndScopeId(VariableScope.PROJECT, projectId);
  }

  @Transactional
  public void cleanupRoleResources(Long roleId) {
    taskTagRepository.deleteByTaskRoleId(roleId);
    taskRepository.deleteByRoleId(roleId);
    handlerRepository.deleteByRoleId(roleId);
    templateRepository.deleteByRoleId(roleId);
    roleFileRepository.deleteByRoleId(roleId);
    roleVariableRepository.deleteByRoleId(roleId);
    roleDefaultVariableRepository.deleteByRoleId(roleId);
    playbookRoleRepository.deleteByRoleId(roleId);
  }

  @Transactional
  public void cleanupHostGroupResources(Long hostGroupId) {
    hostRepository.deleteAllByHostGroupId(hostGroupId);
    variableRepository.deleteByScopeAndScopeId(VariableScope.HOSTGROUP, hostGroupId);
    playbookHostGroupRepository.deleteByHostGroupId(hostGroupId);
  }

  @Transactional
  public void cleanupEnvironmentResources(Long envId) {
    envConfigRepository.deleteByEnvironmentId(envId);
    variableRepository.deleteByScopeAndScopeId(VariableScope.ENVIRONMENT, envId);
    playbookEnvironmentRepository.deleteByEnvironmentId(envId);
  }

  @Transactional
  public void cleanupTagResources(Long tagId) {
    taskTagRepository.deleteByTagId(tagId);
    playbookTagRepository.deleteByTagId(tagId);
  }
}
```

Also add the missing `deleteByProjectId` methods to repositories that don't have them yet:
- `PlaybookRepository.java` — add: `void deleteByProjectId(Long projectId);`
- `RoleRepository.java` — add: `void deleteByProjectId(Long projectId);`
- `HostGroupRepository.java` — add: `void deleteByHostGroupId(Long hostGroupId);` and `void deleteByProjectId(Long projectId);`
- `EnvironmentRepository.java` — add: `void deleteByProjectId(Long projectId);`
- `TagRepository.java` — add: `void deleteByProjectId(Long projectId);`

- [ ] **Step 4: Update ProjectCleanupServiceTest to match implementation**

Update the test to properly verify the cleanup flow using mock returns:

```java
@Test
void cleanupProject_deletesAllChildResources() {
  Long projectId = 1L;
  Playbook pb = new Playbook();
  ReflectionTestUtils.setField(pb, "id", 10L);
  when(playbookRepository.findByProjectIdOrderByIdAsc(projectId)).thenReturn(List.of(pb));

  Role role = new Role();
  ReflectionTestUtils.setField(role, "id", 20L);
  when(roleRepository.findAllByProjectId(projectId)).thenReturn(List.of(role));

  HostGroup hg = new HostGroup();
  ReflectionTestUtils.setField(hg, "id", 30L);
  when(hostGroupRepository.findAllByProjectId(projectId)).thenReturn(List.of(hg));

  Environment env = new Environment();
  ReflectionTestUtils.setField(env, "id", 40L);
  when(environmentRepository.findByProjectIdOrderByIdAsc(projectId)).thenReturn(List.of(env));

  Tag tag = new Tag();
  ReflectionTestUtils.setField(tag, "id", 50L);
  when(tagRepository.findByProjectIdOrderByIdAsc(projectId)).thenReturn(List.of(tag));

  cleanupService.cleanupProject(projectId);

  verify(playbookRoleRepository).deleteByPlaybookId(10L);
  verify(playbookTagRepository).deleteByPlaybookId(10L);
  verify(playbookHostGroupRepository).deleteByPlaybookId(10L);
  verify(playbookEnvironmentRepository).deleteByPlaybookId(10L);
  verify(playbookRepository).deleteByProjectId(projectId);

  verify(taskTagRepository).deleteByTaskRoleId(20L);
  verify(taskRepository).deleteByRoleId(20L);
  verify(roleRepository).deleteByProjectId(projectId);

  verify(hostRepository).deleteAllByHostGroupId(30L);
  verify(variableRepository).deleteByScopeAndScopeId(VariableScope.HOSTGROUP, 30L);
  verify(hostGroupRepository).deleteByProjectId(projectId);

  verify(envConfigRepository).deleteByEnvironmentId(40L);
  verify(variableRepository).deleteByScopeAndScopeId(VariableScope.ENVIRONMENT, 40L);
  verify(environmentRepository).deleteByProjectId(projectId);

  verify(taskTagRepository).deleteByTagId(50L);
  verify(playbookTagRepository).deleteByTagId(50L);
  verify(tagRepository).deleteByProjectId(projectId);

  verify(variableRepository).deleteByScopeAndScopeId(VariableScope.PROJECT, projectId);
}
```

- [ ] **Step 5: Update ProjectService.deleteProject to call cleanupService**

`ProjectService.java` — add field and modify deleteProject:

```java
private final ProjectCleanupService cleanupService; // add to existing fields

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
```

- [ ] **Step 6: Update RoleService.deleteRole to call cleanupService**

`RoleService.java` — add field and modify deleteRole:

```java
private final ProjectCleanupService cleanupService; // add to existing fields

@Transactional
public void deleteRole(Long roleId, Long currentUserId) {
  Role role =
      roleRepository
          .findById(roleId)
          .orElseThrow(() -> new IllegalArgumentException("Role not found"));
  accessChecker.checkOwnerOrAdmin(role.getProjectId(), role.getCreatedBy(), currentUserId);
  cleanupService.cleanupRoleResources(roleId);
  roleRepository.delete(role);
}
```

- [ ] **Step 7: Update HostGroupService.deleteHostGroup to call cleanupService**

`HostGroupService.java` — add field and modify deleteHostGroup:

```java
private final ProjectCleanupService cleanupService; // add to existing fields

@Transactional
public void deleteHostGroup(Long hostGroupId, Long currentUserId) {
  HostGroup hostGroup =
      hostGroupRepository
          .findById(hostGroupId)
          .orElseThrow(() -> new IllegalArgumentException("Host group not found"));
  accessChecker.checkOwnerOrAdmin(hostGroup.getProjectId(), hostGroup.getCreatedBy(), currentUserId);
  cleanupService.cleanupHostGroupResources(hostGroupId);
  hostGroupRepository.delete(hostGroup);
}
```

- [ ] **Step 8: Update EnvironmentService.deleteEnvironment to call cleanupService**

`EnvironmentService.java` — add field and modify deleteEnvironment:

```java
private final ProjectCleanupService cleanupService; // add to existing fields

@Transactional
public void deleteEnvironment(Long envId, Long userId) {
  Environment env =
      environmentRepository
          .findById(envId)
          .orElseThrow(() -> new IllegalArgumentException("Environment not found"));
  accessChecker.checkOwnerOrAdmin(env.getProjectId(), env.getCreatedBy(), userId);
  cleanupService.cleanupEnvironmentResources(envId);
  environmentRepository.delete(env);
}
```

Remove the old `envConfigRepository.deleteByEnvironmentId(envId)` line since cleanupService now handles it.

- [ ] **Step 9: Update TagService.deleteTag to call cleanupService**

`TagService.java` — add field and modify deleteTag:

```java
private final ProjectCleanupService cleanupService; // add to existing fields

@Transactional
public void deleteTag(Long tagId, Long userId) {
  Tag tag =
      tagRepository
          .findById(tagId)
          .orElseThrow(() -> new IllegalArgumentException("Tag not found"));
  accessChecker.checkOwnerOrAdmin(tag.getProjectId(), tag.getCreatedBy(), userId);
  cleanupService.cleanupTagResources(tagId);
  tagRepository.delete(tag);
}
```

- [ ] **Step 10: Update existing service tests that verify delete behavior**

For each service test that tests the delete method (ProjectServiceTest, RoleServiceTest, HostGroupServiceTest, EnvironmentServiceTest, TagServiceTest), update to mock `cleanupService`:

Example for `ProjectServiceTest`:
```java
@Mock private ProjectCleanupService cleanupService;
// In deleteProject test, add:
verify(cleanupService).cleanupProject(projectId);
```

- [ ] **Step 11: Run all backend tests**

Run: `cd backend && mvn test -q`
Expected: All tests pass

- [ ] **Step 12: Run format check and fix**

Run: `cd backend && mvn spotless:apply && mvn checkstyle:check pmd:check spotbugs:check -q`
Expected: All checks pass

- [ ] **Step 13: Commit**

```bash
git add -A && git commit -m "fix: add cascade delete for project, role, hostgroup, environment, tag (C1-C5)"
```

---

### Task 3: C6 VariableService 权限校验 bug 修复

**Files:**
- Modify: `backend/src/main/java/com/ansible/variable/service/VariableService.java:54-91`
- Modify: `backend/src/test/java/com/ansible/variable/service/VariableServiceTest.java`

- [ ] **Step 1: Write failing test for HOSTGROUP scope permission**

`VariableServiceTest.java` — add tests:

```java
@Test
void getVariable_hostGroupScope_resolvesProjectIdFromHostGroup() {
  Variable variable = new Variable();
  ReflectionTestUtils.setField(variable, "id", 1L);
  variable.setScope(VariableScope.HOSTGROUP);
  variable.setScopeId(10L);
  variable.setCreatedBy(2L);
  when(variableRepository.findById(1L)).thenReturn(Optional.of(variable));

  HostGroup hostGroup = new HostGroup();
  hostGroup.setProjectId(100L);
  when(hostGroupRepository.findById(10L)).thenReturn(Optional.of(hostGroup));

  when(accessChecker.checkMembership(100L, 1L)).thenReturn(member);

  variableService.getVariable(1L, 1L);

  verify(accessChecker).checkMembership(100L, 1L);
}

@Test
void getVariable_environmentScope_resolvesProjectIdFromEnvironment() {
  Variable variable = new Variable();
  ReflectionTestUtils.setField(variable, "id", 1L);
  variable.setScope(VariableScope.ENVIRONMENT);
  variable.setScopeId(20L);
  variable.setCreatedBy(2L);
  when(variableRepository.findById(1L)).thenReturn(Optional.of(variable));

  Environment env = new Environment();
  env.setProjectId(200L);
  when(environmentRepository.findById(20L)).thenReturn(Optional.of(env));

  when(accessChecker.checkMembership(200L, 1L)).thenReturn(member);

  variableService.getVariable(1L, 1L);

  verify(accessChecker).checkMembership(200L, 1L);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -Dtest=VariableServiceTest -pl . -q`
Expected: FAIL (accessChecker.checkMembership called with scopeId instead of projectId)

- [ ] **Step 3: Add helper method to VariableService and fix permission checks**

`VariableService.java` — add dependencies and helper:

```java
// Add fields:
private final HostGroupRepository hostGroupRepository;
private final EnvironmentRepository environmentRepository;

private Long resolveProjectId(Variable variable) {
  return switch (variable.getScope()) {
    case PROJECT -> variable.getScopeId();
    case HOSTGROUP -> hostGroupRepository.findById(variable.getScopeId())
        .orElseThrow(() -> new IllegalArgumentException("Host group not found"))
        .getProjectId();
    case ENVIRONMENT -> environmentRepository.findById(variable.getScopeId())
        .orElseThrow(() -> new IllegalArgumentException("Environment not found"))
        .getProjectId();
  };
}
```

Then fix `getVariable`, `updateVariable`, `deleteVariable` to use `resolveProjectId`:

```java
// getVariable (line ~60):
Long projectId = resolveProjectId(v);
accessChecker.checkMembership(projectId, userId);

// updateVariable (line ~71):
Long projectId = resolveProjectId(v);
accessChecker.checkOwnerOrAdmin(projectId, v.getCreatedBy(), userId);

// deleteVariable (line ~89):
Long projectId = resolveProjectId(v);
accessChecker.checkOwnerOrAdmin(projectId, v.getCreatedBy(), userId);
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend && mvn test -Dtest=VariableServiceTest -pl . -q`
Expected: PASS

- [ ] **Step 5: Format and commit**

```bash
cd backend && mvn spotless:apply
git add -A && git commit -m "fix: correct VariableService permission check for non-PROJECT scopes (C6)"
```

---

### Task 4: C7 RoleFileService/TemplateService 权限校验补全

**Files:**
- Modify: `backend/src/main/java/com/ansible/role/service/RoleFileService.java:92-111`
- Modify: `backend/src/main/java/com/ansible/role/service/TemplateService.java:139-146`
- Modify: `backend/src/test/java/com/ansible/role/service/RoleFileServiceTest.java`
- Modify: `backend/src/test/java/com/ansible/role/service/TemplateServiceTest.java`

- [ ] **Step 1: Write failing test for RoleFileService.getFileContent without membership**

`RoleFileServiceTest.java` — add:

```java
@Test
void getFileContent_withoutMembership_throwsSecurityException() {
  RoleFile file = new RoleFile();
  file.setRoleId(1L);
  ReflectionTestUtils.setField(file, "id", 10L);
  file.setIsDirectory(false);
  when(roleFileRepository.findById(10L)).thenReturn(Optional.of(file));

  Role role = new Role();
  role.setProjectId(100L);
  when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

  when(accessChecker.checkMembership(100L, 2L))
      .thenThrow(new SecurityException("Not a member"));

  assertThatThrownBy(() -> roleFileService.getFileContent(10L, 2L))
      .isInstanceOf(SecurityException.class);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -Dtest=RoleFileServiceTest -pl . -q`
Expected: FAIL (no permission check in getFileContent)

- [ ] **Step 3: Add permission check to RoleFileService.getFileContent and getFileName**

`RoleFileService.java` — modify `getFileContent`:

```java
public String getFileContent(Long fileId, Long userId) {
  RoleFile file = roleFileRepository.findById(fileId)
      .orElseThrow(() -> new IllegalArgumentException("File not found"));
  if (file.getIsDirectory()) {
    throw new IllegalArgumentException("Cannot read content of a directory");
  }
  Role role = roleRepository.findById(file.getRoleId())
      .orElseThrow(() -> new IllegalArgumentException("Role not found"));
  accessChecker.checkMembership(role.getProjectId(), userId);
  return file.getContent();
}
```

`RoleFileService.java` — modify `getFileName`:

```java
public String getFileName(Long fileId, Long userId) {
  RoleFile file = roleFileRepository.findById(fileId)
      .orElseThrow(() -> new IllegalArgumentException("File not found"));
  Role role = roleRepository.findById(file.getRoleId())
      .orElseThrow(() -> new IllegalArgumentException("Role not found"));
  accessChecker.checkMembership(role.getProjectId(), userId);
  return file.getName();
}
```

- [ ] **Step 4: Write failing test for TemplateService.getTemplateName without membership**

`TemplateServiceTest.java` — add:

```java
@Test
void getTemplateName_withoutMembership_throwsSecurityException() {
  Template template = new Template();
  template.setRoleId(1L);
  ReflectionTestUtils.setField(template, "id", 10L);
  when(templateRepository.findById(10L)).thenReturn(Optional.of(template));

  Role role = new Role();
  role.setProjectId(100L);
  when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

  when(accessChecker.checkMembership(100L, 2L))
      .thenThrow(new SecurityException("Not a member"));

  assertThatThrownBy(() -> templateService.getTemplateName(10L, 2L))
      .isInstanceOf(SecurityException.class);
}
```

- [ ] **Step 5: Add permission check to TemplateService.getTemplateName**

`TemplateService.java` — modify `getTemplateName`:

```java
public String getTemplateName(Long templateId, Long currentUserId) {
  Template template = templateRepository.findById(templateId)
      .orElseThrow(() -> new IllegalArgumentException("Template not found"));
  Role role = roleRepository.findById(template.getRoleId())
      .orElseThrow(() -> new IllegalArgumentException("Role not found"));
  accessChecker.checkMembership(role.getProjectId(), currentUserId);
  return template.getName();
}
```

- [ ] **Step 6: Run all role service tests**

Run: `cd backend && mvn test -Dtest="RoleFileServiceTest,TemplateServiceTest" -pl . -q`
Expected: All tests pass

- [ ] **Step 7: Format and commit**

```bash
cd backend && mvn spotless:apply
git add -A && git commit -m "fix: add permission checks to RoleFileService/TemplateService read methods (C7)"
```

---

### Task 5: C8 前端 API 返回模式统一

**Files:**
- Modify: `frontend/src/api/auth.ts`
- Modify: `frontend/src/api/user.ts`
- Modify: `frontend/src/stores/authStore.ts:37-39`
- Modify: `frontend/src/pages/auth/Login.tsx`
- Modify: `frontend/src/pages/auth/Register.tsx`
- Modify: `frontend/src/pages/project/MemberManagement.tsx`
- Modify: `frontend/src/components/Layout/MainLayout.tsx`

- [ ] **Step 1: Modify auth.ts to unwrap like other modules**

`frontend/src/api/auth.ts` — replace entire file:

```typescript
import request from "./request";
import type { TokenResponse } from "../types/entity/User";

export interface RegisterPayload {
  username: string;
  password: string;
  email: string;
}

export interface LoginPayload {
  username: string;
  password: string;
}

export const authApi = {
  register: async (payload: RegisterPayload): Promise<TokenResponse> => {
    const res = await request.post<TokenResponse>("/auth/register", payload);
    return res.data;
  },

  login: async (payload: LoginPayload): Promise<TokenResponse> => {
    const res = await request.post<TokenResponse>("/auth/login", payload);
    return res.data;
  },

  me: async (): Promise<TokenResponse["user"]> => {
    const res = await request.get<TokenResponse["user"]>("/auth/me");
    return res.data;
  },
};
```

- [ ] **Step 2: Modify user.ts to unwrap like other modules**

`frontend/src/api/user.ts` — replace entire file:

```typescript
import request from "./request";
import type { User, PageResponse } from "../types/entity/User";

export interface UpdateUserPayload {
  email?: string;
  password?: string;
  oldPassword?: string;
}

export const userApi = {
  searchUsers: async (keyword?: string, page = 0, size = 20): Promise<PageResponse<User>> => {
    const res = await request.get<PageResponse<User>>("/users", { params: { keyword, page, size } });
    return res.data;
  },

  getUser: async (id: number): Promise<User> => {
    const res = await request.get<User>(`/users/${id}`);
    return res.data;
  },

  updateUser: async (id: number, payload: UpdateUserPayload): Promise<User> => {
    const res = await request.put<User>(`/users/${id}`, payload);
    return res.data;
  },

  deleteUser: async (id: number): Promise<void> => {
    await request.delete(`/users/${id}`);
  },
};
```

- [ ] **Step 3: Update authStore.ts caller**

`frontend/src/stores/authStore.ts` — change `fetchUser`:

```typescript
// line 37-39, change from:
//   const res = await authApi.me();
//   set({ user: res.data, loading: false });
// to:
  const user = await authApi.me();
  if (!get().user) {
    set({ user, loading: false });
  }
```

- [ ] **Step 4: Update Login.tsx caller**

`frontend/src/pages/auth/Login.tsx` — change login handler:

```typescript
// Change from: const res = await authApi.login(values);
//              const { token, user } = res.data;
// To:
const { token, user } = await authApi.login(values);
```

- [ ] **Step 5: Update Register.tsx caller**

`frontend/src/pages/auth/Register.tsx` — change register handler:

```typescript
// Change from: const res = await authApi.register(values);
//              const { token, user } = res.data;
// To:
const { token, user } = await authApi.register(values);
```

- [ ] **Step 6: Update MemberManagement.tsx caller**

`frontend/src/pages/project/MemberManagement.tsx` — change search handler:

```typescript
// Change from: const res = await userApi.searchUsers(keyword);
//              ... res.data.content ...
// To:
const result = await userApi.searchUsers(keyword);
// use result.content instead of res.data.content
```

- [ ] **Step 7: Update MainLayout.tsx callers**

`frontend/src/components/Layout/MainLayout.tsx` — change update handler:

```typescript
// Change from: const res = await userApi.updateUser(user.id, { email: values.email });
//              setUser(res.data);
// To:
const updated = await userApi.updateUser(user.id, { email: values.email });
setUser(updated);
```

- [ ] **Step 8: Run frontend tests and type check**

Run: `cd frontend && npx tsc --noEmit && npm run test -- --run`
Expected: All tests pass, no type errors

- [ ] **Step 9: Format and commit**

```bash
cd frontend && npm run format
git add -A && git commit -m "fix: unify API return pattern in auth.ts and user.ts (C8)"
```

---

## P1 — 尽快修复

### Task 6: H3 TagService 添加 @Transactional

**Files:**
- Modify: `backend/src/main/java/com/ansible/tag/service/TagService.java`

- [ ] **Step 1: Add @Transactional to write methods**

`TagService.java` — add `@Transactional` before `createTag`, `updateTag`, `deleteTag`:

```java
@Transactional
public TagResponse createTag(...) { ... }

@Transactional
public TagResponse updateTag(...) { ... }

@Transactional
public void deleteTag(...) { ... }
```

Add import: `import org.springframework.transaction.annotation.Transactional;` (if not present).

- [ ] **Step 2: Run tests**

Run: `cd backend && mvn test -Dtest=TagServiceTest -pl . -q`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "fix: add @Transactional to TagService write methods (H3)"
```

---

### Task 7: H8 JWT 密钥缓存

**Files:**
- Modify: `backend/src/main/java/com/ansible/security/JwtTokenProvider.java`
- Test: `backend/src/test/java/com/ansible/security/JwtTokenProviderTest.java`

- [ ] **Step 1: Write test for JwtTokenProvider**

Create `backend/src/test/java/com/ansible/security/JwtTokenProviderTest.java`:

```java
package com.ansible.security;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenProviderTest {

  private JwtTokenProvider provider;

  @BeforeEach
  void setUp() {
    provider = new JwtTokenProvider();
    ReflectionTestUtils.setField(provider, "jwtSecret",
        "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha");
    ReflectionTestUtils.setField(provider, "jwtExpirationMs", 3600000L);
    provider.init();
  }

  @Test
  void generateToken_andGetUserIdFromToken() {
    Long userId = 42L;
    String token = provider.generateToken(userId);
    Long extracted = provider.getUserIdFromToken(token);
    assertThat(extracted).isEqualTo(userId);
  }

  @Test
  void validateToken_validToken_returnsTrue() {
    String token = provider.generateToken(1L);
    assertThat(provider.validateToken(token)).isTrue();
  }

  @Test
  void validateToken_invalidToken_returnsFalse() {
    assertThat(provider.validateToken("invalid.token.here")).isFalse();
  }

  @Test
  void validateToken_expiredToken_returnsFalse() {
    JwtTokenProvider expiredProvider = new JwtTokenProvider();
    ReflectionTestUtils.setField(expiredProvider, "jwtSecret",
        "test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha");
    ReflectionTestUtils.setField(expiredProvider, "jwtExpirationMs", -1000L);
    expiredProvider.init();
    String token = expiredProvider.generateToken(1L);
    assertThat(provider.validateToken(token)).isFalse();
  }

  @Test
  void validateToken_emptyString_returnsFalse() {
    assertThat(provider.validateToken("")).isFalse();
  }

  @Test
  void validateToken_null_returnsFalse() {
    assertThat(provider.validateToken(null)).isFalse();
  }
}
```

- [ ] **Step 2: Run test to verify it fails (init method doesn't exist yet)**

Run: `cd backend && mvn test -Dtest=JwtTokenProviderTest -pl . -q`
Expected: FAIL

- [ ] **Step 3: Modify JwtTokenProvider to cache key**

`JwtTokenProvider.java` — replace with:

```java
package com.ansible.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  @Value("${app.jwt.secret}")
  private String jwtSecret;

  @Value("${app.jwt.expiration-ms}")
  private long jwtExpirationMs;

  private SecretKey signingKey;

  @PostConstruct
  void init() {
    this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  public String generateToken(Long userId) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + jwtExpirationMs);
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .issuedAt(now)
        .expiration(expiry)
        .signWith(signingKey)
        .compact();
  }

  public Long getUserIdFromToken(String token) {
    Claims claims =
        Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    return Long.valueOf(claims.getSubject());
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }
}
```

- [ ] **Step 4: Run tests**

Run: `cd backend && mvn test -Dtest=JwtTokenProviderTest -pl . -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
cd backend && mvn spotless:apply
git add -A && git commit -m "fix: cache JWT signing key with @PostConstruct (H8)"
```

---

### Task 8: H1 认证端点速率限制

**Files:**
- Create: `backend/src/main/java/com/ansible/security/RateLimitFilter.java`
- Modify: `backend/src/main/java/com/ansible/security/SecurityConfig.java`
- Test: `backend/src/test/java/com/ansible/security/RateLimitFilterTest.java`

- [ ] **Step 1: Write test for RateLimitFilter**

Create `backend/src/test/java/com/ansible/security/RateLimitFilterTest.java`:

```java
package com.ansible.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  private RateLimitFilter filter;

  @BeforeEach
  void setUp() {
    filter = new RateLimitFilter();
  }

  @Test
  void doFilter_nonAuthEndpoint_passesThrough() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/projects");
    filter.doFilter(request, response, filterChain);
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilter_loginEndpoint_underLimit_passesThrough() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/auth/login");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    for (int i = 0; i < 10; i++) {
      filter.doFilter(request, response, filterChain);
    }
    verify(filterChain, times(10)).doFilter(request, response);
    verify(response, never()).setStatus(anyInt());
  }

  @Test
  void doFilter_loginEndpoint_overLimit_returns429() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/auth/login");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    for (int i = 0; i < 10; i++) {
      filter.doFilter(request, response, filterChain);
    }
    filter.doFilter(request, response, filterChain);
    verify(response).setStatus(429);
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -Dtest=RateLimitFilterTest -pl . -q`
Expected: FAIL

- [ ] **Step 3: Create RateLimitFilter**

Create `backend/src/main/java/com/ansible/security/RateLimitFilter.java`:

```java
package com.ansible.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final int MAX_REQUESTS_PER_MINUTE = 10;
  private final ConcurrentHashMap<String, RequestCounter> counters = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String uri = request.getRequestURI();
    if (!uri.equals("/api/auth/login") && !uri.equals("/api/auth/register")) {
      filterChain.doFilter(request, response);
      return;
    }

    String clientIp = request.getRemoteAddr();
    RequestCounter counter = counters.computeIfAbsent(clientIp, k -> new RequestCounter());

    if (counter.isOverLimit()) {
      response.setStatus(429);
      return;
    }

    counter.increment();
    filterChain.doFilter(request, response);
  }

  private static class RequestCounter {
    private final AtomicInteger count = new AtomicInteger(0);
    private volatile long windowStart = System.currentTimeMillis();

    boolean isOverLimit() {
      long now = System.currentTimeMillis();
      if (now - windowStart > 60000) {
        count.set(0);
        windowStart = now;
      }
      return count.get() >= MAX_REQUESTS_PER_MINUTE;
    }

    void increment() {
      count.incrementAndGet();
    }
  }
}
```

- [ ] **Step 4: Register filter in SecurityConfig**

`SecurityConfig.java` — add RateLimitFilter before JwtAuthenticationFilter:

```java
private final RateLimitFilter rateLimitFilter;

// In filterChain method, change:
//   .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
// To:
//   .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
//   .addFilterBefore(jwtAuthFilter, RateLimitFilter.class)
```

- [ ] **Step 5: Run tests**

Run: `cd backend && mvn test -Dtest="RateLimitFilterTest,AuthControllerTest" -pl . -q`
Expected: PASS

- [ ] **Step 6: Format and commit**

```bash
cd backend && mvn spotless:apply
git add -A && git commit -m "feat: add rate limiting to auth endpoints (H1)"
```

---

### Task 9: H4-H7 N+1 查询优化

**Files:**
- Modify: `backend/src/main/java/com/ansible/project/repository/ProjectRepository.java`
- Modify: `backend/src/main/java/com/ansible/project/repository/ProjectMemberRepository.java`
- Modify: `backend/src/main/java/com/ansible/user/repository/UserRepository.java`
- Modify: `backend/src/main/java/com/ansible/playbook/repository/PlaybookRoleRepository.java`
- Modify: `backend/src/main/java/com/ansible/playbook/repository/PlaybookHostGroupRepository.java`
- Modify: `backend/src/main/java/com/ansible/playbook/repository/PlaybookTagRepository.java`
- Modify: `backend/src/main/java/com/ansible/playbook/repository/PlaybookEnvironmentRepository.java`
- Modify: `backend/src/main/java/com/ansible/environment/repository/EnvironmentRepository.java`
- Modify: `backend/src/main/java/com/ansible/project/service/ProjectService.java:43-57`
- Modify: `backend/src/main/java/com/ansible/project/service/ProjectMemberService.java:25-39`
- Modify: `backend/src/main/java/com/ansible/playbook/service/PlaybookService.java:76-82,390-420`
- Modify: `backend/src/main/java/com/ansible/environment/service/EnvironmentService.java:43-53`
- Test: Update existing service tests for these 4 services

- [ ] **Step 1: Add batch query methods to repositories**

`ProjectMemberRepository.java` — add:

```java
@Query("SELECT pm FROM ProjectMember pm WHERE pm.userId = :userId")
List<ProjectMember> findAllByUserId(@Param("userId") Long userId);
```

`UserRepository.java` — add:

```java
@Query("SELECT u FROM User u WHERE u.id IN :ids")
List<User> findAllByIdIn(@Param("ids") List<Long> ids);
```

`PlaybookRoleRepository.java` — add:

```java
@Query("SELECT pr FROM PlaybookRole pr WHERE pr.playbookId IN :playbookIds ORDER BY pr.orderIndex ASC")
List<PlaybookRole> findByPlaybookIdIn(@Param("playbookIds") List<Long> playbookIds);
```

`PlaybookHostGroupRepository.java` — add:

```java
@Query("SELECT phg FROM PlaybookHostGroup phg WHERE phg.playbookId IN :playbookIds")
List<PlaybookHostGroup> findByPlaybookIdIn(@Param("playbookIds") List<Long> playbookIds);
```

`PlaybookTagRepository.java` — add:

```java
@Query("SELECT pt FROM PlaybookTag pt WHERE pt.playbookId IN :playbookIds")
List<PlaybookTag> findByPlaybookIdIn(@Param("playbookIds") List<Long> playbookIds);
```

`PlaybookEnvironmentRepository.java` — add:

```java
@Query("SELECT pe FROM PlaybookEnvironment pe WHERE pe.playbookId IN :playbookIds")
List<PlaybookEnvironment> findByPlaybookIdIn(@Param("playbookIds") List<Long> playbookIds);
```

`EnvironmentRepository.java` — no change needed (Environment entity uses plain Long FK, no JPA relationships to FETCH JOIN).

`EnvConfigRepository.java` — add batch query method:

```java
List<EnvConfig> findByEnvironmentIdInOrderByEnvironmentIdAscConfigKeyAsc(List<Long> environmentIds);
```

- [ ] **Step 2: Optimize ProjectService.getMyProjects**

`ProjectService.java` — replace getMyProjects:

```java
@Transactional(readOnly = true)
public List<ProjectResponse> getMyProjects(Long currentUserId) {
  List<ProjectMember> memberships =
      projectMemberRepository.findAllByUserId(currentUserId);
  Map<Long, ProjectRole> roleMap = memberships.stream()
      .collect(Collectors.toMap(ProjectMember::getProjectId, ProjectMember::getRole));
  List<Long> projectIds = new ArrayList<>(roleMap.keySet());
  if (projectIds.isEmpty()) {
    return List.of();
  }
  List<Project> projects = projectRepository.findAllById(projectIds);
  return projects.stream()
      .map(p -> new ProjectResponse(p, roleMap.get(p.getId())))
      .toList();
}
```

Add imports: `java.util.Map`, `java.util.ArrayList`, `java.util.stream.Collectors`

- [ ] **Step 3: Optimize ProjectMemberService.listMembers**

`ProjectMemberService.java` — replace listMembers:

```java
@Transactional(readOnly = true)
public List<ProjectMemberResponse> listMembers(Long projectId, Long currentUserId) {
  accessChecker.checkMembership(projectId, currentUserId);
  List<ProjectMember> members = projectMemberRepository.findAllByProjectId(projectId);
  List<Long> userIds = members.stream().map(ProjectMember::getUserId).toList();
  Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
      .collect(Collectors.toMap(User::getId, u -> u));
  return members.stream()
      .map(member -> {
        User user = userMap.get(member.getUserId());
        if (user == null) {
          throw new IllegalArgumentException("User not found");
        }
        return new ProjectMemberResponse(member, user);
      })
      .toList();
}
```

Add import: `java.util.stream.Collectors`, `java.util.Map`

- [ ] **Step 4: Optimize PlaybookService.listPlaybooks**

`PlaybookService.java` — replace listPlaybooks:

```java
@Transactional(readOnly = true)
public List<PlaybookResponse> listPlaybooks(Long projectId, Long userId) {
  accessChecker.checkMembership(projectId, userId);
  List<Playbook> playbooks = playbookRepository.findByProjectIdOrderByIdAsc(projectId);
  if (playbooks.isEmpty()) {
    return List.of();
  }
  List<Long> playbookIds = playbooks.stream().map(Playbook::getId).toList();

  Map<Long, List<Long>> roleIdsMap = playbookRoleRepository.findByPlaybookIdIn(playbookIds)
      .stream().collect(Collectors.groupingBy(PlaybookRole::getPlaybookId,
          Collectors.mapping(PlaybookRole::getRoleId, Collectors.toList())));
  Map<Long, List<Long>> hostGroupIdsMap = playbookHostGroupRepository.findByPlaybookIdIn(playbookIds)
      .stream().collect(Collectors.groupingBy(PlaybookHostGroup::getPlaybookId,
          Collectors.mapping(PlaybookHostGroup::getHostGroupId, Collectors.toList())));
  Map<Long, List<Long>> tagIdsMap = playbookTagRepository.findByPlaybookIdIn(playbookIds)
      .stream().collect(Collectors.groupingBy(PlaybookTag::getPlaybookId,
          Collectors.mapping(PlaybookTag::getTagId, Collectors.toList())));
  Map<Long, List<Long>> envIdsMap = playbookEnvironmentRepository.findByPlaybookIdIn(playbookIds)
      .stream().collect(Collectors.groupingBy(PlaybookEnvironment::getPlaybookId,
          Collectors.mapping(PlaybookEnvironment::getEnvironmentId, Collectors.toList())));

  return playbooks.stream()
      .map(p -> new PlaybookResponse(
          p.getId(), p.getProjectId(), p.getName(), p.getDescription(),
          p.getExtraVars(),
          roleIdsMap.getOrDefault(p.getId(), List.of()),
          hostGroupIdsMap.getOrDefault(p.getId(), List.of()),
          tagIdsMap.getOrDefault(p.getId(), List.of()),
          envIdsMap.getOrDefault(p.getId(), List.of()),
          p.getCreatedAt(), p.getUpdatedAt()))
      .toList();
}
```

Remove the old `toResponse(Playbook p)` private method.

Add imports: `java.util.Map`, `java.util.stream.Collectors`

- [ ] **Step 5: Optimize EnvironmentService.listEnvironments**

`EnvironmentService.java` — replace listEnvironments:

```java
@Transactional(readOnly = true)
public List<EnvironmentResponse> listEnvironments(Long projectId, Long userId) {
  accessChecker.checkMembership(projectId, userId);
  List<Environment> environments = environmentRepository.findByProjectIdOrderByIdAsc(projectId);
  if (environments.isEmpty()) {
    return List.of();
  }
  List<Long> envIds = environments.stream().map(Environment::getId).toList();
  Map<Long, List<EnvConfig>> configsMap = new HashMap<>();
  for (Long envId : envIds) {
    configsMap.put(envId, envConfigRepository.findByEnvironmentIdOrderByConfigKeyAsc(envId));
  }
  return environments.stream()
      .map(env -> new EnvironmentResponse(env, configsMap.getOrDefault(env.getId(), List.of())))
      .toList();
}
```

Note: This still does N queries for configs. To fully optimize, add a batch method to EnvConfigRepository:

```java
// EnvConfigRepository.java — add:
List<EnvConfig> findByEnvironmentIdInOrderByEnvironmentIdAscConfigKeyAsc(List<Long> environmentIds);
```

Then use it:
```java
Map<Long, List<EnvConfig>> configsMap = envConfigRepository
    .findByEnvironmentIdInOrderByEnvironmentIdAscConfigKeyAsc(envIds)
    .stream()
    .collect(Collectors.groupingBy(EnvConfig::getEnvironmentId));
```

- [ ] **Step 6: Update service tests to match new method signatures**

Update `ProjectServiceTest`, `ProjectMemberServiceTest`, `PlaybookServiceTest`, `EnvironmentServiceTest` to use the new repository mocks.

- [ ] **Step 7: Run all backend tests**

Run: `cd backend && mvn test -q`
Expected: All tests pass

- [ ] **Step 8: Format and commit**

```bash
cd backend && mvn spotless:apply
git add -A && git commit -m "perf: optimize N+1 queries in project, member, playbook, environment services (H4-H7)"
```

---

### Task 10: H12/H14 前端错误处理补全

**Files:**
- Modify: `frontend/src/pages/role/RoleTasks.tsx:185-189,279-282,284-363`
- Modify: `frontend/src/pages/project/ProjectSettings.tsx:27-44`
- Modify: `frontend/src/pages/playbook/PlaybookBuilder.tsx:135-138`

- [ ] **Step 1: Fix RoleTasks.tsx handleDelete**

```typescript
// Replace:
const handleDelete = async (id: number) => {
  await deleteTask(id);
  message.success('已删除');
  fetchData();
};
// With:
const handleDelete = async (id: number) => {
  try {
    await deleteTask(id);
    message.success('已删除');
    fetchData();
  } catch {
    message.error('删除失败');
  }
};
```

- [ ] **Step 2: Fix RoleTasks.tsx handleCopyYaml**

```typescript
// Replace:
const handleCopyYaml = async () => {
  await navigator.clipboard.writeText(previewYaml);
  message.success('已复制');
};
// With:
const handleCopyYaml = async () => {
  try {
    await navigator.clipboard.writeText(previewYaml);
    message.success('已复制');
  } catch {
    message.error('复制失败');
  }
};
```

- [ ] **Step 3: Fix RoleTasks.tsx handleSubmit — wrap in try/catch**

Add try/catch around the entire handleSubmit body:

```typescript
const handleSubmit = async () => {
  try {
    const values = await form.validateFields();
    // ... existing logic ...
  } catch (error) {
    if (error && typeof error === 'object' && 'errorFields' in error) {
      return; // form validation error, already shown by antd
    }
    message.error('操作失败');
  }
};
```

- [ ] **Step 4: Fix ProjectSettings.tsx handleUpdate**

```typescript
// Replace:
const handleUpdate = async () => {
  const values = await form.validateFields();
  setLoading(true);
  try {
    const updated = await updateProject(Number(id), values);
    setCurrentProject(updated);
    message.success('项目更新成功');
  } finally {
    setLoading(false);
  }
};
// With:
const handleUpdate = async () => {
  try {
    const values = await form.validateFields();
    setLoading(true);
    const updated = await updateProject(Number(id), values);
    setCurrentProject(updated);
    message.success('项目更新成功');
  } catch (error) {
    if (error && typeof error === 'object' && 'errorFields' in error) {
      return;
    }
    message.error('更新失败');
  } finally {
    setLoading(false);
  }
};
```

- [ ] **Step 5: Fix ProjectSettings.tsx handleDelete**

```typescript
// Replace:
const handleDelete = () => {
  deleteProject(Number(id)).then(() => {
    message.success('项目已删除');
    navigate('/projects');
  });
};
// With:
const handleDelete = async () => {
  try {
    await deleteProject(Number(id));
    message.success('项目已删除');
    navigate('/projects');
  } catch {
    message.error('删除失败');
  }
};
```

- [ ] **Step 6: Fix PlaybookBuilder.tsx handleCopyYaml**

```typescript
// Replace:
const handleCopyYaml = () => {
  navigator.clipboard.writeText(yamlPreview);
  message.success('已复制到剪贴板');
};
// With:
const handleCopyYaml = async () => {
  try {
    await navigator.clipboard.writeText(yamlPreview);
    message.success('已复制到剪贴板');
  } catch {
    message.error('复制失败');
  }
};
```

- [ ] **Step 7: Run frontend tests and lint**

Run: `cd frontend && npm run test -- --run && npm run lint`
Expected: All pass

- [ ] **Step 8: Commit**

```bash
cd frontend && npm run format
git add -A && git commit -m "fix: add error handling to frontend submit/delete/clipboard actions (H12, H14)"
```

---

### Task 11: P1#10 JwtTokenProvider / JwtAuthenticationFilter 测试

**Depends on: Task 7 (H8 JWT key cache)**

**Files:**
- Create: `backend/src/test/java/com/ansible/security/JwtAuthenticationFilterTest.java`
- Test: `backend/src/test/java/com/ansible/security/JwtTokenProviderTest.java` (already created in Task 7)

- [ ] **Step 1: Read JwtAuthenticationFilter source**

Read `backend/src/main/java/com/ansible/security/JwtAuthenticationFilter.java` to understand the filter logic.

- [ ] **Step 2: Write JwtAuthenticationFilterTest**

Create `backend/src/test/java/com/ansible/security/JwtAuthenticationFilterTest.java`:

```java
package com.ansible.security;

import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock private JwtTokenProvider tokenProvider;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  @InjectMocks private JwtAuthenticationFilter filter;

  @BeforeEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void doFilterInternal_noAuthorizationHeader_continuesChain() throws Exception {
    when(request.getHeader("Authorization")).thenReturn(null);
    filter.doFilterInternal(request, response, filterChain);
    verify(filterChain).doFilter(request, response);
    verify(tokenProvider, never()).getUserIdFromToken(anyString());
  }

  @Test
  void doFilterInternal_invalidBearerToken_continuesChain() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Bearer invalid");
    when(tokenProvider.validateToken("invalid")).thenReturn(false);
    filter.doFilterInternal(request, response, filterChain);
    verify(filterChain).doFilter(request, response);
    verify(tokenProvider, never()).getUserIdFromToken(anyString());
  }

  @Test
  void doFilterInternal_validBearerToken_setsAuthentication() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
    when(tokenProvider.validateToken("valid-token")).thenReturn(true);
    when(tokenProvider.getUserIdFromToken("valid-token")).thenReturn(42L);
    filter.doFilterInternal(request, response, filterChain);
    verify(filterChain).doFilter(request, response);
    var auth = SecurityContextHolder.getContext().getAuthentication();
    assertThat(auth).isNotNull();
    assertThat(auth.getPrincipal()).isEqualTo(42L);
  }

  @Test
  void doFilterInternal_nonBearerHeader_continuesChain() throws Exception {
    when(request.getHeader("Authorization")).thenReturn("Basic abc123");
    filter.doFilterInternal(request, response, filterChain);
    verify(filterChain).doFilter(request, response);
  }
}
```

Note: Adjust the test to match the actual JwtAuthenticationFilter implementation after reading it.

- [ ] **Step 3: Run security tests**

Run: `cd backend && mvn test -Dtest="JwtTokenProviderTest,JwtAuthenticationFilterTest" -pl . -q`
Expected: PASS

- [ ] **Step 4: Format and commit**

```bash
cd backend && mvn spotless:apply
git add -A && git commit -m "test: add JwtTokenProvider and JwtAuthenticationFilter unit tests (P1#10)"
```

---

## P2 — 计划改进

### Task 12: H10 路由级代码分割

**Files:**
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: Convert static imports to React.lazy**

`frontend/src/App.tsx` — replace entire file:

```typescript
import { lazy, Suspense } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import MainLayout from "./components/Layout/MainLayout";
import ProjectLayout from "./components/Layout/ProjectLayout";
import { useAuthStore } from "./stores/authStore";
import { Spin } from "antd";

const Login = lazy(() => import("./pages/auth/Login"));
const Register = lazy(() => import("./pages/auth/Register"));
const ProjectList = lazy(() => import("./pages/project/ProjectList"));
const ProjectSettings = lazy(() => import("./pages/project/ProjectSettings"));
const MemberManagement = lazy(() => import("./pages/project/MemberManagement"));
const HostGroupManager = lazy(() => import("./pages/host/HostGroupManager"));
const RoleList = lazy(() => import("./pages/role/RoleList"));
const RoleDetail = lazy(() => import("./pages/role/RoleDetail"));
const TagManager = lazy(() => import("./pages/tag/TagManager"));
const EnvironmentManager = lazy(() => import("./pages/environment/EnvironmentManager"));
const VariableManager = lazy(() => import("./pages/variable/VariableManager"));
const PlaybookList = lazy(() => import("./pages/playbook/PlaybookList"));
const PlaybookBuilder = lazy(() => import("./pages/playbook/PlaybookBuilder"));

function RequireAuth({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return <>{children}</>;
}

function PageLoader() {
  return (
    <div style={{ display: "flex", justifyContent: "center", padding: "100px 0" }}>
      <Spin size="large" />
    </div>
  );
}

export default function App() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/" element={<RequireAuth><MainLayout /></RequireAuth>}>
          <Route index element={<Navigate to="/projects" replace />} />
          <Route path="projects" element={<ProjectList />} />
          <Route path="projects/:id" element={<ProjectLayout />}>
            <Route path="settings" element={<ProjectSettings />} />
            <Route path="members" element={<MemberManagement />} />
            <Route path="host-groups" element={<HostGroupManager />} />
            <Route path="roles" element={<RoleList />} />
            <Route path="roles/:roleId" element={<RoleDetail />} />
            <Route path="tags" element={<TagManager />} />
            <Route path="environments" element={<EnvironmentManager />} />
            <Route path="variables" element={<VariableManager />} />
            <Route path="playbooks" element={<PlaybookList />} />
            <Route path="playbooks/:pbId" element={<PlaybookBuilder />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}
```

- [ ] **Step 2: Run build to verify code splitting works**

Run: `cd frontend && npm run build`
Expected: Build succeeds, multiple chunks generated

- [ ] **Step 3: Run tests**

Run: `cd frontend && npm run test -- --run && npm run lint`
Expected: All pass

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "perf: add route-level code splitting with React.lazy (H10)"
```

---

### Task 13: H11 401/403 改用 React Router 导航

**Files:**
- Modify: `frontend/src/api/request.ts:16-25`
- Create: `frontend/src/api/navigate.ts`
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: Create navigate.ts for external navigate injection**

Create `frontend/src/api/navigate.ts`:

```typescript
import type { NavigateFunction } from "react-router-dom";

let navigate: NavigateFunction | null = null;

export function setNavigate(n: NavigateFunction) {
  navigate = n;
}

export function getNavigate(): NavigateFunction | null {
  return navigate;
}
```

- [ ] **Step 2: Modify request.ts to use React Router navigation**

`frontend/src/api/request.ts` — replace error interceptor:

```typescript
import axios from "axios";
import { getNavigate } from "./navigate";

const request = axios.create({
  baseURL: "/api",
  timeout: 10000,
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 403 || error.response?.status === 401) {
      localStorage.removeItem("token");
      const navigate = getNavigate();
      if (navigate) {
        navigate("/login", { replace: true });
      } else {
        window.location.href = "/login";
      }
    }
    return Promise.reject(error.response?.data ?? error);
  }
);

export default request;
```

- [ ] **Step 3: Inject navigate in App.tsx**

`frontend/src/App.tsx` — add navigate injection:

```typescript
import { useNavigate } from "react-router-dom";
import { setNavigate } from "./api/navigate";

// Inside the App component, add:
function AppContent() {
  const navigate = useNavigate();
  setNavigate(navigate);
  return ( /* ... existing routes ... */ );
}

// Wrap with BrowserRouter if not already wrapped, or ensure useNavigate is available
```

Since App.tsx already renders `<Routes>`, it must be inside a `Router` provider. Add `setNavigate` call at the top of the App component or in a dedicated initialization component.

- [ ] **Step 4: Run tests**

Run: `cd frontend && npm run test -- --run`
Expected: All pass

- [ ] **Step 5: Commit**

```bash
cd frontend && npm run format
git add -A && git commit -m "fix: use React Router navigation for 401/403 instead of hard redirect (H11)"
```

---

### Task 14: H9 下载接口认证

**Files:**
- Modify: `frontend/src/api/template.ts:32-34`
- Modify: `frontend/src/api/roleFile.ts:29-31`
- Modify: `frontend/src/pages/role/RoleTemplates.tsx` (download callers)
- Modify: `frontend/src/pages/role/RoleFiles.tsx` (download callers, if applicable)

- [ ] **Step 1: Replace download URL functions with fetch-based downloads**

`frontend/src/api/template.ts` — replace download function:

```typescript
export async function downloadTemplate(id: number): Promise<void> {
  const token = localStorage.getItem("token");
  const response = await fetch(`/api/templates/${id}/download`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) throw new Error("Download failed");
  const blob = await response.blob();
  const contentDisposition = response.headers.get("Content-Disposition");
  const fileName = contentDisposition
    ? contentDisposition.split("filename=")[1]?.replace(/"/g, "")
    : `template-${id}`;
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = fileName;
  a.click();
  URL.revokeObjectURL(url);
}
```

`frontend/src/api/roleFile.ts` — replace download function similarly:

```typescript
export async function downloadFile(id: number): Promise<void> {
  const token = localStorage.getItem("token");
  const response = await fetch(`/api/files/${id}/download`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) throw new Error("Download failed");
  const blob = await response.blob();
  const contentDisposition = response.headers.get("Content-Disposition");
  const fileName = contentDisposition
    ? contentDisposition.split("filename=")[1]?.replace(/"/g, "")
    : `file-${id}`;
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = fileName;
  a.click();
  URL.revokeObjectURL(url);
}
```

- [ ] **Step 2: Update callers to use async download**

Find all callers of `getTemplateDownloadUrl` and `getFileDownloadUrl`, replace:

```typescript
// Old: window.open(getTemplateDownloadUrl(id))
// New:
try {
  await downloadTemplate(id);
} catch {
  message.error('下载失败');
}
```

- [ ] **Step 3: Run tests**

Run: `cd frontend && npm run test -- --run && npm run lint`
Expected: All pass

- [ ] **Step 4: Commit**

```bash
cd frontend && npm run format
git add -A && git commit -m "fix: use fetch with JWT header for file downloads instead of URL (H9)"
```

---

### Task 15: H13 重复代码抽取 — buildArgsJson/parseArgsToForm

**Files:**
- Create: `frontend/src/utils/argsParser.ts`
- Modify: `frontend/src/pages/role/RoleTasks.tsx:21-71`
- Modify: `frontend/src/pages/role/RoleHandlers.tsx:16-66`
- Modify: `frontend/src/components/role/BlockTasksEditor.tsx:490-539`

- [ ] **Step 1: Create shared utils/argsParser.ts**

Create `frontend/src/utils/argsParser.ts`:

```typescript
import { getModuleDefinition } from "./moduleSelect";

interface ArgsForm {
  moduleParams: Record<string, unknown>;
  extraParams: { key: string; value: string }[];
}

export function buildArgsJson(
  moduleParams: Record<string, unknown> | undefined,
  extraParams: { key: string; value: string }[] | undefined,
): string {
  const result: Record<string, unknown> = {};
  if (moduleParams) {
    for (const [k, v] of Object.entries(moduleParams)) {
      if (v !== undefined && v !== "" && v !== null) {
        result[k] = v;
      }
    }
  }
  if (extraParams) {
    for (const item of extraParams) {
      if (item.key) {
        result[item.key] = item.value;
      }
    }
  }
  return Object.keys(result).length > 0 ? JSON.stringify(result) : "";
}

export function parseArgsToForm(
  argsJson: string | undefined,
  moduleName: string | undefined,
): ArgsForm {
  const moduleParams: Record<string, unknown> = {};
  const extraParams: { key: string; value: string }[] = [];
  if (!argsJson) return { moduleParams, extraParams };
  let parsed: Record<string, unknown>;
  try {
    parsed = JSON.parse(argsJson);
  } catch {
    extraParams.push({ key: "", value: argsJson });
    return { moduleParams, extraParams };
  }
  const moduleDef = moduleName ? getModuleDefinition(moduleName) : undefined;
  const knownParams = new Set(moduleDef?.params.map((p) => p.name) ?? []);
  for (const [k, v] of Object.entries(parsed)) {
    if (knownParams.has(k)) {
      moduleParams[k] = v;
    } else {
      extraParams.push({ key: k, value: String(v) });
    }
  }
  return { moduleParams, extraParams };
}
```

- [ ] **Step 2: Replace local functions in RoleTasks.tsx**

Delete local `buildArgsJson` and `parseArgsToForm` functions (lines 21-71).
Add import: `import { buildArgsJson, parseArgsToForm } from "../../../utils/argsParser";`

- [ ] **Step 3: Replace local functions in RoleHandlers.tsx**

Same — delete local functions, add import.

- [ ] **Step 4: Replace local functions in BlockTasksEditor.tsx**

Same — delete local functions, add import.

- [ ] **Step 5: Run tests**

Run: `cd frontend && npm run test -- --run && npm run lint`
Expected: All pass

- [ ] **Step 6: Commit**

```bash
cd frontend && npm run format
git add -A && git commit -m "refactor: extract buildArgsJson/parseArgsToForm to shared utils (H13)"
```

---

### Task 16: P2 前端 Store 测试 + API 层测试补全

**Files:**
- Create: `frontend/src/stores/__tests__/authStore.test.ts`
- Create: `frontend/src/stores/__tests__/projectStore.test.ts`

- [ ] **Step 1: Write authStore test**

Create `frontend/src/stores/__tests__/authStore.test.ts`:

```typescript
import { describe, it, expect, vi, beforeEach } from "vitest";

vi.mock("../../api/auth", () => ({
  authApi: { me: vi.fn() },
}));

import { useAuthStore } from "../authStore";
import { authApi } from "../../api/auth";

describe("authStore", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
    useAuthStore.setState({
      user: null,
      token: null,
      isAuthenticated: false,
      loading: false,
    });
  });

  it("login sets token and user", () => {
    const store = useAuthStore.getState();
    store.login("jwt-token", { id: 1, username: "test", email: "t@t.com" } as any);
    const state = useAuthStore.getState();
    expect(state.token).toBe("jwt-token");
    expect(state.isAuthenticated).toBe(true);
    expect(state.user?.username).toBe("test");
    expect(localStorage.getItem("token")).toBe("jwt-token");
  });

  it("logout clears state and localStorage", () => {
    useAuthStore.setState({ token: "old", isAuthenticated: true });
    localStorage.setItem("token", "old");
    useAuthStore.getState().logout();
    const state = useAuthStore.getState();
    expect(state.token).toBeNull();
    expect(state.isAuthenticated).toBe(false);
    expect(state.user).toBeNull();
    expect(localStorage.getItem("token")).toBeNull();
  });

  it("fetchUser calls authApi.me and sets user", async () => {
    const mockUser = { id: 1, username: "test", email: "t@t.com" };
    vi.mocked(authApi.me).mockResolvedValue(mockUser as any);
    useAuthStore.setState({ token: "valid-token", user: null });
    await useAuthStore.getState().fetchUser();
    expect(useAuthStore.getState().user).toEqual(mockUser);
  });

  it("fetchUser skips when no token", async () => {
    useAuthStore.setState({ token: null, user: null });
    await useAuthStore.getState().fetchUser();
    expect(authApi.me).not.toHaveBeenCalled();
  });

  it("fetchUser clears auth on API error", async () => {
    vi.mocked(authApi.me).mockRejectedValue(new Error("fail"));
    useAuthStore.setState({ token: "bad-token", user: null });
    await useAuthStore.getState().fetchUser();
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(localStorage.getItem("token")).toBeNull();
  });
});
```

- [ ] **Step 2: Write projectStore test**

Create `frontend/src/stores/__tests__/projectStore.test.ts`:

```typescript
import { describe, it, expect, beforeEach } from "vitest";
import { useProjectStore } from "../projectStore";

describe("projectStore", () => {
  beforeEach(() => {
    useProjectStore.setState({ currentProject: null });
  });

  it("sets current project", () => {
    const project = { id: 1, name: "Test" } as any;
    useProjectStore.getState().setCurrentProject(project);
    expect(useProjectStore.getState().currentProject).toEqual(project);
  });

  it("clears current project with null", () => {
    useProjectStore.setState({ currentProject: { id: 1 } as any });
    useProjectStore.getState().setCurrentProject(null);
    expect(useProjectStore.getState().currentProject).toBeNull();
  });
});
```

- [ ] **Step 3: Run store tests**

Run: `cd frontend && npx vitest run src/stores`
Expected: All tests pass

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "test: add authStore and projectStore unit tests (P2)"
```

---

## P3 — 有余力时处理

### Task 17: M1 安全审计日志

**Files:**
- Create: `backend/src/main/java/com/ansible/security/AuditLogService.java`
- Modify: `backend/src/main/java/com/ansible/security/JwtTokenProvider.java` (validateToken)
- Modify: `backend/src/main/java/com/ansible/security/JwtAuthenticationFilter.java` (log failures)
- Modify: `backend/src/main/java/com/ansible/user/service/AuthService.java` (log login failures)

- [ ] **Step 1: Create AuditLogService**

```java
package com.ansible.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
  private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

  public void logLoginFailure(String username, String reason) {
    log.warn("Login failed for user '{}': {}", username, reason);
  }

  public void logTokenValidationFailure(String reason) {
    log.warn("Token validation failed: {}", reason);
  }

  public void logAccessDenied(Long userId, String resource, Long resourceId) {
    log.warn("Access denied: userId={}, resource={}, resourceId={}", userId, resource, resourceId);
  }
}
```

- [ ] **Step 2: Integrate into JwtTokenProvider.validateToken**

```java
private final AuditLogService auditLogService;

public boolean validateToken(String token) {
  try {
    Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
    return true;
  } catch (JwtException | IllegalArgumentException e) {
    auditLogService.logTokenValidationFailure(e.getMessage());
    return false;
  }
}
```

- [ ] **Step 3: Integrate into AuthService login failure path**

Find the login method and add audit logging on authentication failure.

- [ ] **Step 4: Run tests**

Run: `cd backend && mvn test -q`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
cd backend && mvn spotless:apply
git add -A && git commit -m "feat: add security audit logging (M1)"
```

---

### Task 18: M10 数据库索引

**Files:**
- Modify: Entity files for `Task`, `Handler`, `Template`, `RoleFile`, `RoleVariable`, `RoleDefaultVariable`, `Host`, `Variable`, `TaskTag`, `PlaybookRole`, `PlaybookTag`, `PlaybookHostGroup`, `PlaybookEnvironment`

- [ ] **Step 1: Add @Index annotations to entities**

For each entity, add `@Table(indexes = {...})` or add to existing:

```java
// Task.java:
@Table(name = "tasks", indexes = @Index(name = "idx_task_role_id", columnList = "role_id"))

// Handler.java:
@Table(name = "handlers", indexes = @Index(name = "idx_handler_role_id", columnList = "role_id"))

// Template.java — add to existing unique constraint:
@Table(name = "templates", uniqueConstraints = {...}, indexes = @Index(name = "idx_template_role_id", columnList = "role_id"))

// RoleFile.java — same pattern:
indexes = @Index(name = "idx_role_file_role_id", columnList = "role_id")

// RoleVariable.java:
indexes = @Index(name = "idx_role_var_role_id", columnList = "role_id")

// RoleDefaultVariable.java:
indexes = @Index(name = "idx_role_default_var_role_id", columnList = "role_id")

// Host.java:
indexes = @Index(name = "idx_host_group_id", columnList = "host_group_id")

// Variable.java — add to existing unique constraint:
indexes = @Index(name = "idx_variable_scope_scope_id", columnList = "scope, scope_id")

// TaskTag.java:
indexes = {@Index(name = "idx_task_tag_task_id", columnList = "task_id"),
           @Index(name = "idx_task_tag_tag_id", columnList = "tag_id")}

// PlaybookRole.java:
indexes = @Index(name = "idx_playbook_role_playbook_id", columnList = "playbook_id")
```

- [ ] **Step 2: Run integration tests to verify schema creation**

Run: `cd backend && mvn verify -q`
Expected: PASS (Hibernate auto-creates indexes with ddl-auto: update)

- [ ] **Step 3: Commit**

```bash
cd backend && mvn spotless:apply
git add -A && git commit -m "perf: add database indexes for high-frequency query columns (M10)"
```

---

### Task 19: M14 路径解析改用 useParams

**Files:**
- Modify: `frontend/src/components/Layout/ProjectLayout.tsx:55-56`

- [ ] **Step 1: Replace hardcoded path parsing with route-aware detection**

`frontend/src/components/Layout/ProjectLayout.tsx` — replace the path parsing logic:

```typescript
// Replace lines 55-56:
//   const pathParts = location.pathname.split('/');
//   const currentKey = pathParts[3] || 'roles';
// With:
const currentKey = (() => {
  const path = location.pathname;
  if (path.includes('/settings')) return 'settings';
  if (path.includes('/members')) return 'members';
  if (path.includes('/host-groups')) return 'host-groups';
  if (path.includes('/playbooks')) return 'playbooks';
  if (path.includes('/tags')) return 'tags';
  if (path.includes('/environments')) return 'environments';
  if (path.includes('/variables')) return 'variables';
  if (path.includes('/roles')) return 'roles';
  return 'roles';
})();
```

- [ ] **Step 2: Run frontend tests**

Run: `cd frontend && npm run test -- --run`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "fix: replace hardcoded path index with route-aware key detection (M14)"
```

---

### Task 20: M16 Modal confirmLoading

**Files:**
- Modify: `frontend/src/pages/role/RoleList.tsx`
- Modify: `frontend/src/pages/tag/TagManager.tsx`
- Modify: Other modal components that lack confirmLoading

- [ ] **Step 1: Add confirmLoading state to RoleList**

In the modal's submit handler, wrap with loading state:

```typescript
const [confirmLoading, setConfirmLoading] = useState(false);

const handleSubmit = async () => {
  setConfirmLoading(true);
  try {
    // ... existing submit logic
  } catch {
    message.error('操作失败');
  } finally {
    setConfirmLoading(false);
  }
};

// In Modal: <Modal ... confirmLoading={confirmLoading} ... >
```

- [ ] **Step 2: Same for TagManager and other modals**

Apply the same pattern to TagManager and any other modal that lacks confirmLoading.

- [ ] **Step 3: Run frontend tests**

Run: `cd frontend && npm run test -- --run`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
cd frontend && npm run format
git add -A && git commit -m "fix: add confirmLoading to modal submit buttons (M16)"
```

---

### Task 21: 可访问性改进

**Files:**
- Modify: Interactive div elements across frontend components

- [ ] **Step 1: Add role/tabIndex/onKeyDown to clickable divs**

Search for clickable `<div` elements that lack `role="button"`:
```bash
cd frontend && grep -rn "onClick" src/ --include="*.tsx" | grep "<div" | head -20
```

For each interactive div, add:
```typescript
<div
  role="button"
  tabIndex={0}
  onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onClick(); } }}
  onClick={onClick}
>
```

- [ ] **Step 2: Run lint and tests**

Run: `cd frontend && npm run lint && npm run test -- --run`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "a11y: add role/tabIndex/onKeyDown to interactive divs (L5)"
```

---

## P0-P3 全量验证

### Task 22: 全量回归验证

- [ ] **Step 1: Run full backend test suite**

Run: `cd backend && mvn test -q`
Expected: All tests pass

- [ ] **Step 2: Run backend quality gates**

Run: `cd backend && mvn spotless:check checkstyle:check pmd:check spotbugs:check -q`
Expected: All checks pass

- [ ] **Step 3: Run full frontend test suite**

Run: `cd frontend && npm run test -- --run`
Expected: All tests pass

- [ ] **Step 4: Run frontend quality gates**

Run: `cd frontend && npm run lint && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 5: Start dev servers and verify**

Run: `cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev` (in background)
Run: `cd frontend && npm run dev` (in background)

Manually verify:
- Login/register works
- Project CRUD works
- Role/Task/Handler CRUD works
- Template/RoleFile download works
- Playbook CRUD works
- Variable management works
- Tag/Environment management works
- 401 redirect works (test by removing token in localStorage)
