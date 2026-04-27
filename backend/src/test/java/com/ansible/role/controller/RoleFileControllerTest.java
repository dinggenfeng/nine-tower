package com.ansible.role.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.project.dto.CreateProjectRequest;
import com.ansible.project.dto.ProjectResponse;
import com.ansible.project.repository.ProjectMemberRepository;
import com.ansible.project.repository.ProjectRepository;
import com.ansible.role.dto.CreateFileRequest;
import com.ansible.role.dto.CreateRoleRequest;
import com.ansible.role.dto.FileResponse;
import com.ansible.role.dto.RoleResponse;
import com.ansible.role.dto.UpdateFileRequest;
import com.ansible.role.repository.RoleFileRepository;
import com.ansible.role.repository.RoleRepository;
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

class RoleFileControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private RoleFileRepository roleFileRepository;

  private String token;
  private Long roleId;

  @BeforeEach
  void setUp() {
    RegisterRequest reg = new RegisterRequest();
    reg.setUsername("fileuser");
    reg.setPassword("password123");
    reg.setEmail("fileuser@example.com");
    ResponseEntity<Result<TokenResponse>> regResp =
        restTemplate.exchange(
            "/api/auth/register",
            HttpMethod.POST,
            new HttpEntity<>(reg),
            new ParameterizedTypeReference<>() {});
    token = regResp.getBody().getData().getToken();

    CreateProjectRequest projReq = new CreateProjectRequest();
    projReq.setName("File Test Project");
    ResponseEntity<Result<ProjectResponse>> projResp =
        restTemplate.exchange(
            "/api/projects",
            HttpMethod.POST,
            new HttpEntity<>(projReq, authHeaders()),
            new ParameterizedTypeReference<>() {});
    Long projectId = projResp.getBody().getData().getId();

    CreateRoleRequest roleReq = new CreateRoleRequest();
    roleReq.setName("test-role");
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
    roleFileRepository.deleteAll();
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

  private Long createFile(String name, String parentDir, Boolean isDirectory) {
    CreateFileRequest req = new CreateFileRequest();
    req.setName(name);
    req.setParentDir(parentDir);
    req.setIsDirectory(isDirectory);
    ResponseEntity<Result<FileResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/files",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().getId();
  }

  @Test
  void createFile_success() {
    CreateFileRequest req = new CreateFileRequest();
    req.setName("config.yml");
    req.setParentDir("");
    req.setIsDirectory(false);

    ResponseEntity<Result<FileResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/files",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getName()).isEqualTo("config.yml");
    assertThat(resp.getBody().getData().getIsDirectory()).isFalse();
  }

  @Test
  void createDirectory_success() {
    CreateFileRequest req = new CreateFileRequest();
    req.setName("conf.d");
    req.setParentDir("");
    req.setIsDirectory(true);

    ResponseEntity<Result<FileResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/files",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getName()).isEqualTo("conf.d");
    assertThat(resp.getBody().getData().getIsDirectory()).isTrue();
  }

  @Test
  void listFiles_treeStructure() {
    createFile("conf.d", "", true);
    createFile("app.conf", "conf.d", false);
    createFile("readme.txt", "", false);

    ResponseEntity<Result<List<FileResponse>>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/files",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<FileResponse> tree = resp.getBody().getData();
    assertThat(tree).hasSize(2);
    assertThat(tree.stream().map(FileResponse::getName).toList()).contains("conf.d", "readme.txt");
  }

  @Test
  void getFileContent_success() {
    Long fileId = createFile("test.txt", "", false);

    UpdateFileRequest updateReq = new UpdateFileRequest();
    updateReq.setName("test.txt");
    updateReq.setTextContent("hello world");
    restTemplate.exchange(
        "/api/files/" + fileId,
        HttpMethod.PUT,
        new HttpEntity<>(updateReq, authHeaders()),
        new ParameterizedTypeReference<Result<FileResponse>>() {});

    ResponseEntity<byte[]> downloadResp =
        restTemplate.exchange(
            "/api/files/" + fileId + "/download",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            byte[].class);

    assertThat(downloadResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(new String(downloadResp.getBody())).contains("hello world");
  }

  @Test
  void uploadFile_success() {
    HttpHeaders headers = authHeaders();
    headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);
    org.springframework.util.LinkedMultiValueMap<String, Object> body =
        new org.springframework.util.LinkedMultiValueMap<>();
    org.springframework.core.io.ByteArrayResource resource =
        new org.springframework.core.io.ByteArrayResource(
            new byte[] {0x50, 0x4B, 0x03, 0x04}) {
          @Override
          public String getFilename() {
            return "app.jar";
          }
        };
    body.add("file", resource);
    HttpEntity<org.springframework.util.MultiValueMap<String, Object>> entity =
        new HttpEntity<>(body, headers);

    ResponseEntity<Result<FileResponse>> resp =
        restTemplate.exchange(
            "/api/roles/" + roleId + "/files/upload",
            HttpMethod.POST,
            entity,
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody().getData().getName()).isEqualTo("app.jar");
    assertThat(resp.getBody().getData().getSize()).isEqualTo(4);
  }

  @Test
  void deleteFile_success() {
    Long fileId = createFile("config.yml", "", false);

    ResponseEntity<Result<Void>> resp =
        restTemplate.exchange(
            "/api/files/" + fileId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(roleFileRepository.findById(fileId)).isEmpty();
  }
}
