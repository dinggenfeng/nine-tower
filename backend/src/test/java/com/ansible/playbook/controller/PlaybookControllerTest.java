package com.ansible.playbook.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.common.enums.ProjectRole;
import com.ansible.playbook.dto.CreatePlaybookRequest;
import com.ansible.playbook.dto.PlaybookResponse;
import com.ansible.playbook.dto.PlaybookRoleRequest;
import com.ansible.playbook.repository.PlaybookHostGroupRepository;
import com.ansible.playbook.repository.PlaybookRepository;
import com.ansible.playbook.repository.PlaybookRoleRepository;
import com.ansible.playbook.repository.PlaybookTagRepository;
import com.ansible.project.dto.AddMemberRequest;
import com.ansible.project.dto.CreateProjectRequest;
import com.ansible.project.dto.ProjectResponse;
import com.ansible.project.repository.ProjectMemberRepository;
import com.ansible.project.repository.ProjectRepository;
import com.ansible.role.dto.CreateRoleRequest;
import com.ansible.role.dto.RoleResponse;
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

class PlaybookControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;
  @Autowired private PlaybookRepository playbookRepository;
  @Autowired private PlaybookRoleRepository playbookRoleRepository;
  @Autowired private PlaybookHostGroupRepository playbookHostGroupRepository;
  @Autowired private PlaybookTagRepository playbookTagRepository;

  private String token;
  private Long projectId;
  private Long roleId;

  @BeforeEach
  void setUp() {
    RegisterRequest reg = new RegisterRequest();
    reg.setUsername("pbuser");
    reg.setPassword("password123");
    reg.setEmail("pbuser@example.com");
    ResponseEntity<Result<TokenResponse>> regResp =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(reg),
            new ParameterizedTypeReference<>() {});
    token = regResp.getBody().getData().getToken();

    CreateProjectRequest projReq = new CreateProjectRequest();
    projReq.setName("PB Test Project");
    ResponseEntity<Result<ProjectResponse>> projResp =
        restTemplate.exchange(
            "/api/projects",
            HttpMethod.POST,
            new HttpEntity<>(projReq, authHeaders()),
            new ParameterizedTypeReference<>() {});
    projectId = projResp.getBody().getData().getId();

    CreateRoleRequest roleReq = new CreateRoleRequest();
    roleReq.setName("nginx");
    roleReq.setDescription("nginx role");
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
    playbookTagRepository.deleteAll();
    playbookHostGroupRepository.deleteAll();
    playbookRoleRepository.deleteAll();
    playbookRepository.deleteAll();
    projectMemberRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private Long createPlaybook(String name) {
    CreatePlaybookRequest req = new CreatePlaybookRequest(name, "desc", null);
    ResponseEntity<Result<PlaybookResponse>> resp =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/playbooks",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().id();
  }

  @Test
  void createPlaybook_success() {
    CreatePlaybookRequest req = new CreatePlaybookRequest("deploy.yml", "Deploy app", null);

    ResponseEntity<Result<PlaybookResponse>> resp =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/playbooks",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().name()).isEqualTo("deploy.yml");
    assertThat(resp.getBody().getData().description()).isEqualTo("Deploy app");
  }

  @Test
  void listPlaybooks_success() {
    createPlaybook("deploy.yml");

    ResponseEntity<Result<List<PlaybookResponse>>> resp =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/playbooks",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData()).hasSize(1);
    assertThat(resp.getBody().getData().get(0).name()).isEqualTo("deploy.yml");
  }

  @Test
  void getPlaybook_success() {
    Long pbId = createPlaybook("deploy.yml");

    ResponseEntity<Result<PlaybookResponse>> resp =
        restTemplate.exchange(
            "/api/playbooks/" + pbId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().name()).isEqualTo("deploy.yml");
  }

  @Test
  void updatePlaybook_success() {
    Long pbId = createPlaybook("deploy.yml");

    CreatePlaybookRequest req = new CreatePlaybookRequest("staging.yml", "Staging", null);
    ResponseEntity<Result<PlaybookResponse>> resp =
        restTemplate.exchange(
            "/api/playbooks/" + pbId,
            HttpMethod.PUT,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().name()).isEqualTo("staging.yml");
  }

  @Test
  void deletePlaybook_success() {
    Long pbId = createPlaybook("deploy.yml");

    ResponseEntity<Result<Void>> resp =
        restTemplate.exchange(
            "/api/playbooks/" + pbId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(playbookRepository.findById(pbId)).isEmpty();
  }

  @Test
  void addRole_success() {
    Long pbId = createPlaybook("deploy.yml");

    ResponseEntity<Result<Void>> roleResp =
        restTemplate.exchange(
            "/api/playbooks/" + pbId + "/roles",
            HttpMethod.POST,
            new HttpEntity<>(new PlaybookRoleRequest(roleId), authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(roleResp.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void generateYaml_success() {
    Long pbId = createPlaybook("deploy.yml");

    restTemplate.exchange(
        "/api/playbooks/" + pbId + "/roles",
        HttpMethod.POST,
        new HttpEntity<>(new PlaybookRoleRequest(roleId), authHeaders()),
        new ParameterizedTypeReference<Result<Void>>() {});

    ResponseEntity<Result<String>> yamlResp =
        restTemplate.exchange(
            "/api/playbooks/" + pbId + "/yaml",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(yamlResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(yamlResp.getBody().getData()).contains("nginx");
  }

  @Test
  void nonOwnerMember_cannotModifyPlaybookComposition() {
    // Alice (owner) creates the playbook.
    Long pbId = createPlaybook("deploy.yml");

    // Register bob and add him as a plain PROJECT_MEMBER.
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

    // Bob can not add a role to Alice's playbook.
    ResponseEntity<Result<Void>> bobAddRole =
        restTemplate.exchange(
            "/api/playbooks/" + pbId + "/roles",
            HttpMethod.POST,
            new HttpEntity<>(new PlaybookRoleRequest(roleId), bobHeaders),
            new ParameterizedTypeReference<>() {});
    assertThat(bobAddRole.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // Bob can not reorder roles.
    ResponseEntity<Result<Void>> bobReorder =
        restTemplate.exchange(
            "/api/playbooks/" + pbId + "/roles/order",
            HttpMethod.PUT,
            new HttpEntity<>(List.of(roleId), bobHeaders),
            new ParameterizedTypeReference<>() {});
    assertThat(bobReorder.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

    // Alice attaches the role, then Bob can not remove it.
    restTemplate.exchange(
        "/api/playbooks/" + pbId + "/roles",
        HttpMethod.POST,
        new HttpEntity<>(new PlaybookRoleRequest(roleId), authHeaders()),
        new ParameterizedTypeReference<Result<Void>>() {});
    ResponseEntity<Result<Void>> bobRemove =
        restTemplate.exchange(
            "/api/playbooks/" + pbId + "/roles/" + roleId,
            HttpMethod.DELETE,
            new HttpEntity<>(bobHeaders),
            new ParameterizedTypeReference<>() {});
    assertThat(bobRemove.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }
}
