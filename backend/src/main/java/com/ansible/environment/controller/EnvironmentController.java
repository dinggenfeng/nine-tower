package com.ansible.environment.controller;

import com.ansible.common.Result;
import com.ansible.environment.dto.CreateEnvironmentRequest;
import com.ansible.environment.dto.EnvConfigRequest;
import com.ansible.environment.dto.EnvConfigResponse;
import com.ansible.environment.dto.EnvironmentResponse;
import com.ansible.environment.dto.UpdateEnvironmentRequest;
import com.ansible.environment.service.EnvironmentService;
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
public class EnvironmentController {

  private final EnvironmentService environmentService;

  @PostMapping("/projects/{projectId}/environments")
  public Result<EnvironmentResponse> createEnvironment(
      @PathVariable Long projectId,
      @Valid @RequestBody CreateEnvironmentRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(environmentService.createEnvironment(projectId, request, currentUserId));
  }

  @GetMapping("/projects/{projectId}/environments")
  public Result<List<EnvironmentResponse>> listEnvironments(
      @PathVariable Long projectId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(environmentService.listEnvironments(projectId, currentUserId));
  }

  @GetMapping("/environments/{envId}")
  public Result<EnvironmentResponse> getEnvironment(
      @PathVariable Long envId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(environmentService.getEnvironment(envId, currentUserId));
  }

  @PutMapping("/environments/{envId}")
  public Result<EnvironmentResponse> updateEnvironment(
      @PathVariable Long envId,
      @Valid @RequestBody UpdateEnvironmentRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(environmentService.updateEnvironment(envId, request, currentUserId));
  }

  @DeleteMapping("/environments/{envId}")
  public Result<Void> deleteEnvironment(
      @PathVariable Long envId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    environmentService.deleteEnvironment(envId, currentUserId);
    return Result.success();
  }

  @PostMapping("/environments/{envId}/configs")
  public Result<EnvConfigResponse> addConfig(
      @PathVariable Long envId,
      @Valid @RequestBody EnvConfigRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(environmentService.addConfig(envId, request, currentUserId));
  }

  @DeleteMapping("/env-configs/{configId}")
  public Result<Void> removeConfig(
      @PathVariable Long configId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    environmentService.removeConfig(configId, currentUserId);
    return Result.success();
  }
}
