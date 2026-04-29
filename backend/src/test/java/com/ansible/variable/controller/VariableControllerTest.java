package com.ansible.variable.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.environment.dto.CreateEnvironmentRequest;
import com.ansible.environment.dto.EnvironmentResponse;
import com.ansible.project.dto.CreateProjectRequest;
import com.ansible.project.dto.ProjectResponse;
import com.ansible.project.repository.ProjectMemberRepository;
import com.ansible.project.repository.ProjectRepository;
import com.ansible.role.entity.Role;
import com.ansible.role.repository.RoleRepository;
import com.ansible.user.dto.RegisterRequest;
import com.ansible.user.dto.TokenResponse;
import com.ansible.user.repository.UserRepository;
import com.ansible.variable.dto.CreateVariableRequest;
import com.ansible.variable.dto.VariableResponse;
import com.ansible.variable.entity.VariableScope;
import com.ansible.variable.repository.VariableRepository;
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

class VariableControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private VariableRepository variableRepository;

  private String token;
  private Long projectId;

  @BeforeEach
  void setUp() {
    RegisterRequest reg = new RegisterRequest();
    reg.setUsername("varuser");
    reg.setPassword("password123");
    reg.setEmail("varuser@example.com");
    ResponseEntity<Result<TokenResponse>> regResp =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(reg),
            new ParameterizedTypeReference<>() {});
    token = regResp.getBody().getData().getToken();

    CreateProjectRequest projReq = new CreateProjectRequest();
    projReq.setName("Var Test Project");
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
    variableRepository.deleteAll();
    projectMemberRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private Long createVariable(VariableScope scope, Long scopeId, String key, String value) {
    return createVariable(projectId, scope, scopeId, key, value);
  }

  private Long createVariable(
      Long targetProjectId, VariableScope scope, Long scopeId, String key, String value) {
    CreateVariableRequest req = new CreateVariableRequest(scope, scopeId, key, value);
    ResponseEntity<Result<VariableResponse>> resp =
        restTemplate.exchange(
            "/api/projects/" + targetProjectId + "/variables",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().id();
  }

  private Long createEnvironment(String name) {
    CreateEnvironmentRequest req = new CreateEnvironmentRequest();
    req.setName(name);
    ResponseEntity<Result<EnvironmentResponse>> resp =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/environments",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().getId();
  }

  private Long createProject(String name) {
    CreateProjectRequest req = new CreateProjectRequest();
    req.setName(name);
    ResponseEntity<Result<ProjectResponse>> resp =
        restTemplate.exchange(
            "/api/projects",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().getId();
  }

  private Long createRole(Long targetProjectId, String name) {
    Role role = new Role();
    role.setProjectId(targetProjectId);
    role.setName(name);
    role.setCreatedBy(1L);
    return roleRepository.save(role).getId();
  }

  @Test
  void createVariable_projectScope_success() {
    CreateVariableRequest req =
        new CreateVariableRequest(VariableScope.PROJECT, projectId, "APP_PORT", "8080");

    ResponseEntity<Result<VariableResponse>> resp =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/variables",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().key()).isEqualTo("APP_PORT");
    assertThat(resp.getBody().getData().scope()).isEqualTo(VariableScope.PROJECT);
  }

  @Test
  void createVariable_duplicateKey_returns400() {
    createVariable(VariableScope.PROJECT, projectId, "APP_PORT", "8080");

    CreateVariableRequest req =
        new CreateVariableRequest(VariableScope.PROJECT, projectId, "APP_PORT", "9090");
    ResponseEntity<Result<VariableResponse>> resp =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/variables",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void listVariables_byProjectScope() {
    createVariable(VariableScope.PROJECT, projectId, "PORT", "8080");
    createVariable(VariableScope.PROJECT, projectId, "HOST", "localhost");

    ResponseEntity<Result<List<VariableResponse>>> resp =
        restTemplate.exchange(
            "/api/projects/"
                + projectId
                + "/variables?scope=PROJECT&scopeId="
                + projectId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData()).hasSize(2);
  }

  @Test
  void listVariables_byScopeOnly() {
    createVariable(VariableScope.PROJECT, projectId, "PORT", "8080");

    ResponseEntity<Result<List<VariableResponse>>> resp =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/variables?scope=PROJECT",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData()).hasSize(1);
  }

  @Test
  void listVariables_byScopeOnly_filtersOutOtherProjectVariables() {
    Long otherProjectId = createProject("Other Var Test Project");
    createVariable(VariableScope.PROJECT, projectId, "PORT", "8080");
    createVariable(otherProjectId, VariableScope.PROJECT, otherProjectId, "PORT", "9090");

    ResponseEntity<Result<List<VariableResponse>>> resp =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/variables?scope=PROJECT",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData()).hasSize(1);
    assertThat(resp.getBody().getData().get(0).value()).isEqualTo("8080");
  }

  @Test
  void getVariable_success() {
    Long varId = createVariable(VariableScope.PROJECT, projectId, "APP_PORT", "8080");

    ResponseEntity<Result<VariableResponse>> resp =
        restTemplate.exchange(
            "/api/variables/" + varId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().key()).isEqualTo("APP_PORT");
  }

  @Test
  void updateVariable_success() {
    Long varId = createVariable(VariableScope.PROJECT, projectId, "APP_PORT", "8080");

    var updateReq = new com.ansible.variable.dto.UpdateVariableRequest("PORT", "9090");
    ResponseEntity<Result<VariableResponse>> resp =
        restTemplate.exchange(
            "/api/variables/" + varId,
            HttpMethod.PUT,
            new HttpEntity<>(updateReq, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().key()).isEqualTo("PORT");
  }

  @Test
  void deleteVariable_success() {
    Long varId = createVariable(VariableScope.PROJECT, projectId, "APP_PORT", "8080");

    ResponseEntity<Result<Void>> resp =
        restTemplate.exchange(
            "/api/variables/" + varId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(variableRepository.findById(varId)).isEmpty();
  }

  @Test
  void createVariable_environmentScope_success() {
    Long envId = createEnvironment("dev");
    CreateVariableRequest req =
        new CreateVariableRequest(VariableScope.ENVIRONMENT, envId, "DB_HOST", "localhost");

    ResponseEntity<Result<VariableResponse>> resp =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/variables",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().scope()).isEqualTo(VariableScope.ENVIRONMENT);
  }

  @Test
  void createVariable_roleScopeFromAnotherProject_returns400() {
    Long otherProjectId = createProject("Other Role Project");
    Long otherRoleId = createRole(otherProjectId, "cross-project-role");
    CreateVariableRequest req =
        new CreateVariableRequest(VariableScope.ROLE_VARS, otherRoleId, "DB_HOST", "localhost");

    ResponseEntity<Result<VariableResponse>> resp =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/variables",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(variableRepository.findByScopeAndScopeIdOrderByIdAsc(VariableScope.ROLE_VARS, otherRoleId))
        .isEmpty();
  }
}
