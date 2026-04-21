# Playbook Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Playbook CRUD with Role association (ordered), HostGroup association, Tag association, and YAML generation/export. This is the capstone module that ties all other modules together.

**Architecture:** Playbook belongs to Project. Three join entities: PlaybookRole (with order), PlaybookHostGroup, PlaybookTag. Service assembles Playbook with associations and generates YAML output. Controller provides CRUD + YAML export endpoint. Frontend has PlaybookList and PlaybookBuilder pages with drag-to-reorder Roles.

**Tech Stack:** Spring Boot 3.x, Spring Data JPA, Jakarta Validation, Mockito + Testcontainers (PostgreSQL 16), React 18 + Ant Design 5 + TypeScript, js-yaml

**Dependencies:** Tag, Environment, Variable, File modules must all be complete before this module.

---

## File Change Summary

| File | Action |
|------|--------|
| `backend/.../playbook/entity/Playbook.java` | Create |
| `backend/.../playbook/entity/PlaybookRole.java` | Create |
| `backend/.../playbook/entity/PlaybookHostGroup.java` | Create |
| `backend/.../playbook/entity/PlaybookTag.java` | Create |
| `backend/.../playbook/repository/PlaybookRepository.java` | Create |
| `backend/.../playbook/repository/PlaybookRoleRepository.java` | Create |
| `backend/.../playbook/repository/PlaybookHostGroupRepository.java` | Create |
| `backend/.../playbook/repository/PlaybookTagRepository.java` | Create |
| `backend/.../playbook/dto/CreatePlaybookRequest.java` | Create |
| `backend/.../playbook/dto/UpdatePlaybookRequest.java` | Create |
| `backend/.../playbook/dto/PlaybookResponse.java` | Create |
| `backend/.../playbook/dto/PlaybookRoleRequest.java` | Create |
| `backend/.../playbook/service/PlaybookService.java` | Create |
| `backend/.../playbook/yaml/PlaybookYamlGenerator.java` | Create |
| `backend/.../playbook/controller/PlaybookController.java` | Create |
| `backend/.../playbook/service/PlaybookServiceTest.java` | Create |
| `backend/.../playbook/service/PlaybookYamlGeneratorTest.java` | Create |
| `backend/.../playbook/controller/PlaybookControllerTest.java` | Create |
| `frontend/src/types/entity/Playbook.ts` | Create |
| `frontend/src/api/playbook.ts` | Create |
| `frontend/src/pages/playbook/PlaybookList.tsx` | Create |
| `frontend/src/pages/playbook/PlaybookBuilder.tsx` | Create |

---

## Backend Tasks

### Task 1: Playbook Entities

**Files:**
- Create: `backend/src/main/java/com/ansible/playbook/entity/Playbook.java`
- Create: `backend/src/main/java/com/ansible/playbook/entity/PlaybookRole.java`
- Create: `backend/src/main/java/com/ansible/playbook/entity/PlaybookHostGroup.java`
- Create: `backend/src/main/java/com/ansible/playbook/entity/PlaybookTag.java`

- [ ] **Step 1: Create Playbook entity**

```java
package com.ansible.playbook.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "playbooks")
@Getter
@Setter
@NoArgsConstructor
public class Playbook extends BaseEntity {

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String extraVars;
}
```

- [ ] **Step 2: Create PlaybookRole join entity**

```java
package com.ansible.playbook.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "playbook_roles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"playbookId", "roleId"})
})
@Getter
@Setter
@NoArgsConstructor
public class PlaybookRole extends BaseEntity {

    @Column(nullable = false)
    private Long playbookId;

    @Column(nullable = false)
    private Long roleId;

    @Column(nullable = false)
    private Integer orderIndex;
}
```

- [ ] **Step 3: Create PlaybookHostGroup join entity**

```java
package com.ansible.playbook.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "playbook_host_groups", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"playbookId", "hostGroupId"})
})
@Getter
@Setter
@NoArgsConstructor
public class PlaybookHostGroup extends BaseEntity {

    @Column(nullable = false)
    private Long playbookId;

    @Column(nullable = false)
    private Long hostGroupId;
}
```

- [ ] **Step 4: Create PlaybookTag join entity**

```java
package com.ansible.playbook.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "playbook_tags", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"playbookId", "tagId"})
})
@Getter
@Setter
@NoArgsConstructor
public class PlaybookTag extends BaseEntity {

    @Column(nullable = false)
    private Long playbookId;

    @Column(nullable = false)
    private Long tagId;
}
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/playbook/entity/
git commit -m "feat(playbook): add Playbook, PlaybookRole, PlaybookHostGroup, PlaybookTag entities"
```

---

### Task 2: Playbook Repositories

**Files:**
- Create: `backend/src/main/java/com/ansible/playbook/repository/PlaybookRepository.java`
- Create: `backend/src/main/java/com/ansible/playbook/repository/PlaybookRoleRepository.java`
- Create: `backend/src/main/java/com/ansible/playbook/repository/PlaybookHostGroupRepository.java`
- Create: `backend/src/main/java/com/ansible/playbook/repository/PlaybookTagRepository.java`

- [ ] **Step 1: Create repositories**

```java
// PlaybookRepository.java
package com.ansible.playbook.repository;

import com.ansible.playbook.entity.Playbook;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaybookRepository extends JpaRepository<Playbook, Long> {
    List<Playbook> findByProjectIdOrderByIdAsc(Long projectId);
}

// PlaybookRoleRepository.java
package com.ansible.playbook.repository;

import com.ansible.playbook.entity.PlaybookRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaybookRoleRepository extends JpaRepository<PlaybookRole, Long> {
    List<PlaybookRole> findByPlaybookIdOrderByOrderIndexAsc(Long playbookId);
    void deleteByPlaybookIdAndRoleId(Long playbookId, Long roleId);
    boolean existsByPlaybookIdAndRoleId(Long playbookId, Long roleId);
    void deleteByPlaybookId(Long playbookId);
}

// PlaybookHostGroupRepository.java
package com.ansible.playbook.repository;

import com.ansible.playbook.entity.PlaybookHostGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaybookHostGroupRepository extends JpaRepository<PlaybookHostGroup, Long> {
    List<PlaybookHostGroup> findByPlaybookId(Long playbookId);
    void deleteByPlaybookIdAndHostGroupId(Long playbookId, Long hostGroupId);
    boolean existsByPlaybookIdAndHostGroupId(Long playbookId, Long hostGroupId);
    void deleteByPlaybookId(Long playbookId);
}

// PlaybookTagRepository.java
package com.ansible.playbook.repository;

import com.ansible.playbook.entity.PlaybookTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaybookTagRepository extends JpaRepository<PlaybookTag, Long> {
    List<PlaybookTag> findByPlaybookId(Long playbookId);
    void deleteByPlaybookIdAndTagId(Long playbookId, Long tagId);
    boolean existsByPlaybookIdAndTagId(Long playbookId, Long tagId);
    void deleteByPlaybookId(Long playbookId);
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/ansible/playbook/repository/
git commit -m "feat(playbook): add Playbook repositories"
```

---

### Task 3: Playbook DTOs

**Files:**
- Create: `backend/src/main/java/com/ansible/playbook/dto/CreatePlaybookRequest.java`
- Create: `backend/src/main/java/com/ansible/playbook/dto/UpdatePlaybookRequest.java`
- Create: `backend/src/main/java/com/ansible/playbook/dto/PlaybookResponse.java`
- Create: `backend/src/main/java/com/ansible/playbook/dto/PlaybookRoleRequest.java`

- [ ] **Step 1: Create DTOs**

```java
// CreatePlaybookRequest.java
package com.ansible.playbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePlaybookRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 500) String description,
    String extraVars
) {}

// UpdatePlaybookRequest.java
package com.ansible.playbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePlaybookRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 500) String description,
    String extraVars
) {}

// PlaybookResponse.java
package com.ansible.playbook.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PlaybookResponse(
    Long id,
    Long projectId,
    String name,
    String description,
    String extraVars,
    List<Long> roleIds,
    List<Long> hostGroupIds,
    List<Long> tagIds,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

// PlaybookRoleRequest.java
package com.ansible.playbook.dto;

import jakarta.validation.constraints.NotNull;

public record PlaybookRoleRequest(
    @NotNull Long roleId
) {}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/ansible/playbook/dto/
git commit -m "feat(playbook): add Playbook DTOs"
```

---

### Task 4: PlaybookYamlGenerator with Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/playbook/yaml/PlaybookYamlGenerator.java`
- Create: `backend/src/test/java/com/ansible/playbook/yaml/PlaybookYamlGeneratorTest.java`

- [ ] **Step 1: Write PlaybookYamlGeneratorTest**

```java
package com.ansible.playbook.yaml;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.role.entity.Task;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlaybookYamlGeneratorTest {

    private final PlaybookYamlGenerator generator = new PlaybookYamlGenerator();

    @Test
    void generate_emptyPlaybook_returnsEmptyYaml() {
        String yaml = generator.generate(List.of(), List.of("all"), List.of());
        assertThat(yaml).contains("- hosts: all");
    }

    @Test
    void generate_withHostGroups() {
        String yaml = generator.generate(List.of(), List.of("web", "db"), List.of());
        assertThat(yaml).contains("- hosts: web,db");
    }

    @Test
    void generate_withTags() {
        String yaml = generator.generate(List.of(), List.of("all"), List.of("web", "production"));
        assertThat(yaml).contains("tags: [web, production]");
    }

    @Test
    void generate_withRoleNames() {
        String yaml = generator.generate(List.of("nginx", "postgresql"), List.of("all"), List.of());
        assertThat(yaml).contains("roles:").contains("- nginx").contains("- postgresql");
    }

    @Test
    void generate_fullPlaybook() {
        String yaml = generator.generate(
            List.of("nginx", "app"),
            List.of("web_servers"),
            List.of("deploy"));

        assertThat(yaml).contains("- hosts: web_servers");
        assertThat(yaml).contains("roles:");
        assertThat(yaml).contains("- nginx");
        assertThat(yaml).contains("- app");
        assertThat(yaml).contains("tags: [deploy]");
    }
}
```

- [ ] **Step 2: Run test to verify failures**

Run: `cd backend && mvn test -Dtest=PlaybookYamlGeneratorTest -pl .`
Expected: FAIL

- [ ] **Step 3: Write PlaybookYamlGenerator**

```java
package com.ansible.playbook.yaml;

import java.util.List;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class PlaybookYamlGenerator {

    public String generate(List<String> roleNames, List<String> hostGroupNames, List<String> tagNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("- hosts: ").append(String.join(",", hostGroupNames)).append("\n");

        if (!roleNames.isEmpty()) {
            sb.append("  roles:\n");
            for (String role : roleNames) {
                sb.append("    - ").append(role).append("\n");
            }
        }

        if (!tagNames.isEmpty()) {
            sb.append("  tags: [").append(String.join(", ", tagNames)).append("]\n");
        }

        return sb.toString();
    }
}
```

- [ ] **Step 4: Run tests**

Run: `cd backend && mvn test -Dtest=PlaybookYamlGeneratorTest -pl .`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/playbook/yaml/ backend/src/test/java/com/ansible/playbook/yaml/
git commit -m "feat(playbook): add PlaybookYamlGenerator with tests"
```

---

### Task 5: PlaybookService with Unit Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/playbook/service/PlaybookService.java`
- Create: `backend/src/test/java/com/ansible/playbook/service/PlaybookServiceTest.java`

- [ ] **Step 1: Write PlaybookServiceTest**

```java
package com.ansible.playbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ansible.host.entity.HostGroup;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.playbook.dto.*;
import com.ansible.playbook.entity.*;
import com.ansible.playbook.repository.*;
import com.ansible.playbook.yaml.PlaybookYamlGenerator;
import com.ansible.role.entity.Role;
import com.ansible.role.repository.RoleRepository;
import com.ansible.tag.entity.Tag;
import com.ansible.tag.repository.TagRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlaybookServiceTest {

    @Mock private PlaybookRepository playbookRepository;
    @Mock private PlaybookRoleRepository playbookRoleRepository;
    @Mock private PlaybookHostGroupRepository playbookHostGroupRepository;
    @Mock private PlaybookTagRepository playbookTagRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private HostGroupRepository hostGroupRepository;
    @Mock private TagRepository tagRepository;
    @Mock private PlaybookYamlGenerator yamlGenerator;

    @InjectMocks
    private PlaybookService playbookService;

    @Test
    void createPlaybook_success() {
        when(playbookRepository.save(any(Playbook.class))).thenAnswer(inv -> {
            Playbook p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        var response = playbookService.createPlaybook(1L,
            new CreatePlaybookRequest("deploy.yml", "Deploy app", null), 100L);

        assertThat(response.name()).isEqualTo("deploy.yml");
    }

    @Test
    void listPlaybooks_success() {
        Playbook p = new Playbook();
        p.setId(1L); p.setProjectId(1L); p.setName("deploy.yml");
        when(playbookRepository.findByProjectIdOrderByIdAsc(1L)).thenReturn(List.of(p));
        when(playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        when(playbookHostGroupRepository.findByPlaybookId(1L)).thenReturn(List.of());
        when(playbookTagRepository.findByPlaybookId(1L)).thenReturn(List.of());

        var list = playbookService.listPlaybooks(1L);
        assertThat(list).hasSize(1);
    }

    @Test
    void addRole_success() {
        Playbook p = new Playbook(); p.setId(1L); p.setProjectId(1L);
        when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));
        when(playbookRoleRepository.existsByPlaybookIdAndRoleId(1L, 10L)).thenReturn(false);
        when(playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        when(playbookRoleRepository.save(any(PlaybookRole.class))).thenAnswer(inv -> {
            PlaybookRole pr = inv.getArgument(0);
            pr.setId(100L);
            return pr;
        });

        playbookService.addRole(1L, new PlaybookRoleRequest(10L), 100L);

        verify(playbookRoleRepository).save(any(PlaybookRole.class));
    }

    @Test
    void addRole_duplicate_throws() {
        Playbook p = new Playbook(); p.setId(1L);
        when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));
        when(playbookRoleRepository.existsByPlaybookIdAndRoleId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> playbookService.addRole(1L, new PlaybookRoleRequest(10L), 100L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already added");
    }

    @Test
    void removeRole_success() {
        playbookService.removeRole(1L, 10L);
        verify(playbookRoleRepository).deleteByPlaybookIdAndRoleId(1L, 10L);
    }

    @Test
    void addHostGroup_success() {
        Playbook p = new Playbook(); p.setId(1L);
        when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));
        when(playbookHostGroupRepository.existsByPlaybookIdAndHostGroupId(1L, 5L)).thenReturn(false);
        when(playbookHostGroupRepository.save(any(PlaybookHostGroup.class))).thenAnswer(inv -> {
            PlaybookHostGroup phg = inv.getArgument(0);
            phg.setId(100L);
            return phg;
        });

        playbookService.addHostGroup(1L, 5L, 100L);
        verify(playbookHostGroupRepository).save(any(PlaybookHostGroup.class));
    }

    @Test
    void generateYaml_success() {
        Playbook p = new Playbook(); p.setId(1L); p.setProjectId(1L);
        when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));

        PlaybookRole pr = new PlaybookRole(); pr.setRoleId(10L); pr.setOrderIndex(0);
        when(playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(pr));

        Role role = new Role(); role.setId(10L); role.setName("nginx");
        when(roleRepository.findById(10L)).thenReturn(Optional.of(role));

        PlaybookHostGroup phg = new PlaybookHostGroup(); phg.setHostGroupId(5L);
        when(playbookHostGroupRepository.findByPlaybookId(1L)).thenReturn(List.of(phg));

        HostGroup hg = new HostGroup(); hg.setId(5L); hg.setName("web_servers");
        when(hostGroupRepository.findById(5L)).thenReturn(Optional.of(hg));

        PlaybookTag pt = new PlaybookTag(); pt.setTagId(3L);
        when(playbookTagRepository.findByPlaybookId(1L)).thenReturn(List.of(pt));

        Tag tag = new Tag(); tag.setId(3L); tag.setName("deploy");
        when(tagRepository.findById(3L)).thenReturn(Optional.of(tag));

        when(yamlGenerator.generate(List.of("nginx"), List.of("web_servers"), List.of("deploy")))
            .thenReturn("- hosts: web_servers\n  roles:\n    - nginx\n  tags: [deploy]\n");

        String yaml = playbookService.generateYaml(1L);

        assertThat(yaml).contains("nginx", "web_servers", "deploy");
    }

    @Test
    void deletePlaybook_cascades() {
        Playbook p = new Playbook(); p.setId(1L);
        when(playbookRepository.findById(1L)).thenReturn(Optional.of(p));

        playbookService.deletePlaybook(1L, 100L);

        verify(playbookRoleRepository).deleteByPlaybookId(1L);
        verify(playbookHostGroupRepository).deleteByPlaybookId(1L);
        verify(playbookTagRepository).deleteByPlaybookId(1L);
        verify(playbookRepository).delete(p);
    }
}
```

- [ ] **Step 2: Run test to verify failures**

Run: `cd backend && mvn test -Dtest=PlaybookServiceTest -pl .`
Expected: FAIL

- [ ] **Step 3: Write PlaybookService**

```java
package com.ansible.playbook.service;

import com.ansible.host.entity.HostGroup;
import com.ansible.host.repository.HostGroupRepository;
import com.ansible.playbook.dto.*;
import com.ansible.playbook.entity.*;
import com.ansible.playbook.repository.*;
import com.ansible.playbook.yaml.PlaybookYamlGenerator;
import com.ansible.role.entity.Role;
import com.ansible.role.repository.RoleRepository;
import com.ansible.tag.entity.Tag;
import com.ansible.tag.repository.TagRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlaybookService {

    private final PlaybookRepository playbookRepository;
    private final PlaybookRoleRepository playbookRoleRepository;
    private final PlaybookHostGroupRepository playbookHostGroupRepository;
    private final PlaybookTagRepository playbookTagRepository;
    private final RoleRepository roleRepository;
    private final HostGroupRepository hostGroupRepository;
    private final TagRepository tagRepository;
    private final PlaybookYamlGenerator yamlGenerator;

    public PlaybookService(PlaybookRepository playbookRepository,
                           PlaybookRoleRepository playbookRoleRepository,
                           PlaybookHostGroupRepository playbookHostGroupRepository,
                           PlaybookTagRepository playbookTagRepository,
                           RoleRepository roleRepository,
                           HostGroupRepository hostGroupRepository,
                           TagRepository tagRepository,
                           PlaybookYamlGenerator yamlGenerator) {
        this.playbookRepository = playbookRepository;
        this.playbookRoleRepository = playbookRoleRepository;
        this.playbookHostGroupRepository = playbookHostGroupRepository;
        this.playbookTagRepository = playbookTagRepository;
        this.roleRepository = roleRepository;
        this.hostGroupRepository = hostGroupRepository;
        this.tagRepository = tagRepository;
        this.yamlGenerator = yamlGenerator;
    }

    public PlaybookResponse createPlaybook(Long projectId, CreatePlaybookRequest request, Long userId) {
        Playbook p = new Playbook();
        p.setProjectId(projectId);
        p.setName(request.name());
        p.setDescription(request.description());
        p.setExtraVars(request.extraVars());
        p.setCreatedBy(userId);
        return toResponse(playbookRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<PlaybookResponse> listPlaybooks(Long projectId) {
        return playbookRepository.findByProjectIdOrderByIdAsc(projectId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public PlaybookResponse getPlaybook(Long playbookId) {
        Playbook p = playbookRepository.findById(playbookId)
            .orElseThrow(() -> new IllegalArgumentException("Playbook not found"));
        return toResponse(p);
    }

    public PlaybookResponse updatePlaybook(Long playbookId, UpdatePlaybookRequest request, Long userId) {
        Playbook p = playbookRepository.findById(playbookId)
            .orElseThrow(() -> new IllegalArgumentException("Playbook not found"));
        p.setName(request.name());
        p.setDescription(request.description());
        p.setExtraVars(request.extraVars());
        return toResponse(playbookRepository.save(p));
    }

    public void deletePlaybook(Long playbookId, Long userId) {
        Playbook p = playbookRepository.findById(playbookId)
            .orElseThrow(() -> new IllegalArgumentException("Playbook not found"));
        playbookRoleRepository.deleteByPlaybookId(playbookId);
        playbookHostGroupRepository.deleteByPlaybookId(playbookId);
        playbookTagRepository.deleteByPlaybookId(playbookId);
        playbookRepository.delete(p);
    }

    public void addRole(Long playbookId, PlaybookRoleRequest request, Long userId) {
        playbookRepository.findById(playbookId)
            .orElseThrow(() -> new IllegalArgumentException("Playbook not found"));
        if (playbookRoleRepository.existsByPlaybookIdAndRoleId(playbookId, request.roleId())) {
            throw new IllegalArgumentException("Role already added to this playbook");
        }
        List<PlaybookRole> existing = playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(playbookId);
        PlaybookRole pr = new PlaybookRole();
        pr.setPlaybookId(playbookId);
        pr.setRoleId(request.roleId());
        pr.setOrderIndex(existing.size());
        pr.setCreatedBy(userId);
        playbookRoleRepository.save(pr);
    }

    public void removeRole(Long playbookId, Long roleId) {
        playbookRoleRepository.deleteByPlaybookIdAndRoleId(playbookId, roleId);
    }

    public void reorderRoles(Long playbookId, List<Long> roleIds) {
        playbookRepository.findById(playbookId)
            .orElseThrow(() -> new IllegalArgumentException("Playbook not found"));
        List<PlaybookRole> existing = playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(playbookId);
        for (int i = 0; i < roleIds.size(); i++) {
            Long roleId = roleIds.get(i);
            existing.stream()
                .filter(pr -> pr.getRoleId().equals(roleId))
                .findFirst()
                .ifPresent(pr -> pr.setOrderIndex(i));
        }
        playbookRoleRepository.saveAll(existing);
    }

    public void addHostGroup(Long playbookId, Long hostGroupId, Long userId) {
        playbookRepository.findById(playbookId)
            .orElseThrow(() -> new IllegalArgumentException("Playbook not found"));
        if (playbookHostGroupRepository.existsByPlaybookIdAndHostGroupId(playbookId, hostGroupId)) {
            throw new IllegalArgumentException("Host group already added");
        }
        PlaybookHostGroup phg = new PlaybookHostGroup();
        phg.setPlaybookId(playbookId);
        phg.setHostGroupId(hostGroupId);
        phg.setCreatedBy(userId);
        playbookHostGroupRepository.save(phg);
    }

    public void removeHostGroup(Long playbookId, Long hostGroupId) {
        playbookHostGroupRepository.deleteByPlaybookIdAndHostGroupId(playbookId, hostGroupId);
    }

    public void addTag(Long playbookId, Long tagId, Long userId) {
        playbookRepository.findById(playbookId)
            .orElseThrow(() -> new IllegalArgumentException("Playbook not found"));
        if (playbookTagRepository.existsByPlaybookIdAndTagId(playbookId, tagId)) {
            throw new IllegalArgumentException("Tag already added");
        }
        PlaybookTag pt = new PlaybookTag();
        pt.setPlaybookId(playbookId);
        pt.setTagId(tagId);
        pt.setCreatedBy(userId);
        playbookTagRepository.save(pt);
    }

    public void removeTag(Long playbookId, Long tagId) {
        playbookTagRepository.deleteByPlaybookIdAndTagId(playbookId, tagId);
    }

    @Transactional(readOnly = true)
    public String generateYaml(Long playbookId) {
        Playbook p = playbookRepository.findById(playbookId)
            .orElseThrow(() -> new IllegalArgumentException("Playbook not found"));

        List<String> roleNames = playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(playbookId).stream()
            .map(pr -> roleRepository.findById(pr.getRoleId())
                .map(Role::getName).orElse("unknown"))
            .toList();

        List<String> hostGroupNames = playbookHostGroupRepository.findByPlaybookId(playbookId).stream()
            .map(phg -> hostGroupRepository.findById(phg.getHostGroupId())
                .map(HostGroup::getName).orElse("unknown"))
            .toList();

        List<String> tagNames = playbookTagRepository.findByPlaybookId(playbookId).stream()
            .map(pt -> tagRepository.findById(pt.getTagId())
                .map(Tag::getName).orElse("unknown"))
            .toList();

        return yamlGenerator.generate(roleNames, hostGroupNames, tagNames);
    }

    private PlaybookResponse toResponse(Playbook p) {
        List<Long> roleIds = playbookRoleRepository.findByPlaybookIdOrderByOrderIndexAsc(p.getId()).stream()
            .map(PlaybookRole::getRoleId).toList();
        List<Long> hostGroupIds = playbookHostGroupRepository.findByPlaybookId(p.getId()).stream()
            .map(PlaybookHostGroup::getHostGroupId).toList();
        List<Long> tagIds = playbookTagRepository.findByPlaybookId(p.getId()).stream()
            .map(PlaybookTag::getTagId).toList();

        return new PlaybookResponse(p.getId(), p.getProjectId(), p.getName(),
            p.getDescription(), p.getExtraVars(), roleIds, hostGroupIds, tagIds,
            p.getCreatedAt(), p.getUpdatedAt());
    }
}
```

- [ ] **Step 4: Run tests**

Run: `cd backend && mvn test -Dtest=PlaybookServiceTest -pl .`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/playbook/service/ backend/src/test/java/com/ansible/playbook/service/
git commit -m "feat(playbook): add PlaybookService with unit tests"
```

---

### Task 6: PlaybookController with Integration Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/playbook/controller/PlaybookController.java`
- Create: `backend/src/test/java/com/ansible/playbook/controller/PlaybookControllerTest.java`

- [ ] **Step 1: Write PlaybookControllerTest**

```java
package com.ansible.playbook.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.common.AbstractIntegrationTest;
import com.ansible.playbook.dto.CreatePlaybookRequest;
import com.ansible.playbook.dto.PlaybookRoleRequest;
import com.ansible.playbook.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

class PlaybookControllerTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private PlaybookRepository playbookRepository;
    @Autowired private PlaybookRoleRepository playbookRoleRepository;
    @Autowired private PlaybookHostGroupRepository playbookHostGroupRepository;
    @Autowired private PlaybookTagRepository playbookTagRepository;

    private String authHeaders;
    private Long projectId;
    private Long roleId;

    @BeforeEach
    void setUp() {
        var auth = registerAndLogin("pbuser", "pbuser@test.com", "pass123");
        authHeaders = auth.token();
        projectId = createProject(authHeaders, "PB Test Project");
        roleId = createRole(authHeaders, projectId, "nginx");
    }

    @AfterEach
    void tearDown() {
        playbookTagRepository.deleteAll();
        playbookHostGroupRepository.deleteAll();
        playbookRoleRepository.deleteAll();
        playbookRepository.deleteAll();
        cleanupProjectsAndUsers();
    }

    @Test
    void createPlaybook_success() {
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/projects/" + projectId + "/playbooks",
            HttpMethod.POST,
            withAuth(authHeaders, new CreatePlaybookRequest("deploy.yml", "Deploy", null)),
            String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("deploy.yml");
    }

    @Test
    void listPlaybooks_success() {
        restTemplate.exchange("/api/projects/" + projectId + "/playbooks",
            HttpMethod.POST,
            withAuth(authHeaders, new CreatePlaybookRequest("deploy.yml", null, null)),
            String.class);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/projects/" + projectId + "/playbooks",
            HttpMethod.GET, withAuth(authHeaders, null), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("deploy.yml");
    }

    @Test
    void addRole_success() {
        var createResp = restTemplate.exchange("/api/projects/" + projectId + "/playbooks",
            HttpMethod.POST,
            withAuth(authHeaders, new CreatePlaybookRequest("deploy.yml", null, null)),
            String.class);
        Long playbookId = extractId(createResp);

        ResponseEntity<Void> roleResp = restTemplate.exchange(
            "/api/playbooks/" + playbookId + "/roles",
            HttpMethod.POST,
            withAuth(authHeaders, new PlaybookRoleRequest(roleId)),
            Void.class);

        assertThat(roleResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void generateYaml_success() {
        var createResp = restTemplate.exchange("/api/projects/" + projectId + "/playbooks",
            HttpMethod.POST,
            withAuth(authHeaders, new CreatePlaybookRequest("deploy.yml", null, null)),
            String.class);
        Long playbookId = extractId(createResp);

        restTemplate.exchange("/api/playbooks/" + playbookId + "/roles",
            HttpMethod.POST,
            withAuth(authHeaders, new PlaybookRoleRequest(roleId)),
            Void.class);

        ResponseEntity<String> yamlResp = restTemplate.exchange(
            "/api/playbooks/" + playbookId + "/yaml",
            HttpMethod.GET, withAuth(authHeaders, null), String.class);

        assertThat(yamlResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(yamlResp.getBody()).contains("nginx");
    }

    @Test
    void deletePlaybook_success() {
        var createResp = restTemplate.exchange("/api/projects/" + projectId + "/playbooks",
            HttpMethod.POST,
            withAuth(authHeaders, new CreatePlaybookRequest("deploy.yml", null, null)),
            String.class);
        Long playbookId = extractId(createResp);

        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/playbooks/" + playbookId,
            HttpMethod.DELETE, withAuth(authHeaders, null), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(playbookRepository.findById(playbookId)).isEmpty();
    }
}
```

- [ ] **Step 2: Write PlaybookController**

```java
package com.ansible.playbook.controller;

import com.ansible.playbook.dto.*;
import com.ansible.playbook.service.PlaybookService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PlaybookController {

    private final PlaybookService playbookService;

    public PlaybookController(PlaybookService playbookService) {
        this.playbookService = playbookService;
    }

    @PostMapping("/projects/{projectId}/playbooks")
    public ResponseEntity<PlaybookResponse> createPlaybook(
        @PathVariable Long projectId,
        @Valid @RequestBody CreatePlaybookRequest request,
        @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(playbookService.createPlaybook(projectId, request, userId));
    }

    @GetMapping("/projects/{projectId}/playbooks")
    public ResponseEntity<List<PlaybookResponse>> listPlaybooks(@PathVariable Long projectId) {
        return ResponseEntity.ok(playbookService.listPlaybooks(projectId));
    }

    @GetMapping("/playbooks/{playbookId}")
    public ResponseEntity<PlaybookResponse> getPlaybook(@PathVariable Long playbookId) {
        return ResponseEntity.ok(playbookService.getPlaybook(playbookId));
    }

    @PutMapping("/playbooks/{playbookId}")
    public ResponseEntity<PlaybookResponse> updatePlaybook(
        @PathVariable Long playbookId,
        @Valid @RequestBody UpdatePlaybookRequest request,
        @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(playbookService.updatePlaybook(playbookId, request, userId));
    }

    @DeleteMapping("/playbooks/{playbookId}")
    public ResponseEntity<Void> deletePlaybook(
        @PathVariable Long playbookId,
        @AuthenticationPrincipal Long userId) {
        playbookService.deletePlaybook(playbookId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/playbooks/{playbookId}/roles")
    public ResponseEntity<Void> addRole(
        @PathVariable Long playbookId,
        @Valid @RequestBody PlaybookRoleRequest request,
        @AuthenticationPrincipal Long userId) {
        playbookService.addRole(playbookId, request, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/playbooks/{playbookId}/roles/{roleId}")
    public ResponseEntity<Void> removeRole(@PathVariable Long playbookId, @PathVariable Long roleId) {
        playbookService.removeRole(playbookId, roleId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/playbooks/{playbookId}/roles/order")
    public ResponseEntity<Void> reorderRoles(
        @PathVariable Long playbookId,
        @RequestBody List<Long> roleIds) {
        playbookService.reorderRoles(playbookId, roleIds);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/playbooks/{playbookId}/host-groups/{hostGroupId}")
    public ResponseEntity<Void> addHostGroup(
        @PathVariable Long playbookId,
        @PathVariable Long hostGroupId,
        @AuthenticationPrincipal Long userId) {
        playbookService.addHostGroup(playbookId, hostGroupId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/playbooks/{playbookId}/host-groups/{hostGroupId}")
    public ResponseEntity<Void> removeHostGroup(
        @PathVariable Long playbookId, @PathVariable Long hostGroupId) {
        playbookService.removeHostGroup(playbookId, hostGroupId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/playbooks/{playbookId}/tags/{tagId}")
    public ResponseEntity<Void> addTag(
        @PathVariable Long playbookId,
        @PathVariable Long tagId,
        @AuthenticationPrincipal Long userId) {
        playbookService.addTag(playbookId, tagId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/playbooks/{playbookId}/tags/{tagId}")
    public ResponseEntity<Void> removeTag(
        @PathVariable Long playbookId, @PathVariable Long tagId) {
        playbookService.removeTag(playbookId, tagId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/playbooks/{playbookId}/yaml")
    public ResponseEntity<String> generateYaml(@PathVariable Long playbookId) {
        return ResponseEntity.ok(playbookService.generateYaml(playbookId));
    }
}
```

- [ ] **Step 3: Run integration tests**

Run: `cd backend && mvn verify -Dtest=PlaybookControllerTest -pl .`
Expected: PASS

- [ ] **Step 4: Run quality checks**

Run: `cd backend && mvn spotless:apply checkstyle:check pmd:check spotbugs:check -pl .`
Expected: All pass

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/playbook/controller/ backend/src/test/java/com/ansible/playbook/controller/
git commit -m "feat(playbook): add PlaybookController with integration tests"
```

---

## Frontend Tasks

### Task 7: Playbook Types and API Layer

**Files:**
- Create: `frontend/src/types/entity/Playbook.ts`
- Create: `frontend/src/api/playbook.ts`

- [ ] **Step 1: Create Playbook types**

```typescript
// frontend/src/types/entity/Playbook.ts

export interface Playbook {
  id: number;
  projectId: number;
  name: string;
  description: string;
  extraVars: string;
  roleIds: number[];
  hostGroupIds: number[];
  tagIds: number[];
  createdAt: string;
  updatedAt: string;
}

export interface CreatePlaybookRequest {
  name: string;
  description?: string;
  extraVars?: string;
}

export interface UpdatePlaybookRequest {
  name: string;
  description?: string;
  extraVars?: string;
}
```

- [ ] **Step 2: Create Playbook API**

```typescript
// frontend/src/api/playbook.ts

import request from './request';
import type { Playbook, CreatePlaybookRequest, UpdatePlaybookRequest } from '../types/entity/Playbook';

export const playbookApi = {
  list: (projectId: number) =>
    request.get<Playbook[]>(`/api/projects/${projectId}/playbooks`),

  get: (playbookId: number) =>
    request.get<Playbook>(`/api/playbooks/${playbookId}`),

  create: (projectId: number, data: CreatePlaybookRequest) =>
    request.post<Playbook>(`/api/projects/${projectId}/playbooks`, data),

  update: (playbookId: number, data: UpdatePlaybookRequest) =>
    request.put<Playbook>(`/api/playbooks/${playbookId}`, data),

  delete: (playbookId: number) =>
    request.delete(`/api/playbooks/${playbookId}`),

  addRole: (playbookId: number, roleId: number) =>
    request.post(`/api/playbooks/${playbookId}/roles`, { roleId }),

  removeRole: (playbookId: number, roleId: number) =>
    request.delete(`/api/playbooks/${playbookId}/roles/${roleId}`),

  reorderRoles: (playbookId: number, roleIds: number[]) =>
    request.put(`/api/playbooks/${playbookId}/roles/order`, roleIds),

  addHostGroup: (playbookId: number, hostGroupId: number) =>
    request.post(`/api/playbooks/${playbookId}/host-groups/${hostGroupId}`),

  removeHostGroup: (playbookId: number, hostGroupId: number) =>
    request.delete(`/api/playbooks/${playbookId}/host-groups/${hostGroupId}`),

  addTag: (playbookId: number, tagId: number) =>
    request.post(`/api/playbooks/${playbookId}/tags/${tagId}`),

  removeTag: (playbookId: number, tagId: number) =>
    request.delete(`/api/playbooks/${playbookId}/tags/${tagId}`),

  generateYaml: (playbookId: number) =>
    request.get<string>(`/api/playbooks/${playbookId}/yaml`),
};
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/types/entity/Playbook.ts frontend/src/api/playbook.ts
git commit -m "feat(playbook): add Playbook types and API layer"
```

---

### Task 8: PlaybookList Page

**Files:**
- Create: `frontend/src/pages/playbook/PlaybookList.tsx`

- [ ] **Step 1: Create PlaybookList component**

```tsx
// frontend/src/pages/playbook/PlaybookList.tsx

import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, Table, Modal, Form, Input, message, Popconfirm, Space } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { playbookApi } from '../../api/playbook';
import type { Playbook } from '../../types/entity/Playbook';
import { useProjectStore } from '../../stores/projectStore';

export default function PlaybookList() {
  const { currentProject } = useProjectStore();
  const navigate = useNavigate();
  const [playbooks, setPlaybooks] = useState<Playbook[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [form] = Form.useForm();

  const fetchPlaybooks = async () => {
    if (!currentProject) return;
    setLoading(true);
    try {
      const res = await playbookApi.list(currentProject.id);
      setPlaybooks(res.data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchPlaybooks(); }, [currentProject]);

  const handleCreate = async () => {
    const values = await form.validateFields();
    await playbookApi.create(currentProject!.id, values);
    message.success('创建成功');
    setModalOpen(false);
    form.resetFields();
    fetchPlaybooks();
  };

  const handleDelete = async (id: number) => {
    await playbookApi.delete(id);
    message.success('删除成功');
    fetchPlaybooks();
  };

  const columns = [
    { title: '名称', dataIndex: 'name', key: 'name',
      render: (name: string, record: Playbook) => (
        <Button type="link" onClick={() => navigate(`/projects/${currentProject!.id}/playbooks/${record.id}`)}>
          {name}
        </Button>
      ),
    },
    { title: '描述', dataIndex: 'description', key: 'description' },
    { title: 'Roles', key: 'roles', render: (_: unknown, r: Playbook) => r.roleIds.length },
    {
      title: '操作', key: 'action',
      render: (_: unknown, record: Playbook) => (
        <Space>
          <Button type="link" size="small"
            onClick={() => navigate(`/projects/${currentProject!.id}/playbooks/${record.id}`)}>
            编辑
          </Button>
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
          新建 Playbook
        </Button>
      </div>
      <Table rowKey="id" columns={columns} dataSource={playbooks} loading={loading} />

      <Modal title="新建 Playbook" open={modalOpen} onOk={handleCreate} onCancel={() => setModalOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input placeholder="例如: deploy.yml" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea placeholder="Playbook 描述（可选）" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/pages/playbook/PlaybookList.tsx
git commit -m "feat(playbook): add PlaybookList page"
```

---

### Task 9: PlaybookBuilder Page

**Files:**
- Create: `frontend/src/pages/playbook/PlaybookBuilder.tsx`

- [ ] **Step 1: Create PlaybookBuilder component**

```tsx
// frontend/src/pages/playbook/PlaybookBuilder.tsx

import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Card, Select, Button, Space, message, Popconfirm, Input, Spin } from 'antd';
import { DeleteOutlined, CopyOutlined } from '@ant-design/icons';
import { playbookApi } from '../../api/playbook';
import { roleApi } from '../../api/role';
import { hostGroupApi } from '../../api/hostGroup';
import { tagApi } from '../../api/tag';
import type { Playbook } from '../../types/entity/Playbook';
import type { Role } from '../../types/entity/Role';
import type { HostGroup } from '../../types/entity/HostGroup';
import type { Tag } from '../../types/entity/Tag';
import { useProjectStore } from '../../stores/projectStore';

export default function PlaybookBuilder() {
  const { pbId } = useParams<{ pbId: string }>();
  const { currentProject } = useProjectStore();
  const [playbook, setPlaybook] = useState<Playbook | null>(null);
  const [roles, setRoles] = useState<Role[]>([]);
  const [hostGroups, setHostGroups] = useState<HostGroup[]>([]);
  const [tags, setTags] = useState<Tag[]>([]);
  const [yamlPreview, setYamlPreview] = useState('');
  const [loading, setLoading] = useState(true);

  const fetchData = async () => {
    if (!pbId || !currentProject) return;
    setLoading(true);
    try {
      const [pbRes, rolesRes, hgRes, tagsRes, yamlRes] = await Promise.all([
        playbookApi.get(Number(pbId)),
        roleApi.list(currentProject.id),
        hostGroupApi.list(currentProject.id),
        tagApi.list(currentProject.id),
        playbookApi.generateYaml(Number(pbId)),
      ]);
      setPlaybook(pbRes.data);
      setRoles(rolesRes.data);
      setHostGroups(hgRes.data);
      setTags(tagsRes.data);
      setYamlPreview(yamlRes.data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, [pbId, currentProject]);

  const refreshYaml = async () => {
    if (!pbId) return;
    const res = await playbookApi.generateYaml(Number(pbId));
    setYamlPreview(res.data);
  };

  const handleAddRole = async (roleId: number) => {
    await playbookApi.addRole(Number(pbId), roleId);
    message.success('Role 已添加');
    fetchData();
  };

  const handleRemoveRole = async (roleId: number) => {
    await playbookApi.removeRole(Number(pbId), roleId);
    fetchData();
  };

  const handleAddHostGroup = async (hgId: number) => {
    await playbookApi.addHostGroup(Number(pbId), hgId);
    fetchData();
  };

  const handleRemoveHostGroup = async (hgId: number) => {
    await playbookApi.removeHostGroup(Number(pbId), hgId);
    fetchData();
  };

  const handleAddTag = async (tagId: number) => {
    await playbookApi.addTag(Number(pbId), tagId);
    fetchData();
  };

  const handleRemoveTag = async (tagId: number) => {
    await playbookApi.removeTag(Number(pbId), tagId);
    fetchData();
  };

  const handleCopyYaml = () => {
    navigator.clipboard.writeText(yamlPreview);
    message.success('已复制到剪贴板');
  };

  if (loading) return <Spin />;
  if (!playbook) return <div>Playbook not found</div>;

  const availableRoles = roles.filter(r => !playbook.roleIds.includes(r.id));
  const availableHostGroups = hostGroups.filter(h => !playbook.hostGroupIds.includes(h.id));
  const availableTags = tags.filter(t => !playbook.tagIds.includes(t.id));
  const selectedRoles = playbook.roleIds
    .map(id => roles.find(r => r.id === id))
    .filter((r): r is Role => r != null);
  const selectedHostGroups = playbook.hostGroupIds
    .map(id => hostGroups.find(h => h.id === id))
    .filter((h): h is HostGroup => h != null);
  const selectedTags = playbook.tagIds
    .map(id => tags.find(t => t.id === id))
    .filter((t): t is Tag => t != null);

  return (
    <div>
      <h2>{playbook.name}</h2>
      {playbook.description && <p>{playbook.description}</p>}

      <Space direction="vertical" style={{ width: '100%' }} size={16}>
        <Card title="主机组" size="small">
          <Space wrap>
            {selectedHostGroups.map(hg => (
              <span key={hg.id}>
                <Space>
                  <strong>{hg.name}</strong>
                  <Button type="text" size="small" danger icon={<DeleteOutlined />}
                    onClick={() => handleRemoveHostGroup(hg.id)} />
                </Space>
              </span>
            ))}
            {availableHostGroups.length > 0 && (
              <Select placeholder="添加主机组" style={{ width: 180 }}
                onSelect={handleAddHostGroup}
                options={availableHostGroups.map(h => ({ label: h.name, value: h.id }))} />
            )}
          </Space>
        </Card>

        <Card title="Roles（按顺序）" size="small">
          <Space direction="vertical" style={{ width: '100%' }}>
            {selectedRoles.map(r => (
              <div key={r.id} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span>{r.name}</span>
                <Button type="text" size="small" danger icon={<DeleteOutlined />}
                  onClick={() => handleRemoveRole(r.id)} />
              </div>
            ))}
            {availableRoles.length > 0 && (
              <Select placeholder="添加 Role" style={{ width: 200 }}
                onSelect={handleAddRole}
                options={availableRoles.map(r => ({ label: r.name, value: r.id }))} />
            )}
          </Space>
        </Card>

        <Card title="标签" size="small">
          <Space wrap>
            {selectedTags.map(t => (
              <span key={t.id}>
                <Space>
                  <strong>{t.name}</strong>
                  <Button type="text" size="small" danger icon={<DeleteOutlined />}
                    onClick={() => handleRemoveTag(t.id)} />
                </Space>
              </span>
            ))}
            {availableTags.length > 0 && (
              <Select placeholder="添加标签" style={{ width: 160 }}
                onSelect={handleAddTag}
                options={availableTags.map(t => ({ label: t.name, value: t.id }))} />
            )}
          </Space>
        </Card>

        <Card title="YAML 预览" size="small" extra={
          <Button icon={<CopyOutlined />} onClick={handleCopyYaml}>复制</Button>
        }>
          <pre style={{ background: '#f5f5f5', padding: 12, borderRadius: 4, overflow: 'auto', fontSize: 13 }}>
            {yamlPreview || '---'}
          </pre>
        </Card>
      </Space>
    </div>
  );
}
```

- [ ] **Step 2: Wire PlaybookList and PlaybookBuilder into routes**

Add routes:
- `/projects/:id/playbooks` → PlaybookList
- `/projects/:id/playbooks/:pbId` → PlaybookBuilder

Add "Playbook" menu item in project layout.

- [ ] **Step 3: Run lint and build**

Run: `cd frontend && npm run lint && npm run build`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/playbook/ frontend/src/components/Layout/
git commit -m "feat(playbook): add PlaybookBuilder page and routes"
```
