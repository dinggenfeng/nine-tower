package com.ansible.host.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.host.dto.CreateHostGroupRequest;
import com.ansible.host.dto.CreateHostRequest;
import com.ansible.host.dto.HostGroupResponse;
import com.ansible.host.dto.HostResponse;
import com.ansible.host.dto.UpdateHostRequest;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.host.repository.HostRepository;
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

class HostControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;
  @Autowired private HostGroupRepository hostGroupRepository;
  @Autowired private HostRepository hostRepository;

  private String token;
  private Long projectId;
  private Long hostGroupId;

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

    CreateHostGroupRequest hgReq = new CreateHostGroupRequest();
    hgReq.setName("Web Servers");
    ResponseEntity<Result<HostGroupResponse>> hgResp =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/host-groups",
            HttpMethod.POST,
            new HttpEntity<>(hgReq, headers),
            new ParameterizedTypeReference<>() {});
    hostGroupId = hgResp.getBody().getData().getId();
  }

  @AfterEach
  void tearDown() {
    hostRepository.deleteAll();
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

  private Long createHost(String name, String ip) {
    CreateHostRequest req = new CreateHostRequest();
    req.setName(name);
    req.setIp(ip);
    req.setPort(22);
    req.setAnsibleUser("ansible");
    req.setAnsibleSshPass("secret123");
    req.setAnsibleBecome(false);
    ResponseEntity<Result<HostResponse>> resp =
        restTemplate.exchange(
            "/api/host-groups/" + hostGroupId + "/hosts",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().getId();
  }

  @Test
  void createHost_success() {
    CreateHostRequest req = new CreateHostRequest();
    req.setName("web-01");
    req.setIp("192.168.1.10");
    req.setPort(22);
    req.setAnsibleUser("ansible");
    req.setAnsibleSshPass("secret123");
    req.setAnsibleBecome(false);

    ResponseEntity<Result<HostResponse>> response =
        restTemplate.exchange(
            "/api/host-groups/" + hostGroupId + "/hosts",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("web-01");
  }

  @Test
  void getHosts_returns_list() {
    createHost("web-01", "192.168.1.10");
    createHost("web-02", "192.168.1.11");

    ResponseEntity<Result<java.util.List<HostResponse>>> response =
        restTemplate.exchange(
            "/api/host-groups/" + hostGroupId + "/hosts",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData()).hasSize(2);
  }

  @Test
  void getHost_returns_masked_sensitive_fields() {
    Long hostId = createHost("web-01", "192.168.1.10");

    ResponseEntity<Result<HostResponse>> response =
        restTemplate.exchange(
            "/api/hosts/" + hostId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getAnsibleSshPass()).isEqualTo("****");
    assertThat(response.getBody().getData().getAnsibleSshPrivateKeyFile()).isEqualTo("****");
  }

  @Test
  void updateHost_success() {
    Long hostId = createHost("web-01", "192.168.1.10");

    UpdateHostRequest req = new UpdateHostRequest();
    req.setName("web-02");

    ResponseEntity<Result<HostResponse>> response =
        restTemplate.exchange(
            "/api/hosts/" + hostId,
            HttpMethod.PUT,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("web-02");
  }

  @Test
  void deleteHost_success() {
    Long hostId = createHost("web-01", "192.168.1.10");

    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/hosts/" + hostId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(hostRepository.findById(hostId)).isEmpty();
  }

  @Test
  void createHost_withCopyFromHostId_retainsSensitiveFields() {
    Long sourceHostId = createHost("web-01", "192.168.1.10");

    CreateHostRequest req = new CreateHostRequest();
    req.setName("web-01-copy");
    req.setIp("192.168.1.10");
    req.setPort(22);
    req.setAnsibleUser("ansible");
    req.setCopyFromHostId(sourceHostId);

    ResponseEntity<Result<HostResponse>> response =
        restTemplate.exchange(
            "/api/host-groups/" + hostGroupId + "/hosts",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("web-01-copy");
    assertThat(response.getBody().getData().getAnsibleSshPass()).isEqualTo("****");
  }
}
