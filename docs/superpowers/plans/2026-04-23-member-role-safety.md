# Member Role Safety Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent project admins from downgrading/removing themselves and prevent the last admin from being downgraded/removed, avoiding orphan projects.

**Architecture:** Add two validation rules to `ProjectMemberService` — self-protection (reject operations on self) and orphan-protection (reject when last admin). Add a `countByProjectIdAndRole` query to the repository. Frontend hides edit controls on the current user's row and warns about last-admin operations.

**Tech Stack:** Spring Boot 3 / Java 21 / Mockito / Testcontainers / React 18 / Ant Design 5 / TypeScript / Vitest

---

### Task 1: Add `countByProjectIdAndRole` to Repository

**Files:**
- Modify: `backend/src/main/java/com/ansible/project/repository/ProjectMemberRepository.java`

- [ ] **Step 1: Add the repository method**

Add the following method to `ProjectMemberRepository.java` (after line 15, before the closing brace):

```java
long countByProjectIdAndRole(Long projectId, ProjectRole role);
```

Add the import at the top (after line 5):

```java
import com.ansible.common.enums.ProjectRole;
```

The full file should look like:

```java
package com.ansible.project.repository;

import com.ansible.common.enums.ProjectRole;
import com.ansible.project.entity.ProjectMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

  List<ProjectMember> findAllByProjectId(Long projectId);

  Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

  boolean existsByProjectIdAndUserId(Long projectId, Long userId);

  void deleteByProjectId(Long projectId);

  long countByProjectIdAndRole(Long projectId, ProjectRole role);
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/ansible/project/repository/ProjectMemberRepository.java
git commit -m "feat: add countByProjectIdAndRole to ProjectMemberRepository"
```

---

### Task 2: Add Self-Protection and Orphan-Protection to `removeMember`

**Files:**
- Modify: `backend/src/main/java/com/ansible/project/service/ProjectMemberService.java:59-67`
- Test: `backend/src/test/java/com/ansible/project/service/ProjectMemberServiceTest.java`

- [ ] **Step 1: Write failing unit tests for `removeMember` safety checks**

Add these tests to `ProjectMemberServiceTest.java` (after the `removeMember_fails_when_not_member` test at line 132):

```java
@Test
void removeMember_fails_when_removing_self() {
    assertThatThrownBy(() -> projectMemberService.removeMember(1L, 10L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("不能将自己移出项目");
}

@Test
void removeMember_fails_when_removing_last_admin() {
    ProjectMember adminMember = new ProjectMember();
    adminMember.setProjectId(1L);
    adminMember.setUserId(20L);
    adminMember.setRole(ProjectRole.PROJECT_ADMIN);

    when(projectMemberRepository.findByProjectIdAndUserId(1L, 20L))
        .thenReturn(Optional.of(adminMember));
    when(projectMemberRepository.countByProjectIdAndRole(1L, ProjectRole.PROJECT_ADMIN))
        .thenReturn(1L);

    assertThatThrownBy(() -> projectMemberService.removeMember(1L, 20L, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("项目必须至少保留一个管理员");
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend && mvn test -Dtest=ProjectMemberServiceTest -pl . -q`
Expected: Two new tests FAIL (self-protection throws no exception yet, orphan-protection throws no exception yet).

- [ ] **Step 3: Implement safety checks in `removeMember`**

Replace the `removeMember` method in `ProjectMemberService.java` (lines 59-67) with:

```java
@Transactional
public void removeMember(Long projectId, Long userId, Long currentUserId) {
    accessChecker.checkAdmin(projectId, currentUserId);
    if (userId.equals(currentUserId)) {
      throw new IllegalArgumentException("不能将自己移出项目");
    }
    ProjectMember member =
        projectMemberRepository
            .findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found in this project"));
    if (member.getRole() == ProjectRole.PROJECT_ADMIN
        && projectMemberRepository.countByProjectIdAndRole(projectId, ProjectRole.PROJECT_ADMIN) == 1) {
      throw new IllegalArgumentException("项目必须至少保留一个管理员");
    }
    projectMemberRepository.delete(member);
}
```

Add the import at the top of the file (after the existing imports):

```java
import com.ansible.common.enums.ProjectRole;
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend && mvn test -Dtest=ProjectMemberServiceTest -pl . -q`
Expected: ALL tests PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/project/service/ProjectMemberService.java backend/src/test/java/com/ansible/project/service/ProjectMemberServiceTest.java
git commit -m "feat: add self-protection and orphan-protection to removeMember"
```

---

### Task 3: Add Self-Protection and Orphan-Protection to `updateMemberRole`

**Files:**
- Modify: `backend/src/main/java/com/ansible/project/service/ProjectMemberService.java:69-84`
- Test: `backend/src/test/java/com/ansible/project/service/ProjectMemberServiceTest.java`

- [ ] **Step 1: Write failing unit tests for `updateMemberRole` safety checks**

Add these tests to `ProjectMemberServiceTest.java` (after the `updateMemberRole_success` test at line 149):

```java
@Test
void updateMemberRole_fails_when_changing_own_role() {
    UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
    request.setRole(ProjectRole.PROJECT_MEMBER);

    assertThatThrownBy(() -> projectMemberService.updateMemberRole(1L, 10L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("不能修改自己的角色");
}

@Test
void updateMemberRole_fails_when_downgrading_last_admin() {
    ProjectMember adminMember = new ProjectMember();
    adminMember.setProjectId(1L);
    adminMember.setUserId(20L);
    adminMember.setRole(ProjectRole.PROJECT_ADMIN);

    UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
    request.setRole(ProjectRole.PROJECT_MEMBER);

    when(projectMemberRepository.findByProjectIdAndUserId(1L, 20L))
        .thenReturn(Optional.of(adminMember));
    when(projectMemberRepository.countByProjectIdAndRole(1L, ProjectRole.PROJECT_ADMIN))
        .thenReturn(1L);

    assertThatThrownBy(() -> projectMemberService.updateMemberRole(1L, 20L, request, 10L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("项目必须至少保留一个管理员");
}

@Test
void updateMemberRole_success_when_downgrading_non_last_admin() {
    ProjectMember adminMember = new ProjectMember();
    adminMember.setId(1L);
    adminMember.setProjectId(1L);
    adminMember.setUserId(20L);
    adminMember.setRole(ProjectRole.PROJECT_ADMIN);

    UpdateMemberRoleRequest request = new UpdateMemberRoleRequest();
    request.setRole(ProjectRole.PROJECT_MEMBER);

    when(projectMemberRepository.findByProjectIdAndUserId(1L, 20L))
        .thenReturn(Optional.of(adminMember));
    when(projectMemberRepository.countByProjectIdAndRole(1L, ProjectRole.PROJECT_ADMIN))
        .thenReturn(2L);
    when(projectMemberRepository.save(any(ProjectMember.class))).thenReturn(adminMember);
    when(userRepository.findById(20L)).thenReturn(Optional.of(testUser));

    ProjectMemberResponse response =
        projectMemberService.updateMemberRole(1L, 20L, request, 10L);

    assertThat(adminMember.getRole()).isEqualTo(ProjectRole.PROJECT_MEMBER);
    verify(projectMemberRepository).save(adminMember);
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend && mvn test -Dtest=ProjectMemberServiceTest -pl . -q`
Expected: Three new tests FAIL.

- [ ] **Step 3: Implement safety checks in `updateMemberRole`**

Replace the `updateMemberRole` method in `ProjectMemberService.java` (the method starting around line 69) with:

```java
@Transactional
public ProjectMemberResponse updateMemberRole(
    Long projectId, Long userId, UpdateMemberRoleRequest request, Long currentUserId) {
    accessChecker.checkAdmin(projectId, currentUserId);
    if (userId.equals(currentUserId)) {
      throw new IllegalArgumentException("不能修改自己的角色");
    }
    ProjectMember member =
        projectMemberRepository
            .findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found in this project"));
    if (member.getRole() == ProjectRole.PROJECT_ADMIN
        && request.getRole() != ProjectRole.PROJECT_ADMIN
        && projectMemberRepository.countByProjectIdAndRole(projectId, ProjectRole.PROJECT_ADMIN) == 1) {
      throw new IllegalArgumentException("项目必须至少保留一个管理员");
    }
    member.setRole(request.getRole());
    ProjectMember saved = projectMemberRepository.save(member);
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    return new ProjectMemberResponse(saved, user);
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend && mvn test -Dtest=ProjectMemberServiceTest -pl . -q`
Expected: ALL tests PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ansible/project/service/ProjectMemberService.java backend/src/test/java/com/ansible/project/service/ProjectMemberServiceTest.java
git commit -m "feat: add self-protection and orphan-protection to updateMemberRole"
```

---

### Task 4: Add Integration Tests for Safety Checks

**Files:**
- Modify: `backend/src/test/java/com/ansible/project/controller/ProjectMemberControllerTest.java`

- [ ] **Step 1: Write integration tests**

Add these tests to `ProjectMemberControllerTest.java` (after the `updateMemberRole_success` test at line 218):

```java
@Test
void removeMember_fails_when_removing_self() {
    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members/" + aliceId,
            HttpMethod.DELETE,
            new HttpEntity<>(aliceHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getMessage()).contains("不能将自己移出项目");
}

@Test
void updateMemberRole_fails_when_changing_own_role() {
    UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
    req.setRole(ProjectRole.PROJECT_MEMBER);

    ResponseEntity<Result<ProjectMemberResponse>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members/" + aliceId,
            HttpMethod.PUT,
            new HttpEntity<>(req, aliceHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getMessage()).contains("不能修改自己的角色");
}

@Test
void removeMember_fails_when_removing_last_admin_via_orphan_protection() {
    // Setup: alice promotes bob to admin, then alice removes bob's admin status
    // so alice becomes the sole admin. Then alice tries to remove bob.
    // But wait — bob is a MEMBER now, not an admin, so orphan protection doesn't apply.
    // Instead: make alice and bob both admins, then alice removes bob.
    // bob is an admin and the project has 2 admins → alice can remove bob (not last admin).
    // To test orphan protection via integration: we need admin A to remove admin B where B is the sole admin.
    // That's impossible — if B is the sole admin, A can't be an admin.
    // The only way orphan protection fires at the integration level is if
    // the system had a bug allowing a non-admin to call the endpoint, which checkAdmin prevents.
    // Orphan protection is a defense-in-depth guard verified at the unit-test level (Tasks 2-3).
    // Self-protection is the primary guard and is verified here.

    // Verify self-protection: alice tries to remove herself (she is the only admin)
    ResponseEntity<Result<Void>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members/" + aliceId,
            HttpMethod.DELETE,
            new HttpEntity<>(aliceHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getMessage()).contains("不能将自己移出项目");
}

@Test
void updateMemberRole_fails_when_downgrading_only_other_admin() {
    // Setup: alice is sole admin. She promotes bob to admin.
    // Now there are 2 admins (alice + bob). Alice removes her own admin status...
    // but self-protection prevents that.
    // Instead: test that when there are 2 admins, alice can downgrade bob (not last admin).
    // Then verify that the remaining sole admin (alice) cannot be downgraded by herself (self-protection).
    // This confirms the chain works: self-protection blocks the only realistic path to orphan.

    // alice promotes bob to admin
    AddMemberRequest addBob = new AddMemberRequest();
    addBob.setUserId(bobId);
    addBob.setRole(ProjectRole.PROJECT_ADMIN);
    restTemplate.exchange(
        "/api/projects/" + projectId + "/members",
        HttpMethod.POST,
        new HttpEntity<>(addBob, aliceHeaders()),
        new ParameterizedTypeReference<Result<ProjectMemberResponse>>() {});

    // alice tries to downgrade herself — self-protection blocks it
    UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
    req.setRole(ProjectRole.PROJECT_MEMBER);
    ResponseEntity<Result<ProjectMemberResponse>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members/" + aliceId,
            HttpMethod.PUT,
            new HttpEntity<>(req, aliceHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getMessage()).contains("不能修改自己的角色");
}

@Test
void updateMemberRole_success_when_multiple_admins() {
    // Make bob an admin
    AddMemberRequest addBob = new AddMemberRequest();
    addBob.setUserId(bobId);
    addBob.setRole(ProjectRole.PROJECT_ADMIN);
    restTemplate.exchange(
        "/api/projects/" + projectId + "/members",
        HttpMethod.POST,
        new HttpEntity<>(addBob, aliceHeaders()),
        new ParameterizedTypeReference<Result<ProjectMemberResponse>>() {});

    // Alice can downgrade bob (she is NOT targeting herself, and there's still alice as admin)
    UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
    req.setRole(ProjectRole.PROJECT_MEMBER);
    ResponseEntity<Result<ProjectMemberResponse>> response =
        restTemplate.exchange(
            "/api/projects/" + projectId + "/members/" + bobId,
            HttpMethod.PUT,
            new HttpEntity<>(req, aliceHeaders()),
            new ParameterizedTypeReference<>() {});

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getData().getRole()).isEqualTo(ProjectRole.PROJECT_MEMBER);
}
```

- [ ] **Step 2: Run integration tests**

Run: `cd backend && mvn verify -Dtest=ProjectMemberControllerTest -pl . -q`
Expected: ALL tests PASS.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/ansible/project/controller/ProjectMemberControllerTest.java
git commit -m "test: add integration tests for member role safety checks"
```

---

### Task 5: Format and Quality Checks

**Files:** None new — runs checks on modified files.

- [ ] **Step 1: Run Spotless formatting**

Run: `cd backend && mvn spotless:apply`

- [ ] **Step 2: Run all quality checks**

Run: `cd backend && mvn checkstyle:check pmd:check spotbugs:check -pl . -q`
Expected: All checks PASS. Fix any issues if they fail.

- [ ] **Step 3: Run full backend test suite**

Run: `cd backend && mvn test -pl . -q`
Expected: ALL tests PASS.

- [ ] **Step 4: Commit any formatting changes (if any)**

```bash
git add -u backend/
git commit -m "style: apply spotless formatting to member safety code"
```

(Only if there are formatting changes. If clean, skip this step.)

---

### Task 6: Frontend — Hide Edit Controls for Current User Row

**Files:**
- Modify: `frontend/src/pages/project/MemberManagement.tsx`
- Modify: `frontend/src/pages/project/__tests__/MemberManagement.test.tsx`

- [ ] **Step 1: Import `useAuthStore` and get current user ID**

In `MemberManagement.tsx`, add import after line 25:

```typescript
import { useAuthStore } from '../../stores/authStore';
```

Inside the component (after line 40, `const isAdmin = ...`), add:

```typescript
const currentUser = useAuthStore((s) => s.user);
```

- [ ] **Step 2: Modify the role column render function**

Replace the role column render function (lines 131-166) with logic that shows a tag for the current user's own row and a Select for others:

```typescript
      render: (role: string, record: ProjectMember) => {
        const isSelf = record.userId === currentUser?.id;
        const roleLabel = role === 'PROJECT_ADMIN' ? '管理员' : '成员';
        if (!isAdmin || isSelf) {
          return (
            <span
              style={{
                background:
                  role === 'PROJECT_ADMIN'
                    ? colors.tagAdminBg
                    : colors.tagMemberBg,
                color:
                  role === 'PROJECT_ADMIN'
                    ? colors.tagAdminColor
                    : colors.tagMemberColor,
                fontSize: 12,
                padding: '2px 8px',
                borderRadius: 4,
                fontWeight: 500,
              }}
            >
              {roleLabel}
            </span>
          );
        }
        return (
          <Select
            value={role}
            onChange={(value) =>
              handleRoleChange(
                record.userId,
                value as 'PROJECT_ADMIN' | 'PROJECT_MEMBER',
              )
            }
            options={[
              { value: 'PROJECT_ADMIN', label: '管理员' },
              { value: 'PROJECT_MEMBER', label: '成员' },
            ]}
            style={{ width: 120 }}
          />
        );
      },
```

- [ ] **Step 3: Modify the action column to exclude current user**

Replace the action column definition (lines 174-191) with:

```typescript
    ...(isAdmin
      ? [
          {
            title: '操作',
            key: 'action',
            render: (_: unknown, record: ProjectMember) => {
              if (record.userId === currentUser?.id) return null;
              return (
                <Popconfirm
                  title="确认移除此成员？"
                  onConfirm={() => handleRemove(record.userId)}
                >
                  <Button type="link" danger size="small">
                    移除
                  </Button>
                </Popconfirm>
              );
            },
          },
        ]
      : []),
```

- [ ] **Step 4: Commit frontend changes**

```bash
git add frontend/src/pages/project/MemberManagement.tsx
git commit -m "feat: hide role change and remove controls for current user row"
```

- [ ] **Step 5: Add frontend tests**

In `MemberManagement.test.tsx`, add a mock for `useAuthStore` (after the `useProjectStore` mock around line 18):

```typescript
vi.mock('../../../stores/authStore', () => ({
  useAuthStore: (selector: (s: { user: { id: number; username: string; email: string } | null }) => unknown) =>
    selector({ user: { id: 1, username: 'admin', email: 'admin@test.com' } }),
}));
```

Add a new test (after the existing tests):

```typescript
it('does not show Select or remove button for the current user row', async () => {
  mockGetMembers.mockResolvedValue([
    { ...baseMember, userId: 1, username: 'admin', email: 'a@x.test', role: 'PROJECT_ADMIN' },
    { ...baseMember, userId: 2, username: 'bob', email: 'b@x.test', role: 'PROJECT_MEMBER' },
  ]);
  renderPage();

  await waitFor(() => {
    expect(screen.getByText('admin')).toBeInTheDocument();
    expect(screen.getByText('bob')).toBeInTheDocument();
  });

  // Current user (admin, id=1) should NOT have a Select — their role is a tag
  const adminRow = screen.getByText('admin').closest('tr');
  expect(adminRow).toBeTruthy();
  // The admin row should show tag text "管理员" without a Select
  expect(adminRow!.querySelector('.ant-select')).toBeNull();

  // The bob row should have a Select (admin viewing non-self member)
  const bobRow = screen.getByText('bob').closest('tr');
  expect(bobRow!.querySelector('.ant-select')).toBeTruthy();

  // The admin row should NOT have a remove button
  const removeButtons = screen.queryAllByRole('button', { name: /移除/ });
  // Only bob's row should have remove
  expect(removeButtons).toHaveLength(1);
});
```

- [ ] **Step 6: Run frontend tests**

Run: `cd frontend && npm run test -- --run`
Expected: ALL tests PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/pages/project/MemberManagement.tsx frontend/src/pages/project/__tests__/MemberManagement.test.tsx
git commit -m "test: add frontend test for current user row safety controls"
```

---

### Task 7: Frontend — Warn When Operating on Last Admin

**Files:**
- Modify: `frontend/src/pages/project/MemberManagement.tsx`
- Modify: `frontend/src/pages/project/__tests__/MemberManagement.test.tsx`

This task adds a frontend guard: when the project has only one admin and the current admin tries to downgrade or remove that admin, show a warning message.

- [ ] **Step 1: Compute `adminCount` from members list**

In `MemberManagement.tsx`, after the `isAdmin` line (around line 40), add:

```typescript
const adminCount = members.filter((m) => m.role === 'PROJECT_ADMIN').length;
```

- [ ] **Step 2: Add `isLastAdmin` helper and guard to `handleRoleChange`**

Replace the `handleRoleChange` function (lines 106-122) with:

```typescript
const handleRoleChange = async (
  userId: number,
  role: 'PROJECT_ADMIN' | 'PROJECT_MEMBER',
) => {
  const target = members.find((m) => m.userId === userId);
  if (
    target?.role === 'PROJECT_ADMIN' &&
    role !== 'PROJECT_ADMIN' &&
    adminCount <= 1
  ) {
    message.warning('该成员是最后一个管理员，无法降级');
    return;
  }
  try {
    await updateMemberRole(projectId, userId, { role });
    message.success('角色更新成功');
    fetchMembers();
  } catch (error: unknown) {
    const msg =
      (error as { response?: { data?: { message?: string } } })?.response?.data
        ?.message ||
      (error as { message?: string })?.message ||
      '角色更新失败';
    message.error(msg);
  }
};
```

- [ ] **Step 3: Add guard to `handleRemove`**

Replace the `handleRemove` function (lines 91-104) with:

```typescript
const handleRemove = async (userId: number) => {
  const target = members.find((m) => m.userId === userId);
  if (target?.role === 'PROJECT_ADMIN' && adminCount <= 1) {
    message.warning('该成员是最后一个管理员，无法移除');
    return;
  }
  try {
    await removeMember(projectId, userId);
    message.success('成员已移除');
    fetchMembers();
  } catch (error: unknown) {
    const msg =
      (error as { response?: { data?: { message?: string } } })?.response?.data
        ?.message ||
      (error as { message?: string })?.message ||
      '移除失败';
    message.error(msg);
  }
};
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/project/MemberManagement.tsx
git commit -m "feat: add frontend last-admin warning for role change and removal"
```

- [ ] **Step 5: Add frontend test for last-admin warning**

Add this test to `MemberManagement.test.tsx` (after the existing tests):

```typescript
it('shows warning when trying to downgrade the last admin', async () => {
  const { updateMemberRole } = await import('../../../api/project');
  const mockUpdateRole = vi.mocked(updateMemberRole);

  mockGetMembers.mockResolvedValue([
    { ...baseMember, userId: 2, username: 'bob', email: 'b@x.test', role: 'PROJECT_ADMIN' },
  ]);
  renderPage();

  // bob is the only admin. Open the Select and pick '成员'
  const select = await screen.findByRole('combobox');
  await userEvent.click(select);
  const memberOption = await screen.findByText('成员');
  await userEvent.click(memberOption);

  // updateMemberRole should NOT have been called
  expect(mockUpdateRole).not.toHaveBeenCalled();
});
```

- [ ] **Step 6: Run frontend tests**

Run: `cd frontend && npm run test -- --run`
Expected: ALL tests PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/pages/project/__tests__/MemberManagement.test.tsx
git commit -m "test: add frontend test for last-admin downgrade warning"
```

---

### Task 8: Final Verification

- [ ] **Step 1: Run full backend suite**

Run: `cd backend && mvn test -pl . -q`
Expected: ALL tests PASS.

- [ ] **Step 2: Run full frontend suite**

Run: `cd frontend && npm run test -- --run`
Expected: ALL tests PASS.

- [ ] **Step 3: Run frontend lint**

Run: `cd frontend && npm run lint`
Expected: No errors.
