# Tag Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Tag CRUD and Task-Tag association for organizing and filtering Ansible tasks.

**Architecture:** Tag entity belongs to Project (via projectId FK). TaskTag is a join entity linking Task and Tag. Follows existing pattern: entity → repository → DTOs → service → controller → tests. Frontend adds Tag management page and tag picker in Task/Handler editors.

**Tech Stack:** Spring Boot 3.x, Spring Data JPA, Jakarta Validation, Mockito + Testcontainers (PostgreSQL 16), React 18 + Ant Design 5 + TypeScript

---

## File Change Summary

| File | Action |
|------|--------|
| `backend/.../tag/entity/Tag.java` | Create |
| `backend/.../tag/entity/TaskTag.java` | Create |
| `backend/.../tag/repository/TagRepository.java` | Create |
| `backend/.../tag/repository/TaskTagRepository.java` | Create |
| `backend/.../tag/dto/CreateTagRequest.java` | Create |
| `backend/.../tag/dto/UpdateTagRequest.java` | Create |
| `backend/.../tag/dto/TagResponse.java` | Create |
| `backend/.../tag/service/TagService.java` | Create |
| `backend/.../tag/controller/TagController.java` | Create |
| `backend/.../tag/service/TagServiceTest.java` | Create |
| `backend/.../tag/controller/TagControllerTest.java` | Create |
| `frontend/src/types/entity/Tag.ts` | Create |
| `frontend/src/api/tag.ts` | Create |
| `frontend/src/pages/tag/TagManager.tsx` | Create |
| `frontend/src/components/role/TagSelect.tsx` | Create |

---

## Backend Tasks

### Task 1: Tag and TaskTag Entities

**Files:**
- Create: `backend/src/main/java/com/ansible/tag/entity/Tag.java`
- Create: `backend/src/main/java/com/ansible/tag/entity/TaskTag.java`

- [ ] **Step 1: Create Tag entity**

```java
package com.ansible.tag.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tags", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"projectId", "name"})
})
@Getter
@Setter
@NoArgsConstructor
public class Tag extends BaseEntity {

    @Column(nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 100)
    private String name;
}
```

- [ ] **Step 2: Create TaskTag join entity**

```java
package com.ansible.tag.entity;

import com.ansible.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "task_tags", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"taskId", "tagId"})
})
@Getter
@Setter
@NoArgsConstructor
public class TaskTag extends BaseEntity {

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private Long tagId;
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/ansible/tag/
git commit -m "feat(tag): add Tag and TaskTag entities"
```

---

### Task 2: Tag and TaskTag Repositories

**Files:**
- Create: `backend/src/main/java/com/ansible/tag/repository/TagRepository.java`
- Create: `backend/src/main/java/com/ansible/tag/repository/TaskTagRepository.java`

- [ ] **Step 1: Create TagRepository**

```java
package com.ansible.tag.repository;

import com.ansible.tag.entity.Tag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByProjectIdOrderByIdAsc(Long projectId);

    boolean existsByProjectIdAndName(Long projectId, String name);

    boolean existsByProjectIdAndNameAndIdNot(Long projectId, String name, Long id);
}
```

- [ ] **Step 2: Create TaskTagRepository**

```java
package com.ansible.tag.repository;

import com.ansible.tag.entity.TaskTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskTagRepository extends JpaRepository<TaskTag, Long> {

    List<TaskTag> findByTaskId(Long taskId);

    List<TaskTag> findByTagId(Long tagId);

    void deleteByTaskIdAndTagId(Long taskId, Long tagId);

    boolean existsByTaskIdAndTagId(Long taskId, Long tagId);

    void deleteByTaskId(Long taskId);
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/ansible/tag/repository/
git commit -m "feat(tag): add TagRepository and TaskTagRepository"
```

---

### Task 3: Tag DTOs

**Files:**
- Create: `backend/src/main/java/com/ansible/tag/dto/CreateTagRequest.java`
- Create: `backend/src/main/java/com/ansible/tag/dto/UpdateTagRequest.java`
- Create: `backend/src/main/java/com/ansible/tag/dto/TagResponse.java`

- [ ] **Step 1: Create CreateTagRequest**

```java
package com.ansible.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTagRequest(
    @NotBlank @Size(max = 100) String name
) {}
```

- [ ] **Step 2: Create UpdateTagRequest**

```java
package com.ansible.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTagRequest(
    @NotBlank @Size(max = 100) String name
) {}
```

- [ ] **Step 3: Create TagResponse**

```java
package com.ansible.tag.dto;

import java.time.LocalDateTime;

public record TagResponse(
    Long id,
    Long projectId,
    String name,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/ansible/tag/dto/
git commit -m "feat(tag): add Tag DTOs"
```

---

### Task 4: TagService with Unit Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/tag/service/TagService.java`
- Create: `backend/src/test/java/com/ansible/tag/service/TagServiceTest.java`

- [ ] **Step 1: Write TagServiceTest**

```java
package com.ansible.tag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ansible.tag.dto.CreateTagRequest;
import com.ansible.tag.dto.TagResponse;
import com.ansible.tag.dto.UpdateTagRequest;
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
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    private Tag createTag(Long id, Long projectId, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setProjectId(projectId);
        tag.setName(name);
        return tag;
    }

    @Test
    void createTag_success() {
        when(tagRepository.existsByProjectIdAndName(1L, "web")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> {
            Tag t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TagResponse response = tagService.createTag(1L, new CreateTagRequest("web"), 100L);

        assertThat(response.name()).isEqualTo("web");
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void createTag_duplicateName_throws() {
        when(tagRepository.existsByProjectIdAndName(1L, "web")).thenReturn(true);

        assertThatThrownBy(() -> tagService.createTag(1L, new CreateTagRequest("web"), 100L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void listTags_success() {
        when(tagRepository.findByProjectIdOrderByIdAsc(1L))
            .thenReturn(List.of(createTag(1L, 1L, "web"), createTag(2L, 1L, "db")));

        List<TagResponse> list = tagService.listTags(1L);

        assertThat(list).hasSize(2);
    }

    @Test
    void updateTag_success() {
        Tag tag = createTag(1L, 1L, "web");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.existsByProjectIdAndNameAndIdNot(1L, "api", 1L)).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenReturn(tag);

        TagResponse response = tagService.updateTag(1L, new UpdateTagRequest("api"), 100L);

        assertThat(response.name()).isEqualTo("api");
    }

    @Test
    void updateTag_duplicateName_throws() {
        Tag tag = createTag(1L, 1L, "web");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.existsByProjectIdAndNameAndIdNot(1L, "db", 1L)).thenReturn(true);

        assertThatThrownBy(() -> tagService.updateTag(1L, new UpdateTagRequest("db"), 100L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    void deleteTag_success() {
        Tag tag = createTag(1L, 1L, "web");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        doNothing().when(tagRepository).delete(tag);

        tagService.deleteTag(1L, 100L);

        verify(tagRepository).delete(tag);
    }

    @Test
    void deleteTag_notFound_throws() {
        when(tagRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.deleteTag(999L, 100L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }
}
```

- [ ] **Step 2: Run test to verify failures**

Run: `cd backend && mvn test -Dtest=TagServiceTest -pl .`
Expected: FAIL (TagService does not exist yet)

- [ ] **Step 3: Write TagService**

```java
package com.ansible.tag.service;

import com.ansible.tag.dto.CreateTagRequest;
import com.ansible.tag.dto.TagResponse;
import com.ansible.tag.dto.UpdateTagRequest;
import com.ansible.tag.entity.Tag;
import com.ansible.tag.repository.TagRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public TagResponse createTag(Long projectId, CreateTagRequest request, Long userId) {
        if (tagRepository.existsByProjectIdAndName(projectId, request.name())) {
            throw new IllegalArgumentException("Tag with name '" + request.name() + "' already exists in this project");
        }
        Tag tag = new Tag();
        tag.setProjectId(projectId);
        tag.setName(request.name());
        tag.setCreatedBy(userId);
        return toResponse(tagRepository.save(tag));
    }

    @Transactional(readOnly = true)
    public List<TagResponse> listTags(Long projectId) {
        return tagRepository.findByProjectIdOrderByIdAsc(projectId).stream()
            .map(this::toResponse)
            .toList();
    }

    public TagResponse updateTag(Long tagId, UpdateTagRequest request, Long userId) {
        Tag tag = tagRepository.findById(tagId)
            .orElseThrow(() -> new IllegalArgumentException("Tag not found"));
        if (tagRepository.existsByProjectIdAndNameAndIdNot(tag.getProjectId(), request.name(), tagId)) {
            throw new IllegalArgumentException("Tag with name '" + request.name() + "' already exists in this project");
        }
        tag.setName(request.name());
        return toResponse(tagRepository.save(tag));
    }

    public void deleteTag(Long tagId, Long userId) {
        Tag tag = tagRepository.findById(tagId)
            .orElseThrow(() -> new IllegalArgumentException("Tag not found"));
        tagRepository.delete(tag);
    }

    private TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getProjectId(), tag.getName(),
            tag.getCreatedAt(), tag.getUpdatedAt());
    }
}
```

- [ ] **Step 4: Run tests**

Run: `cd backend && mvn test -Dtest=TagServiceTest -pl .`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/tag/service/ backend/src/test/java/com/ansible/tag/
git commit -m "feat(tag): add TagService with unit tests"
```

---

### Task 5: TagController with Integration Tests

**Files:**
- Create: `backend/src/main/java/com/ansible/tag/controller/TagController.java`
- Create: `backend/src/test/java/com/ansible/tag/controller/TagControllerTest.java`

- [ ] **Step 1: Write TagControllerTest** (integration test skeleton)

```java
package com.ansible.tag.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.ansible.common.AbstractIntegrationTest;
import com.ansible.tag.dto.CreateTagRequest;
import com.ansible.tag.dto.UpdateTagRequest;
import com.ansible.tag.entity.Tag;
import com.ansible.tag.repository.TagRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

class TagControllerTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TagRepository tagRepository;

    private String authHeaders;
    private Long projectId;

    @BeforeEach
    void setUp() {
        var auth = registerAndLogin("taguser", "taguser@test.com", "pass123");
        authHeaders = auth.token();
        projectId = createProject(authHeaders, "Tag Test Project");
    }

    @AfterEach
    void tearDown() {
        tagRepository.deleteAll();
        cleanupProjectsAndUsers();
    }

    @Test
    void createTag_success() {
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/projects/" + projectId + "/tags",
            HttpMethod.POST,
            withAuth(authHeaders, new CreateTagRequest("web")),
            String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("web");
    }

    @Test
    void createTag_duplicateName_returns400() {
        restTemplate.exchange("/api/projects/" + projectId + "/tags",
            HttpMethod.POST, withAuth(authHeaders, new CreateTagRequest("web")), String.class);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/projects/" + projectId + "/tags",
            HttpMethod.POST,
            withAuth(authHeaders, new CreateTagRequest("web")),
            String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void listTags_success() {
        restTemplate.exchange("/api/projects/" + projectId + "/tags",
            HttpMethod.POST, withAuth(authHeaders, new CreateTagRequest("web")), String.class);
        restTemplate.exchange("/api/projects/" + projectId + "/tags",
            HttpMethod.POST, withAuth(authHeaders, new CreateTagRequest("db")), String.class);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/projects/" + projectId + "/tags",
            HttpMethod.GET, withAuth(authHeaders, null), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("web", "db");
    }

    @Test
    void updateTag_success() {
        var createResp = restTemplate.exchange("/api/projects/" + projectId + "/tags",
            HttpMethod.POST, withAuth(authHeaders, new CreateTagRequest("web")), String.class);
        Long tagId = extractId(createResp);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/tags/" + tagId,
            HttpMethod.PUT,
            withAuth(authHeaders, new UpdateTagRequest("api")),
            String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("api");
    }

    @Test
    void deleteTag_success() {
        var createResp = restTemplate.exchange("/api/projects/" + projectId + "/tags",
            HttpMethod.POST, withAuth(authHeaders, new CreateTagRequest("web")), String.class);
        Long tagId = extractId(createResp);

        ResponseEntity<Void> response = restTemplate.exchange(
            "/api/tags/" + tagId,
            HttpMethod.DELETE, withAuth(authHeaders, null), Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tagRepository.findById(tagId)).isEmpty();
    }
}
```

- [ ] **Step 2: Write TagController**

```java
package com.ansible.tag.controller;

import com.ansible.tag.dto.CreateTagRequest;
import com.ansible.tag.dto.TagResponse;
import com.ansible.tag.dto.UpdateTagRequest;
import com.ansible.tag.service.TagService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @PostMapping("/projects/{projectId}/tags")
    public ResponseEntity<TagResponse> createTag(
        @PathVariable Long projectId,
        @Valid @RequestBody CreateTagRequest request,
        @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(tagService.createTag(projectId, request, userId));
    }

    @GetMapping("/projects/{projectId}/tags")
    public ResponseEntity<List<TagResponse>> listTags(@PathVariable Long projectId) {
        return ResponseEntity.ok(tagService.listTags(projectId));
    }

    @PutMapping("/tags/{tagId}")
    public ResponseEntity<TagResponse> updateTag(
        @PathVariable Long tagId,
        @Valid @RequestBody UpdateTagRequest request,
        @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(tagService.updateTag(tagId, request, userId));
    }

    @DeleteMapping("/tags/{tagId}")
    public ResponseEntity<Void> deleteTag(
        @PathVariable Long tagId,
        @AuthenticationPrincipal Long userId) {
        tagService.deleteTag(tagId, userId);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 3: Run integration tests**

Run: `cd backend && mvn verify -Dtest=TagControllerTest -pl .`
Expected: PASS

- [ ] **Step 4: Run quality checks**

Run: `cd backend && mvn spotless:apply checkstyle:check pmd:check spotbugs:check -pl .`
Expected: All pass

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/tag/controller/ backend/src/test/java/com/ansible/tag/controller/
git commit -m "feat(tag): add TagController with integration tests"
```

---

## Frontend Tasks

### Task 6: Tag Types and API Layer

**Files:**
- Create: `frontend/src/types/entity/Tag.ts`
- Create: `frontend/src/api/tag.ts`

- [ ] **Step 1: Create Tag types**

```typescript
// frontend/src/types/entity/Tag.ts

export interface Tag {
  id: number;
  projectId: number;
  name: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTagRequest {
  name: string;
}

export interface UpdateTagRequest {
  name: string;
}
```

- [ ] **Step 2: Create Tag API**

```typescript
// frontend/src/api/tag.ts

import request from './request';
import type { Tag, CreateTagRequest, UpdateTagRequest } from '../types/entity/Tag';

export const tagApi = {
  list: (projectId: number) =>
    request.get<Tag[]>(`/api/projects/${projectId}/tags`),

  create: (projectId: number, data: CreateTagRequest) =>
    request.post<Tag>(`/api/projects/${projectId}/tags`, data),

  update: (tagId: number, data: UpdateTagRequest) =>
    request.put<Tag>(`/api/tags/${tagId}`, data),

  delete: (tagId: number) =>
    request.delete(`/api/tags/${tagId}`),
};
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/types/entity/Tag.ts frontend/src/api/tag.ts
git commit -m "feat(tag): add Tag types and API layer"
```

---

### Task 7: TagManager Page

**Files:**
- Create: `frontend/src/pages/tag/TagManager.tsx`

- [ ] **Step 1: Create TagManager component**

```tsx
// frontend/src/pages/tag/TagManager.tsx

import { useEffect, useState } from 'react';
import { Button, Table, Modal, Form, Input, message, Popconfirm, Space } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { tagApi } from '../../api/tag';
import type { Tag } from '../../types/entity/Tag';
import { useProjectStore } from '../../stores/projectStore';

export default function TagManager() {
  const { currentProject } = useProjectStore();
  const [tags, setTags] = useState<Tag[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingTag, setEditingTag] = useState<Tag | null>(null);
  const [form] = Form.useForm();

  const fetchTags = async () => {
    if (!currentProject) return;
    setLoading(true);
    try {
      const res = await tagApi.list(currentProject.id);
      setTags(res.data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchTags(); }, [currentProject]);

  const handleCreate = () => {
    setEditingTag(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (tag: Tag) => {
    setEditingTag(tag);
    form.setFieldsValue({ name: tag.name });
    setModalOpen(true);
  };

  const handleDelete = async (tagId: number) => {
    await tagApi.delete(tagId);
    message.success('删除成功');
    fetchTags();
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (editingTag) {
      await tagApi.update(editingTag.id, values);
      message.success('更新成功');
    } else {
      await tagApi.create(currentProject!.id, values);
      message.success('创建成功');
    }
    setModalOpen(false);
    fetchTags();
  };

  const columns = [
    { title: '标签名称', dataIndex: 'name', key: 'name' },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: Tag) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>编辑</Button>
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
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建标签
        </Button>
      </div>
      <Table rowKey="id" columns={columns} dataSource={tags} loading={loading} />
      <Modal
        title={editingTag ? '编辑标签' : '新建标签'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="标签名称" rules={[{ required: true, message: '请输入标签名称' }]}>
            <Input maxLength={100} placeholder="例如: web, db, production" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
```

- [ ] **Step 2: Wire into project routes**

Add route in the project layout router file (follow existing pattern for environment/tag pages). Add a "标签管理" menu item pointing to `/projects/:id/tags`.

- [ ] **Step 3: Run lint and build**

Run: `cd frontend && npm run lint && npm run build`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/tag/ frontend/src/components/Layout/
git commit -m "feat(tag): add TagManager page and route"
```

---

### Task 8: TagSelect Component for Task/Handler Editors

**Files:**
- Create: `frontend/src/components/role/TagSelect.tsx`

- [ ] **Step 1: Create TagSelect component**

```tsx
// frontend/src/components/role/TagSelect.tsx

import { useEffect, useState } from 'react';
import { Select } from 'antd';
import { tagApi } from '../../api/tag';
import type { Tag } from '../../types/entity/Tag';
import { useProjectStore } from '../../stores/projectStore';

interface TagSelectProps {
  value?: number[];
  onChange?: (value: number[]) => void;
}

export default function TagSelect({ value, onChange }: TagSelectProps) {
  const { currentProject } = useProjectStore();
  const [tags, setTags] = useState<Tag[]>([]);

  useEffect(() => {
    if (!currentProject) return;
    tagApi.list(currentProject.id).then(res => setTags(res.data));
  }, [currentProject]);

  return (
    <Select
      mode="multiple"
      value={value}
      onChange={onChange}
      placeholder="选择标签"
      options={tags.map(t => ({ label: t.name, value: t.id }))}
      allowClear
    />
  );
}
```

- [ ] **Step 2: Integrate into Task/Handler form (follow up)**

This component is ready to be wired into the Task and Handler edit forms once the Task-Tag API endpoints are added to the TaskService. Add a `tags` field to the Task/Handler form using TagSelect.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/role/TagSelect.tsx
git commit -m "feat(tag): add TagSelect component for task/handler editors"
```
