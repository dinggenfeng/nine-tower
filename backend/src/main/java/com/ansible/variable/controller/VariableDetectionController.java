package com.ansible.variable.controller;

import com.ansible.common.Result;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.variable.dto.BatchVariableSaveRequest;
import com.ansible.variable.dto.DetectedVariableResponse;
import com.ansible.variable.entity.Variable;
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
      Long scopeId = resolveScopeId(projectId, req);
      if (variableRepository.existsByScopeAndScopeIdAndKey(req.scope(), scopeId, req.key())) {
        return errorResult(index,
            "Variable '" + req.key() + "' already exists in " + req.scope() + " scope");
      }
      Variable v = new Variable();
      v.setScope(req.scope());
      v.setScopeId(scopeId);
      v.setKey(req.key());
      v.setValue(req.value() != null ? req.value() : "");
      v.setCreatedBy(userId);
      variableRepository.save(v);
      return Map.of("index", index, "success", true, "key", req.key());
    } catch (IllegalArgumentException e) {
      return Map.of("index", index, "success", false, "error", e.getMessage());
    }
  }

  private Long resolveScopeId(Long projectId, BatchVariableSaveRequest req) {
    if (req.scopeId() != null) {
      return req.scopeId();
    }
    if (req.scope() == com.ansible.variable.entity.VariableScope.PROJECT) {
      return projectId;
    }
    throw new IllegalArgumentException("scopeId is required for scope: " + req.scope());
  }

  private static Map<String, Object> errorResult(int index, String message) {
    return Map.of("index", index, "success", false, "error", message);
  }
}
