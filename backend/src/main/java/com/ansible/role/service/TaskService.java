package com.ansible.role.service;

import com.ansible.role.dto.CreateTaskRequest;
import com.ansible.role.dto.HandlerResponse;
import com.ansible.role.dto.TaskResponse;
import com.ansible.role.dto.UpdateTaskRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.Task;
import com.ansible.role.repository.HandlerRepository;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.TaskRepository;
import com.ansible.security.ProjectAccessChecker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
  private final HandlerRepository handlerRepository;
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
    task.setBecome(request.getBecome());
    task.setBecomeUser(request.getBecomeUser());
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
  @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.NPathComplexity"})
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
    if (request.getBecome() != null) {
      task.setBecome(request.getBecome());
    }
    if (request.getBecomeUser() != null) {
      task.setBecomeUser(request.getBecomeUser());
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

  @Transactional(readOnly = true)
  public List<HandlerResponse> getNotifiedHandlers(Long taskId, Long currentUserId) {
    Task task =
        taskRepository
            .findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found"));
    Role role =
        roleRepository
            .findById(task.getRoleId())
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);

    List<String> notifyNames = parseNotify(task.getNotify());
    if (notifyNames.isEmpty()) {
      return List.of();
    }
    return handlerRepository.findAllByRoleIdAndNameIn(task.getRoleId(), notifyNames).stream()
        .map(HandlerResponse::new)
        .toList();
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
