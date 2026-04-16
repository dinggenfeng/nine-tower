package com.ansible.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.role.dto.CreateHandlerRequest;
import com.ansible.role.dto.HandlerResponse;
import com.ansible.role.dto.TaskResponse;
import com.ansible.role.dto.UpdateHandlerRequest;
import com.ansible.role.entity.Handler;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.Task;
import com.ansible.role.repository.HandlerRepository;
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
class HandlerServiceTest {

  @Mock private HandlerRepository handlerRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private TaskRepository taskRepository;
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
  void createHandler_withBecome() {
    CreateHandlerRequest request = new CreateHandlerRequest();
    request.setName("Restart nginx");
    request.setModule("service");
    request.setBecome(true);
    request.setBecomeUser("root");

    Handler savedHandler = new Handler();
    ReflectionTestUtils.setField(savedHandler, "id", 2L);
    savedHandler.setRoleId(1L);
    savedHandler.setName("Restart nginx");
    savedHandler.setModule("service");
    savedHandler.setBecome(true);
    savedHandler.setBecomeUser("root");
    savedHandler.setCreatedBy(10L);
    ReflectionTestUtils.setField(savedHandler, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(savedHandler, "updatedAt", LocalDateTime.now());

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(handlerRepository.save(any(Handler.class))).thenReturn(savedHandler);

    HandlerResponse response = handlerService.createHandler(1L, request, 10L);

    assertThat(response.getBecome()).isTrue();
    assertThat(response.getBecomeUser()).isEqualTo("root");
  }

  @Test
  void updateHandler_withBecome() {
    UpdateHandlerRequest request = new UpdateHandlerRequest();
    request.setBecome(true);
    request.setBecomeUser("deploy");

    testHandler.setBecome(true);
    testHandler.setBecomeUser("deploy");

    when(handlerRepository.findById(1L)).thenReturn(Optional.of(testHandler));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(handlerRepository.save(any(Handler.class))).thenReturn(testHandler);

    HandlerResponse response = handlerService.updateHandler(1L, request, 10L);

    assertThat(response.getBecome()).isTrue();
    assertThat(response.getBecomeUser()).isEqualTo("deploy");
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

  @Test
  void getNotifyingTasks_success() {
    Task task = new Task();
    ReflectionTestUtils.setField(task, "id", 1L);
    task.setRoleId(1L);
    task.setName("Install nginx");
    task.setModule("apt");
    task.setNotify("[\"Restart nginx\"]");
    task.setTaskOrder(1);
    task.setCreatedBy(10L);
    ReflectionTestUtils.setField(task, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(task, "updatedAt", LocalDateTime.now());

    when(handlerRepository.findById(1L)).thenReturn(Optional.of(testHandler));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(taskRepository.findAllByRoleIdOrderByTaskOrderAsc(1L)).thenReturn(List.of(task));

    List<TaskResponse> tasks = handlerService.getNotifyingTasks(1L, 10L);

    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("Install nginx");
  }

  @Test
  void getNotifyingTasks_noMatch() {
    Task task = new Task();
    ReflectionTestUtils.setField(task, "id", 1L);
    task.setRoleId(1L);
    task.setName("Install nginx");
    task.setModule("apt");
    task.setNotify("[\"Reload apache\"]");
    task.setTaskOrder(1);
    task.setCreatedBy(10L);
    ReflectionTestUtils.setField(task, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(task, "updatedAt", LocalDateTime.now());

    when(handlerRepository.findById(1L)).thenReturn(Optional.of(testHandler));
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(taskRepository.findAllByRoleIdOrderByTaskOrderAsc(1L)).thenReturn(List.of(task));

    List<TaskResponse> tasks = handlerService.getNotifyingTasks(1L, 10L);

    assertThat(tasks).isEmpty();
  }
}
