package com.ansible.environment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.environment.dto.CreateEnvironmentRequest;
import com.ansible.environment.dto.EnvConfigRequest;
import com.ansible.environment.dto.EnvConfigResponse;
import com.ansible.environment.dto.EnvironmentResponse;
import com.ansible.environment.dto.UpdateEnvironmentRequest;
import com.ansible.environment.entity.EnvConfig;
import com.ansible.environment.entity.Environment;
import com.ansible.environment.repository.EnvConfigRepository;
import com.ansible.environment.repository.EnvironmentRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EnvironmentServiceTest {

  @Mock private EnvironmentRepository environmentRepository;
  @Mock private EnvConfigRepository envConfigRepository;
  @InjectMocks private EnvironmentService environmentService;

  private Environment testEnv;

  @BeforeEach
  void setUp() {
    testEnv = new Environment();
    ReflectionTestUtils.setField(testEnv, "id", 1L);
    testEnv.setProjectId(10L);
    testEnv.setName("dev");
    testEnv.setDescription("Development environment");
    testEnv.setCreatedBy(100L);
    ReflectionTestUtils.setField(testEnv, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(testEnv, "updatedAt", LocalDateTime.now());
  }

  @Test
  void createEnvironment_success() {
    CreateEnvironmentRequest request = new CreateEnvironmentRequest();
    request.setName("dev");
    request.setDescription("Development environment");

    when(environmentRepository.existsByProjectIdAndName(10L, "dev")).thenReturn(false);
    when(environmentRepository.save(any(Environment.class))).thenReturn(testEnv);

    EnvironmentResponse response = environmentService.createEnvironment(10L, request, 100L);

    assertThat(response.getName()).isEqualTo("dev");
    assertThat(response.getProjectId()).isEqualTo(10L);
    verify(environmentRepository).save(any(Environment.class));
  }

  @Test
  void createEnvironment_duplicateName_throws() {
    CreateEnvironmentRequest request = new CreateEnvironmentRequest();
    request.setName("dev");

    when(environmentRepository.existsByProjectIdAndName(10L, "dev")).thenReturn(true);

    assertThatThrownBy(() -> environmentService.createEnvironment(10L, request, 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void listEnvironments_success() {
    Environment env2 = new Environment();
    ReflectionTestUtils.setField(env2, "id", 2L);
    env2.setProjectId(10L);
    env2.setName("prod");

    when(environmentRepository.findByProjectIdOrderByIdAsc(10L))
        .thenReturn(List.of(testEnv, env2));
    when(envConfigRepository.findByEnvironmentIdOrderByConfigKeyAsc(anyLong()))
        .thenReturn(List.of());

    List<EnvironmentResponse> list = environmentService.listEnvironments(10L);

    assertThat(list).hasSize(2);
    assertThat(list.get(0).getName()).isEqualTo("dev");
    assertThat(list.get(1).getName()).isEqualTo("prod");
  }

  @Test
  void getEnvironment_success() {
    when(environmentRepository.findById(1L)).thenReturn(Optional.of(testEnv));
    when(envConfigRepository.findByEnvironmentIdOrderByConfigKeyAsc(1L)).thenReturn(List.of());

    EnvironmentResponse response = environmentService.getEnvironment(1L);

    assertThat(response.getName()).isEqualTo("dev");
    assertThat(response.getDescription()).isEqualTo("Development environment");
  }

  @Test
  void getEnvironment_notFound_throws() {
    when(environmentRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> environmentService.getEnvironment(99L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Environment not found");
  }

  @Test
  void updateEnvironment_success() {
    UpdateEnvironmentRequest request = new UpdateEnvironmentRequest();
    request.setName("staging");
    request.setDescription("Staging environment");

    when(environmentRepository.findById(1L)).thenReturn(Optional.of(testEnv));
    when(environmentRepository.existsByProjectIdAndNameAndIdNot(10L, "staging", 1L))
        .thenReturn(false);
    when(environmentRepository.save(any(Environment.class))).thenReturn(testEnv);
    when(envConfigRepository.findByEnvironmentIdOrderByConfigKeyAsc(1L)).thenReturn(List.of());

    EnvironmentResponse response = environmentService.updateEnvironment(1L, request, 100L);

    assertThat(response).isNotNull();
    verify(environmentRepository).save(any(Environment.class));
  }

  @Test
  void updateEnvironment_duplicateName_throws() {
    UpdateEnvironmentRequest request = new UpdateEnvironmentRequest();
    request.setName("staging");

    when(environmentRepository.findById(1L)).thenReturn(Optional.of(testEnv));
    when(environmentRepository.existsByProjectIdAndNameAndIdNot(10L, "staging", 1L))
        .thenReturn(true);

    assertThatThrownBy(() -> environmentService.updateEnvironment(1L, request, 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void updateEnvironment_notFound_throws() {
    UpdateEnvironmentRequest request = new UpdateEnvironmentRequest();
    request.setName("staging");

    when(environmentRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> environmentService.updateEnvironment(99L, request, 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Environment not found");
  }

  @Test
  void deleteEnvironment_success() {
    when(environmentRepository.findById(1L)).thenReturn(Optional.of(testEnv));

    environmentService.deleteEnvironment(1L, 100L);

    verify(envConfigRepository).deleteByEnvironmentId(1L);
    verify(environmentRepository).delete(testEnv);
  }

  @Test
  void deleteEnvironment_notFound_throws() {
    when(environmentRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> environmentService.deleteEnvironment(99L, 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Environment not found");
  }

  @Test
  void addConfig_success() {
    EnvConfig savedConfig = new EnvConfig();
    ReflectionTestUtils.setField(savedConfig, "id", 10L);
    savedConfig.setEnvironmentId(1L);
    savedConfig.setConfigKey("DB_HOST");
    savedConfig.setConfigValue("localhost");

    when(environmentRepository.findById(1L)).thenReturn(Optional.of(testEnv));
    when(envConfigRepository.existsByEnvironmentIdAndConfigKey(1L, "DB_HOST")).thenReturn(false);
    when(envConfigRepository.save(any(EnvConfig.class))).thenReturn(savedConfig);

    EnvConfigRequest request = new EnvConfigRequest();
    request.setConfigKey("DB_HOST");
    request.setConfigValue("localhost");
    EnvConfigResponse response = environmentService.addConfig(1L, request, 100L);

    assertThat(response.getConfigKey()).isEqualTo("DB_HOST");
    assertThat(response.getConfigValue()).isEqualTo("localhost");
  }

  @Test
  void addConfig_duplicateKey_throws() {
    when(environmentRepository.findById(1L)).thenReturn(Optional.of(testEnv));
    when(envConfigRepository.existsByEnvironmentIdAndConfigKey(1L, "DB_HOST")).thenReturn(true);

    EnvConfigRequest request = new EnvConfigRequest();
    request.setConfigKey("DB_HOST");
    request.setConfigValue("localhost");

    assertThatThrownBy(() -> environmentService.addConfig(1L, request, 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void addConfig_environmentNotFound_throws() {
    when(environmentRepository.findById(99L)).thenReturn(Optional.empty());

    EnvConfigRequest request = new EnvConfigRequest();
    request.setConfigKey("DB_HOST");
    request.setConfigValue("localhost");

    assertThatThrownBy(() -> environmentService.addConfig(99L, request, 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Environment not found");
  }

  @Test
  void removeConfig_success() {
    EnvConfig config = new EnvConfig();
    ReflectionTestUtils.setField(config, "id", 10L);
    config.setEnvironmentId(1L);
    config.setConfigKey("DB_HOST");
    config.setConfigValue("localhost");

    when(envConfigRepository.findById(10L)).thenReturn(Optional.of(config));

    environmentService.removeConfig(10L);

    verify(envConfigRepository).delete(config);
  }

  @Test
  void removeConfig_notFound_throws() {
    when(envConfigRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> environmentService.removeConfig(99L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Config not found");
  }
}
