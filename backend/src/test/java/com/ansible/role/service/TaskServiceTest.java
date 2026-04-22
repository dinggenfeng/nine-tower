package com.ansible.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.role.dto.BlockChildRequest;
import com.ansible.role.dto.CreateTaskRequest;
import com.ansible.role.dto.HandlerResponse;
import com.ansible.role.dto.TaskResponse;
import com.ansible.role.dto.UpdateTaskRequest;
import com.ansible.role.entity.Handler;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.Task;
import com.ansible.role.repository.HandlerRepository;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.TaskRepository;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.tag.entity.TaskTag;
import com.ansible.tag.repository.TaskTagRepository;
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
  @Mock private HandlerRepository handlerRepository;
  @Mock private ProjectAccessChecker accessChecker;
  @Mock private TaskTagRepository taskTagRepository;
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
    when(taskRepository.findAllByRoleIdAndParentTaskIdIsNullOrderByTaskOrderAsc(1L))
        .thenReturn(List.of());
    when(taskRepository.save(any(Task.class))).thenReturn(testTask);

    TaskResponse response = taskService.createTask(1L, request, 10L);

    assertThat(response.getName()).isEqualTo("Install nginx");
    assertThat(response.getModule()).isEqualTo("apt");
    assertThat(response.getNotify()).containsExactly("Restart nginx");
    verify(taskRepository).save(any(Task.class));
  }

  @Test
  void createTask_withBecome() {
    CreateTaskRequest request = new CreateTaskRequest();
    request.setName("Install nginx");
    request.setModule("apt");
    request.setTaskOrder(1);
    request.setBecome(true);
    request.setBecomeUser("root");

    Task savedTask = new Task();
    ReflectionTestUtils.setField(savedTask, "id", 2L);
    savedTask.setRoleId(1L);
    savedTask.setName("Install nginx");
    savedTask.setModule("apt");
    savedTask.setTaskOrder(1);
    savedTask.setBecome(true);
    savedTask.setBecomeUser("root");
    savedTask.setCreatedBy(10L);
    ReflectionTestUtils.setField(savedTask, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(savedTask, "updatedAt", LocalDateTime.now());

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(taskRepository.findAllByRoleIdAndParentTaskIdIsNullOrderByTaskOrderAsc(1L))
        .thenReturn(List.of());
    when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

    TaskResponse response = taskService.createTask(1L, request, 10L);

    assertThat(response.getBecome()).isTrue();
    assertThat(response.getBecomeUser()).isEqualTo("root");
  }

  @Test
  void updateTask_withBecome() {
    UpdateTaskRequest request = new UpdateTaskRequest();
    request.setBecome(true);
    request.setBecomeUser("deploy");

    testTask.setBecome(true);
    testTask.setBecomeUser("deploy");

    when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(taskRepository.save(any(Task.class))).thenReturn(testTask);

    TaskResponse response = taskService.updateTask(1L, request, 10L);

    assertThat(response.getBecome()).isTrue();
    assertThat(response.getBecomeUser()).isEqualTo("deploy");
  }

  @Test
  void createTask_withIgnoreErrors() {
    CreateTaskRequest request = new CreateTaskRequest();
    request.setName("Install nginx");
    request.setModule("apt");
    request.setTaskOrder(1);
    request.setIgnoreErrors(true);

    Task savedTask = new Task();
    ReflectionTestUtils.setField(savedTask, "id", 3L);
    savedTask.setRoleId(1L);
    savedTask.setName("Install nginx");
    savedTask.setModule("apt");
    savedTask.setTaskOrder(1);
    savedTask.setIgnoreErrors(true);
    savedTask.setCreatedBy(10L);
    ReflectionTestUtils.setField(savedTask, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(savedTask, "updatedAt", LocalDateTime.now());

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(taskRepository.findAllByRoleIdAndParentTaskIdIsNullOrderByTaskOrderAsc(1L))
        .thenReturn(List.of());
    when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

    TaskResponse response = taskService.createTask(1L, request, 10L);

    assertThat(response.getIgnoreErrors()).isTrue();
  }

  @Test
  void updateTask_withIgnoreErrors() {
    UpdateTaskRequest request = new UpdateTaskRequest();
    request.setIgnoreErrors(true);

    testTask.setIgnoreErrors(true);

    when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(taskRepository.save(any(Task.class))).thenReturn(testTask);

    TaskResponse response = taskService.updateTask(1L, request, 10L);

    assertThat(response.getIgnoreErrors()).isTrue();
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
    when(taskRepository.findAllByRoleIdAndParentTaskIdIsNullOrderByTaskOrderAsc(1L)).thenReturn(List.of(testTask));

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
    when(taskRepository.findAllByRoleIdAndParentTaskIdIsNullOrderByTaskOrderAsc(1L))
        .thenReturn(List.of());

    taskService.deleteTask(1L, 10L);

    verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
    verify(taskRepository).delete(testTask);
  }

  @Test
  void deleteTask_compactsOrder() {
    Task task2 = new Task();
    ReflectionTestUtils.setField(task2, "id", 2L);
    task2.setRoleId(1L);
    task2.setName("Keep me");
    task2.setModule("shell");
    task2.setTaskOrder(3);
    task2.setCreatedBy(10L);

    Task task3 = new Task();
    ReflectionTestUtils.setField(task3, "id", 3L);
    task3.setRoleId(1L);
    task3.setName("Keep me too");
    task3.setModule("shell");
    task3.setTaskOrder(5);
    task3.setCreatedBy(10L);

    when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(taskRepository.findAllByRoleIdAndParentTaskIdIsNullOrderByTaskOrderAsc(1L))
        .thenReturn(List.of(task2, task3));

    taskService.deleteTask(1L, 10L);

    assertThat(task2.getTaskOrder()).isEqualTo(1);
    assertThat(task3.getTaskOrder()).isEqualTo(2);
    verify(taskRepository).save(task2);
    verify(taskRepository).save(task3);
  }

  @Test
  void createTask_shiftsExistingOrder() {
    Task existing1 = new Task();
    ReflectionTestUtils.setField(existing1, "id", 2L);
    existing1.setRoleId(1L);
    existing1.setName("Task A");
    existing1.setModule("shell");
    existing1.setTaskOrder(1);
    existing1.setCreatedBy(10L);

    Task existing2 = new Task();
    ReflectionTestUtils.setField(existing2, "id", 3L);
    existing2.setRoleId(1L);
    existing2.setName("Task B");
    existing2.setModule("shell");
    existing2.setTaskOrder(2);
    existing2.setCreatedBy(10L);

    Task existing3 = new Task();
    ReflectionTestUtils.setField(existing3, "id", 4L);
    existing3.setRoleId(1L);
    existing3.setName("Task C");
    existing3.setModule("shell");
    existing3.setTaskOrder(3);
    existing3.setCreatedBy(10L);

    CreateTaskRequest request = new CreateTaskRequest();
    request.setName("Inserted task");
    request.setModule("apt");
    request.setTaskOrder(2);

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(taskRepository.findAllByRoleIdAndParentTaskIdIsNullOrderByTaskOrderAsc(1L))
        .thenReturn(List.of(existing1, existing2, existing3));
    when(taskRepository.save(any(Task.class))).thenReturn(testTask);

    taskService.createTask(1L, request, 10L);

    // existing1 (order 1) should stay at 1, existing2 (order 2) and existing3 (order 3) should shift up
    assertThat(existing1.getTaskOrder()).isEqualTo(1);
    assertThat(existing2.getTaskOrder()).isEqualTo(3);
    assertThat(existing3.getTaskOrder()).isEqualTo(4);
    verify(taskRepository).save(existing2);
    verify(taskRepository).save(existing3);
  }

  @Test
  void getNotifiedHandlers_success() {
    Handler handler = new Handler();
    ReflectionTestUtils.setField(handler, "id", 1L);
    handler.setRoleId(1L);
    handler.setName("Restart nginx");
    handler.setModule("service");
    handler.setCreatedBy(10L);
    ReflectionTestUtils.setField(handler, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(handler, "updatedAt", LocalDateTime.now());

    when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(handlerRepository.findAllByRoleIdAndNameIn(1L, List.of("Restart nginx")))
        .thenReturn(List.of(handler));

    List<HandlerResponse> handlers = taskService.getNotifiedHandlers(1L, 10L);

    assertThat(handlers).hasSize(1);
    assertThat(handlers.get(0).getName()).isEqualTo("Restart nginx");
  }

  @Test
  void getNotifiedHandlers_emptyNotify() {
    testTask.setNotify(null);

    when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    List<HandlerResponse> handlers = taskService.getNotifiedHandlers(1L, 10L);

    assertThat(handlers).isEmpty();
  }

  @Test
  void createBlockTask_withChildren_savesBlockAndChildren() {
    // Setup: create role first
    Role role = new Role();
    ReflectionTestUtils.setField(role, "id", 2L);
    role.setProjectId(10L);
    role.setName("test-role");
    role.setCreatedBy(10L);

    BlockChildRequest child1 = new BlockChildRequest();
    child1.setSection("BLOCK");
    child1.setName("Child task 1");
    child1.setModule("command");
    child1.setArgs("{\"_raw_params\": \"echo hello\"}");
    child1.setTaskOrder(1);

    BlockChildRequest child2 = new BlockChildRequest();
    child2.setSection("RESCUE");
    child2.setName("Rescue task");
    child2.setModule("debug");
    child2.setArgs("{\"msg\": \"failed\"}");
    child2.setTaskOrder(1);

    CreateTaskRequest request = new CreateTaskRequest();
    request.setName("My Block");
    request.setModule("block");
    request.setTaskOrder(1);
    request.setBlockChildren(List.of(child1, child2));

    when(roleRepository.findById(2L)).thenReturn(Optional.of(role));
    when(taskRepository.findAllByRoleIdAndParentTaskIdIsNullOrderByTaskOrderAsc(2L))
        .thenReturn(List.of());
    when(taskRepository.save(any(Task.class))).thenReturn(testTask);

    TaskResponse response = taskService.createTask(2L, request, 10L);

    assertThat(response.getId()).isNotNull();
    // createTask returns new TaskResponse(saved) which has children=null
    assertThat(response.getChildren()).isNull();
    // But children should be saved - verify via repository
    verify(taskRepository, org.mockito.Mockito.times(3)).save(any(Task.class));
  }

  @Test
  void getTasksByRole_withBlockTask_returnsBlockWithChildren() {
    // Create a block task with children
    Task blockTask = new Task();
    ReflectionTestUtils.setField(blockTask, "id", 10L);
    blockTask.setRoleId(1L);
    blockTask.setName("Block Task");
    blockTask.setModule("block");
    blockTask.setTaskOrder(1);
    blockTask.setCreatedBy(10L);
    ReflectionTestUtils.setField(blockTask, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(blockTask, "updatedAt", LocalDateTime.now());

    Task childTask = new Task();
    ReflectionTestUtils.setField(childTask, "id", 11L);
    childTask.setRoleId(1L);
    childTask.setParentTaskId(10L);
    childTask.setBlockSection("BLOCK");
    childTask.setName("Inner task");
    childTask.setModule("shell");
    childTask.setTaskOrder(1);
    childTask.setCreatedBy(10L);
    ReflectionTestUtils.setField(childTask, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(childTask, "updatedAt", LocalDateTime.now());

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(taskRepository.findAllByRoleIdAndParentTaskIdIsNullOrderByTaskOrderAsc(1L))
        .thenReturn(List.of(blockTask));
    when(taskRepository.findAllByParentTaskIdOrderByTaskOrderAsc(10L))
        .thenReturn(List.of(childTask));
    when(taskTagRepository.findByTaskId(10L)).thenReturn(List.of());

    List<TaskResponse> result = taskService.getTasksByRole(1L, 10L);

    assertThat(result).hasSize(1);
    TaskResponse block = result.get(0);
    assertThat(block.getModule()).isEqualTo("block");
    assertThat(block.getChildren()).isNotNull();
    assertThat(block.getChildren()).hasSize(1);
    assertThat(block.getChildren().get(0).getName()).isEqualTo("Inner task");
  }

  @Test
  void deleteTask_blockTask_deletesChildren() {
    Task blockTask = new Task();
    ReflectionTestUtils.setField(blockTask, "id", 20L);
    blockTask.setRoleId(1L);
    blockTask.setName("Block to delete");
    blockTask.setModule("block");
    blockTask.setTaskOrder(1);
    blockTask.setCreatedBy(10L);
    ReflectionTestUtils.setField(blockTask, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(blockTask, "updatedAt", LocalDateTime.now());

    Task childTask = new Task();
    ReflectionTestUtils.setField(childTask, "id", 21L);
    childTask.setRoleId(1L);
    childTask.setParentTaskId(20L);
    childTask.setBlockSection("BLOCK");
    childTask.setName("To be deleted");
    childTask.setModule("command");
    childTask.setTaskOrder(1);
    childTask.setCreatedBy(10L);

    when(taskRepository.findById(20L)).thenReturn(Optional.of(blockTask));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(taskRepository.findAllByParentTaskIdOrderByTaskOrderAsc(20L))
        .thenReturn(List.of(childTask));
    when(taskRepository.findAllByRoleIdAndParentTaskIdIsNullOrderByTaskOrderAsc(1L))
        .thenReturn(List.of());

    taskService.deleteTask(20L, 10L);

    // Verify children were deleted first, then the block
    var inOrder = org.mockito.Mockito.inOrder(taskRepository);
    inOrder.verify(taskRepository).deleteAll(List.of(childTask));
    inOrder.verify(taskRepository).delete(blockTask);
    inOrder.verify(taskRepository).flush();
  }

  @Test
  void updateTaskTags_success() {
    when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    taskService.updateTaskTags(1L, List.of(100L, 200L), 10L);

    verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
    verify(taskTagRepository).deleteByTaskId(1L);
    verify(taskTagRepository).flush();
    verify(taskTagRepository, org.mockito.Mockito.times(2)).save(any(TaskTag.class));
  }

  @Test
  void getTaskTags_success() {
    TaskTag tt1 = new TaskTag();
    ReflectionTestUtils.setField(tt1, "id", 1L);
    tt1.setTaskId(1L);
    tt1.setTagId(100L);

    TaskTag tt2 = new TaskTag();
    ReflectionTestUtils.setField(tt2, "id", 2L);
    tt2.setTaskId(1L);
    tt2.setTagId(200L);

    when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(taskTagRepository.findByTaskId(1L)).thenReturn(List.of(tt1, tt2));

    List<Long> tagIds = taskService.getTaskTags(1L, 10L);

    assertThat(tagIds).containsExactly(100L, 200L);
    verify(accessChecker).checkMembership(10L, 10L);
  }

  @Test
  void getTaskTags_empty() {
    when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(taskTagRepository.findByTaskId(1L)).thenReturn(List.of());

    List<Long> tagIds = taskService.getTaskTags(1L, 10L);

    assertThat(tagIds).isEmpty();
  }

  @Test
  void reorderTasks_success() {
    Task t1 = new Task();
    ReflectionTestUtils.setField(t1, "id", 1L);
    t1.setRoleId(1L);
    t1.setTaskOrder(1);
    Task t2 = new Task();
    ReflectionTestUtils.setField(t2, "id", 2L);
    t2.setRoleId(1L);
    t2.setTaskOrder(2);
    Task t3 = new Task();
    ReflectionTestUtils.setField(t3, "id", 3L);
    t3.setRoleId(1L);
    t3.setTaskOrder(3);

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(taskRepository.findAllByRoleIdAndParentTaskIdIsNullOrderByTaskOrderAsc(1L))
        .thenReturn(List.of(t1, t2, t3));

    taskService.reorderTasks(1L, List.of(3L, 1L, 2L), 10L);

    assertThat(t3.getTaskOrder()).isEqualTo(1);
    assertThat(t1.getTaskOrder()).isEqualTo(2);
    assertThat(t2.getTaskOrder()).isEqualTo(3);
    verify(taskRepository).saveAll(List.of(t1, t2, t3));
  }

  @Test
  void reorderTasks_countMismatch_throws() {
    Task t1 = new Task();
    ReflectionTestUtils.setField(t1, "id", 1L);
    t1.setRoleId(1L);
    Task t2 = new Task();
    ReflectionTestUtils.setField(t2, "id", 2L);
    t2.setRoleId(1L);

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(taskRepository.findAllByRoleIdAndParentTaskIdIsNullOrderByTaskOrderAsc(1L))
        .thenReturn(List.of(t1, t2));

    assertThatThrownBy(() -> taskService.reorderTasks(1L, List.of(1L), 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("mismatch");
  }

  @Test
  void reorderTasks_invalidTaskId_throws() {
    Task t1 = new Task();
    ReflectionTestUtils.setField(t1, "id", 1L);
    t1.setRoleId(1L);
    Task t2 = new Task();
    ReflectionTestUtils.setField(t2, "id", 2L);
    t2.setRoleId(1L);

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(taskRepository.findAllByRoleIdAndParentTaskIdIsNullOrderByTaskOrderAsc(1L))
        .thenReturn(List.of(t1, t2));

    assertThatThrownBy(() -> taskService.reorderTasks(1L, List.of(1L, 999L), 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not belong to role");
  }
}
