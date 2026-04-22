package com.ansible.role.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.common.enums.ProjectRole;
import com.ansible.project.dto.AddMemberRequest;
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
import com.ansible.role.dto.UpdateTaskRequest;
import com.ansible.role.repository.HandlerRepository;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.TaskRepository;
import com.ansible.tag.entity.Tag;
import com.ansible.tag.repository.TagRepository;
import com.ansible.tag.repository.TaskTagRepository;
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
  @Autowired private HandlerRepository handlerRepository;
  @Autowired private TagRepository tagRepository;
  @Autowired private TaskTagRepository taskTagRepository;

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
    taskTagRepository.deleteAll();
    tagRepository.deleteAll();
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
  void createTask_withBecome() {
    CreateTaskRequest req = new CreateTaskRequest();
    req.setName("Install nginx");
    req.setModule("apt");
    req.setTaskOrder(1);
    req.setBecome(true);
    req.setBecomeUser("root");

    ResponseEntity<Result<TaskResponse>> response =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/tasks",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    TaskResponse data = response.getBody().getData();
    assertThat(data.getBecome()).isTrue();
    assertThat(data.getBecomeUser()).isEqualTo("root");
  }

  @Test
  void createTask_withIgnoreErrors() {
    CreateTaskRequest req = new CreateTaskRequest();
    req.setName("Install nginx");
    req.setModule("apt");
    req.setTaskOrder(1);
    req.setIgnoreErrors(true);

    ResponseEntity<Result<TaskResponse>> response =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/tasks",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    TaskResponse data = response.getBody().getData();
    assertThat(data.getIgnoreErrors()).isTrue();
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
  void getNotifiedHandlers_success() {
    CreateHandlerRequest handlerReq = new CreateHandlerRequest();
    handlerReq.setName("Restart nginx");
    handlerReq.setModule("service");
    restTemplate.exchange(
        "/api/roles/" + roleId + "/handlers",
        HttpMethod.POST,
        new HttpEntity<>(handlerReq, authHeaders()),
        new ParameterizedTypeReference<Result<HandlerResponse>>() {});

    Long taskId = createTask("Install nginx", "apt", 1);

    ResponseEntity<Result<List<HandlerResponse>>> response =
        restTemplate.exchange(
            "/api/tasks/" + taskId + "/notifies",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<HandlerResponse> handlers = response.getBody().getData();
    assertThat(handlers).hasSize(1);
    assertThat(handlers.get(0).getName()).isEqualTo("Restart nginx");
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

  @Test
  void updateTaskTags_success() {
    Long taskId = createTask("Install nginx", "apt", 1);
    // Create tags in the project
    Tag tag1 = new Tag();
    tag1.setProjectId(projectId);
    tag1.setName("tag1");
    tag1.setCreatedBy(1L);
    tag1 = tagRepository.save(tag1);

    Tag tag2 = new Tag();
    tag2.setProjectId(projectId);
    tag2.setName("tag2");
    tag2.setCreatedBy(1L);
    tag2 = tagRepository.save(tag2);

    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/tasks/" + taskId + "/tags",
            HttpMethod.PUT,
            new HttpEntity<>(List.of(tag1.getId(), tag2.getId()), authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(taskTagRepository.findByTaskId(taskId)).hasSize(2);
  }

  @Test
  void getTaskTags_success() {
    Long taskId = createTask("Install nginx", "apt", 1);

    // Verify empty initially
    ResponseEntity<Result<List<Long>>> emptyResp =
        restTemplate.exchange(
            "/api/tasks/" + taskId + "/tags",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});
    assertThat(emptyResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(emptyResp.getBody().getData()).isEmpty();

    // Create and associate tags
    Tag tag1 = new Tag();
    tag1.setProjectId(projectId);
    tag1.setName("tagA");
    tag1.setCreatedBy(1L);
    tag1 = tagRepository.save(tag1);

    restTemplate.exchange(
        "/api/tasks/" + taskId + "/tags",
        HttpMethod.PUT,
        new HttpEntity<>(List.of(tag1.getId()), authHeaders()),
        new ParameterizedTypeReference<Result<Void>>() {});

    ResponseEntity<Result<List<Long>>> response =
        restTemplate.exchange(
            "/api/tasks/" + taskId + "/tags",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData()).containsExactly(tag1.getId());
  }

  @Test
  void reorderTasks_success() {
    Long t1 = createTask("Task 1", "apt", 1);
    Long t2 = createTask("Task 2", "copy", 2);
    Long t3 = createTask("Task 3", "service", 3);

    ResponseEntity<Result<Void>> reorderResp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/tasks/order",
            HttpMethod.PUT,
            new HttpEntity<>(List.of(t3, t1, t2), authHeaders()),
            new ParameterizedTypeReference<>() {});
    assertThat(reorderResp.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Result<List<TaskResponse>>> listResp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/tasks",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});
    assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<TaskResponse> tasks = listResp.getBody().getData();
    assertThat(tasks).hasSize(3);
    assertThat(tasks.get(0).getName()).isEqualTo("Task 3");
    assertThat(tasks.get(1).getName()).isEqualTo("Task 1");
    assertThat(tasks.get(2).getName()).isEqualTo("Task 2");
  }

  @Test
  void reorderTasks_countMismatch_returns400() {
    createTask("Task 1", "apt", 1);
    createTask("Task 2", "copy", 2);

    ResponseEntity<Result<Void>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/tasks/order",
            HttpMethod.PUT,
            new HttpEntity<>(List.of(999L), authHeaders()),
            new ParameterizedTypeReference<>() {});
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void reorderTasks_forbiddenForNonOwnerMember() {
    Long t1 = createTask("Task 1", "apt", 1);
    Long t2 = createTask("Task 2", "copy", 2);

    RegisterRequest bobReg = new RegisterRequest();
    bobReg.setUsername("bob");
    bobReg.setPassword("password123");
    bobReg.setEmail("bob@example.com");
    ResponseEntity<Result<TokenResponse>> bobResp =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(bobReg),
            new ParameterizedTypeReference<>() {});
    String bobToken = bobResp.getBody().getData().getToken();
    Long bobId = bobResp.getBody().getData().getUser().getId();

    AddMemberRequest addBob = new AddMemberRequest();
    addBob.setUserId(bobId);
    addBob.setRole(ProjectRole.PROJECT_MEMBER);
    restTemplate.exchange(
        "/api/projects/" + projectId + "/members",
        HttpMethod.POST,
        new HttpEntity<>(addBob, authHeaders()),
        new ParameterizedTypeReference<Result<Void>>() {});

    HttpHeaders bobHeaders = new HttpHeaders();
    bobHeaders.setBearerAuth(bobToken);

    ResponseEntity<Result<Void>> bobReorder =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/tasks/order",
            HttpMethod.PUT,
            new HttpEntity<>(List.of(t2, t1), bobHeaders),
            new ParameterizedTypeReference<>() {});
    assertThat(bobReorder.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }
}
