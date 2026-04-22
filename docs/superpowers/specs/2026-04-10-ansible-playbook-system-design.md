# Ansible Playbook 开发系统 — 设计文档

**日期：** 2026-04-10  
**状态：** 已确认，待实施

---

## 1. 项目概述

一个用于 **开发和管理 Ansible Playbook** 的 Web 系统。系统本身不执行 Playbook，只提供可视化的 Playbook 开发环境，包括主机组、Role、Task、Handler、Template、文件、变量、环境、标签和 Playbook 的完整管理功能。

**单租户，内部团队使用。**

---

## 2. 技术栈

| 层 | 技术 |
|----|------|
| 后端语言 | Java 21 |
| 后端框架 | Spring Boot 3.x |
| 持久层 | Spring Data JPA |
| 安全 | Spring Security + JWT |
| 构建 | Maven（单模块） |
| 数据库 | PostgreSQL |
| 前端框架 | React 18 + TypeScript |
| 前端构建 | Vite |
| UI 组件库 | Ant Design 5.x |
| 路由 | React Router 6 |
| 状态管理 | Zustand |
| HTTP 客户端 | Axios |
| YAML 处理 | js-yaml |
| 代码仓库 | 前后端同一仓库（backend/ + frontend/） |

---

## 3. 整体架构

```
┌─────────────────────────────────────────────────────────┐
│              React 18 + Ant Design 5.x                  │
│              (Vite 构建，TypeScript，SPA)                │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP/REST + JWT Bearer Token
┌──────────────────────▼──────────────────────────────────┐
│             Spring Boot 3.x + Java 21                   │
│                   (Maven 单模块)                         │
│  Package: com.ansible.{module}.{layer}                  │
└──────────────────────┬──────────────────────────────────┘
                       │ Spring Data JPA
┌──────────────────────▼──────────────────────────────────┐
│                    PostgreSQL                            │
└─────────────────────────────────────────────────────────┘
```

---

## 4. 权限模型

### 4.1 项目成员角色

| 角色 | 权限 |
|------|------|
| `PROJECT_ADMIN` | 删除项目、编辑项目信息、添加/移除成员、更新成员角色 |
| `PROJECT_MEMBER` | 使用项目所有功能（创建、查看、编辑/删除自己的数据） |

### 4.2 数据级权限

| 操作 | 数据创建者 | PROJECT_ADMIN | 其他成员 |
|------|-----------|--------------|---------|
| 查看 | ✓ | ✓ | ✓ |
| 使用 | ✓ | ✓ | ✓ |
| 编辑 | ✓ | ✓ | ✗ |
| 删除 | ✓ | ✓ | ✗ |

> 数据包括：主机组、主机、Role、Task、Handler、Template、文件、变量、环境、标签、Playbook 等。

### 4.3 跨项目规则

- 用户只能访问自己参与的项目
- 注册用户默认无任何项目权限，可自行创建项目（成为 PROJECT_ADMIN）
- 无系统级超级管理员

### 4.4 用户 API 权限

- `PUT /api/users/{id}`、`DELETE /api/users/{id}` 仅允许当前登录用户操作自己的账号（`id == 当前用户`）
- `GET /api/users` 用于项目内搜索可添加的成员，仅返回有限字段（id, username, email）

### 4.5 安全检查机制

所有 API 均需依次校验：
1. JWT 是否有效
2. 用户是否为该项目成员
3. 对于编辑/删除操作：是否为数据创建者或 PROJECT_ADMIN

---

## 5. 数据库实体设计

### 5.1 核心实体

| 实体 | 说明 | 关键字段 |
|------|------|----------|
| `User` | 用户 | `username`, `password`(bcrypt), `email`, `createdAt` |
| `Project` | 项目 | `name`, `description`, `createdBy`, `createdAt` |
| `ProjectMember` | 项目成员 | `projectId`, `userId`, `role`(ADMIN/MEMBER) |
| `HostGroup` | 主机组 | `projectId`, `name`, `description`, `createdBy` |
| `Host` | 主机 | `hostGroupId`, `name`, `ip`, `port`, `ansibleUser`, `ansibleSshPass`, `ansibleSshPrivateKeyFile`, `ansibleBecome`, `createdBy` |
| `Role` | 任务组（Ansible Role） | `projectId`, `name`, `description`, `createdBy` |
| `Task` | 任务 | `roleId`, `name`, `module`, `args`(JSON), `when`, `loop`, `until`, `register`, `notify`(JSON数组，如`["Restart nginx"]`), `order`, `createdBy` |
| `Handler` | 处理器 | `roleId`, `name`, `module`, `args`(JSON), `when`, `register`, `createdBy` |
| `Tag` | 标签 | `projectId`, `name`, `createdBy` |
| `TaskTag` | Task-Tag 关联 | `taskId`, `tagId` |
| `Template` | .j2 模板文件 | `roleId`, `parentDir`, `name`, `targetPath`, `content`, `createdBy` |
| `RoleFile` | Role files/ 目录文件 | `roleId`, `parentDir`, `name`, `content`(bytea), `isDirectory`, `createdBy` |
| `RoleVariable` | Role vars/ 变量 | `roleId`, `key`, `value`, `createdBy` |
| `RoleDefaultVariable` | Role defaults/ 变量 | `roleId`, `key`, `value`, `createdBy` |
| `Variable` | 项目/主机组/环境级变量 | `scope`(PROJECT/HOSTGROUP/ENVIRONMENT), `scopeId`, `key`, `value`, `createdBy` |
| `Environment` | 环境 | `projectId`, `name`, `description`, `createdBy` |
| `EnvConfig` | 环境配置项 | `environmentId`, `configKey`, `configValue`, `createdBy` |
| `Playbook` | 剧本 | `projectId`, `name`, `description`, `extraVars`, `createdBy` |
| `PlaybookHostGroup` | Playbook-主机组关联 | `playbookId`, `hostGroupId` |
| `PlaybookRole` | Playbook-Role 关联 | `playbookId`, `roleId`, `order` |
| `PlaybookTag` | Playbook-Tag 关联 | `playbookId`, `tagId` |
| `PlaybookEnvironment` | Playbook-Environment 关联 | `playbookId`, `environmentId` |

### 5.2 变量层级（优先级从高到低）

```
环境级变量 > 主机组级变量 > Role级变量 > 项目级变量
```

### 5.3 Role 子模块

一个 Role 包含以下子模块：

| 子模块 | 说明 |
|--------|------|
| `tasks/` | Task 列表 |
| `handlers/` | Handler 列表 |
| `templates/` | .j2 模板文件（支持目录） |
| `files/` | 任意文件（支持目录，含二进制） |
| `vars/` | Role 内部变量 |
| `defaults/` | Role 默认变量 |

---

## 6. API 设计

### 6.1 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录，返回 JWT |
| POST | `/api/auth/logout` | 登出 |
| GET | `/api/auth/me` | 当前用户信息 |

### 6.2 用户

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/users` | 用户列表（分页） |
| GET | `/api/users/{id}` | 用户详情 |
| PUT | `/api/users/{id}` | 更新用户 |
| DELETE | `/api/users/{id}` | 删除用户 |

### 6.3 项目

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/projects` | 创建项目 |
| GET | `/api/projects` | 我的项目列表 |
| GET | `/api/projects/{id}` | 项目详情 |
| PUT | `/api/projects/{id}` | 更新项目（管理员） |
| DELETE | `/api/projects/{id}` | 删除项目（管理员） |
| GET | `/api/projects/{id}/members` | 成员列表 |
| POST | `/api/projects/{id}/members` | 添加成员 |
| DELETE | `/api/projects/{id}/members/{userId}` | 移除成员 |
| PUT | `/api/projects/{id}/members/{userId}` | 更新成员角色 |

### 6.4 主机组 & 主机

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/projects/{projectId}/host-groups` | 创建主机组 |
| GET | `/api/projects/{projectId}/host-groups` | 主机组列表 |
| GET | `/api/host-groups/{id}` | 主机组详情 |
| PUT | `/api/host-groups/{id}` | 更新主机组 |
| DELETE | `/api/host-groups/{id}` | 删除主机组 |
| POST | `/api/host-groups/{hgId}/hosts` | 创建主机 |
| GET | `/api/host-groups/{hgId}/hosts` | 主机列表 |
| GET | `/api/hosts/{id}` | 主机详情 |
| PUT | `/api/hosts/{id}` | 更新主机 |
| DELETE | `/api/hosts/{id}` | 删除主机 |

### 6.5 Role

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/projects/{projectId}/roles` | 创建 Role |
| GET | `/api/projects/{projectId}/roles` | Role 列表 |
| GET | `/api/roles/{id}` | Role 详情 |
| PUT | `/api/roles/{id}` | 更新 Role |
| DELETE | `/api/roles/{id}` | 删除 Role |

### 6.6 Task

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/roles/{roleId}/tasks` | 创建 Task |
| GET | `/api/roles/{roleId}/tasks` | Task 列表 |
| GET | `/api/tasks/{id}` | Task 详情 |
| PUT | `/api/tasks/{id}` | 更新 Task |
| DELETE | `/api/tasks/{id}` | 删除 Task |
| PUT | `/api/tasks/{id}/tags` | 更新 Task 标签 |
| GET | `/api/tasks/{id}/notifies` | 获取此 Task notify 的 Handlers |

### 6.7 Handler

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/roles/{roleId}/handlers` | 创建 Handler |
| GET | `/api/roles/{roleId}/handlers` | Handler 列表 |
| GET | `/api/handlers/{id}` | Handler 详情 |
| PUT | `/api/handlers/{id}` | 更新 Handler |
| DELETE | `/api/handlers/{id}` | 删除 Handler |
| GET | `/api/handlers/{id}/notified-by` | 获取 notify 此 Handler 的 Tasks |

### 6.8 Role Variable

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/roles/{roleId}/vars` | 创建 Role 变量 |
| GET | `/api/roles/{roleId}/vars` | Role 变量列表 |
| PUT | `/api/role-vars/{id}` | 更新 Role 变量 |
| DELETE | `/api/role-vars/{id}` | 删除 Role 变量 |

### 6.9 Role Default Variable

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/roles/{roleId}/defaults` | 创建 Role 默认变量 |
| GET | `/api/roles/{roleId}/defaults` | Role 默认变量列表 |
| PUT | `/api/role-defaults/{id}` | 更新 Role 默认变量 |
| DELETE | `/api/role-defaults/{id}` | 删除 Role 默认变量 |

### 6.10 Template

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/roles/{roleId}/templates` | 上传/创建模板 |
| GET | `/api/roles/{roleId}/templates` | 模板列表（树形） |
| GET | `/api/templates/{id}` | 模板详情（含内容） |
| PUT | `/api/templates/{id}` | 更新模板 |
| DELETE | `/api/templates/{id}` | 删除模板 |
| GET | `/api/templates/{id}/download` | 下载模板 |

### 6.11 File

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/roles/{roleId}/files` | 上传/创建文件或目录 |
| GET | `/api/roles/{roleId}/files` | 文件列表（树形） |
| GET | `/api/files/{id}` | 文件详情 |
| PUT | `/api/files/{id}` | 更新文件 |
| DELETE | `/api/files/{id}` | 删除文件/目录 |
| GET | `/api/files/{id}/download` | 下载文件 |

### 6.12 Variable

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/projects/{projectId}/variables` | 创建变量 |
| GET | `/api/projects/{projectId}/variables` | 变量列表（按 scope 过滤） |
| GET | `/api/variables/{id}` | 变量详情 |
| PUT | `/api/variables/{id}` | 更新变量 |
| DELETE | `/api/variables/{id}` | 删除变量 |

查询参数：`?scope=PROJECT` / `?scope=HOSTGROUP&hostGroupId=1` / `?scope=ENVIRONMENT&environmentId=1`

### 6.13 Tag

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/projects/{projectId}/tags` | 创建标签 |
| GET | `/api/projects/{projectId}/tags` | 标签列表 |
| PUT | `/api/tags/{id}` | 更新标签 |
| DELETE | `/api/tags/{id}` | 删除标签 |

### 6.14 Environment

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/projects/{projectId}/environments` | 创建环境 |
| GET | `/api/projects/{projectId}/environments` | 环境列表 |
| GET | `/api/environments/{id}` | 环境详情 |
| PUT | `/api/environments/{id}` | 更新环境 |
| DELETE | `/api/environments/{id}` | 删除环境 |

### 6.15 Playbook

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/projects/{projectId}/playbooks` | 创建 Playbook |
| GET | `/api/projects/{projectId}/playbooks` | Playbook 列表 |
| GET | `/api/playbooks/{id}` | Playbook 详情 |
| PUT | `/api/playbooks/{id}` | 更新 Playbook |
| DELETE | `/api/playbooks/{id}` | 删除 Playbook |
| POST | `/api/playbooks/{id}/roles` | 添加 Role |
| DELETE | `/api/playbooks/{id}/roles/{roleId}` | 移除 Role |
| PUT | `/api/playbooks/{id}/roles/order` | 调整 Role 顺序 |
| POST | `/api/playbooks/{id}/host-groups/{hostGroupId}` | 添加主机组 |
| DELETE | `/api/playbooks/{id}/host-groups/{hostGroupId}` | 移除主机组 |
| POST | `/api/playbooks/{id}/tags/{tagId}` | 添加 Tag |
| DELETE | `/api/playbooks/{id}/tags/{tagId}` | 移除 Tag |
| POST | `/api/playbooks/{id}/environments/{environmentId}` | 添加 Environment |
| DELETE | `/api/playbooks/{id}/environments/{environmentId}` | 移除 Environment |
| GET | `/api/playbooks/{id}/yaml` | 导出 YAML |

### 6.16 分页约定

所有列表 API 统一支持分页参数：`?page=0&size=20`，默认每页 20 条。Tag 列表等数据量小的端点可返回全量。

---

## 7. 后端包结构

```
com.ansible
├── AnsibleApplication.java
├── common/
│   ├── BaseEntity.java               # id, createdAt, updatedAt, createdBy
│   ├── Result.java                   # 统一响应体 {code, message, data}
│   ├── GlobalExceptionHandler.java
│   └── enums/
│       ├── ProjectRole.java          # PROJECT_ADMIN, PROJECT_MEMBER
│       └── VariableScope.java        # PROJECT, HOSTGROUP, ENVIRONMENT
├── security/
│   ├── SecurityConfig.java
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   ├── UserDetailsServiceImpl.java
│   └── ProjectAccessChecker.java     # 项目成员/创建者权限检查
├── user/
│   ├── entity/User.java
│   ├── repository/UserRepository.java
│   ├── service/UserService.java
│   └── controller/UserController.java
├── project/
│   ├── entity/{Project, ProjectMember}.java
│   ├── repository/
│   ├── service/
│   └── controller/
├── host/
│   ├── entity/{HostGroup, Host}.java
│   ├── repository/
│   ├── service/
│   └── controller/
├── role/
│   ├── entity/{Role, Task, Handler, Template, RoleFile,
│   │          RoleVariable, RoleDefaultVariable}.java
│   ├── repository/
│   ├── service/
│   └── controller/
├── variable/
│   ├── entity/Variable.java
│   ├── repository/
│   ├── service/
│   └── controller/
├── environment/
│   ├── entity/{Environment, EnvConfig}.java
│   ├── repository/
│   ├── service/
│   └── controller/
├── tag/
│   ├── entity/{Tag, TaskTag}.java
│   ├── repository/
│   ├── service/
│   └── controller/
└── playbook/
    ├── entity/{Playbook, PlaybookHostGroup, PlaybookRole, PlaybookTag, PlaybookEnvironment}.java
    ├── repository/
    ├── service/
    ├── controller/
    └── yaml/PlaybookYamlGenerator.java
```

---

## 8. 前端页面结构

### 8.1 路由

| 路径 | 页面 |
|------|------|
| `/login` | 登录 |
| `/register` | 注册 |
| `/projects` | 项目列表 |
| `/projects/:id/host-groups` | 主机组管理 |
| `/projects/:id/roles` | Role 列表 |
| `/projects/:id/roles/:roleId` | Role 详情（Tab：Tasks/Handlers/Templates/Files/Vars/Defaults） |
| `/projects/:id/variables` | 变量管理 |
| `/projects/:id/environments` | 环境管理 |
| `/projects/:id/tags` | 标签管理 |
| `/projects/:id/playbooks` | Playbook 列表 |
| `/projects/:id/playbooks/:pbId` | Playbook 构建器 |
| `/projects/:id/settings` | 项目设置 |
| `/projects/:id/members` | 成员管理 |

### 8.2 前端目录结构

```
frontend/
├── src/
│   ├── main.tsx
│   ├── App.tsx                        # 路由配置
│   ├── api/                           # API 层
│   │   ├── request.ts                # axios 实例 + 拦截器
│   │   ├── auth.ts
│   │   ├── user.ts
│   │   ├── project.ts
│   │   ├── host.ts
│   │   ├── role.ts
│   │   ├── variable.ts
│   │   ├── environment.ts
│   │   ├── tag.ts
│   │   └── playbook.ts
│   ├── components/
│   │   ├── Layout/
│   │   │   ├── MainLayout.tsx
│   │   │   └── ProjectLayout.tsx
│   │   └── Editor/
│   │       ├── YamlEditor.tsx
│   │       └── TaskEditor.tsx
│   ├── pages/
│   │   ├── auth/{Login, Register}.tsx
│   │   ├── project/{ProjectList, ProjectSettings}.tsx
│   │   ├── host/{HostGroupList, HostDetail}.tsx
│   │   ├── role/{RoleList, RoleDetail/}.tsx
│   │   │   └── RoleDetail/
│   │   │       ├── RoleTasks.tsx
│   │   │       ├── RoleHandlers.tsx
│   │   │       ├── RoleTemplates.tsx
│   │   │       ├── RoleFiles.tsx
│   │   │       ├── RoleVars.tsx
│   │   │       └── RoleDefaults.tsx
│   │   ├── variable/VariableManager.tsx
│   │   ├── environment/EnvironmentManager.tsx
│   │   ├── tag/TagManager.tsx
│   │   └── playbook/{PlaybookList, PlaybookBuilder}.tsx
│   ├── stores/
│   │   ├── authStore.ts
│   │   └── projectStore.ts
│   ├── types/
│   │   ├── entity/             # 实体类型定义
│   │   └── api/                # API 响应类型
│   └── utils/
│       └── yaml.ts
```

---

## 9. 核心功能细节

### 9.1 Playbook 可视化构建器

构建器允许用户通过 UI 组合：
- **主机选择**：选择一个或多个主机组
- **环境选择**：选择适用环境
- **Role 列表**：从项目 Role 中添加，支持拖拽排序
- **标签过滤**：选择需要应用的 Tag
- **YAML 预览**：实时渲染生成的 Playbook YAML
- **导出**：复制或下载 YAML 文件

### 9.2 Task 完整属性编辑器

Task 支持以下所有 Ansible 属性：
- `name`、`module`、`args`（Key-Value 编辑）
- `when`（条件表达式）
- `loop`（循环变量）
- `until`（重试条件）
- `register`（结果注册变量）
- `notify`（触发 Handler 名称列表）
- `tags`（关联标签）

同时提供实时 YAML 预览。

### 9.3 Handler 关联视图

- Handler 列表页展示哪些 Tasks notify 了该 Handler
- Task 编辑器中 notify 字段提供同 Role 内 Handler 名称的下拉选择

### 9.4 变量管理

- 支持树形和表格两种视图
- 按 scope 过滤：项目级/主机组级/Role级/环境级
- 显示变量优先级说明（环境 > 主机组 > Role > 项目）

### 9.5 文件管理（Role files/）

- 支持目录树展示
- 支持上传任意类型文件（文本/二进制）
- 支持创建/删除目录
- 支持文件下载

### 9.6 模板管理（Role templates/）

- 支持目录树展示
- 支持上传 .j2 文件
- 支持在线编辑（CodeMirror 或 Monaco Editor）
- 支持下载

---

## 10. 开发规范

### 10.1 细粒度迭代原则（核心规范）

**每次只实现一个功能单元，闭环后再进行下一个。**

一个功能单元的完成标准：
1. **实现**：功能代码完成
2. **单元测试**：Service 层单元测试全部通过
3. **集成测试**：Controller 层集成测试全部通过（使用真实数据库或 Testcontainers）
4. **代码格式扫描**：通过 Checkstyle/Spotless 检查
5. **代码规范扫描**：通过 PMD/SonarLint 检查
6. **安全扫描**：通过 SpotBugs/OWASP Dependency-Check 检查
7. **代码审查**：确认无问题后，合并进主分支

**严禁批量实现功能后再补测试。**

### 10.2 功能单元定义

以下每一项为一个独立功能单元：

**后端（每个实体的 CRUD 为一个功能单元）：**
- 用户注册/登录/JWT
- 项目 CRUD + 成员管理
- 主机组 CRUD
- 主机 CRUD
- Role CRUD
- Task CRUD
- Handler CRUD + Task 关联
- Template CRUD（含上传/下载/编辑）
- File CRUD（含上传/下载/目录管理）
- RoleVariable / RoleDefaultVariable CRUD
- Variable CRUD（四级）
- Environment CRUD
- Tag CRUD + TaskTag 关联
- Playbook CRUD + Role 关联 + YAML 生成

**前端（每个页面/模块为一个功能单元）：**
- 登录/注册页
- 项目列表/项目设置/成员管理
- 主机组/主机管理页
- Role 列表/详情页（含 6 个 Tab）
- 变量管理页
- 环境管理页
- 标签管理页
- Playbook 列表/构建器页

### 10.3 代码质量工具

#### 后端

| 工具 | 用途 | 集成方式 |
|------|------|----------|
| Checkstyle | 代码格式 | Maven 插件，`mvn checkstyle:check` |
| Spotless | 代码格式化 | Maven 插件，`mvn spotless:apply` |
| PMD | 代码规范 | Maven 插件，`mvn pmd:check` |
| SpotBugs | 安全/Bug 扫描 | Maven 插件，`mvn spotbugs:check` |
| OWASP Dependency-Check | 依赖安全 | Maven 插件，`mvn dependency-check:check` |
| JUnit 5 | 单元测试 | `mvn test` |
| Testcontainers | 集成测试（真实 PostgreSQL） | `mvn verify` |

#### 前端

| 工具 | 用途 | 命令 |
|------|------|------|
| ESLint | 代码规范 | `npm run lint` |
| Prettier | 代码格式 | `npm run format` |
| TypeScript 严格模式 | 类型安全 | `tsc --noEmit` |
| Vitest | 单元测试 | `npm run test` |

### 10.4 测试规范

#### 后端测试

- **单元测试**：Service 层使用 Mockito mock Repository，测试业务逻辑
- **集成测试**：Controller 层使用 `@SpringBootTest` + Testcontainers（PostgreSQL），测试完整请求链路
- **测试覆盖率**：Service 层不低于 80%

#### 前端测试

- **单元测试**：工具函数、状态管理使用 Vitest
- **组件测试**：关键组件使用 React Testing Library

### 10.5 安全规范

- 用户密码使用 BCrypt 加密存储
- 主机敏感字段（`ansibleSshPass`、`ansibleSshPrivateKeyFile`）使用 AES-256 加密存储，API 返回时脱敏显示（`****`）
- JWT Token 设置合理过期时间（默认 8 小时）
- 所有用户输入进行参数校验（Jakarta Validation）
- SQL 通过 JPA 参数化查询，不拼接 SQL
- 文件上传限制文件大小，校验文件类型
- 不在日志中打印敏感信息（密码、Token、主机凭据）
- 返回错误信息不暴露系统内部细节

### 10.6 功能实施顺序

后端按以下顺序实施，每个单元闭环后再进行下一个：

```
1. 用户认证（注册/登录/JWT）
2. 项目管理
3. 项目成员管理
4. 主机组管理
5. 主机管理
6. Role管理
7. Task管理
8. Handler管理 + Task关联
9. Template管理
10. File管理
11. RoleVariable/RoleDefaultVariable
12. Variable管理（四级）
13. Tag管理
14. Environment管理
15. Playbook管理 + YAML生成
```

前端在后端对应功能完成后同步开发。

---

## 11. 仓库结构

```
nine-tower/                    # 仓库根目录
├── backend/                  # Spring Boot 后端
│   ├── pom.xml
│   └── src/
├── frontend/                 # React 前端
│   ├── package.json
│   └── src/
└── docs/
    └── superpowers/
        └── specs/
            └── 2026-04-10-ansible-playbook-system-design.md
```
