package com.ansible.variable.controller;

import com.ansible.common.Result;
import com.ansible.variable.dto.CreateVariableRequest;
import com.ansible.variable.dto.UpdateVariableRequest;
import com.ansible.variable.dto.VariableResponse;
import com.ansible.variable.entity.VariableScope;
import com.ansible.variable.service.VariableService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VariableController {

  private final VariableService variableService;

  @PostMapping("/projects/{projectId}/variables")
  public Result<VariableResponse> createVariable(
      @PathVariable Long projectId,
      @Valid @RequestBody CreateVariableRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(variableService.createVariable(projectId, request, currentUserId));
  }

  @GetMapping("/projects/{projectId}/variables")
  public Result<List<VariableResponse>> listVariables(
      @PathVariable Long projectId,
      @RequestParam(required = false) VariableScope scope,
      @RequestParam(required = false) Long scopeId,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(variableService.listVariables(projectId, scope, scopeId, currentUserId));
  }

  @GetMapping("/variables/{varId}")
  public Result<VariableResponse> getVariable(
      @PathVariable Long varId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(variableService.getVariable(varId, currentUserId));
  }

  @PutMapping("/variables/{varId}")
  public Result<VariableResponse> updateVariable(
      @PathVariable Long varId,
      @Valid @RequestBody UpdateVariableRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(variableService.updateVariable(varId, request, currentUserId));
  }

  @DeleteMapping("/variables/{varId}")
  public Result<Void> deleteVariable(
      @PathVariable Long varId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    variableService.deleteVariable(varId, currentUserId);
    return Result.success();
  }
}
