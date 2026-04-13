package com.ansible.role.controller;

import com.ansible.common.Result;
import com.ansible.role.dto.CreateRoleRequest;
import com.ansible.role.dto.RoleResponse;
import com.ansible.role.dto.UpdateRoleRequest;
import com.ansible.role.service.RoleService;
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
public class RoleController {

  private final RoleService roleService;

  @PostMapping("/projects/{projectId}/roles")
  public Result<RoleResponse> createRole(
      @PathVariable Long projectId,
      @Valid @RequestBody CreateRoleRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleService.createRole(projectId, request, currentUserId));
  }

  @GetMapping("/projects/{projectId}/roles")
  public Result<List<RoleResponse>> getRoles(
      @PathVariable Long projectId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleService.getRolesByProject(projectId, currentUserId));
  }

  @GetMapping("/roles/{id}")
  public Result<RoleResponse> getRole(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleService.getRole(id, currentUserId));
  }

  @PutMapping("/roles/{id}")
  public Result<RoleResponse> updateRole(
      @PathVariable Long id,
      @Valid @RequestBody UpdateRoleRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleService.updateRole(id, request, currentUserId));
  }

  @DeleteMapping("/roles/{id}")
  public Result<Void> deleteRole(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    roleService.deleteRole(id, currentUserId);
    return Result.success();
  }
}
