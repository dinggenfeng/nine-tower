package com.ansible.variable.controller;

import com.ansible.common.Result;
import com.ansible.role.entity.Role;
import com.ansible.role.entity.RoleVariable;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.RoleVariableRepository;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.variable.dto.BatchVariableSaveRequest;
import com.ansible.variable.dto.DetectedVariableResponse;
import com.ansible.variable.entity.Variable;
import com.ansible.variable.entity.VariableScope;
import com.ansible.variable.repository.VariableRepository;
import com.ansible.variable.service.VariableDetectionService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VariableDetectionController {

  private final VariableDetectionService detectionService;
  private final VariableRepository variableRepository;
  private final RoleVariableRepository roleVariableRepository;
  private final RoleRepository roleRepository;
  private final ProjectAccessChecker accessChecker;

  @GetMapping("/projects/{projectId}/detect-variables")
  public Result<List<DetectedVariableResponse>> detectVariables(
      @PathVariable Long projectId,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    return Result.success(detectionService.detectVariables(projectId, userId));
  }

  @PostMapping("/projects/{projectId}/variables/batch")
  public Result<List<Map<String, Object>>> batchSave(
      @PathVariable Long projectId,
      @Valid @RequestBody List<BatchVariableSaveRequest> requests,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long userId = Long.valueOf(userDetails.getUsername());
    accessChecker.checkMembership(projectId, userId);
    List<Map<String, Object>> results = new ArrayList<>();
    for (int i = 0; i < requests.size(); i++) {
      results.add(saveOneItem(projectId, userId, i, requests.get(i)));
    }
    return Result.success(results);
  }

  private Map<String, Object> saveOneItem(
      Long projectId, Long userId, int index, BatchVariableSaveRequest req) {
    try {
      if ("ROLE_VARIABLE".equals(req.saveAs())) {
        return saveAsRoleVariable(projectId, userId, index, req);
      }
      return saveAsVariable(projectId, userId, index, req);
    } catch (IllegalArgumentException e) {
      return Map.of("index", index, "success", false, "error", e.getMessage());
    }
  }

  private Map<String, Object> saveAsRoleVariable(
      Long projectId, Long userId, int index, BatchVariableSaveRequest req) {
    if (req.roleId() == null) {
      return errorResult(index, "roleId is required for ROLE_VARIABLE");
    }
    Role role = roleRepository
        .findById(req.roleId())
        .orElseThrow(
            () -> new IllegalArgumentException("Role not found: " + req.roleId()));
    if (!role.getProjectId().equals(projectId)) {
      return errorResult(index, "Role does not belong to this project");
    }
    if (roleVariableRepository.existsByRoleIdAndKey(req.roleId(), req.key())) {
      return errorResult(index,
          "Variable '" + req.key() + "' already exists in this Role");
    }
    RoleVariable rv = new RoleVariable();
    rv.setRoleId(req.roleId());
    rv.setKey(req.key());
    rv.setValue(req.value() != null ? req.value() : "");
    rv.setCreatedBy(userId);
    roleVariableRepository.save(rv);
    return Map.of("index", index, "success", true, "key", req.key());
  }

  private Map<String, Object> saveAsVariable(
      Long projectId, Long userId, int index, BatchVariableSaveRequest req) {
    VariableScope scope = VariableScope.valueOf(req.scope());
    if (variableRepository.existsByScopeAndScopeIdAndKey(scope, projectId, req.key())) {
      return errorResult(index,
          "Variable '" + req.key() + "' already exists at " + scope + " level");
    }
    Variable v = new Variable();
    v.setScope(scope);
    v.setScopeId(projectId);
    v.setKey(req.key());
    v.setValue(req.value() != null ? req.value() : "");
    v.setCreatedBy(userId);
    variableRepository.save(v);
    return Map.of("index", index, "success", true, "key", req.key());
  }

  private static Map<String, Object> errorResult(int index, String message) {
    return Map.of("index", index, "success", false, "error", message);
  }
}
