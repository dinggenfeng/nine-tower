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
      BatchVariableSaveRequest req = requests.get(i);
      try {
        if ("ROLE_VARIABLE".equals(req.saveAs())) {
          if (req.roleId() == null) {
            results.add(Map.of(
                "index", i, "success", false, "error", "roleId is required for ROLE_VARIABLE"));
            continue;
          }
          Role role = roleRepository
              .findById(req.roleId())
              .orElseThrow(
                  () -> new IllegalArgumentException("Role not found: " + req.roleId()));
          if (!role.getProjectId().equals(projectId)) {
            results.add(Map.of(
                "index", i, "success", false, "error",
                "Role does not belong to this project"));
            continue;
          }
          if (roleVariableRepository.existsByRoleIdAndKey(req.roleId(), req.key())) {
            results.add(Map.of(
                "index", i, "success", false, "error",
                "Variable '" + req.key() + "' already exists in this Role"));
            continue;
          }
          RoleVariable rv = new RoleVariable();
          rv.setRoleId(req.roleId());
          rv.setKey(req.key());
          rv.setValue(req.value() != null ? req.value() : "");
          rv.setCreatedBy(userId);
          roleVariableRepository.save(rv);
          results.add(Map.of("index", i, "success", true, "key", req.key()));
        } else {
          VariableScope scope = VariableScope.valueOf(req.scope());
          if (variableRepository.existsByScopeAndScopeIdAndKey(scope, projectId, req.key())) {
            results.add(Map.of(
                "index", i, "success", false, "error",
                "Variable '" + req.key() + "' already exists at " + scope + " level"));
            continue;
          }
          Variable v = new Variable();
          v.setScope(scope);
          v.setScopeId(projectId);
          v.setKey(req.key());
          v.setValue(req.value() != null ? req.value() : "");
          v.setCreatedBy(userId);
          variableRepository.save(v);
          results.add(Map.of("index", i, "success", true, "key", req.key()));
        }
      } catch (Exception e) {
        results.add(Map.of("index", i, "success", false, "error", e.getMessage()));
      }
    }
    return Result.success(results);
  }
}
