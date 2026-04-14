package com.ansible.role.controller;

import com.ansible.common.Result;
import com.ansible.role.dto.CreateRoleVariableRequest;
import com.ansible.role.dto.RoleVariableResponse;
import com.ansible.role.dto.UpdateRoleVariableRequest;
import com.ansible.role.service.RoleVariableService;
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
public class RoleVariableController {

  private final RoleVariableService roleVariableService;

  @PostMapping("/roles/{roleId}/vars")
  public Result<RoleVariableResponse> createVariable(
      @PathVariable Long roleId,
      @Valid @RequestBody CreateRoleVariableRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleVariableService.createVariable(roleId, request, currentUserId));
  }

  @GetMapping("/roles/{roleId}/vars")
  public Result<List<RoleVariableResponse>> getVariables(
      @PathVariable Long roleId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleVariableService.getVariablesByRole(roleId, currentUserId));
  }

  @PutMapping("/role-vars/{id}")
  public Result<RoleVariableResponse> updateVariable(
      @PathVariable Long id,
      @Valid @RequestBody UpdateRoleVariableRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleVariableService.updateVariable(id, request, currentUserId));
  }

  @DeleteMapping("/role-vars/{id}")
  public Result<Void> deleteVariable(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    roleVariableService.deleteVariable(id, currentUserId);
    return Result.success();
  }
}
