package com.ansible.role.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.project.dto.CreateProjectRequest;
import com.ansible.project.dto.ProjectResponse;
import com.ansible.project.repository.ProjectMemberRepository;
import com.ansible.project.repository.ProjectRepository;
import com.ansible.role.dto.CreateRoleRequest;
import com.ansible.role.dto.CreateRoleDefaultVariableRequest;
import com.ansible.role.dto.RoleDefaultVariableResponse;
import com.ansible.role.dto.RoleResponse;
import com.ansible.role.dto.UpdateRoleDefaultVariableRequest;
import com.ansible.role.repository.RoleDefaultVariableRepository;
import com.ansible.role.repository.RoleRepository;
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

class RoleDefaultVariableControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private RoleDefaultVariableRepository roleDefaultVariableRepository;

  private String token;
  private Long roleId;

  @BeforeEach
  void setUp() {
    RegisterRequest reg = new RegisterRequest();
    reg.setUsername("alice");
    reg.setPassword("password123");
    reg.setEmail("alice@example.com");
    ResponseEntity<Result<TokenResponse>> regResp =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(reg),
            new ParameterizedTypeReference<>() {});
    token = regResp.getBody().getData().getToken();

    CreateProjectRequest projReq = new CreateProjectRequest();
    projReq.setName("Test Project");
    ResponseEntity<Result<ProjectResponse>> projResp =
        restTemplate.exchange(
            "/api/projects",
            HttpMethod.POST,
            new HttpEntity<>(projReq, authHeaders()),
            new ParameterizedTypeReference<>() {});
    Long projectId = projResp.getBody().getData().getId();

    CreateRoleRequest roleReq = new CreateRoleRequest();
    roleReq.setName("nginx");
    ResponseEntity<Result<RoleResponse>> roleResp =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/roles",
            HttpMethod.POST,
            new HttpEntity<>(roleReq, authHeaders()),
            new ParameterizedTypeReference<>() {});
    roleId = roleResp.getBody().getData().getId();
  }

  @AfterEach
  void tearDown() {
    roleDefaultVariableRepository.deleteAll();
    roleRepository.deleteAll();
    projectMemberRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private Long createDefault(String key, String value) {
    CreateRoleDefaultVariableRequest req = new CreateRoleDefaultVariableRequest();
    req.setKey(key);
    req.setValue(value);
    ResponseEntity<Result<RoleDefaultVariableResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/defaults",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().getId();
  }

  @Test
  void createDefault_success() {
    CreateRoleDefaultVariableRequest req = new CreateRoleDefaultVariableRequest();
    req.setKey("http_port");
    req.setValue("80");

    ResponseEntity<Result<RoleDefaultVariableResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/defaults",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getKey()).isEqualTo("http_port");
    assertThat(resp.getBody().getData().getValue()).isEqualTo("80");
  }

  @Test
  void createDefault_duplicateKey() {
    createDefault("http_port", "80");

    CreateRoleDefaultVariableRequest req = new CreateRoleDefaultVariableRequest();
    req.setKey("http_port");
    req.setValue("8080");

    ResponseEntity<Result<RoleDefaultVariableResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/defaults",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void getDefaults_success() {
    createDefault("http_port", "80");
    createDefault("server_name", "localhost");

    ResponseEntity<Result<List<RoleDefaultVariableResponse>>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/defaults",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData()).hasSize(2);
  }

  @Test
  void updateDefault_success() {
    Long varId = createDefault("http_port", "80");

    UpdateRoleDefaultVariableRequest req = new UpdateRoleDefaultVariableRequest();
    req.setValue("8080");

    ResponseEntity<Result<RoleDefaultVariableResponse>> resp =
        restTemplate.exchange(
            "/api/role-defaults/" + varId,
            HttpMethod.PUT,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getValue()).isEqualTo("8080");
  }

  @Test
  void deleteDefault_success() {
    Long varId = createDefault("http_port", "80");

    ResponseEntity<Result<Void>> resp =
        restTemplate.exchange(
            "/api/role-defaults/" + varId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Result<List<RoleDefaultVariableResponse>>> listResp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/defaults",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});
    assertThat(listResp.getBody().getData()).isEmpty();
  }
}
