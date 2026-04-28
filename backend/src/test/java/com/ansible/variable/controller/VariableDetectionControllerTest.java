package com.ansible.variable.controller;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.variable.dto.*;
import com.ansible.variable.entity.VariableScope;
import com.ansible.user.repository.UserRepository;
import com.ansible.project.repository.*;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.TaskRepository;
import com.ansible.role.repository.HandlerRepository;
import com.ansible.role.repository.TemplateRepository;
import com.ansible.variable.repository.VariableRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class VariableDetectionControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private TaskRepository taskRepository;
  @Autowired private HandlerRepository handlerRepository;
  @Autowired private TemplateRepository templateRepository;
  @Autowired private VariableRepository variableRepository;

  private String token;
  private Long projectId;
  private Long roleId;

  @BeforeEach
  void setUp() {
    // Register user and get token
    var registerReq = Map.of(
        "username", "testuser_" + System.currentTimeMillis(),
        "password", "password123",
        "email", "testuser_" + System.currentTimeMillis() + "@example.com");
    var authRes = restTemplate.postForEntity("/api/auth/register", registerReq, Map.class);
    token = (String) ((Map) authRes.getBody().get("data")).get("token");

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    // Create project
    var projectReq = Map.of("name", "Test Project " + System.currentTimeMillis(), "description", "test");
    var projectRes = restTemplate.exchange("/api/projects", HttpMethod.POST,
        new HttpEntity<>(projectReq, headers),
        new ParameterizedTypeReference<Result<Map<String, Object>>>() {});
    projectId = ((Number) projectRes.getBody().getData().get("id")).longValue();

    // Create role
    var roleReq = Map.of("name", "Test Role");
    var roleRes = restTemplate.exchange("/api/projects/" + projectId + "/roles", HttpMethod.POST,
        new HttpEntity<>(roleReq, headers),
        new ParameterizedTypeReference<Result<Map<String, Object>>>() {});
    roleId = ((Number) roleRes.getBody().getData().get("id")).longValue();

    // Create a task with variable reference
    var taskReq = Map.of(
        "name", "Start {{ app_port }}",
        "module", "shell",
        "args", "{\"cmd\":\"start --port {{ app_port }}\"}",
        "taskOrder", 1);
    restTemplate.exchange("/api/roles/" + roleId + "/tasks", HttpMethod.POST,
        new HttpEntity<>(taskReq, headers),
        new ParameterizedTypeReference<Result<Map<String, Object>>>() {});
  }

  @AfterEach
  void tearDown() {
    variableRepository.deleteAll();
    templateRepository.deleteAll();
    handlerRepository.deleteAll();
    taskRepository.deleteAll();
    roleRepository.deleteAll();
    projectMemberRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void detectVariables_returnsDetectedVars() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    var response = restTemplate.exchange("/api/projects/" + projectId + "/detect-variables",
        HttpMethod.GET, new HttpEntity<>(headers),
        new ParameterizedTypeReference<Result<List<DetectedVariableResponse>>>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<DetectedVariableResponse> vars = response.getBody().getData();
    assertThat(vars).isNotEmpty();
    DetectedVariableResponse appPort = vars.stream()
        .filter(v -> v.key().equals("app_port")).findFirst().orElseThrow();
    assertThat(appPort.suggestedScope()).isEqualTo("ROLE");
  }

  @Test
  void batchSave_savesVariableWithScopeAndScopeId() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    List<BatchVariableSaveRequest> requests = List.of(
        new BatchVariableSaveRequest("my_var", VariableScope.ROLE_VARS, roleId, "test_value")
    );
    var response = restTemplate.exchange("/api/projects/" + projectId + "/variables/batch",
        HttpMethod.POST, new HttpEntity<>(requests, headers),
        new ParameterizedTypeReference<Result<List<Map<String, Object>>>>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Map<String, Object>> results = response.getBody().getData();
    assertThat(results).hasSize(1);
    assertThat(results.get(0).get("success")).isEqualTo(true);

    var saved = variableRepository.findByScopeAndScopeIdOrderByIdAsc(VariableScope.ROLE_VARS, roleId);
    assertThat(saved).hasSize(1);
    assertThat(saved.get(0).getKey()).isEqualTo("my_var");
  }

  @Test
  void batchSave_rejectsDuplicateVariable() {
    // Pre-create a variable with the same scope/scopeId/key
    com.ansible.variable.entity.Variable existing = new com.ansible.variable.entity.Variable();
    existing.setScope(VariableScope.ROLE_VARS);
    existing.setScopeId(roleId);
    existing.setKey("my_var");
    existing.setValue("old");
    existing.setCreatedBy(1L);
    variableRepository.save(existing);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    List<BatchVariableSaveRequest> requests = List.of(
        new BatchVariableSaveRequest("my_var", VariableScope.ROLE_VARS, roleId, "new")
    );
    var response = restTemplate.exchange("/api/projects/" + projectId + "/variables/batch",
        HttpMethod.POST, new HttpEntity<>(requests, headers),
        new ParameterizedTypeReference<Result<List<Map<String, Object>>>>() {});

    List<Map<String, Object>> results = response.getBody().getData();
    assertThat(results.get(0).get("success")).isEqualTo(false);
  }
}
