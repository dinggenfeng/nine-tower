package com.ansible.project.controller;

import com.ansible.common.Result;
import com.ansible.project.dto.AddMemberRequest;
import com.ansible.project.dto.CreateProjectRequest;
import com.ansible.project.dto.ProjectMemberResponse;
import com.ansible.project.dto.ProjectResponse;
import com.ansible.project.dto.UpdateMemberRoleRequest;
import com.ansible.project.dto.UpdateProjectRequest;
import com.ansible.project.service.ProjectMemberService;
import com.ansible.project.service.ProjectService;
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
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

  private final ProjectService projectService;
  private final ProjectMemberService projectMemberService;

  @PostMapping
  public Result<ProjectResponse> createProject(
      @Valid @RequestBody CreateProjectRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(projectService.createProject(request, currentUserId));
  }

  @GetMapping
  public Result<List<ProjectResponse>> getMyProjects(
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(projectService.getMyProjects(currentUserId));
  }

  @GetMapping("/{id}")
  public Result<ProjectResponse> getProject(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(projectService.getProject(id, currentUserId));
  }

  @PutMapping("/{id}")
  public Result<ProjectResponse> updateProject(
      @PathVariable Long id,
      @Valid @RequestBody UpdateProjectRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(projectService.updateProject(id, request, currentUserId));
  }

  @DeleteMapping("/{id}")
  public Result<Void> deleteProject(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    projectService.deleteProject(id, currentUserId);
    return Result.success();
  }

  @GetMapping("/{id}/members")
  public Result<List<ProjectMemberResponse>> listMembers(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(projectMemberService.listMembers(id, currentUserId));
  }

  @PostMapping("/{id}/members")
  public Result<ProjectMemberResponse> addMember(
      @PathVariable Long id,
      @Valid @RequestBody AddMemberRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(projectMemberService.addMember(id, request, currentUserId));
  }

  @DeleteMapping("/{id}/members/{userId}")
  public Result<Void> removeMember(
      @PathVariable Long id,
      @PathVariable Long userId,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    projectMemberService.removeMember(id, userId, currentUserId);
    return Result.success();
  }

  @PutMapping("/{id}/members/{userId}")
  public Result<ProjectMemberResponse> updateMemberRole(
      @PathVariable Long id,
      @PathVariable Long userId,
      @Valid @RequestBody UpdateMemberRoleRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(
        projectMemberService.updateMemberRole(id, userId, request, currentUserId));
  }
}
