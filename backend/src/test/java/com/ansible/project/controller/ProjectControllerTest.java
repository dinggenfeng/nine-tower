package com.ansible.project.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.project.dto.CreateProjectRequest;
import com.ansible.project.dto.ProjectResponse;
import com.ansible.project.dto.UpdateProjectRequest;
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

class ProjectControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;

  private String token;

  @BeforeEach
  void setUp() {
    RegisterRequest reg = new RegisterRequest();
    reg.setUsername("alice");
    reg.setPassword("password123");
    reg.setEmail("alice@example.com");
    ResponseEntity<Result<TokenResponse>> response =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(reg),
            new ParameterizedTypeReference<>() {});
    token = response.getBody().getData().getToken();
  }

  @AfterEach
  void tearDown() {
    projectMemberRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private Long createProject(String name) {
    CreateProjectRequest req = new CreateProjectRequest();
    req.setName(name);
    req.setDescription("desc");
    ResponseEntity<Result<ProjectResponse>> resp =
        restTemplate.exchange(
            "/api/projects",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().getId();
  }

  @Test
  void createProject_success() {
    CreateProjectRequest req = new CreateProjectRequest();
    req.setName("My Project");
    req.setDescription("A project");

    ResponseEntity<Result<ProjectResponse>> response =
        restTemplate.exchange(
            "/api/projects",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("My Project");
    assertThat(response.getBody().getData().getMyRole().name()).isEqualTo("PROJECT_ADMIN");
  }

  @Test
  void getMyProjects_returns_list() {
    createProject("Project A");
    createProject("Project B");

    ResponseEntity<Result<List<ProjectResponse>>> response =
        restTemplate.exchange(
            "/api/projects",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData()).hasSize(2);
  }

  @Test
  void getProject_success() {
    Long projectId = createProject("Project X");

    ResponseEntity<Result<ProjectResponse>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("Project X");
  }

  @Test
  void updateProject_success() {
    Long projectId = createProject("Old Name");

    UpdateProjectRequest req = new UpdateProjectRequest();
    req.setName("New Name");

    ResponseEntity<Result<ProjectResponse>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId,
            HttpMethod.PUT,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("New Name");
  }

  @Test
  void deleteProject_success() {
    Long projectId = createProject("To Delete");

    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(projectRepository.findById(projectId)).isEmpty();
  }

  @Test
  void getProject_forbidden_for_non_member() {
    Long projectId = createProject("Private Project");

    // Register second user
    RegisterRequest reg2 = new RegisterRequest();
    reg2.setUsername("bob");
    reg2.setPassword("password123");
    reg2.setEmail("bob@example.com");
    ResponseEntity<Result<TokenResponse>> reg2Resp =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(reg2),
            new ParameterizedTypeReference<>() {});
    String bobToken = reg2Resp.getBody().getData().getToken();

    HttpHeaders bobHeaders = new HttpHeaders();
    bobHeaders.setBearerAuth(bobToken);

    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId,
            HttpMethod.GET,
            new HttpEntity<>(bobHeaders),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }
}
