package com.ansible.role.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.project.dto.CreateProjectRequest;
import com.ansible.project.dto.ProjectResponse;
import com.ansible.project.repository.ProjectMemberRepository;
import com.ansible.project.repository.ProjectRepository;
import com.ansible.role.dto.CreateRoleRequest;
import com.ansible.role.dto.CreateTemplateRequest;
import com.ansible.role.dto.RoleResponse;
import com.ansible.role.dto.TemplateResponse;
import com.ansible.role.dto.UpdateTemplateRequest;
import com.ansible.role.repository.RoleRepository;
import com.ansible.role.repository.TemplateRepository;
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

class TemplateControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private TemplateRepository templateRepository;

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
    templateRepository.deleteAll();
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

  private Long createTemplate(String name, String parentDir, String targetPath, String content) {
    CreateTemplateRequest req = new CreateTemplateRequest();
    req.setName(name);
    req.setParentDir(parentDir);
    req.setTargetPath(targetPath);
    req.setContent(content);
    ResponseEntity<Result<TemplateResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/templates",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().getId();
  }

  @Test
  void createTemplate_success() {
    CreateTemplateRequest req = new CreateTemplateRequest();
    req.setName("nginx.conf.j2");
    req.setTargetPath("/etc/nginx/nginx.conf");
    req.setContent("server { listen {{ http_port }}; }");

    ResponseEntity<Result<TemplateResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/templates",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getName()).isEqualTo("nginx.conf.j2");
    assertThat(resp.getBody().getData().getTargetPath()).isEqualTo("/etc/nginx/nginx.conf");
    assertThat(resp.getBody().getData().getContent())
        .isEqualTo("server { listen {{ http_port }}; }");
  }

  @Test
  void createTemplate_withParentDir() {
    CreateTemplateRequest req = new CreateTemplateRequest();
    req.setName("vhost.conf.j2");
    req.setParentDir("nginx/conf.d");
    req.setTargetPath("/etc/nginx/conf.d/vhost.conf");
    req.setContent("server { server_name {{ domain }}; }");

    ResponseEntity<Result<TemplateResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/templates",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getParentDir()).isEqualTo("nginx/conf.d");
    assertThat(resp.getBody().getData().getName()).isEqualTo("vhost.conf.j2");
  }

  @Test
  void createTemplate_duplicateName() {
    createTemplate("nginx.conf.j2", null, "/etc/nginx/nginx.conf", "content");

    CreateTemplateRequest req = new CreateTemplateRequest();
    req.setName("nginx.conf.j2");
    req.setContent("other content");

    ResponseEntity<Result<TemplateResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/templates",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void getTemplates_success() {
    createTemplate("nginx.conf.j2", null, "/etc/nginx/nginx.conf", "content1");
    createTemplate("vhost.conf.j2", "conf.d", "/etc/nginx/conf.d/vhost.conf", "content2");

    ResponseEntity<Result<List<TemplateResponse>>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/templates",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData()).hasSize(2);
  }

  @Test
  void getTemplate_success() {
    Long templateId =
        createTemplate("nginx.conf.j2", null, "/etc/nginx/nginx.conf", "template content");

    ResponseEntity<Result<TemplateResponse>> resp =
        restTemplate.exchange(
            "/api/templates/" + templateId,
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getName()).isEqualTo("nginx.conf.j2");
    assertThat(resp.getBody().getData().getContent()).isEqualTo("template content");
  }

  @Test
  void updateTemplate_success() {
    Long templateId =
        createTemplate("nginx.conf.j2", null, "/etc/nginx/nginx.conf", "old content");

    UpdateTemplateRequest req = new UpdateTemplateRequest();
    req.setContent("new content");

    ResponseEntity<Result<TemplateResponse>> resp =
        restTemplate.exchange(
            "/api/templates/" + templateId,
            HttpMethod.PUT,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getContent()).isEqualTo("new content");
  }

  @Test
  void deleteTemplate_success() {
    Long templateId =
        createTemplate("nginx.conf.j2", null, "/etc/nginx/nginx.conf", "content");

    ResponseEntity<Result<Void>> resp =
        restTemplate.exchange(
            "/api/templates/" + templateId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<Result<List<TemplateResponse>>> listResp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/templates",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});
    assertThat(listResp.getBody().getData()).isEmpty();
  }

  @Test
  void downloadTemplate_success() {
    Long templateId =
        createTemplate("nginx.conf.j2", null, "/etc/nginx/nginx.conf", "server { listen 80; }");

    ResponseEntity<byte[]> resp =
        restTemplate.exchange(
            "/api/templates/" + templateId + "/download",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            byte[].class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody()).isNotNull();
    assertThat(new String(resp.getBody())).isEqualTo("server { listen 80; }");
    assertThat(resp.getHeaders().getContentDisposition().getFilename()).isEqualTo("nginx.conf.j2");
  }

  @Test
  void createDirectory_success() {
    CreateTemplateRequest req = new CreateTemplateRequest();
    req.setName("conf.d");
    req.setIsDirectory(true);

    ResponseEntity<Result<TemplateResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/templates",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getIsDirectory()).isTrue();
  }

  @Test
  void listTemplates_treeWithDirectory() {
    CreateTemplateRequest dirReq = new CreateTemplateRequest();
    dirReq.setName("conf.d");
    dirReq.setIsDirectory(true);
    restTemplate.exchange(
        "/api/roles/" + roleId + "/templates",
        HttpMethod.POST,
        new HttpEntity<>(dirReq, authHeaders()),
        new ParameterizedTypeReference<Result<TemplateResponse>>() {});

    createTemplate("app.conf.j2", "conf.d", "/etc/app.conf", "content");

    ResponseEntity<Result<List<TemplateResponse>>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/templates",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<TemplateResponse> tree = resp.getBody().getData();
    assertThat(tree).isNotEmpty();
  }

  @Test
  void downloadTemplate_notFound() {
    ResponseEntity<byte[]> resp =
        restTemplate.exchange(
            "/api/templates/99999/download",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            byte[].class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }
}
