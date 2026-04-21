# File Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement RoleFile CRUD for managing files in the Role files/ directory, supporting directory trees, file upload/download, and text editing.

**Architecture:** RoleFile entity belongs to Role (via roleId FK). Stores file content as bytea in DB (not filesystem). parentDir field supports directory hierarchy. isDirectory flag distinguishes files from folders. Service handles tree assembly from flat rows. Controller provides upload (multipart), download (streaming), and text content editing. Follows existing Template pattern.

**Tech Stack:** Spring Boot 3.x, Spring Data JPA, Jakarta Validation, Mockito + Testcontainers (PostgreSQL 16), React 18 + Ant Design 5 + TypeScript

---

## File Change Summary

| File | Action |
|------|--------|
| `backend/.../role/entity/RoleFile.java` | Create |
| `backend/.../role/repository/RoleFileRepository.java` | Create |
| `backend/.../role/dto/CreateFileRequest.java` | Create |
| `backend/.../role/dto/UpdateFileRequest.java` | Create |
| `backend/.../role/dto/FileResponse.java` | Create |
| `backend/.../role/service/RoleFileService.java` | Create |
| `backend/.../role/controller/RoleFileController.java` | Create |
| `backend/.../role/service/RoleFileServiceTest.java` | Create |
| `backend/.../role/controller/RoleFileControllerTest.java` | Create |
| `frontend/src/types/entity/RoleFile.ts` | Create |
| `frontend/src/api/roleFile.ts` | Create |
| `frontend/src/pages/role/RoleFiles.tsx` | Create |

---

## Backend Tasks

### Task 1: RoleFile Entity and Repository

**Files:**
- Create: `backend/src/main/java/com/ansible/role/entity/RoleFile.java`
- Create: `backend/src/main/java/com/ansible/role/repository/RoleFileRepository.java`

- [ ] **Step 1: Create RoleFile entity**

```java
package com.ansible.role.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "role_files", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"roleId", "parentDir", "name"})
})
@Getter
@Setter
@NoArgsConstructor
public class RoleFile extends BaseEntity {

    @Column(nullable = false)
    private Long roleId;

    @Column(nullable = false, length = 500)
    private String parentDir;

    @Column(nullable = false, length = 200)
    private String name;

    @Lob
    @Column(columnDefinition = "bytea")
    private byte[] content;

    @Column(nullable = false)
    private Boolean isDirectory = false;
}
```

- [ ] **Step 2: Create RoleFileRepository**

```java
package com.ansible.role.repository;

import com.ansible.role.entity.RoleFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleFileRepository extends JpaRepository<RoleFile, Long> {

    List<RoleFile> findByRoleIdOrderByParentDirAscNameAsc(Long roleId);

    List<RoleFile> findByRoleIdAndParentDirOrderByIsDirectoryDescNameAsc(Long roleId, String parentDir);

    Optional<RoleFile> findByRoleIdAndParentDirAndName(Long roleId, String parentDir, String name);

    boolean existsByRoleIdAndParentDirAndName(Long roleId, String parentDir, String name);

    boolean existsByRoleIdAndParentDirAndNameAndIdNot(Long roleId, String parentDir, String name, Long id);

    void deleteByRoleId(Long roleId);

    long countByRoleIdAndParentDirStartingWith(Long roleId, String parentDirPrefix);
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/ansible/role/entity/RoleFile.java backend/src/main/java/com/ansible/role/repository/RoleFileRepository.java
git commit -m "feat(file): add RoleFile entity and repository"
```

---

### Task 2: File DTOs

**Files:**
- Create: `backend/src/main/java/com/ansible/role/dto/CreateFileRequest.java`
- Create: `backend/src/main/java/com/ansible/role/dto/UpdateFileRequest.java`
- Create: `backend/src/main/java/com/ansible/role/dto/FileResponse.java`

- [ ] **Step 1: Create file DTOs**

```java
// CreateFileRequest.java
package com.ansible.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFileRequest(
    @NotBlank @Size(max = 500) String parentDir,
    @NotBlank @Size(max = 200) String name,
    Boolean isDirectory
) {}

// UpdateFileRequest.java
package com.ansible.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateFileRequest(
    @NotBlank @Size(max = 200) String name,
    String textContent
) {}

// FileResponse.java
package com.ansible.role.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FileResponse(
    Long id,
    Long roleId,
    String parentDir,
    String name,
    Boolean isDirectory,
    Long size,
    String textContent,
    List<FileResponse> children,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/ansible/role/dto/CreateFileRequest.java backend/src/main/java/com/ansible/role/dto/UpdateFileRequest.java backend/src/main/java/com/ansible/role/dto/FileResponse.java
git commit -m "feat(file): add RoleFile DTOs"
```

---

### Task 3: RoleFileService with Unit Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/role/service/RoleFileService.java`
- Create: `backend/src/test/java/com/ansible/role/service/RoleFileServiceTest.java`

- [ ] **Step 1: Write RoleFileServiceTest**

```java
package com.ansible.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ansible.role.dto.CreateFileRequest;
import com.ansible.role.dto.FileResponse;
import com.ansible.role.dto.UpdateFileRequest;
import com.ansible.role.entity.RoleFile;
import com.ansible.role.repository.RoleFileRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleFileServiceTest {

    @Mock
    private RoleFileRepository roleFileRepository;

    @InjectMocks
    private RoleFileService roleFileService;

    private RoleFile createFile(Long id, Long roleId, String parentDir, String name, boolean isDir) {
        RoleFile f = new RoleFile();
        f.setId(id);
        f.setRoleId(roleId);
        f.setParentDir(parentDir);
        f.setName(name);
        f.setIsDirectory(isDir);
        if (!isDir) {
            f.setContent("content".getBytes(StandardCharsets.UTF_8));
        }
        return f;
    }

    @Test
    void createFile_success() {
        when(roleFileRepository.existsByRoleIdAndParentDirAndName(1L, "", "config.yml")).thenReturn(false);
        when(roleFileRepository.save(any(RoleFile.class))).thenAnswer(inv -> {
            RoleFile f = inv.getArgument(0);
            f.setId(10L);
            return f;
        });

        FileResponse response = roleFileService.createFile(1L,
            new CreateFileRequest("", "config.yml", false),
            "content".getBytes(StandardCharsets.UTF_8), 100L);

        assertThat(response.name()).isEqualTo("config.yml");
        assertThat(response.isDirectory()).isFalse();
    }

    @Test
    void createFile_duplicateName_throws() {
        when(roleFileRepository.existsByRoleIdAndParentDirAndName(1L, "", "config.yml")).thenReturn(true);

        assertThatThrownBy(() -> roleFileService.createFile(1L,
            new CreateFileRequest("", "config.yml", false),
            "content".getBytes(StandardCharsets.UTF_8), 100L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void createDirectory_success() {
        when(roleFileRepository.existsByRoleIdAndParentDirAndName(1L, "", "conf.d")).thenReturn(false);
        when(roleFileRepository.save(any(RoleFile.class))).thenAnswer(inv -> {
            RoleFile f = inv.getArgument(0);
            f.setId(10L);
            return f;
        });

        FileResponse response = roleFileService.createFile(1L,
            new CreateFileRequest("", "conf.d", true), null, 100L);

        assertThat(response.name()).isEqualTo("conf.d");
        assertThat(response.isDirectory()).isTrue();
    }

    @Test
    void listFiles_treeStructure() {
        when(roleFileRepository.findByRoleIdOrderByParentDirAscNameAsc(1L))
            .thenReturn(List.of(
                createFile(1L, 1L, "", "conf.d", true),
                createFile(2L, 1L, "conf.d", "app.conf", false),
                createFile(3L, 1L, "", "readme.txt", false)
            ));

        List<FileResponse> tree = roleFileService.listFilesAsTree(1L);

        assertThat(tree).hasSize(2);
        assertThat(tree.get(0).name()).isEqualTo("conf.d");
        assertThat(tree.get(0).children()).hasSize(1);
        assertThat(tree.get(1).name()).isEqualTo("readme.txt");
    }

    @Test
    void getFile_success() {
        RoleFile file = createFile(1L, 1L, "", "config.yml", false);
        file.setContent("hello world".getBytes(StandardCharsets.UTF_8));
        when(roleFileRepository.findById(1L)).thenReturn(Optional.of(file));

        FileResponse response = roleFileService.getFile(1L);

        assertThat(response.name()).isEqualTo("config.yml");
        assertThat(response.textContent()).isEqualTo("hello world");
    }

    @Test
    void updateFileName_success() {
        RoleFile file = createFile(1L, 1L, "", "old.yml", false);
        when(roleFileRepository.findById(1L)).thenReturn(Optional.of(file));
        when(roleFileRepository.existsByRoleIdAndParentDirAndNameAndIdNot(1L, "", "new.yml", 1L)).thenReturn(false);
        when(roleFileRepository.save(any(RoleFile.class))).thenReturn(file);

        FileResponse response = roleFileService.updateFile(1L, new UpdateFileRequest("new.yml", null), 100L);

        assertThat(response.name()).isEqualTo("new.yml");
    }

    @Test
    void updateFileContent_success() {
        RoleFile file = createFile(1L, 1L, "", "config.yml", false);
        when(roleFileRepository.findById(1L)).thenReturn(Optional.of(file));
        when(roleFileRepository.existsByRoleIdAndParentDirAndNameAndIdNot(1L, "", "config.yml", 1L)).thenReturn(false);
        when(roleFileRepository.save(any(RoleFile.class))).thenReturn(file);

        FileResponse response = roleFileService.updateFile(1L, new UpdateFileRequest("config.yml", "new content"), 100L);

        assertThat(response.textContent()).isEqualTo("new content");
    }

    @Test
    void deleteFile_success() {
        RoleFile file = createFile(1L, 1L, "", "config.yml", false);
        when(roleFileRepository.findById(1L)).thenReturn(Optional.of(file));

        roleFileService.deleteFile(1L, 100L);

        verify(roleFileRepository).delete(file);
    }

    @Test
    void deleteDirectory_cascadesChildren() {
        RoleFile dir = createFile(1L, 1L, "", "conf.d", true);
        when(roleFileRepository.findById(1L)).thenReturn(Optional.of(dir));
        when(roleFileRepository.findByRoleIdAndParentDirStartingWith(1L, "conf.d/"))
            .thenReturn(List.of(createFile(2L, 1L, "conf.d", "app.conf", false)));

        roleFileService.deleteFile(1L, 100L);

        verify(roleFileRepository).delete(any(RoleFile.class));
        verify(roleFileRepository, atLeast(2)).delete(any(RoleFile.class));
    }
}
```

- [ ] **Step 2: Run test to verify failures**

Run: `cd backend && mvn test -Dtest=RoleFileServiceTest -pl .`
Expected: FAIL

- [ ] **Step 3: Write RoleFileService**

```java
package com.ansible.role.service;

import com.ansible.role.dto.CreateFileRequest;
import com.ansible.role.dto.FileResponse;
import com.ansible.role.dto.UpdateFileRequest;
import com.ansible.role.entity.RoleFile;
import com.ansible.role.repository.RoleFileRepository;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoleFileService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final RoleFileRepository roleFileRepository;

    public RoleFileService(RoleFileRepository roleFileRepository) {
        this.roleFileRepository = roleFileRepository;
    }

    public FileResponse createFile(Long roleId, CreateFileRequest request, byte[] content, Long userId) {
        boolean isDir = Boolean.TRUE.equals(request.isDirectory());
        if (roleFileRepository.existsByRoleIdAndParentDirAndName(roleId, request.parentDir(), request.name())) {
            throw new IllegalArgumentException("File or directory '" + request.name() + "' already exists");
        }
        if (!isDir && content != null && content.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }
        RoleFile file = new RoleFile();
        file.setRoleId(roleId);
        file.setParentDir(request.parentDir());
        file.setName(request.name());
        file.setIsDirectory(isDir);
        file.setContent(isDir ? null : (content != null ? content : new byte[0]));
        file.setCreatedBy(userId);
        return toResponse(roleFileRepository.save(file), null);
    }

    @Transactional(readOnly = true)
    public List<FileResponse> listFilesAsTree(Long roleId) {
        List<RoleFile> allFiles = roleFileRepository.findByRoleIdOrderByParentDirAscNameAsc(roleId);
        Map<String, List<RoleFile>> byParent = allFiles.stream()
            .collect(Collectors.groupingBy(RoleFile::getParentDir));

        return buildTree("", byParent);
    }

    private List<FileResponse> buildTree(String parentDir, Map<String, List<RoleFile>> byParent) {
        List<RoleFile> files = byParent.getOrDefault(parentDir, List.of());
        return files.stream()
            .map(f -> toResponse(f, buildTree(
                f.getParentDir().isEmpty() ? f.getName() : f.getParentDir() + "/" + f.getName(),
                byParent)))
            .toList();
    }

    @Transactional(readOnly = true)
    public FileResponse getFile(Long fileId) {
        RoleFile file = roleFileRepository.findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
        return toResponse(file, null);
    }

    public byte[] getFileContent(Long fileId) {
        RoleFile file = roleFileRepository.findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            throw new IllegalArgumentException("Cannot download a directory");
        }
        return file.getContent();
    }

    public String getFileName(Long fileId) {
        RoleFile file = roleFileRepository.findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
        return file.getName();
    }

    public FileResponse updateFile(Long fileId, UpdateFileRequest request, Long userId) {
        RoleFile file = roleFileRepository.findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
        if (!file.getName().equals(request.name())) {
            if (roleFileRepository.existsByRoleIdAndParentDirAndNameAndIdNot(
                file.getRoleId(), file.getParentDir(), request.name(), fileId)) {
                throw new IllegalArgumentException("File or directory '" + request.name() + "' already exists");
            }
        }
        file.setName(request.name());
        if (request.textContent() != null && !Boolean.TRUE.equals(file.getIsDirectory())) {
            file.setContent(request.textContent().getBytes(StandardCharsets.UTF_8));
        }
        return toResponse(roleFileRepository.save(file), null);
    }

    public void deleteFile(Long fileId, Long userId) {
        RoleFile file = roleFileRepository.findById(fileId)
            .orElseThrow(() -> new IllegalArgumentException("File not found"));
        if (Boolean.TRUE.equals(file.getIsDirectory())) {
            String childPrefix = file.getParentDir().isEmpty()
                ? file.getName() + "/" : file.getParentDir() + "/" + file.getName() + "/";
            List<RoleFile> children = roleFileRepository.findByRoleIdAndParentDirStartingWith(file.getRoleId(), childPrefix);
            roleFileRepository.deleteAll(children);
        }
        roleFileRepository.delete(file);
    }

    private FileResponse toResponse(RoleFile file, List<FileResponse> children) {
        String textContent = null;
        Long size = null;
        if (!Boolean.TRUE.equals(file.getIsDirectory()) && file.getContent() != null) {
            size = (long) file.getContent().length;
            try {
                textContent = new String(file.getContent(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                textContent = null; // binary file
            }
        }
        return new FileResponse(
            file.getId(), file.getRoleId(), file.getParentDir(), file.getName(),
            file.getIsDirectory(), size, textContent, children,
            file.getCreatedAt(), file.getUpdatedAt());
    }
}
```

- [ ] **Step 4: Run tests**

Run: `cd backend && mvn test -Dtest=RoleFileServiceTest -pl .`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/role/service/RoleFileService.java backend/src/test/java/com/ansible/role/service/RoleFileServiceTest.java
git commit -m "feat(file): add RoleFileService with unit tests"
```

---

### Task 4: RoleFileController with Integration Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/role/controller/RoleFileController.java`
- Create: `backend/src/test/java/com/ansible/role/controller/RoleFileControllerTest.java`

- [ ] **Step 1: Write RoleFileControllerTest**

```java
package com.ansible.role.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.common.AbstractIntegrationTest;
import com.ansible.role.dto.CreateFileRequest;
import com.ansible.role.dto.UpdateFileRequest;
import com.ansible.role.repository.RoleFileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

class RoleFileControllerTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RoleFileRepository roleFileRepository;

    private String authHeaders;
    private Long roleId;

    @BeforeEach
    void setUp() {
        var auth = registerAndLogin("fileuser", "fileuser@test.com", "pass123");
        authHeaders = auth.token();
        Long projectId = createProject(authHeaders, "File Test Project");
        roleId = createRole(authHeaders, projectId, "test-role");
    }

    @AfterEach
    void tearDown() {
        roleFileRepository.deleteAll();
        cleanupProjectsAndUsers();
    }

    @Test
    void createFile_success() {
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/roles/" + roleId + "/files",
            HttpMethod.POST,
            withAuth(authHeaders, new CreateFileRequest("", "config.yml", false)),
            String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("config.yml");
    }

    @Test
    void createDirectory_success() {
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/roles/" + roleId + "/files",
            HttpMethod.POST,
            withAuth(authHeaders, new CreateFileRequest("", "conf.d", true)),
            String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("conf.d");
    }

    @Test
    void listFiles_treeStructure() {
        restTemplate.exchange("/api/roles/" + roleId + "/files",
            HttpMethod.POST, withAuth(authHeaders, new CreateFileRequest("", "conf.d", true)), String.class);
        restTemplate.exchange("/api/roles/" + roleId + "/files",
            HttpMethod.POST, withAuth(authHeaders, new CreateFileRequest("conf.d", "app.conf", false)), String.class);
        restTemplate.exchange("/api/roles/" + roleId + "/files",
            HttpMethod.POST, withAuth(authHeaders, new CreateFileRequest("", "readme.txt", false)), String.class);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/roles/" + roleId + "/files",
            HttpMethod.GET, withAuth(authHeaders, null), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("conf.d", "readme.txt");
    }

    @Test
    void getFileContent_success() {
        var createResp = restTemplate.exchange("/api/roles/" + roleId + "/files",
            HttpMethod.POST, withAuth(authHeaders, new CreateFileRequest("", "test.txt", false)), String.class);
        Long fileId = extractId(createResp);

        // Update with text content
        restTemplate.exchange("/api/files/" + fileId,
            HttpMethod.PUT, withAuth(authHeaders, new UpdateFileRequest("test.txt", "hello world")), String.class);

        ResponseEntity<byte[]> downloadResp = restTemplate.exchange(
            "/api/files/" + fileId + "/download",
            HttpMethod.GET, withAuth(authHeaders, null), byte[].class);

        assertThat(downloadResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(new String(downloadResp.getBody())).contains("hello world");
    }

    @Test
    void deleteFile_success() {
        var createResp = restTemplate.exchange("/api/roles/" + roleId + "/files",
            HttpMethod.POST, withAuth(authHeaders, new CreateFileRequest("", "config.yml", false)), String.class);
        Long fileId = extractId(createResp);

        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/files/" + fileId,
            HttpMethod.DELETE, withAuth(authHeaders, null), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(roleFileRepository.findById(fileId)).isEmpty();
    }
}
```

- [ ] **Step 2: Write RoleFileController**

```java
package com.ansible.role.controller;

import com.ansible.role.dto.CreateFileRequest;
import com.ansible.role.dto.FileResponse;
import com.ansible.role.dto.UpdateFileRequest;
import com.ansible.role.service.RoleFileService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RoleFileController {

    private final RoleFileService roleFileService;

    public RoleFileController(RoleFileService roleFileService) {
        this.roleFileService = roleFileService;
    }

    @PostMapping("/roles/{roleId}/files")
    public ResponseEntity<FileResponse> createFile(
        @PathVariable Long roleId,
        @Valid @RequestBody CreateFileRequest request,
        @RequestParam(required = false) String textContent,
        @AuthenticationPrincipal Long userId) {
        byte[] content = textContent != null ? textContent.getBytes(StandardCharsets.UTF_8) : null;
        return ResponseEntity.ok(roleFileService.createFile(roleId, request, content, userId));
    }

    @GetMapping("/roles/{roleId}/files")
    public ResponseEntity<List<FileResponse>> listFiles(@PathVariable Long roleId) {
        return ResponseEntity.ok(roleFileService.listFilesAsTree(roleId));
    }

    @GetMapping("/files/{fileId}")
    public ResponseEntity<FileResponse> getFile(@PathVariable Long fileId) {
        return ResponseEntity.ok(roleFileService.getFile(fileId));
    }

    @PutMapping("/files/{fileId}")
    public ResponseEntity<FileResponse> updateFile(
        @PathVariable Long fileId,
        @Valid @RequestBody UpdateFileRequest request,
        @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(roleFileService.updateFile(fileId, request, userId));
    }

    @DeleteMapping("/files/{fileId}")
    public ResponseEntity<Void> deleteFile(
        @PathVariable Long fileId,
        @AuthenticationPrincipal Long userId) {
        roleFileService.deleteFile(fileId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long fileId) {
        byte[] content = roleFileService.getFileContent(fileId);
        String filename = roleFileService.getFileName(fileId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(content);
    }
}
```

- [ ] **Step 3: Run integration tests**

Run: `cd backend && mvn verify -Dtest=RoleFileControllerTest -pl .`
Expected: PASS

- [ ] **Step 4: Run quality checks**

Run: `cd backend && mvn spotless:apply checkstyle:check pmd:check spotbugs:check -pl .`
Expected: All pass

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/role/controller/RoleFileController.java backend/src/test/java/com/ansible/role/controller/RoleFileControllerTest.java
git commit -m "feat(file): add RoleFileController with integration tests"
```

---

## Frontend Tasks

### Task 5: RoleFile Types and API Layer

**Files:**
- Create: `frontend/src/types/entity/RoleFile.ts`
- Create: `frontend/src/api/roleFile.ts`

- [ ] **Step 1: Create RoleFile types**

```typescript
// frontend/src/types/entity/RoleFile.ts

export interface RoleFile {
  id: number;
  roleId: number;
  parentDir: string;
  name: string;
  isDirectory: boolean;
  size: number | null;
  textContent: string | null;
  children: RoleFile[] | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFileRequest {
  parentDir: string;
  name: string;
  isDirectory: boolean;
  textContent?: string;
}

export interface UpdateFileRequest {
  name: string;
  textContent?: string;
}
```

- [ ] **Step 2: Create RoleFile API**

```typescript
// frontend/src/api/roleFile.ts

import request from './request';
import type { RoleFile, CreateFileRequest, UpdateFileRequest } from '../types/entity/RoleFile';

export const roleFileApi = {
  list: (roleId: number) =>
    request.get<RoleFile[]>(`/api/roles/${roleId}/files`),

  get: (fileId: number) =>
    request.get<RoleFile>(`/api/files/${fileId}`),

  create: (roleId: number, data: CreateFileRequest) =>
    request.post<RoleFile>(`/api/roles/${roleId}/files`, data,
      { params: data.textContent ? { textContent: data.textContent } : undefined }),

  update: (fileId: number, data: UpdateFileRequest) =>
    request.put<RoleFile>(`/api/files/${fileId}`, data),

  delete: (fileId: number) =>
    request.delete(`/api/files/${fileId}`),

  downloadUrl: (fileId: number) => `/api/files/${fileId}/download`,
};
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/types/entity/RoleFile.ts frontend/src/api/roleFile.ts
git commit -m "feat(file): add RoleFile types and API layer"
```

---

### Task 6: RoleFiles Page Component

**Files:**
- Create: `frontend/src/pages/role/RoleFiles.tsx`

- [ ] **Step 1: Create RoleFiles component**

```tsx
// frontend/src/pages/role/RoleFiles.tsx

import { useEffect, useState } from 'react';
import { Button, Tree, Modal, Form, Input, message, Popconfirm, Space, Typography } from 'antd';
import { PlusOutlined, FolderOutlined, FileOutlined, DownloadOutlined } from '@ant-design/icons';
import { roleFileApi } from '../../api/roleFile';
import type { RoleFile } from '../../types/entity/RoleFile';

const { Text } = Typography;

interface Props {
  roleId: number;
}

export default function RoleFiles({ roleId }: Props) {
  const [files, setFiles] = useState<RoleFile[]>([]);
  const [loading, setLoading] = useState(false);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState<RoleFile | null>(null);
  const [currentDir, setCurrentDir] = useState('');
  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();

  const fetchFiles = async () => {
    setLoading(true);
    try {
      const res = await roleFileApi.list(roleId);
      setFiles(res.data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchFiles(); }, [roleId]);

  const handleCreate = (parentDir: string, isDir: boolean) => {
    setCurrentDir(parentDir);
    createForm.resetFields();
    createForm.setFieldsValue({ isDirectory: isDir, parentDir });
    setCreateModalOpen(true);
  };

  const handleCreateSubmit = async () => {
    const values = await createForm.validateFields();
    await roleFileApi.create(roleId, values);
    message.success(values.isDirectory ? '目录已创建' : '文件已创建');
    setCreateModalOpen(false);
    fetchFiles();
  };

  const handleEdit = (file: RoleFile) => {
    setSelectedFile(file);
    editForm.setFieldsValue({ name: file.name, textContent: file.textContent || '' });
    setEditModalOpen(true);
  };

  const handleEditSubmit = async () => {
    const values = await editForm.validateFields();
    await roleFileApi.update(selectedFile!.id, values);
    message.success('更新成功');
    setEditModalOpen(false);
    fetchFiles();
  };

  const handleDelete = async (fileId: number) => {
    await roleFileApi.delete(fileId);
    message.success('删除成功');
    fetchFiles();
  };

  const handleDownload = (fileId: number) => {
    window.open(roleFileApi.downloadUrl(fileId), '_blank');
  };

  const toTreeData = (fileList: RoleFile[]): any[] =>
    fileList.map(f => ({
      key: f.id,
      title: (
        <Space>
          {f.isDirectory ? <FolderOutlined /> : <FileOutlined />}
          <span>{f.name}</span>
          {!f.isDirectory && f.size != null && (
            <Text type="secondary" style={{ fontSize: 12 }}>
              ({(f.size / 1024).toFixed(1)} KB)
            </Text>
          )}
          <Button type="link" size="small" onClick={(e) => { e.stopPropagation(); handleEdit(f); }}>编辑</Button>
          {!f.isDirectory && (
            <Button type="link" size="small" icon={<DownloadOutlined />}
              onClick={(e) => { e.stopPropagation(); handleDownload(f.id); }} />
          )}
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(f.id)}>
            <Button type="link" size="small" danger onClick={(e) => e.stopPropagation()}>删除</Button>
          </Popconfirm>
        </Space>
      ),
      children: f.children ? toTreeData(f.children) : undefined,
    }));

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Space>
          <Button icon={<PlusOutlined />} onClick={() => handleCreate('', false)}>新建文件</Button>
          <Button icon={<PlusOutlined />} onClick={() => handleCreate('', true)}>新建目录</Button>
        </Space>
      </div>

      <Tree
        treeData={toTreeData(files)}
        loading={loading}
        defaultExpandAll
      />

      <Modal title="新建" open={createModalOpen} onOk={handleCreateSubmit} onCancel={() => setCreateModalOpen(false)}>
        <Form form={createForm} layout="vertical">
          <Form.Item name="parentDir" hidden><Input /></Form.Item>
          <Form.Item name="isDirectory" hidden><Input /></Form.Item>
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input placeholder={createForm.getFieldValue('isDirectory') ? '目录名称' : '文件名称'} />
          </Form.Item>
          {!createForm.getFieldValue('isDirectory') && (
            <Form.Item name="textContent" label="文件内容">
              <Input.TextArea rows={8} placeholder="文件内容（可选，创建后可编辑）" />
            </Form.Item>
          )}
        </Form>
      </Modal>

      <Modal title="编辑文件" open={editModalOpen} onOk={handleEditSubmit} onCancel={() => setEditModalOpen(false)} width={640}>
        <Form form={editForm} layout="vertical">
          <Form.Item name="name" label="文件名" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          {selectedFile && !selectedFile.isDirectory && (
            <Form.Item name="textContent" label="内容">
              <Input.TextArea rows={15} style={{ fontFamily: 'monospace' }} />
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  );
}
```

- [ ] **Step 2: Add Files tab to RoleDetail**

Add RoleFiles as a new tab in the RoleDetail page (follow existing pattern for RoleTemplates tab).

- [ ] **Step 3: Run lint and build**

Run: `cd frontend && npm run lint && npm run build`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/role/RoleFiles.tsx
git commit -m "feat(file): add RoleFiles page component with tree view"
```
