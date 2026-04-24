package com.ansible.environment.service;

import com.ansible.project.service.ProjectCleanupService;
import com.ansible.security.ProjectAccessChecker;
import com.ansible.environment.dto.CreateEnvironmentRequest;
import com.ansible.environment.dto.EnvConfigRequest;
import com.ansible.environment.dto.EnvConfigResponse;
import com.ansible.environment.dto.EnvironmentResponse;
import com.ansible.environment.dto.UpdateEnvironmentRequest;
import com.ansible.environment.entity.EnvConfig;
import com.ansible.environment.entity.Environment;
import com.ansible.environment.repository.EnvConfigRepository;
import com.ansible.environment.repository.EnvironmentRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnvironmentService {

  private final EnvironmentRepository environmentRepository;
  private final EnvConfigRepository envConfigRepository;
  private final ProjectAccessChecker accessChecker;
  private final ProjectCleanupService cleanupService;

  @Transactional
  public EnvironmentResponse createEnvironment(
      Long projectId, CreateEnvironmentRequest request, Long userId) {
    accessChecker.checkMembership(projectId, userId);
    if (environmentRepository.existsByProjectIdAndName(projectId, request.getName())) {
      throw new IllegalArgumentException(
          "Environment with name '" + request.getName() + "' already exists");
    }
    Environment env = new Environment();
    env.setProjectId(projectId);
    env.setName(request.getName());
    env.setDescription(request.getDescription());
    env.setCreatedBy(userId);
    Environment saved = environmentRepository.save(env);
    return new EnvironmentResponse(saved, List.of());
  }

  @Transactional(readOnly = true)
  public List<EnvironmentResponse> listEnvironments(Long projectId, Long userId) {
    accessChecker.checkMembership(projectId, userId);
    List<Environment> environments = environmentRepository.findByProjectIdOrderByIdAsc(projectId);
    if (environments.isEmpty()) {
      return List.of();
    }
    List<Long> envIds = environments.stream().map(Environment::getId).toList();
    Map<Long, List<EnvConfig>> configsMap = envConfigRepository
        .findByEnvironmentIdInOrderByEnvironmentIdAscConfigKeyAsc(envIds)
        .stream()
        .collect(Collectors.groupingBy(EnvConfig::getEnvironmentId));
    return environments.stream()
        .map(env -> new EnvironmentResponse(
            env, configsMap.getOrDefault(env.getId(), List.of())))
        .toList();
  }

  @Transactional(readOnly = true)
  public EnvironmentResponse getEnvironment(Long envId, Long userId) {
    Environment env =
        environmentRepository
            .findById(envId)
            .orElseThrow(() -> new IllegalArgumentException("Environment not found"));
    accessChecker.checkMembership(env.getProjectId(), userId);
    return new EnvironmentResponse(
        env, envConfigRepository.findByEnvironmentIdOrderByConfigKeyAsc(envId));
  }

  @Transactional
  public EnvironmentResponse updateEnvironment(
      Long envId, UpdateEnvironmentRequest request, Long userId) {
    Environment env =
        environmentRepository
            .findById(envId)
            .orElseThrow(() -> new IllegalArgumentException("Environment not found"));
    accessChecker.checkOwnerOrAdmin(env.getProjectId(), env.getCreatedBy(), userId);
    if (environmentRepository.existsByProjectIdAndNameAndIdNot(
        env.getProjectId(), request.getName(), envId)) {
      throw new IllegalArgumentException(
          "Environment with name '" + request.getName() + "' already exists");
    }
    env.setName(request.getName());
    env.setDescription(request.getDescription());
    Environment saved = environmentRepository.save(env);
    return new EnvironmentResponse(
        saved, envConfigRepository.findByEnvironmentIdOrderByConfigKeyAsc(envId));
  }

  @Transactional
  public void deleteEnvironment(Long envId, Long userId) {
    Environment env =
        environmentRepository
            .findById(envId)
            .orElseThrow(() -> new IllegalArgumentException("Environment not found"));
    accessChecker.checkOwnerOrAdmin(env.getProjectId(), env.getCreatedBy(), userId);
    cleanupService.cleanupEnvironmentResources(envId);
    environmentRepository.delete(env);
  }

  @Transactional
  public EnvConfigResponse addConfig(Long envId, EnvConfigRequest request, Long userId) {
    Environment env =
        environmentRepository
            .findById(envId)
            .orElseThrow(() -> new IllegalArgumentException("Environment not found"));
    accessChecker.checkMembership(env.getProjectId(), userId);
    if (envConfigRepository.existsByEnvironmentIdAndConfigKey(envId, request.getConfigKey())) {
      throw new IllegalArgumentException(
          "Config key '" + request.getConfigKey() + "' already exists");
    }
    EnvConfig config = new EnvConfig();
    config.setEnvironmentId(envId);
    config.setConfigKey(request.getConfigKey());
    config.setConfigValue(request.getConfigValue());
    config.setCreatedBy(userId);
    config = envConfigRepository.save(config);
    return new EnvConfigResponse(config);
  }

  @Transactional
  public EnvConfigResponse updateConfig(Long configId, EnvConfigRequest request, Long userId) {
    EnvConfig config =
        envConfigRepository
            .findById(configId)
            .orElseThrow(() -> new IllegalArgumentException("Config not found"));
    Environment env =
        environmentRepository
            .findById(config.getEnvironmentId())
            .orElseThrow(() -> new IllegalArgumentException("Environment not found"));
    accessChecker.checkOwnerOrAdmin(env.getProjectId(), config.getCreatedBy(), userId);
    if (!config.getConfigKey().equals(request.getConfigKey())
        && envConfigRepository.existsByEnvironmentIdAndConfigKeyAndIdNot(
            config.getEnvironmentId(), request.getConfigKey(), configId)) {
      throw new IllegalArgumentException(
          "Config key '" + request.getConfigKey() + "' already exists");
    }
    config.setConfigKey(request.getConfigKey());
    config.setConfigValue(request.getConfigValue());
    config = envConfigRepository.save(config);
    return new EnvConfigResponse(config);
  }

  @Transactional
  public void removeConfig(Long configId, Long userId) {
    EnvConfig config =
        envConfigRepository
            .findById(configId)
            .orElseThrow(() -> new IllegalArgumentException("Config not found"));
    Environment env =
        environmentRepository
            .findById(config.getEnvironmentId())
            .orElseThrow(() -> new IllegalArgumentException("Environment not found"));
    accessChecker.checkOwnerOrAdmin(env.getProjectId(), config.getCreatedBy(), userId);
    envConfigRepository.delete(config);
  }
}
