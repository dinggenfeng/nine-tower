# Phase 3: HostGroup + Host + Role CRUD — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement HostGroup CRUD, Host CRUD (with AES-256 encryption), and Role CRUD (basic) — backend and frontend complete闭环.

**Architecture:** `com.ansible.host/` for HostGroup and Host; `com.ansible.role/` for Role. `EncryptionService` for AES-256-GCM host credential encryption. Frontend: single-page Master-Detail for HostGroupManager, RoleList + RoleDetail with Tab skeleton.

**Tech Stack:** Java 21, Spring Boot 3.3.5, Spring Data JPA, PostgreSQL, Testcontainers, JUnit 5, Mockito, React 18, TypeScript, Ant Design 5, Zustand

---

## File Map

### Backend — New Files

| File | Responsibility |
|------|---------------|
| `backend/src/main/java/com/ansible/common/EncryptionService.java` | AES-256-GCM encryption/decryption |
| `backend/src/main/java/com/ansible/host/entity/HostGroup.java` | HostGroup JPA entity (extends BaseEntity) |
| `backend/src/main/java/com/ansible/host/entity/Host.java` | Host JPA entity (extends BaseEntity) |
| `backend/src/main/java/com/ansible/host/repository/HostGroupRepository.java` | Spring Data repository |
| `backend/src/main/java/com/ansible/host/repository/HostRepository.java` | Spring Data repository |
| `backend/src/main/java/com/ansible/host/dto/CreateHostGroupRequest.java` | DTO: name, description |
| `backend/src/main/java/com/ansible/host/dto/UpdateHostGroupRequest.java` | DTO: name, description |
| `backend/src/main/java/com/ansible/host/dto/HostGroupResponse.java` | DTO: id, projectId, name, description, createdBy, createdAt |
| `backend/src/main/java/com/ansible/host/dto/CreateHostRequest.java` | DTO: name, ip, port, ansibleUser, ansibleSshPass, ansibleSshPrivateKeyFile, ansibleBecome |
| `backend/src/main/java/com/ansible/host/dto/UpdateHostRequest.java` | DTO: same fields |
| `backend/src/main/java/com/ansible/host/dto/HostResponse.java` | DTO: all fields, sensitive fields masked as `****` |
| `backend/src/main/java/com/ansible/host/service/HostGroupService.java` | HostGroup CRUD business logic |
| `backend/src/main/java/com/ansible/host/service/HostService.java` | Host CRUD + encryption/decryption |
| `backend/src/main/java/com/ansible/host/controller/HostGroupController.java` | REST: `/api/projects/{projectId}/host-groups` + `/api/host-groups/{id}` |
| `backend/src/main/java/com/ansible/host/controller/HostController.java` | REST: `/api/host-groups/{hgId}/hosts` + `/api/hosts/{id}` |
| `backend/src/test/java/com/ansible/host/service/HostGroupServiceTest.java` | Unit tests |
| `backend/src/test/java/com/ansible/host/service/HostServiceTest.java` | Unit tests (incl. encryption) |
| `backend/src/test/java/com/ansible/host/service/EncryptionServiceTest.java` | Encryption unit tests |
| `backend/src/test/java/com/ansible/host/controller/HostGroupControllerTest.java` | Integration tests |
| `backend/src/test/java/com/ansible/host/controller/HostControllerTest.java` | Integration tests |
| `backend/src/main/java/com/ansible/role/entity/Role.java` | Role JPA entity (extends BaseEntity) |
| `backend/src/main/java/com/ansible/role/repository/RoleRepository.java` | Spring Data repository |
| `backend/src/main/java/com/ansible/role/dto/CreateRoleRequest.java` | DTO: name, description |
| `backend/src/main/java/com/ansible/role/dto/UpdateRoleRequest.java` | DTO: name, description |
| `backend/src/main/java/com/ansible/role/dto/RoleResponse.java` | DTO: id, projectId, name, description, createdBy, createdAt |
| `backend/src/main/java/com/ansible/role/service/RoleService.java` | Role CRUD business logic |
| `backend/src/main/java/com/ansible/role/controller/RoleController.java` | REST: `/api/projects/{projectId}/roles` + `/api/roles/{id}` |
| `backend/src/test/java/com/ansible/role/service/RoleServiceTest.java` | Unit tests |
| `backend/src/test/java/com/ansible/role/controller/RoleControllerTest.java` | Integration tests |
| `backend/src/main/resources/application.yml` | Add `app.encryption.key` config |

### Frontend — New Files

| File | Responsibility |
|------|---------------|
| `frontend/src/types/entity/Host.ts` | HostGroup, Host, request/response TypeScript interfaces |
| `frontend/src/types/entity/Role.ts` | Role TypeScript interfaces |
| `frontend/src/api/host.ts` | HostGroup and Host API functions |
| `frontend/src/api/role.ts` | Role API functions |
| `frontend/src/pages/host/HostGroupManager.tsx` | Single-page Master-Detail: left=HostGroup list, right=Host list |
| `frontend/src/pages/role/RoleList.tsx` | Role table with create/edit/delete |
| `frontend/src/pages/role/RoleDetail.tsx` | Role detail with 6 Tab skeleton (Tasks/Handlers/Templates/Files/Vars/Defaults) |

---

## Task 1: EncryptionService with AES-256-GCM

**Files:**
- Create: `backend/src/main/java/com/ansible/common/EncryptionService.java`
- Create: `backend/src/test/java/com/ansible/host/service/EncryptionServiceTest.java`
- Modify: `backend/src/main/resources/application.yml` (add `app.encryption.key`)

- [ ] **Step 1: Add AES config to application.yml**

Read `backend/src/main/resources/application.yml`, then add:

```yaml
app:
  encryption:
    key: ${ENCRYPTION_KEY:ZGVmYXVsdC1lbmNyeXB0aW9uLWtleS0zMi1ieXRlcw==}
```

> Base64-encoded 32-byte key. In production, set `ENCRYPTION_KEY` env var.

- [ ] **Step 2: Create `EncryptionService.java`**

```java
package com.ansible.common;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class EncryptionService {

  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;

  private final SecretKeySpec secretKey;

  public EncryptionService(@Value("${app.encryption.key}") String base64Key) {
    byte[] keyBytes = Base64.getDecoder().decode(base64Key);
    if (keyBytes.length != 32) {
      throw new IllegalStateException("AES-256 key must be 32 bytes");
    }
    this.secretKey = new SecretKeySpec(keyBytes, "AES");
  }

  public String encrypt(String plaintext) {
    if (!StringUtils.hasText(plaintext)) {
      return plaintext;
    }
    try {
      byte[] iv = new byte[GCM_IV_LENGTH];
      new SecureRandom().nextBytes(iv);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      byte[] combined = new byte[iv.length + ciphertext.length];
      System.arraycopy(iv, 0, combined, 0, iv.length);
      System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
      return Base64.getEncoder().encodeToString(combined);
    } catch (Exception e) {
      throw new RuntimeException("Encryption failed", e);
    }
  }

  public String decrypt(String encrypted) {
    if (encrypted == null || encrypted.equals(mask())) {
      return encrypted;
    }
    try {
      byte[] combined = Base64.getDecoder().decode(encrypted);
      byte[] iv = new byte[GCM_IV_LENGTH];
      byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
      System.arraycopy(combined, 0, iv, 0, iv.length);
      System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
      return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("Decryption failed", e);
    }
  }

  public static String mask() {
    return "****";
  }
}
```

- [ ] **Step 3: Write `EncryptionServiceTest.java`**

```java
package com.ansible.host.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ansible.common.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EncryptionServiceTest {

  private EncryptionService encryptionService;

  @BeforeEach
  void setUp() {
    // 32-byte key in base64
    encryptionService = new EncryptionService("dGVzdC1lbmNyeXB0aW9uLWtleS0zMi1ieXRlcw==");
  }

  @Test
  void encrypt_then_decrypt_returns_original() {
    String original = "my-secret-password";
    String encrypted = encryptionService.encrypt(original);
    assertThat(encrypted).isNotEqualTo(original);
    assertThat(encryptionService.decrypt(encrypted)).isEqualTo(original);
  }

  @Test
  void encrypt_produces_different_ciphertext_each_time() {
    String plaintext = "password";
    String encrypted1 = encryptionService.encrypt(plaintext);
    String encrypted2 = encryptionService.encrypt(plaintext);
    assertThat(encrypted1).isNotEqualTo(encrypted2);
  }

  @Test
  void encrypt_null_returns_null() {
    assertThat(encryptionService.encrypt(null)).isNull();
  }

  @Test
  void encrypt_empty_returns_empty() {
    assertThat(encryptionService.encrypt("")).isEmpty();
  }

  @Test
  void decrypt_mask_returns_mask() {
    assertThat(encryptionService.decrypt("****")).isEqualTo("****");
  }

  @Test
  void decrypt_null_returns_null() {
    assertThat(encryptionService.decrypt(null)).isNull();
  }

  @Test
  void mask_returns_four_asterisks() {
    assertThat(EncryptionService.mask()).isEqualTo("****");
  }
}
```

- [ ] **Step 4: Run encryption tests**

Run: `cd backend && mvn test -Dtest="EncryptionServiceTest" -q`
Expected: Tests run: 5, Failures: 0, Errors: 0

- [ ] **Step 5: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/ansible/common/EncryptionService.java
git add backend/src/test/java/com/ansible/host/service/EncryptionServiceTest.java
git add backend/src/main/resources/application.yml
git commit -m "feat: add EncryptionService with AES-256-GCM"
```

---

## Task 2: ProjectAccessChecker — add checkOwnerOrAdmin

**Files:**
- Modify: `backend/src/main/java/com/ansible/security/ProjectAccessChecker.java`
- Create: `backend/src/test/java/com/ansible/security/ProjectAccessCheckerTest.java`

- [ ] **Step 1: Read current ProjectAccessChecker**

Read `backend/src/main/java/com/ansible/security/ProjectAccessChecker.java`

- [ ] **Step 2: Add checkOwnerOrAdmin method**

Replace the entire file with:

```java
package com.ansible.security;

import com.ansible.common.enums.ProjectRole;
import com.ansible.project.entity.ProjectMember;
import com.ansible.project.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectAccessChecker {

  private final ProjectMemberRepository projectMemberRepository;

  public ProjectMember checkMembership(Long projectId, Long userId) {
    return projectMemberRepository
        .findByProjectIdAndUserId(projectId, userId)
        .orElseThrow(() -> new SecurityException("Not a member of this project"));
  }

  public void checkAdmin(Long projectId, Long userId) {
    ProjectMember member = checkMembership(projectId, userId);
    if (member.getRole() != ProjectRole.PROJECT_ADMIN) {
      throw new SecurityException("Only project admins can perform this action");
    }
  }

  public void checkOwnerOrAdmin(Long projectId, Long resourceCreatedBy, Long currentUserId) {
    if (resourceCreatedBy.equals(currentUserId)) {
      return;
    }
    ProjectMember member = checkMembership(projectId, currentUserId);
    if (member.getRole() != ProjectRole.PROJECT_ADMIN) {
      throw new SecurityException("Only the creator or project admins can perform this action");
    }
  }
}
```

- [ ] **Step 3: Write `ProjectAccessCheckerTest.java`**

```java
package com.ansible.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ansible.common.enums.ProjectRole;
import com.ansible.project.entity.ProjectMember;
import com.ansible.project.repository.ProjectMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectAccessCheckerTest {

  @Mock private ProjectMemberRepository projectMemberRepository;
  @InjectMocks private ProjectAccessChecker accessChecker;

  private ProjectMember adminMember;

  @BeforeEach
  void setUp() {
    adminMember = new ProjectMember();
    adminMember.setProjectId(1L);
    adminMember.setUserId(10L);
    adminMember.setRole(ProjectRole.PROJECT_ADMIN);
  }

  @Test
  void checkOwnerOrAdmin_passes_when_user_is_owner() {
    // no exception
    accessChecker.checkOwnerOrAdmin(1L, 10L, 10L);
  }

  @Test
  void checkOwnerOrAdmin_passes_when_user_is_admin() {
    when(projectMemberRepository.findByProjectIdAndUserId(1L, 20L))
        .thenReturn(java.util.Optional.of(adminMember));
    // no exception
    accessChecker.checkOwnerOrAdmin(1L, 10L, 20L);
  }

  @Test
  void checkOwnerOrAdmin_throws_when_neither_owner_nor_admin() {
    ProjectMember regularMember = new ProjectMember();
    regularMember.setProjectId(1L);
    regularMember.setUserId(20L);
    regularMember.setRole(ProjectRole.PROJECT_MEMBER);
    when(projectMemberRepository.findByProjectIdAndUserId(1L, 20L))
        .thenReturn(java.util.Optional.of(regularMember));

    assertThatThrownBy(() -> accessChecker.checkOwnerOrAdmin(1L, 10L, 20L))
        .isInstanceOf(SecurityException.class);
  }
}
```

- [ ] **Step 4: Run tests**

Run: `cd backend && mvn test -Dtest="ProjectAccessCheckerTest" -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/security/ProjectAccessChecker.java
git add backend/src/test/java/com/ansible/security/ProjectAccessCheckerTest.java
git commit -m "feat: add checkOwnerOrAdmin to ProjectAccessChecker"
```

---

## Task 3: HostGroup Backend — Entity, Repository, DTOs, Service + Unit Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/host/entity/HostGroup.java`
- Create: `backend/src/main/java/com/ansible/host/repository/HostGroupRepository.java`
- Create: `backend/src/main/java/com/ansible/host/dto/CreateHostGroupRequest.java`
- Create: `backend/src/main/java/com/ansible/host/dto/UpdateHostGroupRequest.java`
- Create: `backend/src/main/java/com/ansible/host/dto/HostGroupResponse.java`
- Create: `backend/src/main/java/com/ansible/host/service/HostGroupService.java`
- Create: `backend/src/test/java/com/ansible/host/service/HostGroupServiceTest.java`

- [ ] **Step 1: Create directory structure**

```bash
mkdir -p backend/src/main/java/com/ansible/host/entity
mkdir -p backend/src/main/java/com/ansible/host/repository
mkdir -p backend/src/main/java/com/ansible/host/dto
mkdir -p backend/src/main/java/com/ansible/host/service
mkdir -p backend/src/test/java/com/ansible/host/service
```

- [ ] **Step 2: Create `HostGroup.java`**

```java
package com.ansible.host.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "host_groups")
@Getter
@Setter
@NoArgsConstructor
public class HostGroup extends BaseEntity {

  @Column(nullable = false)
  private Long projectId;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 500)
  private String description;
}
```

- [ ] **Step 3: Create `HostGroupRepository.java`**

```java
package com.ansible.host.repository;

import com.ansible.host.entity.HostGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HostGroupRepository extends JpaRepository<HostGroup, Long> {

  List<HostGroup> findAllByProjectId(Long projectId);

  boolean existsByProjectIdAndName(Long projectId, String name);
}
```

- [ ] **Step 4: Create `CreateHostGroupRequest.java`**

```java
package com.ansible.host.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateHostGroupRequest {

  @NotBlank(message = "Host group name is required")
  @Size(max = 100, message = "Name must not exceed 100 characters")
  private String name;

  @Size(max = 500, message = "Description must not exceed 500 characters")
  private String description;
}
```

- [ ] **Step 5: Create `UpdateHostGroupRequest.java`**

```java
package com.ansible.host.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateHostGroupRequest {

  @Size(max = 100, message = "Name must not exceed 100 characters")
  private String name;

  @Size(max = 500, message = "Description must not exceed 500 characters")
  private String description;
}
```

- [ ] **Step 6: Create `HostGroupResponse.java`**

```java
package com.ansible.host.dto;

import com.ansible.host.entity.HostGroup;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class HostGroupResponse {

  private final Long id;
  private final Long projectId;
  private final String name;
  private final String description;
  private final Long createdBy;
  private final LocalDateTime createdAt;

  @JsonCreator
  public HostGroupResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("projectId") Long projectId,
      @JsonProperty("name") String name,
      @JsonProperty("description") String description,
      @JsonProperty("createdBy") Long createdBy,
      @JsonProperty("createdAt") LocalDateTime createdAt) {
    this.id = id;
    this.projectId = projectId;
    this.name = name;
    this.description = description;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
  }

  public HostGroupResponse(HostGroup hostGroup) {
    this.id = hostGroup.getId();
    this.projectId = hostGroup.getProjectId();
    this.name = hostGroup.getName();
    this.description = hostGroup.getDescription();
    this.createdBy = hostGroup.getCreatedBy();
    this.createdAt = hostGroup.getCreatedAt();
  }
}
```

- [ ] **Step 7: Write `HostGroupServiceTest.java`**

```java
package com.ansible.host.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.host.dto.CreateHostGroupRequest;
import com.ansible.host.dto.HostGroupResponse;
import com.ansible.host.dto.UpdateHostGroupRequest;
import com.ansible.host.entity.HostGroup;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.security.ProjectAccessChecker;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HostGroupServiceTest {

  @Mock private HostGroupRepository hostGroupRepository;
  @Mock private ProjectAccessChecker accessChecker;
  @InjectMocks private HostGroupService hostGroupService;

  private HostGroup testHostGroup;

  @BeforeEach
  void setUp() {
    testHostGroup = new HostGroup();
    ReflectionTestUtils.setField(testHostGroup, "id", 1L);
    testHostGroup.setProjectId(10L);
    testHostGroup.setName("Web Servers");
    testHostGroup.setDescription("All web servers");
    testHostGroup.setCreatedBy(10L);
    ReflectionTestUtils.setField(testHostGroup, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(testHostGroup, "updatedAt", LocalDateTime.now());
  }

  @Test
  void createHostGroup_success() {
    CreateHostGroupRequest request = new CreateHostGroupRequest();
    request.setName("Web Servers");
    request.setDescription("All web servers");

    when(hostGroupRepository.save(any(HostGroup.class))).thenReturn(testHostGroup);

    HostGroupResponse response = hostGroupService.createHostGroup(request, 10L);

    assertThat(response.getName()).isEqualTo("Web Servers");
    verify(hostGroupRepository).save(any(HostGroup.class));
  }

  @Test
  void getHostGroupsByProject_success() {
    when(hostGroupRepository.findAllByProjectId(10L)).thenReturn(List.of(testHostGroup));

    List<HostGroupResponse> groups = hostGroupService.getHostGroupsByProject(10L);

    assertThat(groups).hasSize(1);
    assertThat(groups.get(0).getName()).isEqualTo("Web Servers");
  }

  @Test
  void getHostGroup_success() {
    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));

    HostGroupResponse response = hostGroupService.getHostGroup(1L, 10L);

    assertThat(response.getName()).isEqualTo("Web Servers");
  }

  @Test
  void getHostGroup_notFound_throws() {
    when(hostGroupRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> hostGroupService.getHostGroup(99L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void updateHostGroup_success() {
    UpdateHostGroupRequest request = new UpdateHostGroupRequest();
    request.setName("Updated Name");

    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));
    when(hostGroupRepository.save(any(HostGroup.class))).thenReturn(testHostGroup);

    hostGroupService.updateHostGroup(1L, request, 10L);

    verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
    verify(hostGroupRepository).save(testHostGroup);
  }

  @Test
  void deleteHostGroup_success() {
    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));

    hostGroupService.deleteHostGroup(1L, 10L);

    verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
    verify(hostGroupRepository).delete(testHostGroup);
  }
}
```

- [ ] **Step 8: Run tests — expect failure (service not yet created)**

Run: `cd backend && mvn test -Dtest="HostGroupServiceTest" -q`
Expected: COMPILATION ERROR (HostGroupService does not exist)

- [ ] **Step 9: Create `HostGroupService.java`**

```java
package com.ansible.host.service;

import com.ansible.host.dto.CreateHostGroupRequest;
import com.ansible.host.dto.HostGroupResponse;
import com.ansible.host.dto.UpdateHostGroupRequest;
import com.ansible.host.entity.HostGroup;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.security.ProjectAccessChecker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class HostGroupService {

  private final HostGroupRepository hostGroupRepository;
  private final ProjectAccessChecker accessChecker;

  @Transactional
  public HostGroupResponse createHostGroup(CreateHostGroupRequest request, Long currentUserId) {
    accessChecker.checkMembership(request.getProjectId(), currentUserId);
    HostGroup hostGroup = new HostGroup();
    hostGroup.setProjectId(request.getProjectId());
    hostGroup.setName(request.getName());
    hostGroup.setDescription(request.getDescription());
    hostGroup.setCreatedBy(currentUserId);
    HostGroup saved = hostGroupRepository.save(hostGroup);
    return new HostGroupResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<HostGroupResponse> getHostGroupsByProject(Long projectId) {
    accessChecker.checkMembership(projectId, null);
    return hostGroupRepository.findAllByProjectId(projectId).stream()
        .map(HostGroupResponse::new)
        .toList();
  }

  @Transactional(readOnly = true)
  public HostGroupResponse getHostGroup(Long hostGroupId, Long currentUserId) {
    HostGroup hostGroup =
        hostGroupRepository
            .findById(hostGroupId)
            .orElseThrow(() -> new IllegalArgumentException("Host group not found"));
    accessChecker.checkMembership(hostGroup.getProjectId(), currentUserId);
    return new HostGroupResponse(hostGroup);
  }

  @Transactional
  public HostGroupResponse updateHostGroup(
      Long hostGroupId, UpdateHostGroupRequest request, Long currentUserId) {
    HostGroup hostGroup =
        hostGroupRepository
            .findById(hostGroupId)
            .orElseThrow(() -> new IllegalArgumentException("Host group not found"));
    accessChecker.checkOwnerOrAdmin(hostGroup.getProjectId(), hostGroup.getCreatedBy(), currentUserId);
    if (StringUtils.hasText(request.getName())) {
      hostGroup.setName(request.getName());
    }
    if (request.getDescription() != null) {
      hostGroup.setDescription(request.getDescription());
    }
    HostGroup saved = hostGroupRepository.save(hostGroup);
    return new HostGroupResponse(saved);
  }

  @Transactional
  public void deleteHostGroup(Long hostGroupId, Long currentUserId) {
    HostGroup hostGroup =
        hostGroupRepository
            .findById(hostGroupId)
            .orElseThrow(() -> new IllegalArgumentException("Host group not found"));
    accessChecker.checkOwnerOrAdmin(hostGroup.getProjectId(), hostGroup.getCreatedBy(), currentUserId);
    hostGroupRepository.delete(hostGroup);
  }
}
```

Note: `getHostGroupsByProject` receives `currentUserId` from controller but `checkMembership` requires both projectId and userId. Adjust service to pass currentUserId to `checkMembership`.

Fix: Read `HostGroupService.java` after creating, update the line:
```java
accessChecker.checkMembership(projectId, currentUserId);
```

- [ ] **Step 10: Run tests**

Run: `cd backend && mvn test -Dtest="HostGroupServiceTest" -q`
Expected: Tests run: 6, Failures: 0, Errors: 0

- [ ] **Step 11: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 12: Commit**

```bash
git add backend/src/main/java/com/ansible/host/entity/HostGroup.java
git add backend/src/main/java/com/ansible/host/repository/HostGroupRepository.java
git add backend/src/main/java/com/ansible/host/dto/CreateHostGroupRequest.java
git add backend/src/main/java/com/ansible/host/dto/UpdateHostGroupRequest.java
git add backend/src/main/java/com/ansible/host/dto/HostGroupResponse.java
git add backend/src/main/java/com/ansible/host/service/HostGroupService.java
git add backend/src/test/java/com/ansible/host/service/HostGroupServiceTest.java
git commit -m "feat: add HostGroup entity, repository, DTOs, service and unit tests"
```

---

## Task 4: Host Backend — Entity, Repository, DTOs, Service + Unit Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/host/entity/Host.java`
- Create: `backend/src/main/java/com/ansible/host/repository/HostRepository.java`
- Create: `backend/src/main/java/com/ansible/host/dto/CreateHostRequest.java`
- Create: `backend/src/main/java/com/ansible/host/dto/UpdateHostRequest.java`
- Create: `backend/src/main/java/com/ansible/host/dto/HostResponse.java`
- Create: `backend/src/main/java/com/ansible/host/service/HostService.java`
- Create: `backend/src/test/java/com/ansible/host/service/HostServiceTest.java`

- [ ] **Step 1: Create `Host.java`**

```java
package com.ansible.host.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hosts")
@Getter
@Setter
@NoArgsConstructor
public class Host extends BaseEntity {

  @Column(nullable = false)
  private Long hostGroupId;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 45)
  private String ip;

  @Column(nullable = false)
  private Integer port = 22;

  @Column(length = 100)
  private String ansibleUser;

  @Column(length = 500)
  private String ansibleSshPass;

  @Column(length = 2000)
  private String ansibleSshPrivateKeyFile;

  @Column(nullable = false)
  private Boolean ansibleBecome = false;
}
```

- [ ] **Step 2: Create `HostRepository.java`**

```java
package com.ansible.host.repository;

import com.ansible.host.entity.Host;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HostRepository extends JpaRepository<Host, Long> {

  List<Host> findAllByHostGroupId(Long hostGroupId);

  void deleteAllByHostGroupId(Long hostGroupId);
}
```

- [ ] **Step 3: Create `CreateHostRequest.java`**

```java
package com.ansible.host.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateHostRequest {

  @NotBlank(message = "Host name is required")
  @Size(max = 100, message = "Name must not exceed 100 characters")
  private String name;

  @NotBlank(message = "IP address is required")
  @Size(max = 45, message = "IP must not exceed 45 characters")
  private String ip;

  private Integer port = 22;

  @Size(max = 100, message = "SSH user must not exceed 100 characters")
  private String ansibleUser;

  @Size(max = 500, message = "SSH password must not exceed 500 characters")
  private String ansibleSshPass;

  @Size(max = 2000, message = "SSH private key must not exceed 2000 characters")
  private String ansibleSshPrivateKeyFile;

  private Boolean ansibleBecome = false;
}
```

- [ ] **Step 4: Create `UpdateHostRequest.java`**

```java
package com.ansible.host.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateHostRequest {

  @Size(max = 100, message = "Name must not exceed 100 characters")
  private String name;

  @Size(max = 45, message = "IP must not exceed 45 characters")
  private String ip;

  private Integer port;

  @Size(max = 100, message = "SSH user must not exceed 100 characters")
  private String ansibleUser;

  @Size(max = 500, message = "SSH password must not exceed 500 characters")
  private String ansibleSshPass;

  @Size(max = 2000, message = "SSH private key must not exceed 2000 characters")
  private String ansibleSshPrivateKeyFile;

  private Boolean ansibleBecome;
}
```

- [ ] **Step 5: Create `HostResponse.java`**

```java
package com.ansible.host.dto;

import com.ansible.common.EncryptionService;
import com.ansible.host.entity.Host;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class HostResponse {

  private final Long id;
  private final Long hostGroupId;
  private final String name;
  private final String ip;
  private final Integer port;
  private final String ansibleUser;
  private final String ansibleSshPass;
  private final String ansibleSshPrivateKeyFile;
  private final Boolean ansibleBecome;
  private final Long createdBy;
  private final LocalDateTime createdAt;

  @JsonCreator
  public HostResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("hostGroupId") Long hostGroupId,
      @JsonProperty("name") String name,
      @JsonProperty("ip") String ip,
      @JsonProperty("port") Integer port,
      @JsonProperty("ansibleUser") String ansibleUser,
      @JsonProperty("ansibleSshPass") String ansibleSshPass,
      @JsonProperty("ansibleSshPrivateKeyFile") String ansibleSshPrivateKeyFile,
      @JsonProperty("ansibleBecome") Boolean ansibleBecome,
      @JsonProperty("createdBy") Long createdBy,
      @JsonProperty("createdAt") LocalDateTime createdAt) {
    this.id = id;
    this.hostGroupId = hostGroupId;
    this.name = name;
    this.ip = ip;
    this.port = port;
    this.ansibleUser = ansibleUser;
    this.ansibleSshPass = ansibleSshPass;
    this.ansibleSshPrivateKeyFile = ansibleSshPrivateKeyFile;
    this.ansibleBecome = ansibleBecome;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
  }

  public HostResponse(Host host) {
    this.id = host.getId();
    this.hostGroupId = host.getHostGroupId();
    this.name = host.getName();
    this.ip = host.getIp();
    this.port = host.getPort();
    this.ansibleUser = host.getAnsibleUser();
    this.ansibleSshPass = EncryptionService.mask();
    this.ansibleSshPrivateKeyFile = EncryptionService.mask();
    this.ansibleBecome = host.getAnsibleBecome();
    this.createdBy = host.getCreatedBy();
    this.createdAt = host.getCreatedAt();
  }
}
```

- [ ] **Step 6: Write `HostServiceTest.java`**

```java
package com.ansible.host.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.common.EncryptionService;
import com.ansible.host.dto.CreateHostRequest;
import com.ansible.host.dto.HostResponse;
import com.ansible.host.dto.UpdateHostRequest;
import com.ansible.host.entity.Host;
import com.ansible.host.entity.HostGroup;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.host.repository.HostRepository;
import com.ansible.security.ProjectAccessChecker;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HostServiceTest {

  @Mock private HostRepository hostRepository;
  @Mock private HostGroupRepository hostGroupRepository;
  @Mock private ProjectAccessChecker accessChecker;
  @Mock private EncryptionService encryptionService;
  @InjectMocks private HostService hostService;

  private Host testHost;
  private HostGroup testHostGroup;

  @BeforeEach
  void setUp() {
    testHostGroup = new HostGroup();
    ReflectionTestUtils.setField(testHostGroup, "id", 1L);
    testHostGroup.setProjectId(10L);

    testHost = new Host();
    ReflectionTestUtils.setField(testHost, "id", 1L);
    testHost.setHostGroupId(1L);
    testHost.setName("web-01");
    testHost.setIp("192.168.1.10");
    testHost.setPort(22);
    testHost.setAnsibleUser("ansible");
    testHost.setAnsibleSshPass("encrypted");
    testHost.setAnsibleSshPrivateKeyFile("encrypted");
    testHost.setAnsibleBecome(false);
    testHost.setCreatedBy(10L);
    ReflectionTestUtils.setField(testHost, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(testHost, "updatedAt", LocalDateTime.now());
  }

  @Test
  void createHost_success() {
    CreateHostRequest request = new CreateHostRequest();
    request.setName("web-01");
    request.setIp("192.168.1.10");
    request.setPort(22);
    request.setAnsibleUser("ansible");
    request.setAnsibleSshPass("plaintext");
    request.setAnsibleBecome(false);

    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));
    when(encryptionService.encrypt("plaintext")).thenReturn("encrypted");
    when(hostRepository.save(any(Host.class))).thenReturn(testHost);

    HostResponse response = hostService.createHost(1L, request, 10L);

    assertThat(response.getName()).isEqualTo("web-01");
    verify(encryptionService).encrypt("plaintext");
  }

  @Test
  void getHostsByHostGroup_success() {
    when(hostRepository.findAllByHostGroupId(1L)).thenReturn(List.of(testHost));

    List<HostResponse> hosts = hostService.getHostsByHostGroup(1L, 10L);

    assertThat(hosts).hasSize(1);
    assertThat(hosts.get(0).getName()).isEqualTo("web-01");
  }

  @Test
  void getHost_success() {
    when(hostRepository.findById(1L)).thenReturn(Optional.of(testHost));
    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));

    HostResponse response = hostService.getHost(1L, 10L);

    assertThat(response.getName()).isEqualTo("web-01");
    assertThat(response.getAnsibleSshPass()).isEqualTo("****");
  }

  @Test
  void updateHost_success() {
    UpdateHostRequest request = new UpdateHostRequest();
    request.setName("web-02");

    when(hostRepository.findById(1L)).thenReturn(Optional.of(testHost));
    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));
    when(hostRepository.save(any(Host.class))).thenReturn(testHost);

    hostService.updateHost(1L, request, 10L);

    verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
    assertThat(testHost.getName()).isEqualTo("web-02");
  }

  @Test
  void deleteHost_success() {
    when(hostRepository.findById(1L)).thenReturn(Optional.of(testHost));
    when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));

    hostService.deleteHost(1L, 10L);

    verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
    verify(hostRepository).delete(testHost);
  }
}
```

- [ ] **Step 7: Run tests — expect failure**

Run: `cd backend && mvn test -Dtest="HostServiceTest" -q`
Expected: COMPILATION ERROR (HostService does not exist)

- [ ] **Step 8: Create `HostService.java`**

```java
package com.ansible.host.service;

import com.ansible.common.EncryptionService;
import com.ansible.host.dto.CreateHostRequest;
import com.ansible.host.dto.HostResponse;
import com.ansible.host.dto.UpdateHostRequest;
import com.ansible.host.entity.Host;
import com.ansible.host.entity.HostGroup;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.host.repository.HostRepository;
import com.ansible.security.ProjectAccessChecker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class HostService {

  private final HostRepository hostRepository;
  private final HostGroupRepository hostGroupRepository;
  private final ProjectAccessChecker accessChecker;
  private final EncryptionService encryptionService;

  @Transactional
  public HostResponse createHost(Long hostGroupId, CreateHostRequest request, Long currentUserId) {
    HostGroup hostGroup =
        hostGroupRepository
            .findById(hostGroupId)
            .orElseThrow(() -> new IllegalArgumentException("Host group not found"));
    accessChecker.checkMembership(hostGroup.getProjectId(), currentUserId);
    Host host = new Host();
    host.setHostGroupId(hostGroupId);
    host.setName(request.getName());
    host.setIp(request.getIp());
    host.setPort(request.getPort() != null ? request.getPort() : 22);
    host.setAnsibleUser(request.getAnsibleUser());
    if (StringUtils.hasText(request.getAnsibleSshPass())) {
      host.setAnsibleSshPass(encryptionService.encrypt(request.getAnsibleSshPass()));
    }
    if (StringUtils.hasText(request.getAnsibleSshPrivateKeyFile())) {
      host.setAnsibleSshPrivateKeyFile(
          encryptionService.encrypt(request.getAnsibleSshPrivateKeyFile()));
    }
    host.setAnsibleBecome(request.getAnsibleBecome() != null ? request.getAnsibleBecome() : false);
    host.setCreatedBy(currentUserId);
    Host saved = hostRepository.save(host);
    return new HostResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<HostResponse> getHostsByHostGroup(Long hostGroupId, Long currentUserId) {
    HostGroup hostGroup =
        hostGroupRepository
            .findById(hostGroupId)
            .orElseThrow(() -> new IllegalArgumentException("Host group not found"));
    accessChecker.checkMembership(hostGroup.getProjectId(), currentUserId);
    return hostRepository.findAllByHostGroupId(hostGroupId).stream()
        .map(HostResponse::new)
        .toList();
  }

  @Transactional(readOnly = true)
  public HostResponse getHost(Long hostId, Long currentUserId) {
    Host host =
        hostRepository
            .findById(hostId)
            .orElseThrow(() -> new IllegalArgumentException("Host not found"));
    HostGroup hostGroup =
        hostGroupRepository
            .findById(host.getHostGroupId())
            .orElseThrow(() -> new IllegalArgumentException("Host group not found"));
    accessChecker.checkMembership(hostGroup.getProjectId(), currentUserId);
    return new HostResponse(host);
  }

  @Transactional
  public HostResponse updateHost(Long hostId, UpdateHostRequest request, Long currentUserId) {
    Host host =
        hostRepository
            .findById(hostId)
            .orElseThrow(() -> new IllegalArgumentException("Host not found"));
    HostGroup hostGroup =
        hostGroupRepository
            .findById(host.getHostGroupId())
            .orElseThrow(() -> new IllegalArgumentException("Host group not found"));
    accessChecker.checkOwnerOrAdmin(hostGroup.getProjectId(), host.getCreatedBy(), currentUserId);
    if (StringUtils.hasText(request.getName())) {
      host.setName(request.getName());
    }
    if (StringUtils.hasText(request.getIp())) {
      host.setIp(request.getIp());
    }
    if (request.getPort() != null) {
      host.setPort(request.getPort());
    }
    if (request.getAnsibleUser() != null) {
      host.setAnsibleUser(request.getAnsibleUser());
    }
    if (StringUtils.hasText(request.getAnsibleSshPass())
        && !EncryptionService.mask().equals(request.getAnsibleSshPass())) {
      host.setAnsibleSshPass(encryptionService.encrypt(request.getAnsibleSshPass()));
    }
    if (StringUtils.hasText(request.getAnsibleSshPrivateKeyFile())
        && !EncryptionService.mask().equals(request.getAnsibleSshPrivateKeyFile())) {
      host.setAnsibleSshPrivateKeyFile(
          encryptionService.encrypt(request.getAnsibleSshPrivateKeyFile()));
    }
    if (request.getAnsibleBecome() != null) {
      host.setAnsibleBecome(request.getAnsibleBecome());
    }
    Host saved = hostRepository.save(host);
    return new HostResponse(saved);
  }

  @Transactional
  public void deleteHost(Long hostId, Long currentUserId) {
    Host host =
        hostRepository
            .findById(hostId)
            .orElseThrow(() -> new IllegalArgumentException("Host not found"));
    HostGroup hostGroup =
        hostGroupRepository
            .findById(host.getHostGroupId())
            .orElseThrow(() -> new IllegalArgumentException("Host group not found"));
    accessChecker.checkOwnerOrAdmin(hostGroup.getProjectId(), host.getCreatedBy(), currentUserId);
    hostRepository.delete(host);
  }
}
```

- [ ] **Step 9: Run tests**

Run: `cd backend && mvn test -Dtest="HostServiceTest" -q`
Expected: Tests run: 6, Failures: 0, Errors: 0

- [ ] **Step 10: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 11: Commit**

```bash
git add backend/src/main/java/com/ansible/host/entity/Host.java
git add backend/src/main/java/com/ansible/host/repository/HostRepository.java
git add backend/src/main/java/com/ansible/host/dto/CreateHostRequest.java
git add backend/src/main/java/com/ansible/host/dto/UpdateHostRequest.java
git add backend/src/main/java/com/ansible/host/dto/HostResponse.java
git add backend/src/main/java/com/ansible/host/service/HostService.java
git add backend/src/test/java/com/ansible/host/service/HostServiceTest.java
git commit -m "feat: add Host entity, repository, DTOs, service and unit tests"
```

---

## Task 5: HostGroup + Host Controllers with Integration Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/host/controller/HostGroupController.java`
- Create: `backend/src/main/java/com/ansible/host/controller/HostController.java`
- Create: `backend/src/test/java/com/ansible/host/controller/HostGroupControllerTest.java`
- Create: `backend/src/test/java/com/ansible/host/controller/HostControllerTest.java`

- [ ] **Step 1: Create `HostGroupController.java`**

```java
package com.ansible.host.controller;

import com.ansible.common.Result;
import com.ansible.host.dto.CreateHostGroupRequest;
import com.ansible.host.dto.HostGroupResponse;
import com.ansible.host.dto.UpdateHostGroupRequest;
import com.ansible.host.service.HostGroupService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HostGroupController {

  private final HostGroupService hostGroupService;

  @PostMapping("/projects/{projectId}/host-groups")
  public Result<HostGroupResponse> createHostGroup(
      @PathVariable Long projectId,
      @Valid @RequestBody CreateHostGroupRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(hostGroupService.createHostGroup(projectId, request, currentUserId));
  }

  @GetMapping("/projects/{projectId}/host-groups")
  public Result<List<HostGroupResponse>> getHostGroups(
      @PathVariable Long projectId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(hostGroupService.getHostGroupsByProject(projectId, currentUserId));
  }

  @GetMapping("/host-groups/{id}")
  public Result<HostGroupResponse> getHostGroup(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(hostGroupService.getHostGroup(id, currentUserId));
  }

  @PutMapping("/host-groups/{id}")
  public Result<HostGroupResponse> updateHostGroup(
      @PathVariable Long id,
      @Valid @RequestBody UpdateHostGroupRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(hostGroupService.updateHostGroup(id, request, currentUserId));
  }

  @DeleteMapping("/host-groups/{id}")
  public Result<Void> deleteHostGroup(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    hostGroupService.deleteHostGroup(id, currentUserId);
    return Result.success();
  }
}
```

- [ ] **Step 2: Create `HostController.java`**

```java
package com.ansible.host.controller;

import com.ansible.common.Result;
import com.ansible.host.dto.CreateHostRequest;
import com.ansible.host.dto.HostResponse;
import com.ansible.host.dto.UpdateHostRequest;
import com.ansible.host.service.HostService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HostController {

  private final HostService hostService;

  @PostMapping("/host-groups/{hgId}/hosts")
  public Result<HostResponse> createHost(
      @PathVariable Long hgId,
      @Valid @RequestBody CreateHostRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(hostService.createHost(hgId, request, currentUserId));
  }

  @GetMapping("/host-groups/{hgId}/hosts")
  public Result<List<HostResponse>> getHosts(
      @PathVariable Long hgId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(hostService.getHostsByHostGroup(hgId, currentUserId));
  }

  @GetMapping("/hosts/{id}")
  public Result<HostResponse> getHost(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(hostService.getHost(id, currentUserId));
  }

  @PutMapping("/hosts/{id}")
  public Result<HostResponse> updateHost(
      @PathVariable Long id,
      @Valid @RequestBody UpdateHostRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(hostService.updateHost(id, request, currentUserId));
  }

  @DeleteMapping("/hosts/{id}")
  public Result<Void> deleteHost(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    hostService.deleteHost(id, currentUserId);
    return Result.success();
  }
}
```

- [ ] **Step 3: Create `HostGroupControllerTest.java`**

```java
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

    // Alice creates a project
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
```

- [ ] **Step 4: Create `HostControllerTest.java`**

```java
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

    // Alice creates a project
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

    // Alice creates a host group
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
}
```

- [ ] **Step 5: Run integration tests**

Run: `cd backend && mvn test -Dtest="HostGroupControllerTest,HostControllerTest" -q`
Expected: Tests run: 9, Failures: 0, Errors: 0

- [ ] **Step 6: Verify full compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/ansible/host/controller/HostGroupController.java
git add backend/src/main/java/com/ansible/host/controller/HostController.java
git add backend/src/test/java/com/ansible/host/controller/HostGroupControllerTest.java
git add backend/src/test/java/com/ansible/host/controller/HostControllerTest.java
git commit -m "feat: add HostGroup and Host controllers with integration tests"
```

---

## Task 6: Role Backend — Entity, Repository, DTOs, Service + Unit Tests + Controller + Integration Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/role/entity/Role.java`
- Create: `backend/src/main/java/com/ansible/role/repository/RoleRepository.java`
- Create: `backend/src/main/java/com/ansible/role/dto/CreateRoleRequest.java`
- Create: `backend/src/main/java/com/ansible/role/dto/UpdateRoleRequest.java`
- Create: `backend/src/main/java/com/ansible/role/dto/RoleResponse.java`
- Create: `backend/src/main/java/com/ansible/role/service/RoleService.java`
- Create: `backend/src/main/java/com/ansible/role/service/RoleServiceTest.java`
- Create: `backend/src/main/java/com/ansible/role/controller/RoleController.java`
- Create: `backend/src/main/java/com/ansible/role/controller/RoleControllerTest.java`

- [ ] **Step 1: Create directory structure**

```bash
mkdir -p backend/src/main/java/com/ansible/role/entity
mkdir -p backend/src/main/java/com/ansible/role/repository
mkdir -p backend/src/main/java/com/ansible/role/dto
mkdir -p backend/src/main/java/com/ansible/role/service
mkdir -p backend/src/main/java/com/ansible/role/controller
mkdir -p backend/src/test/java/com/ansible/role/service
mkdir -p backend/src/test/java/com/ansible/role/controller
```

- [ ] **Step 2: Create `Role.java`**

```java
package com.ansible.role.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role extends BaseEntity {

  @Column(nullable = false)
  private Long projectId;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 500)
  private String description;
}
```

- [ ] **Step 3: Create `RoleRepository.java`**

```java
package com.ansible.role.repository;

import com.ansible.role.entity.Role;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

  List<Role> findAllByProjectId(Long projectId);
}
```

- [ ] **Step 4: Create `CreateRoleRequest.java`**

```java
package com.ansible.role.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRoleRequest {

  @NotBlank(message = "Role name is required")
  @Size(max = 100, message = "Name must not exceed 100 characters")
  private String name;

  @Size(max = 500, message = "Description must not exceed 500 characters")
  private String description;
}
```

- [ ] **Step 5: Create `UpdateRoleRequest.java`**

```java
package com.ansible.role.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoleRequest {

  @Size(max = 100, message = "Name must not exceed 100 characters")
  private String name;

  @Size(max = 500, message = "Description must not exceed 500 characters")
  private String description;
}
```

- [ ] **Step 6: Create `RoleResponse.java`**

```java
package com.ansible.role.dto;

import com.ansible.role.entity.Role;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class RoleResponse {

  private final Long id;
  private final Long projectId;
  private final String name;
  private final String description;
  private final Long createdBy;
  private final LocalDateTime createdAt;

  @JsonCreator
  public RoleResponse(
      @JsonProperty("id") Long id,
      @JsonProperty("projectId") Long projectId,
      @JsonProperty("name") String name,
      @JsonProperty("description") String description,
      @JsonProperty("createdBy") Long createdBy,
      @JsonProperty("createdAt") LocalDateTime createdAt) {
    this.id = id;
    this.projectId = projectId;
    this.name = name;
    this.description = description;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
  }

  public RoleResponse(Role role) {
    this.id = role.getId();
    this.projectId = role.getProjectId();
    this.name = role.getName();
    this.description = role.getDescription();
    this.createdBy = role.getCreatedBy();
    this.createdAt = role.getCreatedAt();
  }
}
```

- [ ] **Step 7: Write `RoleServiceTest.java`**

```java
package com.ansible.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ansible.role.dto.CreateRoleRequest;
import com.ansible.role.dto.RoleResponse;
import com.ansible.role.dto.UpdateRoleRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.repository.RoleRepository;
import com.ansible.security.ProjectAccessChecker;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

  @Mock private RoleRepository roleRepository;
  @Mock private ProjectAccessChecker accessChecker;
  @InjectMocks private RoleService roleService;

  private Role testRole;

  @BeforeEach
  void setUp() {
    testRole = new Role();
    ReflectionTestUtils.setField(testRole, "id", 1L);
    testRole.setProjectId(10L);
    testRole.setName("nginx");
    testRole.setDescription("Install and configure nginx");
    testRole.setCreatedBy(10L);
    ReflectionTestUtils.setField(testRole, "createdAt", LocalDateTime.now());
    ReflectionTestUtils.setField(testRole, "updatedAt", LocalDateTime.now());
  }

  @Test
  void createRole_success() {
    CreateRoleRequest request = new CreateRoleRequest();
    request.setName("nginx");
    request.setDescription("Install nginx");

    when(roleRepository.save(any(Role.class))).thenReturn(testRole);

    RoleResponse response = roleService.createRole(10L, request, 10L);

    assertThat(response.getName()).isEqualTo("nginx");
    verify(roleRepository).save(any(Role.class));
  }

  @Test
  void getRolesByProject_success() {
    when(roleRepository.findAllByProjectId(10L)).thenReturn(List.of(testRole));

    List<RoleResponse> roles = roleService.getRolesByProject(10L, 10L);

    assertThat(roles).hasSize(1);
    assertThat(roles.get(0).getName()).isEqualTo("nginx");
  }

  @Test
  void getRole_success() {
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    RoleResponse response = roleService.getRole(1L, 10L);

    assertThat(response.getName()).isEqualTo("nginx");
  }

  @Test
  void updateRole_success() {
    UpdateRoleRequest request = new UpdateRoleRequest();
    request.setName("apache");

    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));
    when(roleRepository.save(any(Role.class))).thenReturn(testRole);

    roleService.updateRole(1L, request, 10L);

    verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
  }

  @Test
  void deleteRole_success() {
    when(roleRepository.findById(1L)).thenReturn(Optional.of(testRole));

    roleService.deleteRole(1L, 10L);

    verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
    verify(roleRepository).delete(testRole);
  }
}
```

- [ ] **Step 8: Run tests — expect failure**

Run: `cd backend && mvn test -Dtest="RoleServiceTest" -q`
Expected: COMPILATION ERROR

- [ ] **Step 9: Create `RoleService.java`**

```java
package com.ansible.role.service;

import com.ansible.role.dto.CreateRoleRequest;
import com.ansible.role.dto.RoleResponse;
import com.ansible.role.dto.UpdateRoleRequest;
import com.ansible.role.entity.Role;
import com.ansible.role.repository.RoleRepository;
import com.ansible.security.ProjectAccessChecker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class RoleService {

  private final RoleRepository roleRepository;
  private final ProjectAccessChecker accessChecker;

  @Transactional
  public RoleResponse createRole(Long projectId, CreateRoleRequest request, Long currentUserId) {
    accessChecker.checkMembership(projectId, currentUserId);
    Role role = new Role();
    role.setProjectId(projectId);
    role.setName(request.getName());
    role.setDescription(request.getDescription());
    role.setCreatedBy(currentUserId);
    Role saved = roleRepository.save(role);
    return new RoleResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<RoleResponse> getRolesByProject(Long projectId, Long currentUserId) {
    accessChecker.checkMembership(projectId, currentUserId);
    return roleRepository.findAllByProjectId(projectId).stream()
        .map(RoleResponse::new)
        .toList();
  }

  @Transactional(readOnly = true)
  public RoleResponse getRole(Long roleId, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkMembership(role.getProjectId(), currentUserId);
    return new RoleResponse(role);
  }

  @Transactional
  public RoleResponse updateRole(Long roleId, UpdateRoleRequest request, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), role.getCreatedBy(), currentUserId);
    if (StringUtils.hasText(request.getName())) {
      role.setName(request.getName());
    }
    if (request.getDescription() != null) {
      role.setDescription(request.getDescription());
    }
    Role saved = roleRepository.save(role);
    return new RoleResponse(saved);
  }

  @Transactional
  public void deleteRole(Long roleId, Long currentUserId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("Role not found"));
    accessChecker.checkOwnerOrAdmin(role.getProjectId(), role.getCreatedBy(), currentUserId);
    roleRepository.delete(role);
  }
}
```

- [ ] **Step 10: Run tests**

Run: `cd backend && mvn test -Dtest="RoleServiceTest" -q`
Expected: Tests run: 6, Failures: 0, Errors: 0

- [ ] **Step 11: Create `RoleController.java`**

```java
package com.ansible.role.controller;

import com.ansible.common.Result;
import com.ansible.role.dto.CreateRoleRequest;
import com.ansible.role.dto.RoleResponse;
import com.ansible.role.dto.UpdateRoleRequest;
import com.ansible.role.service.RoleService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RoleController {

  private final RoleService roleService;

  @PostMapping("/projects/{projectId}/roles")
  public Result<RoleResponse> createRole(
      @PathVariable Long projectId,
      @Valid @RequestBody CreateRoleRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleService.createRole(projectId, request, currentUserId));
  }

  @GetMapping("/projects/{projectId}/roles")
  public Result<List<RoleResponse>> getRoles(
      @PathVariable Long projectId, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleService.getRolesByProject(projectId, currentUserId));
  }

  @GetMapping("/roles/{id}")
  public Result<RoleResponse> getRole(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleService.getRole(id, currentUserId));
  }

  @PutMapping("/roles/{id}")
  public Result<RoleResponse> updateRole(
      @PathVariable Long id,
      @Valid @RequestBody UpdateRoleRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    return Result.success(roleService.updateRole(id, request, currentUserId));
  }

  @DeleteMapping("/roles/{id}")
  public Result<Void> deleteRole(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Long currentUserId = Long.valueOf(userDetails.getUsername());
    roleService.deleteRole(id, currentUserId);
    return Result.success();
  }
}
```

- [ ] **Step 12: Create `RoleControllerTest.java`**

```java
package com.ansible.role.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.AbstractIntegrationTest;
import com.ansible.common.Result;
import com.ansible.project.dto.CreateProjectRequest;
import com.ansible.project.dto.ProjectResponse;
import com.ansible.role.dto.CreateRoleRequest;
import com.ansible.role.dto.RoleResponse;
import com.ansible.role.dto.UpdateRoleRequest;
import com.ansible.role.repository.RoleRepository;
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

class RoleControllerTest extends AbstractIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private ProjectMemberRepository projectMemberRepository;
  @Autowired private RoleRepository roleRepository;

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

    CreateProjectRequest projReq = new CreateProjectRequest();
    projReq.setName("Test Project");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    ResponseEntity<Result<ProjectResponse>> projResp =
        restTemplate.exchange(
            "/api/projects",
            HttpMethod.POST,
            new HttpEntity<>(projReq, headers),
            new ParameterizedTypeReference<>() {});
    projectId = projResp.getBody().getData().getId();
  }

  @AfterEach
  void tearDown() {
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

  private Long createRole(String name) {
    CreateRoleRequest req = new CreateRoleRequest();
    req.setName(name);
    req.setDescription("desc");
    ResponseEntity<Result<RoleResponse>> resp =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/roles",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});
    return resp.getBody().getData().getId();
  }

  @Test
  void createRole_success() {
    CreateRoleRequest req = new CreateRoleRequest();
    req.setName("nginx");
    req.setDescription("Install nginx");

    ResponseEntity<Result<RoleResponse>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/roles",
            HttpMethod.POST,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("nginx");
  }

  @Test
  void getRoles_returns_list() {
    createRole("nginx");
    createRole("apache");

    ResponseEntity<Result<java.util.List<RoleResponse>>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/roles",
            HttpMethod.GET,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData()).hasSize(2);
  }

  @Test
  void updateRole_success() {
    Long roleId = createRole("nginx");

    UpdateRoleRequest req = new UpdateRoleRequest();
    req.setName("apache");

    ResponseEntity<Result<RoleResponse>> response =
        restTemplate.exchange(
            "/api/roles/" + roleId,
            HttpMethod.PUT,
            new HttpEntity<>(req, authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getName()).isEqualTo("apache");
  }

  @Test
  void deleteRole_success() {
    Long roleId = createRole("nginx");

    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/roles/" + roleId,
            HttpMethod.DELETE,
            new HttpEntity<>(authHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(roleRepository.findById(roleId)).isEmpty();
  }
}
```

- [ ] **Step 13: Run integration tests**

Run: `cd backend && mvn test -Dtest="RoleControllerTest" -q`
Expected: Tests run: 4, Failures: 0, Errors: 0

- [ ] **Step 14: Verify full compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 15: Commit**

```bash
git add backend/src/main/java/com/ansible/role/
git add backend/src/test/java/com/ansible/role/
git commit -m "feat: add Role entity, repository, DTOs, service, controller and tests"
```

---

## Task 7: Code Quality Scans + Fix

**Files:**
- Modify: any backend files with violations

- [ ] **Step 1: Run Spotless auto-format**

Run: `cd backend && mvn spotless:apply -q`

- [ ] **Step 2: Run Checkstyle**

Run: `cd backend && mvn checkstyle:check -q`
Fix any violations found.

- [ ] **Step 3: Run PMD**

Run: `cd backend && mvn pmd:check -q`
Fix any violations found.

- [ ] **Step 4: Run SpotBugs**

Run: `cd backend && mvn compile spotbugs:check -q`
Fix any violations found.

- [ ] **Step 5: Run all tests**

Run: `cd backend && mvn test -q`
Expected: All tests pass.

- [ ] **Step 6: Commit if fixes were needed**

```bash
git add -A
git commit -m "fix: resolve code quality scan violations for host and role modules"
```

---

## Task 8: Frontend — Host Types + API Layer

**Files:**
- Create: `frontend/src/types/entity/Host.ts`
- Create: `frontend/src/types/entity/Role.ts`
- Create: `frontend/src/api/host.ts`
- Create: `frontend/src/api/role.ts`

- [ ] **Step 1: Create `frontend/src/types/entity/Host.ts`**

```typescript
export interface HostGroup {
  id: number;
  projectId: number;
  name: string;
  description: string;
  createdBy: number;
  createdAt: string;
}

export interface Host {
  id: number;
  hostGroupId: number;
  name: string;
  ip: string;
  port: number;
  ansibleUser: string;
  ansibleSshPass: string;
  ansibleSshPrivateKeyFile: string;
  ansibleBecome: boolean;
  createdBy: number;
  createdAt: string;
}

export interface CreateHostGroupRequest {
  name: string;
  description?: string;
}

export interface UpdateHostGroupRequest {
  name?: string;
  description?: string;
}

export interface CreateHostRequest {
  name: string;
  ip: string;
  port?: number;
  ansibleUser?: string;
  ansibleSshPass?: string;
  ansibleSshPrivateKeyFile?: string;
  ansibleBecome?: boolean;
}

export interface UpdateHostRequest {
  name?: string;
  ip?: string;
  port?: number;
  ansibleUser?: string;
  ansibleSshPass?: string;
  ansibleSshPrivateKeyFile?: string;
  ansibleBecome?: boolean;
}
```

- [ ] **Step 2: Create `frontend/src/types/entity/Role.ts`**

```typescript
export interface Role {
  id: number;
  projectId: number;
  name: string;
  description: string;
  createdBy: number;
  createdAt: string;
}

export interface CreateRoleRequest {
  name: string;
  description?: string;
}

export interface UpdateRoleRequest {
  name?: string;
  description?: string;
}
```

- [ ] **Step 3: Create `frontend/src/api/host.ts`**

```typescript
import request from './request';
import type {
  HostGroup,
  Host,
  CreateHostGroupRequest,
  UpdateHostGroupRequest,
  CreateHostRequest,
  UpdateHostRequest,
} from '../types/entity/Host';

// HostGroup APIs
export async function createHostGroup(
  projectId: number,
  data: CreateHostGroupRequest
): Promise<HostGroup> {
  const res = await request.post<HostGroup>(
    `/api/projects/${projectId}/host-groups`,
    data
  );
  return res.data;
}

export async function getHostGroups(projectId: number): Promise<HostGroup[]> {
  const res = await request.get<HostGroup[]>(
    `/api/projects/${projectId}/host-groups`
  );
  return res.data;
}

export async function getHostGroup(id: number): Promise<HostGroup> {
  const res = await request.get<HostGroup>(`/api/host-groups/${id}`);
  return res.data;
}

export async function updateHostGroup(
  id: number,
  data: UpdateHostGroupRequest
): Promise<HostGroup> {
  const res = await request.put<HostGroup>(`/api/host-groups/${id}`, data);
  return res.data;
}

export async function deleteHostGroup(id: number): Promise<void> {
  await request.delete(`/api/host-groups/${id}`);
}

// Host APIs
export async function createHost(
  hostGroupId: number,
  data: CreateHostRequest
): Promise<Host> {
  const res = await request.post<Host>(
    `/api/host-groups/${hostGroupId}/hosts`,
    data
  );
  return res.data;
}

export async function getHosts(hostGroupId: number): Promise<Host[]> {
  const res = await request.get<Host[]>(
    `/api/host-groups/${hostGroupId}/hosts`
  );
  return res.data;
}

export async function getHost(id: number): Promise<Host> {
  const res = await request.get<Host>(`/api/hosts/${id}`);
  return res.data;
}

export async function updateHost(
  id: number,
  data: UpdateHostRequest
): Promise<Host> {
  const res = await request.put<Host>(`/api/hosts/${id}`, data);
  return res.data;
}

export async function deleteHost(id: number): Promise<void> {
  await request.delete(`/api/hosts/${id}`);
}
```

- [ ] **Step 4: Create `frontend/src/api/role.ts`**

```typescript
import request from './request';
import type { Role, CreateRoleRequest, UpdateRoleRequest } from '../types/entity/Role';

export async function createRole(
  projectId: number,
  data: CreateRoleRequest
): Promise<Role> {
  const res = await request.post<Role>(`/api/projects/${projectId}/roles`, data);
  return res.data;
}

export async function getRoles(projectId: number): Promise<Role[]> {
  const res = await request.get<Role[]>(`/api/projects/${projectId}/roles`);
  return res.data;
}

export async function getRole(id: number): Promise<Role> {
  const res = await request.get<Role>(`/api/roles/${id}`);
  return res.data;
}

export async function updateRole(
  id: number,
  data: UpdateRoleRequest
): Promise<Role> {
  const res = await request.put<Role>(`/api/roles/${id}`, data);
  return res.data;
}

export async function deleteRole(id: number): Promise<void> {
  await request.delete(`/api/roles/${id}`);
}
```

- [ ] **Step 5: Verify TypeScript compilation**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 6: Commit**

```bash
git add frontend/src/types/entity/Host.ts
git add frontend/src/types/entity/Role.ts
git add frontend/src/api/host.ts
git add frontend/src/api/role.ts
git commit -m "feat: add frontend Host and Role types and API layer"
```

---

## Task 9: Frontend — HostGroupManager Page

**Files:**
- Create: `frontend/src/pages/host/HostGroupManager.tsx`

- [ ] **Step 1: Create `HostGroupManager.tsx`**

```tsx
import { useEffect, useState, useCallback } from 'react';
import {
  Button,
  Card,
  Divider,
  Empty,
  Form,
  Input,
  List,
  message,
  Modal,
  Popconfirm,
  Space,
  Table,
  Tag,
  Tooltip,
} from 'antd';
import {
  DatabaseOutlined,
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  SaveOutlined,
} from '@ant-design/icons';
import { useParams } from 'react-router-dom';
import type {
  HostGroup,
  Host,
  CreateHostGroupRequest,
  UpdateHostGroupRequest,
  CreateHostRequest,
  UpdateHostRequest,
} from '../../types/entity/Host';
import {
  createHostGroup,
  deleteHostGroup,
  getHostGroups,
  updateHostGroup,
  createHost,
  deleteHost,
  getHosts,
  updateHost,
} from '../../api/host';

const { TextArea } = Input;

export default function HostGroupManager() {
  const { id: projectId } = useParams<{ id: string }>();
  const pid = Number(projectId);

  const [hostGroups, setHostGroups] = useState<HostGroup[]>([]);
  const [selectedHostGroup, setSelectedHostGroup] = useState<HostGroup | null>(null);
  const [hosts, setHosts] = useState<Host[]>([]);
  const [loading, setLoading] = useState(false);
  const [hgModalOpen, setHgModalOpen] = useState(false);
  const [hostModalOpen, setHostModalOpen] = useState(false);
  const [editingHg, setEditingHg] = useState<HostGroup | null>(null);
  const [editingHost, setEditingHost] = useState<Host | null>(null);
  const [hgForm] = Form.useForm<CreateHostGroupRequest>();
  const [hostForm] = Form.useForm<CreateHostRequest>();

  const fetchHostGroups = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getHostGroups(pid);
      setHostGroups(data);
    } finally {
      setLoading(false);
    }
  }, [pid]);

  const fetchHosts = useCallback(async (hgId: number) => {
    setLoading(true);
    try {
      setHosts(await getHosts(hgId));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchHostGroups();
  }, [fetchHostGroups]);

  const handleSelectHostGroup = (hg: HostGroup) => {
    setSelectedHostGroup(hg);
    fetchHosts(hg.id);
  };

  const handleCreateHg = async () => {
    const values = await hgForm.validateFields();
    await createHostGroup(pid, values);
    message.success('主机组创建成功');
    setHgModalOpen(false);
    hgForm.resetFields();
    fetchHostGroups();
  };

  const handleUpdateHg = async () => {
    if (!editingHg) return;
    const values = await hgForm.validateFields();
    await updateHostGroup(editingHg.id, values);
    message.success('主机组更新成功');
    setHgModalOpen(false);
    setEditingHg(null);
    hgForm.resetFields();
    fetchHostGroups();
  };

  const handleDeleteHg = async (hgId: number) => {
    await deleteHostGroup(hgId);
    message.success('主机组已删除');
    if (selectedHostGroup?.id === hgId) {
      setSelectedHostGroup(null);
      setHosts([]);
    }
    fetchHostGroups();
  };

  const openHgModal = (hg?: HostGroup) => {
    if (hg) {
      setEditingHg(hg);
      hgForm.setFieldsValue({ name: hg.name, description: hg.description });
    } else {
      setEditingHg(null);
      hgForm.resetFields();
    }
    setHgModalOpen(true);
  };

  const handleCreateHost = async () => {
    if (!selectedHostGroup) return;
    const values = await hostForm.validateFields();
    await createHost(selectedHostGroup.id, values);
    message.success('主机创建成功');
    setHostModalOpen(false);
    hostForm.resetFields();
    fetchHosts(selectedHostGroup.id);
  };

  const handleUpdateHost = async () => {
    if (!editingHost) return;
    const values = await hostForm.validateFields();
    await updateHost(editingHost.id, values);
    message.success('主机更新成功');
    setHostModalOpen(false);
    setEditingHost(null);
    hostForm.resetFields();
    if (selectedHostGroup) fetchHosts(selectedHostGroup.id);
  };

  const handleDeleteHost = async (hostId: number) => {
    await deleteHost(hostId);
    message.success('主机已删除');
    if (selectedHostGroup) fetchHosts(selectedHostGroup.id);
  };

  const openHostModal = (host?: Host) => {
    if (host) {
      setEditingHost(host);
      hostForm.setFieldsValue({
        name: host.name,
        ip: host.ip,
        port: host.port,
        ansibleUser: host.ansibleUser,
        ansibleBecome: host.ansibleBecome,
      });
    } else {
      setEditingHost(null);
      hostForm.resetFields();
    }
    setHostModalOpen(true);
  };

  const hostColumns = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: 'IP', dataIndex: 'ip', key: 'ip' },
    { title: '端口', dataIndex: 'port', key: 'port' },
    { title: 'SSH用户', dataIndex: 'ansibleUser', key: 'ansibleUser' },
    {
      title: '提权',
      dataIndex: 'ansibleBecome',
      key: 'ansibleBecome',
      render: (v: boolean) => <Tag color={v ? 'green' : 'default'}>{v ? '是' : '否'}</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: Host) => (
        <Space>
          <Tooltip title="编辑">
            <Button type="text" size="small" icon={<EditOutlined />} onClick={() => openHostModal(record)} />
          </Tooltip>
          <Popconfirm title="确认删除此主机？" onConfirm={() => handleDeleteHost(record.id)}>
            <Button type="text" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ display: 'flex', gap: 16, height: 'calc(100vh - 180px)' }}>
      {/* Left: HostGroup list */}
      <Card
        title="主机组"
        extra={
          <Button type="primary" size="small" icon={<PlusOutlined />} onClick={() => openHgModal()}>
            新建
          </Button>
        }
        style={{ width: 320, overflow: 'auto' }}
        bodyStyle={{ padding: 0 }}
      >
        <List
          loading={loading}
          dataSource={hostGroups}
          locale={{ emptyText: <Empty description="暂无主机组" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
          renderItem={(hg) => (
            <List.Item
              style={{
                padding: '12px 16px',
                cursor: 'pointer',
                background: selectedHostGroup?.id === hg.id ? '#f0f5ff' : undefined,
                borderLeft: selectedHostGroup?.id === hg.id ? '2px solid #1677ff' : '2px solid transparent',
              }}
              onClick={() => handleSelectHostGroup(hg)}
            >
              <List.Item.Meta
                title={
                  <Space>
                    <DatabaseOutlined />
                    <span>{hg.name}</span>
                  </Space>
                }
                description={hg.description || '无描述'}
              />
              <Space onClick={(e) => e.stopPropagation()}>
                <Button type="text" size="small" icon={<EditOutlined />} onClick={() => openHgModal(hg)} />
                <Popconfirm title="确认删除此主机组？" onConfirm={() => handleDeleteHg(hg.id)}>
                  <Button type="text" size="small" danger icon={<DeleteOutlined />} />
                </Popconfirm>
              </Space>
            </List.Item>
          )}
        />
      </Card>

      {/* Right: Host list */}
      <Card
        title={selectedHostGroup ? `主机 — ${selectedHostGroup.name}` : '请选择主机组'}
        extra={
          selectedHostGroup && (
            <Button type="primary" size="small" icon={<PlusOutlined />} onClick={() => openHostModal()}>
              新建主机
            </Button>
          )
        }
        style={{ flex: 1, overflow: 'auto' }}
        bodyStyle={{ padding: 0 }}
      >
        {selectedHostGroup ? (
          <Table
            columns={hostColumns}
            dataSource={hosts}
            rowKey="id"
            loading={loading}
            pagination={false}
            locale={{ emptyText: <Empty description="该主机组下暂无主机" image={Empty.PRESENTED_IMAGE_SIMPLE} /> }}
          />
        ) : (
          <Empty description="请从左侧选择一个主机组" style={{ marginTop: 80 }} />
        )}
      </Card>

      {/* HostGroup Modal */}
      <Modal
        title={editingHg ? '编辑主机组' : '新建主机组'}
        open={hgModalOpen}
        onOk={editingHg ? handleUpdateHg : handleCreateHg}
        onCancel={() => { setHgModalOpen(false); hgForm.resetFields(); }}
      >
        <Form form={hgForm} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Host Modal */}
      <Modal
        title={editingHost ? '编辑主机' : '新建主机'}
        open={hostModalOpen}
        onOk={editingHost ? handleUpdateHost : handleCreateHost}
        onCancel={() => { setHostModalOpen(false); hostForm.resetFields(); }}
      >
        <Form form={hostForm} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="ip" label="IP" rules={[{ required: true, message: '请输入IP' }]}>
            <Input maxLength={45} placeholder="192.168.1.10" />
          </Form.Item>
          <Form.Item name="port" label="端口" initialValue={22}>
            <Input type="number" />
          </Form.Item>
          <Form.Item name="ansibleUser" label="SSH用户">
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="ansibleSshPass" label="SSH密码（加密存储）">
            <Input.Password maxLength={500} placeholder="留空则不更新" />
          </Form.Item>
          <Form.Item name="ansibleBecome" label="提权" valuePropName="checked" initialValue={false}>
            <Input type="checkbox" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
```

- [ ] **Step 2: Verify TypeScript compilation**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/host/HostGroupManager.tsx
git commit -m "feat: add HostGroupManager page — single-page Master-Detail layout"
```

---

## Task 10: Frontend — RoleList + RoleDetail Pages

**Files:**
- Create: `frontend/src/pages/role/RoleList.tsx`
- Create: `frontend/src/pages/role/RoleDetail.tsx`

- [ ] **Step 1: Create `RoleList.tsx`**

```tsx
import { useEffect, useState, useCallback } from 'react';
import { Button, Form, Input, message, Modal, Popconfirm, Space, Table, Tag } from 'antd';
import { DeleteOutlined, EditOutlined, PlusOutlined } from '@ant-design/icons';
import { useNavigate, useParams } from 'react-router-dom';
import type { Role, CreateRoleRequest, UpdateRoleRequest } from '../../types/entity/Role';
import { createRole, deleteRole, getRoles, updateRole } from '../../api/role';

export default function RoleList() {
  const { id: projectId } = useParams<{ id: string }>();
  const pid = Number(projectId);
  const navigate = useNavigate();

  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRole, setEditingRole] = useState<Role | null>(null);
  const [form] = Form.useForm<CreateRoleRequest>();

  const fetchRoles = useCallback(async () => {
    setLoading(true);
    try {
      setRoles(await getRoles(pid));
    } finally {
      setLoading(false);
    }
  }, [pid]);

  useEffect(() => {
    fetchRoles();
  }, [fetchRoles]);

  const handleCreate = async () => {
    const values = await form.validateFields();
    await createRole(pid, values);
    message.success('Role 创建成功');
    setModalOpen(false);
    form.resetFields();
    fetchRoles();
  };

  const handleUpdate = async () => {
    if (!editingRole) return;
    const values = await form.validateFields();
    await updateRole(editingRole.id, values);
    message.success('Role 更新成功');
    setModalOpen(false);
    setEditingRole(null);
    form.resetFields();
    fetchRoles();
  };

  const handleDelete = async (roleId: number) => {
    await deleteRole(roleId);
    message.success('Role 已删除');
    fetchRoles();
  };

  const openModal = (role?: Role) => {
    if (role) {
      setEditingRole(role);
      form.setFieldsValue({ name: role.name, description: role.description });
    } else {
      setEditingRole(null);
      form.resetFields();
    }
    setModalOpen(true);
  };

  const columns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      render: (name: string, record: Role) => (
        <a onClick={() => navigate(`/projects/${pid}/roles/${record.id}`)}>{name}</a>
      ),
    },
    { title: '描述', dataIndex: 'description', key: 'description' },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: Role) => (
        <Space>
          <Button type="text" size="small" icon={<EditOutlined />} onClick={() => openModal(record)} />
          <Popconfirm title="确认删除此 Role？" onConfirm={() => handleDelete(record.id)}>
            <Button type="text" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h2 style={{ margin: 0 }}>Roles</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>
          新建 Role
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={roles}
        rowKey="id"
        loading={loading}
        pagination={false}
      />

      <Modal
        title={editingRole ? '编辑 Role' : '新建 Role'}
        open={modalOpen}
        onOk={editingRole ? handleUpdate : handleCreate}
        onCancel={() => { setModalOpen(false); form.resetFields(); }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true, message: '请输入名称' }]}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} maxLength={500} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
```

- [ ] **Step 2: Create `RoleDetail.tsx`**

```tsx
import { useEffect, useState } from 'react';
import { Card, Col, Row, Skeleton, Tabs } from 'antd';
import { useParams } from 'react-router-dom';
import type { Role } from '../../types/entity/Role';
import { getRole } from '../../api/role';

const { TabPane } = Tabs;

export default function RoleDetail() {
  const { id: projectId, roleId } = useParams<{ id: string; roleId: string }>();
  const [role, setRole] = useState<Role | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (roleId) {
      getRole(Number(roleId)).then((r) => {
        setRole(r);
        setLoading(false);
      });
    }
  }, [roleId]);

  if (loading) {
    return <Skeleton active />;
  }

  return (
    <div>
      <Card title={role?.name} style={{ marginBottom: 16 }}>
        <p>{role?.description || '无描述'}</p>
      </Card>
      <Card bodyStyle={{ padding: 0 }}>
        <Tabs defaultActiveKey="tasks" style={{ padding: '0 24px' }}>
          <TabPane tab="Tasks" key="tasks">
            <div style={{ padding: '40px 0', textAlign: 'center', color: '#999' }}>
              即将推出
            </div>
          </TabPane>
          <TabPane tab="Handlers" key="handlers">
            <div style={{ padding: '40px 0', textAlign: 'center', color: '#999' }}>
              即将推出
            </div>
          </TabPane>
          <TabPane tab="Templates" key="templates">
            <div style={{ padding: '40px 0', textAlign: 'center', color: '#999' }}>
              即将推出
            </div>
          </TabPane>
          <TabPane tab="Files" key="files">
            <div style={{ padding: '40px 0', textAlign: 'center', color: '#999' }}>
              即将推出
            </div>
          </TabPane>
          <TabPane tab="Vars" key="vars">
            <div style={{ padding: '40px 0', textAlign: 'center', color: '#999' }}>
              即将推出
            </div>
          </TabPane>
          <TabPane tab="Defaults" key="defaults">
            <div style={{ padding: '40px 0', textAlign: 'center', color: '#999' }}>
              即将推出
            </div>
          </TabPane>
        </Tabs>
      </Card>
    </div>
  );
}
```

- [ ] **Step 3: Verify TypeScript compilation**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/role/RoleList.tsx
git add frontend/src/pages/role/RoleDetail.tsx
git commit -m "feat: add RoleList and RoleDetail pages with Tab skeleton"
```

---

## Task 11: Frontend — Update App.tsx with New Routes + Run ESLint

**Files:**
- Modify: `frontend/src/App.tsx`

- [ ] **Step 1: Read current `frontend/src/App.tsx`**

- [ ] **Step 2: Add imports and nested routes under ProjectLayout**

Add these imports:
```tsx
import HostGroupManager from './pages/host/HostGroupManager';
import RoleList from './pages/role/RoleList';
import RoleDetail from './pages/role/RoleDetail';
```

Add these nested routes inside `ProjectLayout`:
```tsx
<Route path="host-groups" element={<HostGroupManager />} />
<Route path="roles" element={<RoleList />} />
<Route path="roles/:roleId" element={<RoleDetail />} />
```

The updated ProjectLayout section should be:
```tsx
<Route path="projects/:id" element={<ProjectLayout />}>
  <Route path="settings" element={<ProjectSettings />} />
  <Route path="members" element={<MemberManagement />} />
  <Route path="host-groups" element={<HostGroupManager />} />
  <Route path="roles" element={<RoleList />} />
  <Route path="roles/:roleId" element={<RoleDetail />} />
</Route>
```

- [ ] **Step 3: Verify TypeScript compilation**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 4: Run ESLint**

Run: `cd frontend && npx eslint src/ --ext .ts,.tsx`
Expected: No errors (fix if found)

- [ ] **Step 5: Commit**

```bash
git add frontend/src/App.tsx
git commit -m "feat: add host-groups and roles routes to App"
```

---

## Task 12: i18n — Update UI Text to Simplified Chinese

**Files:**
- Modify: `frontend/src/pages/host/HostGroupManager.tsx`
- Modify: `frontend/src/pages/role/RoleList.tsx`
- Modify: `frontend/src/pages/role/RoleDetail.tsx`

- [ ] **Step 1: Update HostGroupManager Chinese text**

Replace these English strings with Chinese:
- `"HostGroup"` → `"主机组"`
- `"New"` → `"新建"`
- `"Create host group"` → `"创建主机组"`
- `"Edit host group"` → `"编辑主机组"`
- `"Name"` → `"名称"`
- `"Description"` → `"描述"`
- `"No host groups yet"` → `"暂无主机组"`
- `"No hosts yet"` → `"该主机组下暂无主机"`
- `"Select a host group from the left"` → `"请从左侧选择一个主机组"`
- `"Create host"` → `"新建主机"`
- `"Edit host"` → `"编辑主机"`
- `"IP"` → `"IP"`
- `"Port"` → `"端口"`
- `"SSH User"` → `"SSH用户"`
- `"SSH Password (encrypted)"` → `"SSH密码（加密存储）"`
- `"Privilege Escalation"` → `"提权"`
- `"Leave blank to skip"` → `"留空则不更新"`
- `"Host group deleted"` → `"主机组已删除"`
- `"Host deleted"` → `"主机已删除"`

- [ ] **Step 2: Update RoleList Chinese text**

- `"Roles"` → `"Roles"` (keep as-is, or "角色")
- `"New Role"` → `"新建 Role"`
- `"Edit Role"` → `"编辑 Role"`
- `"Role created"` → `"Role 创建成功"`
- `"Role updated"` → `"Role 更新成功"`
- `"Role deleted"` → `"Role 已删除"`
- `"Are you sure you want to delete this Role?"` → `"确认删除此 Role？"`

- [ ] **Step 3: Update RoleDetail Chinese text**

- `"Tasks"` → `"Tasks"` (or `"任务"`)
- `"Handlers"` → `"Handlers"` (or `"处理器"`)
- `"Templates"` → `"Templates"` (or `"模板"`)
- `"Files"` → `"Files"` (or `"文件"`)
- `"Vars"` → `"Vars"` (or `"变量"`)
- `"Defaults"` → `"Defaults"` (or `"默认变量"`)
- `"Coming soon"` → `"即将推出"`

- [ ] **Step 4: Verify TypeScript compilation**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "i18n: add Chinese text for host and role pages"
```
