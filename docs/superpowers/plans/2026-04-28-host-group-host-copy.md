# Host Group/Host Copy Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add copy/duplicate functionality for host groups (with cascade to hosts) and hosts (with pre-filled confirmation modal).

**Architecture:** New backend endpoint `POST /api/host-groups/{id}/copy` for host group copy. Reuse existing `POST /api/host-groups/{hgId}/hosts` for host copy with a new optional `copyFromHostId` field that triggers encrypted-field cloning from the source host.

**Tech Stack:** Spring Boot 3.x + Java 21 (JUnit 5 + Mockito + Testcontainers), React 18 + Ant Design 5 + TypeScript

---

### Task 1: Add copyFromHostId field to CreateHostRequest DTO

**Files:**
- Modify: `backend/src/main/java/com/ansible/host/dto/CreateHostRequest.java`

- [ ] **Step 1: Add the field**

```java
// Add after the ansibleBecome field, before the closing brace
private Long copyFromHostId;
```

The DTO uses `@Getter`/`@Setter` (Lombok), so no manual getters/setters needed. No validation annotation — it's optional and purely internal.

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/ansible/host/dto/CreateHostRequest.java
git commit -m "feat: add copyFromHostId field to CreateHostRequest DTO

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 2: Enhance HostService.createHost to support copy

**Files:**
- Modify: `backend/src/main/java/com/ansible/host/service/HostService.java:29-52`

- [ ] **Step 1: Add the copy logic to createHost**

Replace the existing `createHost` method body after the access check and HostGroup resolution. The changed section is from `host.setAnsibleSshPass` to the save call:

```java
// Replace lines 41-47 with:
if (StringUtils.hasText(request.getAnsibleSshPass())) {
  host.setAnsibleSshPass(encryptionService.encrypt(request.getAnsibleSshPass()));
} else if (request.getCopyFromHostId() != null) {
  Host sourceHost =
      hostRepository
          .findById(request.getCopyFromHostId())
          .orElse(null);
  if (sourceHost != null && sourceHost.getAnsibleSshPass() != null) {
    host.setAnsibleSshPass(sourceHost.getAnsibleSshPass());
  }
}
if (StringUtils.hasText(request.getAnsibleSshPrivateKeyFile())) {
  host.setAnsibleSshPrivateKeyFile(
      encryptionService.encrypt(request.getAnsibleSshPrivateKeyFile()));
} else if (request.getCopyFromHostId() != null) {
  Host sourceHost =
      hostRepository
          .findById(request.getCopyFromHostId())
          .orElse(null);
  if (sourceHost != null && sourceHost.getAnsibleSshPrivateKeyFile() != null) {
    host.setAnsibleSshPrivateKeyFile(sourceHost.getAnsibleSshPrivateKeyFile());
  }
}
```

Important: The source host lookup above needs to only happen once. Restructure the code so we look up the source host once instead of twice:

```java
// Snippet for the new createHost body — replace lines 35-48 (from "Host host = new Host();" through the save):
Host host = new Host();
host.setHostGroupId(hostGroupId);
host.setName(request.getName());
host.setIp(request.getIp());
host.setPort(Objects.requireNonNullElse(request.getPort(), 22));
host.setAnsibleUser(request.getAnsibleUser());

Host sourceHost = null;
if (request.getCopyFromHostId() != null) {
  sourceHost = hostRepository.findById(request.getCopyFromHostId()).orElse(null);
}

if (StringUtils.hasText(request.getAnsibleSshPass())) {
  host.setAnsibleSshPass(encryptionService.encrypt(request.getAnsibleSshPass()));
} else if (sourceHost != null && sourceHost.getAnsibleSshPass() != null) {
  host.setAnsibleSshPass(sourceHost.getAnsibleSshPass());
}
if (StringUtils.hasText(request.getAnsibleSshPrivateKeyFile())) {
  host.setAnsibleSshPrivateKeyFile(
      encryptionService.encrypt(request.getAnsibleSshPrivateKeyFile()));
} else if (sourceHost != null && sourceHost.getAnsibleSshPrivateKeyFile() != null) {
  host.setAnsibleSshPrivateKeyFile(sourceHost.getAnsibleSshPrivateKeyFile());
}
host.setAnsibleBecome(Objects.requireNonNullElse(request.getAnsibleBecome(), false));
host.setCreatedBy(currentUserId);
Host saved = hostRepository.save(host);
return new HostResponse(saved);
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/ansible/host/service/HostService.java
git commit -m "feat: support copyFromHostId in HostService.createHost

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 3: Add copyHostGroup to HostGroupService

**Files:**
- Modify: `backend/src/main/java/com/ansible/host/service/HostGroupService.java`
- Read: `backend/src/main/java/com/ansible/host/repository/HostRepository.java` (for reference — `findAllByHostGroupId`)

- [ ] **Step 1: Add HostRepository dependency and copyHostGroup method**

Add to imports:
```java
import com.ansible.host.entity.Host;
import com.ansible.host.repository.HostRepository;
```

Add to constructor (the class uses `@RequiredArgsConstructor`, so declare the field):
```java
private final HostRepository hostRepository;
```

Add the method before the closing brace of the class:

```java
@Transactional
public HostGroupResponse copyHostGroup(Long hostGroupId, Long currentUserId) {
  HostGroup source =
      hostGroupRepository
          .findById(hostGroupId)
          .orElseThrow(() -> new IllegalArgumentException("Host group not found"));
  accessChecker.checkOwnerOrAdmin(
      source.getProjectId(), source.getCreatedBy(), currentUserId);

  String newName = source.getName() + " (副本)";
  if (hostGroupRepository.existsByProjectIdAndName(source.getProjectId(), newName)) {
    int suffix = 2;
    while (hostGroupRepository.existsByProjectIdAndName(
        source.getProjectId(), newName + suffix)) {
      suffix++;
      if (suffix > 100) {
        throw new IllegalStateException(
            "Cannot generate a unique name for the copied host group");
      }
    }
    newName = newName + suffix;
  }

  HostGroup copy = new HostGroup();
  copy.setProjectId(source.getProjectId());
  copy.setName(newName);
  copy.setDescription(source.getDescription());
  copy.setCreatedBy(currentUserId);
  HostGroup saved = hostGroupRepository.save(copy);

  List<Host> sourceHosts = hostRepository.findAllByHostGroupId(hostGroupId);
  if (!sourceHosts.isEmpty()) {
    for (Host sourceHost : sourceHosts) {
      Host hostCopy = new Host();
      hostCopy.setHostGroupId(saved.getId());
      hostCopy.setName(sourceHost.getName());
      hostCopy.setIp(sourceHost.getIp());
      hostCopy.setPort(sourceHost.getPort());
      hostCopy.setAnsibleUser(sourceHost.getAnsibleUser());
      hostCopy.setAnsibleSshPass(sourceHost.getAnsibleSshPass());
      hostCopy.setAnsibleSshPrivateKeyFile(sourceHost.getAnsibleSshPrivateKeyFile());
      hostCopy.setAnsibleBecome(sourceHost.getAnsibleBecome());
      hostCopy.setCreatedBy(currentUserId);
      hostRepository.save(hostCopy);
    }
  }

  return new HostGroupResponse(saved);
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/ansible/host/service/HostGroupService.java
git commit -m "feat: add copyHostGroup method to HostGroupService

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 4: Add copy endpoint to HostGroupController

**Files:**
- Modify: `backend/src/main/java/com/ansible/host/controller/HostGroupController.java`

- [ ] **Step 1: Add the copy endpoint**

Add after the `deleteHostGroup` method:

```java
@PostMapping("/host-groups/{id}/copy")
public Result<HostGroupResponse> copyHostGroup(
    @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
  Long currentUserId = Long.valueOf(userDetails.getUsername());
  return Result.success(hostGroupService.copyHostGroup(id, currentUserId));
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/ansible/host/controller/HostGroupController.java
git commit -m "feat: add copy host group endpoint

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 5: Unit test HostGroupService.copyHostGroup

**Files:**
- Modify: `backend/src/test/java/com/ansible/host/service/HostGroupServiceTest.java`

- [ ] **Step 1: Add @Mock for HostRepository and write the test**

Add to the class fields:
```java
@Mock
private HostRepository hostRepository;
```

Add these test methods before the closing brace:

```java
@Test
void copyHostGroup_copiesHostGroupAndHosts() {
  Host sourceHost = new Host();
  ReflectionTestUtils.setField(sourceHost, "id", 10L);
  sourceHost.setHostGroupId(1L);
  sourceHost.setName("web-01");
  sourceHost.setIp("192.168.1.10");
  sourceHost.setPort(22);
  sourceHost.setAnsibleUser("ansible");
  sourceHost.setAnsibleSshPass("encrypted-pass");
  sourceHost.setAnsibleBecome(false);
  sourceHost.setCreatedBy(10L);

  HostGroup copiedHostGroup = new HostGroup();
  ReflectionTestUtils.setField(copiedHostGroup, "id", 2L);
  copiedHostGroup.setProjectId(10L);
  copiedHostGroup.setName("Web Servers (副本)");
  copiedHostGroup.setDescription("All web servers");
  copiedHostGroup.setCreatedBy(10L);

  when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));
  when(hostGroupRepository.existsByProjectIdAndName(10L, "Web Servers (副本)"))
      .thenReturn(false);
  when(hostGroupRepository.save(any(HostGroup.class))).thenReturn(copiedHostGroup);
  when(hostRepository.findAllByHostGroupId(1L)).thenReturn(List.of(sourceHost));
  when(hostRepository.save(any(Host.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));

  HostGroupResponse response = hostGroupService.copyHostGroup(1L, 10L);

  assertThat(response.getName()).isEqualTo("Web Servers (副本)");
  verify(accessChecker).checkOwnerOrAdmin(10L, 10L, 10L);
  verify(hostGroupRepository).save(any(HostGroup.class));
  verify(hostRepository).findAllByHostGroupId(1L);
  verify(hostRepository).save(any(Host.class));
}

@Test
void copyHostGroup_generatesUniqueName_whenDuplicate() {
  when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));
  when(hostGroupRepository.existsByProjectIdAndName(10L, "Web Servers (副本)"))
      .thenReturn(true);
  when(hostGroupRepository.existsByProjectIdAndName(10L, "Web Servers (副本)2"))
      .thenReturn(true);
  when(hostGroupRepository.existsByProjectIdAndName(10L, "Web Servers (副本)3"))
      .thenReturn(false);

  HostGroup copiedHostGroup = new HostGroup();
  ReflectionTestUtils.setField(copiedHostGroup, "id", 2L);
  copiedHostGroup.setName("Web Servers (副本)3");
  when(hostGroupRepository.save(any(HostGroup.class))).thenReturn(copiedHostGroup);
  when(hostRepository.findAllByHostGroupId(1L)).thenReturn(List.of());

  HostGroupResponse response = hostGroupService.copyHostGroup(1L, 10L);

  assertThat(response.getName()).isEqualTo("Web Servers (副本)3");
}

@Test
void copyHostGroup_hostGroupNotFound_throws() {
  when(hostGroupRepository.findById(99L)).thenReturn(Optional.empty());

  assertThatThrownBy(() -> hostGroupService.copyHostGroup(99L, 10L))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("not found");
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `cd backend && mvn test -Dtest=HostGroupServiceTest -q`
Expected: Tests run: 8, Failures: 0 (3 new + 5 existing)

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/ansible/host/service/HostGroupServiceTest.java
git commit -m "test: add unit tests for HostGroupService.copyHostGroup

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 6: Unit test HostService.createHost with copyFromHostId

**Files:**
- Modify: `backend/src/test/java/com/ansible/host/service/HostServiceTest.java`

- [ ] **Step 1: Add test for copy with retained encrypted fields**

Add before the closing brace:

```java
@Test
void createHost_withCopyFromHostId_retainsEncryptedFields() {
  Host sourceHost = new Host();
  sourceHost.setAnsibleSshPass("encrypted-source-pass");
  sourceHost.setAnsibleSshPrivateKeyFile("encrypted-source-key");

  CreateHostRequest request = new CreateHostRequest();
  request.setName("web-02");
  request.setIp("192.168.1.11");
  request.setPort(22);
  request.setAnsibleUser("ansible");
  request.setCopyFromHostId(5L);

  when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));
  when(hostRepository.findById(5L)).thenReturn(Optional.of(sourceHost));
  when(hostRepository.save(any(Host.class))).thenReturn(testHost);

  hostService.createHost(1L, request, 10L);

  verify(hostRepository).findById(5L);
  verify(encryptionService, never()).encrypt(any());
}

@Test
void createHost_withCopyFromHostId_userProvidedPasswordOverrides() {
  Host sourceHost = new Host();
  sourceHost.setAnsibleSshPass("encrypted-source-pass");

  CreateHostRequest request = new CreateHostRequest();
  request.setName("web-02");
  request.setIp("192.168.1.11");
  request.setPort(22);
  request.setAnsibleSshPass("new-password");
  request.setCopyFromHostId(5L);

  when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));
  when(encryptionService.encrypt("new-password")).thenReturn("encrypted-new");
  when(hostRepository.save(any(Host.class))).thenReturn(testHost);

  hostService.createHost(1L, request, 10L);

  verify(encryptionService).encrypt("new-password");
  verify(hostRepository, never()).findById(5L);
}

@Test
void createHost_withCopyFromHostId_sourceNotFound_doesNotFail() {
  CreateHostRequest request = new CreateHostRequest();
  request.setName("web-02");
  request.setIp("192.168.1.11");
  request.setPort(22);
  request.setCopyFromHostId(99L);

  when(hostGroupRepository.findById(1L)).thenReturn(Optional.of(testHostGroup));
  when(hostRepository.findById(99L)).thenReturn(Optional.empty());
  when(hostRepository.save(any(Host.class))).thenReturn(testHost);

  HostResponse response = hostService.createHost(1L, request, 10L);

  assertThat(response.getName()).isEqualTo("web-02");
}
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `cd backend && mvn test -Dtest=HostServiceTest -q`
Expected: Tests run: 8, Failures: 0 (3 new + 5 existing)

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/ansible/host/service/HostServiceTest.java
git commit -m "test: add unit tests for HostService.createHost with copyFromHostId

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 7: Integration tests for copy endpoints

**Files:**
- Modify: `backend/src/test/java/com/ansible/host/controller/HostGroupControllerTest.java`
- Modify: `backend/src/test/java/com/ansible/host/controller/HostControllerTest.java`

- [ ] **Step 1: Add integration test for copy host group endpoint**

In `HostGroupControllerTest.java`, add after the `deleteHostGroup_success` test:

```java
@Test
void copyHostGroup_copiesHostGroupAndHosts() {
  Long hgId = createHostGroup("Web Servers");

  ResponseEntity<Result<HostGroupResponse>> response =
      restTemplate.exchange(
          "/api/host-groups/" + hgId + "/copy",
          HttpMethod.POST,
          new HttpEntity<>(authHeaders()),
          new ParameterizedTypeReference<>() {});

  assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  assertThat(response.getBody().getData().getName()).isEqualTo("Web Servers (副本)");
  assertThat(response.getBody().getData().getProjectId()).isEqualTo(projectId);
}
```

- [ ] **Step 2: Add integration test for create host with copyFromHostId**

In `HostControllerTest.java`, add after the `deleteHost_success` test:

```java
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
```

- [ ] **Step 3: Run integration tests**

Run: `cd backend && mvn verify -Dtest=HostGroupControllerTest,HostControllerTest -q`
Expected: All tests pass (Testcontainers required — Docker must be running)

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/com/ansible/host/controller/HostGroupControllerTest.java backend/src/test/java/com/ansible/host/controller/HostControllerTest.java
git commit -m "test: add integration tests for host group copy and host copy

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 8: Add copy API and update types on frontend

**Files:**
- Modify: `frontend/src/api/host.ts`
- Modify: `frontend/src/types/entity/Host.ts`

- [ ] **Step 1: Add copyFromHostId to CreateHostRequest type**

In `frontend/src/types/entity/Host.ts`, add the field to `CreateHostRequest`:

```typescript
export interface CreateHostRequest {
  name: string;
  ip: string;
  port?: number;
  ansibleUser?: string;
  ansibleSshPass?: string;
  ansibleSshPrivateKeyFile?: string;
  ansibleBecome?: boolean;
  copyFromHostId?: number;
}
```

- [ ] **Step 2: Add copyHostGroup API function**

In `frontend/src/api/host.ts`, add before the `// Host APIs` comment:

```typescript
export async function copyHostGroup(id: number): Promise<HostGroup> {
  const res = await request.post<HostGroup>(`/host-groups/${id}/copy`);
  return res.data;
}
```

- [ ] **Step 3: Verify TypeScript compilation**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add frontend/src/types/entity/Host.ts frontend/src/api/host.ts
git commit -m "feat: add copyHostGroup API and copyFromHostId type

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 9: Add copy buttons to HostGroupManager

**Files:**
- Modify: `frontend/src/pages/host/HostGroupManager.tsx`

- [ ] **Step 1: Add imports and state variables**

Replace the icon import (line 17):
```typescript
import { CopyOutlined, DeleteOutlined, EditOutlined, PlusOutlined } from "@ant-design/icons";
```

Add `copyHostGroup` to the API import (line 26-34):
```typescript
import {
  copyHostGroup,
  createHostGroup,
  deleteHostGroup,
  getHostGroups,
  updateHostGroup,
  createHost,
  deleteHost,
  getHosts,
  updateHost,
} from "../../api/host";
```

Add state for copying host (after line 54, after `hostSaving`):
```typescript
const [copyingFromHostId, setCopyingFromHostId] = useState<number | null>(null);
```

- [ ] **Step 2: Add handleCopyHostGroup function**

Add after `handleDeleteHg` (after line 122):

```typescript
const handleCopyHostGroup = async (hgId: number) => {
  try {
    const copied = await copyHostGroup(hgId);
    message.success("主机组已复制");
    await fetchHostGroups();
    setSelectedHostGroup(copied);
    fetchHosts(copied.id);
  } catch {
    message.error("复制主机组失败");
  }
};
```

- [ ] **Step 3: Add handleCopyHost function**

Add after `handleDeleteHost` (after line 170):

```typescript
const handleCopyHost = (host: Host) => {
  setCopyingFromHostId(host.id);
  setEditingHost(null);
  hostForm.setFieldsValue({
    name: host.name,
    ip: host.ip,
    port: host.port,
    ansibleUser: host.ansibleUser,
    ansibleBecome: host.ansibleBecome,
  });
  setHostModalOpen(true);
};
```

- [ ] **Step 4: Modify handleCreateHost to pass copyFromHostId**

Replace the `handleCreateHost` function (lines 135-148):

```typescript
const handleCreateHost = async () => {
  if (!selectedHostGroup) return;
  setHostSaving(true);
  try {
    const values = await hostForm.validateFields();
    await createHost(selectedHostGroup.id, {
      ...values,
      copyFromHostId: copyingFromHostId ?? undefined,
    });
    message.success("主机创建成功");
    setHostModalOpen(false);
    setCopyingFromHostId(null);
    hostForm.resetFields();
    fetchHosts(selectedHostGroup.id);
  } finally {
    setHostSaving(false);
  }
};
```

- [ ] **Step 5: Add copy button to host group list**

In the host group list item actions (the `<Space>` on lines 266-282), add a copy button before the delete button:

```tsx
<Button
  type="text"
  size="small"
  icon={<CopyOutlined />}
  onClick={() => handleCopyHostGroup(hg.id)}
  style={{ color: "inherit" }}
/>
```

- [ ] **Step 6: Add copy button to host table**

In the `hostColumns` action column (lines 210-229), add a copy button before the edit button:

```tsx
<Tooltip title="复制">
  <Button
    type="text"
    size="small"
    icon={<CopyOutlined />}
    onClick={() => handleCopyHost(record)}
  />
</Tooltip>
```

- [ ] **Step 7: Update host modal title for copy mode**

Replace the host Modal title (line 354):
```tsx
title={editingHost ? "编辑主机" : copyingFromHostId ? "复制主机" : "新建主机"}
```

- [ ] **Step 8: Reset copyingFromHostId on modal close**

Replace the host Modal `onCancel` (lines 357-360):
```tsx
onCancel={() => {
  setHostModalOpen(false);
  setCopyingFromHostId(null);
  hostForm.resetFields();
}}
```

- [ ] **Step 9: Verify TypeScript compilation**

Run: `cd frontend && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 10: Commit**

```bash
git add frontend/src/pages/host/HostGroupManager.tsx
git commit -m "feat: add copy buttons for host groups and hosts

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

### Task 10: Run full verification

- [ ] **Step 1: Run all backend tests**

Run: `cd backend && mvn clean verify -q`
Expected: All tests pass, including Checkstyle, Spotless, PMD, SpotBugs

- [ ] **Step 2: Run frontend type check and lint**

Run: `cd frontend && npx tsc --noEmit && npm run lint`
Expected: No errors

- [ ] **Step 3: Start backend and frontend for manual smoke test**

Backend: `cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev`
Frontend: `cd frontend && npm run dev`

Manual checks:
- [ ] Navigate to project → host groups
- [ ] Create a host group "Web Servers" with some hosts
- [ ] Click copy button on the host group → verify new "Web Servers (副本)" appears with all hosts
- [ ] Click copy button on a host → verify modal opens with pre-filled fields (password blank)
- [ ] Modify host fields and confirm → verify new host created
- [ ] Leave password blank and confirm → verify host created (password copied from source)
- [ ] Fill new password and confirm → verify new password used
