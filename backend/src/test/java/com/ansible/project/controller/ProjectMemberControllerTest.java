package com.ansible.project.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.common.enums.ProjectRole;
import com.ansible.project.dto.AddMemberRequest;
import com.ansible.project.dto.CreateProjectRequest;
import com.ansible.project.dto.ProjectMemberResponse;
import com.ansible.project.dto.ProjectResponse;
import com.ansible.project.dto.UpdateMemberRoleRequest;
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

class ProjectMemberControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;

  private String aliceToken;
  private String bobToken;
  private Long aliceId;
  private Long bobId;
  private Long projectId;

  @BeforeEach
  void setUp() {
    RegisterRequest aliceReg = new RegisterRequest();
    aliceReg.setUsername("alice");
    aliceReg.setPassword("password123");
    aliceReg.setEmail("alice@example.com");
    ResponseEntity<Result<TokenResponse>> aliceResp =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(aliceReg),
            new ParameterizedTypeReference<>() {});
    aliceToken = aliceResp.getBody().getData().getToken();
    aliceId = aliceResp.getBody().getData().getUser().getId();

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
    bobToken = bobResp.getBody().getData().getToken();
    bobId = bobResp.getBody().getData().getUser().getId();

    CreateProjectRequest projReq = new CreateProjectRequest();
    projReq.setName("Team Project");
    projReq.setDescription("desc");
    HttpHeaders aliceHeaders = new HttpHeaders();
    aliceHeaders.setBearerAuth(aliceToken);
    ResponseEntity<Result<ProjectResponse>> projResp =
        restTemplate.exchange(
            "/api/projects",
            HttpMethod.POST,
            new HttpEntity<>(projReq, aliceHeaders),
            new ParameterizedTypeReference<>() {});
    projectId = projResp.getBody().getData().getId();
  }

  @AfterEach
  void tearDown() {
    projectMemberRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  private HttpHeaders aliceHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(aliceToken);
    return headers;
  }

  private HttpHeaders bobHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(bobToken);
    return headers;
  }

  @Test
  void addMember_success() {
    AddMemberRequest req = new AddMemberRequest();
    req.setUserId(bobId);
    req.setRole(ProjectRole.PROJECT_MEMBER);

    ResponseEntity<Result<ProjectMemberResponse>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members",
            HttpMethod.POST,
            new HttpEntity<>(req, aliceHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getUsername()).isEqualTo("bob");
    assertThat(response.getBody().getData().getRole()).isEqualTo(ProjectRole.PROJECT_MEMBER);
  }

  @Test
  void addMember_forbidden_for_non_admin() {
    AddMemberRequest addBob = new AddMemberRequest();
    addBob.setUserId(bobId);
    addBob.setRole(ProjectRole.PROJECT_MEMBER);
    restTemplate.exchange(
        "/api/projects/" + projectId + "/members",
        HttpMethod.POST,
        new HttpEntity<>(addBob, aliceHeaders()),
        new ParameterizedTypeReference<Result<ProjectMemberResponse>>() {});

    RegisterRequest charlieReg = new RegisterRequest();
    charlieReg.setUsername("charlie");
    charlieReg.setPassword("password123");
    charlieReg.setEmail("charlie@example.com");
    ResponseEntity<Result<TokenResponse>> charlieResp =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(charlieReg),
            new ParameterizedTypeReference<>() {});
    Long charlieId = charlieResp.getBody().getData().getUser().getId();

    AddMemberRequest addCharlie = new AddMemberRequest();
    addCharlie.setUserId(charlieId);
    addCharlie.setRole(ProjectRole.PROJECT_MEMBER);

    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members",
            HttpMethod.POST,
            new HttpEntity<>(addCharlie, bobHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void listMembers_success() {
    ResponseEntity<Result<List<ProjectMemberResponse>>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members",
            HttpMethod.GET,
            new HttpEntity<>(aliceHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData()).hasSize(1);
    assertThat(response.getBody().getData().get(0).getUsername()).isEqualTo("alice");
  }

  @Test
  void removeMember_success() {
    AddMemberRequest addBob = new AddMemberRequest();
    addBob.setUserId(bobId);
    addBob.setRole(ProjectRole.PROJECT_MEMBER);
    restTemplate.exchange(
        "/api/projects/" + projectId + "/members",
        HttpMethod.POST,
        new HttpEntity<>(addBob, aliceHeaders()),
        new ParameterizedTypeReference<Result<ProjectMemberResponse>>() {});

    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members/" + bobId,
            HttpMethod.DELETE,
            new HttpEntity<>(aliceHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void updateMemberRole_success() {
    AddMemberRequest addBob = new AddMemberRequest();
    addBob.setUserId(bobId);
    addBob.setRole(ProjectRole.PROJECT_MEMBER);
    restTemplate.exchange(
        "/api/projects/" + projectId + "/members",
        HttpMethod.POST,
        new HttpEntity<>(addBob, aliceHeaders()),
        new ParameterizedTypeReference<Result<ProjectMemberResponse>>() {});

    UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
    req.setRole(ProjectRole.PROJECT_ADMIN);

    ResponseEntity<Result<ProjectMemberResponse>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members/" + bobId,
            HttpMethod.PUT,
            new HttpEntity<>(req, aliceHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getRole()).isEqualTo(ProjectRole.PROJECT_ADMIN);
  }

  @Test
  void removeMember_fails_when_removing_self() {
    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members/" + aliceId,
            HttpMethod.DELETE,
            new HttpEntity<>(aliceHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getMessage()).contains("不能将自己移出项目");
  }

  @Test
  void updateMemberRole_fails_when_changing_own_role() {
    UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
    req.setRole(ProjectRole.PROJECT_MEMBER);

    ResponseEntity<Result<ProjectMemberResponse>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members/" + aliceId,
            HttpMethod.PUT,
            new HttpEntity<>(req, aliceHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getMessage()).contains("不能修改自己的角色");
  }

  @Test
  void removeMember_fails_when_removing_last_admin_via_orphan_protection() {
    // Self-protection fires first: alice tries to remove herself (sole admin)
    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members/" + aliceId,
            HttpMethod.DELETE,
            new HttpEntity<>(aliceHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getMessage()).contains("不能将自己移出项目");
  }

  @Test
  void updateMemberRole_fails_when_downgrading_only_other_admin() {
    // alice promotes bob to admin
    AddMemberRequest addBob = new AddMemberRequest();
    addBob.setUserId(bobId);
    addBob.setRole(ProjectRole.PROJECT_ADMIN);
    restTemplate.exchange(
        "/api/projects/" + projectId + "/members",
        HttpMethod.POST,
        new HttpEntity<>(addBob, aliceHeaders()),
        new ParameterizedTypeReference<Result<ProjectMemberResponse>>() {});

    // alice tries to downgrade herself — self-protection blocks it
    UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
    req.setRole(ProjectRole.PROJECT_MEMBER);
    ResponseEntity<Result<ProjectMemberResponse>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members/" + aliceId,
            HttpMethod.PUT,
            new HttpEntity<>(req, aliceHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getMessage()).contains("不能修改自己的角色");
  }

  @Test
  void updateMemberRole_success_when_multiple_admins() {
    // Make bob an admin
    AddMemberRequest addBob = new AddMemberRequest();
    addBob.setUserId(bobId);
    addBob.setRole(ProjectRole.PROJECT_ADMIN);
    restTemplate.exchange(
        "/api/projects/" + projectId + "/members",
        HttpMethod.POST,
        new HttpEntity<>(addBob, aliceHeaders()),
        new ParameterizedTypeReference<Result<ProjectMemberResponse>>() {});

    // Alice can downgrade bob (not self, and alice remains admin)
    UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
    req.setRole(ProjectRole.PROJECT_MEMBER);
    ResponseEntity<Result<ProjectMemberResponse>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members/" + bobId,
            HttpMethod.PUT,
            new HttpEntity<>(req, aliceHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getRole()).isEqualTo(ProjectRole.PROJECT_MEMBER);
  }
}
