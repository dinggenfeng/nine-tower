package com.ansible.host.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.host.dto.CreateHostGroupRequest;
import com.ansible.host.dto.HostGroupResponse;
import com.ansible.host.dto.UpdateHostGroupRequest;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.project.repository.ProjectMemberRepository;
import com.ansible.project.repository.ProjectRepository;
import com.ansible.user.dto.RegisterRequest;
import com.ansible.user.dto.TokenResponse;
import com.ansible.user.repository.UserRepository;
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

class HostGroupControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;
  @Autowired private HostGroupRepository hostGroupRepository;

  private String token;
  private Long projectId;

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

    com.ansible.project.dto.CreateProjectRequest projReq =
        new com.ansible.project.dto.CreateProjectRequest();
    projReq.setName("Test Project");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    ResponseEntity<Result<com.ansible.project.dto.ProjectResponse>> projResp =
        restTemplate.exchange(
            "/api/projects",
            HttpMethod.POST,
            new HttpEntity<>(projReq, headers),
            new ParameterizedTypeReference<>() {});
    projectId = projResp.getBody().getData().getId();
  }

  @AfterEach
  void tearDown() {
    hostGroupRepository.deleteAll();
    projectMemberRepository.deleteAll();
    projectRepository.deleteAll();
    userRepository.deleteAll();
  }

  private HttpHeaders authHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return headers;
  }

  private Long createHostGroup(String name) {
    CreateHostGroupRequest req = new CreateHostGroupRequest();
    req.setName(name);
    req.setDescription("desc");
    ResponseEntity<Result<HostGroupResponse>> resp =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/host-groups",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().getId();
  }

  @Test
  void createHostGroup_success() {
    CreateHostGroupRequest req = new CreateHostGroupRequest();
    req.setName("Web Servers");
    req.setDescription("All web servers");

    ResponseEntity<Result<HostGroupResponse>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/host-groups",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("Web Servers");
  }

  @Test
  void getHostGroups_returns_list() {
    createHostGroup("Group A");
    createHostGroup("Group B");

    ResponseEntity<Result<java.util.List<HostGroupResponse>>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/host-groups",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData()).hasSize(2);
  }

  @Test
  void updateHostGroup_success() {
    Long hgId = createHostGroup("Old Name");

    UpdateHostGroupRequest req = new UpdateHostGroupRequest();
    req.setName("New Name");

    ResponseEntity<Result<HostGroupResponse>> response =
        restTemplate.exchange(
            "/api/host-groups/" + hgId,
            HttpMethod.PUT,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("New Name");
  }

  @Test
  void deleteHostGroup_success() {
    Long hgId = createHostGroup("To Delete");

    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/host-groups/" + hgId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(hostGroupRepository.findById(hgId)).isEmpty();
  }
}
