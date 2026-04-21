package com.ansible.environment.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnvironmentService {

  private final EnvironmentRepository environmentRepository;
  private final EnvConfigRepository envConfigRepository;

  @Transactional
  public EnvironmentResponse createEnvironment(
      Long projectId, CreateEnvironmentRequest request, Long userId) {
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
  public List<EnvironmentResponse> listEnvironments(Long projectId) {
    return environmentRepository.findByProjectIdOrderByIdAsc(projectId).stream()
        .map(
            env ->
                new EnvironmentResponse(
                    env,
                    envConfigRepository.findByEnvironmentIdOrderByConfigKeyAsc(env.getId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public EnvironmentResponse getEnvironment(Long envId) {
    Environment env =
        environmentRepository
            .findById(envId)
            .orElseThrow(() -> new IllegalArgumentException("Environment not found"));
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
    envConfigRepository.deleteByEnvironmentId(envId);
    environmentRepository.delete(env);
  }

  @Transactional
  public EnvConfigResponse addConfig(Long envId, EnvConfigRequest request, Long userId) {
    environmentRepository
        .findById(envId)
        .orElseThrow(() -> new IllegalArgumentException("Environment not found"));
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
  public void removeConfig(Long configId) {
    EnvConfig config =
        envConfigRepository
            .findById(configId)
            .orElseThrow(() -> new IllegalArgumentException("Config not found"));
    envConfigRepository.delete(config);
  }
}
