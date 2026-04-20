package com.ansible.role.service;

import com.ansible.role.dto.BlockChildRequest;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.TooManyMethods")
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
    task.setIgnoreErrors(request.getIgnoreErrors());
    task.setCreatedBy(currentUserId);
    if (request.getBlockChildren() != null && !request.getBlockChildren().isEmpty()) {
      validateBlockChildren(request.getModule(), request.getBlockChildren());
    }
    shiftTaskOrder(roleId, null, request.getTaskOrder());
    Task saved = taskRepository.save(task);
    if (request.getBlockChildren() != null && !request.getBlockChildren().isEmpty()) {
      saveBlockChildren(saved.getId(), roleId, currentUserId, request.getBlockChildren());
    }
    return new TaskResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<TaskResponse> getTasksByRole(Long roleId, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    List<Task> topLevel = taskRepository.findAllByRoleIdAndParentTaskIdIsNullOrderByTaskOrderAsc(roleId);
    return topLevel.stream()
        .map(task -> {
          if ("block".equals(task.getModule())) {
            return buildBlockResponse(task);
          }
          return new TaskResponse(task);
        })
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
  @SuppressWarnings({
    "PMD.CyclomaticComplexity",
    "PMD.NPathComplexity",
    "PMD.CognitiveComplexity"
  })
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
    if (request.getIgnoreErrors() != null) {
      task.setIgnoreErrors(request.getIgnoreErrors());
    }
    if (request.getBlockChildren() != null && !request.getBlockChildren().isEmpty()) {
      validateBlockChildren(task.getModule(), request.getBlockChildren());
    }
    Task saved = taskRepository.save(task);
    if (request.getBlockChildren() != null) {
      List<Task> existingChildren = taskRepository.findAllByParentTaskIdOrderByTaskOrderAsc(taskId);
      taskRepository.deleteAll(existingChildren);
      taskRepository.flush();
      saveBlockChildren(taskId, task.getRoleId(), task.getCreatedBy(), request.getBlockChildren());
    }
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

    Long parentTaskId = task.getParentTaskId();
    Long roleId = task.getRoleId();

    if ("block".equals(task.getModule())) {
      List<Task> children = taskRepository.findAllByParentTaskIdOrderByTaskOrderAsc(taskId);
      taskRepository.deleteAll(children);
    }
    taskRepository.delete(task);
    taskRepository.flush();

    compactTaskOrder(roleId, parentTaskId);
  }

  private void shiftTaskOrder(Long roleId, Long parentTaskId, int fromOrder) {
    List<Task> tasks;
    if (parentTaskId != null) {
      tasks = taskRepository.findAllByParentTaskIdOrderByTaskOrderAsc(parentTaskId);
    } else {
      tasks =
          taskRepository.findAllByRoleIdAndParentTaskIdIsNullOrderByTaskOrderAsc(roleId);
    }
    for (Task t : tasks) {
      if (t.getTaskOrder() >= fromOrder) {
        t.setTaskOrder(t.getTaskOrder() + 1);
        taskRepository.save(t);
      }
    }
  }

  private void compactTaskOrder(Long roleId, Long parentTaskId) {
    if (parentTaskId != null) {
      // Block children: taskOrder is scoped to (parentTaskId, blockSection), so
      // renumber each section independently.
      Map<String, List<Task>> bySection =
          taskRepository.findAllByParentTaskIdOrderByTaskOrderAsc(parentTaskId).stream()
              .collect(
                  Collectors.groupingBy(
                      t -> t.getBlockSection() == null ? "" : t.getBlockSection()));
      for (List<Task> section : bySection.values()) {
        renumber(section);
      }
    } else {
      renumber(taskRepository.findAllByRoleIdAndParentTaskIdIsNullOrderByTaskOrderAsc(roleId));
    }
  }

  private void renumber(List<Task> ordered) {
    for (int i = 0; i < ordered.size(); i++) {
      Task t = ordered.get(i);
      int newOrder = i + 1;
      if (!Objects.equals(newOrder, t.getTaskOrder())) {
        t.setTaskOrder(newOrder);
        taskRepository.save(t);
      }
    }
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

  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  private void saveBlockChildren(
      Long parentTaskId, Long roleId, Long createdBy, List<BlockChildRequest> children) {
    for (BlockChildRequest child : children) {
      Task childTask = new Task();
      childTask.setRoleId(roleId);
      childTask.setParentTaskId(parentTaskId);
      childTask.setBlockSection(child.getSection());
      childTask.setName(child.getName());
      childTask.setModule(child.getModule());
      childTask.setArgs(child.getArgs());
      childTask.setWhenCondition(child.getWhenCondition());
      childTask.setLoop(child.getLoop());
      childTask.setUntil(child.getUntil());
      childTask.setRegister(child.getRegister());
      childTask.setNotify(child.getNotify());
      childTask.setTaskOrder(child.getTaskOrder());
      childTask.setBecome(child.getBecome());
      childTask.setBecomeUser(child.getBecomeUser());
      childTask.setIgnoreErrors(child.getIgnoreErrors());
      childTask.setCreatedBy(createdBy);
      taskRepository.save(childTask);
    }
  }

  private void validateBlockChildren(String parentModule, List<BlockChildRequest> children) {
    if (!"block".equals(parentModule)) {
      throw new IllegalArgumentException(
          "blockChildren can only be provided when module is 'block'");
    }
    for (BlockChildRequest child : children) {
      if ("block".equals(child.getModule())) {
        throw new IllegalArgumentException("Nested block tasks are not supported");
      }
    }
  }

  private TaskResponse buildBlockResponse(Task blockTask) {
    List<Task> children = taskRepository.findAllByParentTaskIdOrderByTaskOrderAsc(blockTask.getId());
    List<TaskResponse> childResponses = children.stream()
        .map(TaskResponse::new)
        .toList();
    return new TaskResponse(
        blockTask.getId(),
        blockTask.getRoleId(),
        blockTask.getName(),
        blockTask.getModule(),
        blockTask.getArgs(),
        blockTask.getWhenCondition(),
        blockTask.getLoop(),
        blockTask.getUntil(),
        blockTask.getRegister(),
        parseNotify(blockTask.getNotify()),
        blockTask.getTaskOrder(),
        blockTask.getBecome(),
        blockTask.getBecomeUser(),
        blockTask.getIgnoreErrors(),
        blockTask.getCreatedBy(),
        blockTask.getCreatedAt(),
        blockTask.getParentTaskId(),
        blockTask.getBlockSection(),
        childResponses
    );
  }
}
