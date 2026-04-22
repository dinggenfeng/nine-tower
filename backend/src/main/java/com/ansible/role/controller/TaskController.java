package com.ansible.role.controller;

import com.ansible.common.Result;
import com.ansible.role.dto.CreateTaskRequest;
import com.ansible.role.dto.HandlerResponse;
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

  @GetMapping("/tasks/{id}/notifies")
  public Result<List<HandlerResponse>> getNotifiedHandlers(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(taskService.getNotifiedHandlers(id, currentUserId));
  }

  @PutMapping("/tasks/{id}/tags")
  public Result<Void> updateTaskTags(
      @PathVariable Long id,
      @RequestBody List<Long> tagIds,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    taskService.updateTaskTags(id, tagIds, currentUserId);
    return Result.success();
  }

  @GetMapping("/tasks/{id}/tags")
  public Result<List<Long>> getTaskTags(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(taskService.getTaskTags(id, currentUserId));
  }

  @DeleteMapping("/tasks/{id}")
  public Result<Void> deleteTask(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    taskService.deleteTask(id, currentUserId);
    return Result.success();
  }
}
