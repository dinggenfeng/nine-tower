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
