# Phase 4: Task + Handler CRUD — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Task CRUD and Handler CRUD within the `role/` module, including the Task→Handler `notify` relationship, with full backend tests and frontend UI in the RoleDetail tabs.

**Architecture:** Task and Handler are child entities of Role, living in `com.ansible.role/`. Task has a `notify` field (JSON array of Handler names). Handler has a read-only "notified by" lookup. Frontend adds `RoleTasks.tsx` and `RoleHandlers.tsx` tab components to the existing `RoleDetail.tsx`.

**Tech Stack:** Java 21, Spring Boot 3.3.5, Spring Data JPA, PostgreSQL, Testcontainers, JUnit 5, Mockito, React 18, TypeScript, Ant Design 5, Zustand

---

## File Map

### Backend — New Files

| File | Responsibility |
|------|---------------|
| `backend/src/main/java/com/ansible/role/entity/Task.java` | Task JPA entity (extends BaseEntity) |
| `backend/src/main/java/com/ansible/role/entity/Handler.java` | Handler JPA entity (extends BaseEntity) |
| `backend/src/main/java/com/ansible/role/repository/TaskRepository.java` | Spring Data repository for Task |
| `backend/src/main/java/com/ansible/role/repository/HandlerRepository.java` | Spring Data repository for Handler |
| `backend/src/main/java/com/ansible/role/dto/CreateTaskRequest.java` | DTO: name, module, args, when, loop, until, register, notify, order |
| `backend/src/main/java/com/ansible/role/dto/UpdateTaskRequest.java` | DTO: same fields, all optional |
| `backend/src/main/java/com/ansible/role/dto/TaskResponse.java` | DTO: all fields + id, roleId, createdBy, createdAt |
| `backend/src/main/java/com/ansible/role/dto/CreateHandlerRequest.java` | DTO: name, module, args, when, register |
| `backend/src/main/java/com/ansible/role/dto/UpdateHandlerRequest.java` | DTO: same fields, all optional |
| `backend/src/main/java/com/ansible/role/dto/HandlerResponse.java` | DTO: all fields + id, roleId, createdBy, createdAt |
| `backend/src/main/java/com/ansible/role/service/TaskService.java` | Task CRUD business logic |
| `backend/src/main/java/com/ansible/role/service/HandlerService.java` | Handler CRUD business logic |
| `backend/src/main/java/com/ansible/role/controller/TaskController.java` | REST: `/api/roles/{roleId}/tasks` + `/api/tasks/{id}` |
| `backend/src/main/java/com/ansible/role/controller/HandlerController.java` | REST: `/api/roles/{roleId}/handlers` + `/api/handlers/{id}` |
| `backend/src/test/java/com/ansible/role/service/TaskServiceTest.java` | Unit tests |
| `backend/src/test/java/com/ansible/role/service/HandlerServiceTest.java` | Unit tests |
| `backend/src/test/java/com/ansible/role/controller/TaskControllerTest.java` | Integration tests |
| `backend/src/test/java/com/ansible/role/controller/HandlerControllerTest.java` | Integration tests |

### Frontend — New/Modified Files

| File | Responsibility |
|------|---------------|
| `frontend/src/types/entity/Task.ts` | Task, Handler, request/response TypeScript interfaces |
| `frontend/src/api/task.ts` | Task API functions |
| `frontend/src/api/handler.ts` | Handler API functions |
| `frontend/src/pages/role/RoleTasks.tsx` | Tasks tab content — table with create/edit/delete |
| `frontend/src/pages/role/RoleHandlers.tsx` | Handlers tab content — table with create/edit/delete |
| `frontend/src/pages/role/RoleDetail.tsx` | **Modify:** replace ComingSoon placeholders for Tasks and Handlers tabs |

---

## Task 1: Task Entity + Repository

**Files:**
- Create: `backend/src/main/java/com/ansible/role/entity/Task.java`
- Create: `backend/src/main/java/com/ansible/role/repository/TaskRepository.java`

- [ ] **Step 1: Create Task entity**

```java
package com.ansible.role.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
public class Task extends BaseEntity {

  @Column(nullable = false)
  private Long roleId;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false, length = 100)
  private String module;

  @Column(columnDefinition = "TEXT")
  private String args;

  @Column(name = "when_condition", length = 500)
  private String whenCondition;

  @Column(length = 500)
  private String loop;

  @Column(length = 500)
  private String until;

  @Column(length = 100)
  private String register;

  @Column(columnDefinition = "TEXT")
  private String notify;

  @Column(nullable = false)
  private Integer taskOrder;
}
```

- [ ] **Step 2: Create TaskRepository**

```java
package com.ansible.role.repository;

import com.ansible.role.entity.Task;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

  List<Task> findAllByRoleIdOrderByTaskOrderAsc(Long roleId);
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/ansible/role/entity/Task.java backend/src/main/java/com/ansible/role/repository/TaskRepository.java
git commit -m "feat: add Task entity and repository"
```

---

## Task 2: Task DTOs

**Files:**
- Create: `backend/src/main/java/com/ansible/role/dto/CreateTaskRequest.java`
- Create: `backend/src/main/java/com/ansible/role/dto/UpdateTaskRequest.java`
- Create: `backend/src/main/java/com/ansible/role/dto/TaskResponse.java`

- [ ] **Step 1: Create CreateTaskRequest**

```java
package com.ansible.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTaskRequest {

  @NotBlank(message = "Task name is required")
  @Size(max = 200, message = "Name must not exceed 200 characters")
  private String name;

  @NotBlank(message = "Module is required")
  @Size(max = 100, message = "Module must not exceed 100 characters")
  private String module;

  private String args;

  @Size(max = 500, message = "When condition must not exceed 500 characters")
  private String whenCondition;

  @Size(max = 500, message = "Loop must not exceed 500 characters")
  private String loop;

  @Size(max = 500, message = "Until must not exceed 500 characters")
  private String until;

  @Size(max = 100, message = "Register must not exceed 100 characters")
  private String register;

  private List<String> notify;

  @NotNull(message = "Task order is required")
  private Integer taskOrder;
}
```

- [ ] **Step 2: Create UpdateTaskRequest**

```java
package com.ansible.role.dto;

import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTaskRequest {

  @Size(max = 200, message = "Name must not exceed 200 characters")
  private String name;

  @Size(max = 100, message = "Module must not exceed 100 characters")
  private String module;

  private String args;

  @Size(max = 500, message = "When condition must not exceed 500 characters")
  private String whenCondition;

  @Size(max = 500, message = "Loop must not exceed 500 characters")
  private String loop;

  @Size(max = 500, message = "Until must not exceed 500 characters")
  private String until;

  @Size(max = 100, message = "Register must not exceed 100 characters")
  private String register;

  private List<String> notify;

  private Integer taskOrder;
}
```

- [ ] **Step 3: Create TaskResponse**

```java
package com.ansible.role.dto;

import com.ansible.role.entity.Task;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

@Getter
public class TaskResponse {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final Long id;
  private final Long roleId;
  private final String name;
  private final String module;
  private final String args;
  private final String whenCondition;
  private final String loop;
  private final String until;
  private final String register;
  private final List<String> notify;
  private final Integer taskOrder;
  private final Long createdBy;
  private final LocalDateTime createdAt;

  @JsonCreator
  public TaskResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("roleId") Long roleId,
      @JsonProperty("name") String name,
      @JsonProperty("module") String module,
      @JsonProperty("args") String args,
      @JsonProperty("whenCondition") String whenCondition,
      @JsonProperty("loop") String loop,
      @JsonProperty("until") String until,
      @JsonProperty("register") String register,
      @JsonProperty("notify") List<String> notify,
      @JsonProperty("taskOrder") Integer taskOrder,
      @JsonProperty("createdBy") Long createdBy,
      @JsonProperty("createdAt") LocalDateTime createdAt) {
    this.id = id;
    this.roleId = roleId;
    this.name = name;
    this.module = module;
    this.args = args;
    this.whenCondition = whenCondition;
    this.loop = loop;
    this.until = until;
    this.register = register;
    this.notify = notify;
    this.taskOrder = taskOrder;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
  }

  public TaskResponse(Task task) {
    this.id = task.getId();
    this.roleId = task.getRoleId();
    this.name = task.getName();
    this.module = task.getModule();
    this.args = task.getArgs();
    this.whenCondition = task.getWhenCondition();
    this.loop = task.getLoop();
    this.until = task.getUntil();
    this.register = task.getRegister();
    this.notify = parseNotify(task.getNotify());
    this.taskOrder = task.getTaskOrder();
    this.createdBy = task.getCreatedBy();
    this.createdAt = task.getCreatedAt();
  }

  private static List<String> parseNotify(String notifyJson) {
    if (notifyJson == null || notifyJson.isBlank()) {
      return List.of();
    }
    try {
      return MAPPER.readValue(notifyJson, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      return List.of();
    }
  }
}
```

- [ ] **Step 4: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/role/dto/CreateTaskRequest.java backend/src/main/java/com/ansible/role/dto/UpdateTaskRequest.java backend/src/main/java/com/ansible/role/dto/TaskResponse.java
git commit -m "feat: add Task DTOs"
```

---

## Task 3: TaskService + Unit Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/role/service/TaskService.java`
- Create: `backend/src/test/java/com/ansible/role/service/TaskServiceTest.java`

- [ ] **Step 1: Create TaskService**

```java
package com.ansible.role.service;

import com.ansible.role.dto.CreateTaskRequest;
import com.ansible.role.dto.TaskResponse;
import com.ansible.role.dto.UpdateTaskRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.Task;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.TaskRepository;
import com.ansible.security.ProjectAccessChecker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TaskService {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final TaskRepository taskRepository;
  private final RoleRepository roleRepository;
  private final ProjectAccessChecker accessChecker;

  @Transactional
  public TaskResponse createTask(Long roleId, CreateTaskRequest request, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);

    Task task = new Task();
    task.setRoleId(roleId);
    task.setName(request.getName());
    task.setModule(request.getModule());
    task.setArgs(request.getArgs());
    task.setWhenCondition(request.getWhenCondition());
    task.setLoop(request.getLoop());
    task.setUntil(request.getUntil());
    task.setRegister(request.getRegister());
    task.setNotify(toJson(request.getNotify()));
    task.setTaskOrder(request.getTaskOrder());
    task.setCreatedBy(currentUserId);
    Task saved = taskRepository.save(task);
    return new TaskResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<TaskResponse> getTasksByRole(Long roleId, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    return taskRepository.findAllByRoleIdOrderByTaskOrderAsc(roleId).stream()
        .map(TaskResponse::new)
        .toList();
  }

  @Transactional(readOnly = true)
  public TaskResponse getTask(Long taskId, Long currentUserId) {
    Task task =
        taskRepository
            .findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));
    Role role =
        roleRepository
            .findById(task.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    return new TaskResponse(task);
  }

  @Transactional
  public TaskResponse updateTask(Long taskId, UpdateTaskRequest request, Long currentUserId) {
    Task task =
        taskRepository
            .findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));
    Role role =
        roleRepository
            .findById(task.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), task.getCreatedBy(), currentUserId);

    if (StringUtils.hasText(request.getName())) {
      task.setName(request.getName());
    }
    if (StringUtils.hasText(request.getModule())) {
      task.setModule(request.getModule());
    }
    if (request.getArgs() != null) {
      task.setArgs(request.getArgs());
    }
    if (request.getWhenCondition() != null) {
      task.setWhenCondition(request.getWhenCondition());
    }
    if (request.getLoop() != null) {
      task.setLoop(request.getLoop());
    }
    if (request.getUntil() != null) {
      task.setUntil(request.getUntil());
    }
    if (request.getRegister() != null) {
      task.setRegister(request.getRegister());
    }
    if (request.getNotify() != null) {
      task.setNotify(toJson(request.getNotify()));
    }
    if (request.getTaskOrder() != null) {
      task.setTaskOrder(request.getTaskOrder());
    }
    Task saved = taskRepository.save(task);
    return new TaskResponse(saved);
  }

  @Transactional
  public void deleteTask(Long taskId, Long currentUserId) {
    Task task =
        taskRepository
            .findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));
    Role role =
        roleRepository
            .findById(task.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), task.getCreatedBy(), currentUserId);
    taskRepository.delete(task);
  }

  private String toJson(List<String> list) {
    if (list == null || list.isEmpty()) {
      return null;
    }
    try {
      return MAPPER.writeValueAsString(list);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Invalid notify list", e);
    }
  }
}
```

- [ ] **Step 2: Create TaskServiceTest**

```java
package com.ansible.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.role.dto.CreateTaskRequest;
import com.ansible.role.dto.TaskResponse;
import com.ansible.role.dto.UpdateTaskRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.Task;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.TaskRepository;
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
class TaskServiceTest {

  @Mock private TaskRepository taskRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private ProjectAccessChecker accessChecker;
  @InjectMocks private TaskService taskService;

  private Role testRole;
  private Task testTask;

  @BeforeEach
  void setUp() {
    testRole = new Role();
    ReflectionTestUtils.setField(testRole, "id", 1L);
    testRole.setProjectId(10L);
    testRole.setName("nginx");
    testRole.setCreatedBy(10L);

    testTask = new Task();
    ReflectionTestUtils.setField(testTask, "id", 1L);
    testTask.setRoleId(1L);
    testTask.setName("Install nginx");
    testTask.setModule("apt");
    testTask.setArgs("{\"name\":\"nginx\",\"state\":\"present\"}");
    testTask.setNotify("[\"Restart nginx\"]");
    testTask.setTaskOrder(1);
    testTask.setCreatedBy(10L);
    ReflectionTestUtils.setField(testTask, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(testTask, "updatedAt", LocalDateTime.now());
  }

  @Test
  void createTask_success() {
    CreateTaskRequest request = new CreateTaskRequest();
    request.setName("Install nginx");
    request.setModule("apt");
    request.setArgs("{\"name\":\"nginx\",\"state\":\"present\"}");
    request.setNotify(List.of("Restart nginx"));
    request.setTaskOrder(1);

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(taskRepository.save(any(Task.class))).thenReturn(testTask);

    TaskResponse response = taskService.createTask(1L, request, 10L);

    assertThat(response.getName()).isEqualTo("Install nginx");
    assertThat(response.getModule()).isEqualTo("apt");
    assertThat(response.getNotify()).containsExactly("Restart nginx");
    verify(taskRepository).save(any(Task.class));
  }

  @Test
  void createTask_roleNotFound() {
    CreateTaskRequest request = new CreateTaskRequest();
    request.setName("Install nginx");
    request.setModule("apt");
    request.setTaskOrder(1);

    when(roleRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> taskService.createTask(99L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Role not found");
  }

  @Test
  void getTasksByRole_success() {
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(taskRepository.findAllByRoleIdOrderByTaskOrderAsc(1L)).thenReturn(List.of(testTask));

    List<TaskResponse> tasks = taskService.getTasksByRole(1L, 10L);

    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("Install nginx");
  }

  @Test
  void getTask_success() {
    when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    TaskResponse response = taskService.getTask(1L, 10L);

    assertThat(response.getName()).isEqualTo("Install nginx");
  }

  @Test
  void updateTask_success() {
    UpdateTaskRequest request = new UpdateTaskRequest();
    request.setName("Install apache");

    when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(taskRepository.save(any(Task.class))).thenReturn(testTask);

    taskService.updateTask(1L, request, 10L);

    verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
    verify(taskRepository).save(any(Task.class));
  }

  @Test
  void deleteTask_success() {
    when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    taskService.deleteTask(1L, 10L);

    verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
    verify(taskRepository).delete(testTask);
  }
}
```

- [ ] **Step 3: Run unit tests**

Run: `cd backend && mvn test -Dtest=TaskServiceTest -pl .`
Expected: All 5 tests PASS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/ansible/role/service/TaskService.java backend/src/test/java/com/ansible/role/service/TaskServiceTest.java
git commit -m "feat: add TaskService with unit tests"
```

---

## Task 4: TaskController + Integration Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/role/controller/TaskController.java`
- Create: `backend/src/test/java/com/ansible/role/controller/TaskControllerTest.java`

- [ ] **Step 1: Create TaskController**

```java
package com.ansible.role.controller;

import com.ansible.common.Result;
import com.ansible.role.dto.CreateTaskRequest;
import com.ansible.role.dto.TaskResponse;
import com.ansible.role.dto.UpdateTaskRequest;
import com.ansible.role.service.TaskService;
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
public class TaskController {

  private final TaskService taskService;

  @PostMapping("/roles/{roleId}/tasks")
  public Result<TaskResponse> createTask(
      @PathVariable Long roleId,
      @Valid @RequestBody CreateTaskRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(taskService.createTask(roleId, request, currentUserId));
  }

  @GetMapping("/roles/{roleId}/tasks")
  public Result<List<TaskResponse>> getTasks(
      @PathVariable Long roleId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(taskService.getTasksByRole(roleId, currentUserId));
  }

  @GetMapping("/tasks/{id}")
  public Result<TaskResponse> getTask(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(taskService.getTask(id, currentUserId));
  }

  @PutMapping("/tasks/{id}")
  public Result<TaskResponse> updateTask(
      @PathVariable Long id,
      @Valid @RequestBody UpdateTaskRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(taskService.updateTask(id, request, currentUserId));
  }

  @DeleteMapping("/tasks/{id}")
  public Result<Void> deleteTask(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    taskService.deleteTask(id, currentUserId);
    return Result.success();
  }
}
```

- [ ] **Step 2: Create TaskControllerTest**

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
import com.ansible.role.dto.CreateTaskRequest;
import com.ansible.role.dto.RoleResponse;
import com.ansible.role.dto.TaskResponse;
import com.ansible.role.dto.UpdateTaskRequest;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.TaskRepository;
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

class TaskControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private TaskRepository taskRepository;

  private String token;
  private Long projectId;
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
    projectId = projResp.getBody().getData().getId();

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
    taskRepository.deleteAll();
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

  private Long createTask(String name, String module, int order) {
    CreateTaskRequest req = new CreateTaskRequest();
    req.setName(name);
    req.setModule(module);
    req.setNotify(List.of("Restart nginx"));
    req.setTaskOrder(order);
    ResponseEntity<Result<TaskResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/tasks",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().getId();
  }

  @Test
  void createTask_success() {
    CreateTaskRequest req = new CreateTaskRequest();
    req.setName("Install nginx");
    req.setModule("apt");
    req.setArgs("{\"name\":\"nginx\",\"state\":\"present\"}");
    req.setNotify(List.of("Restart nginx"));
    req.setTaskOrder(1);

    ResponseEntity<Result<TaskResponse>> response =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/tasks",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    TaskResponse data = response.getBody().getData();
    assertThat(data.getName()).isEqualTo("Install nginx");
    assertThat(data.getModule()).isEqualTo("apt");
    assertThat(data.getNotify()).containsExactly("Restart nginx");
  }

  @Test
  void getTasks_returns_ordered_list() {
    createTask("Second task", "shell", 2);
    createTask("First task", "apt", 1);

    ResponseEntity<Result<List<TaskResponse>>> response =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/tasks",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<TaskResponse> tasks = response.getBody().getData();
    assertThat(tasks).hasSize(2);
    assertThat(tasks.get(0).getName()).isEqualTo("First task");
    assertThat(tasks.get(1).getName()).isEqualTo("Second task");
  }

  @Test
  void getTask_success() {
    Long taskId = createTask("Install nginx", "apt", 1);

    ResponseEntity<Result<TaskResponse>> response =
        restTemplate.exchange(
            "/api/tasks/" + taskId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("Install nginx");
  }

  @Test
  void updateTask_success() {
    Long taskId = createTask("Install nginx", "apt", 1);

    UpdateTaskRequest req = new UpdateTaskRequest();
    req.setName("Install apache");
    req.setModule("apt");

    ResponseEntity<Result<TaskResponse>> response =
        restTemplate.exchange(
            "/api/tasks/" + taskId,
            HttpMethod.PUT,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("Install apache");
  }

  @Test
  void deleteTask_success() {
    Long taskId = createTask("Install nginx", "apt", 1);

    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/tasks/" + taskId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(taskRepository.findById(taskId)).isEmpty();
  }
}
```

- [ ] **Step 3: Run integration tests**

Run: `cd backend && mvn verify -Dtest=TaskControllerTest -DfailIfNoTests=false -Dsurefire.skip=true -Dit.test=TaskControllerTest`

Note: Integration tests may need to run with: `cd backend && mvn test -Dtest=TaskControllerTest`
Expected: All 5 tests PASS

- [ ] **Step 4: Run code quality checks**

Run: `cd backend && mvn spotless:apply && mvn checkstyle:check pmd:check spotbugs:check`
Expected: All checks PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/role/controller/TaskController.java backend/src/test/java/com/ansible/role/controller/TaskControllerTest.java
git commit -m "feat: add TaskController with integration tests"
```

---

## Task 5: Handler Entity + Repository

**Files:**
- Create: `backend/src/main/java/com/ansible/role/entity/Handler.java`
- Create: `backend/src/main/java/com/ansible/role/repository/HandlerRepository.java`

- [ ] **Step 1: Create Handler entity**

```java
package com.ansible.role.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "handlers")
@Getter
@Setter
@NoArgsConstructor
public class Handler extends BaseEntity {

  @Column(nullable = false)
  private Long roleId;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false, length = 100)
  private String module;

  @Column(columnDefinition = "TEXT")
  private String args;

  @Column(name = "when_condition", length = 500)
  private String whenCondition;

  @Column(length = 100)
  private String register;
}
```

- [ ] **Step 2: Create HandlerRepository**

```java
package com.ansible.role.repository;

import com.ansible.role.entity.Handler;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandlerRepository extends JpaRepository<Handler, Long> {

  List<Handler> findAllByRoleId(Long roleId);
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/ansible/role/entity/Handler.java backend/src/main/java/com/ansible/role/repository/HandlerRepository.java
git commit -m "feat: add Handler entity and repository"
```

---

## Task 6: Handler DTOs

**Files:**
- Create: `backend/src/main/java/com/ansible/role/dto/CreateHandlerRequest.java`
- Create: `backend/src/main/java/com/ansible/role/dto/UpdateHandlerRequest.java`
- Create: `backend/src/main/java/com/ansible/role/dto/HandlerResponse.java`

- [ ] **Step 1: Create CreateHandlerRequest**

```java
package com.ansible.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateHandlerRequest {

  @NotBlank(message = "Handler name is required")
  @Size(max = 200, message = "Name must not exceed 200 characters")
  private String name;

  @NotBlank(message = "Module is required")
  @Size(max = 100, message = "Module must not exceed 100 characters")
  private String module;

  private String args;

  @Size(max = 500, message = "When condition must not exceed 500 characters")
  private String whenCondition;

  @Size(max = 100, message = "Register must not exceed 100 characters")
  private String register;
}
```

- [ ] **Step 2: Create UpdateHandlerRequest**

```java
package com.ansible.role.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateHandlerRequest {

  @Size(max = 200, message = "Name must not exceed 200 characters")
  private String name;

  @Size(max = 100, message = "Module must not exceed 100 characters")
  private String module;

  private String args;

  @Size(max = 500, message = "When condition must not exceed 500 characters")
  private String whenCondition;

  @Size(max = 100, message = "Register must not exceed 100 characters")
  private String register;
}
```

- [ ] **Step 3: Create HandlerResponse**

```java
package com.ansible.role.dto;

import com.ansible.role.entity.Handler;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class HandlerResponse {

  private final Long id;
  private final Long roleId;
  private final String name;
  private final String module;
  private final String args;
  private final String whenCondition;
  private final String register;
  private final Long createdBy;
  private final LocalDateTime createdAt;

  @JsonCreator
  public HandlerResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("roleId") Long roleId,
      @JsonProperty("name") String name,
      @JsonProperty("module") String module,
      @JsonProperty("args") String args,
      @JsonProperty("whenCondition") String whenCondition,
      @JsonProperty("register") String register,
      @JsonProperty("createdBy") Long createdBy,
      @JsonProperty("createdAt") LocalDateTime createdAt) {
    this.id = id;
    this.roleId = roleId;
    this.name = name;
    this.module = module;
    this.args = args;
    this.whenCondition = whenCondition;
    this.register = register;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
  }

  public HandlerResponse(Handler handler) {
    this.id = handler.getId();
    this.roleId = handler.getRoleId();
    this.name = handler.getName();
    this.module = handler.getModule();
    this.args = handler.getArgs();
    this.whenCondition = handler.getWhenCondition();
    this.register = handler.getRegister();
    this.createdBy = handler.getCreatedBy();
    this.createdAt = handler.getCreatedAt();
  }
}
```

- [ ] **Step 4: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/role/dto/CreateHandlerRequest.java backend/src/main/java/com/ansible/role/dto/UpdateHandlerRequest.java backend/src/main/java/com/ansible/role/dto/HandlerResponse.java
git commit -m "feat: add Handler DTOs"
```

---

## Task 7: HandlerService + Unit Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/role/service/HandlerService.java`
- Create: `backend/src/test/java/com/ansible/role/service/HandlerServiceTest.java`

- [ ] **Step 1: Create HandlerService**

```java
package com.ansible.role.service;

import com.ansible.role.dto.CreateHandlerRequest;
import com.ansible.role.dto.HandlerResponse;
import com.ansible.role.dto.UpdateHandlerRequest;
import com.ansible.role.entity.Handler;
import com.ansible.role.entity.Role;
import com.ansible.role.repository.HandlerRepository;
import com.ansible.role.repository.RoleRepository;
import com.ansible.security.ProjectAccessChecker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class HandlerService {

  private final HandlerRepository handlerRepository;
  private final RoleRepository roleRepository;
  private final ProjectAccessChecker accessChecker;

  @Transactional
  public HandlerResponse createHandler(
      Long roleId, CreateHandlerRequest request, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);

    Handler handler = new Handler();
    handler.setRoleId(roleId);
    handler.setName(request.getName());
    handler.setModule(request.getModule());
    handler.setArgs(request.getArgs());
    handler.setWhenCondition(request.getWhenCondition());
    handler.setRegister(request.getRegister());
    handler.setCreatedBy(currentUserId);
    Handler saved = handlerRepository.save(handler);
    return new HandlerResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<HandlerResponse> getHandlersByRole(Long roleId, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    return handlerRepository.findAllByRoleId(roleId).stream()
        .map(HandlerResponse::new)
        .toList();
  }

  @Transactional(readOnly = true)
  public HandlerResponse getHandler(Long handlerId, Long currentUserId) {
    Handler handler =
        handlerRepository
            .findById(handlerId)
            .orElseThrow(() -> new IllegalArgumentException("Handler not found"));
    Role role =
        roleRepository
            .findById(handler.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    return new HandlerResponse(handler);
  }

  @Transactional
  public HandlerResponse updateHandler(
      Long handlerId, UpdateHandlerRequest request, Long currentUserId) {
    Handler handler =
        handlerRepository
            .findById(handlerId)
            .orElseThrow(() -> new IllegalArgumentException("Handler not found"));
    Role role =
        roleRepository
            .findById(handler.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), handler.getCreatedBy(), currentUserId);

    if (StringUtils.hasText(request.getName())) {
      handler.setName(request.getName());
    }
    if (StringUtils.hasText(request.getModule())) {
      handler.setModule(request.getModule());
    }
    if (request.getArgs() != null) {
      handler.setArgs(request.getArgs());
    }
    if (request.getWhenCondition() != null) {
      handler.setWhenCondition(request.getWhenCondition());
    }
    if (request.getRegister() != null) {
      handler.setRegister(request.getRegister());
    }
    Handler saved = handlerRepository.save(handler);
    return new HandlerResponse(saved);
  }

  @Transactional
  public void deleteHandler(Long handlerId, Long currentUserId) {
    Handler handler =
        handlerRepository
            .findById(handlerId)
            .orElseThrow(() -> new IllegalArgumentException("Handler not found"));
    Role role =
        roleRepository
            .findById(handler.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), handler.getCreatedBy(), currentUserId);
    handlerRepository.delete(handler);
  }
}
```

- [ ] **Step 2: Create HandlerServiceTest**

```java
package com.ansible.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.role.dto.CreateHandlerRequest;
import com.ansible.role.dto.HandlerResponse;
import com.ansible.role.dto.UpdateHandlerRequest;
import com.ansible.role.entity.Handler;
import com.ansible.role.entity.Role;
import com.ansible.role.repository.HandlerRepository;
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
class HandlerServiceTest {

  @Mock private HandlerRepository handlerRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private ProjectAccessChecker accessChecker;
  @InjectMocks private HandlerService handlerService;

  private Role testRole;
  private Handler testHandler;

  @BeforeEach
  void setUp() {
    testRole = new Role();
    ReflectionTestUtils.setField(testRole, "id", 1L);
    testRole.setProjectId(10L);
    testRole.setName("nginx");
    testRole.setCreatedBy(10L);

    testHandler = new Handler();
    ReflectionTestUtils.setField(testHandler, "id", 1L);
    testHandler.setRoleId(1L);
    testHandler.setName("Restart nginx");
    testHandler.setModule("service");
    testHandler.setArgs("{\"name\":\"nginx\",\"state\":\"restarted\"}");
    testHandler.setCreatedBy(10L);
    ReflectionTestUtils.setField(testHandler, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(testHandler, "updatedAt", LocalDateTime.now());
  }

  @Test
  void createHandler_success() {
    CreateHandlerRequest request = new CreateHandlerRequest();
    request.setName("Restart nginx");
    request.setModule("service");
    request.setArgs("{\"name\":\"nginx\",\"state\":\"restarted\"}");

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(handlerRepository.save(any(Handler.class))).thenReturn(testHandler);

    HandlerResponse response = handlerService.createHandler(1L, request, 10L);

    assertThat(response.getName()).isEqualTo("Restart nginx");
    assertThat(response.getModule()).isEqualTo("service");
    verify(handlerRepository).save(any(Handler.class));
  }

  @Test
  void createHandler_roleNotFound() {
    CreateHandlerRequest request = new CreateHandlerRequest();
    request.setName("Restart nginx");
    request.setModule("service");

    when(roleRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handlerService.createHandler(99L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Role not found");
  }

  @Test
  void getHandlersByRole_success() {
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(handlerRepository.findAllByRoleId(1L)).thenReturn(List.of(testHandler));

    List<HandlerResponse> handlers = handlerService.getHandlersByRole(1L, 10L);

    assertThat(handlers).hasSize(1);
    assertThat(handlers.get(0).getName()).isEqualTo("Restart nginx");
  }

  @Test
  void getHandler_success() {
    when(handlerRepository.findById(1L)).thenReturn(Optional.of(testHandler));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    HandlerResponse response = handlerService.getHandler(1L, 10L);

    assertThat(response.getName()).isEqualTo("Restart nginx");
  }

  @Test
  void updateHandler_success() {
    UpdateHandlerRequest request = new UpdateHandlerRequest();
    request.setName("Reload nginx");

    when(handlerRepository.findById(1L)).thenReturn(Optional.of(testHandler));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(handlerRepository.save(any(Handler.class))).thenReturn(testHandler);

    handlerService.updateHandler(1L, request, 10L);

    verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
    verify(handlerRepository).save(any(Handler.class));
  }

  @Test
  void deleteHandler_success() {
    when(handlerRepository.findById(1L)).thenReturn(Optional.of(testHandler));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    handlerService.deleteHandler(1L, 10L);

    verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
    verify(handlerRepository).delete(testHandler);
  }
}
```

- [ ] **Step 3: Run unit tests**

Run: `cd backend && mvn test -Dtest=HandlerServiceTest -pl .`
Expected: All 6 tests PASS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/ansible/role/service/HandlerService.java backend/src/test/java/com/ansible/role/service/HandlerServiceTest.java
git commit -m "feat: add HandlerService with unit tests"
```

---

## Task 8: HandlerController + Integration Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/role/controller/HandlerController.java`
- Create: `backend/src/test/java/com/ansible/role/controller/HandlerControllerTest.java`

- [ ] **Step 1: Create HandlerController**

```java
package com.ansible.role.controller;

import com.ansible.common.Result;
import com.ansible.role.dto.CreateHandlerRequest;
import com.ansible.role.dto.HandlerResponse;
import com.ansible.role.dto.UpdateHandlerRequest;
import com.ansible.role.service.HandlerService;
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
public class HandlerController {

  private final HandlerService handlerService;

  @PostMapping("/roles/{roleId}/handlers")
  public Result<HandlerResponse> createHandler(
      @PathVariable Long roleId,
      @Valid @RequestBody CreateHandlerRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(handlerService.createHandler(roleId, request, currentUserId));
  }

  @GetMapping("/roles/{roleId}/handlers")
  public Result<List<HandlerResponse>> getHandlers(
      @PathVariable Long roleId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(handlerService.getHandlersByRole(roleId, currentUserId));
  }

  @GetMapping("/handlers/{id}")
  public Result<HandlerResponse> getHandler(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(handlerService.getHandler(id, currentUserId));
  }

  @PutMapping("/handlers/{id}")
  public Result<HandlerResponse> updateHandler(
      @PathVariable Long id,
      @Valid @RequestBody UpdateHandlerRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(handlerService.updateHandler(id, request, currentUserId));
  }

  @DeleteMapping("/handlers/{id}")
  public Result<Void> deleteHandler(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    handlerService.deleteHandler(id, currentUserId);
    return Result.success();
  }
}
```

- [ ] **Step 2: Create HandlerControllerTest**

```java
package com.ansible.role.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.project.dto.CreateProjectRequest;
import com.ansible.project.dto.ProjectResponse;
import com.ansible.project.repository.ProjectMemberRepository;
import com.ansible.project.repository.ProjectRepository;
import com.ansible.role.dto.CreateHandlerRequest;
import com.ansible.role.dto.CreateRoleRequest;
import com.ansible.role.dto.HandlerResponse;
import com.ansible.role.dto.RoleResponse;
import com.ansible.role.dto.UpdateHandlerRequest;
import com.ansible.role.repository.HandlerRepository;
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

class HandlerControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private HandlerRepository handlerRepository;

  private String token;
  private Long projectId;
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
    projectId = projResp.getBody().getData().getId();

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
    handlerRepository.deleteAll();
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

  private Long createHandler(String name, String module) {
    CreateHandlerRequest req = new CreateHandlerRequest();
    req.setName(name);
    req.setModule(module);
    req.setArgs("{\"name\":\"nginx\",\"state\":\"restarted\"}");
    ResponseEntity<Result<HandlerResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/handlers",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().getId();
  }

  @Test
  void createHandler_success() {
    CreateHandlerRequest req = new CreateHandlerRequest();
    req.setName("Restart nginx");
    req.setModule("service");
    req.setArgs("{\"name\":\"nginx\",\"state\":\"restarted\"}");

    ResponseEntity<Result<HandlerResponse>> response =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/handlers",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    HandlerResponse data = response.getBody().getData();
    assertThat(data.getName()).isEqualTo("Restart nginx");
    assertThat(data.getModule()).isEqualTo("service");
  }

  @Test
  void getHandlers_returns_list() {
    createHandler("Restart nginx", "service");
    createHandler("Reload nginx", "service");

    ResponseEntity<Result<List<HandlerResponse>>> response =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/handlers",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData()).hasSize(2);
  }

  @Test
  void getHandler_success() {
    Long handlerId = createHandler("Restart nginx", "service");

    ResponseEntity<Result<HandlerResponse>> response =
        restTemplate.exchange(
            "/api/handlers/" + handlerId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("Restart nginx");
  }

  @Test
  void updateHandler_success() {
    Long handlerId = createHandler("Restart nginx", "service");

    UpdateHandlerRequest req = new UpdateHandlerRequest();
    req.setName("Reload nginx");

    ResponseEntity<Result<HandlerResponse>> response =
        restTemplate.exchange(
            "/api/handlers/" + handlerId,
            HttpMethod.PUT,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("Reload nginx");
  }

  @Test
  void deleteHandler_success() {
    Long handlerId = createHandler("Restart nginx", "service");

    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/handlers/" + handlerId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(handlerRepository.findById(handlerId)).isEmpty();
  }
}
```

- [ ] **Step 3: Run integration tests**

Run: `cd backend && mvn test -Dtest=HandlerControllerTest`
Expected: All 5 tests PASS

- [ ] **Step 4: Run full code quality checks**

Run: `cd backend && mvn spotless:apply && mvn checkstyle:check pmd:check spotbugs:check`
Expected: All checks PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/role/controller/HandlerController.java backend/src/test/java/com/ansible/role/controller/HandlerControllerTest.java
git commit -m "feat: add HandlerController with integration tests"
```

---

## Task 9: Frontend TypeScript Types + API Layer

**Files:**
- Create: `frontend/src/types/entity/Task.ts`
- Create: `frontend/src/api/task.ts`
- Create: `frontend/src/api/handler.ts`

- [ ] **Step 1: Create Task and Handler TypeScript types**

```typescript
// frontend/src/types/entity/Task.ts

export interface Task {
  id: number;
  roleId: number;
  name: string;
  module: string;
  args: string;
  whenCondition: string;
  loop: string;
  until: string;
  register: string;
  notify: string[];
  taskOrder: number;
  createdBy: number;
  createdAt: string;
}

export interface CreateTaskRequest {
  name: string;
  module: string;
  args?: string;
  whenCondition?: string;
  loop?: string;
  until?: string;
  register?: string;
  notify?: string[];
  taskOrder: number;
}

export interface UpdateTaskRequest {
  name?: string;
  module?: string;
  args?: string;
  whenCondition?: string;
  loop?: string;
  until?: string;
  register?: string;
  notify?: string[];
  taskOrder?: number;
}

export interface Handler {
  id: number;
  roleId: number;
  name: string;
  module: string;
  args: string;
  whenCondition: string;
  register: string;
  createdBy: number;
  createdAt: string;
}

export interface CreateHandlerRequest {
  name: string;
  module: string;
  args?: string;
  whenCondition?: string;
  register?: string;
}

export interface UpdateHandlerRequest {
  name?: string;
  module?: string;
  args?: string;
  whenCondition?: string;
  register?: string;
}
```

- [ ] **Step 2: Create Task API functions**

```typescript
// frontend/src/api/task.ts

import request from './request';
import type { Task, CreateTaskRequest, UpdateTaskRequest } from '../types/entity/Task';

export async function createTask(
  roleId: number,
  data: CreateTaskRequest
): Promise<Task> {
  const res = await request.post<Task>(`/roles/${roleId}/tasks`, data);
  return res.data;
}

export async function getTasks(roleId: number): Promise<Task[]> {
  const res = await request.get<Task[]>(`/roles/${roleId}/tasks`);
  return res.data;
}

export async function getTask(id: number): Promise<Task> {
  const res = await request.get<Task>(`/tasks/${id}`);
  return res.data;
}

export async function updateTask(
  id: number,
  data: UpdateTaskRequest
): Promise<Task> {
  const res = await request.put<Task>(`/tasks/${id}`, data);
  return res.data;
}

export async function deleteTask(id: number): Promise<void> {
  await request.delete(`/tasks/${id}`);
}
```

- [ ] **Step 3: Create Handler API functions**

```typescript
// frontend/src/api/handler.ts

import request from './request';
import type { Handler, CreateHandlerRequest, UpdateHandlerRequest } from '../types/entity/Task';

export async function createHandler(
  roleId: number,
  data: CreateHandlerRequest
): Promise<Handler> {
  const res = await request.post<Handler>(`/roles/${roleId}/handlers`, data);
  return res.data;
}

export async function getHandlers(roleId: number): Promise<Handler[]> {
  const res = await request.get<Handler[]>(`/roles/${roleId}/handlers`);
  return res.data;
}

export async function getHandler(id: number): Promise<Handler> {
  const res = await request.get<Handler>(`/handlers/${id}`);
  return res.data;
}

export async function updateHandler(
  id: number,
  data: UpdateHandlerRequest
): Promise<Handler> {
  const res = await request.put<Handler>(`/handlers/${id}`, data);
  return res.data;
}

export async function deleteHandler(id: number): Promise<void> {
  await request.delete(`/handlers/${id}`);
}
```

- [ ] **Step 4: Verify frontend compiles**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 5: Commit**

```bash
git add frontend/src/types/entity/Task.ts frontend/src/api/task.ts frontend/src/api/handler.ts
git commit -m "feat: add Task and Handler TypeScript types and API layer"
```

---

## Task 10: RoleTasks Tab Component

**Files:**
- Create: `frontend/src/pages/role/RoleTasks.tsx`
- Modify: `frontend/src/pages/role/RoleDetail.tsx`

- [ ] **Step 1: Create RoleTasks component**

```tsx
// frontend/src/pages/role/RoleTasks.tsx

import { useEffect, useState } from 'react';
import { Button, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Table, Tag, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { Task, CreateTaskRequest, UpdateTaskRequest } from '../../types/entity/Task';
import type { Handler } from '../../types/entity/Task';
import { createTask, getTasks, updateTask, deleteTask } from '../../api/task';
import { getHandlers } from '../../api/handler';

interface RoleTasksProps {
  roleId: number;
}

export default function RoleTasks({ roleId }: RoleTasksProps) {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [handlers, setHandlers] = useState<Handler[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  const [form] = Form.useForm();

  const fetchData = async () => {
    setLoading(true);
    try {
      const [taskList, handlerList] = await Promise.all([
        getTasks(roleId),
        getHandlers(roleId),
      ]);
      setTasks(taskList);
      setHandlers(handlerList);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [roleId]);

  const handleCreate = () => {
    setEditingTask(null);
    form.resetFields();
    form.setFieldValue('taskOrder', tasks.length + 1);
    setModalOpen(true);
  };

  const handleEdit = (task: Task) => {
    setEditingTask(task);
    form.setFieldsValue({
      name: task.name,
      module: task.module,
      args: task.args,
      whenCondition: task.whenCondition,
      loop: task.loop,
      until: task.until,
      register: task.register,
      notify: task.notify,
      taskOrder: task.taskOrder,
    });
    setModalOpen(true);
  };

  const handleDelete = async (id: number) => {
    await deleteTask(id);
    message.success('已删除');
    fetchData();
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (editingTask) {
      const data: UpdateTaskRequest = { ...values };
      await updateTask(editingTask.id, data);
      message.success('已更新');
    } else {
      const data: CreateTaskRequest = { ...values };
      await createTask(roleId, data);
      message.success('已创建');
    }
    setModalOpen(false);
    fetchData();
  };

  const columns = [
    {
      title: '顺序',
      dataIndex: 'taskOrder',
      key: 'taskOrder',
      width: 70,
    },
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '模块',
      dataIndex: 'module',
      key: 'module',
      width: 120,
    },
    {
      title: 'Notify',
      dataIndex: 'notify',
      key: 'notify',
      render: (notify: string[]) =>
        notify?.map((n) => <Tag key={n}>{n}</Tag>),
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: unknown, record: Task) => (
        <Space size="small">
          <Button
            type="text"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          />
          <Popconfirm title="确定删除?" onConfirm={() => handleDelete(record.id)}>
            <Button type="text" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16, textAlign: 'right' }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          添加 Task
        </Button>
      </div>
      <Table
        dataSource={tasks}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={false}
        size="middle"
      />
      <Modal
        title={editingTask ? '编辑 Task' : '创建 Task'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        width={640}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入 Task 名称' }]}
          >
            <Input placeholder="例如: Install nginx" />
          </Form.Item>
          <Form.Item
            name="module"
            label="模块"
            rules={[{ required: true, message: '请输入 Ansible 模块名' }]}
          >
            <Input placeholder="例如: apt, yum, service, copy" />
          </Form.Item>
          <Form.Item name="args" label="参数 (JSON)">
            <Input.TextArea rows={3} placeholder='{"name": "nginx", "state": "present"}' />
          </Form.Item>
          <Form.Item name="whenCondition" label="When 条件">
            <Input placeholder="例如: ansible_os_family == 'Debian'" />
          </Form.Item>
          <Form.Item name="loop" label="Loop">
            <Input placeholder="例如: {{ packages }}" />
          </Form.Item>
          <Form.Item name="until" label="Until">
            <Input placeholder="例如: result.rc == 0" />
          </Form.Item>
          <Form.Item name="register" label="Register">
            <Input placeholder="例如: install_result" />
          </Form.Item>
          <Form.Item name="notify" label="Notify (Handler)">
            <Select
              mode="multiple"
              placeholder="选择要通知的 Handler"
              options={handlers.map((h) => ({ label: h.name, value: h.name }))}
            />
          </Form.Item>
          <Form.Item
            name="taskOrder"
            label="顺序"
            rules={[{ required: true, message: '请输入顺序' }]}
          >
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
```

- [ ] **Step 2: Update RoleDetail.tsx to use RoleTasks**

In `frontend/src/pages/role/RoleDetail.tsx`, replace the Tasks tab `ComingSoon` placeholder with the `RoleTasks` component. The key change is:

1. Import `RoleTasks` at the top
2. Change `tabItems` from a static array to be generated inside the component (since it needs `roleId`)
3. Replace `<ComingSoon />` for tasks key with `<RoleTasks roleId={Number(roleId)} />`

Updated full file:

```tsx
import { useEffect, useState } from 'react';
import { Button, Card, Empty, Skeleton, Tabs } from 'antd';
import { ArrowLeftOutlined, InboxOutlined } from '@ant-design/icons';
import { useParams, useNavigate } from 'react-router-dom';
import type { Role } from '../../types/entity/Role';
import { getRole } from '../../api/role';
import RoleTasks from './RoleTasks';

function ComingSoon() {
  return (
    <Empty
      image={<InboxOutlined style={{ fontSize: 48, color: '#94a3b8' }} />}
      description={
        <span style={{ color: '#64748b' }}>即将推出</span>
      }
      style={{ padding: '48px 0' }}
    />
  );
}

export default function RoleDetail() {
  const { id, roleId } = useParams<{ id: string; roleId: string }>();
  const navigate = useNavigate();
  const [role, setRole] = useState<Role | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (roleId) {
      getRole(Number(roleId)).then((r) => {
        setRole(r);
        setLoading(false);
      });
    }
  }, [roleId]);

  if (loading) {
    return <Skeleton active />;
  }

  const tabItems = [
    { key: 'tasks', label: 'Tasks', children: <RoleTasks roleId={Number(roleId)} /> },
    { key: 'handlers', label: 'Handlers', children: <ComingSoon /> },
    { key: 'templates', label: 'Templates', children: <ComingSoon /> },
    { key: 'files', label: 'Files', children: <ComingSoon /> },
    { key: 'vars', label: 'Vars', children: <ComingSoon /> },
    { key: 'defaults', label: 'Defaults', children: <ComingSoon /> },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate(`/projects/${id}/roles`)}
          style={{ color: '#64748b', padding: '4px 8px' }}
        >
          返回 Roles
        </Button>
      </div>
      <Card
        style={{ marginBottom: 16 }}
        title={
          <span style={{ fontSize: 18, fontWeight: 600 }}>{role?.name}</span>
        }
      >
        <p style={{ color: '#64748b', margin: 0 }}>
          {role?.description || '无描述'}
        </p>
      </Card>
      <Card bodyStyle={{ padding: 0 }}>
        <Tabs
          defaultActiveKey="tasks"
          items={tabItems}
          style={{ padding: '0 24px' }}
        />
      </Card>
    </div>
  );
}
```

- [ ] **Step 3: Verify frontend compiles**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 4: Run lint**

Run: `cd frontend && npm run lint`
Expected: No errors

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/role/RoleTasks.tsx frontend/src/pages/role/RoleDetail.tsx
git commit -m "feat: add RoleTasks tab component with CRUD"
```

---

## Task 11: RoleHandlers Tab Component

**Files:**
- Create: `frontend/src/pages/role/RoleHandlers.tsx`
- Modify: `frontend/src/pages/role/RoleDetail.tsx`

- [ ] **Step 1: Create RoleHandlers component**

```tsx
// frontend/src/pages/role/RoleHandlers.tsx

import { useEffect, useState } from 'react';
import { Button, Form, Input, Modal, Popconfirm, Space, Table, message } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { Handler, CreateHandlerRequest, UpdateHandlerRequest } from '../../types/entity/Task';
import { createHandler, getHandlers, updateHandler, deleteHandler } from '../../api/handler';

interface RoleHandlersProps {
  roleId: number;
}

export default function RoleHandlers({ roleId }: RoleHandlersProps) {
  const [handlers, setHandlers] = useState<Handler[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingHandler, setEditingHandler] = useState<Handler | null>(null);
  const [form] = Form.useForm();

  const fetchHandlers = async () => {
    setLoading(true);
    try {
      const list = await getHandlers(roleId);
      setHandlers(list);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHandlers();
  }, [roleId]);

  const handleCreate = () => {
    setEditingHandler(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (handler: Handler) => {
    setEditingHandler(handler);
    form.setFieldsValue({
      name: handler.name,
      module: handler.module,
      args: handler.args,
      whenCondition: handler.whenCondition,
      register: handler.register,
    });
    setModalOpen(true);
  };

  const handleDelete = async (id: number) => {
    await deleteHandler(id);
    message.success('已删除');
    fetchHandlers();
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (editingHandler) {
      const data: UpdateHandlerRequest = { ...values };
      await updateHandler(editingHandler.id, data);
      message.success('已更新');
    } else {
      const data: CreateHandlerRequest = { ...values };
      await createHandler(roleId, data);
      message.success('已创建');
    }
    setModalOpen(false);
    fetchHandlers();
  };

  const columns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '模块',
      dataIndex: 'module',
      key: 'module',
      width: 120,
    },
    {
      title: 'When',
      dataIndex: 'whenCondition',
      key: 'whenCondition',
      render: (val: string) => val || '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: unknown, record: Handler) => (
        <Space size="small">
          <Button
            type="text"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          />
          <Popconfirm title="确定删除?" onConfirm={() => handleDelete(record.id)}>
            <Button type="text" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16, textAlign: 'right' }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          添加 Handler
        </Button>
      </div>
      <Table
        dataSource={handlers}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={false}
        size="middle"
      />
      <Modal
        title={editingHandler ? '编辑 Handler' : '创建 Handler'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        width={640}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入 Handler 名称' }]}
          >
            <Input placeholder="例如: Restart nginx" />
          </Form.Item>
          <Form.Item
            name="module"
            label="模块"
            rules={[{ required: true, message: '请输入 Ansible 模块名' }]}
          >
            <Input placeholder="例如: service, systemd" />
          </Form.Item>
          <Form.Item name="args" label="参数 (JSON)">
            <Input.TextArea rows={3} placeholder='{"name": "nginx", "state": "restarted"}' />
          </Form.Item>
          <Form.Item name="whenCondition" label="When 条件">
            <Input placeholder="例如: ansible_os_family == 'Debian'" />
          </Form.Item>
          <Form.Item name="register" label="Register">
            <Input placeholder="例如: restart_result" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
```

- [ ] **Step 2: Update RoleDetail.tsx to use RoleHandlers**

In `frontend/src/pages/role/RoleDetail.tsx`, add the import and replace the Handlers tab:

Add import:
```tsx
import RoleHandlers from './RoleHandlers';
```

Change the handlers tab item from:
```tsx
{ key: 'handlers', label: 'Handlers', children: <ComingSoon /> },
```
to:
```tsx
{ key: 'handlers', label: 'Handlers', children: <RoleHandlers roleId={Number(roleId)} /> },
```

- [ ] **Step 3: Verify frontend compiles and lint passes**

Run: `cd frontend && npx tsc --noEmit && npm run lint`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/role/RoleHandlers.tsx frontend/src/pages/role/RoleDetail.tsx
git commit -m "feat: add RoleHandlers tab component with CRUD"
```

---

## Task 12: Final Verification

- [ ] **Step 1: Run all backend tests**

Run: `cd backend && mvn test`
Expected: All tests PASS (including new TaskServiceTest, TaskControllerTest, HandlerServiceTest, HandlerControllerTest)

- [ ] **Step 2: Run all backend quality checks**

Run: `cd backend && mvn spotless:apply && mvn checkstyle:check pmd:check spotbugs:check`
Expected: All checks PASS

- [ ] **Step 3: Run frontend checks**

Run: `cd frontend && npx tsc --noEmit && npm run lint`
Expected: No errors

- [ ] **Step 4: Update CLAUDE.md progress**

Change Phase 3 status in `CLAUDE.md` from `🔄` to `✅` and add Phase 4:

```
- Phase 3 ✅ 主机组、主机、Role CRUD
- Phase 4 ✅ Task、Handler CRUD
- 后续：RoleVariable、RoleDefaultVariable、Template、File、Variable、Environment、Tag、Playbook
```

- [ ] **Step 5: Commit progress update**

```bash
git add CLAUDE.md
git commit -m "docs: update implementation progress — Phase 4 Task+Handler complete"
```
