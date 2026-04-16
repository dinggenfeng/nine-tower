package com.ansible.role.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.project.dto.CreateProjectRequest;
import com.ansible.project.dto.ProjectResponse;
import com.ansible.project.repository.ProjectMemberRepository;
import com.ansible.project.repository.ProjectRepository;
import com.ansible.role.dto.CreateHandlerRequest;
import com.ansible.role.dto.CreateRoleRequest;
import com.ansible.role.dto.CreateTaskRequest;
import com.ansible.role.dto.HandlerResponse;
import com.ansible.role.dto.RoleResponse;
import com.ansible.role.dto.TaskResponse;
import com.ansible.role.dto.UpdateHandlerRequest;
import com.ansible.role.repository.HandlerRepository;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.TaskRepository;
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

class HandlerControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private HandlerRepository handlerRepository;
  @Autowired private TaskRepository taskRepository;

  private String token;
  private Long projectId;
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
    projectId = projResp.getBody().getData().getId();

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
    taskRepository.deleteAll();
    handlerRepository.deleteAll();
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

  private Long createHandler(String name, String module) {
    CreateHandlerRequest req = new CreateHandlerRequest();
    req.setName(name);
    req.setModule(module);
    req.setArgs("{\"name\":\"nginx\",\"state\":\"restarted\"}");
    ResponseEntity<Result<HandlerResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/handlers",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().getId();
  }

  @Test
  void createHandler_success() {
    CreateHandlerRequest req = new CreateHandlerRequest();
    req.setName("Restart nginx");
    req.setModule("service");
    req.setArgs("{\"name\":\"nginx\",\"state\":\"restarted\"}");

    ResponseEntity<Result<HandlerResponse>> response =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/handlers",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    HandlerResponse data = response.getBody().getData();
    assertThat(data.getName()).isEqualTo("Restart nginx");
    assertThat(data.getModule()).isEqualTo("service");
  }

  @Test
  void createHandler_withBecome() {
    CreateHandlerRequest req = new CreateHandlerRequest();
    req.setName("Restart nginx");
    req.setModule("service");
    req.setBecome(true);
    req.setBecomeUser("root");

    ResponseEntity<Result<HandlerResponse>> response =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/handlers",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    HandlerResponse data = response.getBody().getData();
    assertThat(data.getBecome()).isTrue();
    assertThat(data.getBecomeUser()).isEqualTo("root");
  }

  @Test
  void getHandlers_returns_list() {
    createHandler("Restart nginx", "service");
    createHandler("Reload nginx", "service");

    ResponseEntity<Result<List<HandlerResponse>>> response =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/handlers",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData()).hasSize(2);
  }

  @Test
  void getHandler_success() {
    Long handlerId = createHandler("Restart nginx", "service");

    ResponseEntity<Result<HandlerResponse>> response =
        restTemplate.exchange(
            "/api/handlers/" + handlerId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("Restart nginx");
  }

  @Test
  void updateHandler_success() {
    Long handlerId = createHandler("Restart nginx", "service");

    UpdateHandlerRequest req = new UpdateHandlerRequest();
    req.setName("Reload nginx");

    ResponseEntity<Result<HandlerResponse>> response =
        restTemplate.exchange(
            "/api/handlers/" + handlerId,
            HttpMethod.PUT,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("Reload nginx");
  }

  @Test
  void getNotifiedBy_success() {
    Long handlerId = createHandler("Restart nginx", "service");

    CreateTaskRequest taskReq = new CreateTaskRequest();
    taskReq.setName("Install nginx");
    taskReq.setModule("apt");
    taskReq.setNotify(List.of("Restart nginx"));
    taskReq.setTaskOrder(1);
    restTemplate.exchange(
        "/api/roles/" + roleId + "/tasks",
        HttpMethod.POST,
        new HttpEntity<>(taskReq, authHeaders()),
        new ParameterizedTypeReference<Result<TaskResponse>>() {});

    ResponseEntity<Result<List<TaskResponse>>> response =
        restTemplate.exchange(
            "/api/handlers/" + handlerId + "/notified-by",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<TaskResponse> tasks = response.getBody().getData();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("Install nginx");
  }

  @Test
  void deleteHandler_success() {
    Long handlerId = createHandler("Restart nginx", "service");

    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/handlers/" + handlerId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(handlerRepository.findById(handlerId)).isEmpty();
  }
}
