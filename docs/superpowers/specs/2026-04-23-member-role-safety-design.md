# 项目成员角色变更安全防护

日期：2026-04-23

## 背景

当前项目成员管理存在安全漏洞：管理员可以将自己的角色降级为普通成员，或将最后一个管理员降级/移除，导致项目变成无管理员的"孤儿项目"，无人能管理。系统没有恢复机制，只能通过数据库手动修复。

## 规则

### 后端：自保护（禁止管理员对自己操作）

`updateMemberRole` 和 `removeMember` 中，如果目标用户 `userId` 等于当前用户 `currentUserId`，拒绝操作：
- `updateMemberRole`："不能修改自己的角色"
- `removeMember`："不能将自己移出项目"

### 后端：孤儿保护（禁止操作导致零管理员）

在降级或移除管理员之前，检查该项目剩余管理员数量：
- 新增 `ProjectMemberRepository.countByProjectIdAndRole(Long projectId, ProjectRole role)`
- 如果目标当前是 `PROJECT_ADMIN`，且操作（降级或移除）后管理员数将变为 0，拒绝操作
- 错误消息："项目必须至少保留一个管理员"

### 后端方法逻辑

```
updateMemberRole(projectId, userId, request, currentUserId):
  1. checkAdmin(projectId, currentUserId)
  2. if userId == currentUserId → 400 "不能修改自己的角色"
  3. 查找目标成员，不存在则 404
  4. if 目标角色 == ADMIN && 新角色 != ADMIN
     && countByProjectIdAndRole(projectId, ADMIN) == 1
     → 400 "项目必须至少保留一个管理员"
  5. 更新角色

removeMember(projectId, userId, currentUserId):
  1. checkAdmin(projectId, currentUserId)
  2. if userId == currentUserId → 400 "不能将自己移出项目"
  3. 查找目标成员，不存在则 404
  4. if 目标角色 == ADMIN
     && countByProjectIdAndRole(projectId, ADMIN) == 1
     → 400 "项目必须至少保留一个管理员"
  5. 删除成员
```

### 前端：当前用户行特殊处理

在成员列表中，当前登录用户的行：
- 角色列：显示角色文本标签（如"管理员"），不渲染下拉选择框
- 操作列：不显示"移除"按钮

### 前端：最后一个管理员提示

对其他管理员的行，如果当前项目只有一个管理员：
- 角色下拉框选择"成员"时，前端预先提示"该成员是最后一个管理员，无法降级"
- 点击"移除"按钮时，同理提示

## 涉及文件

### 后端
- `ProjectMemberService.java` — 增加两条校验规则
- `ProjectMemberRepository.java` — 新增 `countByProjectIdAndRole` 方法
- `ProjectMemberServiceTest.java` — 新增测试用例
- `ProjectMemberControllerTest.java` — 新增集成测试用例

### 前端
- `MemberManagement.tsx` — 当前用户行不渲染编辑控件；最后一个管理员降级提示
- `MemberManagement.test.tsx` — 新增测试用例

## 测试用例

### 后端单元测试
- 管理员修改自己角色 → 400
- 管理员移除自己 → 400
- 降级最后一个管理员 → 400
- 移除最后一个管理员 → 400
- 降级非最后一个管理员 → 成功
- 移除非最后一个管理员 → 成功

### 后端集成测试
- PUT /api/projects/{id}/members/{self} 降级 → 400
- DELETE /api/projects/{id}/members/{self} → 400
- PUT 降级最后一个管理员 → 400
- DELETE 移除最后一个管理员 → 400

### 前端测试
- 当前用户行不显示角色下拉框和移除按钮
- 只有一个管理员时，降级操作有提示
