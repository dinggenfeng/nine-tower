package com.ansible.role.controller;

import com.ansible.common.Result;
import com.ansible.role.dto.CreateRoleDefaultVariableRequest;
import com.ansible.role.dto.RoleDefaultVariableResponse;
import com.ansible.role.dto.UpdateRoleDefaultVariableRequest;
import com.ansible.role.service.RoleDefaultVariableService;
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
public class RoleDefaultVariableController {

  private final RoleDefaultVariableService roleDefaultVariableService;

  @PostMapping("/roles/{roleId}/defaults")
  public Result<RoleDefaultVariableResponse> createDefault(
      @PathVariable Long roleId,
      @Valid @RequestBody CreateRoleDefaultVariableRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(
        roleDefaultVariableService.createDefault(roleId, request, currentUserId));
  }

  @GetMapping("/roles/{roleId}/defaults")
  public Result<List<RoleDefaultVariableResponse>> getDefaults(
      @PathVariable Long roleId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleDefaultVariableService.getDefaultsByRole(roleId, currentUserId));
  }

  @PutMapping("/role-defaults/{id}")
  public Result<RoleDefaultVariableResponse> updateDefault(
      @PathVariable Long id,
      @Valid @RequestBody UpdateRoleDefaultVariableRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleDefaultVariableService.updateDefault(id, request, currentUserId));
  }

  @DeleteMapping("/role-defaults/{id}")
  public Result<Void> deleteDefault(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    roleDefaultVariableService.deleteDefault(id, currentUserId);
    return Result.success();
  }
}
