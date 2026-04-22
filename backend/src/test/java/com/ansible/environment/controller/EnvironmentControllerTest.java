package com.ansible.environment.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.environment.dto.CreateEnvironmentRequest;
import com.ansible.environment.dto.EnvConfigRequest;
import com.ansible.environment.dto.EnvConfigResponse;
import com.ansible.environment.dto.EnvironmentResponse;
import com.ansible.environment.dto.UpdateEnvironmentRequest;
import com.ansible.environment.repository.EnvConfigRepository;
import com.ansible.environment.repository.EnvironmentRepository;
import com.ansible.project.dto.CreateProjectRequest;
import com.ansible.project.dto.ProjectResponse;
import com.ansible.project.repository.ProjectMemberRepository;
import com.ansible.project.repository.ProjectRepository;
import com.ansible.user.dto.RegisterRequest;
import com.ansible.user.dto.TokenResponse;
import com.ansible.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class EnvironmentControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;
  @Autowired private EnvironmentRepository environmentRepository;
  @Autowired private EnvConfigRepository envConfigRepository;

  private String token;
  private Long projectId;

  @BeforeEach
  void setUp() {
    RegisterRequest reg = new RegisterRequest();
    reg.setUsername("envuser");
    reg.setPassword("password123");
    reg.setEmail("envuser@example.com");
    ResponseEntity<Result<TokenResponse>> regResp =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(reg),
            new ParameterizedTypeReference<>() {});
    token = regResp.getBody().getData().getToken();

    CreateProjectRequest projReq = new CreateProjectRequest();
    projReq.setName("Env Test Project");
    ResponseEntity<Result<ProjectResponse>> projResp =
        restTemplate.exchange(
            "/api/projects",
            HttpMethod.POST,
            new HttpEntity<>(projReq, authHeaders()),
            new ParameterizedTypeReference<>() {});
    projectId = projResp.getBody().getData().getId();
  }

  @AfterEach
  void tearDown() {
    envConfigRepository.deleteAll();
    environmentRepository.deleteAll();
    projectMemberRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private Long createEnvironment(String name, String description) {
    CreateEnvironmentRequest req = new CreateEnvironmentRequest();
    req.setName(name);
    req.setDescription(description);
    ResponseEntity<Result<EnvironmentResponse>> resp =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/environments",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().getId();
  }

  @Test
  void createEnvironment_success() {
    CreateEnvironmentRequest req = new CreateEnvironmentRequest();
    req.setName("dev");
    req.setDescription("Development environment");

    ResponseEntity<Result<EnvironmentResponse>> resp =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/environments",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getName()).isEqualTo("dev");
    assertThat(resp.getBody().getData().getDescription()).isEqualTo("Development environment");
  }

  @Test
  void createEnvironment_duplicateName_returns400() {
    createEnvironment("dev", null);

    CreateEnvironmentRequest req = new CreateEnvironmentRequest();
    req.setName("dev");

    ResponseEntity<Result<EnvironmentResponse>> resp =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/environments",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void listEnvironments_success() {
    createEnvironment("dev", "Development");
    createEnvironment("prod", "Production");

    ResponseEntity<Result<List<EnvironmentResponse>>> resp =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/environments",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData()).hasSize(2);
  }

  @Test
  void getEnvironment_success() {
    Long envId = createEnvironment("dev", "Dev env");

    ResponseEntity<Result<EnvironmentResponse>> resp =
        restTemplate.exchange(
            "/api/environments/" + envId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getName()).isEqualTo("dev");
  }

  @Test
  void updateEnvironment_success() {
    Long envId = createEnvironment("dev", "Old description");

    UpdateEnvironmentRequest req = new UpdateEnvironmentRequest();
    req.setName("staging");
    req.setDescription("Staging environment");

    ResponseEntity<Result<EnvironmentResponse>> resp =
        restTemplate.exchange(
            "/api/environments/" + envId,
            HttpMethod.PUT,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getName()).isEqualTo("staging");
  }

  @Test
  void deleteEnvironment_success() {
    Long envId = createEnvironment("dev", null);

    ResponseEntity<Result<Void>> resp =
        restTemplate.exchange(
            "/api/environments/" + envId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(environmentRepository.findById(envId)).isEmpty();
  }

  @Test
  void addConfig_success() {
    Long envId = createEnvironment("dev", null);

    EnvConfigRequest req = new EnvConfigRequest();
    req.setConfigKey("DB_HOST");
    req.setConfigValue("localhost");

    ResponseEntity<Result<EnvConfigResponse>> resp =
        restTemplate.exchange(
            "/api/environments/" + envId + "/configs",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getConfigKey()).isEqualTo("DB_HOST");
    assertThat(resp.getBody().getData().getConfigValue()).isEqualTo("localhost");
  }

  @Test
  void addConfig_duplicateKey_returns400() {
    Long envId = createEnvironment("dev", null);

    EnvConfigRequest req = new EnvConfigRequest();
    req.setConfigKey("DB_HOST");
    req.setConfigValue("localhost");

    restTemplate.exchange(
        "/api/environments/" + envId + "/configs",
        HttpMethod.POST,
        new HttpEntity<>(req, authHeaders()),
        new ParameterizedTypeReference<Result<EnvConfigResponse>>() {});

    ResponseEntity<Result<EnvConfigResponse>> resp =
        restTemplate.exchange(
            "/api/environments/" + envId + "/configs",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void removeConfig_success() {
    Long envId = createEnvironment("dev", null);

    EnvConfigRequest req = new EnvConfigRequest();
    req.setConfigKey("DB_HOST");
    req.setConfigValue("localhost");

    ResponseEntity<Result<EnvConfigResponse>> configResp =
        restTemplate.exchange(
            "/api/environments/" + envId + "/configs",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    Long configId = configResp.getBody().getData().getId();

    ResponseEntity<Result<Void>> resp =
        restTemplate.exchange(
            "/api/env-configs/" + configId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(envConfigRepository.findById(configId)).isEmpty();
  }

  @Test
  void updateConfig_success() {
    Long envId = createEnvironment("dev", null);

    EnvConfigRequest createReq = new EnvConfigRequest();
    createReq.setConfigKey("DB_HOST");
    createReq.setConfigValue("localhost");

    ResponseEntity<Result<EnvConfigResponse>> configResp =
        restTemplate.exchange(
            "/api/environments/" + envId + "/configs",
            HttpMethod.POST,
            new HttpEntity<>(createReq, authHeaders()),
            new ParameterizedTypeReference<>() {});
    Long configId = configResp.getBody().getData().getId();

    EnvConfigRequest updateReq = new EnvConfigRequest();
    updateReq.setConfigKey("DB_HOST");
    updateReq.setConfigValue("192.168.1.1");

    ResponseEntity<Result<EnvConfigResponse>> resp =
        restTemplate.exchange(
            "/api/env-configs/" + configId,
            HttpMethod.PUT,
            new HttpEntity<>(updateReq, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getConfigKey()).isEqualTo("DB_HOST");
    assertThat(resp.getBody().getData().getConfigValue()).isEqualTo("192.168.1.1");
  }

  @Test
  void updateConfig_duplicateKey_returns400() {
    Long envId = createEnvironment("dev", null);

    EnvConfigRequest req1 = new EnvConfigRequest();
    req1.setConfigKey("DB_HOST");
    req1.setConfigValue("localhost");
    restTemplate.exchange(
        "/api/environments/" + envId + "/configs",
        HttpMethod.POST,
        new HttpEntity<>(req1, authHeaders()),
        new ParameterizedTypeReference<Result<EnvConfigResponse>>() {});

    EnvConfigRequest req2 = new EnvConfigRequest();
    req2.setConfigKey("DB_PORT");
    req2.setConfigValue("5432");
    ResponseEntity<Result<EnvConfigResponse>> config2Resp =
        restTemplate.exchange(
            "/api/environments/" + envId + "/configs",
            HttpMethod.POST,
            new HttpEntity<>(req2, authHeaders()),
            new ParameterizedTypeReference<>() {});
    Long config2Id = config2Resp.getBody().getData().getId();

    EnvConfigRequest updateReq = new EnvConfigRequest();
    updateReq.setConfigKey("DB_HOST");
    updateReq.setConfigValue("3306");

    ResponseEntity<Result<EnvConfigResponse>> resp =
        restTemplate.exchange(
            "/api/env-configs/" + config2Id,
            HttpMethod.PUT,
            new HttpEntity<>(updateReq, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void deleteEnvironment_cascadesConfigs() {
    Long envId = createEnvironment("dev", null);

    EnvConfigRequest req = new EnvConfigRequest();
    req.setConfigKey("KEY");
    req.setConfigValue("val");
    restTemplate.exchange(
        "/api/environments/" + envId + "/configs",
        HttpMethod.POST,
        new HttpEntity<>(req, authHeaders()),
        new ParameterizedTypeReference<Result<EnvConfigResponse>>() {});

    restTemplate.exchange(
        "/api/environments/" + envId,
        HttpMethod.DELETE,
        new HttpEntity<>(authHeaders()),
        new ParameterizedTypeReference<Result<Void>>() {});

    assertThat(environmentRepository.findById(envId)).isEmpty();
    assertThat(envConfigRepository.findByEnvironmentIdOrderByConfigKeyAsc(envId)).isEmpty();
  }
}
