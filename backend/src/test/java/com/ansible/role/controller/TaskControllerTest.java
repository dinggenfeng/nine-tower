package com.ansible.role.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.project.dto.CreateProjectRequest;
import com.ansible.project.dto.ProjectResponse;
import com.ansible.project.repository.ProjectMemberRepository;
import com.ansible.project.repository.ProjectRepository;
import com.ansible.role.dto.CreateRoleRequest;
import com.ansible.role.dto.CreateTaskRequest;
import com.ansible.role.dto.RoleResponse;
import com.ansible.role.dto.TaskResponse;
import com.ansible.role.dto.UpdateTaskRequest;
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

class TaskControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;
  @Autowired private RoleRepository roleRepository;
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

  private Long createTask(String name, String module, int order) {
    CreateTaskRequest req = new CreateTaskRequest();
    req.setName(name);
    req.setModule(module);
    req.setNotify(List.of("Restart nginx"));
    req.setTaskOrder(order);
    ResponseEntity<Result<TaskResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/tasks",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().getId();
  }

  @Test
  void createTask_success() {
    CreateTaskRequest req = new CreateTaskRequest();
    req.setName("Install nginx");
    req.setModule("apt");
    req.setArgs("{\"name\":\"nginx\",\"state\":\"present\"}");
    req.setNotify(List.of("Restart nginx"));
    req.setTaskOrder(1);

    ResponseEntity<Result<TaskResponse>> response =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/tasks",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    TaskResponse data = response.getBody().getData();
    assertThat(data.getName()).isEqualTo("Install nginx");
    assertThat(data.getModule()).isEqualTo("apt");
    assertThat(data.getNotify()).containsExactly("Restart nginx");
  }

  @Test
  void getTasks_returns_ordered_list() {
    createTask("Second task", "shell", 2);
    createTask("First task", "apt", 1);

    ResponseEntity<Result<List<TaskResponse>>> response =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/tasks",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<TaskResponse> tasks = response.getBody().getData();
    assertThat(tasks).hasSize(2);
    assertThat(tasks.get(0).getName()).isEqualTo("First task");
    assertThat(tasks.get(1).getName()).isEqualTo("Second task");
  }

  @Test
  void getTask_success() {
    Long taskId = createTask("Install nginx", "apt", 1);

    ResponseEntity<Result<TaskResponse>> response =
        restTemplate.exchange(
            "/api/tasks/" + taskId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("Install nginx");
  }

  @Test
  void updateTask_success() {
    Long taskId = createTask("Install nginx", "apt", 1);

    UpdateTaskRequest req = new UpdateTaskRequest();
    req.setName("Install apache");
    req.setModule("apt");

    ResponseEntity<Result<TaskResponse>> response =
        restTemplate.exchange(
            "/api/tasks/" + taskId,
            HttpMethod.PUT,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("Install apache");
  }

  @Test
  void deleteTask_success() {
    Long taskId = createTask("Install nginx", "apt", 1);

    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/tasks/" + taskId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(taskRepository.findById(taskId)).isEmpty();
  }
}
